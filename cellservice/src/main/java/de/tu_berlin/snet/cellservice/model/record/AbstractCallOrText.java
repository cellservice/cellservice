package de.tu_berlin.snet.cellservice.model.record;

import com.google.gson.annotations.SerializedName;

/**
 * Created by giraffe on 4/17/16.
 */
public abstract class AbstractCallOrText {
    @SerializedName("id")
    private long id;
    @SerializedName("direction")
    private String direction;
    @SerializedName("address")
    private String address;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        if (!direction.equals("incoming") && !direction.equals("outgoing")) {
            throw new IllegalArgumentException("direction must be either \"incoming\" or \"outgoing\"");
        }
        this.direction = direction;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        if(address == null) {
            // This should only happen if the service restarts, and we are already in a call.
            // TODO: IT'S A PROBLEM. DON'T SOLVE IT HERE. SOLVE IT IN PhonecallReceiver.java
            address = "null"; // UGLY FIX...
        }
        if(address.isEmpty()) {
            throw new IllegalArgumentException("address cannot be empty");
        }
        this.address = address;
    }
}
