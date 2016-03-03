package de.tu_berlin.snet.cellactivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;


public class DatabaseHelper extends SQLiteOpenHelper {

    // Declaring Database, Table and Column names
    private static final String DATABASE_NAME = "CellEvents.db";
    private static final String TABLE_NAME = "events";
    private static final int DATABASE_VERSION = 1;

    private static final Pair<String, String> COL_ID = new Pair("ID", "INTEGER PRIMARY KEY AUTOINCREMENT");
    private static final Pair<String, String> COL_TIMESTAMP = new Pair("TIMESTAMP", "INTEGER");
    private static final Pair<String, String> COL_EVENT = new Pair("EVENT", "TEXT");
    private static final Pair<String, String> COL_CELLID = new Pair("CELLID", "INTEGER");
    private static final Pair<String, String> COL_LAC = new Pair("LAC", "INTEGER");
    private static final Pair<String, String> COL_MNC = new Pair("MNC", "INTEGER");
    private static final Pair<String, String> COL_MCC = new Pair("MCC", "INTEGER");
    private static final Pair<String, String> COL_MTYPE = new Pair("MTYPE", "TEXT");
    private static final Pair<String, String> COL_DATA_COUNT = new Pair("DATA_COUNT", "INTEGER");
    private static final Pair<String, String> COL_NETWORK_LAT = new Pair("NETWORK_LAT", "REAL");
    private static final Pair<String, String> COL_NETWORK_LON = new Pair("NETWORK_LON", "REAL");
    private static final Pair<String, String> COL_NETWORK_ACC = new Pair("NETWORK_ACC", "REAL");
    private static final Pair<String, String> COL_WIFI_LAT = new Pair("WIFI_LAT", "REAL");
    private static final Pair<String, String> COL_WIFI_LON = new Pair("WIFI_LON", "REAL");
    private static final Pair<String, String> COL_WIFI_ACC = new Pair("WIFI_ACC", "REAL");
    private static final Pair<String, String> COL_GPS_LAT = new Pair("GPS_LAT", "REAL");
    private static final Pair<String, String> COL_GPS_LON = new Pair("GPS_LON", "REAL");
    private static final Pair<String, String> COL_GPS_ACC = new Pair("GPS_ACC", "REAL");
    private static final Pair<String, String> COL_POST_PROCESS = new Pair("POST_PROC", "INTEGER");


    private static DatabaseHelper sInstance;

    public static synchronized DatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Constructor should be private to prevent direct instantiation.
     * make call to static method "getInstance()" instead.
     */
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create the table
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                        COL_ID.first + " " + COL_ID.second + ", " +
                        COL_TIMESTAMP.first + " " + COL_TIMESTAMP.second + ", " +
                        COL_EVENT.first + " " + COL_EVENT.second + ", " +
                        COL_CELLID.first + " " + COL_CELLID.second + ", " +
                        COL_LAC.first + " " + COL_LAC.second + ", " +
                        COL_MNC.first + " " + COL_MNC.second + ", " +
                        COL_MCC.first + " " + COL_MCC.second + ", " +
                        COL_MTYPE.first + " " + COL_MTYPE.second + ", " +
                        COL_DATA_COUNT.first + " " + COL_DATA_COUNT.second + ", " +
                        COL_NETWORK_LAT.first + " " + COL_NETWORK_LAT.second + ", " +
                        COL_NETWORK_LON.first + " " + COL_NETWORK_LON.second + ", " +
                        COL_NETWORK_ACC.first + " " + COL_NETWORK_ACC.second + ", " +
                        COL_WIFI_LAT.first + " " + COL_WIFI_LAT.second + ", " +
                        COL_WIFI_LON.first + " " + COL_WIFI_LON.second + ", " +
                        COL_WIFI_ACC.first + " " + COL_WIFI_ACC.second + ", " +
                        COL_GPS_LAT.first + " " + COL_GPS_LAT.second + ", " +
                        COL_GPS_LON.first + " " + COL_GPS_LON.second + ", " +
                        COL_GPS_ACC.first + " " + COL_GPS_ACC.second + "," +
                        COL_POST_PROCESS.first + " " + COL_POST_PROCESS.second + ");"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(Long timestamp, String event, Integer cid, Integer lac, Integer mnc,
                              Integer mcc, String mobileNetworkType, Integer byteCount,
                              Double netlat, Double netlon, Float netacc, Double gpslat,
                              Double gpslon, Float gpsacc, Integer isPostProc) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_TIMESTAMP.first, timestamp);
        contentValues.put(COL_EVENT.first, event);
        contentValues.put(COL_CELLID.first, cid);
        contentValues.put(COL_LAC.first, lac);
        contentValues.put(COL_MNC.first, mnc);
        contentValues.put(COL_MCC.first, mcc);
        contentValues.put(COL_MTYPE.first, mobileNetworkType);
        contentValues.put(COL_DATA_COUNT.first, byteCount);
        contentValues.put(COL_NETWORK_LAT.first, netlat);
        contentValues.put(COL_NETWORK_LON.first, netlon);
        contentValues.put(COL_NETWORK_ACC.first, netacc);
        contentValues.put(COL_GPS_LAT.first, gpslat);
        contentValues.put(COL_GPS_LON.first, gpslon);
        contentValues.put(COL_GPS_ACC.first, gpsacc);
        contentValues.put(COL_POST_PROCESS.first, isPostProc);
        // returns -1 if it is not inserted
        long result = db.insert(TABLE_NAME, null, contentValues);
        return (result != -1);
    }

    public String[][] getAllDataAsArray() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY id", null);
        String[][] cellEventArray = new String[res.getCount()][18];

        int i = 0;
        while (res.moveToNext()) {
            cellEventArray[i][0] = res.getString(res.getColumnIndex(COL_ID.first));
            cellEventArray[i][1] = res.getString(res.getColumnIndex(COL_TIMESTAMP.first));
            cellEventArray[i][2] = res.getString(res.getColumnIndex(COL_EVENT.first));
            cellEventArray[i][3] = res.getString(res.getColumnIndex(COL_CELLID.first));
            cellEventArray[i][4] = res.getString(res.getColumnIndex(COL_LAC.first));
            cellEventArray[i][5] = res.getString(res.getColumnIndex(COL_MNC.first));
            cellEventArray[i][6] = res.getString(res.getColumnIndex(COL_MCC.first));
            cellEventArray[i][7] = res.getString(res.getColumnIndex(COL_MTYPE.first));
            cellEventArray[i][8] = res.getString(res.getColumnIndex(COL_DATA_COUNT.first));
            cellEventArray[i][9] = res.getString(res.getColumnIndex(COL_NETWORK_LAT.first));
            cellEventArray[i][10] = res.getString(res.getColumnIndex(COL_NETWORK_LON.first));
            cellEventArray[i][11] = res.getString(res.getColumnIndex(COL_NETWORK_ACC.first));
            cellEventArray[i][12] = res.getString(res.getColumnIndex(COL_WIFI_LAT.first));
            cellEventArray[i][13] = res.getString(res.getColumnIndex(COL_WIFI_LON.first));
            cellEventArray[i][14] = res.getString(res.getColumnIndex(COL_WIFI_ACC.first));
            cellEventArray[i][15] = res.getString(res.getColumnIndex(COL_GPS_LAT.first));
            cellEventArray[i][16] = res.getString(res.getColumnIndex(COL_GPS_LON.first));
            cellEventArray[i][17] = res.getString(res.getColumnIndex(COL_GPS_ACC.first));
            i++;
        }

        return cellEventArray;
    }

    public String[][] getAllDataAsArrayOnDate(String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE date(TIMESTAMP, 'unixepoch') = '" + date + "' ORDER BY id", null);
        String[][] cellEventArray = new String[res.getCount()][18];

        int i = 0;
        while (res.moveToNext()) {
            cellEventArray[i][0] = res.getString(res.getColumnIndex(COL_ID.first));
            cellEventArray[i][1] = res.getString(res.getColumnIndex(COL_TIMESTAMP.first));
            cellEventArray[i][2] = res.getString(res.getColumnIndex(COL_EVENT.first));
            cellEventArray[i][3] = res.getString(res.getColumnIndex(COL_CELLID.first));
            cellEventArray[i][4] = res.getString(res.getColumnIndex(COL_LAC.first));
            cellEventArray[i][5] = res.getString(res.getColumnIndex(COL_MNC.first));
            cellEventArray[i][6] = res.getString(res.getColumnIndex(COL_MCC.first));
            cellEventArray[i][7] = res.getString(res.getColumnIndex(COL_MTYPE.first));
            cellEventArray[i][8] = res.getString(res.getColumnIndex(COL_DATA_COUNT.first));
            cellEventArray[i][9] = res.getString(res.getColumnIndex(COL_NETWORK_LAT.first));
            cellEventArray[i][10] = res.getString(res.getColumnIndex(COL_NETWORK_LON.first));
            cellEventArray[i][11] = res.getString(res.getColumnIndex(COL_NETWORK_ACC.first));
            cellEventArray[i][12] = res.getString(res.getColumnIndex(COL_WIFI_LAT.first));
            cellEventArray[i][13] = res.getString(res.getColumnIndex(COL_WIFI_LON.first));
            cellEventArray[i][14] = res.getString(res.getColumnIndex(COL_WIFI_ACC.first));
            cellEventArray[i][15] = res.getString(res.getColumnIndex(COL_GPS_LAT.first));
            cellEventArray[i][16] = res.getString(res.getColumnIndex(COL_GPS_LON.first));
            cellEventArray[i][17] = res.getString(res.getColumnIndex(COL_GPS_ACC.first));
            i++;
        }

        return cellEventArray;
    }

    public String[] getLastThreeDates() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT date(TIMESTAMP, 'unixepoch') AS date FROM events ORDER BY date DESC LIMIT 3;", null);
        String[] dates = new String[3];

        int i = 0;
        while (res.moveToNext()) {
            dates[i] = res.getString(0);
            i++;
        }

        return dates;
    }
}
