package de.tu_berlin.snet.cellactivity.util;


import android.telephony.TelephonyManager;
import android.util.Log;

public class CellInfo {
    private int mCellId, mLac, mMnc, mMcc;
    private String mConnectionType;

    public CellInfo(int cellid, int lac, int mnc, int mcc, int connectionType) {
        mCellId = cellid;
        mLac = lac;
        mMnc = mnc;
        mMcc = mcc;
        mConnectionType = resolveNetworkType(connectionType);
    }

    private String resolveNetworkType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return "eHRPD";
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return "EVDO rev. 0";
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return "EVDO rev. A";
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return "EVDO rev. B";
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return "HSDPA";
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return "HSPA";
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "HSPA+";
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return "HSUPA";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "iDen";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return "UMTS";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "Unknown";
            default:
                return "Unknown new type";
        }
    }

    /**
     * Error Constructor
     */
    public CellInfo() {
        mCellId = -1;
        mLac = -1;
        mMnc = -1;
        mMcc = -1;
    }

    public int getCellId() {
        return mCellId;
    }

    public int getLac() {
        return mLac;
    }

    public int getMnc() {
        return mMnc;
    }

    public int getMcc() {
        return mMcc;
    }

    public String getConnectionType() {
        return mConnectionType;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CellInfo)) {
            return false;
        }

        CellInfo that = (CellInfo) other;

        return that.getCellId() == this.getCellId() &&
                that.getLac() == this.getLac() &&
                that.getMnc() == this.getMnc() &&
                that.getMcc() == this.getMcc();
    }

    @Override
    public String toString() {
        return getCellId() + " / " + getLac();
    }
}