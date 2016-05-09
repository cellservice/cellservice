package de.tu_berlin.snet.cellservice.observer;

import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tu_berlin.snet.cellservice.CellService;
import de.tu_berlin.snet.cellservice.util.Functions;

public class OutgoingSMSObserver extends ContentObserver implements Observer {
    public interface OutgoingSMSListener {
        void onSMSSent(String receiverAddress);
    }

    private static boolean NOTIFY_LISTENERS = true;
    private static boolean DONT_NOTIFY_LISTENERS = false;

    private static OutgoingSMSObserver instance;

    private List<OutgoingSMSListener> listeners = new ArrayList<OutgoingSMSListener>();

    private Set<String> textMessageCache = new HashSet<String>();

    public void addTextMessage(String message) {
        textMessageCache.add(message);
    }

    public boolean isExistingTextMessage(String message) {
        return textMessageCache.contains(message);
    }

    private OutgoingSMSObserver(Handler handler) {
        super(handler);
        checkForTextMessages(DONT_NOTIFY_LISTENERS);

    }

    // See http://stackoverflow.com/questions/14057273/android-singleton-with-global-context/14057777#14057777
    // for reference / double check synchronization
    public static OutgoingSMSObserver getInstance() {
        if (instance == null) instance = getInstanceSync();
        return instance;
    }

    private static synchronized OutgoingSMSObserver getInstanceSync() {
        if (instance == null) instance = new OutgoingSMSObserver(new Handler());
        return instance;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }


    public void addListener(OutgoingSMSListener toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(OutgoingSMSListener toRemove) {
        listeners.remove(toRemove);
    }

    private void checkForTextMessages(boolean notifyListeners) {
        Uri smsuri = Uri.parse("content://sms/sent");
        Cursor cursor = CellService.get().getContentResolver().query(smsuri, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) { // must check the result to prevent exception
            do {
                String type = cursor.getString(cursor.getColumnIndex("type"));
                if(type.equals("2")) { // successfully sent message
                    String receiverAddress = cursor.getString(cursor.getColumnIndex("address"));
                    String body = cursor.getString(cursor.getColumnIndex("body"));
                    String date = cursor.getString(cursor.getColumnIndex("date"));

                    String md5TextMessage = Functions.md5(receiverAddress + body + date);

                    if(!isExistingTextMessage(md5TextMessage)) {
                        addTextMessage(md5TextMessage);
                        if(notifyListeners) {
                            Log.e("OUTGOING SMS STUFF", "to: "+receiverAddress+" at: "+date+" writing: "+body);
                            for (OutgoingSMSListener l : listeners)
                                l.onSMSSent(receiverAddress);
                        }
                    }
                }
            } while (cursor.moveToNext());
        } else {
            // empty box, no SMS
        }
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        checkForTextMessages(NOTIFY_LISTENERS);
    }
}