package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.Call;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface CallHelper {
    int getPrimaryKey(Call call);
    boolean insertRecord(Call call);
    ArrayList<Call> getAllCallRecords();
    ArrayList<Call> getCallRecords(Date day);
    ArrayList<Call> getCallRecords(Date from, Date to);
}
