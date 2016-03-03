package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.location.Location;
import android.location.LocationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import de.tu_berlin.snet.cellactivity.util.CellInfo;


public class MobileNetworkHelper extends ContextWrapper {

    private static final String TAG = "T-Lab TRACKER";
    private CellInfo mLastCellInfo = null;

    private String mLastConnectionType = "UNKNOWN";

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
    int cellBytes = 0;
    private TrafficStateListener trafficStateListener;
    private int mPreviousCallState;
    private CallStateListener callStateListener;
    private SmsReceiver smsReceiver;

    //location checker items
    private LocationManager mLocationManager = null;
    private LocationChecker locationChecker = null;

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
        trafficStateListener = new TrafficStateListener();
        trafficObserver.addListener(trafficStateListener);

        //location checker items
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationChecker = new LocationChecker(mLocationManager);


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
        trafficObserver.removeListener(trafficStateListener);
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

            mLastCellInfo = new CellInfo(cellID, lac, mnc, mcc, tm.getNetworkType());
        }
        catch (Exception e){
            mLastCellInfo = new CellInfo();
        }
    }


    public String getLastConnectionType() {
        return mLastConnectionType;
    }

    public void setLastConnectionType(String lastConnectionType) {
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

                if(cellBytes > 0) {
                   makeDataEntry("data", cellBytes);
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

                //setLastConnectionType(networkType);
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
                        makeCallOrTextEntry("out: " + incomingNumber);
                    } else if (newState == TelephonyManager.CALL_STATE_RINGING) {
                        Log.e("callState", "idle --> ringing = new incoming call");
                        makeCallOrTextEntry("inc: " + incomingNumber);
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
                         makeCallOrTextEntry("recv: "+incomingNumber);
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
                makeCallOrTextEntry("SMS recv");
            }
            /* THIS DOESN'T WORK 
            * http://stackoverflow.com/questions/990558/android-broadcast-receiver-for-sent-sms-messages
            * */
            else if(action.equals("android.provider.Telephony.SMS_SENT")){
                Log.e("SMS", "From Broadcast receiver: "+"SMS Sent");
                makeCallOrTextEntry("SMS sent");
            }
        }
        // constructor
        public SmsReceiver(){}
    }

    public void makeCallOrTextEntry(String type) {
        makeEntry(type, null);
    }
    public void makeDataEntry(String type, Integer byteCount) {
        makeEntry(type, byteCount);
    }

    public boolean makeEntry(String type, Integer byteCount){
        Log.d(TAG, "from MakeEntry method");
        int isPostProcessflag=0;
        Double NetLat =null, NetLong =null; Float Netacc=null;
        Double GPSLat =null, GPSLong =null; Float GPSacc=null;
        Location locationNet= locationChecker.getlocationNETWORK();
        Location locationGPS = locationChecker.getlocationGPS();
        Log.d(TAG, "Location from GPS when "+type +" "+ locationGPS);
        Log.d(TAG, "Location from NETWORK when  " + type + " " + locationNet);

        if (locationGPS==null) {
            isPostProcessflag++;
        }
        else{
            GPSLat=locationGPS.getLatitude();
            GPSLong=locationGPS.getLongitude();
            GPSacc = locationGPS.getAccuracy();
        }
        if (locationNet==null) {
            isPostProcessflag++;
        }
        else{
            NetLat=locationNet.getLatitude();
            NetLong=locationNet.getLongitude();
            Netacc = locationNet.getAccuracy();
        }
        boolean result = myDb.insertData(
                System.currentTimeMillis() / 1000, //utc timestamp in seconds,
                type,
                getCellInfo().getCellId(),
                getCellInfo().getLac(),
                getCellInfo().getMnc(),
                getCellInfo().getMcc(),
                getCellInfo().getConnectionType(),
                byteCount,
                NetLat,//locationNet.getLatitude(),
                NetLong,//locationNet.getLongitude(),
                Netacc,
                GPSLat,//locationGPS.getLatitude(),
                GPSLong,//locationGPS.getLongitude(),
                GPSacc,
                isPostProcessflag
        );

        if (result == true)
            Log.d(TAG,"inserted data to DB");
        else
            Log.d(TAG,"Failed to insert data");
        return result;

    }
}
