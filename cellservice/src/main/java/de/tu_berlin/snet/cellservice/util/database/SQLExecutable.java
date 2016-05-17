package de.tu_berlin.snet.cellservice.util.database;

public interface SQLExecutable {
    void execSQL(String sql);
    String[][] getSQLTableResult(String sql);
}