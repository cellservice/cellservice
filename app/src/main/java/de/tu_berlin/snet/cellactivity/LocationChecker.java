package de.tu_berlin.snet.cellactivity;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by ashish on 02.02.16.
 * This class is created to manage all the loation related activity in one class.
 */
public class LocationChecker {

    private static final String TAG = "T-LAB-LocationChecker";
    LocationManager mLocationManager;

    public LocationChecker(LocationManager mLocationManager) {
        this.mLocationManager = mLocationManager;
    }
    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    public Location getlocationNETWORK() {

        LocationListener locationListenerNET = new LocationListener(LocationManager.NETWORK_PROVIDER);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0,locationListenerNET);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update network, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        Location temp_loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        //Log.e(TAG, "Location received from: " + temp_loc.getProvider());
        mLocationManager.removeUpdates(locationListenerNET);
        return temp_loc;
    }

    public Location getlocationGPS(){
        LocationListener locationListenerGPS = new LocationListener(LocationManager.GPS_PROVIDER);
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,locationListenerGPS);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update gps, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

        Location temp_loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        // Log.e(TAG, "Location received from: " + temp_loc.getProvider());
        mLocationManager.removeUpdates(locationListenerGPS);
        return temp_loc;
    }
}
