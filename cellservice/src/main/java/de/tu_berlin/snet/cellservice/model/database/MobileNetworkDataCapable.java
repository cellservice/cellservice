package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.Handover;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.Measurement;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * Created by Friedhelm Victor on 4/21/16.
 */
public interface MobileNetworkDataCapable {
    boolean insertRecord(Call call);
    boolean insertRecord(TextMessage textMessage);
    boolean insertRecord(Handover handover, int callId);
    boolean insertRecord(LocationUpdate locationUpdate);
    boolean insertRecord(Data data);
    boolean insertRecord(CellInfo cellInfo);
    boolean insertMeasurements(CellInfo cellInfo, int eventId, int eventType);

    Date[] getLastThreeDates();

    CellInfo getCellById(long id);
    ArrayList<CellInfo> getAllCellRecords();

    ArrayList<Call> getAllCallRecords();
    ArrayList<Call> getCallRecords(Date day);
    ArrayList<Call> getCallRecords(Date from, Date to);

    ArrayList<TextMessage> getAllTextMessageRecords();
    ArrayList<TextMessage> getTextMessageRecords(Date day);
    ArrayList<TextMessage> getTextMessageRecords(Date from, Date to);

    ArrayList<Handover> getAllHandoverRecords();
    ArrayList<Handover> getHandoverRecords(Date day);
    ArrayList<Handover> getHandoverRecords(Date from, Date to);

    ArrayList<LocationUpdate> geAllLocationUpdateRecords();
    ArrayList<LocationUpdate> getLocationUpdateRecords(Date day);
    ArrayList<LocationUpdate> getLocationUpdateRecords(Date from, Date to);

    ArrayList<Data> getAllDataRecords();
    ArrayList<Data> getDataRecords(Date day);
    ArrayList<Data> getDataRecords(Date from, Date to);

    ArrayList<Measurement> getAllMeasurements();
}
