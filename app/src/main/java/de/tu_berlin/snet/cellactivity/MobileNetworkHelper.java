package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;


public class MobileNetworkHelper extends ContextWrapper {

    private CellInfo mLastCellInfo;

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
        myDb = new DatabaseHelper(this);
        Log.e("networkhelper", "stopping");
    }

    public void listenForEvents(){
        Log.e("networkhelper", "starting");
        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = setupPhoneStateListener();
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    public void stopListening() {
        Log.e("networkhelper", "stopping");
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE );
    }

    private CellInfo getCellInfo() {
        TelephonyManager tm =(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

        /* Why I use this Bitmask:
         * https://stackoverflow.com/questions/9808396/android-cellid-not-available-on-all-carriers#12969638
         */
        int cellID = location.getCid() & 0xffff;
        int lac = location.getLac();

        return new CellInfo(cellID, lac);
    }

    private class CellInfo {
        private String mCellId, mLac;

        public CellInfo(String cellid, String lac) {
            mCellId = cellid;
            mLac = lac;
        }

        public CellInfo(int cellid, int lac) {
            mCellId = Integer.toString(cellid);
            mLac = Integer.toString(lac);
        }

        public String getCellId() {
            return mCellId;
        }

        public String getLac() {
            return mLac;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof CellInfo)) {
                return false;
            }

            CellInfo that = (CellInfo) other;

            if(that.getCellId().equals(this.getCellId()) && that.getLac().equals(this.getLac())) {
                return true;
            } else { return false; }
        }

        @Override
        public String toString() {
            return getCellId() + " / " + getLac();
        }
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

                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_CDMA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_EDGE");
                        break;
                    case TelephonyManager.NETWORK_TYPE_EVDO_0:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_EVDO_0");
                        break;
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_GPRS");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSDPA:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_HSDPA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_HSPA:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_HSPA");
                        break;
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_IDEN");
                        break;
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_LTE");
                        break;
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_UMTS");
                        break;
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                        Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_UNKNOWN");
                        break;
                    default:
                        Log.e("data", "onDataConnectionStateChanged: Undefined Network: "
                                + networkType);
                        break;
                }
            }

            /** Callback invoked when network signal strengths changes. */
            public void onSignalStrengthsChanged(SignalStrength signalStrength)
            {
                Log.e("cellp", "registered: "+signalStrength.getGsmSignalStrength() + " and cellinfo is "+getCellInfo());
                CellInfo cellInfo = getCellInfo();

                /* Check whether the cell id changed, which would be another method
                 * more frequently checking than onCellLocationChanged */
                if(!cellInfo.equals(getLastCellInfo())) {
                    setLastCellInfo(cellInfo);
                    Log.e("cellp", "cellid CHANGED to " + cellInfo);
                    myDb.insertData(System.currentTimeMillis() / 1000, cellInfo.toString());
                }
            }

        };
    }
}
