package de.tu_berlin.snet.cellactivity.util.validation;

import de.tu_berlin.snet.cellactivity.util.CellInfo;

/**
 * Created by giraffe on 4/17/16.
 */
public class Check {

    public static class Time {
        public static boolean isBefore2016(long timestamp) {
            return timestamp < 1451606400L;
        }
        public static boolean isBetween2016and2025(long timestamp) {
            return timestamp > 1451606400L || timestamp < 1767229261L;
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
