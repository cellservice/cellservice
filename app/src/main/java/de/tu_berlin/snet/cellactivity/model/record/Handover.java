package de.tu_berlin.snet.cellactivity.model.record;

/**
 * Created by giraffe on 4/16/16.
 */
public class Handover extends AbstractCellChange {
    public Handover(CellInfo startCell, CellInfo endCell) {
        super(startCell, endCell);
    }
}
