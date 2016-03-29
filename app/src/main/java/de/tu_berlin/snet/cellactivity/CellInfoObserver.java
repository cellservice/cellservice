package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;

import java.util.ArrayList;
import java.util.List;

import de.tu_berlin.snet.cellactivity.util.CellInfo;

/**
 * Created by giraffe on 3/29/16.
 */

// An interface to be implemented by event listeners
interface CellInfoListener {
    void onLocationUpdate(CellInfo oldCell, CellInfo newCell);
}

public class CellInfoObserver {
    private static CellInfoObserver instance;

    // See http://stackoverflow.com/questions/14057273/android-singleton-with-global-context/14057777#14057777
    // for reference / double check synchronization
    public static CellInfoObserver getInstance() {
        if (instance == null) instance = getInstanceSync();
        return instance;
    }

    private static synchronized CellInfoObserver getInstanceSync() {
        if (instance == null) instance = new CellInfoObserver();
        return instance;
    }

    private CellInfoObserver() {
        mTelephonyManager = (TelephonyManager) CellService.get().getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = setupPhoneStateListener();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    private List<CellInfoListener> listeners = new ArrayList<CellInfoListener>();

    private CellInfo mPreviousCellInfo = new CellInfo();
    private CellInfo mCurrentCellInfo = getCurrentCellInfo();

    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;


    public void addListener(CellInfoListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(CellInfoListener toRemove) {
        listeners.remove(toRemove);
    }

    public CellInfo getPreviousCellInfo() {
        return mPreviousCellInfo;
    }

    private boolean isLocationUpdate(CellInfo oldCellInfo, CellInfo newCellInfo) {
        return oldCellInfo.getLac() != -1 &&
                newCellInfo.getLac() != -1 &&
                oldCellInfo.getLac() != newCellInfo.getLac();
    }

    public CellInfo getCurrentCellInfo() {
        try {
            TelephonyManager tm = (TelephonyManager) CellService.get().getSystemService(Context.TELEPHONY_SERVICE);
            GsmCellLocation location = (GsmCellLocation) tm.getCellLocation();

            /* Why I use this Bitmask:
             * https://stackoverflow.com/questions/9808396/android-cellid-not-available-on-all-carriers#12969638
             */
            int cellID = location.getCid();// & 0xffff;
            int lac = location.getLac();

            String networkOperator = tm.getNetworkOperator();
            int mcc = Integer.parseInt(networkOperator.substring(0, 3));
            int mnc = Integer.parseInt(networkOperator.substring(3));

            return new CellInfo(cellID, lac, mnc, mcc, tm.getNetworkType());
        } catch (Exception e) {
            return new CellInfo();
        }
    }

    public PhoneStateListener setupPhoneStateListener() {
        return new PhoneStateListener() {
            /**
             * Callback invoked when device cell id / location changes.
             */
            @SuppressLint("NewApi")
            public void onCellLocationChanged(CellLocation location) {
                CellInfo newCellInfo = getCurrentCellInfo();

                // if the cell changed, adjust current and previous values
                if (!mCurrentCellInfo.equals(newCellInfo)) {
                    mPreviousCellInfo = mCurrentCellInfo;
                    mCurrentCellInfo = newCellInfo;
                }

                if(isLocationUpdate(getPreviousCellInfo(), newCellInfo)) {
                    for (CellInfoListener cil : listeners)
                        cil.onLocationUpdate(getPreviousCellInfo(), newCellInfo);
                }

            }
        };
    }
}
