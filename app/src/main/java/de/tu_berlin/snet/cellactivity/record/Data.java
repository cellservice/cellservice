package de.tu_berlin.snet.cellactivity.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class Data {
    private long rxBytes, txBytes;
    private long sessionStart, sessionEnd;
    private CellInfo cell;

    public Data(long rxBytes, long txBytes, long sessionStart, long sessionEnd, CellInfo cell) {
        setRxBytes(rxBytes);
        setTxBytes(txBytes);
        setSessionStart(sessionStart);
        setSessionEnd(sessionEnd);
        setCell(cell);
    }

    public long getRxBytes() {
        return rxBytes;
    }

    public void setRxBytes(long rxBytes) {
        if(rxBytes < 0) {
            throw new IllegalArgumentException("rxBytes cannot be less than 0");
        }
        this.rxBytes = rxBytes;
    }

    public long getTxBytes() {
        return txBytes;
    }

    public void setTxBytes(long txBytes) {
        if(txBytes < 0) {
            throw new IllegalArgumentException("txBytes cannot be less than 0");
        }
        this.txBytes = txBytes;
    }

    public long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(long sessionStart) {
        if(Check.Time.isBefore2016(sessionStart)) {
            throw new IllegalArgumentException("sessionStart should be after 2015");
        }
        this.sessionStart = sessionStart;
    }

    public long getSessionEnd() {
        return sessionEnd;
    }

    public void setSessionEnd(long sessionEnd) {
        if(Check.Time.isBefore(sessionEnd, getSessionStart())) {
            throw new IllegalArgumentException("sessionEnd must be after sessionStart");
        }
        this.sessionEnd = sessionEnd;
    }

    public CellInfo getCell() {
        return cell;
    }

    public void setCell(CellInfo cell) {
        this.cell = cell;
    }
}
