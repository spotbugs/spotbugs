package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public abstract class PersistenceHelper {

    public abstract PersistenceManagerFactory getPersistenceManagerFactory() throws IOException;
    public abstract PersistenceManager getPersistenceManager() throws IOException;

    public abstract DbUser createDbUser(String openidUrl, String email);
    public abstract SqlCloudSession createSqlCloudSession(long id, Date date, Object userKey, String email);
    public abstract DbInvocation createDbInvocation();
    public abstract DbIssue createDbIssue();
    public abstract DbEvaluation createDbEvaluation();
    public abstract DbClientVersionStats createDbClientVersionStats(String application, String version, long dayStart);
    public abstract DbUsageEntry createDbUsageEntry();
    public abstract DbPluginUpdateXml createPluginUpdateXml(String value);

    public abstract Class<? extends DbUser> getDbUserClass();
    public abstract Class<? extends SqlCloudSession> getSqlCloudSessionClass();
    public abstract Class<? extends DbInvocation> getDbInvocationClass();
    public abstract Class<? extends DbIssue> getDbIssueClass();
    public abstract Class<? extends DbEvaluation> getDbEvaluationClass();
    public abstract Class<? extends DbClientVersionStats> getDbClientVersionStatsClass();
    public abstract Class<? extends DbUsageEntry> getDbUsageEntryClass();
    public abstract Class<? extends DbPluginUpdateXml> getDbPluginUpdateXmlClass();

    public String getDbUserClassname() {
        return getDbUserClass().getName();
    }
    public String getSqlCloudSessionClassname() {
        return getSqlCloudSessionClass().getName();
    }
    public String getDbInvocationClassname() {
        return getDbInvocationClass().getName();
    }
    public String getDbIssueClassname() {
        return getDbIssueClass().getName();
    }
    public String getDbEvaluationClassname() {
        return getDbEvaluationClass().getName();
    }
    public String getDbClientVersionStatsClassname() {
        return getDbClientVersionStatsClass().getName();
    }
    public String getDbUsageEntryClassname() {
        return getDbUsageEntryClass().getName();
    }
    public String getDbPluginUpdateXmlClassname() {
        return getDbPluginUpdateXmlClass().getName();
    }


    public abstract int clearAllData();

    public abstract <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key);

    public abstract Map<String, DbIssue> findIssues(PersistenceManager pm, Iterable<String> hashes);

    public abstract String getEmail(PersistenceManager pm, Comparable<?> who);
    public abstract String getEmailOfCurrentAppengineUser();

    public abstract boolean isOldCommentStyle(DbEvaluation eval);
    public abstract void convertToOldCommentStyleForTesting(DbEvaluation eval);
    public abstract boolean convertToNewCommentStyle(DbEvaluation eval);

    public abstract boolean shouldRecordClientStats(String ip, String appName, String appVer, long midnightToday);

}
