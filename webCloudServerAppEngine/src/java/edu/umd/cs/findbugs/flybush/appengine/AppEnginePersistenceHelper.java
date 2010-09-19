package edu.umd.cs.findbugs.flybush.appengine;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import edu.umd.cs.findbugs.flybush.DbEvaluation;
import edu.umd.cs.findbugs.flybush.DbInvocation;
import edu.umd.cs.findbugs.flybush.DbIssue;
import edu.umd.cs.findbugs.flybush.DbUser;
import edu.umd.cs.findbugs.flybush.PersistenceHelper;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppEnginePersistenceHelper implements PersistenceHelper {
    private static final Logger LOGGER = Logger.getLogger(AppEnginePersistenceHelper.class.getName());

    public PersistenceManagerFactory getPersistenceManagerFactory() {
        return PMF.get();
    }

    public PersistenceManager getPersistenceManager() {
        return getPersistenceManagerFactory().getPersistenceManager();
    }

    public AppEngineDbUser createDbUser(String openidUrl, String email) {
        return new AppEngineDbUser(openidUrl, email);
    }

    public AppEngineSqlCloudSession createSqlCloudSession(long id, Date date, Object userKey, String email) {
        return new AppEngineSqlCloudSession((Key) userKey, id, email, date);
    }

    public Class<? extends DbUser> getDbUserClass() {
        return AppEngineDbUser.class;
    }

    public DbInvocation createDbInvocation() {
        return new AppEngineDbInvocation();
    }

    public Class<AppEngineSqlCloudSession> getSqlCloudSessionClass() {
        return AppEngineSqlCloudSession.class;
    }

    public int clearAllData() {
        int deleted = 0;
        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery pquery = ds.prepare(new Query().setKeysOnly());
        for (Entity entity : pquery.asIterable()) {
            ds.delete(entity.getKey());
            deleted++;
        }
        return deleted;
    }

    public AppEngineDbIssue createDbIssue() {
        return new AppEngineDbIssue();
    }

    public Class<AppEngineDbInvocation> getDbInvocationClass() {
        return AppEngineDbInvocation.class;
    }

    public AppEngineDbEvaluation createDbEvaluation() {
        return new AppEngineDbEvaluation();
    }

    public Class<AppEngineDbIssue> getDbIssueClass() {
        return AppEngineDbIssue.class;
    }

    public Class<AppEngineDbEvaluation> getDbEvaluationClass() {
        return AppEngineDbEvaluation.class;
    }

    public <E> E getObjectById(PersistenceManager pm, Class<? extends E> cls, Object key) {
        return pm.getObjectById(cls, key);
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, DbIssue> findIssues(PersistenceManager pm, Iterable<String> hashes) {
        javax.jdo.Query query = pm.newQuery("select hash, firstSeen, lastSeen, bugLink, bugLinkType, hasEvaluations, evaluations from "
                                  + getDbIssueClass().getName() + " where :hashes.contains(hash)");
        List<Object[]> results = (List<Object[]>) query.execute(hashes);
        Map<String,DbIssue> map = new HashMap<String, DbIssue>();
        for (Object[] result : results) {
            DbIssue issue = createDbIssue();
            issue.setHash((String) result[0]);
            issue.setFirstSeen((Long) result[1]);
            issue.setLastSeen((Long) result[2]);
            issue.setBugLink((String) result[3]);
            try {
                String linkType = (String) result[4];
                if (linkType != null) {
                    issue.setBugLinkType(linkType);
                }
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Error parsing issue " + issue.getHash(), e);
            }
            issue.setHasEvaluations((Boolean) result[5]);
            issue.setEvaluationsDontLook((Set<DbEvaluation>) result[6]);
            map.put(issue.getHash(), issue);
        }
        return map;
    }

    public void convertToOldCommentStyleForTesting(DbEvaluation eval) {
        AppEngineDbEvaluation aede = (AppEngineDbEvaluation) eval;
        aede.setShortComment(aede.getLongComment().getValue());
        aede.setLongComment(null);
    }

    @Override
    public boolean convertToNewCommentStyle(DbEvaluation eval) {
        return ((AppEngineDbEvaluation)eval).convertToNewCommentStyle();
    }

    public String getEmail(PersistenceManager pm, Comparable<?> who) {
        //noinspection RedundantCast
        return pm.getObjectById(getDbUserClass(), (Key) who).getEmail();
    }

    @Override
    public boolean isOldCommentStyle(DbEvaluation eval) {
        return ((AppEngineDbEvaluation) eval).getShortComment() != null;
    }
}
