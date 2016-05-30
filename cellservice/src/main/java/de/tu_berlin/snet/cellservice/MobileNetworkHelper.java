package de.tu_berlin.snet.cellservice;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;
import de.tu_berlin.snet.cellservice.observer.CellInfoObserver;
import de.tu_berlin.snet.cellservice.observer.OutgoingSMSObserver;
import de.tu_berlin.snet.cellservice.observer.PhonecallReceiver;
import de.tu_berlin.snet.cellservice.observer.TrafficObserver;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.util.Anonymizer;

public class MobileNetworkHelper extends ContextWrapper {

    // *********************************************************************************************
    // members *************************************************************************************

    private static MobileNetworkHelper instance;
    private List<CDRListener> listeners = new ArrayList<CDRListener>();

    private Data mCurrentDataRecord;
    private Call mCurrentCall;

    // traffic observer items
    private TrafficObserver mTrafficObserver;
    private TrafficStateListener mTrafficStateListener;

    // cell change items
    private CellInfoObserver mCellInfoObserver;
    private CellInfoStateListener mCellInfoStateListener;

    private SmsReceiver mSMSReceiver;

    private CallReceiver mCallReceiver;

    // outgoing sms observer items
    private ContentResolver mContentResolver;
    private OutgoingSMSObserver mSMSObserver;
    private OutgoingSMSStateListener mOutgoingSMSStateListener;

    private ExecutorService executor;

    public static MobileNetworkHelper getInstance(Context base) {
        if (instance == null) {
            instance = new MobileNetworkHelper(base);
        }
        return instance;
    }

    private MobileNetworkHelper(Context base) {
        super(base);
    }

    // *********************************************************************************************
    // methods *************************************************************************************
    public void onStart() {
        mCellInfoObserver = CellInfoObserver.getInstance();
        executor = Executors.newFixedThreadPool(10);
        mCellInfoObserver.start();
        listenForEvents();
    }

    public void onStop() {
        stopListening();
    }

    public void addListener(CDRListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CDRListener listener) {
        listeners.remove(listener);
    }

    private void listenForEvents() {
        Log.d("networkhelper", "starting");

        mTrafficObserver = TrafficObserver.getInstance();
        mTrafficObserver.start();
        mTrafficStateListener = new TrafficStateListener();
        mTrafficObserver.addListener(mTrafficStateListener);

        mCellInfoStateListener = new CellInfoStateListener();
        mCellInfoObserver.addListener(mCellInfoStateListener);

        mSMSObserver = OutgoingSMSObserver.getInstance();
        mContentResolver = this.getContentResolver();
        mContentResolver.registerContentObserver(Uri.parse("content://sms"), true, mSMSObserver);
        mOutgoingSMSStateListener = new OutgoingSMSStateListener();
        mSMSObserver.addListener(mOutgoingSMSStateListener);

        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        smsFilter.addAction("android.provider.Telephony.SMS_SENT");
        mSMSReceiver = new SmsReceiver();
        registerReceiver(mSMSReceiver, smsFilter);

        IntentFilter callFilter = new IntentFilter();
        callFilter.addAction("android.intent.action.PHONE_STATE");
        callFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        mCallReceiver = new CallReceiver();
        registerReceiver(mCallReceiver, callFilter);

    }

    private void stopListening() {
        Log.d("networkhelper", "stopping");
        mTrafficObserver.removeListener(mTrafficStateListener);
        mTrafficObserver.stop();

        mCellInfoObserver.removeListener(mCellInfoStateListener);
        mCellInfoObserver.stop();

        unregisterReceiver(mSMSReceiver);

        unregisterReceiver(mCallReceiver);

        mContentResolver.unregisterContentObserver(mSMSObserver);
        mSMSObserver.removeListener(mOutgoingSMSStateListener);
    }

    private CellInfo addGPSLocation(CellInfo cellInfo) {
        Future<Location> lf = executor.submit(new LocationFuture(LocationManager.GPS_PROVIDER));
        cellInfo.addLocations(lf);
        return cellInfo;
    }

    private CellInfo addNetworkLocation(CellInfo cellInfo) {
        // TODO: Only if WLAN is connected, i.e. has internet access
        // THUS NOT LEADING TO A NEW DATAPOINT THAT WOULD OTHERWISE NOT BE CREATED
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            Future<Location> lf = executor.submit(new LocationFuture(LocationManager.NETWORK_PROVIDER));
            cellInfo.addLocations(lf);
        }

        return cellInfo;
    }

    private CellInfo addNetworkLocation(CellInfo cellInfo, boolean force) {
        if(force) {
            cellInfo.addLocations(executor.submit(new LocationFuture(LocationManager.NETWORK_PROVIDER)));
            return cellInfo;
        } else {
            return addNetworkLocation(cellInfo);
        }
    }

    // *********************************************************************************************
    // listener logic ******************************************************************************

    private final class TrafficStateListener implements TrafficObserver.TrafficListener {
        @Override
        public void onBytesTransferred(long rxBytes, long txBytes, long timestamp) {
            synchronized (MobileNetworkHelper.this) {
                if (mCurrentDataRecord == null) {
                    mCurrentDataRecord = new Data(mCellInfoObserver.getCurrentCellInfo(), rxBytes, txBytes);
                    CellInfo cell = mCurrentDataRecord.getCell();
                    mCurrentDataRecord.setCell(addGPSLocation(addNetworkLocation(cell, true)));

                } else {
                    mCurrentDataRecord.addBytes(rxBytes, txBytes, timestamp);
                }
            }
        }
    }

    private final class CellInfoStateListener implements CellInfoObserver.CellInfoListener {
        @Override
        public void onCellLocationChanged(CellInfo oldCell, CellInfo newCell) {
            synchronized (MobileNetworkHelper.this) {
                if (mCurrentDataRecord != null) {
                    for (CDRListener l : listeners) {
                        l.onDataSession(mCurrentDataRecord);
                    }
                    mCurrentDataRecord = null;
                }

                if (mCurrentCall != null) {
                    oldCell = addGPSLocation(addNetworkLocation(oldCell));
                    newCell = addGPSLocation(addNetworkLocation(newCell));
                    mCurrentCall.addHandover(new Handover(oldCell, newCell));
                }
            }
        }

        @Override
        public void onLocationUpdate(CellInfo oldCell, CellInfo newCell) {
            oldCell = addGPSLocation(addNetworkLocation(oldCell));
            newCell = addGPSLocation(addNetworkLocation(newCell));

            for (CDRListener l : listeners) {
                l.onLocationUpdate(new LocationUpdate(oldCell, newCell));
            }
        }
    }

    private final class OutgoingSMSStateListener implements OutgoingSMSObserver.OutgoingSMSListener {
        @Override
        public void onSMSSent(String receiverAddress) {
            CellInfo cell = mCellInfoObserver.getCurrentCellInfo();
            cell = addGPSLocation(addNetworkLocation(cell));
            for (CDRListener l : listeners) {
                l.onTextMessage(new TextMessage(cell, "outgoing", Anonymizer.anonymize(receiverAddress)));
            }
        }
    }

    private final class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {
                CellInfo cell = mCellInfoObserver.getCurrentCellInfo();
                cell = addGPSLocation(addNetworkLocation(cell));
                // TODO: ACQUIRE THE NUMBER THE TEXT MESSAGE HAS BEEN RECEIVED FROM, INSTEAD of "secret"
                for (CDRListener l : listeners) {
                    l.onTextMessage(new TextMessage(cell, "incoming", Anonymizer.anonymize("secret")));
                }
            }
        }
    }

    private final class CallReceiver extends PhonecallReceiver {
        @Override
        protected void onIncomingCallReceived(Context ctx, String number, long start) {
        }

        @Override
        protected void onIncomingCallAnswered(Context ctx, String number, long start) {
            CellInfo cell = mCellInfoObserver.getCurrentCellInfo();
            cell = addGPSLocation(addNetworkLocation(cell));
            mCurrentCall = new Call(cell, "incoming", number, new ArrayList<Handover>());
        }

        @Override
        protected void onIncomingCallEnded(Context ctx, String number, long start, long end) {
            if (mCurrentCall != null && mCurrentCall.getAddress().equals(number)) {
                mCurrentCall.setEndTime(end);
                mCurrentCall.setAddress(Anonymizer.anonymize(mCurrentCall.getAddress()));
                for (CDRListener l : listeners) {
                    l.onCallRecord(mCurrentCall);
                }
                mCurrentCall = null;
            }
        }

        @Override
        protected void onOutgoingCallStarted(Context ctx, String number, long start) {
            CellInfo cell = mCellInfoObserver.getCurrentCellInfo();
            cell = addGPSLocation(addNetworkLocation(cell));
            mCurrentCall = new Call(cell, "outgoing", number, new ArrayList<Handover>());
        }

        @Override
        protected void onOutgoingCallEnded(Context ctx, String number, long start, long end) {
            if (mCurrentCall != null && mCurrentCall.getAddress().equals(number)) {
                mCurrentCall.setEndTime(end);
                mCurrentCall.setAddress(Anonymizer.anonymize(mCurrentCall.getAddress()));
                for (CDRListener l : listeners) {
                    l.onCallRecord(mCurrentCall);
                }
                mCurrentCall = null;
            }
        }

        @Override
        protected void onMissedCall(Context ctx, String number, long start) {
        }
    }

}
