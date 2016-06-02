package de.tu_berlin.snet.cellservice.util;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by giraffe on 5/4/16.
 */
public class Functions {
    private final static String LOG_TAG = Functions.class.getSimpleName();

    // http://stackoverflow.com/questions/5787894/android-is-there-any-way-to-listen-outgoing-sms/5788013#5788013
    public static String md5(String in) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.reset();
            digest.update(in.getBytes());
            byte[] a = digest.digest();
            int len = a.length;
            StringBuilder sb = new StringBuilder(len << 1);
            for (int i = 0; i < len; i++) {
                sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
                sb.append(Character.forDigit(a[i] & 0x0f, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.d(LOG_TAG, e.getMessage());
        }
        return null;
    }
}
