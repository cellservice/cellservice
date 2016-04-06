package de.tu_berlin.snet.cellactivity;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import de.tu_berlin.snet.cellactivity.util.Event;

/**
 * Created by ashish on 30.03.16.
 */

    public class AsyncEventResponse extends AsyncTask<Void, Void, Boolean> implements LocationListener {
    public interface LocationTaskListener {
         void onAllLocationReceived(Location netLocation, Location gpsLocation);
    }
    private final LocationTaskListener taskListener;


        private Event event;
        private Context context = null;
        final long ONE_MINUTES = 1 * 60 * 1000;
        final long FRESHNESS_TIME = 10 * 1000; //10 seconds
        final long THD_SLEEP = 100; //100 miliseconds
        private Location Netlocation;
        private Location GPSlocation;
        private LocationManager lm;

        public AsyncEventResponse(LocationTaskListener taskListener, Context context, Event event) {

            this.context = context;
            this.taskListener = taskListener;
            this.event = event;

        }

        protected void onPreExecute() {
            //initialise
            Log.d("Async: PreExecute", "PreExecute for " + event.type);
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, this);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Try to use the last known position
            try {

                Location lastLocationNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Location lastLocationGPS = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.d("Async: background", "Last known location from GPS was: " + lastLocationGPS.toString());
                Log.d("Async: background", "Last known location from Network was: " + lastLocationNet.toString());
                //no last location availabe
                if (lastLocationNet == null || lastLocationGPS == null) {
                    long startTime = System.currentTimeMillis(); //fetch starting time
                    while ((Netlocation == null || GPSlocation == null) && (System.currentTimeMillis() - startTime < ONE_MINUTES)) {
                        try {
                            Log.d("Async: background", "Waiting for new location " + this.getStatus());
                            Thread.sleep(THD_SLEEP);
                        } catch (Exception ex) {
                            //handle here ?
                        }
                    }
                    return true;
                }
                // If it's too old, get a new one by location manager
                if (System.currentTimeMillis() - lastLocationGPS.getTime() > FRESHNESS_TIME || System.currentTimeMillis() - lastLocationNet.getTime() > 10000) {
                    long startTime = System.currentTimeMillis(); //fetch starting time
                    while ((Netlocation == null || GPSlocation == null) && (System.currentTimeMillis() - startTime < ONE_MINUTES)) {
                        try {
                            Log.d("Async: background", "Waiting for new location  " + this.getStatus());
                            Thread.sleep(THD_SLEEP);
                        } catch (Exception ex) {
                            //handle here ?
                        }
                    }
                    return true;
                }
                Log.d("Async: Background", "Known location is fresh");
                return true;


            } catch (Exception e) {
                Log.d("Async: background", e.toString());
            }
            return true;
        }


        protected void onPostExecute(Boolean isDone) {
            // dismiss progress dialog and update ui
            try {
                Log.d("Async: PostExecute", this.GPSlocation.toString());
            } catch (Exception e) {
                Log.d("Async: PostExecute", "GPS null " + this.getStatus());
            }
            try {
                Log.d("Async: PostExecute", this.Netlocation.toString());
            } catch (Exception e) {
                Log.d("Async: PostExecute", "Netlocation null " + this.getStatus());
            }
            //deregister location manager
            lm.removeUpdates(this);

            if(this.taskListener != null) {
                this.taskListener.onAllLocationReceived(Netlocation, GPSlocation);
            }


        }

        public void onLocationChanged(Location newLocation) {

            Log.d("Async: LocationChanged", "new location from " + newLocation.getProvider() + " available: " + newLocation.toString());
            if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                this.Netlocation = newLocation;
                //deregister the listener here
            }

            if (newLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                this.GPSlocation = newLocation;
                //deregister the listener here
            }

        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

