package de.tu_berlin.snet.cellservice.model.record;

/**
 * Created by giraffe on 4/16/16.
 */
public class Handover extends AbstractCellChange {
    /**
     * Default constructor taking two {@link CellInfo}s that must be different.
     * The internal database record id is set to -1 as it doesn't exist
     * @param startCell
     * @param endCell
     */
    public Handover(CellInfo startCell, CellInfo endCell) {
        this(-1, startCell, endCell);
    }

    /**
     * Constructor that lets you set a database record id
     * @param id the database record id this object corresponds to
     * @param startCell
     * @param endCell
     */
    public Handover(long id, CellInfo startCell, CellInfo endCell) {
        super(id, startCell, endCell);
    }
}
