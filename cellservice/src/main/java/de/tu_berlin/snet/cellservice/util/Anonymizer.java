package de.tu_berlin.snet.cellservice.util;


/**
 * Created by Friedhelm Victor on 5/4/16.
 */

import java.util.Random;

public class Anonymizer {
    public static final int UNMODIFIED = 0;
    public static final int RANDOMIZE = 1;
    public static final int MD5HASH = 2;

    public static String anonymize(int number) {
        return anonymize(String.valueOf(number));
    }

    public static String anonymize(String string) {
        CellServiceConfig config = CellServiceConfig.getInstance();
        switch (config.getPhoneNumberAnonymizationTechnique()) {
            case UNMODIFIED: return string;
            case RANDOMIZE: return Functions.md5(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
            case MD5HASH: return Functions.md5(string);
            default: return Functions.md5(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
        }
    }
}
