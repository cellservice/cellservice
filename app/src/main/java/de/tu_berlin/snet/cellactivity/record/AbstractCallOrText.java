package de.tu_berlin.snet.cellactivity.record;

/**
 * Created by giraffe on 4/17/16.
 */
public abstract class AbstractCallOrText {
    private String direction;
    private String address;

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
        if(address.isEmpty()) {
            throw new IllegalArgumentException("address cannot be empty");
        }
        this.address = address;
    }
}
