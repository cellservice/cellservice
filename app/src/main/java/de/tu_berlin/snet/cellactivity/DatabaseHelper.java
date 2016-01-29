package de.tu_berlin.snet.cellactivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;


public class DatabaseHelper extends SQLiteOpenHelper{

    // Declaring Database, Table and Column names
    private static final String DATABASE_NAME = "CellEvents.db";
    private static final String TABLE_NAME = "events";
    private static final int DATABASE_VERSION = 1;

    private static final Pair<String, String> COL_ID = new Pair("ID", "INTEGER PRIMARY KEY AUTOINCREMENT");
    private static final Pair<String, String> COL_TIMESTAMP = new Pair("TIMESTAMP", "INTEGER");
    private static final Pair<String, String> COL_EVENT = new Pair("EVENT", "TEXT");

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
                   COL_EVENT.first + " " + COL_EVENT.second + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(Long timestamp, String event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_TIMESTAMP.first, timestamp);
        contentValues.put(COL_EVENT.first, event);
        // returns -1 if it is not inserted
        long result = db.insert(TABLE_NAME, null, contentValues);
        return (result != -1);
    }

    public String[][] getAllDataAsArray() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        String[][] cellEventArray = new String[res.getCount()][2];
        int i = 0;
        while(res.moveToNext()) {
            String time = res.getString(res.getColumnIndex(COL_TIMESTAMP.first));
            String event = res.getString(res.getColumnIndex(COL_EVENT.first));
            cellEventArray[i][0] = time;
            cellEventArray[i][1] = event;
            i++;
        }

        return cellEventArray;
    }
}
