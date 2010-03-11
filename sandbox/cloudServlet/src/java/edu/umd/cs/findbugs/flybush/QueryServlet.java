package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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

    private void findIssues(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		FindIssues loginMsg = FindIssues.parseFrom(req.getInputStream());
        long sessionId = loginMsg.getSessionId();
        if (isAuthenticated(resp, pm, sessionId))
            return;

        List<String> hashes = AppEngineProtoUtil.decodeHashes(loginMsg.getMyIssueHashesList());
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
        LOGGER.warning("Found: " + found + ", missing: " + (hashes.size()-found));
        
		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
	}

    @SuppressWarnings("unchecked")
	private void getRecentEvaluations(HttpServletRequest req,
			HttpServletResponse resp, PersistenceManager pm) throws IOException {
		GetRecentEvaluations recentEvalsRequest = GetRecentEvaluations.parseFrom(req.getInputStream());
        if (isAuthenticated(resp, pm, recentEvalsRequest.getSessionId())) return;
		long startTime = recentEvalsRequest.getTimestamp();
		Query query = pm.newQuery(
				"select from " + persistenceHelper.getDbEvaluationClass().getName()
				+ " where when > " + startTime + " order by when ascending"
				);
		SortedSet<DbEvaluation> evaluations = new TreeSet((List<DbEvaluation>) query.execute());
		RecentEvaluations.Builder issueProtos = RecentEvaluations.newBuilder();
		Map<String, SortedSet<DbEvaluation>> issues = groupUniqueEvaluationsByIssue(evaluations);
		for (SortedSet<DbEvaluation> evaluationsForIssue : issues.values()) {
			DbIssue issue = evaluations.iterator().next().getIssue();
			Issue issueProto = buildFullIssueProto(issue, evaluationsForIssue, pm);
			issueProtos.addIssues(issueProto);
		}
		query.closeAll();

		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
	}

    // ========================= end of request handling ================================

    private void buildTerseIssueProto(DbIssue dbIssue, Builder issueBuilder, PersistenceManager pm) {
        issueBuilder.setFirstSeen(dbIssue.getFirstSeen())
                .setLastSeen(dbIssue.getLastSeen());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            DbIssue.DbBugLinkType linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkType(ProtoClasses.BugLinkType.valueOf(linkType.name()));
        }

        if (dbIssue.hasEvaluations()) {
            addEvaluations(issueBuilder, dbIssue.getEvaluations(), pm);
        }
    }

    private Issue buildFullIssueProto(DbIssue dbIssue, Set<? extends DbEvaluation> evaluations, PersistenceManager pm) {
		Issue.Builder issueBuilder = Issue.newBuilder()
				.setBugPattern(dbIssue.getBugPattern())
				.setPriority(dbIssue.getPriority())
				.setHash(AppEngineProtoUtil.encodeHash(dbIssue.getHash()))
				.setFirstSeen(dbIssue.getFirstSeen())
				.setLastSeen(dbIssue.getLastSeen())
				.setPrimaryClass(dbIssue.getPrimaryClass());
        if (dbIssue.getBugLink() != null) {
            issueBuilder.setBugLink(dbIssue.getBugLink());
            DbIssue.DbBugLinkType linkType = dbIssue.getBugLinkType();
            if (linkType != null)
                issueBuilder.setBugLinkType(ProtoClasses.BugLinkType.valueOf(linkType.name()));
        }
        addEvaluations(issueBuilder, evaluations, pm);
        return issueBuilder.build();
	}

    private void addEvaluations(Builder issueBuilder, Set<? extends DbEvaluation> evaluations, PersistenceManager pm) {
        for (DbEvaluation dbEval : sortAndFilterEvaluations(evaluations)) {
			issueBuilder.addEvaluations(Evaluation.newBuilder()
					.setComment(dbEval.getComment())
					.setDesignation(dbEval.getDesignation())
					.setWhen(dbEval.getWhen())
					.setWho(persistenceHelper.getObjectById(pm, persistenceHelper.getDbUserClass(), dbEval.getWho()).getEmail()).build());
		}
    }

    private static LinkedList<DbEvaluation> sortAndFilterEvaluations(Set<? extends DbEvaluation> origEvaluations) {
        Set<String> seenUsernames = new HashSet<String>();
        List<DbEvaluation> evaluationsList = new ArrayList<DbEvaluation>(origEvaluations);
        Collections.sort(evaluationsList);
        int numEvaluations = evaluationsList.size();
        LinkedList<DbEvaluation> result = new LinkedList<DbEvaluation>();
        for (ListIterator<DbEvaluation> it = evaluationsList.listIterator(numEvaluations); it.hasPrevious();) {
            DbEvaluation dbEvaluation = it.previous();
            boolean userIsNew = seenUsernames.add(dbEvaluation.getWhoId());
            if (userIsNew) {
                result.add(0, dbEvaluation);
            }
        }
        return result;
    }

    private Map<String, SortedSet<DbEvaluation>> groupUniqueEvaluationsByIssue(SortedSet<DbEvaluation> evaluations) {
		Map<String,SortedSet<DbEvaluation>> issues = new HashMap<String, SortedSet<DbEvaluation>>();
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
