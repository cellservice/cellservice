package de.tu_berlin.snet.cellservice.util.database;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import de.tu_berlin.snet.cellservice.util.Constants;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by Friedhelm Victor on 5/16/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(android.util.Log.class)
public class MigrationManagerTest {
    @Mock
    private Context context;
    @Mock
    AssetManager assetManager;
    @Mock
    private SQLExecutable sqlExecutable;

    private MigrationManager migrationManager;

    private String[][] assetFiles = {
            {"201605161149_Initial_setup.sql", // is OK
                    "CREATE TABLE IF NOT EXISTS Cells (\n" +
                            "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                            "  cellid INTEGER,\n" +
                            "  lac INTEGER,\n" +
                            "  mnc INTEGER,\n" +
                            "  mcc INTEGER,\n" +
                            "  technology INTEGER\n" +
                            "); \n" +
                            "CREATE TABLE IF NOT EXISTS Measurements (\n" +
                            "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                            "  cell_id INTEGER REFERENCES Cells(id),\n" +
                            "  provider TEXT,\n" +
                            "  accuracy REAL,\n" +
                            "  event TEXT,\n" +
                            "  time INTEGER\n" +
                            ");"},
            {"201605201159_Add_data_records_table.sql", // is OK
                    "CREATE TABLE IF NOT EXISTS DataRecords (\n" +
                            "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                            "  rxbytes INTEGER,\n" +
                            "  txbytes INTEGER,\n" +
                            "  starttime INTEGER,\n" +
                            "  endtime INTEGER,\n" +
                            "  cell_id INTEGER REFERENCES Cells(id)\n" +
                            ");"},
            {"201605201159_Add_data_records_table(2).sql", // is not OK b/c same time prefix
                    "CREATE TABLE IF NOT EXISTS DataRecords (\n" +
                            "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,\n" +
                            "  rxbytes INTEGER,\n" +
                            "  txbytes INTEGER,\n" +
                            "  starttime INTEGER,\n" +
                            "  endtime INTEGER,\n" +
                            "  cell_id INTEGER REFERENCES Cells(id)\n" +
                            ");"},
            {"201612141800_ChristmasMigration.sql", // is OK
                    "DROP TABLE DataRecords;"},
            {"V1_Add_more_stuff.sql", // is not OK b/c no proper time prefix
                    "CREATE TABLE MoreStuff(id INTEGER NOT NULL PRIMARY KEY);"},
            {"MyMigration.sql", // is not OK b/c no time prefix at all
                    "CREATE TABLE MyMigration(id INTEGER NOT NULL PRIMARY KEY);"},
            {"test", // is not OK b/c no .sql extension
                    "-- HERE IS JUST A COMMENT"}
    };

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Log.class); // To avoid errors in regards to Log methods

        prepareAssets(assetFiles);

        when(context.getAssets()).thenReturn(assetManager);
        when(sqlExecutable.getSQLTableResult("SELECT timestamp, title, sql FROM SchemaVersion;")).thenReturn(null);
        migrationManager = new MigrationManager(context, sqlExecutable, Constants.MIGRATION_FILE_PATH);

    }

    private void prepareAssets(String[][] assetFiles) throws IOException {
        String[] fileNames = new String[assetFiles.length];
        for (int i = 0; i < assetFiles.length; i++) {
            when(assetManager.open(Constants.MIGRATION_FILE_PATH + "/" + assetFiles[i][0])).thenReturn(
                    new ByteArrayInputStream(assetFiles[i][1].getBytes(Charset.forName("UTF-8"))));
            fileNames[i] = assetFiles[i][0];
        }
        doReturn(fileNames).when(assetManager).list(anyString());
    }

    @Test
    public void shouldOnlyListMatchingMigrations() throws Exception {
        assertEquals(migrationManager.retrieveTrueMigrationFiles(Constants.MIGRATION_FILE_PATH).length, 3);
    }

    @Test
    public void shouldExecuteAllMigrationsIfNoneInDatabase() throws Exception {
        // check that none are in database
        assertEquals(migrationManager.getAppliedMigrations().size(), 0);

        migrationManager.run();
        verify(sqlExecutable).execSQL(assetFiles[0][1].split(";")[0] + ";");
        verify(sqlExecutable).execSQL(assetFiles[0][1].split(";")[1] + ";");
        verify(sqlExecutable).execSQL(assetFiles[1][1]);
        verify(sqlExecutable).execSQL(assetFiles[3][1]);
    }

    @Test
    public void shouldExecuteMigrationsOrderedByDate() throws Exception {

        InOrder inOrder = inOrder(sqlExecutable);

        migrationManager.run();
        inOrder.verify(sqlExecutable).execSQL(assetFiles[0][1].split(";")[0] + ";");
        inOrder.verify(sqlExecutable).execSQL(assetFiles[0][1].split(";")[1] + ";");
        inOrder.verify(sqlExecutable).execSQL(assetFiles[1][1]);
        inOrder.verify(sqlExecutable).execSQL(assetFiles[3][1]);
    }


    @Test
    public void shouldExecuteUpdateOnSchemaVersionTableForValidMigrations() throws Exception {
        migrationManager.run();
        verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201605161149, 'Initial setup', '" + assetFiles[0][1] + "');");
        verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201605201159, 'Add data records table', '" + assetFiles[1][1] + "');");
        verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201612141800, 'ChristmasMigration', '" + assetFiles[3][1] + "');");

        verify(sqlExecutable, never()).execSQL("INSERT INTO SchemaVersions(timestamp, title, sql) VALUES" +
                " (201605201159, 'Add data records table(2)', '" + assetFiles[2][1] + "');");
    }

    // should not execute something if current state is higher
    @Test
    public void shouldNotExecuteOlderMigrationIfNewerApplied() throws Exception {
        when(sqlExecutable.getSQLTableResult("SELECT timestamp, title, sql FROM SchemaVersion;")).thenReturn(new String[][]{
                {"201605161149", "Initial setup", assetFiles[0][1]},
                {"201612141800", "ChristmasMigration", assetFiles[3][1]}
        });
        // In other words: Migration 'Add data records table' has not been applied
        // and should never be applied, as it may result in a conflict.
        migrationManager.run();
        verify(sqlExecutable, never()).execSQL(anyString());
    }

    @Test
    public void shouldHaveLastMigrationOnChristmas() throws Exception {
        when(sqlExecutable.getSQLTableResult("SELECT timestamp, title, sql FROM SchemaVersion;")).thenReturn(new String[][]{
                {"201605161149", "Initial setup", assetFiles[0][1]},
                {"201612141800", "ChristmasMigration", assetFiles[3][1]}
        });
        assertEquals(migrationManager.getAppliedMigrations().get(1).getTime(), 201612141800L);
    }

    @Test
    public void shouldApplyRemainingMigrationsIfSomeExist() throws Exception {
        when(sqlExecutable.getSQLTableResult("SELECT timestamp, title, sql FROM SchemaVersion;")).thenReturn(new String[][]{
                {"201605161149", "Initial setup", assetFiles[0][1]}});

        InOrder inOrder = inOrder(sqlExecutable);
        migrationManager.run();

        inOrder.verify(sqlExecutable).execSQL(assetFiles[1][1]);
        inOrder.verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201605201159, 'Add data records table', '" + assetFiles[1][1] + "');");
        inOrder.verify(sqlExecutable).execSQL(assetFiles[3][1]);
        inOrder.verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201612141800, 'ChristmasMigration', '" + assetFiles[3][1] + "');");
    }

    @Test
    public void shouldCreateSchemaOnInitialize() throws Exception {
        migrationManager.initialize();

        InOrder inOrder = inOrder(sqlExecutable);
        inOrder.verify(sqlExecutable).execSQL("CREATE TABLE IF NOT EXISTS SchemaVersion(" +
                "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "  timestamp INTEGER NOT NULL," +
                "  title TEXT," +
                "  sql TEXT" +
                ");");
    }

    @Test
    public void shouldStripAwayCommentsFromSQLStatements() throws Exception {
        String statement = "DROP TABLE Calls;";
        String migrationContent = "-- THIS IS A COMMENT\n" +
                statement + " --inline;!comment\n" +
                "-- EndComment";
        prepareAssets(new String[][]{
                {"201605161149_Initial_setup.sql",
                        migrationContent}
        });

        migrationManager.run();

        InOrder inOrder = inOrder(sqlExecutable);
        inOrder.verify(sqlExecutable).execSQL(statement);
        inOrder.verify(sqlExecutable, never()).execSQL("-- EndComment;");
        inOrder.verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201605161149, 'Initial setup', '" + migrationContent + "');");
    }

    @Test
    public void shouldStripQuotesOnSchemaInsertion() throws Exception {
        String statementWithQuotes = "INSERT INTO Foobar(id, name) VALUES (1, 'KungFu');";
        String statementWithoutQuotes = "INSERT INTO Foobar(id, name) VALUES (1, ''KungFu'');";
        prepareAssets(new String[][]{
                {"201605161149_Initial_setup.sql",
                        statementWithQuotes}
        });

        migrationManager.run();

        verify(sqlExecutable).execSQL(statementWithQuotes);
        verify(sqlExecutable).execSQL("INSERT INTO SchemaVersion(timestamp, title, sql) VALUES" +
                " (201605161149, 'Initial setup', '" + statementWithoutQuotes + "');");
    }
}