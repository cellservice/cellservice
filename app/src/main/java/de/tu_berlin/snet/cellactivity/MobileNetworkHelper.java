package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.TrafficStats;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
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
    }

    public void stopListening() {
        Log.e("networkhelper", "stopping");
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
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
        this.mLastConnectionType= lastConnectionType;
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
                GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
                gsmCellLocation.getCid();
                gsmCellLocation.getLac();
                gsmCellLocation.getPsc();


                Log.e("cellp", "registered location changed: " + gsmCellLocation.toString());
                myDb.insertData(System.currentTimeMillis() / 1000, "location change: "+gsmCellLocation.toString());
            }

            /** invoked when data connection state changes (only way to get the network type) */
            public void onDataConnectionStateChanged(int state, int networkType)
            {
                Log.e("cellp", "registered data connection change: "+networkType);
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
                }

                setLastConnectionType(networkType);
            }
        };
    }
}
