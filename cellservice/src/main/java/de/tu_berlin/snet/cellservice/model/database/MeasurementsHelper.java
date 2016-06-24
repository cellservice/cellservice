package de.tu_berlin.snet.cellservice.model.database;

import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.Measurement;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface MeasurementsHelper {
    boolean insertMeasurements(CellInfo cellInfo, int eventId, int eventType);
    ArrayList<Measurement> getAllMeasurements();
}
