package de.tu_berlin.snet.cellservice.model.record;

import de.tu_berlin.snet.cellservice.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class TextMessage extends AbstractCallOrText {
    private CellInfo cell;
    private long time;

    public TextMessage(long id, CellInfo cell, String direction, String address) {
        this(id, cell, direction, address, System.currentTimeMillis() / 1000);
    }

    public TextMessage(long id, CellInfo cell, String direction, String address, long time) {
        setId(id);
        setDirection(direction);
        setAddress(address);
        setCell(cell);
        setTime(time);
    }

    public CellInfo getCell() {
        return cell;
    }

    public void setCell(CellInfo cell) {
        this.cell = cell;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        if (!Check.Time.isBetween2016and2025(time)) {
            throw new IllegalArgumentException("not a valid epoch timestamp between 2016 and 2025");
        }
        this.time = time;
    }
}
