package de.tu_berlin.snet.cellactivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;


public class TimeLineFragment extends Fragment {

    Activity context;

    public static TimeLineFragment newInstance(String date) {
        TimeLineFragment aFragment = new TimeLineFragment();

        Bundle args = new Bundle();
        args.putString("date", date);
        aFragment.setArguments(args);

        return aFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getActivity();
        return inflater.inflate(R.layout.timeline_container, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rebuildDataTable();
    }

    private void rebuildDataTable() {
        // get all the database entries
        DatabaseHelper myDb = DatabaseHelper.getInstance(context);
        String[][] databaseEntries = myDb.getAllDataAsArrayOnDate(getArguments().getString("date"));

        // get the timeline container and inflater to add event items to it
        LinearLayout eventListLayout = (LinearLayout) getView().findViewById(R.id.eventList);
        LayoutInflater li = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int durationHeight = 100; // default duration height

        // variables for sigmoid based function determining time interval drawing length
        double a = 1.902, b = 2.024, c = 13618, d = 1408;

        for (int i = 0; i < databaseEntries.length; i++) {
            View eventView = li.inflate(R.layout.timeline_event, (ViewGroup) null);
            TextView eventText = (TextView) eventView.findViewById(R.id.eventText);
            TextView eventTime = (TextView) eventView.findViewById(R.id.eventTime);
            View eventTimeBar = (View) eventView.findViewById(R.id.eventTimeBar);

            // extract color from string that contains cellid + lac and set it as the background
            String cellidLAC = databaseEntries[i][3] + "/" + databaseEntries[i][4];
            String opacity = "#FF";
            String hexColor = String.format(
                    opacity + "%06X", (0xFFFFFF & cellidLAC.hashCode()));
            eventTimeBar.setBackgroundColor(Color.parseColor(hexColor));

            // Display the date as only hours and minutes
            DateFormat df = new SimpleDateFormat("HH:mm");
            eventTime.setText(df.format(new java.util.Date(Long.parseLong(databaseEntries[i][1]) * 1000)));

            String eventTextBuilder = databaseEntries[i][2] + " ( "+databaseEntries[i][3] + " / " +
                                      databaseEntries[i][4] + " / " + databaseEntries[i][7] + ")";

            if(databaseEntries[i][8] != null) {
                eventTextBuilder += " " + (Integer.parseInt(databaseEntries[i][8])/1000f) + " Kb";
            }

            eventText.setText(eventTextBuilder);

            // Set the length of the time intervals with a sigmoid based function
            if (i != 0) {
                double secondDiff = Double.parseDouble(databaseEntries[i][1]) - Double.parseDouble(databaseEntries[i - 1][1]);
                durationHeight = (int) (100 + d + ((a - d) / (1 + Math.pow((secondDiff / c), b))));
            }
            eventListLayout.addView(eventView, ViewGroup.LayoutParams.WRAP_CONTENT, durationHeight);
        }

    }
}