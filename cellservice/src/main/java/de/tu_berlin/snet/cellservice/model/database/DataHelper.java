package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.Data;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface DataHelper {
    int getPrimaryKey(Data data);
    boolean insertRecord(Data data);
    ArrayList<Data> getAllDataRecords();
    ArrayList<Data> getDataRecords(Date day);
    ArrayList<Data> getDataRecords(Date from, Date to);
}
