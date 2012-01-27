package edu.umd.cs.findbugs.flybush;

import java.util.Date;

/**
 * Stores various kinds of usage data for one day in history.
 */
public interface DbUsageSummary extends Comparable<DbUsageSummary> {


    void setDate(Date date);
    /** midnight on the date in question */
    Date getDate();
    void setEndDate(Date date);
    /** null means this is a daily summary */
    Date getEndDate();
    void setCategory(String category);
    /** such as "version", "users", "ips" */
    String getCategory();
    void setCategoryKey(String categoryKey);
    /** such as a version number or plugin name */
    String getCategoryKey();
    void setCategorySubkey(String categorySubkey);
    /** such as a plugin version number */
    String getCategorySubkey();
    void setValue(int value);
    int getValue();
    void setLastUpdated(Date lastUpdated);
    Date getLastUpdated();
    byte[] getBlob();
    void setBlob(byte[] blob);

    DbUsageSummary copy();
}
