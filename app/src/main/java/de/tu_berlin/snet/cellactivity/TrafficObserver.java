package de.tu_berlin.snet.cellactivity;


import android.net.TrafficStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


// An interface to be implemented by event listeners
interface TrafficListener {
    void bytesTransferred(long kbytes);
}

public class TrafficObserver {

    private List<TrafficListener> listeners = new ArrayList<TrafficListener>();
    private long mMobileRxAndTxBytes = TrafficStats.getMobileRxBytes() +
            TrafficStats.getMobileTxBytes();
    private Timer timer;

    private TrafficObserver() {
    }

    static private TrafficObserver instance;

    synchronized public static TrafficObserver getInstance() {
        if (null == instance) {
            instance = new TrafficObserver();
        }
        return instance;
    }

    public void addListener(TrafficListener toAdd) {
        listeners.add(toAdd);
    }


    private long getMobileRxAndTxBytes() {
        return mMobileRxAndTxBytes;
    }

    private void setMobileRxAndTxBytes(long mMobileRxAndTxBytes) {
        this.mMobileRxAndTxBytes = mMobileRxAndTxBytes;
    }

    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                long bytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();

                long bytesTransferred = bytes - getMobileRxAndTxBytes();
                if(bytesTransferred != 0) {
                    for (TrafficListener tl : listeners)
                        tl.bytesTransferred(bytesTransferred);
                }
                setMobileRxAndTxBytes(bytes);
            }

        }, 1, 1000);
    }

    public void stop() {
        timer.cancel();
    }
}
