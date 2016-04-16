package de.tu_berlin.snet.cellactivity.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class TextMessage extends AbstractCallOrText {
    private CellInfo cell;
    private long time;

    public TextMessage(String direction, String address, CellInfo cell) {
        setDirection(direction);
        setAddress(address);
        setCell(cell);
        setTime(System.currentTimeMillis());
    }

    public TextMessage(String direction, String address, CellInfo cell, long time) {
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
        if(Check.Time.isBefore2016(time)) {
            throw new IllegalArgumentException("time should be after 2015");
        }
        this.time = time;
    }
}
