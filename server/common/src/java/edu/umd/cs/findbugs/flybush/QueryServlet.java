package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
        // long sessionId = loginMsg.getSessionId();
        // if (isAuthenticated(resp, pm, sessionId))
        // return;

        List<String> hashes = WebCloudProtoUtil.decodeHashes(loginMsg.getMyIssueHashesList());
        Map<String, DbIssue> issues = persistenceHelper.findIssues(pm, hashes);
        FindIssuesResponse.Builder issueProtos = FindIssuesResponse.newBuilder();
        int found = 0;
        for (String hash : hashes) {
            DbIssue dbIssue = issues.get(hash);
            Builder issueBuilder = Issue.newBuilder();
            if (dbIssue != null) {
                buildTerseIssueProto(dbIssue, issueBuilder, pm);
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
        LOGGER.info("Looking for updates since " + new Date(startTime) + " for " + req.getRemoteAddr());
        
        Query query = pm.newQuery("select from " + persistenceHelper.getDbEvaluationClass().getName() + " where when > "
                + startTime + " order by when ascending");
        SortedSet<DbEvaluation> evaluations = new TreeSet((List<DbEvaluation>) query.execute());
        LOGGER.info("Found " + evaluations.size());
        RecentEvaluations.Builder issueProtos = RecentEvaluations.newBuilder();
        Map<String, SortedSet<DbEvaluation>> issues = groupUniqueEvaluationsByIssue(evaluations);
        for (SortedSet<DbEvaluation> evaluationsForIssue : issues.values()) {
            DbIssue issue = evaluationsForIssue.iterator().next().getIssue();
            Issue issueProto = buildFullIssueProto(issue, evaluationsForIssue, pm);
            issueProtos.addIssues(issueProto);
        }
        query.closeAll();

        resp.setStatus(200);
        issueProtos.build().writeTo(resp.getOutputStream());
    }

    // ========================= end of request handling
    // ================================

    private void buildTerseIssueProto(DbIssue dbIssue, Builder issueBuilder, PersistenceManager pm) {
        issueBuilder.setFirstSeen(dbIssue.getFirstSeen()).setLastSeen(dbIssue.getLastSeen());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            String linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkTypeStr(linkType);
        }

        if (dbIssue.hasEvaluations()) {
            addEvaluations(issueBuilder, dbIssue.getEvaluations(), pm);
        }
    }

    private Issue buildFullIssueProto(DbIssue dbIssue, Set<? extends DbEvaluation> evaluations, PersistenceManager pm) {
        Issue.Builder issueBuilder = Issue.newBuilder().setBugPattern(dbIssue.getBugPattern()).setPriority(dbIssue.getPriority())
                .setHash(WebCloudProtoUtil.encodeHash(dbIssue.getHash())).setFirstSeen(dbIssue.getFirstSeen())
                .setLastSeen(dbIssue.getLastSeen()).setPrimaryClass(dbIssue.getPrimaryClass());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            String linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkTypeStr(linkType);
        }
        addEvaluations(issueBuilder, evaluations, pm);
        return issueBuilder.build();
    }

    private void addEvaluations(Builder issueBuilder, Set<? extends DbEvaluation> evaluations, PersistenceManager pm) {
        for (DbEvaluation dbEval : sortAndFilterEvaluations(evaluations)) {
            issueBuilder.addEvaluations(Evaluation.newBuilder().setComment(dbEval.getComment())
                    .setDesignation(dbEval.getDesignation()).setWhen(dbEval.getWhen())
                    .setWho(persistenceHelper.getObjectById(pm, persistenceHelper.getDbUserClass(), dbEval.getWho()).getEmail())
                    .build());
        }
    }

    private Map<String, SortedSet<DbEvaluation>> groupUniqueEvaluationsByIssue(SortedSet<DbEvaluation> evaluations) {
        Map<String, SortedSet<DbEvaluation>> issues = new HashMap<String, SortedSet<DbEvaluation>>();
        for (DbEvaluation dbEvaluation : evaluations) {
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
