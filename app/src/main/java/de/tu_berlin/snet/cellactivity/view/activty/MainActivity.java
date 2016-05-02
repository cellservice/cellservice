package de.tu_berlin.snet.cellactivity.view.activty;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.tu_berlin.snet.cellactivity.CellService;
import de.tu_berlin.snet.cellactivity.R;
import de.tu_berlin.snet.cellactivity.view.fragment.TabFragment;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    @Bind(R.id.drawer_layout)
    DrawerLayout drawerLayout;

    @Bind(R.id.drawer_navigation)
    NavigationView drawerNavigationView;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.container_view, new TabFragment()).commit();

        drawerNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);

        drawerLayout.addDrawerListener(drawerToggle);

        drawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_service:
                startCellService();
                break;
            case R.id.action_stop_service:
                stopCellService();
                break;
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

    /*
    public void updateCellServiceState(View v) {
        // check whether CellService is running and display in MainActivity
        TextView serviceIsRunningValueTextView = (TextView)findViewById(R.id.serviceIsRunningValue);
        serviceIsRunningValueTextView.setText(
                Boolean.toString(isMyServiceRunning(CellService.class))
        );
    }
    */

    private void startCellService() {
        Intent i = new Intent(this, CellService.class);
        // if you want to pass data to the service
        i.putExtra("key", "some value");
        startService(i);
    }

    private void stopCellService() {
        Intent i = new Intent(this, CellService.class);
        stopService(i);
        // alternative: stopSelf();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        drawerLayout.closeDrawers();
                /* for each drawer item

                if (menuItem.getItemId() == R.id.nav_item) {
                    FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.containerView, new TimeLineFragment()).commit();

                } */

        return false;
    }
}
