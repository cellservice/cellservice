package snet.tu_berlin.de.mycellservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper{

    // Declaring Database, Table and Column names
    public static final String DATABASE_NAME = "CellEvents.db";
    public static final String TABLE_NAME = "events";

    public static final String COL_1 = "ID";
    public static final String COL_2 = "TIMESTAMP";
    public static final String COL_3 = "EVENT";


    // Whenever this constructor is called, the database will be created
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase(); // this is just for checking
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create the table
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "TIMESTAMP INTEGER, " +
                "EVENT TEXT);");


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);

    }

    public boolean insertData(Long timestamp, String event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2, timestamp);
        contentValues.put(COL_3, event);
        long result = db.insert(TABLE_NAME, null, contentValues); // returns -1 if it is not inserted
        return (result != -1);
    }

    private Cursor getAllDataAsCursor() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return res;
    }

    public String[][] getAllDataAsArray() {
        Cursor res = getAllDataAsCursor();
        String[][] cellEventArray = new String[res.getCount()][2];
        int i = 0;
        while(res.moveToNext()) {
            String time = res.getString(res.getColumnIndex("TIMESTAMP"));
            String event = res.getString(res.getColumnIndex("EVENT"));
            cellEventArray[i][0] = time;
            cellEventArray[i][1] = event;
            i++;
        }
        return cellEventArray;
    }
}
