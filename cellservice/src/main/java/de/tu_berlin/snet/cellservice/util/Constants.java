package de.tu_berlin.snet.cellservice.util;

/**
 * @author Markus Paeschke (markus.paeschke@gmail.com)
 */
public class Constants {
    public final static String SHARED_PREFERENCES_FILE_NAME = "CellServiceSharedPreferences";
    public final static String SHARED_PREFERENCES_LAST_TIMESTAMP = "CellInfoObserverLastTimestamp";
    public final static Long SHARED_PREFERENCES_LAST_TIMESTAMP_DEFAULT = 1451606461000L;

    public final static String SHARED_PREFERENCES_PREVIOUS_CELL = "previousCellInfo";
    public final static String SHARED_PREFERENCES_CURRENT_CELL = "currentCellInfo";

    public static final String MIGRATION_FILE_PATH = "db/migration";

    private Constants() {

    }
}
