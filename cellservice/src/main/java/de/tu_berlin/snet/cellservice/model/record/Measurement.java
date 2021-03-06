package de.tu_berlin.snet.cellservice.model.record;

import com.google.gson.annotations.SerializedName;

import jsqlite.Blob;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class Measurement {
    @SerializedName("id")
    private long id;
    @SerializedName("cell_id")
    private int cell_id;
    @SerializedName("provider")
    private String provider;
    @SerializedName("accuracy")
    private double accuracy;
    @SerializedName("time")
    private long time;
    @SerializedName("event_id")
    private int event_id;
    @SerializedName("event_type")
    private int event_type;
    @SerializedName("lon")
    private double lon;
    @SerializedName("lat")
    private double lat;

    public Measurement(long id, int cell_id, String provider, double accuracy, long time, int event_id, int event_type, double lon, double lat) {
        this.id = id;
        this.cell_id = cell_id;
        this.provider = provider;
        this.accuracy = accuracy;
        this.time = time;
        this.event_id = event_id;
        this.event_type = event_type;
        this.lon = lon;
        this.lat = lat;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCell_id() {
        return cell_id;
    }

    public void setCell_id(int cell_id) {
        this.cell_id = cell_id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getEvent_id() {
        return event_id;
    }

    public void setEvent_id(int event_id) {
        this.event_id = event_id;
    }

    public int getEvent_type() {
        return event_type;
    }

    public void setEvent_type(int event_type) {
        this.event_type = event_type;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
}
