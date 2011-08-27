package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;

@SuppressWarnings("serial")
public class QueryServlet extends AbstractFlybushServlet {

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        if (uri.equals("/find-issues")) {
            findIssues(req, resp, pm);

        } else if (uri.equals("/get-recent-evaluations")) {
            getRecentEvaluations(req, resp, pm);
        }
    }

    private void findIssues(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        FindIssues loginMsg = FindIssues.parseFrom(req.getInputStream());

        if (loginMsg.hasVersionInfo()) {
            recordAppVersionStats(req.getRemoteAddr(), pm, loginMsg.getVersionInfo());
        }

        List<String> hashes = WebCloudProtoUtil.decodeHashes(loginMsg.getMyIssueHashesList());
        Map<String, DbIssue> issues = persistenceHelper.findIssues(pm, hashes);
        FindIssuesResponse.Builder issueProtos = FindIssuesResponse.newBuilder();
        issueProtos.setCurrentServerTime(System.currentTimeMillis());
        int found = 0;
        for (String hash : hashes) {
            DbIssue dbIssue = issues.get(hash);
            Builder issueBuilder = Issue.newBuilder();
            if (dbIssue != null) {
                buildTerseIssueProto(dbIssue, issueBuilder);
                found++;
            }

            Issue protoIssue = issueBuilder.build();
            issueProtos.addFoundIssues(protoIssue);
        }
        LOGGER.info("Found on server: " + found + ", missing from server: " + (hashes.size() - found));

        resp.setStatus(200);
        issueProtos.build().writeTo(resp.getOutputStream());
    }

    @SuppressWarnings("unchecked")
    private void getRecentEvaluations(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {
        GetRecentEvaluations recentEvalsRequest = GetRecentEvaluations.parseFrom(req.getInputStream());
        long startTime = recentEvalsRequest.getTimestamp();

        String limitParam = req.getParameter("_debug_max");
        int limit = limitParam != null ? Integer.parseInt(limitParam) : 10;
        LOGGER.info("Looking for " + limit + " updates since " + new Date(startTime) + " for " + req.getRemoteAddr());
        // we request limit+1 so we can tell the client whether there are more results beyond the limit they provided.
        int queryLimit = limit + 1;

        Query query = pm.newQuery();
        query.setClass(persistenceHelper.getDbEvaluationClass());
        query.setFilter("when > " + startTime);
        query.setOrdering("when ascending");
        query.setRange(0, queryLimit);

        List<DbEvaluation> evaluations = (List<DbEvaluation>) query.execute();
        int resultCount = evaluations.size();
        LOGGER.fine(resultCount + " results");
        RecentEvaluations.Builder issueProtos = RecentEvaluations.newBuilder();
        issueProtos.setCurrentServerTime(System.currentTimeMillis());
        Iterator<DbEvaluation> iterator = evaluations.iterator();
        Map<String, SortedSet<DbEvaluation>> issues = groupUniqueEvaluationsByIssue(iterator, limit);
        boolean askAgain = false;
        try {
            for (SortedSet<DbEvaluation> evaluationsForIssue : issues.values()) {
                DbIssue issue = evaluationsForIssue.iterator().next().getIssue();
                Issue issueProto = buildFullIssueProto(issue, evaluationsForIssue);
                issueProtos.addIssues(issueProto);
                LOGGER.fine(evaluationsForIssue.size() + " evals");
            }
        } catch (RuntimeException e) {
            if (issueProtos.getIssuesCount() > 0) {
                LOGGER.log(Level.WARNING, e.getClass().getSimpleName() + ", returning only "
                        + issueProtos.getIssuesCount() + " results", e);
                askAgain = true;
            } else {
                // just return 500 so the client tries again
                throw e;
            }
        }
        query.closeAll();
        if (!askAgain) {
            askAgain = resultCount > limit;
            LOGGER.info("Returning " + issueProtos.getIssuesCount() + (askAgain ? " (more to come)" : ""));
        }
        issueProtos.setAskAgain(askAgain);

        resp.setStatus(200);
        issueProtos.build().writeTo(resp.getOutputStream());
    }

    // ========================= end of request handling
    // ================================

    @SuppressWarnings({"unchecked"})
    protected void recordAppVersionStats(String ip, PersistenceManager pm, ProtoClasses.VersionInfo loginMsg) {
        String appName = loginMsg.getAppName();
        String appVer = loginMsg.getAppVersion();
        if (appVer == null)
            appVer = "<notgiven>";
        String fbVersion = loginMsg.getFindbugsVersion();
        if (appName != null || fbVersion != null) {
            if (fbVersion == null)
                fbVersion = "<notgiven>";
            LOGGER.info("FindBugs " + fbVersion + (appName != null ? " via " + appName + " " + appVer : ""));
        }

        if (appName != null) {
            long midnightToday = getMidnightToday();
            if (!persistenceHelper.shouldRecordClientStats(ip, appName, appVer, midnightToday)) {
                return;
            }
            Transaction tx = pm.currentTransaction();
            tx.begin();
            try {
                Query q = pm.newQuery("select from "
                        + persistenceHelper.getDbClientVersionStatsClassname()
                        + " where application == :name && version == :ver && dayStart == :todayStart");

                List<DbClientVersionStats> results = (List<DbClientVersionStats>) q.execute(appName, appVer, midnightToday);
                DbClientVersionStats entry;
                if (results.isEmpty()) {
                    LOGGER.info("First hit from this client/version today!");
                    entry = persistenceHelper.createDbClientVersionStats(appName, appVer, midnightToday);
                } else {
                    entry = results.get(0);
                    LOGGER.info("Increasing hit count to " + (entry.getHits() + 1));
                }
                q.closeAll();
                entry.incrHits();
                pm.makePersistent(entry);
                tx.commit();
            } finally {
                if (tx.isActive()) {
                    tx.rollback();
                }
            }
        }
    }

    private long getMidnightToday() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getCurrentTimeMillis());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void buildTerseIssueProto(DbIssue dbIssue, Builder issueBuilder) {
        issueBuilder.setFirstSeen(dbIssue.getFirstSeen()).setLastSeen(dbIssue.getLastSeen());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            String linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkTypeStr(linkType);
        }

        if (dbIssue.hasEvaluations()) {
            addEvaluations(issueBuilder, dbIssue.getEvaluations());
        }
    }

    private Issue buildFullIssueProto(DbIssue dbIssue, Set<? extends DbEvaluation> evaluations) {
        Issue.Builder issueBuilder = Issue.newBuilder()
                .setBugPattern(dbIssue.getBugPattern())
                .setPriority(dbIssue.getPriority())
                .setHash(WebCloudProtoUtil.encodeHash(dbIssue.getHash()))
                .setFirstSeen(dbIssue.getFirstSeen())
                .setLastSeen(dbIssue.getLastSeen())
                .setPrimaryClass(dbIssue.getPrimaryClass());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            String linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkTypeStr(linkType);
        }
        addEvaluations(issueBuilder, evaluations);
        return issueBuilder.build();
    }

    private void addEvaluations(Builder issueBuilder, Set<? extends DbEvaluation> evaluations) {
        for (DbEvaluation dbEval : sortAndFilterEvaluations(evaluations)) {
            Evaluation.Builder eval = Evaluation.newBuilder()
                    .setComment(dbEval.getComment())
                    .setDesignation(dbEval.getDesignation())
                    .setWhen(dbEval.getWhen());
            if (dbEval.getEmail() != null)
                eval.setWho(dbEval.getEmail());
            else
                LOGGER.warning("Warning: evaluation has no email address: " +
                        "issue=" + dbEval.getIssue().getHash()
                        + ", user=" + dbEval.getWhoId()
                        + ", date=" + new Date(dbEval.getWhen()));
            issueBuilder.addEvaluations(eval.build());
        }
    }

    private Map<String, SortedSet<DbEvaluation>> groupUniqueEvaluationsByIssue(Iterable<DbEvaluation> evaluations) {
        Iterator<DbEvaluation> iterator = evaluations.iterator();
        return groupUniqueEvaluationsByIssue(iterator, Integer.MAX_VALUE);
    }

    private Map<String, SortedSet<DbEvaluation>> groupUniqueEvaluationsByIssue(Iterator<DbEvaluation> iterator, int limit) {
        Map<String, SortedSet<DbEvaluation>> issues = new HashMap<String, SortedSet<DbEvaluation>>();
        for (int i = 0; iterator.hasNext() && i < limit; i++) {
            DbEvaluation dbEvaluation = iterator.next();
            String issueHash = dbEvaluation.getIssue().getHash();
            SortedSet<DbEvaluation> evaluationsForIssue = issues.get(issueHash);
            if (evaluationsForIssue == null) {
                evaluationsForIssue = new TreeSet<DbEvaluation>();
                issues.put(issueHash, evaluationsForIssue);
            }
            // only include the latest evaluation for each user
            for (Iterator<DbEvaluation> it = evaluationsForIssue.iterator(); it.hasNext();) {
                DbEvaluation eval = it.next();
                if (eval.getWho().equals(dbEvaluation.getWho()))
                    it.remove();
            }
            evaluationsForIssue.add(dbEvaluation);
        }
        return issues;
    }

}
