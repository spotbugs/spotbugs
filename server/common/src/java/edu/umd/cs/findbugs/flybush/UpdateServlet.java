package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps.IssueGroup;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

@SuppressWarnings("serial")
public class UpdateServlet extends AbstractFlybushCloudServlet {
    static final int ONE_DAY_IN_MILLIS = 1000 * 60 * 60 * 24;

    @SuppressWarnings({ "deprecation" })
    private static final long FINDBUGS_FIRST_RELEASE = new Date("Jan 23, 1996").getTime();

    /** package-private for testing */
    static Set<String> buildPackageList(String cls) {
        Set<String> list = Sets.newHashSet();
        while (true) {
            int dot = cls.lastIndexOf('.');
            if (dot == -1)
                break;
            cls = cls.substring(0, dot);
            list.add(cls);
        }
        return list;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PersistenceManager pm = getPersistenceManager();
        try {
            if (req.getRequestURI().equals("/expire-sql-sessions")) {
                expireSqlSessions(resp, pm);

            } else if (req.getRequestURI().equals("/update-evaluation-emails")) {
                updateEvaluationEmails(resp, pm);

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

        } else if (uri.equals("/update-issue-timestamps")) {
            updateIssueTimestamps(req, resp, pm);

        } else if (uri.equals("/upload-issues")) {
            uploadIssues(req, resp, pm);

        } else if (uri.equals("/upload-evaluation")) {
            uploadEvaluation(req, resp, pm);

        } else if (uri.equals("/set-bug-link")) {
            setBugLink(req, resp, pm);
        }
    }

   
    @SuppressWarnings({ "unchecked" })
    private Map<String, String> buildPrimaryClassMap(PersistenceManager pm) {
        Query iq = pm.newQuery("select from " + persistenceHelper.getDbIssueClassname() + " where hasEvaluations");
        List<DbIssue> issues = (List<DbIssue>) iq.execute();
        Map<String, String> primaryClasses = Maps.newHashMap();
        for (DbIssue issue : issues) {
            primaryClasses.put(issue.getHash(), issue.getPrimaryClass());
        }
        iq.closeAll();
        return primaryClasses;
    }

    @SuppressWarnings({ "unchecked" })
    private void updateEvaluationEmails(HttpServletResponse resp, PersistenceManager pm) {
        int skipped = 0;
        int updated = 0;
        boolean finished = false;
        try {
            Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClassname());
            for (DbEvaluation evaluation : (List<DbEvaluation>) query.execute()) {
                if (evaluation.getEmail() != null) {
                    skipped++;
                    continue;
                }
                evaluation.setEmail(persistenceHelper.getEmail(pm, evaluation.getWho()));
                Transaction tx = pm.currentTransaction();
                try {
                    tx.begin();
                    pm.makePersistent(evaluation);
                    tx.commit();
                    updated++;
                } finally {
                    if (tx.isActive())
                        tx.rollback();
                }
            }
            finished = true;
        } finally {
            LOGGER.info((finished ? "" : "(PARTIAL UPDATE) ") + "Updated " + updated + ", skipped " + skipped);
            resp.setStatus(200);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private void expireSqlSessions(HttpServletResponse resp, PersistenceManager pm) {
        Query query = pm.newQuery("select from " + persistenceHelper.getSqlCloudSessionClassname() + " where date < :when");
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

    @SuppressWarnings({ "unchecked" })
    private void updateIssueTimestamps(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {
        UpdateIssueTimestamps issues = UpdateIssueTimestamps.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(issues.getSessionId(), pm);
        if (session == null) {
            resp.setStatus(403);
            return;
        }
        DbUser user = persistenceHelper.getObjectById(pm, persistenceHelper.getDbUserClass(), session.getUser());
        boolean completed = false;
        int updated = 0;
        try {
            for (IssueGroup issueGroup : issues.getIssueGroupsList()) {
                long newFirstSeen = issueGroup.getTimestamp();
                if (newFirstSeen < FINDBUGS_FIRST_RELEASE) {
                    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                    LOGGER.warning("Skipping update of " + issueGroup.getIssueHashesCount() + " issue timestamps "
                            + "- date too early - " + dateFormat.format(new Date(newFirstSeen)));
                    continue;
                }
                Query query = pm.newQuery("select from " + persistenceHelper.getDbIssueClassname()
                        + " where :hashes.contains(hash)");
                for (DbIssue issue : (List<? extends DbIssue>) query.execute(WebCloudProtoUtil.decodeHashes(issueGroup
                        .getIssueHashesList()))) {
                    long storedFirstSeen = issue.getFirstSeen();
                    long firstSeen = storedFirstSeen == 0 ? newFirstSeen : Math.min(newFirstSeen, storedFirstSeen);
                    if (storedFirstSeen != firstSeen) {
                        issue.setFirstSeen(firstSeen);
                        Transaction tx = pm.currentTransaction();
                        tx.begin();
                        try {
                            pm.makePersistent(issue);
                            tx.commit();
                            updated++;
                        } finally {
                            if (tx.isActive())
                                tx.rollback();
                        }
                    }
                }
            }

            setResponse(resp, 200, null);
            completed = true;
        } finally {
            LOGGER.log(completed ? Level.INFO : Level.WARNING, "Updated " + updated + " issue timestamps from " + user.getEmail()
                    + "(" + user.getOpenid() + ")");
        }
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

    @SuppressWarnings({"unchecked"})
    private void uploadIssues(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        UploadIssues issues = UploadIssues.parseFrom(req.getInputStream());
        boolean authenticated = false;
        if (issues.hasSessionId()) {
            SqlCloudSession session = lookupCloudSessionById(issues.getSessionId(), pm);
            if (session != null) {
                authenticated = true;
                LOGGER.info("User: " + session.getEmail());
            }

        }
        if (!authenticated && issues.hasToken()) {
            String token = issues.getToken();
            Query query = pm.newQuery("select from " + persistenceHelper.getDbUserClassname()
                    + " where uploadToken == :token");
            List<DbUser> users = (List<DbUser>) query.execute(issues.getToken());
            if (!users.isEmpty()) {
                DbUser user = users.iterator().next();
                LOGGER.info("Token registered to " + user.getEmail() + " - " + token);
                authenticated = true;
            }
        }

        if (!authenticated) {
            resp.setStatus(403);
            return;
        }
        List<String> hashes = decodeHashesForIssues(issues);
        long start = System.currentTimeMillis();
        Set<String> existingIssueHashes = lookupHashes(hashes, pm);
        LOGGER.info("Looking up hashes took " + (System.currentTimeMillis() - start) + "ms");
        for (Issue issue : issues.getNewIssuesList()) {
            String hashStr = WebCloudProtoUtil.decodeHash(issue.getHash());
            if (!existingIssueHashes.contains(hashStr)) {
                DbIssue dbIssue = createDbIssue(issue);

                start = System.currentTimeMillis();
                commitInTransaction(pm, dbIssue);
                LOGGER.info("Committed new issue in " + (System.currentTimeMillis() - start) + "ms: " + dbIssue.getPrimaryClass());
            } else {
                LOGGER.warning("User is trying to upload existing issue " + hashStr);
            }
        }

        setResponse(resp, 200, "");
    }

    private void uploadEvaluation(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        UploadEvaluation uploadEvalMsg = UploadEvaluation.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(uploadEvalMsg.getSessionId(), pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return;
        }

        LOGGER.info("Review from " + session.getEmail() + ": " + uploadEvalMsg.getEvaluation().getDesignation() + " - "
                + uploadEvalMsg.getEvaluation().getComment());

        DbEvaluation dbEvaluation = createDbEvaluation(uploadEvalMsg.getEvaluation());
        dbEvaluation.setWho(session.getUser());
        dbEvaluation.setEmail(session.getEmail());
        copyInvocationToEvaluation(pm, session, dbEvaluation);
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();

            String hash = WebCloudProtoUtil.decodeHash(uploadEvalMsg.getHash());
            DbIssue issue = findIssue(pm, hash);
            if (issue == null) {
                setResponse(resp, 404, "no such issue " + WebCloudProtoUtil.decodeHash(uploadEvalMsg.getHash()));
                return;
            }
            LOGGER.info("Bug: " + issue.getHash() + " - " + issue.getPrimaryClass());
            dbEvaluation.setIssue(issue);
            issue.addEvaluation(dbEvaluation);
            dbEvaluation.setPrimaryClass(issue.getPrimaryClass());
            dbEvaluation.setPackages(buildPackageList(issue.getPrimaryClass()));
            pm.makePersistent(issue);

            tx.commit();

        } finally {
            if (tx.isActive())
                tx.rollback();
        }

        resp.setStatus(200);
    }

    private void setBugLink(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        SetBugLink setBugLinkMsg = SetBugLink.parseFrom(req.getInputStream());
        SqlCloudSession session = lookupCloudSessionById(setBugLinkMsg.getSessionId(), pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return;
        }

        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            String decodedHash = WebCloudProtoUtil.decodeHash(setBugLinkMsg.getHash());
            DbIssue issue = findIssue(pm, decodedHash);
            if (issue == null) {
                setResponse(resp, 404, "no such issue " + decodedHash);
                return;
            }
            String bugLink = setBugLinkMsg.hasUrl() ? setBugLinkMsg.getUrl().trim() : null;
            if (bugLink != null && bugLink.length() == 0)
                bugLink = null;
            issue.setBugLink(bugLink);

            issue.setBugLinkType(setBugLinkMsg.hasBugLinkType() ? setBugLinkMsg.getBugLinkType() : null);
            pm.makePersistent(issue);

            tx.commit();

        } finally {
            if (tx.isActive())
                tx.rollback();
        }

        resp.setStatus(200);
    }

    // ========================= end of request handling
    // ================================

    private List<String> decodeHashesForIssues(UploadIssues issues) {
        List<String> hashes = new ArrayList<String>();
        for (Issue issue : issues.getNewIssuesList()) {
            hashes.add(WebCloudProtoUtil.decodeHash(issue.getHash()));
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
            if (tx.isActive())
                tx.rollback();
        }
    }

    private DbIssue createDbIssue(Issue issue) {
        DbIssue dbIssue = persistenceHelper.createDbIssue();
        dbIssue.setHash(WebCloudProtoUtil.decodeHash(issue.getHash()));
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
                invocation = persistenceHelper.getObjectById(pm, persistenceHelper.getDbInvocationClass(), invocationKey);
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
        Query query = pm.newQuery("select from " + persistenceHelper.getDbIssueClassname()
                + " where :hashes.contains(hash)");
        query.setResult("hash");
        Set<String> result = new HashSet<String>((List<String>) query.execute(hashes));
        query.closeAll();
        return result;
    }
}
