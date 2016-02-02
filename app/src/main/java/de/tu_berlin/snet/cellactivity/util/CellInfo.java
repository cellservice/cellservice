package de.tu_berlin.snet.cellactivity.util;


import android.telephony.TelephonyManager;
import android.util.Log;

public class CellInfo {
    private String mCellId, mLac, mMnc, mMcc, mConnectionType;

    public CellInfo(String cellid, String lac, String mnc, String mcc, String connectionType) {
        mCellId = cellid;
        mLac = lac;
        mMnc = mnc;
        mMcc = mcc;
        mConnectionType = connectionType;
    }

    public CellInfo(int cellid, int lac, int mnc, int mcc, int connectionType) {
        mCellId = Integer.toString(cellid);
        mLac = Integer.toString(lac);
        mMnc = Integer.toString(mnc);
        mMcc = Integer.toString(mcc);

        switch (connectionType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
                mConnectionType = "CDMA";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_CDMA");
                break;
            case TelephonyManager.NETWORK_TYPE_EDGE:
                mConnectionType = "EDGE";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_EDGE");
                break;
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                mConnectionType = "EVDO_0";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_EVDO_0");
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
                mConnectionType = "GPRS";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_GPRS");
                break;
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                mConnectionType = "HSDPA";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_HSDPA");
                break;
            case TelephonyManager.NETWORK_TYPE_HSPA:
                mConnectionType = "HSPA";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_HSPA");
                break;
            case TelephonyManager.NETWORK_TYPE_IDEN:
                mConnectionType = "IDEN";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_IDEN");
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                mConnectionType = "LTE";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_LTE");
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                mConnectionType = "UMTS";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_UMTS");
                break;
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                mConnectionType = "UNKNOWN";
                //Log.e("data", "onDataConnectionStateChanged: NETWORK_TYPE_UNKNOWN");
                break;
            default:
                mConnectionType = "UNDEFINED";
                break;
        }
    }

    /**
     * Error Constructor
     */
    public CellInfo() {
        mCellId = "-1";
        mLac = "-1";
        mMnc = "-1";
        mMcc = "-1";
    }

    public String getCellId() { return mCellId; }

    public String getLac() { return mLac; }

    public String getMnc() { return mMnc; }

    public String getMcc() { return mMcc; }

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