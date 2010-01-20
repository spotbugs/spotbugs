package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.datanucleus.store.query.QueryResult;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.DeadlineExceededException;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

@SuppressWarnings("serial")
public class FlybushServlet extends HttpServlet {
	private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[0-9A-Za-z_-]+");

	private static final Logger LOGGER = Logger.getLogger(FlybushServlet.class.getName());

	private PersistenceManager persistenceManager;

	public FlybushServlet() {
		this(null);
	}

	/** for testing */
	FlybushServlet(PersistenceManager pm) {
		setPersistenceManager(pm);
	}

	/** for testing */
	void setPersistenceManager(PersistenceManager persistenceManager) {
		this.persistenceManager = persistenceManager;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		try {
			String uri = req.getPathInfo();
			PersistenceManager pm = getPersistenceManager();
			try {
				if (uri.startsWith("/browser-auth/")) {
					browserAuth(req, resp, pm);

				} else if (uri.startsWith("/check-auth/")) {
					checkAuth(req, resp, pm);

				} else {
					show404(resp);
				}
			} finally {
				pm.close();
			}
		} catch (DeadlineExceededException e) {
			LOGGER.log(Level.SEVERE, "Timed out", e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String uri = req.getPathInfo();

		PersistenceManager pm = getPersistenceManager();
		try {
			if (uri.equals("/find-issues")) {
				findIssues(req, resp, pm);

			} else if (uri.equals("/upload-issues")) {
				uploadIssues(req, resp, pm);

			} else if (uri.equals("/upload-evaluation")) {
				uploadEvaluation(req, resp, pm);

			} else if (uri.equals("/get-evaluations")) {
				getEvaluations(req, resp, pm);

			} else {
				show404(resp);
			}
		} finally {
			pm.close();
		}
	}

	private void show404(HttpServletResponse resp) throws IOException {
		setResponse(resp, 404, "Not Found");
	}

	private void setResponse(HttpServletResponse resp, int statusCode, String textResponse)
			throws IOException {
		resp.setStatus(statusCode);
		resp.setContentType("text/plain");
		resp.getWriter().println(textResponse);
	}

	private void browserAuth(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();

		if (user != null) {
			long id = Long.parseLong(req.getPathInfo().substring("/browser-auth/".length()));
		    Date date = new Date();
		    SqlCloudSession session = new SqlCloudSession(user, id, date);

		    Transaction tx = pm.currentTransaction();
		    tx.begin();
		    try {
				pm.makePersistent(session);
				tx.commit();
			} finally {
				if (tx.isActive()) tx.rollback();
			}
			resp.setStatus(200);
		    resp.setContentType("text/html");
		    PrintWriter writer = resp.getWriter();
		    writer.println("<title>FindBugs Cloud</title>");
			writer.println("<h1>You are now signed in</h1>");
		    writer.println("<p style='font-size: large; font-weight: bold'>"
		    		+ "Please return to the FindBugs application window to continue.</p>");
		    writer.println("<p style='font-style: italic'>Signed in as " + user.getNickname()
		    		       + " (" + user.getEmail() + ")</p>");

		} else {
		    resp.sendRedirect(userService.createLoginURL(req.getPathInfo()));
		}
	}

	private void checkAuth(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		long id = Long.parseLong(req.getPathInfo().substring("/check-auth/".length()));
		SqlCloudSession sqlCloudSession = lookupCloudSessionById(id, pm);
		if (sqlCloudSession == null) {
			setResponse(resp, 418, "FAIL");
		} else {
			setResponse(resp, 200,
					"OK\n"
					+ sqlCloudSession.getRandomID() + "\n"
					+ sqlCloudSession.getUser().getNickname());
		}
		resp.flushBuffer();
	}

	@SuppressWarnings("unchecked")
	private void getEvaluations(HttpServletRequest req,
			HttpServletResponse resp, PersistenceManager pm) throws IOException {
		GetRecentEvaluations recentEvalsRequest = GetRecentEvaluations.parseFrom(req.getInputStream());
		SqlCloudSession sqlCloudSession = lookupCloudSessionById(recentEvalsRequest.getSessionId(), pm);
		if (sqlCloudSession == null) {
			setResponse(resp, 403, "not authenticated");
			return;
		}
		long startTime = recentEvalsRequest.getTimestamp();
		Query query = pm.newQuery(
				"select from " + DbEvaluation.class.getName()
				+ " where when > " + startTime + " order by when"
				);
		List<DbEvaluation> evaluations = (List<DbEvaluation>) query.execute();
		RecentEvaluations.Builder issueProtos = RecentEvaluations.newBuilder();
		Map<String, List<DbEvaluation>> issues = groupEvaluationsByIssue(evaluations);
		for (List<DbEvaluation> evaluationsForIssue : issues.values()) {
			DbIssue issue = evaluations.get(0).getIssue();
			Issue issueProto = buildIssueProto(issue, evaluationsForIssue);
			issueProtos.addIssues(issueProto);
		}
		query.closeAll();

		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
	}

	private void findIssues(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		LogIn loginMsg = LogIn.parseFrom(req.getInputStream());
		SqlCloudSession session = lookupCloudSessionById(loginMsg.getSessionId(), pm);
		if (session == null) {
			setResponse(resp, 403, "not authenticated");
			return;
		}

		DbInvocation invocation = new DbInvocation();
		invocation.setWho(session.getUser().getNickname());
		invocation.setStartTime(loginMsg.getAnalysisTimestamp());

		Transaction tx = pm.currentTransaction();
		tx.begin();
		try {
			invocation = pm.makePersistent(invocation);
			Key invocationKey = invocation.getKey();
			session.setInvocation(invocationKey);
			pm.makePersistent(session);
			tx.commit();
		} finally {
			if (tx.isActive()) {
				tx.rollback();
			}
		}

		LogInResponse.Builder issueProtos = LogInResponse.newBuilder();
		for (DbIssue issue : lookupIssues(loginMsg.getMyIssueHashesList(), pm)) {
			Issue issueProto = buildIssueProto(issue, issue.getEvaluations());
			issueProtos.addFoundIssues(issueProto);
		}
		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
	}

	private void uploadIssues(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		UploadIssues issues = UploadIssues.parseFrom(req.getInputStream());
		SqlCloudSession session = lookupCloudSessionById(issues.getSessionId(), pm);
		if (session == null) {
			resp.setStatus(403);
			return;
		}
		List<String> hashes = new ArrayList<String>();
		for (Issue issue : issues.getNewIssuesList()) {
			hashes.add(issue.getHash());
		}
		HashSet<String> existingIssueHashes = lookupHashes(hashes, pm);
		for (Issue issue : issues.getNewIssuesList()) {
			if (!existingIssueHashes.contains(issue.getHash())) {
				DbIssue dbIssue = new DbIssue();
				dbIssue.setHash(issue.getHash());
				dbIssue.setBugPattern(issue.getBugPattern());
				dbIssue.setPriority(issue.getPriority());
				dbIssue.setPrimaryClass(issue.getPrimaryClass());
				dbIssue.setFirstSeen(issue.getFirstSeen());
				dbIssue.setLastSeen(issue.getLastSeen());
				Transaction tx = pm.currentTransaction();
				tx.begin();
				try {
					pm.makePersistent(dbIssue);
					tx.commit();
				} finally {
					if (tx.isActive()) tx.rollback();
				}
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
		dbEvaluation.setWho(session.getUser().getNickname());
		Key invocationKey = session.getInvocation();
		if (invocationKey != null) {
			DbInvocation invocation;
			try {
				invocation = (DbInvocation) pm.getObjectById(DbInvocation.class, invocationKey);
			if (invocation != null) {
				dbEvaluation.setInvocation(invocation.getKey());
			}
			} catch (JDOObjectNotFoundException e) {
				// ignore
			}
		}
		Transaction tx = pm.currentTransaction();
		boolean setStatusAlready = false;
		try {
		    tx.begin();

			String hash = uploadEvalMsg.getHash();
			DbIssue issue = findIssue(pm, hash);
			if (issue == null) {
				setResponse(resp, 404, "no such issue " + uploadEvalMsg.getHash());
				setStatusAlready  = true;
				return;
			}
			dbEvaluation.setIssue(issue);
			issue.addEvaluation(dbEvaluation);
			pm.makePersistent(issue);

		    tx.commit();

		} finally {
		    if (tx.isActive()) {
		    	tx.rollback();
		    	if (!setStatusAlready) {
		    		setResponse(resp, 403, "Transaction failed");
		    	}
		    }
		}

		resp.setStatus(200);
	}

	private DbEvaluation createDbEvaluation(Evaluation protoEvaluation) {
		DbEvaluation dbEvaluation = new DbEvaluation();
		dbEvaluation.setComment(protoEvaluation.getComment());
		dbEvaluation.setDesignation(protoEvaluation.getDesignation());
		dbEvaluation.setWhen(protoEvaluation.getWhen());
		return dbEvaluation;
	}

	@SuppressWarnings("unchecked")
	private SqlCloudSession lookupCloudSessionById(long id, PersistenceManager pm) {
		Query query = pm.newQuery(
					"select from " + SqlCloudSession.class.getName() +
					" where randomID == " + id + " order by date desc range 0,1");
		try {
			List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute();
			return sessions.isEmpty() ? null : sessions.get(0);
		} finally {
			query.closeAll();
		}
	}

	@SuppressWarnings("unchecked")
	private DbIssue findIssue(PersistenceManager pm, String hash) {
		Query query = pm.newQuery(DbIssue.class, "hash == hashParam");
		try {
			query.declareParameters("String hashParam");
			Iterator<DbIssue> it = ((QueryResult) query.execute(hash))
					.iterator();
			if (!it.hasNext()) {
				return null;
			}
			return it.next();
		} finally {
			query.closeAll();
		}
	}

	private Issue buildIssueProto(DbIssue issue, List<DbEvaluation> evaluations) {
		Issue.Builder issueBuilder = Issue.newBuilder()
				.setBugPattern(issue.getBugPattern())
				.setPriority(issue.getPriority())
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

	@SuppressWarnings("unchecked")
	private List<DbIssue> lookupIssues(Iterable<String> hashes, PersistenceManager pm) {
		List<DbIssue> allIssues = new ArrayList<DbIssue>();
		for (List<String> partition : partition(hashes, 10)) {
			Query query = pm.newQuery("select from " + DbIssue.class.getName()
					+ " where " + makeSqlHashList("hash", partition));
			allIssues.addAll((List<DbIssue>) query.execute());
			query.closeAll();
		}
		return allIssues;
	}

	@SuppressWarnings("unchecked")
	private HashSet<String> lookupHashes(Iterable<String> hashes, PersistenceManager pm) {
		HashSet<String> allHashes = new HashSet<String>();
		for (List<String> partition : partition(hashes, 10)) {
			Query query = pm.newQuery("select from " + DbIssue.class.getName()
					+ " where " + makeSqlHashList("hash", partition));
			query.setResult("hash");
			allHashes.addAll((List<String>) query.execute());
			query.closeAll();
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

	private String makeSqlHashList(String fieldName, Iterable<String> hashesList) {
		StringBuilder str = new StringBuilder();
		boolean first = true;
		for (String hash : hashesList) {
			if (!ALPHANUMERIC_PATTERN.matcher(hash).matches()) {
				continue;
			}
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

	private PersistenceManager getPersistenceManager() {
		if (persistenceManager != null) {
			return persistenceManager;
		}

		return PMF.get().getPersistenceManager();

	}
}
