package de.tu_berlin.snet.cellservice.model.database;

import java.sql.Date;
import java.util.ArrayList;

import de.tu_berlin.snet.cellservice.model.record.TextMessage;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public interface TextMessageHelper {
    int getPrimaryKey(TextMessage message);
    boolean insertRecord(TextMessage textMessage);
    ArrayList<TextMessage> getAllTextMessageRecords();
    ArrayList<TextMessage> getTextMessageRecords(Date day);
    ArrayList<TextMessage> getTextMessageRecords(Date from, Date to);
}
