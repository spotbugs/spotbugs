package edu.umd.cs.findbugs.flybush.local;

import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbIssue;
import edu.umd.cs.findbugs.flybush.DbUser;
import edu.umd.cs.findbugs.flybush.PersistenceHelper;
import edu.umd.cs.findbugs.flybush.SqlCloudSession;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LocalPersistenceHelper implements PersistenceHelper {
    private static PersistenceManagerFactory pmf;

    private static PersistenceManagerFactory createPersistenceManagerFactory() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream("jdo.properties"));
        return JDOHelper.getPersistenceManagerFactory(properties);
    }

    private static synchronized PersistenceManagerFactory get() throws IOException {
        if (pmf == null || pmf.isClosed()) {
            pmf = createPersistenceManagerFactory();
        }
        return pmf;
    }

    public PersistenceManager getPersistenceManager() throws IOException {
        PersistenceManagerFactory pmf = getPersistenceManagerFactory();
        return pmf.getPersistenceManager();
    }

    public PersistenceManagerFactory getPersistenceManagerFactory() throws IOException {
        return get();
    }

    public DbUser createDbUser(String openidUrl, String email) {
        return new LocalDbUser(openidUrl, email);
    }

    public SqlCloudSession createSqlCloudSession(long id, Date date, Object userKey, String email) {
        return new LocalSqlCloudSession((LocalDbUser) userKey, email, id, date);
    }

    public DbInvocation createDbInvocation() {
        return new LocalDbInvocation();
    }

    public DbIssue createDbIssue() {
        return new LocalDbIssue();
    }

    public DbEvaluation createDbEvaluation() {
        return new LocalDbEvaluation();
    }

    public Class<? extends DbUser> getDbUserClass() {
        return LocalDbUser.class;
    }

    public Class<? extends SqlCloudSession> getSqlCloudSessionClass() {
        return LocalSqlCloudSession.class;
    }

    public Class<? extends DbInvocation> getDbInvocationClass() {
        return LocalDbInvocation.class;
    }

    public Class<? extends DbIssue> getDbIssueClass() {
        return LocalDbIssue.class;
    }

    public Class<? extends DbEvaluation> getDbEvaluationClass() {
        return LocalDbEvaluation.class;
    }

    public int clearAllData() {
        throw new UnsupportedOperationException();
    }

    public <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key) {
        // JDO on DataNucleus doesn't use key objects, it just uses actual object references, so the key is the object
        // itself
        return cls.cast(key);
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, DbIssue> findIssues(PersistenceManager pm, Iterable<String> hashes) {
        // we can do this here but not in App Engine because App Engine's JDO impl makes it so each DbIssue query result
        // reqiures another query to get DbEvaluations.
        Query query = pm.newQuery("select from " + getDbIssueClass().getName() + " where :hashes.contains(hash)");
        List<DbIssue> results = (List<DbIssue>) query.execute(hashes);
        Map<String,DbIssue> map = new HashMap<String, DbIssue>();
        for (DbIssue issue : results) {
            map.put(issue.getHash(), issue);
		}
        return map;
    }

    public void convertToOldStyleForTesting(DbEvaluation eval) {
    }

    public String getEmail(PersistenceManager pm, Comparable<?> who) {
        return ((LocalDbUser) who).getEmail();
    }
}
