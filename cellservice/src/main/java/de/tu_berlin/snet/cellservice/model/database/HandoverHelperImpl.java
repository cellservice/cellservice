package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class HandoverHelperImpl implements HandoverHelper {
    private final static String LOG_TAG = HandoverHelperImpl.class.getSimpleName();
    private Context context;
    private CellHelper cellHelper;
    private MeasurementsHelper measurementsHelper;

    public HandoverHelperImpl(Context context) {
        this.context = context;
        cellHelper = new CellHelperImpl(context);
        measurementsHelper = new MeasurementsHelperImpl(context);
    }

    @Override
    public int getPrimaryKey(Handover handover) {
        String handoverIdQuery =
                "SELECT id" +
                        "   FROM Handovers" +
                        "   WHERE startcell = %s AND endcell = %s AND time = %s" +
                        "   LIMIT 1;";
        int startcell_id = cellHelper.getPrimaryKey(handover.getStartCell());
        int endcell_id = cellHelper.getPrimaryKey(handover.getEndCell());
        return GeoDatabaseHelper.getInstance(context).getId(String.format(handoverIdQuery, startcell_id, endcell_id, handover.getTimestamp()));
    }

    @Override
    public boolean insertRecord(Handover handover, int callId) {
        String insertHandoverStatement =
                "INSERT INTO Handovers (call_id, startcell, endcell, time)" +
                        "   VALUES (%s, %s, %s, %s);";

        cellHelper.insertRecord(handover.getStartCell());
        cellHelper.insertRecord(handover.getEndCell());
        int startCellId = cellHelper.getPrimaryKey(handover.getStartCell());
        int endCellid = cellHelper.getPrimaryKey(handover.getEndCell());
        GeoDatabaseHelper.getInstance(context).execSQL(String.format(insertHandoverStatement, callId, startCellId, endCellid, handover.getTimestamp()));
        int handoverId = getPrimaryKey(handover);

        measurementsHelper.insertMeasurements(handover.getStartCell(), handoverId, GeoDatabaseHelper.getInstance(context).HANDOVER);
        measurementsHelper.insertMeasurements(handover.getEndCell(), handoverId, GeoDatabaseHelper.getInstance(context).HANDOVER);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onHandoverInserted(handover, handoverId);
        }

        return false;
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
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @Override
    public ArrayList<Handover> getAllHandoverRecords() {
        ArrayList<Handover> handoverArrayList = new ArrayList<Handover>();
        final String selectHandoversByDate =
                "SELECT id, startcell, endcell, time FROM Handovers;";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectHandoversByDate);
        }

        return handoverArrayList;
    }


    @Override
    public ArrayList<Handover> getHandoversByCallId(long id) {
        ArrayList<Handover> handovers = new ArrayList<Handover>();
        String getHandoverByCallIdStatement =
                "SELECT id, startcell, endcell, time" +
                        "   FROM Handovers" +
                        "   WHERE call_id = %d;";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(String.format(getHandoverByCallIdStatement, id));
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

    @Override
    public ArrayList<Handover> getHandoverRecordsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<Handover> handoverArrayList = new ArrayList<Handover>();
        final String selectHandoversByDate =
                "SELECT id, startcell, endcell, time FROM Handovers" +
                        " LIMIT " + start + "," + end + ";";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectHandoversByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Handover handover = parseHandover(row);
                handoverArrayList.add(handover);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectHandoversByDate);
        }

        return handoverArrayList;
    }

    @NonNull
    private Handover parseHandover(String[] row) {
        long id = Long.parseLong(row[0]);
        CellInfo startCell = cellHelper.getCellById(Long.parseLong(row[1]));
        CellInfo endCell = cellHelper.getCellById(Long.parseLong(row[2]));
        Handover handover = new Handover(id, startCell, endCell);
        handover.setTimestamp(Long.parseLong(row[3]));
        return handover;
    }
}
