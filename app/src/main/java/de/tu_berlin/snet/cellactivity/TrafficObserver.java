package de.tu_berlin.snet.cellactivity;


import android.net.TrafficStats;
import android.util.Log;

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
    //will store the RxTx Byte state when internet interface changes from mobile to any other
    private long mChangeIndicator = 0;
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
    public void removeListener(TrafficListener toRemove) { listeners.remove(toRemove); }


    private long getMobileRxAndTxBytes() {
        return mMobileRxAndTxBytes;
    }
    private long getChangeIndicator() {
        return mChangeIndicator;
    }

    private void setMobileRxAndTxBytes(long mMobileRxAndTxBytes) {
        this.mMobileRxAndTxBytes = mMobileRxAndTxBytes;
    }
    private void setChangeIndicator(long mlastBytesBeforeChange) {
        this.mChangeIndicator = mlastBytesBeforeChange;
    }

    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                long bytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();

                long bytesTransferred = bytes - getMobileRxAndTxBytes();
                /**
                 * TrafficStats internally maintains the traffic stats(Tx and Rx) for each internet interface (mobile and wifi) separately.
                 * Both records are monotonically increasing but incase the interface is turned off, the values of Tx and Rx are set to zero and the methods
                 * getMobileTxBytes() and getMobileRxBytes returns zero.
                 * However, upon turnin on the interface again, it reinstate the old value of Tx/Rx stats instead of starting from zero.
                 * mChangeIndicator variable is added to store the stats when the mobile interface shuts down.
                 * 0 value indicates normal mobile interface and negative value indicates the interface was turned off
                 *
                 */
                //mobile interface turned on->off
                if (bytesTransferred<0){
                    for (TrafficListener tl : listeners)
                        tl.bytesTransferred(0);
                        setChangeIndicator(bytesTransferred);
                    Log.d("Traffic", "Bytes transferred: " + bytesTransferred / 1000 + "\tTurning off Mobile Interface");
                }
                //mobile interface normal state
                if(bytesTransferred > 0 && getChangeIndicator()==0) {
                    for (TrafficListener tl : listeners)
                        tl.bytesTransferred(bytesTransferred);
                    Log.d("Traffic", "Bytes transferred: " + bytesTransferred / 1000 + "\tNormal");
                }
                //mobile interface tunred off->on
                if(bytesTransferred > 0 && getChangeIndicator()<0) {
                    for (TrafficListener tl : listeners)
                        tl.bytesTransferred(bytesTransferred+getChangeIndicator());
                    Log.d("Traffic", "Bytes transferred: " + (bytesTransferred + getChangeIndicator())/ 1000 + "\tTurning on MObile Interface");
                    setChangeIndicator(0);
                }
                setMobileRxAndTxBytes(bytes);
            }

        }, 1, 1000);
    }

    public void stop() {
        timer.cancel();
    }
}
