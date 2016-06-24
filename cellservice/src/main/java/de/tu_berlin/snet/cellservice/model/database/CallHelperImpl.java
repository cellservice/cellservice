package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class CallHelperImpl implements CallHelper {
    private final static String LOG_TAG = CallHelperImpl.class.getSimpleName();
    private Context context;
    private HandoverHelper handoverHelper;
    private CellHelper cellHelper;
    private MeasurementsHelper measurementsHelper;

    public CallHelperImpl(Context context) {
        this.context = context;
        handoverHelper = new HandoverHelperImpl(context);
        cellHelper = new CellHelperImpl(context);
        measurementsHelper = new MeasurementsHelperImpl(context);
    }

    @Override
    public int getPrimaryKey(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        int cellRecordId = cellHelper.getPrimaryKey(cellInfo);
        return GeoDatabaseHelper.getInstance(context).getId(String.format(GeoDatabaseHelper.getInstance(context).callExistsQuery, direction, address, starttime, endtime, cellRecordId));
    }

    @Override
    public boolean insertRecord(Call call) {
        CellInfo cellInfo = call.getStartCell();
        String direction = call.getDirection();
        String address = call.getAddress();
        Long starttime = call.getStartTime();
        Long endtime = call.getEndTime();
        cellHelper.insertRecord(cellInfo);
        int cellRecordId = cellHelper.getPrimaryKey(cellInfo);

        String insertCallRecordStatement =
                "INSERT INTO Calls (direction, address, starttime, endtime, startcell)" +
                        "   VALUES ('%1$s', '%2$s', %3$s, %4$s, %5$s);";
        GeoDatabaseHelper.getInstance(context).execSQL(String.format(insertCallRecordStatement, direction, address, starttime, endtime, cellRecordId));
        int callId = getPrimaryKey(call);

        measurementsHelper.insertMeasurements(cellInfo, callId, GeoDatabaseHelper.getInstance(context).CALL);

        for (Handover handover : call.getHandovers()) {
            handoverHelper.insertRecord(handover, callId);
        }

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onCallRecordInserted(call, callId);
        }

        return false;
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
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectCallsByDate);
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
    public ArrayList<Call> getAllCallRecords() {
        ArrayList<Call> callArrayList = new ArrayList<Call>();
        final String selectAllCalls =
                "SELECT id, direction, address, starttime, endtime, startcell" +
                        "   FROM Calls;";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllCalls);
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
    public ArrayList<Call> getCallRecordsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<Call> callArrayList = new ArrayList<Call>();
        final String selectAllCalls =
                "SELECT id, direction, address, starttime, endtime, startcell" +
                        "   FROM Calls" +
                "LIMIT " + start + "," + end + ";";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllCalls);
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
        CellInfo startCell = cellHelper.getCellById(Long.valueOf(fields[5]));

        Call call = new Call(call_id, startCell, direction, address, new ArrayList<Handover>(), startTime, endTime);
        for (Handover handover : handoverHelper.getHandoversByCallId(call_id)) {
            call.addHandover(handover);
        }
        return call;
    }
}
