package edu.umd.cs.findbugs.flybush;

/**
 * Counts the number of Cloud accesses by a given application on a given day.
 */
public interface DbClientVersionStats extends Comparable<DbClientVersionStats> {
    String getApplication();
    String getVersion();
    /** unix date of midnight on the day that this object represents */
    long getDayStart();
    int getHits();
    void incrHits();
}
