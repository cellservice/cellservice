package de.tu_berlin.snet.cellservice.model.record;

/**
 * Created by giraffe on 4/16/16.
 */
public class Handover extends AbstractCellChange {
    public Handover(long id, CellInfo startCell, CellInfo endCell) {
        super(id, startCell, endCell);
    }
}
