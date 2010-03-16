package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.*;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("serial")
public class UpdateServlet extends AbstractFlybushServlet {
    static final int ONE_DAY_IN_MILLIS = 1000*60*60*24;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PersistenceManager pm = getPersistenceManager();
        try {
            if (req.getRequestURI().equals("/expire-sql-sessions")) {
                expireSqlSessions(resp, pm);

            } else {
                super.doGet(req, resp);
            }
        } finally {
            pm.close();
        }
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri) 
            throws IOException {
        if (uri.equals("/clear-all-data")) {
            clearAllData(resp);

        } else if (uri.equals("/upload-issues")) {
            uploadIssues(req, resp, pm);

        } else if (uri.equals("/upload-evaluation")) {
            uploadEvaluation(req, resp, pm);
            
        } else if (uri.equals("/set-bug-link")) {
            setBugLink(req, resp, pm);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void expireSqlSessions(HttpServletResponse resp, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + persistenceHelper.getSqlCloudSessionClass().getName() + " where date < :when");
        Date oneWeekAgo = new Date(System.currentTimeMillis() - 7 * ONE_DAY_IN_MILLIS);
        for (SqlCloudSession session : (List<SqlCloudSession>) query.execute(oneWeekAgo)) {
            Transaction tx = pm.currentTransaction();
            tx.begin();
            try {
                pm.deletePersistent(session);
                tx.commit();
            } finally {
                if (tx.isActive())
                    tx.rollback();
            }
        }
        resp.setStatus(200);
    }

    private void clearAllData(HttpServletResponse resp) throws IOException {
        int deleted = 0;
        try {
            deleted = persistenceHelper.clearAllData();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not delete all data - only " + deleted + " entities", e);
        }
        setResponse(resp, 200, "Deleted " + deleted + " entities");
    }

    private void uploadIssues(HttpServletRequest req, HttpServletResponse resp,
                              PersistenceManager pm) throws IOException {
        UploadIssues issues = UploadIssues.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(issues.getSessionId(), pm);
        if (session == null) {
            resp.setStatus(403);
            return;
        }
        List<String> hashes = decodeHashesForIssues(issues);
        long start = System.currentTimeMillis();
        Set<String> existingIssueHashes = lookupHashes(hashes, pm);
        LOGGER.info("Looking up hashes took " + (System.currentTimeMillis() - start) + "ms");
        for (Issue issue : issues.getNewIssuesList()) {
            String hashStr = AppEngineProtoUtil.decodeHash(issue.getHash());
            if (!existingIssueHashes.contains(hashStr)) {
                DbIssue dbIssue = createDbIssue(issue);

                start = System.currentTimeMillis();
                commitInTransaction(pm, dbIssue);
                LOGGER.info("Committed new issue in " + (System.currentTimeMillis() - start) + "ms");
            } else {
                LOGGER.warning("User is trying to upload existing issue " + hashStr);
            }
        }

        setResponse(resp, 200, "");
    }

    private void uploadEvaluation(HttpServletRequest req,
                                  HttpServletResponse resp, PersistenceManager pm) throws IOException {
        UploadEvaluation uploadEvalMsg = UploadEvaluation.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(uploadEvalMsg.getSessionId(), pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return;
        }

        DbEvaluation dbEvaluation = createDbEvaluation(uploadEvalMsg.getEvaluation());
        dbEvaluation.setWho(session.getUser());
        copyInvocationToEvaluation(pm, session, dbEvaluation);
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            String hash = AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash());
            DbIssue issue = findIssue(pm, hash);
            if (issue == null) {
                setResponse(resp, 404, "no such issue " + AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash()));
                return;
            }
            dbEvaluation.setIssue(issue);
            issue.addEvaluation(dbEvaluation);
            pm.makePersistent(issue);

            tx.commit();

        } finally {
            if (tx.isActive())
                tx.rollback();
        }

        resp.setStatus(200);
    }
    
    private void setBugLink(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) 
            throws IOException {
        SetBugLink setBugLinkMsg = SetBugLink.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(setBugLinkMsg.getSessionId(), pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return;
        }

        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            String decodedHash = AppEngineProtoUtil.decodeHash(setBugLinkMsg.getHash());
            DbIssue issue = findIssue(pm, decodedHash);
            if (issue == null) {
                setResponse(resp, 404, "no such issue " + decodedHash);
                return;
            }
            String bugLink = setBugLinkMsg.hasUrl() ? setBugLinkMsg.getUrl().trim() : null;
            if (bugLink != null && bugLink.length() == 0)
                bugLink = null;
            issue.setBugLink(bugLink);

            issue.setBugLinkType(setBugLinkMsg.hasBugLinkType()
                    ? DbIssue.DbBugLinkType.valueOf(setBugLinkMsg.getBugLinkType().name())
                    : null);
            pm.makePersistent(issue);

            tx.commit();

        } finally {
            if (tx.isActive())
                tx.rollback();
        }

        resp.setStatus(200);
    }
    
    // ========================= end of request handling ================================

    private List<String> decodeHashesForIssues(UploadIssues issues) {
        List<String> hashes = new ArrayList<String>();
        for (Issue issue : issues.getNewIssuesList()) {
            hashes.add(AppEngineProtoUtil.decodeHash(issue.getHash()));
        }
        return hashes;
    }

    private void commitInTransaction(PersistenceManager pm, DbIssue dbIssue) {
        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.makePersistent(dbIssue);
            tx.commit();
        } finally {
            if (tx.isActive()) tx.rollback();
        }
    }

    private DbIssue createDbIssue(Issue issue) {
        DbIssue dbIssue = persistenceHelper.createDbIssue();
        dbIssue.setHash(AppEngineProtoUtil.decodeHash(issue.getHash()));
        dbIssue.setBugPattern(issue.getBugPattern());
        dbIssue.setPriority(issue.getPriority());
        dbIssue.setPrimaryClass(issue.getPrimaryClass());
        dbIssue.setFirstSeen(issue.getFirstSeen());
        dbIssue.setLastSeen(issue.getFirstSeen()); // ignore last seen
        return dbIssue;
    }

    private void copyInvocationToEvaluation(PersistenceManager pm, SqlCloudSession session, DbEvaluation dbEvaluation) {
        Object invocationKey = session.getInvocation();
        if (invocationKey != null) {
            DbInvocation invocation;
            try {
                invocation = persistenceHelper.getObjectById(pm,
                                                             persistenceHelper.getDbInvocationClass(), 
                                                             invocationKey);
                if (invocation != null) {
                    dbEvaluation.setInvocation(invocation);
                }
            } catch (JDOObjectNotFoundException e) {
                // ignore
            }
        }
    }

    private DbEvaluation createDbEvaluation(Evaluation protoEvaluation) {
        DbEvaluation dbEvaluation = persistenceHelper.createDbEvaluation();
        dbEvaluation.setComment(protoEvaluation.getComment());
        dbEvaluation.setDesignation(protoEvaluation.getDesignation());
        dbEvaluation.setWhen(protoEvaluation.getWhen());
        return dbEvaluation;
    }

    @SuppressWarnings("unchecked")
    private DbIssue findIssue(PersistenceManager pm, String hash) {
        Query query = pm.newQuery(persistenceHelper.getDbIssueClass(), "hash == :hashParam");
        Iterator<DbIssue> it = ((List<DbIssue>) query.execute(hash)).iterator();
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
    }

    @SuppressWarnings("unchecked")
    private Set<String> lookupHashes(Iterable<String> hashes, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + persistenceHelper.getDbIssueClass().getName()
                                  + " where :hashes.contains(hash)");
        query.setResult("hash");
        Set<String> result = new HashSet<String>((List<String>) query.execute(hashes));
        query.closeAll();
        return result;
    }
}