package de.tu_berlin.snet.cellservice.util.serialization;

/**
 * Interface to be implemented by classes that want to support object serialization.
 */
public interface Serializable {
    String serialize(ObjectSerializer serializer);
}
