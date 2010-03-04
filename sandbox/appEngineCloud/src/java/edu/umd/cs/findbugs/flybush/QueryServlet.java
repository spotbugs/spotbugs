package edu.umd.cs.findbugs.flybush;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
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
import java.util.logging.Level;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHashes;

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

        List<String> hashes = decodeHashes(loginMsg.getMyIssueHashesList());
        Map<String, DbIssue> issues = lookupTimesAndEvaluations(pm, hashes);
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
				"select from " + DbEvaluation.class.getName()
				+ " where when > " + startTime + " order by when"
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

	private void getEvaluations(HttpServletRequest req, HttpServletResponse resp,
                                PersistenceManager pm) throws IOException {

		GetEvaluations evalsRequest = GetEvaluations.parseFrom(req.getInputStream());
        if (isAuthenticated(resp, pm, evalsRequest.getSessionId()))
            return;

		RecentEvaluations.Builder response = RecentEvaluations.newBuilder();
		for (DbIssue issue : lookupIssues(decodeHashes(evalsRequest.getHashesList()), pm)) {
			Issue issueProto = buildFullIssueProto(issue, issue.getEvaluations(), pm);
			response.addIssues(issueProto);
		}

		resp.setStatus(200);
		ServletOutputStream output = resp.getOutputStream();
		response.build().writeTo(output);
		output.close();
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

    private Issue buildFullIssueProto(DbIssue dbIssue, Set<DbEvaluation> evaluations, PersistenceManager pm) {
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

    private void addEvaluations(Builder issueBuilder, Set<DbEvaluation> evaluations, PersistenceManager pm) {
        for (DbEvaluation dbEval : sortAndFilterEvaluations(evaluations)) {
			issueBuilder.addEvaluations(Evaluation.newBuilder()
					.setComment(dbEval.getComment())
					.setDesignation(dbEval.getDesignation())
					.setWhen(dbEval.getWhen())
					.setWho(pm.getObjectById(DbUser.class, dbEval.getWho()).getEmail()).build());
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
            boolean userIsNew = seenUsernames.add(dbEvaluation.getWho().getName());
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
        Query query = pm.newQuery("select hash, firstSeen, lastSeen, bugLink, bugLinkType, hasEvaluations, evaluations from "
                                  + DbIssue.class.getName() + " where :hashes.contains(hash)");
        List<Object[]> results = (List<Object[]>) query.execute(hashes);
        Map<String,DbIssue> map = new HashMap<String, DbIssue>();
        for (Object[] result : results) {
            DbIssue issue = new DbIssue();
            issue.setHash((String) result[0]);
            issue.setFirstSeen((Long) result[1]);
            issue.setLastSeen((Long) result[2]);
            issue.setBugLink((String) result[3]);
            try {
                DbIssue.DbBugLinkType linkType = (DbIssue.DbBugLinkType) result[4];
                if (linkType != null) {
                    issue.setBugLinkType(DbIssue.DbBugLinkType.valueOf(linkType.name()));
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
}
