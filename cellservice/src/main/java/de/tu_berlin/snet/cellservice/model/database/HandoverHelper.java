package de.tu_berlin.snet.cellservice.model.database;

import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface HandoverHelper {
    int getPrimaryKey(Handover handover);
    boolean insertRecord(Handover handover, int callId);
    ArrayList<Handover> getHandoverRecords(Date day);
    ArrayList<Handover> getHandoverRecords(Date from, Date to);
    ArrayList<Handover> getAllHandoverRecords();
    ArrayList<Handover> getHandoversByCallId(long id);
    ArrayList<Handover> getHandoverRecordsPaginated(int start, int end) throws IllegalArgumentException;
}
