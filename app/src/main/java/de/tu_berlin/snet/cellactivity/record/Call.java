package de.tu_berlin.snet.cellactivity.record;

import java.util.ArrayList;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
public class Call extends AbstractCallOrText {
    private CellInfo startCell;
    private ArrayList<Handover> handovers;
    private long startTime, endTime;

    public Call(String direction, String address, CellInfo startCell, ArrayList<Handover> handovers, long startTime, long endTime) {
        setDirection(direction);
        setAddress(address);
        setStartCell(startCell);
        setHandovers(handovers);
        setStartTime(startTime);
        setEndTime(endTime);
    }

    public CellInfo getStartCell() {
        return startCell;
    }

    public void setStartCell(CellInfo startCell) {
        if(getHandovers().size() > 0) {
            if(!Check.Network.isSameCell(startCell, getHandovers().get(0).getStartCell())) {
                throw new IllegalArgumentException("startCell must match first startCell in handovers");
            }
        }

        this.startCell = startCell;
    }

    public ArrayList<Handover> getHandovers() {
        return handovers;
    }
    private void setHandovers(ArrayList<Handover> handovers) {
        if(!Check.Network.isSameCell(getHandovers().get(0).getStartCell(), getStartCell())) {
            throw new IllegalArgumentException("StartCell in handovers must match startCell of Call");
        }
        this.handovers = handovers;
    }

    public void addHandover(Handover handover) {
        this.handovers.add(handover);
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        if (Check.Time.isBefore2016(startTime)) {
            throw new IllegalArgumentException("startTime should be after 2015");
        }
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        if (Check.Time.isBefore(endTime, getStartTime())) {
            throw new IllegalArgumentException("endTime cannot be before startTime");
        }
        this.endTime = endTime;
    }
}
