package de.tu_berlin.snet.cellactivity.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;

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
        if (timestamp < 1451606400000L) {
            throw new IllegalArgumentException("timestamp cannot be before 2016");
        }
        this.timestamp = timestamp;
    }

    public AbstractCellChange(CellInfo startCell, CellInfo endCell) {
        setStartCell(startCell);
        setEndCell(endCell);
        setTimestamp(System.currentTimeMillis());
    }

    public AbstractCellChange(CellInfo startCell, CellInfo endCell, long timestamp) {
        setStartCell(startCell);
        setEndCell(endCell);
        setTimestamp(timestamp);
    }
}