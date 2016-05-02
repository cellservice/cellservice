package de.tu_berlin.snet.cellactivity;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import de.tu_berlin.snet.cellactivity.model.database.GeoDatabaseHelper;
import de.tu_berlin.snet.cellactivity.model.record.Call;
import de.tu_berlin.snet.cellactivity.model.record.Data;
import de.tu_berlin.snet.cellactivity.model.record.LocationUpdate;
import de.tu_berlin.snet.cellactivity.model.record.TextMessage;

public class CellService extends Service {

    public static final String SHARED_PREFERENCES = "CellServiceSharedPreferences";
    private static Service instance;
    MobileNetworkHelper mobileNetworkHelper;
    CDRReceiver cdrReceiver = new CDRReceiver();
    GeoDatabaseHelper geoDatabaseHelper;

    public CellService() {
    }

    // Allows others to retrieve a reference to this service, to get the context
    public static Service get() { return instance; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mobileNetworkHelper = MobileNetworkHelper.getInstance(this);
        geoDatabaseHelper = GeoDatabaseHelper.getInstance(this);
        Toast.makeText(this, "Service is created", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service is started", Toast.LENGTH_LONG).show();
        // catching data if it has been sent
        try {
            String message;
            message = intent.getStringExtra("key");
            Toast.makeText(this, "data sent to service was \"" + message + "\"", Toast.LENGTH_LONG).show();
            // otherwise catch exception
        } catch (Exception e) {
            Toast.makeText(this, "didn't receive any data, but caught exception "+e.toString(), Toast.LENGTH_LONG).show();
        }

        mobileNetworkHelper.addListener(cdrReceiver);
        mobileNetworkHelper.onStart();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mobileNetworkHelper.onStop();
        mobileNetworkHelper.removeListener(cdrReceiver);
        Toast.makeText(this, "Service is stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private final class CDRReceiver implements CDRListener {
        @Override
        public void onDataSession(Data data) {
            Log.e("CDRReceiver", "received data: " + data);
            geoDatabaseHelper.insertRecord(data);
        }

        @Override
        public void onCallRecord(Call call) {
            Log.e("CDRReceiver", "received call: " + call);
            geoDatabaseHelper.insertRecord(call);
        }

        @Override
        public void onTextMessage(TextMessage textMessage) {
            Log.e("CDRReceiver", "received text: " + textMessage);
            geoDatabaseHelper.insertRecord(textMessage);
        }

        @Override
        public void onLocationUpdate(LocationUpdate locationUpdate) {
            Log.e("CDRReceiver", "received location update: " + locationUpdate);
            geoDatabaseHelper.insertRecord(locationUpdate);
        }
    }

}
