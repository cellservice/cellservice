package de.tu_berlin.snet.cellactivity.model.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class TextMessage extends AbstractCallOrText {
    private CellInfo cell;
    private long time;

    public TextMessage(CellInfo cell, String direction, String address) {
        setDirection(direction);
        setAddress(address);
        setCell(cell);
        setTime(System.currentTimeMillis()/1000);
    }

    public TextMessage(CellInfo cell, String direction, String address, long time) {
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
        if(!Check.Time.isBetween2016and2025(time)) {
            throw new IllegalArgumentException("not a valid epoch timestamp between 2016 and 2025");
        }
        this.time = time;
    }
}
