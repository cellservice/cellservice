package de.tu_berlin.snet.cellservice;

import de.tu_berlin.snet.cellservice.model.record.*;

/**
 * Created by giraffe on 5/18/16.
 */
public interface CDRDatabaseInsertionListener {

    void onCallRecordInserted(Call call, int primaryKey);

    void onTextMessageInserted(TextMessage textMessage, int primaryKey);

    void onHandoverInserted(Handover handover, int primaryKey);

    void onLocationUpdateInserted(LocationUpdate locationUpdate, int primaryKey);

    void onDataSessionInserted(Data data, int primaryKey);

    void onCellInfoInserted(CellInfo cellInfo, int primaryKey);
}
