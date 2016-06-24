package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class LocationUpdateHelperImpl implements LocationUpdateHelper {
    private final static String LOG_TAG = LocationUpdateHelperImpl.class.getSimpleName();
    private Context context;
    private CellHelper cellHelper;
    private MeasurementsHelper measurementsHelper;

    public LocationUpdateHelperImpl(Context context) {
        this.context = context;
        cellHelper = new CellHelperImpl(context);
        measurementsHelper = new MeasurementsHelperImpl(context);
    }

    @Override
    public int getPrimaryKey(LocationUpdate locationUpdate) {
        String locationUpdateIdQuery =
                "SELECT id" +
                        "   FROM LocationUpdates" +
                        "   WHERE startcell = %s AND endcell = %s AND time = %s" +
                        "   LIMIT 1;";
        int startcell_id = cellHelper.getPrimaryKey(locationUpdate.getStartCell());
        int endcell_id = cellHelper.getPrimaryKey(locationUpdate.getEndCell());
        return GeoDatabaseHelper.getInstance(context).getId(String.format(locationUpdateIdQuery, startcell_id, endcell_id, locationUpdate.getTimestamp()));
    }

    @Override
    public boolean insertRecord(LocationUpdate locationUpdate) {
        String insertLocationUpdateStatement =
                "INSERT INTO LocationUpdates (startcell, endcell, time)" +
                        "   VALUES (%s, %s, %s);";

        cellHelper.insertRecord(locationUpdate.getStartCell());
        cellHelper.insertRecord(locationUpdate.getEndCell());
        int startCellId = cellHelper.getPrimaryKey(locationUpdate.getStartCell());
        int endCellid = cellHelper.getPrimaryKey(locationUpdate.getEndCell());
        GeoDatabaseHelper.getInstance(context).execSQL(String.format(insertLocationUpdateStatement, startCellId, endCellid, locationUpdate.getTimestamp()));
        int locationUpdateId = getPrimaryKey(locationUpdate);

        measurementsHelper.insertMeasurements(locationUpdate.getStartCell(), locationUpdateId, GeoDatabaseHelper.getInstance(context).LOCATION_UPDATE);
        measurementsHelper.insertMeasurements(locationUpdate.getEndCell(), locationUpdateId, GeoDatabaseHelper.getInstance(context).LOCATION_UPDATE);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onLocationUpdateInserted(locationUpdate, locationUpdateId);
        }

        return false;
    }

    @Override
    public ArrayList<LocationUpdate> getAllLocationUpdateRecords() {
        ArrayList<LocationUpdate> locationUpdateArrayList = new ArrayList<LocationUpdate>();
        final String selectLocationUpdatesByDate =
                "SELECT id, startcell, endcell, time FROM LocationUpdates;";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }

    @NonNull
    private LocationUpdate parseLocationUpdate(String[] row) {
        long id = Long.parseLong(row[0]);
        CellInfo startCell = cellHelper.getCellById(Long.parseLong(row[1]));
        CellInfo endCell = cellHelper.getCellById(Long.parseLong(row[2]));
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
                "SELECT id, startcell, endcell, time FROM LocationUpdates" +
                        "   WHERE date(time, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                        "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(selectLocationUpdatesByDate);
            Vector<String[]> rows = result.rows;
            for (String[] row : rows) {
                LocationUpdate locationUpdate = parseLocationUpdate(row);
                locationUpdateArrayList.add(locationUpdate);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            Log.d(LOG_TAG, "could not find: " + selectLocationUpdatesByDate);
        }

        return locationUpdateArrayList;
    }
}
