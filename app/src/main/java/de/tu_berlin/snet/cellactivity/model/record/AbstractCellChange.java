package de.tu_berlin.snet.cellactivity.model.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
abstract class AbstractCellChange {
    private CellInfo startCell, endCell;
    private long timestamp;

    public CellInfo getStartCell() {
        return startCell;
    }

    public CellInfo getEndCell() {
        return endCell;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setStartCell(CellInfo startCell) {
        if (startCell == null) {
            throw new IllegalArgumentException("startCell can't be null");
        } else if (startCell.equals(getEndCell())) {
            throw new IllegalArgumentException("startCell can't be the same as endCell");
        }
        this.startCell = startCell;
    }

    public void setEndCell(CellInfo endCell) {
        if (endCell == null) {
            throw new IllegalArgumentException("endCell can't be null");
        } else if (endCell.equals(getStartCell())) {
            throw new IllegalArgumentException("endCell can't be the same as startCell");
        }
        this.endCell = endCell;
    }

    public void setTimestamp(long timestamp) {
        if (!Check.Time.isBetween2016and2025(timestamp)) {
            throw new IllegalArgumentException("not a valid epoch timestamp between 2016 and 2025");
        }
        this.timestamp = timestamp;
    }

    public AbstractCellChange(CellInfo startCell, CellInfo endCell) {
        setStartCell(startCell);
        setEndCell(endCell);
        setTimestamp(System.currentTimeMillis()/1000);
    }

    public AbstractCellChange(CellInfo startCell, CellInfo endCell, long timestamp) {
        setStartCell(startCell);
        setEndCell(endCell);
        setTimestamp(timestamp);
    }
}