package de.tu_berlin.snet.cellactivity;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by giraffe on 4/15/16.
 */
public class LocationFuture implements Callable<Location>, LocationListener {

    private LocationManager locationManager;
    private String provider;
    // minimum time interval between location updates, in milliseconds
    // 6 seconds is the fastest time recommended by google for better battery usage.
    final long MINIMUM_INTERVAL_TIME = 6 * 1000;
    final long FRESHNESS_TIME = 120 * 1000; // 2 minutes
    private Location location = null;
    CountDownLatch freshLocationReady = new CountDownLatch(1);

    /**
     * @param provider name of a provider as defined in Android's LocationManager.
     *                 Obtainable through LocationManager.NETWORK_PROVIDER
     */
    public LocationFuture(String provider) {
        this.provider = provider;
        locationManager = (LocationManager) CellService.get().getSystemService(Context.LOCATION_SERVICE);
        /* Note: to avoid exception:
         * Can't create Handler inside thread that has not called Looper.prepare()
         * Use solution suggestion: http://stackoverflow.com/a/29691963 (Adding Looper.getMainLooper()) */
        locationManager.requestLocationUpdates(provider, MINIMUM_INTERVAL_TIME, 0, this, Looper.getMainLooper());
    }

    @Override
    public Location call() {

        try {
            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
            Log.d("Async: background", "Last known location from " + provider + " was: " + lastKnownLocation.toString());

            // If the location's age is sufficiently fresh, return it
            if (System.currentTimeMillis() - lastKnownLocation.getTime() < FRESHNESS_TIME) {
                Log.d("Async: Background", "Known " + provider + " location is fresh");
                locationManager.removeUpdates(this);
                return lastKnownLocation;
            } else {
                // wait for the done signal be triggered by the onLocationChanged method
                // If it doesn't happen after 2 minutes, the original value (null) will be returned
                freshLocationReady.await(2, TimeUnit.MINUTES);
                Log.d("Async: Background", "Returning fresh "+provider+" location");
                locationManager.removeUpdates(this);
                return location;
            }
        } catch (Exception e) {
            locationManager.removeUpdates(this);
            return null;
        }
    }


    @Override
    public void onLocationChanged(Location newLocation) {
        Log.d("Async: LocationChanged", "new location from " + newLocation.getProvider() + " available: " + newLocation.toString());
        if (newLocation.getProvider().equals(this.provider)) {
            this.location = newLocation;
            //deregister the listener here
            locationManager.removeUpdates(this);
            freshLocationReady.countDown();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Async: Background", "onProviderDisabled");
    }
}
