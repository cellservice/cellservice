package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Data;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class DataHelperImpl implements DataHelper {
    private final static String LOG_TAG = DataHelperImpl.class.getSimpleName();
    private Context context;
    private CellHelper cellHelper;
    private MeasurementsHelper measurementsHelper;

    public DataHelperImpl(Context context) {
        this.context = context;
        cellHelper = new CellHelperImpl(context);
        measurementsHelper = new MeasurementsHelperImpl(context);
    }

    @Override
    public int getPrimaryKey(Data data) {
        String dataRecordIdQuery =
                "SELECT id" +
                        "   FROM DataRecords" +
                        "   WHERE rxbytes = %s AND txbytes = %s AND starttime = %s AND" +
                        "   endtime = %s AND cell_id = %s" +
                        "   LIMIT 1;";
        int cell_id = cellHelper.getPrimaryKey(data.getCell());
        return GeoDatabaseHelper.getInstance(context).getId(String.format(dataRecordIdQuery, data.getRxBytes(), data.getTxBytes(),
                data.getSessionStart(), data.getSessionEnd(), cell_id));
    }

    @Override
    public boolean insertRecord(Data data) {
        String insertDataRecordStatement =
                "INSERT INTO DataRecords (rxbytes, txbytes, starttime, endtime, cell_id)" +
                        "   VALUES (%s, %s, %s, %s, %s);";
        CellInfo cellInfo = data.getCell();
        cellHelper.insertRecord(cellInfo);
        int cellPrimaryKey = cellHelper.getPrimaryKey(cellInfo);

        GeoDatabaseHelper.getInstance(context).execSQL(String.format(insertDataRecordStatement, data.getRxBytes(), data.getTxBytes(),
                data.getSessionStart(), data.getSessionEnd(), cellPrimaryKey));
        int dataId = getPrimaryKey(data);

        measurementsHelper.insertMeasurements(cellInfo, dataId, GeoDatabaseHelper.getInstance(context).DATA);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onDataSessionInserted(data, dataId);
        }

        return false;
    }

    @Override
    public ArrayList<Data> getAllDataRecords() {
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT id, rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords;";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectDataRecordByDate);
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
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @Override
    public ArrayList<Data> getDataRecordsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<Data> dataArrayList = new ArrayList<Data>();
        final String selectDataRecordByDate =
                "SELECT id, rxbytes, txbytes, starttime, endtime, cell_id FROM DataRecords " +
                "LIMIT " + start + "," + end + ";";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectDataRecordByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                Data data = parseDataRecord(row);
                dataArrayList.add(data);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectDataRecordByDate);
        }

        return dataArrayList;
    }

    @NonNull
    private Data parseDataRecord(String[] row) {
        CellInfo cellInfo = cellHelper.getCellById(Long.parseLong(row[5]));
        return new Data(Long.parseLong(row[0]), cellInfo, Long.parseLong(row[1]), Long.parseLong(row[2]), Long.parseLong(row[3]), Long.parseLong(row[4]));
    }
}
