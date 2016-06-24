package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.Future;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Measurement;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class MeasurementsHelperImpl implements MeasurementsHelper {
    private final static String LOG_TAG = MeasurementsHelperImpl.class.getSimpleName();
    private Context context;
    private CellHelper cellHelper;

    public MeasurementsHelperImpl(Context context) {
        this.context = context;
        cellHelper = new CellHelperImpl(context);
    }

    @Override
    public boolean insertMeasurements(CellInfo cellInfo, final int eventId, final int eventType) {
        final int cellRecordId = cellHelper.getPrimaryKey(cellInfo);
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
                            Log.e(LOG_TAG, "Inserting measurement sql: " + statement);
                            GeoDatabaseHelper.getInstance(context).execSQL(statement);
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
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllMeasurements);
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

    @Override
    public ArrayList<Measurement> getMeasurementsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<Measurement> measurementsArrayList = new ArrayList<Measurement>();
        final String selectAllMeasurements =
                "SELECT id, cell_id, provider, accuracy, time, event_id, event_type" +
                        "   FROM Measurements" +
                        " LIMIT " + start + "," + end + ";";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllMeasurements);
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
}
