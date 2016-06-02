package de.tu_berlin.snet.cellservice.util.serialization;

import com.google.gson.Gson;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * A {@link com.google.gson.Gson} based serializer and de-serializer.
 */
public class GsonSerializer implements ObjectSerializer {
    Gson gson = new Gson();

    /**
     * Serialization method for a {@link CellInfo}.
     * @param cellInfo the {@link CellInfo} to be serialized
     * @return a String representation
     */
    @Override
    public String serialize(CellInfo cellInfo) {
        return gson.toJson(cellInfo);
    }

    /**
     * Method to deserialize a String into a {@link CellInfo}.
     * Initializes measurement locations as they are transient.
     * @param serialized the String to be deserialized from
     * @return a {@link CellInfo} from the deserialization
     */
    public CellInfo deSerialize(String serialized) {
        CellInfo cellInfo = gson.fromJson(serialized, CellInfo.class);
        cellInfo.initializeLocations();
        return cellInfo;
    }
}
