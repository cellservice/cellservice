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

    public class AsyncEventResponseLP extends AsyncTask<Void, Void, Boolean> implements LocationListener {
    public interface NetLocationTaskListener {
         void onNetLocationReceived(Location netLocation, int key);
    }
    private final NetLocationTaskListener taskListener;


        private Event event;
        private Context context = null;
        final long ONE_MINUTES = 1 * 60 * 1000;
        final long FRESHNESS_TIME = 10 * 1000; //10 seconds
        final long THD_SLEEP = 500; //in miliseconds
        final  long LP_MIN_TIME = 100; // as Network location provider can be very fast, 100 ms should be enough
        private Location Netlocation;
         private LocationManager lm;
         private int eventKey;


        public AsyncEventResponseLP(NetLocationTaskListener taskListener, Context context, Event event, int key) {

            this.context = context;
            this.taskListener = taskListener;
            this.event = event;
            this.eventKey = key;

        }

        protected void onPreExecute() {
            //initialise
            Log.d("Async: PreExecute", "PreExecute Netlocation for " + event.type);
            lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LP_MIN_TIME, 0, this);


        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Try to use the last known position
            try {

                Location lastLocationNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                Log.d("Async: background", "Last known location from Network was: " + lastLocationNet.toString());
                //no last location availabe
                if (lastLocationNet == null ) {
                    long startTime = System.currentTimeMillis(); //fetch starting time
                    while ((Netlocation == null) && (System.currentTimeMillis() - startTime < ONE_MINUTES)) {
                        try {
                            Log.d("Async: background", "Waiting for new Netlocation " + this.getStatus());
                            Thread.sleep(THD_SLEEP);
                        } catch (Exception ex) {
                            //handle here ?
                        }
                    }
                    return true;
                }
                // If it's too old, get a new one by location manager
                if ( System.currentTimeMillis() - lastLocationNet.getTime() > FRESHNESS_TIME) {
                    long startTime = System.currentTimeMillis(); //fetch starting time
                    while ((Netlocation == null) && (System.currentTimeMillis() - startTime < ONE_MINUTES)) {
                        try {
                            Log.d("Async: background", "Waiting for new Netlocation  " + this.getStatus());
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
                Log.d("Async: PostExecute", this.Netlocation.toString());
            } catch (Exception e) {
                Log.d("Async: PostExecute", "Netlocation null " + this.getStatus());
            }
            if(this.taskListener != null) {
                this.taskListener.onNetLocationReceived(Netlocation, eventKey);
            }


        }

        public void onLocationChanged(Location newLocation) {

            Log.d("Async: LocationChanged", "new location from " + newLocation.getProvider() + " available: " + newLocation.toString());
            if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                this.Netlocation = newLocation;
                //deregister the listener here
                lm.removeUpdates(this);
            }


        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

