package de.tu_berlin.snet.cellservice.model.database;

import android.app.ActionBar;
import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.FakeCellInfo;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

/**
 * Created by Friedhelm Victor on 4/21/16.
 * This class should implement the Interface MobileNetworkDataCapable
 */
public class GeoDatabaseHelper implements MobileNetworkDataCapable {

    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = Environment.getExternalStorageDirectory().getPath();
    //private static String DB_PATH = "/data/data/de.tu_berlin.snet.cellservice/databases";
    private static String DB_NAME = "spatial.sqlite";
    private Database mDb;
    private static GeoDatabaseHelper sInstance;

    private final String cellExistsQuery =
            "SELECT id FROM Cells" +
            "   WHERE cellid = %1$s" +
            "   AND lac = %2$s" +
            "   AND mnc = %3$s" +
            "   AND mcc = %4$s" +
            "   AND technology = %5$s";

    private final String callExistsQuery =
            "SELECT id FROM Calls" +
            "   WHERE direction = '%1$s'" +
            "   AND address = '%2$s'" +
            "   AND starttime = %3$s" +
            "   AND endtime = %4$s" +
            "   AND startcell = %5$s";

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
            Log.e("CREATE DATABASE FILE", "PATH: "+spatialDbFile);

            mDb = new jsqlite.Database();
            mDb.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        createTables(); // TODO: ONLY CREATE TABLES IF THE DATABASE DIDN'T EXIST YET
    }


    /*
    TODO: WHY NOT USE mDb.exec? What about the callback feature?
     */
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
        insertMeasurement(cellInfo, "call");

        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = getCellPrimaryKey(cellInfo);

        String insertCallRecordStatement =
                "INSERT INTO Calls (direction, address, starttime, endtime, startcell)" +
                "   VALUES ('%1$s', '%2$s', %3$s, %4$s, %5$s);";

        execSQL(String.format(insertCallRecordStatement, direction, address, starttime, endtime, cellRecordId));

        int callId = getCallPrimaryKey(call);

        for(Handover handover : call.getHandovers()) {
            insertRecord(handover, callId);
        }
        return false;
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {
        CellInfo cellInfo = textMessage.getCell();
        insertMeasurement(cellInfo, "text message");
        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertTextMessageStatement =
                "INSERT INTO TextMessages (direction, address, time, cell_id)" +
                "   VALUES ('%s', '%s', %s, %s);";

        execSQL(String.format(insertTextMessageStatement, textMessage.getDirection(), textMessage.getAddress(), textMessage.getTime(), cellRecordId));
        return false;
    }

    @Override
    public boolean insertRecord(Handover handover, int callId) {
        String insertHandoverStatement =
                "INSERT INTO Handovers (call_id, startcell, endcell, time)" +
                "   VALUES (%s, %s, %s, %s);";

        insertMeasurement(handover.getStartCell(), "handover start");
        int startCellId = getCellPrimaryKey(handover.getStartCell());
        insertMeasurement(handover.getEndCell(), "handover end");
        int endCellid = getCellPrimaryKey(handover.getEndCell());

        execSQL(String.format(insertHandoverStatement, callId, startCellId, endCellid, handover.getTimestamp()));

        return false;
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        String insertLocationUpdateStatement =
                "INSERT INTO LocationUpdates (startcell, endcell, time)" +
                        "   VALUES (%s, %s, %s);";

        insertMeasurement(locationUpdate.getStartCell(), "location update start");
        int startCellId = getCellPrimaryKey(locationUpdate.getStartCell());
        insertMeasurement(locationUpdate.getEndCell(), "location update end");
        int endCellid = getCellPrimaryKey(locationUpdate.getEndCell());

        execSQL(String.format(insertLocationUpdateStatement, startCellId, endCellid, locationUpdate.getTimestamp()));
        return false;
    }

    @Override
    public boolean insertRecord(Data data) {
        CellInfo cellInfo = data.getCell();
        insertMeasurement(cellInfo, "data");

        int cellRecordId = getCellPrimaryKey(cellInfo);
        String insertDataRecordStatement =
                "INSERT INTO DataRecords (rxbytes, txbytes, starttime, endtime, cell_id)" +
                "   VALUES (%s, %s, %s, %s, %s);";
        execSQL(String.format(insertDataRecordStatement, data.getRxBytes(), data.getTxBytes(), data.getSessionStart(), data.getSessionEnd(), cellRecordId));
        return false;
    }


    @Override
    public boolean insertMeasurement(final CellInfo cellInfo, final String event) {
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
                "INSERT INTO Measurements (cell_id, provider, accuracy, centroid, event, time)" +
                "   VALUES (%s, '%s', %s, GeomFromText('POINT(%s %s)', 4326), '%s', %s);";

        // TODO: POSSIBLY BIG PROBLEM HERE WITH final
        // Maybe the Location Futures are being frozen when they actually should still be running
        final ArrayList<Future<Location>> locationMeasurements = (ArrayList<Future<Location>>) cellInfo.getLocations().clone();

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
                        String statement = String.format(insertMeasurementStatement, cellRecordId, provider, accuracy, longitude, latitude, event, location.getTime()/1000);
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

    private int getCallPrimaryKey(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = getCellPrimaryKey(cellInfo);
        return getId(String.format(callExistsQuery, direction, address, starttime, endtime, cellRecordId));
    }

    private int getId(String queryWithOneIdResult) {
        try {
            TableResult result = mDb.get_table(queryWithOneIdResult);
            Vector<String[]> rows = result.rows;
            return Integer.valueOf(rows.get(0)[0]);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find id with query: "+queryWithOneIdResult);
            return -1;
        }
    }

    @Override
    public void createTables() {
        // Careful! This step can take some time!
        // See http://northredoubt.com/n/2012/06/03/recent-spatialite-news-may-2012/
        //execSQL("SELECT InitSpatialMetaData('WGS84_ONLY');"); // 130 rows
        execSQL("SELECT InitSpatialMetaData('NONE');");
        execSQL("SELECT InsertEpsgSrid(4326);");
        execSQL("SELECT InsertEpsgSrid(32632);");
        execSQL("SELECT InsertEpsgSrid(32633);");
        execSQL("SELECT InsertEpsgSrid(25832);");
        execSQL("SELECT InsertEpsgSrid(25833);");

        String createCellsTable =
                "CREATE TABLE IF NOT EXISTS Cells (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   cellid INTEGER NOT NULL," +
                        "   lac INTEGER NOT NULL," +
                        "   mnc INTEGER NOT NULL," +
                        "   mcc INTEGER NOT NULL," +
                        "   technology INTEGER NOT NULL" +
                        "   );";

        String createCallsTable =
                "CREATE TABLE IF NOT EXISTS Calls (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	direction TEXT NOT NULL," +
                        "	address TEXT NOT NULL," +
                        "	starttime INTEGER NOT NULL," +
                        "	endtime INTEGER NOT NULL," +
                        "	startcell INTEGER NOT NULL REFERENCES Cells(id)" +
                        "	);";

        String createHandoversTable =
                "CREATE TABLE IF NOT EXISTS Handovers (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   call_id INTEGER NOT NULL REFERENCES Calls(id)," +
                        "	startcell NOT NULL REFERENCES Cells(id)," +
                        "	endcell NOT NULL REFERENCES Cells(id)," +
                        "   time INTEGER NOT NULL" +
                        "	)";

        String createLocationUpdatesTable =
                "CREATE TABLE IF NOT EXISTS LocationUpdates (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   startcell NOT NULL REFERENCES Cells(id)," +
                        "   endcell NOT NULL REFERENCES Cells(id)," +
                        "   time INTEGER NOT NULL" +
                        "   );";

        String createDataEventsTable =
                "CREATE TABLE IF NOT EXISTS DataRecords (" +
                        "   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "   rxbytes INTEGER NOT NULL," +
                        "   txbytes INTEGER NOT NULL," +
                        "   starttime INTEGER NOT NULL," +
                        "   endtime INTEGER NOT NULL," +
                        "   cell_id INTEGER NOT NULL REFERENCES Cells(id)" +
                        "   );";

        String createTextMessagesTable =
                "CREATE TABLE IF NOT EXISTS TextMessages (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	direction TEXT NOT NULL," +
                        "	address TEXT NOT NULL," +
                        "	time INTEGER NOT NULL," +
                        "	cell_id INTEGER NOT NULL REFERENCES Cells(id)" +
                        "	);";

        String createMeasurementsTable =
                "CREATE TABLE IF NOT EXISTS Measurements (" +
                        "	id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                        "	cell_id INTEGER NOT NULL REFERENCES Cells(id)," +
                        "	provider TEXT NOT NULL," +
                        "   accuracy REAL NOT NULL," +
                        "   event TEXT NOT NULL," +
                        "   time INTEGER NOT NULL" +
                        "	);";

        String addPointGeometryToMeasurementsTable =
                "SELECT AddGeometryColumn('Measurements', 'centroid', 4326, 'POINT', 'XY', 1);";


        execSQL(createCellsTable);
        execSQL(createCallsTable);
        execSQL(createHandoversTable);
        execSQL(createLocationUpdatesTable);
        execSQL(createDataEventsTable);
        execSQL(createTextMessagesTable);
        execSQL(createMeasurementsTable);
        execSQL(addPointGeometryToMeasurementsTable);
    }

    public CellInfo getCellById(long id) {
        String getCellByIdStatement =
                "SELECT cellid, lac, mnc, mcc, technology" +
                "   FROM Cells" +
                "   WHERE id = %d;";
        try {
            TableResult result = mDb.get_table(String.format(getCellByIdStatement, id));
            String[] fields = (String[]) result.rows.get(0);
            return new CellInfo(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]), Integer.parseInt(fields[3]), Integer.parseInt(fields[4]));
        } catch (Exception e) {
            e.printStackTrace();
            return new FakeCellInfo();
        }
    }

    public ArrayList<CellInfo> getAllCellRecords() {
        ArrayList<CellInfo> cellInfoArrayList = new ArrayList<CellInfo>();
        final String selectAllCells =
                "SELECT cellid, lac, mnc, mcc, technology" +
                        "   FROM Cells;";
        try {
            TableResult tableResult = mDb.get_table(selectAllCells);
            Vector<String[]> rows = tableResult.rows;
            for(String[] fields : rows) {
                CellInfo cellInfo = parseCell(fields);
                cellInfoArrayList.add(cellInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cellInfoArrayList;
    }

    @NonNull
    private CellInfo parseCell(String[] fields) {
        int cellid = Integer.valueOf(fields[0]);
        int lac = Integer.valueOf(fields[1]);
        int mnc = Integer.valueOf(fields[2]);
        int mcc = Integer.valueOf(fields[3]);
        int technology = Integer.valueOf(fields[5]);

        return new CellInfo(cellid, lac, mnc, mcc, technology);
    }

    private ArrayList<Handover> getHandoversByCallId(long id) {
        ArrayList<Handover> handovers = new ArrayList<Handover>();
        String getHandoverByCallIdStatement =
                "SELECT startcell, endcell time" +
                "   FROM Handovers" +
                "   WHERE call_id = %d;";
        try {
            TableResult result = mDb.get_table(String.format(getHandoverByCallIdStatement, id));
            Vector<String[]> rows = result.rows;
            for(String[] fields : rows) {
                CellInfo startCell = getCellById(Long.valueOf(fields[0]));
                CellInfo endCell = getCellById(Long.valueOf(fields[1]));
                long timestamp = Long.valueOf(fields[2]);
                Handover handover = new Handover(startCell, endCell);
                handover.setTimestamp(timestamp);
                handovers.add(handover);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return handovers;
    }

    public Date[] getLastThreeDates() {
        String threeDatesStatement =
                "SELECT DISTINCT strftime('%s',date(starttime, 'unixepoch', 'localtime')) AS date" +
                "   FROM (SELECT starttime FROM Calls UNION" +
                "         SELECT starttime FROM DataRecords UNION" +
                "         SELECT time AS starttime FROM Handovers UNION" +
                "         SELECT time AS starttime FROM LocationUpdates UNION" +
                "         SELECT time AS starttime FROM TextMessages)" +
                "   AS events" +
                "   ORDER BY date DESC" +
                "   LIMIT 3;";
        try {
            TableResult tableResult = mDb.get_table(threeDatesStatement);
            Vector<String[]> rows = tableResult.rows;
            Date[] result = new Date[rows.size()];
            for(int i = 0; i<rows.size(); i++) result[i] = new Date(Long.valueOf(rows.get(i)[0])*1000);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return new Date[] {};
        }
    }

    @Override
    public ArrayList<Call> getAllCallRecords() {
        ArrayList<Call> callArrayList = new ArrayList<Call>();
        final String selectAllCalls =
                "SELECT id, direction, address, starttime, endtime, startcell" +
                        "   FROM Calls;";
        try {
            TableResult tableResult = mDb.get_table(selectAllCalls);
            Vector<String[]> rows = tableResult.rows;
            for(String[] fields : rows) {
                Call call = parseCall(fields);
                callArrayList.add(call);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return callArrayList;
    }

    @Override
    public ArrayList<Call> getCallRecords(Date day) {
        return getCallRecords(day, day);
    }

    @Override
    public ArrayList<Call> getCallRecords(Date from, Date to) {
        ArrayList<Call> callArrayList = new ArrayList<Call>();
        final String selectCallsByDate =
                "SELECT id, direction, address, starttime, endtime, startcell" +
                "   FROM Calls" +
                "   WHERE date(starttime, 'unixepoch', 'localtime') >= '"+ from.toString() + "'" +
                "   AND date(endtime, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult tableResult = mDb.get_table(selectCallsByDate);
            Vector<String[]> rows = tableResult.rows;
            for(String[] fields : rows) {
                Call call = parseCall(fields);
                callArrayList.add(call);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return callArrayList;
    }

    @NonNull
    private Call parseCall(String[] fields) {
        long call_id = Long.valueOf(fields[0]);
        String direction = fields[1];
        String address = fields[2];
        long starttime = Long.valueOf(fields[3]);
        long endtime = Long.valueOf(fields[4]);
        CellInfo startCell = getCellById(Long.valueOf(fields[5]));

        Call call = new Call(startCell, direction, address, new ArrayList<Handover>(), starttime, endtime);
        for(Handover handover : getHandoversByCallId(call_id)) {
            call.addHandover(handover);
        }
        return call;
    }

    @Override
    public ArrayList<TextMessage> getAllTextMessageRecords() {
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT direction, address, time, cell_id" +
                        "   FROM TextMessages;";
        try {
            TableResult tableResult = mDb.get_table(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for(String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textMessages;
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date day) {
        return getTextMessageRecords(day, day);
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date from, Date to) {
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT direction, address, time, cell_id" +
                "   FROM TextMessages" +
                "   WHERE date(time, 'unixepoch', 'localtime') >= '"+ from.toString() + "'" +
                "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult tableResult = mDb.get_table(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for(String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return textMessages;
    }

    @NonNull
    private TextMessage parseTextMessage(String[] fields) {
        String direction = fields[0];
        String address = fields[1];
        long time = Long.valueOf(fields[2]);
        CellInfo cell = getCellById(Long.valueOf(fields[3]));
        return new TextMessage(cell, direction, address, time);
    }

    @Override
    public ArrayList<Handover> getAllHandoverRecords() {
        ArrayList<Handover> handoverArrayList = new ArrayList<Handover>();
        final String selectHandoversByDate =
                "SELECT startcell, endcell, time FROM Handovers;";
        try {
            TableResult result = mDb.get_table(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @NonNull
    private Handover parseHandover(String[] row) {
        CellInfo startCell = getCellById(Long.parseLong(row[0]));
        CellInfo endCell = getCellById(Long.parseLong(row[1]));
        Handover handover = new Handover(startCell, endCell);
        handover.setTimestamp(Long.parseLong(row[2]));
        return handover;
    }

    @Override
    public ArrayList<Handover> getHandoverRecords(Date day) {
        return getHandoverRecords(day, day);
    }

    @Override
    public ArrayList<Handover> getHandoverRecords(Date from, Date to) {
        ArrayList<Handover> handoverArrayList = new ArrayList<Handover>();
        final String selectHandoversByDate =
                "SELECT startcell, endcell, time FROM Handovers" +
                "   WHERE date(time, 'unixepoch', 'localtime') >= '"+ from.toString() + "'" +
                "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @Override
    public ArrayList<LocationUpdate> geAllLocationUpdateRecords() {
        ArrayList<LocationUpdate> locationUpdateArrayList = new ArrayList<LocationUpdate>();
        final String selectLocationUpdatesByDate =
                "SELECT startcell, endcell, time FROM LocationUpdates;";
        try {
            TableResult result = mDb.get_table(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }

    @NonNull
    private LocationUpdate parseLocationUpdate(String[] row) {
        CellInfo startCell = getCellById(Long.parseLong(row[0]));
        CellInfo endCell = getCellById(Long.parseLong(row[1]));
        LocationUpdate locationUpdate = new LocationUpdate(startCell, endCell);
        locationUpdate.setTimestamp(Long.parseLong(row[2]));
        return locationUpdate;
    }

    @Override
    public ArrayList<LocationUpdate> getLocationUpdateRecords(Date day) {
        return getLocationUpdateRecords(day, day);
    }

    @Override
    public ArrayList<LocationUpdate> getLocationUpdateRecords(Date from, Date to) {
        ArrayList<LocationUpdate> locationUpdateArrayList = new ArrayList<LocationUpdate>();
        final String selectLocationUpdatesByDate =
                "SELECT startcell, endcell, time FROM LocationUpdates" +
                        "   WHERE date(time, 'unixepoch', 'localtime') >= '"+ from.toString() + "'" +
                        "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }

    @Override
    public ArrayList<Data> getAllDataRecords(Date day) {
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords;";
        try {
            TableResult result = mDb.get_table(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @Override
    public ArrayList<Data> getDataRecords(Date day) {
        return  getDataRecords(day, day);
    }

    @Override
    public ArrayList<Data> getDataRecords(Date from, Date to) {
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords"+
                "   WHERE date(starttime, 'unixepoch', 'localtime') >= '"+ from.toString() + "'" +
                "   AND date(endtime, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG_SL, "could not find: "+selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @NonNull
    private Data parseDataRecord(String[] row) {
        CellInfo cellInfo = getCellById(Long.parseLong(row[4]));
        return new Data(cellInfo, Long.parseLong(row[0]), Long.parseLong(row[1]), Long.parseLong(row[2]), Long.parseLong(row[3]));
    }
}
