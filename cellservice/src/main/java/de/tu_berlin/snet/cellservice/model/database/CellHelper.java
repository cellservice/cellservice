package de.tu_berlin.snet.cellservice.model.database;

import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface CellHelper {
    int getPrimaryKey(CellInfo cellInfo);
    boolean insertRecord(CellInfo cellInfo);
    CellInfo getCellById(long id);
    ArrayList<CellInfo> getAllCellRecords();
}
