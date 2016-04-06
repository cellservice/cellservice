package de.tu_berlin.snet.cellactivity.util;

/**
 * Created by ashish on 31.03.16.
 */
public class Event {
    public String type;
    public long timestamp;
    public Event(String type, long timestamp){
        this.type= type;
        this.timestamp = timestamp;
    }
}
