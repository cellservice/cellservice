package de.tu_berlin.snet.cellservice.observer;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Telephony;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tu_berlin.snet.cellservice.CellService;
import de.tu_berlin.snet.cellservice.util.Constants;
import de.tu_berlin.snet.cellservice.util.Functions;

public class OutgoingSMSObserver extends ContentObserver implements Observer {
    public interface OutgoingSMSListener {
        void onSMSSent(String receiverAddress);
    }

    public static boolean NOTIFY_LISTENERS = true;
    public static boolean DONT_NOTIFY_LISTENERS = false;
    public final static String SMS_URI = "content://sms/sent";
    public final static String SMS_TYPE_SUCCESSFULLY_SENT = "2";
    public final static String SMS_FIELD_TYPE = "type";
    public final static String SMS_FIELD_ADDRESS = "address";
    public final static String SMS_FIELD_BODY = "body";
    public final static String SMS_FIELD_DATE = "date";

    private static OutgoingSMSObserver instance;

    private List<OutgoingSMSListener> listeners = new ArrayList<>();

    private Set<String> textMessageCache = new HashSet<>();

    private OutgoingSMSObserver(Handler handler) {
        super(handler);
        checkForTextMessages(DONT_NOTIFY_LISTENERS);

    }

    // See http://stackoverflow.com/questions/14057273/android-singleton-with-global-context/14057777#14057777
    // for reference / double check synchronization
    public static OutgoingSMSObserver getInstance() {
        if (instance == null) {
            instance = getInstanceSync();
        }
        return instance;
    }

    private static synchronized OutgoingSMSObserver getInstanceSync() {
        if (instance == null) {
            instance = new OutgoingSMSObserver(new Handler());
        }
        return instance;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        checkForTextMessages(NOTIFY_LISTENERS);
    }

    public void addListener(OutgoingSMSListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(OutgoingSMSListener toRemove) {
        listeners.remove(toRemove);
    }

    private void addTextMessage(String message) {
        textMessageCache.add(message);
    }

    private boolean isExistingTextMessage(String message) {
        return textMessageCache.contains(message);
    }

    private void checkForTextMessages(boolean notifyListeners) {
        Uri smsUri = Uri.parse(SMS_URI);

        Cursor cursor = CellService.get().getContentResolver().query(smsUri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String type = cursor.getString(cursor.getColumnIndex(SMS_FIELD_TYPE));
                if(type.equals(SMS_TYPE_SUCCESSFULLY_SENT)) {
                    final String receiverAddress = cursor.getString(cursor.getColumnIndex(SMS_FIELD_ADDRESS));
                    final String body = cursor.getString(cursor.getColumnIndex(SMS_FIELD_BODY));
                    final String date = cursor.getString(cursor.getColumnIndex(SMS_FIELD_DATE));
                    final String md5TextMessage = Functions.md5(receiverAddress + body + date);

                    if(!isExistingTextMessage(md5TextMessage)) {
                        addTextMessage(md5TextMessage);
                        if(notifyListeners) {
                            Log.d("OUTGOING SMS STUFF",String.format("to: %s at: %s writing: %s",
                                    receiverAddress, date, body));
                            for (OutgoingSMSListener listener : listeners) {
                                listener.onSMSSent(receiverAddress);
                            }
                        }
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }
}