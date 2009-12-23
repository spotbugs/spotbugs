package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.HashList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList;

@SuppressWarnings("serial")
public class FlybushServlet extends HttpServlet {
	private PersistenceManagerFactory pmf;

	public FlybushServlet() {
		this(null);
	}

	/** for testing */
	FlybushServlet(PersistenceManagerFactory pmf) {
		this.pmf = pmf;
	}

	/** for testing */
	void setPmf(PersistenceManagerFactory pmf) {
		this.pmf = pmf;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String uri = req.getRequestURI();
		if (uri.startsWith("/browser-auth/")) {
	        UserService userService = UserServiceFactory.getUserService();
	        User user = userService.getCurrentUser();

	        if (user != null) {
	    		long id = Long.parseLong(uri.substring("/browser-auth/".length()));
	            Date date = new Date();
	            SqlCloudSession session = new SqlCloudSession(user, id, date);

	            PersistenceManager pm = getPersistenceManager();
	            try {
	                pm.makePersistent(session);
	            } finally {
	                pm.close();
	            }

	            resp.setStatus(200);
	            resp.setContentType("text/html");
	            PrintWriter writer = resp.getWriter();
				writer.println("<h1>You are now signed in</h1>");
	            writer.println("<p style='font-size: large; font-weight: bold'>"
	            		+ "Please return to the FindBugs application window to continue.</p>");
	            writer.println("<p style='font-style: italic'>Signed in as " + user.getNickname()
	            		       + " (" + user.getEmail() + ")</p>");

	        } else {
	            resp.sendRedirect(userService.createLoginURL(uri));
	        }

		} else if (uri.startsWith("/check-auth/")) {
			long id = Long.parseLong(uri.substring("/check-auth/".length()));
			PersistenceManager pm = getPersistenceManager();
		    String query = "select from " + SqlCloudSession.class.getName() 
		    		+ " where randomID == " + id + " order by date desc range 0,1";
		    List<SqlCloudSession> sessions = (List<SqlCloudSession>) pm.newQuery(query).execute();
            PrintWriter writer = resp.getWriter();
            resp.setContentType("text/plain");
		    if (sessions.isEmpty()) {
		    	resp.setStatus(418);
				writer.println("FAIL");
		    } else {
		    	resp.setStatus(200);
		    	writer.println("OK\n"
		    			+ sessions.get(0).getRandomID() + "\n"
		    			+ sessions.get(0).getAuthor().getEmail());
		    }
		    resp.flushBuffer();

		} else if (uri.equals("/find-issues")) {
			HashList hashes = HashList.parseFrom(req.getInputStream());
			Set<String> hashSet = new LinkedHashSet<String>(hashes.getHashesList());
			IssueList.Builder issueProtos = IssueList.newBuilder();
		    for (DbIssue issue : lookupIssues(hashSet)) {
				hashSet.remove(issue.getHash());
				Issue issueProto = buildIssueProto(issue, issue.getEvaluations());
				issueProtos.addFoundIssues(issueProto);
			}
		    issueProtos.addAllMissingIssues(hashSet);
		    issueProtos.build().writeTo(resp.getOutputStream());

		} else if (uri.startsWith("/get-evaluations/")) {
			long startTime = Long.parseLong(uri.substring("/get-evaluations/".length()));
			List<DbEvaluation> evaluations = (List<DbEvaluation>) getPersistenceManager().newQuery(
					"select from " + DbEvaluation.class.getName()
					+ " where when > " + startTime + " order by when"
					).execute();
			IssueList.Builder issueProtos = IssueList.newBuilder();
			Map<String, List<DbEvaluation>> issues = groupEvaluationsByIssue(evaluations);
			for (List<DbEvaluation> evaluationsForIssue : issues.values()) {
				DbIssue issue = evaluations.get(0).getIssue();
				Issue issueProto = buildIssueProto(issue, evaluationsForIssue);
				issueProtos.addFoundIssues(issueProto);
			}
			resp.setStatus(200);
			issueProtos.build().writeTo(resp.getOutputStream());


		} else {
			show404(resp);
		}
	}

	private void show404(HttpServletResponse resp) throws IOException {
		resp.setStatus(404);
		resp.setContentType("text/plain");
		resp.getWriter().println("Not Found");
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		if (req.getRequestURI().equals("/upload-issues")) {
			PersistenceManager pm = getPersistenceManager();
			IssueList issues = IssueList.parseFrom(req.getInputStream());
			List<String> hashes = new ArrayList<String>();
			for (Issue issue : issues.getFoundIssuesList()) {
				hashes.add(issue.getHash());
			}
			HashSet<String> existingIssueHashes = lookupHashes(hashes);
			List<DbIssue> newDbIssues = new ArrayList<DbIssue>();
			for (Issue issue : issues.getFoundIssuesList()) {
				if (!existingIssueHashes.contains(issue.getHash())) {
					DbIssue dbIssue = new DbIssue();
					dbIssue.setHash(issue.getHash());
					dbIssue.setBugPattern(issue.getBugPattern());
					dbIssue.setPriority(issue.getPriority());
					dbIssue.setRank(issue.getRank());
					dbIssue.setPrimaryClass(issue.getPrimaryClass());
					dbIssue.setFirstSeen(issue.getFirstSeen());
					dbIssue.setLastSeen(issue.getLastSeen());
					newDbIssues.add(dbIssue);
				}
			}
			pm.makePersistentAll(newDbIssues);
			resp.setStatus(200);
			resp.setContentType("text/plain");
		} else {
			show404(resp);
		}
	}

	private Issue buildIssueProto(DbIssue issue, List<DbEvaluation> evaluations) {
		Issue.Builder issueBuilder = Issue.newBuilder()
				.setBugPattern(issue.getBugPattern())
				.setPriority(issue.getPriority())
				.setRank(issue.getRank())
				.setHash(issue.getHash())
				.setFirstSeen(issue.getFirstSeen())
				.setLastSeen(issue.getLastSeen())
				.setPrimaryClass(issue.getPrimaryClass());
		for (DbEvaluation dbEval : evaluations) {
			issueBuilder.addEvaluations(Evaluation.newBuilder()
					.setComment(dbEval.getComment())
					.setDesignation(dbEval.getDesignation())
					.setWhen(dbEval.getWhen())
					.setWho(dbEval.getWho()).build());
		}
		Issue issueProto = issueBuilder.build();
		return issueProto;
	}

	private Map<String, List<DbEvaluation>> groupEvaluationsByIssue(
			List<DbEvaluation> evaluations) {
		Map<String,List<DbEvaluation>> issues = new HashMap<String, List<DbEvaluation>>();
		for (DbEvaluation dbEvaluation : evaluations) {
			String issueHash = dbEvaluation.getIssue().getHash();
			List<DbEvaluation> evaluationsForIssue = issues.get(issueHash);
			if (evaluationsForIssue == null) {
				evaluationsForIssue = new ArrayList<DbEvaluation>();
				issues.put(issueHash, evaluationsForIssue);
			}
			evaluationsForIssue.add(dbEvaluation);
		}
		return issues;
	}

	private PersistenceManager getPersistenceManager() {
		return getPMF().getPersistenceManager();
	}

	private HashSet<String> getHashes(List<DbIssue> lookupIssues) {
		HashSet<String> hashes = new HashSet<String>();
		for (DbIssue dbIssue : lookupIssues) {
			hashes.add(dbIssue.getHash());
		}
		return hashes;
	}

	private List<DbIssue> lookupIssues(Iterable<String> hashes) {
		List<DbIssue> allIssues = new ArrayList<DbIssue>();
		PersistenceManager pm = getPersistenceManager();
		for (List<String> partition : partition(hashes, 10)) {
			allIssues.addAll((List<DbIssue>) pm.newQuery("select from " + DbIssue.class.getName()
					+ " where " + formatStrings("hash", partition)).execute());
		}
		return allIssues;
	}

	private HashSet<String> lookupHashes(Iterable<String> hashes) {
		HashSet<String> allHashes = new HashSet<String>();
		PersistenceManager pm = getPersistenceManager();
		for (List<String> partition : partition(hashes, 10)) {
			Query query = pm.newQuery("select from " + DbIssue.class.getName()
					+ " where " + formatStrings("hash", partition));
			query.setResult("hash");
			allHashes.addAll((List<String>) query.execute());
		}
		return allHashes;
	}

	private <E> List<List<E>> partition(Iterable<E> collection, int partitionSize) {
		List<List<E>> partitions = new ArrayList<List<E>>();
		partitions.add(new ArrayList<E>());
		for (E hash : collection) {
			List<E> currentPartition = partitions.get(partitions.size()-1);
			if (currentPartition.size() == partitionSize) {
				currentPartition = new ArrayList<E>();
				partitions.add(currentPartition);
			}
			currentPartition.add(hash);
		}
		return partitions;
	}

	private String formatStrings(String fieldName, Iterable<String> hashesList) {
		StringBuilder str = new StringBuilder();
		boolean first = true;
		for (String hash : hashesList) {
			if (!first) str.append(" || ");

			str.append(fieldName);
			str.append(" == ");
			str.append('\"');
			str.append(hash);
			str.append('\"');

			first = false;
		}
		return str.toString();
	}

	private PersistenceManagerFactory getPMF() {
		if (pmf == null) return PMF.get();
		else  return pmf;
	}
}
