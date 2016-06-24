package de.tu_berlin.snet.cellservice.model.database;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Vector;

import de.tu_berlin.snet.cellservice.CDRDatabaseInsertionListener;
import de.tu_berlin.snet.cellservice.model.record.CellInfo;
import de.tu_berlin.snet.cellservice.model.record.TextMessage;
import jsqlite.Exception;
import jsqlite.TableResult;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class TextMessageHelperImpl implements TextMessageHelper {
    private final static String LOG_TAG = TextMessageHelperImpl.class.getSimpleName();
    private Context context;
    private CellHelper cellHelper;
    private MeasurementsHelper measurementsHelper;

    public TextMessageHelperImpl(Context context) {
        this.context = context;
        cellHelper = new CellHelperImpl(context);
        measurementsHelper = new MeasurementsHelperImpl(context);
    }

    @Override
    public int getPrimaryKey(TextMessage message) {
        String messageIdQuery =
                "SELECT id" +
                        "   FROM TextMessages" +
                        "   WHERE direction = '%s' AND address = '%s' AND time = %s AND cell_id = %s" +
                        "   LIMIT 1;";
        int cell_id = cellHelper.getPrimaryKey(message.getCell());
        return GeoDatabaseHelper.getInstance(context).getId(String.format(messageIdQuery, message.getDirection(), message.getAddress(),
                message.getTime(), cell_id));
    }

    @Override
    public boolean insertRecord(TextMessage textMessage) {
        CellInfo cellInfo = textMessage.getCell();
        cellHelper.insertRecord(cellInfo);
        int cellRecordId = cellHelper.getPrimaryKey(cellInfo);

        String insertTextMessageStatement =
                "INSERT INTO TextMessages (direction, address, time, cell_id)" +
                        "   VALUES ('%s', '%s', %s, %s);";
        GeoDatabaseHelper.getInstance(context).execSQL(String.format(insertTextMessageStatement, textMessage.getDirection(),
                textMessage.getAddress(), textMessage.getTime(), cellRecordId));

        int messageId = getPrimaryKey(textMessage);
        measurementsHelper.insertMeasurements(cellInfo, messageId, GeoDatabaseHelper.getInstance(context).TEXT);

        // Notify listeners
        for (CDRDatabaseInsertionListener l : GeoDatabaseHelper.getInstance(context).getListeners()) {
            l.onTextMessageInserted(textMessage, messageId);
        }

        return false;
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date day) {
        return getTextMessageRecords(day, day);
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecords(Date from, Date to) {
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT id, direction, address, time, cell_id" +
                        "   FROM TextMessages" +
                        "   WHERE date(time, 'unixepoch', 'localtime') >= '" + from.toString() + "'" +
                        "   AND date(time, 'unixepoch', 'localtime') <= '" + to.toString() + "';";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return textMessages;
    }

    @Override
    public ArrayList<TextMessage> getAllTextMessageRecords() {
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT id, direction, address, time, cell_id" +
                        "   FROM TextMessages;";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return textMessages;
    }

    @Override
    public ArrayList<TextMessage> getTextMessageRecordsPaginated(int start, int end) throws IllegalArgumentException {
        if (start < 0 || end < 0 || end < start) {
            throw new IllegalArgumentException("End must be greater than start and both must be greater than 0!");
        }
        ArrayList<TextMessage> textMessages = new ArrayList<TextMessage>();
        final String selectTextMessagesByDate =
                "SELECT id, direction, address, time, cell_id" +
                        "   FROM TextMessages" +
                        " LIMIT " + start + "," + end + ";";
        try {
            TableResult tableResult = GeoDatabaseHelper.getInstance(context).getTable(selectTextMessagesByDate);
            Vector<String[]> rows = tableResult.rows;
            for (String[] fields : rows) {
                TextMessage textMessage = parseTextMessage(fields);
                textMessages.add(textMessage);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
        return textMessages;
    }

    @NonNull
    private TextMessage parseTextMessage(String[] fields) {
        long id = Long.parseLong(fields[0]);
        String direction = fields[1];
        String address = fields[2];
        long time = Long.valueOf(fields[3]);
        CellInfo cell = cellHelper.getCellById(Long.valueOf(fields[4]));
        return new TextMessage(id, cell, direction, address, time);
    }
}
