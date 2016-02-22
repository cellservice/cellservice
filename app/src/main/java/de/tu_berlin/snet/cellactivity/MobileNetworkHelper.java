package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import de.tu_berlin.snet.cellactivity.util.CellInfo;


public class MobileNetworkHelper extends ContextWrapper {

    private CellInfo mLastCellInfo = null;

    private int mLastConnectionType = -1;

    private CellInfo getLastCellInfo() {
        return mLastCellInfo;
    }

    private void setLastCellInfo(CellInfo mLastCellInfo) {
        this.mLastCellInfo = mLastCellInfo;
    }

    DatabaseHelper myDb;
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;
    TrafficObserver trafficObserver;
    long cellBytes = 0;
    private int mPreviousCallState;
    private CallStateListener callStateListener;
    private SmsReceiver smsReceiver;

    public MobileNetworkHelper(Context base) {
        super(base);
        myDb = DatabaseHelper.getInstance(this);
        Log.e("networkhelper", "stopping");
    }

    public void listenForEvents(){
        Log.e("networkhelper", "starting");
        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = setupPhoneStateListener();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);


        trafficObserver = TrafficObserver.getInstance();
        trafficObserver.start();
        TrafficStateListener trafficStateListener = new TrafficStateListener();
        trafficObserver.addListener(trafficStateListener);

        mPreviousCallState = telephonyManager.getCallState();
        callStateListener = new CallStateListener();
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_SENT");
        smsReceiver = new SmsReceiver();
        registerReceiver(smsReceiver, filter);
    }

    public void stopListening() {
        Log.e("networkhelper", "stopping");
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        trafficObserver.stop();
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(smsReceiver);
    }


    private void updateCellInfo() {
        try {
            TelephonyManager tm =(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

            /* Why I use this Bitmask:
             * https://stackoverflow.com/questions/9808396/android-cellid-not-available-on-all-carriers#12969638
             */
            int cellID = location.getCid() & 0xffff;
            int lac = location.getLac();

            String networkOperator = tm.getNetworkOperator();
            int mcc = Integer.parseInt(networkOperator.substring(0, 3));
            int mnc = Integer.parseInt(networkOperator.substring(3));

            mLastCellInfo = new CellInfo(cellID, lac, mnc, mcc, getLastConnectionType());
        }
        catch (Exception e){
            mLastCellInfo = new CellInfo();
        }
    }


    public int getLastConnectionType() {
        return mLastConnectionType;
    }

    public void setLastConnectionType(int lastConnectionType) {
        this.mLastConnectionType = lastConnectionType;
    }

    public CellInfo getCellInfo() {
        return mLastCellInfo;
    }

    public PhoneStateListener setupPhoneStateListener()
    {
        return new PhoneStateListener() {

            /** Callback invoked when device cell location changes. */
            @SuppressLint("NewApi")
            public void onCellLocationChanged(CellLocation location)
            {
                Log.e("cellp", "last cellid: " + getCellInfo() + " total bytes: " + cellBytes);
                if(cellBytes > 1000) {
                    myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " kbytes: " +cellBytes/1000);
                }

                // reset
                cellBytes = 0;
                updateCellInfo();
            }

            /** invoked when data connection state changes (only way to get the network type) */
            public void onDataConnectionStateChanged(int state, int networkType)
            {
                /*Log.e("cellp", "registered data connection change: "+networkType);
                switch (state) {
                    case TelephonyManager.DATA_DISCONNECTED:
                        Log.e("data", "onDataConnectionStateChanged: DATA_DISCONNECTED");
                        break;
                    case TelephonyManager.DATA_CONNECTING:
                        Log.e("data", "onDataConnectionStateChanged: DATA_CONNECTING");
                        break;
                    case TelephonyManager.DATA_CONNECTED:
                        Log.e("data", "onDataConnectionStateChanged: DATA_CONNECTED");
                        break;
                    case TelephonyManager.DATA_SUSPENDED:
                        Log.e("data", "onDataConnectionStateChanged: DATA_SUSPENDED");
                        break;
                    default:
                        Log.e("data", "onDataConnectionStateChanged: UNKNOWN " + state);
                        break;
                }*/

                setLastConnectionType(networkType);
            }
        };
    }

    class TrafficStateListener implements TrafficListener {
        @Override
        public void bytesTransferred(long bytes) {
            cellBytes += bytes;
            System.out.println(bytes + " bytes exchanged. Total cellbytes : "+cellBytes);
        }
    }

    private final class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int newState, String incomingNumber) {

            switch (mPreviousCallState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (newState == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Log.e("callState", "idle --> off hook = new outgoing call");
                        myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " out: " + incomingNumber);
                    } else if (newState == TelephonyManager.CALL_STATE_RINGING) {
                        Log.e("callState", "idle --> ringing = new incoming call");
                        myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " inc: " + incomingNumber);
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (newState == TelephonyManager.CALL_STATE_IDLE) {
                        Log.e("callState", "off hook --> idle  = disconnected");

                    } else if (newState == TelephonyManager.CALL_STATE_RINGING) {
                        Log.e("callState", "off hook --> ringing = another call waiting");
                    }
                    Log.e("CALL_STATE_OFFHOOK", String.valueOf(newState));
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    if (newState == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Log.e("callState", "ringing --> off hook = received");
                        //makeEntry("Call Received");
                        myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " recv: "+incomingNumber);

                    } else if (newState == TelephonyManager.CALL_STATE_IDLE) {
                        Log.e("callState", "ringing --> idle = missed call");
                    }
                    break;
            }
            mPreviousCallState = newState;
        }
    }

    public class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
                //action for sms received
                Log.e("SMS", "From Broadcast receiver: "+"SMS Received");
                myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " SMS recv");
            }
            /* THIS DOESN'T WORK 
            * http://stackoverflow.com/questions/990558/android-broadcast-receiver-for-sent-sms-messages
            * */
            else if(action.equals("android.provider.Telephony.SMS_SENT")){
                Log.e("SMS", "From Broadcast receiver: "+"SMS Sent");
                myDb.insertData(System.currentTimeMillis() / 1000, getCellInfo() + " SMS sent");
            }
        }
        // constructor
        public SmsReceiver(){}
    }
}
