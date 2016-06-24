package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface LocationUpdateHelper {
    int getPrimaryKey(LocationUpdate locationUpdate);
    boolean insertRecord(LocationUpdate locationUpdate);
    ArrayList<LocationUpdate> getAllLocationUpdateRecords();
    ArrayList<LocationUpdate> getLocationUpdateRecords(Date day);
    ArrayList<LocationUpdate> getLocationUpdateRecords(Date from, Date to);
}
