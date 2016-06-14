package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.Measurement;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.FakeCellInfo;
import de.tu_berlin.snet.cellservice.util.Constants;
import de.tu_berlin.snet.cellservice.util.database.MigrationManager;
import de.tu_berlin.snet.cellservice.util.database.SQLExecutable;
import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;
import jsqlite.TableResult;

/**
 * Created by Friedhelm Victor on 4/21/16.
 */
public class GeoDatabaseHelper implements MobileNetworkDataCapable, SQLExecutable {
    private final static String LOG_TAG = GeoDatabaseHelper.class.getSimpleName();

    private static final int CALL = 1, HANDOVER = 2, LOCATION_UPDATE = 3, DATA = 4, TEXT = 5, UNKNOWN = -1;
    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = Environment.getExternalStorageDirectory().getPath();
    private static String DB_NAME = "spatial.sqlite";
    private Database mDb;
    private static GeoDatabaseHelper sInstance;
    private List<CDRDatabaseInsertionListener> listeners = new ArrayList<CDRDatabaseInsertionListener>();

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
            File spatialDbFile = new File(DB_PATH, DB_NAME);
            Log.d("CREATE DATABASE FILE", "PATH: " + spatialDbFile);

            mDb = new jsqlite.Database();
            mDb.open(spatialDbFile.getAbsolutePath(), jsqlite.Constants.SQLITE_OPEN_READWRITE
                    | jsqlite.Constants.SQLITE_OPEN_CREATE);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }

        MigrationManager migrationManager = new MigrationManager(context, this, Constants.MIGRATION_FILE_PATH);
        migrationManager.initialize();
        migrationManager.run();
    }

    public void addListener(CDRDatabaseInsertionListener listener) {
        listeners.add(listener);
    }

    public void removeListener(CDRDatabaseInsertionListener listener) {
        listeners.remove(listener);
    }


    /*
    TODO: WHY NOT USE mDb.exec? What about the callback feature?
     */
    public synchronized void execSQL(String statement) {
        try {
            Stmt stmt = mDb.prepare(statement);
            stmt.step();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public String[][] getSQLTableResult(String sql) {
        String[][] resultRows;
        try {
            TableResult result = mDb.get_table(sql);
            Vector<String[]> rows = result.rows;
            if (rows != null && rows.size() > 0) {
                resultRows = new String[rows.size()][rows.firstElement().length];
                for (int i = 0; i < rows.size(); i++) {
                    resultRows[i] = rows.get(i);
                }
                return resultRows;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return null;
    }

    public synchronized void execSQL(String statement, String... args) {
        try {
            Stmt stmt = mDb.prepare(String.format(statement, args));
            stmt.step();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public boolean insertRecord(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        insertRecord(cellInfo);
        int cellRecordId = getPrimaryKey(cellInfo);

        String insertCallRecordStatement =
                "INSERT INTO Calls (direction, address, starttime, endtime, startcell)" +
                "   VALUES ('%1$s', '%2$s', %3$s, %4$s, %5$s);";
        execSQL(String.format(insertCallRecordStatement, direction, address, starttime, endtime, cellRecordId));
        int callId = getPrimaryKey(call);

        insertMeasurements(cellInfo, callId, CALL);

        for (Handover handover : call.getHandovers()) {
            insertRecord(handover, callId);
        }

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onCallRecordInserted(call, callId);

        return false;
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {
        CellInfo cellInfo = textMessage.getCell();
        insertRecord(cellInfo);
        int cellRecordId = getPrimaryKey(cellInfo);

        String insertTextMessageStatement =
                "INSERT INTO TextMessages (direction, address, time, cell_id)" +
                "   VALUES ('%s', '%s', %s, %s);";
        execSQL(String.format(insertTextMessageStatement, textMessage.getDirection(),
                textMessage.getAddress(), textMessage.getTime(), cellRecordId));

        int messageId = getPrimaryKey(textMessage);
        insertMeasurements(cellInfo, messageId, TEXT);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onTextMessageInserted(textMessage, messageId);

        return false;
    }

    @Override
    public boolean insertRecord(Handover handover, int callId) {
        String insertHandoverStatement =
                "INSERT INTO Handovers (call_id, startcell, endcell, time)" +
                "   VALUES (%s, %s, %s, %s);";

        insertRecord(handover.getStartCell());
        insertRecord(handover.getEndCell());
        int startCellId = getPrimaryKey(handover.getStartCell());
        int endCellid = getPrimaryKey(handover.getEndCell());
        execSQL(String.format(insertHandoverStatement, callId, startCellId, endCellid, handover.getTimestamp()));
        int handoverId = getPrimaryKey(handover);

        insertMeasurements(handover.getStartCell(), handoverId, HANDOVER);
        insertMeasurements(handover.getEndCell(), handoverId, HANDOVER);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onHandoverInserted(handover, handoverId);

        return false;
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        String insertLocationUpdateStatement =
                "INSERT INTO LocationUpdates (startcell, endcell, time)" +
                "   VALUES (%s, %s, %s);";

        insertRecord(locationUpdate.getStartCell());
        insertRecord(locationUpdate.getEndCell());
        int startCellId = getPrimaryKey(locationUpdate.getStartCell());
        int endCellid = getPrimaryKey(locationUpdate.getEndCell());
        execSQL(String.format(insertLocationUpdateStatement, startCellId, endCellid, locationUpdate.getTimestamp()));
        int locationUpdateId = getPrimaryKey(locationUpdate);

        insertMeasurements(locationUpdate.getStartCell(), locationUpdateId, LOCATION_UPDATE);
        insertMeasurements(locationUpdate.getEndCell(), locationUpdateId, LOCATION_UPDATE);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onLocationUpdateInserted(locationUpdate, locationUpdateId);

        return false;
    }

    @Override
    public boolean insertRecord(Data data) {
        String insertDataRecordStatement =
                "INSERT INTO DataRecords (rxbytes, txbytes, starttime, endtime, cell_id)" +
                "   VALUES (%s, %s, %s, %s, %s);";
        CellInfo cellInfo = data.getCell();
        insertRecord(cellInfo);
        int cellPrimaryKey = getPrimaryKey(cellInfo);

        execSQL(String.format(insertDataRecordStatement, data.getRxBytes(), data.getTxBytes(),
                data.getSessionStart(), data.getSessionEnd(), cellPrimaryKey));
        int dataId = getPrimaryKey(data);

        insertMeasurements(cellInfo, dataId, DATA);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onDataSessionInserted(data, dataId);

        return false;
    }


    @Override
    public boolean insertRecord(final CellInfo cellInfo) {
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
        final int cellInfoId = getPrimaryKey(cellInfo);
        Log.e("DB", "INSERTED CELL " + cellInfoId);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : listeners) l.onCellInfoInserted(cellInfo, cellInfoId);

        return true;
    }

    @Override
    public boolean insertMeasurements(CellInfo cellInfo, final int eventId, final int eventType) {
        final int cellRecordId = getPrimaryKey(cellInfo);
        final String insertMeasurementStatement =
                "INSERT INTO Measurements (cell_id, provider, accuracy, centroid, time, event_id, event_type)" +
                "   VALUES (%s, '%s', %s, GeomFromText('POINT(%s %s)', 4326), '%s', %s, %s);";

        // TODO: POSSIBLY BIG PROBLEM HERE WITH final - UPDATE: IS PROBABLY FINE!
        // Maybe the Location Futures are being frozen when they actually should still be running
        final ArrayList<Future<Location>> locationMeasurements = (ArrayList<Future<Location>>) cellInfo.getLocations().clone();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (Future<Location> futureLocation : locationMeasurements) {
                    try {
                        Location location = futureLocation.get();
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            float accuracy = location.getAccuracy();
                            String provider = location.getProvider();
                            String statement = String.format(insertMeasurementStatement, cellRecordId,
                                    provider, accuracy, longitude, latitude, location.getTime() / 1000, eventId, eventType);
                            Log.e(TAG_SL, "Inserting measurement sql: " + statement);
                            execSQL(statement);
                        }
                    } catch (java.lang.Exception e) {
                        Log.d(LOG_TAG, e.getMessage());
                    }
                }
            }
        }).start();
        return false;
    }

    @Override
    public ArrayList<Measurement> getAllMeasurements() {
        ArrayList<Measurement> measurementsArrayList = new ArrayList<Measurement>();
        final String selectAllMeasurements =
                "SELECT id, cell_id, provider, accuracy, time, event_id, event_type" +
                        "   FROM Measurements;";
        try {
            TableResult tableResult = mDb.get_table(selectAllMeasurements);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                Measurement measurement = parseMeasurement(fields);
                measurementsArrayList.add(measurement);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return measurementsArrayList;
    }

    private Measurement parseMeasurement(String[] fields) {
        long id = Long.parseLong(fields[0]);
        int cellID = Integer.valueOf(fields[1]);
        String provider = fields[2];
        double accuracy = Double.valueOf(fields[3]);
        long time = Long.valueOf(fields[4]);
        int eventID = Integer.valueOf(fields[5]);
        int eventType = Integer.valueOf(fields[6]);
        return new Measurement(id, cellID, provider, accuracy, time, eventID, eventType);
    }

    private int getPrimaryKey(CellInfo cellInfo) {
        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());
        return getId(String.format(cellExistsQuery, cid, lac, mnc, mcc, technology));
    }

    private int getPrimaryKey(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = getPrimaryKey(cellInfo);
        return getId(String.format(callExistsQuery, direction, address, starttime, endtime, cellRecordId));
    }

    private int getPrimaryKey(TextMessage message) {
        String messageIdQuery =
                "SELECT id" +
                "   FROM TextMessages" +
                "   WHERE direction = '%s' AND address = '%s' AND time = %s AND cell_id = %s" +
                "   LIMIT 1;";
        int cell_id = getPrimaryKey(message.getCell());
        return getId(String.format(messageIdQuery, message.getDirection(), message.getAddress(),
                message.getTime(), cell_id));
    }

    private int getPrimaryKey(Handover handover) {
        String handoverIdQuery =
                "SELECT id" +
                "   FROM Handovers" +
                "   WHERE startcell = %s AND endcell = %s AND time = %s" +
                "   LIMIT 1;";
        int startcell_id = getPrimaryKey(handover.getStartCell());
        int endcell_id = getPrimaryKey(handover.getEndCell());
        return getId(String.format(handoverIdQuery, startcell_id, endcell_id, handover.getTimestamp()));
    }

    private int getPrimaryKey(LocationUpdate locationUpdate) {
        String locationUpdateIdQuery =
                "SELECT id" +
                "   FROM LocationUpdates" +
                "   WHERE startcell = %s AND endcell = %s AND time = %s" +
                "   LIMIT 1;";
        int startcell_id = getPrimaryKey(locationUpdate.getStartCell());
        int endcell_id = getPrimaryKey(locationUpdate.getEndCell());
        return getId(String.format(locationUpdateIdQuery, startcell_id, endcell_id, locationUpdate.getTimestamp()));
    }

    private int getPrimaryKey(Data data) {
        String dataRecordIdQuery =
                "SELECT id" +
                "   FROM DataRecords" +
                "   WHERE rxbytes = %s AND txbytes = %s AND starttime = %s AND" +
                "   endtime = %s AND cell_id = %s" +
                "   LIMIT 1;";
        int cell_id = getPrimaryKey(data.getCell());
        return getId(String.format(dataRecordIdQuery, data.getRxBytes(), data.getTxBytes(),
                data.getSessionStart(), data.getSessionEnd(), cell_id));
    }

    private int getId(String queryWithOneIdResult) {
        try {
            TableResult result = mDb.get_table(queryWithOneIdResult);
            Vector<String[]> rows = result.rows;
            return Integer.valueOf(rows.get(0)[0]);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find id with query: " + queryWithOneIdResult);
            return -1;
        }
    }

    public CellInfo getCellById(long id) {
        String getCellByIdStatement =
                "SELECT id, cellid, lac, mnc, mcc, technology" +
                "   FROM Cells" +
                "   WHERE id = %d;";
        try {
            TableResult result = mDb.get_table(String.format(getCellByIdStatement, id));
            String[] fields = (String[]) result.rows.get(0);
            return new CellInfo(Long.parseLong(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
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
        int technology = Integer.valueOf(fields[4]);

        return new CellInfo(cellid, lac, mnc, mcc, technology);
    }

    private ArrayList<Handover> getHandoversByCallId(long id) {
        ArrayList<Handover> handovers = new ArrayList<Handover>();
        String getHandoverByCallIdStatement =
                "SELECT id, startcell, endcell, time" +
                "   FROM Handovers" +
                "   WHERE call_id = %d;";
        try {
            TableResult result = mDb.get_table(String.format(getHandoverByCallIdStatement, id));
            Vector<String[]> rows = result.rows;
            for (String[] fields : rows) {
                Handover handover = parseHandover(fields);
                handovers.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return handovers;
    }

    public Date[] getLastThreeDates() {
        final String threeDatesStatement =
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
            for (int i = 0; i < rows.size(); i++)
                result[i] = new Date(Long.valueOf(rows.get(i)[0]) * 1000);
            return result;
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return new Date[]{};
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
            for (String[] fields : rows) {
                Call call = parseCall(fields);
                callArrayList.add(call);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
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
                "   WHERE date(starttime, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                "   AND date(endtime, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult tableResult = mDb.get_table(selectCallsByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                Call call = parseCall(fields);
                callArrayList.add(call);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return callArrayList;
    }

    @NonNull
    private Call parseCall(String[] fields) {
        long call_id = Long.valueOf(fields[0]);
        String direction = fields[1];
        String address = fields[2];
        long startTime = Long.valueOf(fields[3]);
        long endTime = Long.valueOf(fields[4]);
        CellInfo startCell = getCellById(Long.valueOf(fields[5]));

        Call call = new Call(call_id, startCell, direction, address, new ArrayList<Handover>(), startTime, endTime);
        for (Handover handover : getHandoversByCallId(call_id)) {
            call.addHandover(handover);
        }
        return call;
    }


    @Override
    public ArrayList<TextMessage> getAllTextMessageRecords() {
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT id, direction, address, time, cell_id" +
                "   FROM TextMessages;";
        try {
            TableResult tableResult = mDb.get_table(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
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
                "SELECT id, direction, address, time, cell_id" +
                "   FROM TextMessages" +
                "   WHERE date(time, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult tableResult = mDb.get_table(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return textMessages;
    }

    @NonNull
    private TextMessage parseTextMessage(String[] fields) {
        long id = Long.parseLong(fields[0]);
        String direction = fields[1];
        String address = fields[2];
        long time = Long.valueOf(fields[3]);
        CellInfo cell = getCellById(Long.valueOf(fields[4]));
        return new TextMessage(id, cell, direction, address, time);
    }

    @Override
    public ArrayList<Handover> getAllHandoverRecords() {
        ArrayList<Handover> handoverArrayList = new ArrayList<Handover>();
        final String selectHandoversByDate =
                "SELECT id, startcell, endcell, time FROM Handovers;";
        try {
            TableResult result = mDb.get_table(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @NonNull
    private Handover parseHandover(String[] row) {
        long id = Long.parseLong(row[0]);
        CellInfo startCell = getCellById(Long.parseLong(row[1]));
        CellInfo endCell = getCellById(Long.parseLong(row[2]));
        Handover handover = new Handover(id, startCell, endCell);
        handover.setTimestamp(Long.parseLong(row[3]));
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
                "SELECT id, startcell, endcell, time FROM Handovers" +
                "   WHERE date(time, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @Override
    public ArrayList<LocationUpdate> geAllLocationUpdateRecords() {
        ArrayList<LocationUpdate> locationUpdateArrayList = new ArrayList<LocationUpdate>();
        final String selectLocationUpdatesByDate =
                "SELECT id, startcell, endcell, time FROM LocationUpdates;";
        try {
            TableResult result = mDb.get_table(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }

    @NonNull
    private LocationUpdate parseLocationUpdate(String[] row) {
        long id = Long.parseLong(row[0]);
        CellInfo startCell = getCellById(Long.parseLong(row[1]));
        CellInfo endCell = getCellById(Long.parseLong(row[2]));
        LocationUpdate locationUpdate = new LocationUpdate(id, startCell, endCell);
        locationUpdate.setTimestamp(Long.parseLong(row[3]));
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
                "   WHERE date(time, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }

    @Override
    public ArrayList<Data> getAllDataRecords() {
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT id, rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords;";
        try {
            TableResult result = mDb.get_table(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @Override
    public ArrayList<Data> getDataRecords(Date day) {
        return getDataRecords(day, day);
    }

    @Override
    public ArrayList<Data> getDataRecords(Date from, Date to) {
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT id, rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords" +
                "   WHERE date(starttime, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                "   AND date(endtime, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = mDb.get_table(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(TAG_SL, "could not find: " + selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @NonNull
    private Data parseDataRecord(String[] row) {
        CellInfo cellInfo = getCellById(Long.parseLong(row[5]));
        return new Data(Long.parseLong(row[0]), cellInfo, Long.parseLong(row[1]), Long.parseLong(row[2]), Long.parseLong(row[3]), Long.parseLong(row[4]));
    }
}
