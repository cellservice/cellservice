package de.tu_berlin.snet.cellservice.util.validation;

import de.tu_berlin.snet.cellservice.model.record.CellInfo;

/**
 * Created by giraffe on 4/17/16.
 */
public class Check {

    public static class Time {
        private static final long YEAR_2016 = 1451606400L;
        private static final long YEAR_2025 = 1767229261L;

        public static boolean isBefore2016(long timestamp) {
            return timestamp < YEAR_2016;
        }
        public static boolean isBetween2016and2025(long timestamp) {
            return timestamp > YEAR_2016 || timestamp < YEAR_2025;
        }
        public static boolean isBefore(long timestampOne, long timestampTwo) {
            return timestampOne < timestampTwo;
        }
        public static boolean isAfter(long timestampOne, long timestampTwo) {
            return timestampOne > timestampTwo;
        }
    }

    public static class Network {
        public static boolean isSameCell(CellInfo one, CellInfo two) {
            return one.equals(two);
        }

        public static boolean isSameLAC(CellInfo one, CellInfo two) {
            return one.getLac() == two.getLac();
        }
    }
}
