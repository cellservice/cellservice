package de.tu_berlin.snet.cellservice.util.serialization;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * Interface of object serialization visitor pattern defining what object are serializable
 */
public interface ObjectSerializer {
    String serialize(CellInfo cellInfo);
}
