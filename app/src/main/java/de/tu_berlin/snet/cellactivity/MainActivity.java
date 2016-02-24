package de.tu_berlin.snet.cellactivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        rebuildDataTable();

        Button checkServiceStateButton = (Button) findViewById(R.id.checkServiceState);
        checkServiceStateButton.performClick();
    }

    private void rebuildDataTable() {
        // get all the database entries
        DatabaseHelper myDb = DatabaseHelper.getInstance(this);
        String[][] databaseEntries = myDb.getAllDataAsArray();

        // get the timeline container and inflater to add event items to it
        LinearLayout eventListLayout = (LinearLayout) findViewById(R.id.eventList);
        LayoutInflater li =  (LayoutInflater)getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int durationHeight = 100; // default duration height

        // variables for sigmoid based function determining time interval drawing length
        double a = 1.902, b = 2.024, c = 13618, d = 1408;

        for (int i = 0; i <databaseEntries.length; i++){
            View eventView = li.inflate(R.layout.event_slot, (ViewGroup)null);
            TextView eventText = (TextView) eventView.findViewById(R.id.eventText);
            TextView eventTime = (TextView) eventView.findViewById(R.id.eventTime);
            View eventTimeBar = (View) eventView.findViewById(R.id.eventTimeBar);

            // extract color from string that contains cellid + lac and set it as the background
            String cellidLAC = databaseEntries[i][2].split("kbytes")[0];
            String opacity = "#FF";
            String hexColor = String.format(
                    opacity + "%06X", (0xFFFFFF & cellidLAC.hashCode()));
            eventTimeBar.setBackgroundColor(Color.parseColor(hexColor));

            // Display the date as only hours and minutes
            DateFormat df = new SimpleDateFormat("HH:mm");
            eventTime.setText(df.format(new java.util.Date(Long.parseLong(databaseEntries[i][1]) * 1000)));
            eventText.setText(databaseEntries[i][2]);

            // Set the length of the time intervals with a sigmoid based function
            if( i != 0) {
                double secondDiff = Double.parseDouble(databaseEntries[i][1]) - Double.parseDouble(databaseEntries[i-1][1]);
                durationHeight = (int)(100+d + ((a-d)/(1+Math.pow((secondDiff/c),b))));
            }
            eventListLayout.addView(eventView, ViewGroup.LayoutParams.WRAP_CONTENT, durationHeight);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // method to determine whether the service is running
    // See http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-on-android
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void updateCellServiceState(View v) {
        // check whether CellService is running and display in MainActivity
        TextView serviceIsRunningValueTextView = (TextView)findViewById(R.id.serviceIsRunningValue);
        serviceIsRunningValueTextView.setText(
                Boolean.toString(isMyServiceRunning(CellService.class))
        );
    }


    // my own onclick methods
    public void startService(View v) {
        Intent i = new Intent(this, CellService.class);
        // if you want to pass data to the service
        i.putExtra("key", "some value");
        startService(i);
    }

    public void stopService(View v) {
        Intent i = new Intent(this, CellService.class);
        stopService(i);
        // alternative: stopSelf();
    }

    public void addData(View v) {
        DatabaseHelper myDb = DatabaseHelper.getInstance(this);
        myDb.insertData(System.currentTimeMillis() / 1000, "started");
        rebuildDataTable();
    }
}
