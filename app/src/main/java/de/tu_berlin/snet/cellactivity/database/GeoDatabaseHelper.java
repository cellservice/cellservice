package de.tu_berlin.snet.cellactivity.database;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellactivity.R;
import de.tu_berlin.snet.cellactivity.record.Call;
import de.tu_berlin.snet.cellactivity.record.Data;
import de.tu_berlin.snet.cellactivity.record.Handover;
import de.tu_berlin.snet.cellactivity.record.LocationUpdate;
import de.tu_berlin.snet.cellactivity.record.TextMessage;
import de.tu_berlin.snet.cellactivity.util.CellInfo;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

/**
 * Created by Friedhelm Victor on 4/21/16.
 * This class should implement the Interface MobileNetworkDataWritable
 */
public class GeoDatabaseHelper implements MobileNetworkDataCapable {

    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = "/data/data/de.tu_berlin.snet.cellactivity/databases";
    private static String DB_NAME = "spatial.sqlite";
    private Context context;
    private Database mDb;
    private static GeoDatabaseHelper sInstance;

    private final String cellExistsQuery =
            "SELECT id FROM Cells" +
            "   WHERE cellid = %1$s" +
            "   AND lac = %2$s" +
            "   AND mnc = %3$s" +
            "   AND mcc = %4$s" +
            "   AND technology = %5$s";
    private  final String handoverExistsQuery =
            "SELECT id FROM Handovers"+
            "  WHERE startcell = %1$s"+
            "  AND endcell =%2$s"+
            "  AND time = %3$s"+
            "  AND time = %4$s";

    public static synchronized GeoDatabaseHelper getInstance(Context context) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new GeoDatabaseHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private GeoDatabaseHelper(Context context) {
        try {
            //File sdcardDir = ""; // your sdcard path
            File spatialDbFile = new File(DB_PATH, DB_NAME);

            mDb = new jsqlite.Database();
            mDb.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Careful! This step can take some time!
        // See http://northredoubt.com/n/2012/06/03/recent-spatialite-news-may-2012/
        //execSQL("SELECT InitSpatialMetaData('WGS84_ONLY');"); // 130 rows
        execSQL("SELECT InitSpatialMetaData('NONE');");
        execSQL("SELECT InsertEpsgSrid(4326);");

        String createCellsTable =
                "CREATE TABLE IF NOT EXISTS Cells (" +
                "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "   cellid INTEGER," +
                "   lac INTEGER," +
                "   mnc INTEGER," +
                "   mcc INTEGER," +
                "   technology INTEGER" +
                "   );";

        String createLocationUpdatesTable =
                "CREATE TABLE IF NOT EXISTS LocationUpdates (" +
                "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "   startcell REFERENCES Cells(id)," +
                "   endcell REFERENCES Cells(id)," +
                "   time INTEGER" +
                "   );";

        String createHandoversTable =
                "CREATE TABLE IF NOT EXISTS Handovers (" +
                "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "	startcell REFERENCES Cells(id)," +
                "	endcell REFERENCES Cells(id)," +
                "   time INTEGER" +
                "   callid REFERENCES Calls(id)"+
                "	)";

        String createDataEventsTable =
                "CREATE TABLE IF NOT EXISTS DataRecords (" +
                "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "   rxbytes INTEGER," +
                "   txbytes INTEGER," +
                "   starttime INTEGER," +
                "   endtime INTEGER," +
                "   cell INTEGER REFERENCES Cells(id)" +
                "   );";

        String createCallsTable =
                "CREATE TABLE IF NOT EXISTS Calls (" +
                "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "	direction TEXT," +
                "	address TEXT," +
                "	starttime INTEGER," +
                "	endtime INTEGER," +
                "	startcell INTEGER REFERENCES Cells(id)" +
              //  "	handover INTEGER REFERENCES Handovers(id)" + //one call can be have more than one HO
                "	);";

        String createTextMessagesTable =
                "CREATE TABLE IF NOT EXISTS TextMessages (" +
                "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "	direction TEXT," +
                "	address TEXT," +
                "	time INTEGER," +
                "	cell INTEGER REFERENCES Cells(id)" +
                "	);";

        String createMeasurementsTable =
                "CREATE TABLE IF NOT EXISTS Measurements (" +
                "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "	cell INTEGER REFERENCES Cells(id)," +
                "	provider TEXT," +
                "   accuracy REAL" +
                "	);";

        String addPointGeometryToMeasurementsTable =
                "SELECT AddGeometryColumn('Measurements', 'centroid', 4326, 'POINT', 'XY', 1);";


        execSQL(createCellsTable);
        execSQL(createLocationUpdatesTable);
        execSQL(createHandoversTable);
        execSQL(createDataEventsTable);
        execSQL(createCallsTable);
        execSQL(createTextMessagesTable);
        execSQL(createMeasurementsTable);
        execSQL(addPointGeometryToMeasurementsTable);

    }

    synchronized void execSQL(String statement) {
        try {
            Stmt stmt = mDb.prepare(statement);
            stmt.step();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized void execSQL(String statement, String... args) {
        try {
            Stmt stmt = mDb.prepare(String.format(statement, args));
            stmt.step();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean insertRecord(Call call) {
        CellInfo cellInfo = call.getStartCell();
        insertMeasurement(cellInfo);
        //get the id of the cellid
        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertCallRecordStatement =
                "INSERT INTO Calls (direction, address, starttime, endtime, startcell)" + "  VALUES (%1$s, %2$s, %3$s, %4$s, %5$s);";

        execSQL(insertCallRecordStatement, call.getDirection(), call.getAddress(), String.valueOf(call.getStartTime()), String.valueOf(call.getEndTime()), String.valueOf(cellRecordId));
        //int callId = getLastRowId("Calls");
        //get the row id
       // insert handover(handovers, id of the call)
        Iterator<Handover> handoverIterator = call.getHandovers().iterator();
        while (handoverIterator.hasNext()) {
            insertRecord(handoverIterator.next());
        }
        return false;
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {

        CellInfo cellInfo = textMessage.getCell();
        insertMeasurement(cellInfo);

        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertDataRecordStatement =
                "INSERT INTO TextMessages (direction, address, time, cell)" +
                        "   VALUES (%s, %s, %s, %s);";

        execSQL(String.format(insertDataRecordStatement,textMessage.getDirection(), textMessage.getAddress(), textMessage.getTime(),cellRecordId));
        return false;
    }

    @Override
    public boolean insertRecord(Handover handover) {

        String insertHandoverStatement =
                "INSERT INTO Handovers (startcell, endcell, time, callid )" +
                        "   SELECT %1$s, %2$s, %3$s, %4$s" +
                        "   WHERE NOT EXISTS (" +
                        handoverExistsQuery +
                        "   );";
        int startcellId = getCellPrimaryKey(handover.getStartCell());
        int endcellid   = getCellPrimaryKey(handover.getEndCell());
        long time       = handover.getTimestamp();
        execSQL(String.format(insertHandoverStatement,startcellId, endcellid, time));
        return false;
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        return false;
    }

    @Override
    public boolean insertRecord(Data data) {
        CellInfo cellInfo = data.getCell();
        insertMeasurement(cellInfo);

        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertDataRecordStatement =
                "INSERT INTO DataRecords (rxbytes, txbytes, starttime, endtime, cell)" +
                "   VALUES (%s, %s, %s, %s, %s);";
        Log.i("test",""+getLastRowId("Datarecords"));
        execSQL(String.format(insertDataRecordStatement, data.getRxBytes(), data.getTxBytes(), data.getSessionStart(), data.getSessionEnd(), cellRecordId));
        return false;
    }


    @Override
    public boolean insertMeasurement(CellInfo cellInfo) {
        String insertCellStatement =
                "INSERT INTO Cells (cellid, lac, mnc, mcc, technology)" +
                "   SELECT %1$s, %2$s, %3$s, %4$s, %5$s" +
                "   WHERE NOT EXISTS (" +
                    cellExistsQuery +
                "   );";

        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());

        execSQL(insertCellStatement, cid, lac, mnc, mcc, technology);
        final int cellRecordId = getCellPrimaryKey(cellInfo);
        Log.e("DB", "INSERTED CELL " + cellRecordId);

        final String insertMeasurementStatement =
                "INSERT INTO Measurements (cell, provider, accuracy, centroid)" +
                "   VALUES (%s, '%s', %s, GeomFromText('POINT(%s %s)', 4326));";

        // TODO: BIG PROBLEM HERE WITH final
        final ArrayList<Future<Location>> locationMeasurements = cellInfo.getLocations();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Future<Location> futureLocation : locationMeasurements) {
                    try {
                        Location location = futureLocation.get();
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float accuracy = location.getAccuracy();
                        String provider = location.getProvider();
                        String statement = String.format(insertMeasurementStatement, cellRecordId, provider, accuracy, longitude, latitude);
                        Log.e(TAG_SL, "Inserting measurement sql: "+statement);
                        execSQL(statement);
                    } catch (java.lang.Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        return true;
    }

    private int getCellPrimaryKey(CellInfo cellInfo) {
        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());
        return getId(String.format(cellExistsQuery, cid, lac, mnc, mcc, technology));
    }

    private int getId(String queryWithOneIdResult) {
        try {
            TableResult result = mDb.get_table(queryWithOneIdResult);
            Vector<String[]> rows = result.rows;
            int id = Integer.valueOf(rows.get(0)[0]);
            return id;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find id with query: "+queryWithOneIdResult);
            return -1;
        }
    }
    private int getLastRowId(String tablename){
        try {
            Stmt stmt = mDb.prepare("SELECT MAX(id) FROM "+tablename);
            stmt.step();
            int lastid = Integer.parseInt(stmt.column_string(0));
            return  lastid;
        }catch (java.lang.Exception e){
            e.printStackTrace();
        }
        return -1;
    }
    // https://www.gaia-gis.it/gaia-sins/spatialite-cookbook/html/ins-upd-del.html

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
