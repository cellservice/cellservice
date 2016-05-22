package de.tu_berlin.snet.cellservice.model.record;

import de.tu_berlin.snet.cellservice.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
public class LocationUpdate extends AbstractCellChange {
    public LocationUpdate(long id, CellInfo startCell, CellInfo endCell) {
        super(id, startCell, endCell);
        validateLocationUpdate();
    }

    private void validateLocationUpdate() {
        if (Check.Network.isSameLAC(getStartCell(), getEndCell())) {
            throw new IllegalArgumentException("startCell and endCell cannot have the same LAC");
        }
    }
}
