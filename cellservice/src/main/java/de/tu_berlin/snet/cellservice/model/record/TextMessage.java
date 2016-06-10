package de.tu_berlin.snet.cellservice.model.record;

import com.google.gson.annotations.SerializedName;

import de.tu_berlin.snet.cellservice.util.validation.Check;

/**
 * Created by giraffe on 4/17/16.
 */
public class TextMessage extends AbstractCallOrText {
    @SerializedName("cell_id")
    private CellInfo cell;
    @SerializedName("time")
    private long time;

    /**
     * Default constructor.
     * @param cell The cell the text message has been sent from
     * @param direction Outgoing or incoming text message?
     * @param address The address the message has been sent to or received from
     */
    public TextMessage(CellInfo cell, String direction, String address) {
        this(-1, cell, direction, address);
    }

    /**
     * Constructor that lets you set a database record id
     * @param id the database record id this object corresponds to
     * @param cell The cell the text message has been sent from
     * @param direction Outgoing or incoming text message?
     * @param address The address the message has been sent to or received from
     */
    public TextMessage(long id, CellInfo cell, String direction, String address) {
        this(id, cell, direction, address, System.currentTimeMillis() / 1000);
    }

    /**
     * Constructor that lets you set a database record id and the time
     * @param id the database record id this object corresponds to
     * @param cell The cell the text message has been sent from
     * @param direction Outgoing or incoming text message?
     * @param address The address the message has been sent to or received from
     * @param time The timestamp in seconds when the message has been sent
     */
    public TextMessage(long id, CellInfo cell, String direction, String address, long time) {
        setId(id);
        setDirection(direction);
        setAddress(address);
        setCell(cell);
        setTime(time);
    }

    public CellInfo getCell() {
        return cell;
    }

    public void setCell(CellInfo cell) {
        this.cell = cell;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        if (!Check.Time.isBetween2016and2025(time)) {
            throw new IllegalArgumentException("not a valid epoch timestamp between 2016 and 2025");
        }
        this.time = time;
    }
}
