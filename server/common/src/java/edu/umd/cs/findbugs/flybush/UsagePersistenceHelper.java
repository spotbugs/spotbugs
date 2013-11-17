package edu.umd.cs.findbugs.flybush;

import java.util.Map;

import javax.jdo.PersistenceManager;

public abstract class UsagePersistenceHelper extends BasePersistenceHelper {

    public abstract DbClientVersionStats createDbClientVersionStats(String application, String version, long dayStart);

    public abstract DbUsageEntry createDbUsageEntry();

    public abstract DbPluginUpdateXml createPluginUpdateXml();

    public abstract DbUsageSummary createDbUsageSummary();

    public abstract Class<? extends DbClientVersionStats> getDbClientVersionStatsClass();

    public abstract Class<? extends DbUsageEntry> getDbUsageEntryClass();

    public abstract Class<? extends DbPluginUpdateXml> getDbPluginUpdateXmlClass();

    public abstract Class<? extends DbUsageSummary> getDbUsageSummaryClass();

    public String getDbClientVersionStatsClassname() {
        return getDbClientVersionStatsClass().getName();
    }

    public String getDbUsageEntryClassname() {
        return getDbUsageEntryClass().getName();
    }

    public String getDbPluginUpdateXmlClassname() {
        return getDbPluginUpdateXmlClass().getName();
    }

    public String getDbUsageSummaryClassname() {
        return getDbUsageSummaryClass().getName();
    }

    public abstract int clearAllData();

    public abstract <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key);

    public abstract String getEmailOfCurrentAppengineUser();

    public abstract boolean shouldRecordClientStats(String ip, String appName, String appVer, long midnightToday);

    public abstract void addToQueue(String url, Map<String, String> params);

}
