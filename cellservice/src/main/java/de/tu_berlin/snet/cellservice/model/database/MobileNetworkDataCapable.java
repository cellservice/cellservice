package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.Measurement;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import jsqlite.TableResult;

/**
 * Created by Friedhelm Victor on 4/21/16.
 */
public interface MobileNetworkDataCapable {
    void addListener(CDRDatabaseInsertionListener listener);
    void removeListener(CDRDatabaseInsertionListener listener);
    List<CDRDatabaseInsertionListener> getListeners();
    TableResult getTable(String sql) throws jsqlite.Exception;
    void execSQL(String statement);
    void execSQL(String statement, String... args);
    int getId(String queryWithOneIdResult);
    Date[] getLastThreeDates();
}
