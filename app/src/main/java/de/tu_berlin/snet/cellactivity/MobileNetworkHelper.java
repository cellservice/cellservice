package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.location.Location;
import android.location.LocationManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.Event;
import de.tu_berlin.snet.cellactivity.util.EventList;


public class MobileNetworkHelper extends ContextWrapper {

    private static final String TAG = "T-Lab TRACKER";

    private DatabaseHelper mDB;
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener;

    // traffic observer items
    private TrafficObserver mTrafficObserver;
    private TrafficStateListener mTrafficStateListener;
    private int mCellRxBytes = 0;
    private int mCellTxBytes = 0;

    // cell change items
    private CellInfoObserver mCellInfoObserver;
    private CellInfoStateListener mCellInfoStateListener;

    private int mPreviousCallState;
    private CallStateListener mCallStateListener;
    private SmsReceiver mSMSReceiver;

    // location checker items
    private LocationManager mLocationManager = null;
    private LocationChecker mLocationChecker = null;

    // outgoing sms observer items
    private ContentResolver mContentResolver;
    private OutgoingSMSObserver mSMSObserver;
    private OutgoingSMSStateListener mOutgoingSMSStateListener;

    public MobileNetworkHelper(Context base) {
        super(base);
        mDB = DatabaseHelper.getInstance(this);
        Log.e("networkhelper", "stopping");
    }


    public void listenForEvents(){
        Log.e("networkhelper", "starting");
        mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = setupPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);


        mTrafficObserver = TrafficObserver.getInstance();
        mTrafficObserver.start();
        mTrafficStateListener = new TrafficStateListener();
        mTrafficObserver.addListener(mTrafficStateListener);

        mCellInfoObserver = CellInfoObserver.getInstance();
        mCellInfoStateListener = new CellInfoStateListener();
        mCellInfoObserver.addListener(mCellInfoStateListener);

        //location checker items
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationChecker = new LocationChecker(mLocationManager);


        mSMSObserver = OutgoingSMSObserver.getInstance();
        mContentResolver = this.getContentResolver();
        mContentResolver.registerContentObserver(Uri.parse("content://sms"), true, mSMSObserver);
        mOutgoingSMSStateListener = new OutgoingSMSStateListener();
        mSMSObserver.addListener(mOutgoingSMSStateListener);


        mPreviousCallState = mTelephonyManager.getCallState();
        mCallStateListener = new CallStateListener();
        mTelephonyManager.listen(mCallStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        filter.addAction("android.provider.Telephony.SMS_SENT");
        mSMSReceiver = new SmsReceiver();
        registerReceiver(mSMSReceiver, filter);

    }

    public void stopListening() {
        Log.e("networkhelper", "stopping");
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mTrafficObserver.removeListener(mTrafficStateListener);
        mTrafficObserver.stop();

        mCellInfoObserver.removeListener(mCellInfoStateListener);

        mTelephonyManager.listen(mCallStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(mSMSReceiver);

        mContentResolver.unregisterContentObserver(mSMSObserver);
        mSMSObserver.removeListener(mOutgoingSMSStateListener);
    }

    public PhoneStateListener setupPhoneStateListener()
    {
        return new PhoneStateListener() {

            /** Callback invoked when device cell location changes. */
            @SuppressLint("NewApi")
            public void onCellLocationChanged(CellLocation location)
            {
                Log.e("cellp", "last cellid: " + mCellInfoObserver.getPreviousCellInfo() + " total bytes: " + mCellRxBytes + mCellTxBytes);

                if(mCellRxBytes + mCellTxBytes > 0) {
                    makeEntry(new Event("data", System.currentTimeMillis(), mCellRxBytes, mCellTxBytes));
                }

                // reset traffic stats
                mCellRxBytes = 0;
                mCellTxBytes = 0;
            }
        };
    }

    class TrafficStateListener implements TrafficListener {
        @Override
        public void bytesRxTransferred(long bytes) {
            mCellRxBytes += bytes;
            System.out.println(bytes + " bytes received, Total cellRxbytes : "+ mCellRxBytes);
        }
        public void bytesTxTransferred(long bytes) {
            mCellTxBytes += bytes;
            System.out.println(bytes + " bytes transmitted. Total mCellTxBytes : "+ mCellTxBytes);
        }
    }

    class CellInfoStateListener implements CellInfoListener {
        @Override
        public void onLocationUpdate(CellInfo oldCellInfo, CellInfo newCellInfo) {
            makeEntry(new Event("Location Update", System.currentTimeMillis()));
        }
    }

    private final class CallStateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int newState, String incomingNumber) {

            switch (mPreviousCallState) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if (newState == TelephonyManager.CALL_STATE_OFFHOOK) {
                        Log.e("callState", "idle --> off hook = new outgoing call");
                        makeEntry(new Event("out: " + incomingNumber, System.currentTimeMillis()));
                    } else if (newState == TelephonyManager.CALL_STATE_RINGING) {
                        Log.e("callState", "idle --> ringing = new incoming call");
                        makeEntry(new Event("inc: " + incomingNumber, System.currentTimeMillis()));
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
                        makeEntry(new Event("recv: " + incomingNumber, System.currentTimeMillis()));
                    } else if (newState == TelephonyManager.CALL_STATE_IDLE) {
                        Log.e("callState", "ringing --> idle = missed call");
                    }
                    break;
            }
            mPreviousCallState = newState;
        }
    }

    private final class OutgoingSMSStateListener implements OutgoingSMSListener {
        @Override
        public void onSMSSent(String receiverAddress) {
            makeEntry(new Event("SMS sent", System.currentTimeMillis()));
        }
    }

    public class SmsReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals("android.provider.Telephony.SMS_RECEIVED")){
                //action for sms received
                Log.e("SMS", "From Broadcast receiver: "+"SMS Received");
                makeEntry(new Event("SMS recv", System.currentTimeMillis()));
            }
            /* THIS DOESN'T WORK 
            * http://stackoverflow.com/questions/990558/android-broadcast-receiver-for-sent-sms-messages
            * */
            else if(action.equals("android.provider.Telephony.SMS_SENT")){
                Log.e("SMS", "From Broadcast receiver: "+"SMS Sent");
                makeEntry(new Event("SMS sent", System.currentTimeMillis()));
            }
        }
        // constructor
        public SmsReceiver(){}
    }

   /* public void makeCallOrTextEntry(Event event) {
        makeEntry(event, null, null);
    }
    public void makeDataEntry(Event event, Integer byteRxCount, Integer byteTxCount) {
        makeEntry(event, byteRxCount,byteTxCount);
    }*/

  /*  public boolean makeEntry(String type, Integer byteRxCount, Integer byteTxCount){
        Log.d(TAG, "from MakeEntry method");
        int isPostProcessflag=0;
        Double NetLat =null, NetLong =null; Float Netacc=null;
        Double GPSLat =null, GPSLong =null; Float GPSacc=null;
        Location locationNet= mLocationChecker.getlocationNETWORK();
        Location locationGPS = mLocationChecker.getlocationGPS();
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
        boolean result = mDB.insertData(
                System.currentTimeMillis() / 1000, //utc timestamp in seconds,
                type,
                mCellInfoObserver.getPreviousCellInfo().getCellId(),
                mCellInfoObserver.getPreviousCellInfo().getLac(),
                mCellInfoObserver.getPreviousCellInfo().getMnc(),
                mCellInfoObserver.getPreviousCellInfo().getMcc(),
                mCellInfoObserver.getPreviousCellInfo().getConnectionType(),
                byteRxCount,
                byteTxCount,
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

    }*/
  public boolean makeEntry(Event event){
      Log.d(TAG, "from MakeEntry method");
      event.cellinfo = mCellInfoObserver.getPreviousCellInfo();
      Log.d("makeEntry", "("+ event.cellinfo.getCellId()+" " +event.cellinfo.getLac()+")");
      EventList.getmInstance().addToMap(event);

      AsyncEventResponse.LocationTaskListener listener = new AsyncEventResponse.LocationTaskListener(){
          @Override
          public void onAllLocationReceived(Location Netlocation, Location GPSlocation, int key) {

              EventList.getmInstance().eventMap.get(key).netLocation = Netlocation;
              EventList.getmInstance().eventMap.get(key).isProcessed++;
              EventList.getmInstance().eventMap.get(key).gpsLocation = GPSlocation;
              EventList.getmInstance().eventMap.get(key).isProcessed++;
             // Log.e("Async:", "call back "+ Netlocation.toString());
              if(EventList.getmInstance().eventMap.get(key).isProcessed == 3)
                 mDB.insertData(key);
          }
      };
      LocationGoogleApiAsync.HiddenApiTaskListener listener1 = new LocationGoogleApiAsync.HiddenApiTaskListener() {
          @Override
          public void onLocationReceived(Location HiddenApiLocation, int key) {
              EventList.getmInstance().eventMap.get(key).hiddenApiLocation = HiddenApiLocation;
              EventList.getmInstance().eventMap.get(key).isProcessed++;
              //  Log.e("Async:", "API Location "+ Netlocation.toString());
              if(EventList.getmInstance().eventMap.get(key).isProcessed == 3)
                  mDB.insertData(key);

          }
      };
      /*
      * add the event to the global map.
      * start the async tasks (api and network providers) by pasing event object and a token
      * when the task is done, return event object with location data and token to identify the object
      * */
      //task 1
      AsyncEventResponse task1 = new AsyncEventResponse (listener,getBaseContext(),event,EventList.getCount());
      task1.execute();
      //task 2
      LocationGoogleApiAsync task2 = new LocationGoogleApiAsync(listener1, event, EventList.getCount());
      task2.execute();

      return  true;


  }

}
