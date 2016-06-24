package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
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

    public final static int CALL = 1;
    public final static int HANDOVER = 2;
    public final static int LOCATION_UPDATE = 3;
    public final static int DATA = 4;
    public final static int TEXT = 5;
    public final static int UNKNOWN = -1;

    private static final String TAG = "GEODBH";
    private static final String TAG_SL = TAG + "_JSQLITE";
    private static String DB_PATH = Environment.getExternalStorageDirectory().getPath();
    private static String DB_NAME = "spatial.sqlite";
    private Database mDb;
    private static GeoDatabaseHelper sInstance;
    private List<CDRDatabaseInsertionListener> listeners = new ArrayList<CDRDatabaseInsertionListener>();

    protected final static String cellExistsQuery =
            "SELECT id FROM Cells" +
            "   WHERE cellid = %1$s" +
            "   AND lac = %2$s" +
            "   AND mnc = %3$s" +
            "   AND mcc = %4$s" +
            "   AND technology = %5$s";

    protected final static String callExistsQuery =
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

    @Override
    public void addListener(CDRDatabaseInsertionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(CDRDatabaseInsertionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public List<CDRDatabaseInsertionListener> getListeners() {
        return listeners;
    }

    @Override
    public TableResult getTable(String sql) throws jsqlite.Exception {
        return mDb.get_table(sql);
    }

    /*
    TODO: WHY NOT USE mDb.exec? What about the callback feature?
     */
    @Override
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

    @Override
    public synchronized void execSQL(String statement, String... args) {
        try {
            Stmt stmt = mDb.prepare(String.format(statement, args));
            stmt.step();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    @Override
    public int getId(String queryWithOneIdResult) {
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

    @Override
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
}
