package de.tu_berlin.snet.cellservice.model.record;

import de.tu_berlin.snet.cellservice.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
public class LocationUpdate extends AbstractCellChange {

    /**
     * Default constructor taking two {@link CellInfo}s that must be different.
     * The internal database record id is set to -1 as it doesn't exist
     * @param startCell
     * @param endCell
     */
    public LocationUpdate(CellInfo startCell, CellInfo endCell) {
        this(-1, startCell, endCell);
    }

    /**
     * Constructor that lets you set a database record id
     * @param id the database record id this object corresponds to
     * @param startCell
     * @param endCell
     */
    public LocationUpdate(long id, CellInfo startCell, CellInfo endCell) {
        super(id, startCell, endCell);
        validateLocationUpdate();
    }

    /**
     * Validates whether the object is consistent. This is determined by checking that the
     * Location Area Code of both Cells are distinct.
     */
    private void validateLocationUpdate() {
        if (Check.Network.isSameLAC(getStartCell(), getEndCell())) {
            throw new IllegalArgumentException("startCell and endCell cannot have the same LAC");
        }
    }
}
