package de.tu_berlin.snet.cellservice.model.record;


import android.location.Location;
import android.telephony.TelephonyManager;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellservice.util.serialization.ObjectSerializer;
import de.tu_berlin.snet.cellservice.util.serialization.Serializable;

public class CellInfo implements Serializable {
    @SerializedName("id")
    private long id;
    @SerializedName("cellid")
    private int cellId;
    @SerializedName("lac")
    private int lac;
    @SerializedName("mnc")
    private int mnc;
    @SerializedName("mcc")
    private int mcc;
    @SerializedName("technology")
    private int connectionType;
    private transient ArrayList<Future<Location>> futureLocations;

    public CellInfo(int cellId, int lac, int mnc, int mcc, int connectionType) {
        this(-1, cellId, lac, mnc, mcc, connectionType);
    }

    public CellInfo(long id, int cellId, int lac, int mnc, int mcc, int connectionType) {
        this.id = id;
        this.cellId = cellId;
        this.lac = lac;
        this.mnc = mnc;
        this.mcc = mcc;
        this.connectionType = connectionType;
        initializeLocations();
    }

    public void initializeLocations() {
        this.futureLocations = new ArrayList<Future<Location>>();
    }

    public CellInfo(CellInfo cellInfo) {
        this.id = cellInfo.getId();
        this.cellId = cellInfo.getCellId();
        this.lac = cellInfo.getLac();
        this.mnc = cellInfo.getMnc();
        this.mcc = cellInfo.getMcc();
        this.connectionType = cellInfo.getConnectionType();
        this.futureLocations = cellInfo.getLocations(); // Do they need to be cloned?
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

    public long getId() {
        return id;
    }

    public int getCellId() {
        return cellId;
    }

    public int getLac() {
        return lac;
    }

    public int getMnc() {
        return mnc;
    }

    public int getMcc() {
        return mcc;
    }

    public int getConnectionType() { return connectionType; }

    public String getConnectionTypeString() {
        return resolveNetworkType(connectionType);
    }

    public boolean isFake() {
        return getCellId() == -1 || getLac() == -1 || getMnc() == -1 || getMcc() == -1;
    }

    @SafeVarargs
    final public void addLocations(Future<Location>... futureLocations) {
        for(Future<Location> fl : futureLocations) {
            this.futureLocations.add(fl);
        }
    }

    final public ArrayList<Future<Location>> getLocations(){
        return futureLocations;
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

    @Override
    public String serialize(ObjectSerializer serializer) {
        return serializer.serialize(this);
    }
}