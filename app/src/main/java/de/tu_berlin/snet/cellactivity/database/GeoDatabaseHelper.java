package de.tu_berlin.snet.cellactivity.database;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellactivity.R;
import de.tu_berlin.snet.cellactivity.record.Call;
import de.tu_berlin.snet.cellactivity.record.Data;
import de.tu_berlin.snet.cellactivity.record.Handover;
import de.tu_berlin.snet.cellactivity.record.LocationUpdate;
import de.tu_berlin.snet.cellactivity.record.TextMessage;
import de.tu_berlin.snet.cellactivity.util.CellInfo;
import jsqlite.Database;
import jsqlite.Stmt;

/**
 * Created by Friedhelm Victor on 4/21/16.
 * This class should implement the Interface MobileNetworkDataWritable
 */
public class GeoDatabaseHelper implements MobileNetworkDataCapable {

    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = "/data/data/de.tu_berlin.snet.cellactivity/databases";
    private static String DB_NAME = "spatial.sqlite";
    private static final int GPS_SRID = 4326;
    private static final int SOURCE_DATA_SRID = 2277;
    private Context context;
    private  Database mDb;
    private static GeoDatabaseHelper sInstance;
    public static synchronized GeoDatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new GeoDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private GeoDatabaseHelper( Context context) {
        try {
            //File sdcardDir = ""; // your sdcard path
            File spatialDbFile = new File(DB_PATH, DB_NAME);

            mDb = new jsqlite.Database();
            mDb.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try{
            mDb.prepare("SELECT InitSpatialMetaData();").step();
        }catch (Exception e){}
       StringBuilder sb = new StringBuilder();
        String query1 = "CREATE TABLE Cells (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, cellid INT, lac INT, mnc INT, mcc INT);";
       sb.append("Execute query: ").append(query1).append("\n");
        try{
            Stmt stmt = mDb.prepare(query1);
            stmt.step();
            Log.i(TAG, "Table created ");
            // mDb.prepare("SELECT AddGeometryColumn('test_geom', 'the_geom', 4326, 'POINT', 'XY');").step();
        }catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }
        try{
            String query2 = "CREATE TABLE Measurements (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, location POINT, provider varchar(80), cell INT references Cells(id));";
        }
        catch (Exception e){}
        try{
            String query3 = "CREATE TABLE TextMsgs (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, direction varchar(80), address varchar(80), utc REAL, cell INT references Cells(id));";
        }catch (Exception e){}


        /*String query1= "\n" +
                "INSERT INTO test_geom\n" +
                "    (id, name, measured_value, the_geom)\n" +
                "  VALUES (NULL, 'first point', 1.23456,\n" +
                "    GeomFromText('POINT(1.01 2.02)', 4326));";

        try{
            Stmt stmt = mDb.prepare(query1);
            boolean istable = stmt.step();
            Log.i(TAG, "inserted : " + istable);


        }catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }*/

    }

    @Override
    public boolean insertRecord(Call call) {

        return false;
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {

        return false;
    }

    @Override
    public boolean insertRecord(Handover handover) {
        return false;
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        return false;
    }

    @Override
    public boolean insertRecord(Data data) {
        return false;
    }

    @Override
    public boolean insertMeasurement(CellInfo cellInfo) {
        return false;
    }

    @Override
    public void createTables() {

    }

    @Override
    public ArrayList<Call> getCallRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getCallRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Call> getTextMessageRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getTextMessageRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Call> getHandoverRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getHandoverRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Call> getLocationUpdateRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getLocationUpdateRecords(Date from, Date to) {
        return null;
    }

    @Override
    public ArrayList<Call> getDataRecords(Date day) {
        return null;
    }

    @Override
    public ArrayList<Call> getDataRecords(Date from, Date to) {
        return null;
    }
}
