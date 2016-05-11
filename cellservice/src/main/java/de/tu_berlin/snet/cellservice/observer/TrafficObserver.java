package de.tu_berlin.snet.cellservice.observer;

import android.net.TrafficStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TrafficObserver implements Observer {
    public interface TrafficListener {
        void onBytesTransferred(long rxBytes, long txBytes, long timestamp);
    }

    private List<TrafficListener> listeners = new ArrayList<TrafficListener>();
    private long mCurrentMobileRxBytes = TrafficStats.getMobileRxBytes();
    private long mCurrentMobileTxBytes = TrafficStats.getMobileTxBytes();
    private Timer mTimer;

    private TrafficObserver() {
    }

    static private TrafficObserver instance;

    synchronized public static TrafficObserver getInstance() {
        if (null == instance) {
            instance = new TrafficObserver();
        }
        return instance;
    }

    @Override
    public void start() {
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                long newRxBytes = TrafficStats.getMobileRxBytes();
                long newTxBytes = TrafficStats.getMobileTxBytes();

                /**
                 * TrafficStats internally maintains the traffic stats(Tx and Rx) for each internet
                 * interface (mobile and wifi) separately.
                 * Both records are monotonically increasing but in case the interface is turned
                 * off, the values of Tx and Rx are set to zero and the methods
                 * TrafficStats.getMobileTxBytes() and TrafficStats.getMobileRxBytes return zero.
                 * However, upon turning on the interface again, the old values of Tx/Rx stats are
                 * reinstated instead of starting from zero
                 */

                // TODO: REFACTOR THIS UGLY CONDITION
                if ((newRxBytes > 0 && getCurrentMobileRxBytes() > 0 && ((newRxBytes - getCurrentMobileRxBytes()) > 0)) ||
                        (newTxBytes > 0 && getCurrentMobileTxBytes() > 0 && ((newTxBytes - getCurrentMobileTxBytes()) > 0))) {
                    for (TrafficListener tl : listeners) {
                        tl.onBytesTransferred(newRxBytes - getCurrentMobileRxBytes(), newTxBytes - getCurrentMobileTxBytes(), System.currentTimeMillis() / 1000);
                    }
                }
                setCurrentMobileRxBytes(newRxBytes);
                setCurrentMobileTxBytes(newTxBytes);
            }

        }, 1, 1000);
    }

    @Override
    public void stop() {
        mTimer.cancel();
    }

    public void addListener(TrafficListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(TrafficListener toRemove) {
        listeners.remove(toRemove);
    }

    private long getCurrentMobileRxBytes() {
        return mCurrentMobileRxBytes;
    }

    private void setCurrentMobileRxBytes(long mMobileRxBytes) {
        this.mCurrentMobileRxBytes = mMobileRxBytes;
    }

    private long getCurrentMobileTxBytes() {
        return mCurrentMobileTxBytes;
    }

    private void setCurrentMobileTxBytes(long mMobileTxBytes) {
        this.mCurrentMobileTxBytes = mMobileTxBytes;
    }
}
