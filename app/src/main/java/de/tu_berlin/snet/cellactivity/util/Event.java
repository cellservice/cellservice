package de.tu_berlin.snet.cellactivity.util;

import android.location.Location;

/**
 * Created by ashish on 31.03.16.
 */
public class Event {
    public String type;
    public long timestamp;
    public long endtimestamp;
    public Location netLocation;
    public Location gpsLocation;
    public Location hiddenApiLocation;
    public  Integer byteRxCount;
    public Integer byteTxCount;
    public int isProcessed;
    public CellInfo cellinfo;
    public Event(String type, long timestamp){
        this.type= type;
        this.timestamp = timestamp;
    }
    public Event(String type, long timestamp, Integer byteRxCount, Integer byteTxCount){
        this.type= type;
        this.timestamp = timestamp;
        this.byteRxCount = byteRxCount;
        this.byteTxCount = byteTxCount;
    }
    public Event(String type, long timestamp, Integer byteRxCount, Integer byteTxCount, CellInfo cellInfo){
        this.type= type;
        this.timestamp = timestamp;
        this.byteRxCount = byteRxCount;
        this.byteTxCount = byteTxCount;
        this.cellinfo = cellInfo;
    }
}
