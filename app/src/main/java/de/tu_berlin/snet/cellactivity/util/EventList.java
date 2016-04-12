package de.tu_berlin.snet.cellactivity.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ashish on 07.04.16.
 */
public class EventList {
    private static EventList mInstance = null;
    private static  Integer count = 0;
    public Map<Integer,Event> eventMap;


    protected EventList(){

        eventMap  = new HashMap<Integer, Event>();
    }

    public static synchronized EventList getmInstance(){
        if (null == mInstance){
            mInstance = new EventList();
        }
        return mInstance;
    }
    public static int getCount(){
        return count;
    }
    public void addToMap (Event e){
        count++;
        mInstance.eventMap.put(count, e);
    }

    public  static  void removeFromMap (Integer position){
        mInstance.eventMap.remove(position);
    }

}
