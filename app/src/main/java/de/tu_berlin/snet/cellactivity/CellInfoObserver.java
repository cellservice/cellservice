package de.tu_berlin.snet.cellactivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.tu_berlin.snet.cellactivity.util.CellInfo;

/**
 * Created by Friedhelm Victor on 3/29/16.
 * Observes the mobile network cell the device is / was connected to.
 * It keeps a reference to these variables. It also provides an Interface that can be used to
 * listen to Location Updates (triggered when the LAC between Cells change)
 */

// An interface to be implemented by event listeners
interface CellInfoListener {
    void onLocationUpdate(CellInfo oldCell, CellInfo newCell);
}

public class CellInfoObserver {
    private static CellInfoObserver instance;

    private List<CellInfoListener> listeners = new ArrayList<CellInfoListener>();

    private CellInfo mPreviousCellInfo, mCurrentCellInfo;

    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

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
    }

    public void onStart() {
        initializeOrRestoreCellInfos();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
    }

    public void onStop() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private void initializeOrRestoreCellInfos() {
        // Restore preferences
        SharedPreferences settings = CellService.get().getSharedPreferences(CellService.SHARED_PREFERENCES, 0);
        // retrieve the last timestamp. Default value is 2016-01-01 01:01:01 CET
        long lastTimestamp = settings.getLong("CellInfoObserverLastTimestamp", 1451606461000L);
        // if there was a last timestamp in the previous 5 minutes
        long fiveMinsAgo = System.currentTimeMillis() - (5 * 60 * 1000);

        if(fiveMinsAgo < lastTimestamp) {
            Log.e("PERSISTENCE", "retrieving cellinfos");
            setPreviousCellInfo(new CellInfo(
                    settings.getInt("mPreviousCellInfo.CellId", -1),
                    settings.getInt("mPreviousCellInfo.LAC", -1),
                    settings.getInt("mPreviousCellInfo.MNC", -1),
                    settings.getInt("mPreviousCellInfo.MCC", -1),
                    settings.getInt("mPreviousCellInfo.type", -1)));

            setCurrentCellInfo(new CellInfo(
                    settings.getInt("mCurrentCellInfo.CellId", -1),
                    settings.getInt("mCurrentCellInfo.LAC", -1),
                    settings.getInt("mCurrentCellInfo.MNC", -1),
                    settings.getInt("mCurrentCellInfo.MCC", -1),
                    settings.getInt("mCurrentCellInfo.type", -1)));
        } // otherwise: create new objects
        else {
            setPreviousCellInfo(new CellInfo());
            setCurrentCellInfo(getNewCellInfo());
        }
    }

    private void persistCellInfosToPreferences() {
        Log.e("PERSISTENCE", "saving cellinfos");
        SharedPreferences settings = CellService.get().getSharedPreferences(CellService.SHARED_PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("mPreviousCellInfo.CellId", mPreviousCellInfo.getCellId());
        editor.putInt("mPreviousCellInfo.LAC", mPreviousCellInfo.getLac());
        editor.putInt("mPreviousCellInfo.MNC", mPreviousCellInfo.getMnc());
        editor.putInt("mPreviousCellInfo.MCC", mPreviousCellInfo.getMcc());
        editor.putInt("mPreviousCellInfo.type", mPreviousCellInfo.getConnectionType());

        editor.putInt("mCurrentCellInfo.CellId", mCurrentCellInfo.getCellId());
        editor.putInt("mCurrentCellInfo.LAC", mCurrentCellInfo.getLac());
        editor.putInt("mCurrentCellInfo.MNC", mCurrentCellInfo.getMnc());
        editor.putInt("mCurrentCellInfo.MCC", mCurrentCellInfo.getMcc());
        editor.putInt("mCurrentCellInfo.type", mCurrentCellInfo.getConnectionType());

        editor.putLong("CellInfoObserverLastTimestamp", System.currentTimeMillis());

        editor.apply();
    }

    public void addListener(CellInfoListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(CellInfoListener toRemove) {
        listeners.remove(toRemove);
    }

    private void setPreviousCellInfo(CellInfo cellInfo) { mPreviousCellInfo = cellInfo; }
    public CellInfo getPreviousCellInfo() {
        return mPreviousCellInfo;
    }

    private boolean isLocationUpdate(CellInfo oldCellInfo, CellInfo newCellInfo) {
        return oldCellInfo.getLac() != -1 &&
                newCellInfo.getLac() != -1 &&
                oldCellInfo.getLac() != newCellInfo.getLac();
    }

    private void setCurrentCellInfo(CellInfo cellInfo) { mCurrentCellInfo = cellInfo; }
    public CellInfo getCurrentCellInfo() { return mCurrentCellInfo; }

    public CellInfo getNewCellInfo() {
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
                CellInfo newCellInfo = getNewCellInfo();

                // if the cell changed, adjust current and previous values and persist
                if (!getCurrentCellInfo().equals(newCellInfo)) {
                    setPreviousCellInfo(getCurrentCellInfo());
                    setCurrentCellInfo(newCellInfo);
                    persistCellInfosToPreferences();
                }

                if(isLocationUpdate(getPreviousCellInfo(), newCellInfo)) {
                    for (CellInfoListener cil : listeners)
                        cil.onLocationUpdate(getPreviousCellInfo(), newCellInfo);
                }

            }
        };
    }
}
