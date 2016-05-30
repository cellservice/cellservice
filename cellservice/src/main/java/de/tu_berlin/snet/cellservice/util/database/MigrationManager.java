package de.tu_berlin.snet.cellservice.util.database;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Friedhelm Victor on 5/13/16.
 */
public class MigrationManager {
    private Context context;
    private SQLExecutable database;
    private String migrationFilePath;

    public MigrationManager(Context context, SQLExecutable database, String migrationFilePath) {
        this.context = context;
        this.database = database;
        this.migrationFilePath = migrationFilePath;
    }

    public void initialize() {
        database.execSQL("CREATE TABLE IF NOT EXISTS SchemaVersion(" +
                "  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "  timestamp INTEGER NOT NULL," +
                "  title TEXT," +
                "  sql TEXT" +
                ");");
    }

    public void run() {
        for (Migration migration : getApplicableMigrations()) {
            for(String partialMigration : migration.getSqlStatements()) {
                Log.d("MIGRATION","Executing: "+partialMigration);
                database.execSQL(partialMigration);
            }
            String insertSchemaLog = "INSERT INTO SchemaVersion(timestamp, title, sql) VALUES " +
                    "("+migration.getTime()+", '"+migration.getTitle()+"', '"+migration.getSqlStatementsEscaped()+"');";
            database.execSQL(insertSchemaLog);
        }
    }

    private ArrayList<Migration> getApplicableMigrations() {
        ArrayList<Migration> appliedMigrations = getAppliedMigrations();
        ArrayList<Migration> migrationsFromFiles = getTrueMigrationsFromFiles();
        ArrayList<Migration> applicableMigrations = new ArrayList<Migration>();

        Migration lastAppliedMigration;

        if(appliedMigrations != null && appliedMigrations.size() > 0) {
            lastAppliedMigration = appliedMigrations.get(appliedMigrations.size()-1);
        } else {
            return migrationsFromFiles;
        }

        for(Migration migration : migrationsFromFiles) {
            if(migration.getTime() > lastAppliedMigration.getTime()) {
                applicableMigrations.add(migration);
            }
        }
        return applicableMigrations;
    }

    private ArrayList<Migration> getTrueMigrationsFromFiles() {
        String[] files = retrieveTrueMigrationFiles(migrationFilePath);
        ArrayList<Migration> migrations = new ArrayList<Migration>();
        for (String file : files) {
            migrations.add(getMigrationFromFilename(file));
        }
        Collections.sort(migrations);
        return migrations;
    }

    /**
     * Returns a list of all true migration files in a given directory inside /assets/
     *
     * @param dirFrom a relative filepath of a directory inside /assets/
     * @return a list of file names
     */
    protected String[] retrieveTrueMigrationFiles(String dirFrom) {
        ArrayList<String> fileListVector = new ArrayList<String>();
        try {
            AssetManager am = context.getAssets();
            String[] fileList = am.list(dirFrom);

            if (fileList != null) {
                for (int i = 0; i < fileList.length; i++) {
                    String[] previousFiles = Arrays.copyOfRange(fileList, 0, i);
                    if (hasSQLExtension(fileList[i]) && hasTimePrefix(fileList[i]) &&
                            !filenamePrefixExists(fileList[i], previousFiles)) {
                        fileListVector.add(fileList[i]);
                    }
                }
            }
        } catch (Exception e) {
        }
        return fileListVector.toArray(new String[fileListVector.size()]);
    }

    private boolean hasSQLExtension(String filename) {
        String fileExtension = filename.substring(filename.length() - 4, filename.length());
        return fileExtension.equals(".sql");
    }

    private boolean hasTimePrefix(String filename) {
        try {
            long timePrefix = Long.parseLong(filenamePrefix(filename));
            return String.valueOf(timePrefix).length() == "YYYYMMDDhhmm".length();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String filenamePrefix(String filename) {
        return filename.split("_", 2)[0];
    }

    private boolean filenamePrefixExists(String newFilename, String[] filenames) {
        for (String filename : filenames) {
            if (filenamePrefix(newFilename).equals(filenamePrefix(filename))) {
                return true;
            }
        }
        return false;
    }

    private Migration getMigrationFromFilename(String filename) {
        BufferedReader reader = null;
        StringBuilder sqlBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(migrationFilePath + "/" + filename)));

            // do reading, loop until end of file reading adding newlines except for the last line
            String mLine;
            if ((mLine = reader.readLine()) != null) sqlBuilder.append(mLine);
            while ((mLine = reader.readLine()) != null) {
                sqlBuilder.append("\n");
                sqlBuilder.append(mLine);
            }


        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }

        return new Migration(getTimeFromMigrationFilename(filename),
                getTitleFromMigrationFilename(filename), sqlBuilder.toString());
    }

    private long getTimeFromMigrationFilename(String filename) {
        return Long.valueOf(filenamePrefix(filename));
    }

    private String getTitleFromMigrationFilename(String filename) {
        return filename.split("_", 2)[1].split(".sql", 2)[0].replace("_", " ");
    }

    protected ArrayList<Migration> getAppliedMigrations() {
        ArrayList<Migration> appliedMigrations = new ArrayList<Migration>();
        String[][] sqlTableResult = database.getSQLTableResult("SELECT timestamp, title, sql FROM SchemaVersion;");
        if (sqlTableResult != null) {
            for (String[] row : sqlTableResult) {
                appliedMigrations.add(new Migration(Long.parseLong(row[0]), row[1], row[2]));
            }
        }
        return appliedMigrations;
    }

}
