package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.FakeCellInfo;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class CellHelperImpl implements CellHelper {
    private final static String LOG_TAG = CellHelperImpl.class.getSimpleName();
    private Context context;

    public CellHelperImpl(Context context) {
        this.context = context;
    }

    @Override
    public int getPrimaryKey(CellInfo cellInfo) {
        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());
        return GeoDatabaseHelper.getInstance(context).getId(String.format(GeoDatabaseHelper.getInstance(context).cellExistsQuery, cid, lac, mnc, mcc, technology));
    }

    @Override
    public boolean insertRecord(final CellInfo cellInfo) {
        String insertCellStatement =
                "INSERT INTO Cells (cellid, lac, mnc, mcc, technology)" +
                        "   SELECT %1$s, %2$s, %3$s, %4$s, %5$s" +
                        "   WHERE NOT EXISTS (" +
                        GeoDatabaseHelper.getInstance(context).cellExistsQuery +
                        "   );";

        String cid = String.valueOf(cellInfo.getCellId());
        String lac = String.valueOf(cellInfo.getLac());
        String mnc = String.valueOf(cellInfo.getMnc());
        String mcc = String.valueOf(cellInfo.getMcc());
        String technology = String.valueOf(cellInfo.getConnectionType());

        GeoDatabaseHelper.getInstance(context).execSQL(insertCellStatement, cid, lac, mnc, mcc, technology);
        final int cellInfoId = getPrimaryKey(cellInfo);
        Log.e("DB", "INSERTED CELL " + cellInfoId);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onCellInfoInserted(cellInfo, cellInfoId);
        }

        return true;
    }

    @Override
    public CellInfo getCellById(long id) {
        String getCellByIdStatement =
                "SELECT id, cellid, lac, mnc, mcc, technology" +
                        "   FROM Cells" +
                        "   WHERE id = %d;";
        try {
            TableResult result = GeoDatabaseHelper.getInstance(context).getTable(String.format(getCellByIdStatement, id));
            String[] fields = (String[]) result.rows.get(0);
            return new CellInfo(Long.parseLong(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            return new FakeCellInfo();
        }
    }

    @Override
    public ArrayList<CellInfo> getAllCellRecords() {
        ArrayList<CellInfo> cellInfoArrayList = new ArrayList<CellInfo>();
        final String selectAllCells =
                "SELECT id, cellid, lac, mnc, mcc, technology" +
                        "   FROM Cells;";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllCells);
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

    @Override
    public ArrayList<CellInfo> getAllCellRecordsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<CellInfo> cellInfoArrayList = new ArrayList<CellInfo>();
        final String selectAllCells =
                "SELECT id, cellid, lac, mnc, mcc, technology" +
                        "   FROM Cells" +
                " LIMIT " + start + "," + end + ";";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectAllCells);
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
        long id = Long.parseLong(fields[0]);
        int cellid = Integer.valueOf(fields[1]);
        int lac = Integer.valueOf(fields[2]);
        int mnc = Integer.valueOf(fields[3]);
        int mcc = Integer.valueOf(fields[4]);
        int technology = Integer.valueOf(fields[5]);

        return new CellInfo(id, cellid, lac, mnc, mcc, technology);
    }
}
