package de.tu_berlin.snet.cellactivity;


import android.net.TrafficStats;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

// An interface to be implemented by event listeners
interface TrafficListener {
    void bytesRxTransferred(long kbytes);
    void bytesTxTransferred(long kbytes);
}

public class TrafficObserver {

    private List<TrafficListener> listeners = new ArrayList<TrafficListener>();
    private long mMobileRxAndTxBytes = TrafficStats.getMobileRxBytes() +
            TrafficStats.getMobileTxBytes();
    private long mMobileRxBytes = TrafficStats.getMobileRxBytes();
    private long mMobileTxBytes = TrafficStats.getMobileTxBytes();
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
    private void setMobileRxAndTxBytes(long mMobileRxAndTxBytes) {
        this.mMobileRxAndTxBytes = mMobileRxAndTxBytes;
    }
    private long getChangeIndicator() {
        return mChangeIndicator;
    }
    private void setChangeIndicator(long mlastBytesBeforeChange) {
        this.mChangeIndicator = mlastBytesBeforeChange;
    }
    private long getMobileRxBytes() {
        return mMobileRxBytes;
    }
    private void setMobileRxBytes(long mMobileRxBytes) {
        this.mMobileRxBytes = mMobileRxBytes;
    }

    private long getMobileTxBytes() {
        return mMobileTxBytes;
    }
    private void setMobileTxBytes(long mMobileTxBytes) {
        this.mMobileTxBytes = mMobileTxBytes;
    }


    public void start() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                long bytes = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes();
                long bytesRx = TrafficStats.getMobileRxBytes();
                long bytesTx = TrafficStats.getMobileTxBytes();
               // For Rx
                if ( bytesRx>0 && getMobileRxBytes()>0 ){
                   Log.d("Traffic", "Bytes Received: " + (bytesRx - getMobileRxBytes()) + "\tNormal");
                    if ((bytesRx-getMobileRxBytes())>0)
                    for (TrafficListener tl : listeners)
                        tl.bytesRxTransferred(bytesRx-getMobileRxBytes());
               }
               //else case added for debugging purpose. It can be removed with no change in the functionality
               else if ( bytesRx==0 && getMobileRxBytes()!=0){
                   //turning of of the mobile interface detected
                   //set the indicator for change in the interface state. It will be later used to detect turnin on of the interface
                   //normally or due to a restart of the service.
                    Log.d("Traffic", "Switching OFF the mobile interface");
               }
                //else case added for debugging purpose. It can be removed with no change in the functionality
                else if ( bytesRx!=0 && getMobileRxBytes()==0) {
                    //turning on of the mobile interface was detected.
                    Log.d("Traffic", "Switching ON the mobile interface");
               }
               //for Tx
                if ( bytesTx!=0 && getMobileTxBytes()!=0 ){
                    Log.d("Traffic", "Bytes Transmitted: " + (bytesTx-getMobileTxBytes())  + "\tNormal");
                   if ((bytesTx-getMobileTxBytes())>0)
                    for (TrafficListener tl : listeners)
                        tl.bytesTxTransferred(bytesTx-getMobileTxBytes());
                }
                //else case added for debugging purpose. It can be removed with no change in the functionality
                else if ( bytesTx==0 && getMobileTxBytes()!=0){
                    //turning of of the mobile interface detected
                    //set the indicator for change in the interface state. It will be later used to detect turnin on of the interface
                    //normally or due to a restart of the service.
                    Log.d("Traffic", "Switching OFF the mobile interface");
                }
                //else case added for debugging purpose. It can be removed with no change in the functionality
                else if ( bytesTx!=0 && getMobileTxBytes()==0) {
                    //turning on of the mobile interface was detected.
                    Log.d("Traffic", "Switching ON the mobile interface");

                }
                /**
                 * TrafficStats internally maintains the traffic stats(Tx and Rx) for each internet interface (mobile and wifi) separately.
                 * Both records are monotonically increasing but incase the interface is turned off, the values of Tx and Rx are set to zero and the methods
                 * getMobileTxBytes() and getMobileRxBytes returns zero.
                 * However, upon turnin on the interface again, it reinstate the old value of Tx/Rx stats instead of starting from zero.
                 * mChangeIndicator variable is added to store the stats when the mobile interface shuts down.
                 * 0 value indicates normal mobile interface and negative value indicates the interface was turned off
                 *
                 */
                setMobileRxAndTxBytes(bytes);
                setMobileRxBytes(bytesRx);
                setMobileTxBytes(bytesTx);
            }

        }, 1, 1000);
    }

    public void stop() {
        timer.cancel();
    }
}
