package de.tu_berlin.snet.cellservice;

import de.tu_berlin.snet.cellservice.model.record.Call;
import de.tu_berlin.snet.cellservice.model.record.Data;
import de.tu_berlin.snet.cellservice.model.record.LocationUpdate;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;

/**
 *
 *
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface CDRListener {
    void onDataSession(Data data);

    void onCallRecord(Call call);

    void onTextMessage(TextMessage textMessage);

    void onLocationUpdate(LocationUpdate locationUpdate);
}
