package de.tu_berlin.snet.cellactivity.record;

import de.tu_berlin.snet.cellactivity.util.CellInfo;
import de.tu_berlin.snet.cellactivity.util.validation.Check;

/**
 * Created by giraffe on 4/16/16.
 */
public class LocationUpdate extends AbstractCellChange {
    public LocationUpdate(CellInfo startCell, CellInfo endCell) {
        super(startCell, endCell);
        validateLocationUpdate();
    }

    private void validateLocationUpdate() {
        if(Check.Network.isSameLAC(getStartCell(), getEndCell()))
        {
            throw new IllegalArgumentException("startCell and endCell cannot have the same LAC");
        }
    }
}
