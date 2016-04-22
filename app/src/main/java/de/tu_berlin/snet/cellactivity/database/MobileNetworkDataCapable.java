package de.tu_berlin.snet.cellactivity.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellactivity.record.*;
import de.tu_berlin.snet.cellactivity.util.CellInfo;

/**
 * Created by Friedhelm Victor on 4/21/16.
 */
public interface MobileNetworkDataCapable {
    public boolean insertRecord(Call call);
    public boolean insertRecord(TextMessage textMessage);
    public boolean insertRecord(Handover handover);
    public boolean insertRecord(LocationUpdate locationUpdate);
    public boolean insertRecord(Data data);
    public boolean insertMeasurement(CellInfo cellInfo);

    public void createTables();

    public ArrayList<Call> getCallRecords(Date day);
    public ArrayList<Call> getCallRecords(Date from, Date to);

    public ArrayList<Call> getTextMessageRecords(Date day);
    public ArrayList<Call> getTextMessageRecords(Date from, Date to);

    public ArrayList<Call> getHandoverRecords(Date day);
    public ArrayList<Call> getHandoverRecords(Date from, Date to);

    public ArrayList<Call> getLocationUpdateRecords(Date day);
    public ArrayList<Call> getLocationUpdateRecords(Date from, Date to);

    public ArrayList<Call> getDataRecords(Date day);
    public ArrayList<Call> getDataRecords(Date from, Date to);
}
