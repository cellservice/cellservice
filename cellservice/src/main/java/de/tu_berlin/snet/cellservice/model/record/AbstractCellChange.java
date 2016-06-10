package de.tu_berlin.snet.cellservice.model.record;

import com.google.gson.annotations.SerializedName;

import de.tu_berlin.snet.cellservice.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
abstract class AbstractCellChange {
    @SerializedName("id")
    private long id;
    @SerializedName("startcell")
    private CellInfo startCell;
    @SerializedName("endcell")
    private CellInfo endCell;
    @SerializedName("time")
    private long timestamp;

    public AbstractCellChange(long id, CellInfo startCell, CellInfo endCell) {
        this(id, startCell, endCell, System.currentTimeMillis() / 1000);
    }

    public AbstractCellChange(long id, CellInfo startCell, CellInfo endCell, long timestamp) {
        setStartCell(startCell);
        setEndCell(endCell);
        setTimestamp(timestamp);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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
}