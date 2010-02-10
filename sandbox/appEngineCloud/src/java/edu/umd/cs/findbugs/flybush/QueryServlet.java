package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletOutputStream;
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

        } else if (uri.equals("/get-evaluations")) {
            getEvaluations(req, resp, pm);

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
        Map<String, DbIssue> issues = lookupTimesAndEvaluations(pm, hashes);
		FindIssuesResponse.Builder issueProtos = FindIssuesResponse.newBuilder();
        for (String hash : hashes) {
            DbIssue issue = issues.get(hash);
            Issue.Builder issueBuilder = Issue.newBuilder();
            if (issue != null) {
                issueBuilder.setFirstSeen(issue.getFirstSeen())
                        .setLastSeen(issue.getLastSeen());

                if (issue.hasEvaluations()) {
                    addEvaluations(issueBuilder, issue.getEvaluations());
                }
            }

            issueProtos.addFoundIssues(issueBuilder.build());
        }
        
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
				"select from " + DbEvaluation.class.getName()
				+ " where when > " + startTime + " order by when"
				);
		SortedSet<DbEvaluation> evaluations = new TreeSet((List<DbEvaluation>) query.execute());
		RecentEvaluations.Builder issueProtos = RecentEvaluations.newBuilder();
		Map<String, SortedSet<DbEvaluation>> issues = groupUniqueEvaluationsByIssue(evaluations);
		for (SortedSet<DbEvaluation> evaluationsForIssue : issues.values()) {
			DbIssue issue = evaluations.iterator().next().getIssue();
			Issue issueProto = buildIssueProto(issue, evaluationsForIssue);
			issueProtos.addIssues(issueProto);
		}
		query.closeAll();

		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
	}

	private void getEvaluations(HttpServletRequest req,
			HttpServletResponse resp, PersistenceManager pm) throws IOException {

		GetEvaluations evalsRequest = GetEvaluations.parseFrom(req.getInputStream());
        if (isAuthenticated(resp, pm, evalsRequest.getSessionId()))
            return;

		RecentEvaluations.Builder response = RecentEvaluations.newBuilder();
		for (DbIssue issue : lookupIssues(AppEngineProtoUtil.decodeHashes(evalsRequest.getHashesList()), pm)) {
			Issue issueProto = buildIssueProto(issue, issue.getEvaluations());
			response.addIssues(issueProto);
		}

		resp.setStatus(200);
		ServletOutputStream output = resp.getOutputStream();
		response.build().writeTo(output);
		output.close();
	}

    // ========================= end of request handling ================================

    private Issue buildIssueProto(DbIssue issue, Set<DbEvaluation> evaluations) {
		Issue.Builder issueBuilder = Issue.newBuilder()
				.setBugPattern(issue.getBugPattern())
				.setPriority(issue.getPriority())
				.setHash(AppEngineProtoUtil.encodeHash(issue.getHash()))
				.setFirstSeen(issue.getFirstSeen())
				.setLastSeen(issue.getLastSeen())
				.setPrimaryClass(issue.getPrimaryClass());
        addEvaluations(issueBuilder, evaluations);
        return issueBuilder.build();
	}

    private void addEvaluations(Builder issueBuilder, Set<DbEvaluation> evaluations) {
        for (DbEvaluation dbEval : sortAndFilterEvaluations(evaluations)) {
			issueBuilder.addEvaluations(Evaluation.newBuilder()
					.setComment(dbEval.getComment())
					.setDesignation(dbEval.getDesignation())
					.setWhen(dbEval.getWhen())
					.setWho(dbEval.getWho()).build());
		}
    }

    private static LinkedList<DbEvaluation> sortAndFilterEvaluations(Set<DbEvaluation> origEvaluations) {
        Set<String> seenUsernames = new HashSet<String>();
        List<DbEvaluation> evaluationsList = new ArrayList<DbEvaluation>(origEvaluations);
        Collections.sort(evaluationsList);
        int numEvaluations = evaluationsList.size();
        LinkedList<DbEvaluation> result = new LinkedList<DbEvaluation>();
        for (ListIterator<DbEvaluation> it = evaluationsList.listIterator(numEvaluations); it.hasPrevious();) {
            DbEvaluation dbEvaluation = it.previous();
            boolean userIsNew = seenUsernames.add(dbEvaluation.getWho());
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

	@SuppressWarnings("unchecked")
	private List<DbIssue> lookupIssues(Iterable<String> hashes, PersistenceManager pm) {
		Query query = pm.newQuery("select from " + DbIssue.class.getName() + " where :hashes.contains(hash)");
        return (List<DbIssue>) query.execute(hashes);
	}

    @SuppressWarnings("unchecked")
    private Map<String, DbIssue> lookupTimesAndEvaluations(PersistenceManager pm, List<String> hashes) {
        Query query = pm.newQuery("select hash, firstSeen, lastSeen, hasEvaluations, evaluations from "
                                  + DbIssue.class.getName() + " where :hashes.contains(hash)");
        List<Object[]> results = (List<Object[]>) query.execute(hashes);
        Map<String,DbIssue> map = new HashMap<String, DbIssue>();
        for (Object[] result : results) {
            DbIssue issue = new DbIssue();
            issue.setHash((String) result[0]);
            issue.setFirstSeen((Long) result[1]);
            issue.setLastSeen((Long) result[2]);
            issue.setHasEvaluations((Boolean) result[3]);
            issue.setEvaluationsDontLook((Set<DbEvaluation>) result[4]);
            map.put(issue.getHash(), issue);
		}
        return map;
    }
}
