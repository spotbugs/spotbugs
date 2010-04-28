package edu.umd.cs.findbugs.flybush;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public interface PersistenceHelper {

    PersistenceManagerFactory getPersistenceManagerFactory() throws IOException;
    PersistenceManager getPersistenceManager() throws IOException;

    DbUser createDbUser(String openidUrl, String email);
    SqlCloudSession createSqlCloudSession(long id, Date date, Object userKey, String email);
    DbInvocation createDbInvocation();
    DbIssue createDbIssue();
    DbEvaluation createDbEvaluation();

    Class<? extends DbUser> getDbUserClass();
    Class<? extends SqlCloudSession> getSqlCloudSessionClass();
    Class<? extends DbInvocation> getDbInvocationClass();
    Class<? extends DbIssue> getDbIssueClass();
    Class<? extends DbEvaluation> getDbEvaluationClass();

    int clearAllData();

    <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key);

    Map<String, DbIssue> findIssues(PersistenceManager pm, Iterable<String> hashes);

    void convertToOldStyleForTesting(DbEvaluation eval);

    String getEmail(PersistenceManager pm, Comparable<?> who);
}
