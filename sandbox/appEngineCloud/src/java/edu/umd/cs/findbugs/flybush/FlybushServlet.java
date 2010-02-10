package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.apphosting.api.DeadlineExceededException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import org.datanucleus.store.query.QueryResult;

import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class FlybushServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(FlybushServlet.class.getName());

	private PersistenceManager persistenceManager;

	public FlybushServlet() {
		this(null);
	}

	/** for testing */
	FlybushServlet(PersistenceManager pm) {
		setPersistenceManager(pm);
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

        long start = System.currentTimeMillis();
		PersistenceManager pm = getPersistenceManager();
        LOGGER.warning("loading PM took " + (System.currentTimeMillis() - start) + "ms");

		try {
			if (uri.equals("/log-in")) {
				logIn(req, resp, pm);

			} else if (uri.startsWith("/log-out/")) {
				logOut(req, resp, pm);

            } else if (uri.equals("/clear-all-data")) {
                clearAllData(resp);

			} else if (uri.equals("/find-issues")) {
				findIssues(req, resp, pm);

			} else if (uri.equals("/upload-issues")) {
				uploadIssues(req, resp, pm);

			} else if (uri.equals("/upload-evaluation")) {
				uploadEvaluation(req, resp, pm);

			} else if (uri.equals("/get-evaluations")) {
				getEvaluations(req, resp, pm);

			} else if (uri.equals("/get-recent-evaluations")) {
				getRecentEvaluations(req, resp, pm);

			} else {
				show404(resp);
			}
		} finally {
			pm.close();
		}
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

	private void logOut(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		long id = Long.parseLong(req.getPathInfo().substring("/log-out/".length()));
		Query query = pm.newQuery("select from " + SqlCloudSession.class.getName()
                                  + " where randomID == :idToDelete");
		Transaction tx = pm.currentTransaction();
		tx.begin();
		long deleted = 0;
		try {
			deleted = query.deletePersistentAll(Long.toString(id));
			query.execute();
			tx.commit();
		} finally {
			if (tx.isActive()) tx.rollback();
		}
		if (deleted >= 1) {
			resp.setStatus(200);
		} else {
			setResponse(resp, 404, "no such session");
		}
	}

	private void logIn(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
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
		resp.setStatus(200);
	}

	private void clearAllData(HttpServletResponse resp) throws IOException {
        int deleted = 0;
        try {
            DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
            for (Entity entity : ds.prepare(new com.google.appengine.api.datastore.Query().setKeysOnly()).asIterable()) {
                ds.delete(entity.getKey());
                deleted++;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not clear all data - only " + deleted + " entities", e);
        }
        setResponse(resp, 200, "Deleted " + deleted + " entities");
    }

	private void findIssues(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
        long start = System.currentTimeMillis();
		FindIssues loginMsg = FindIssues.parseFrom(req.getInputStream());
        LOGGER.warning("parsing took " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
        long sessionId = loginMsg.getSessionId();
        if (isAuthenticated(resp, pm, sessionId))
            return;
        LOGGER.warning("authenticating took " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();

        List<String> hashes = AppEngineProtoUtil.decodeHashes(loginMsg.getMyIssueHashesList());
        Map<String, DbIssue> issues = lookupTimesAndEvaluations(pm, hashes);
        LOGGER.warning("looking up took " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
		FindIssuesResponse.Builder issueProtos = FindIssuesResponse.newBuilder();
        for (String hash : hashes) {
            long istart = System.currentTimeMillis();
            DbIssue issue = issues.get(hash);
            Issue.Builder issueBuilder = Issue.newBuilder();
            if (issue != null) {
                issueBuilder.setFirstSeen(issue.getFirstSeen())
                        .setLastSeen(issue.getLastSeen());

                long estart = System.currentTimeMillis();
                LOGGER.warning("* * getting evaluations took " + (System.currentTimeMillis() - estart) + "ms");
                estart = System.currentTimeMillis();
                if (issue.hasEvaluations()) {
                    addEvaluations(issueBuilder, issue.getEvaluations());
                }
                LOGGER.warning("* *  processing evaluations took " + (System.currentTimeMillis() - estart) + "ms");
            }

            issueProtos.addFoundIssues(issueBuilder.build());
            LOGGER.warning("* building issue took " + (System.currentTimeMillis() - istart) + "ms");
        }
        
        LOGGER.warning("building response took " + (System.currentTimeMillis() - start) + "ms");
        start = System.currentTimeMillis();
		resp.setStatus(200);
		issueProtos.build().writeTo(resp.getOutputStream());
        LOGGER.warning("sending took " + (System.currentTimeMillis() - start) + "ms");
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
			hashes.add(AppEngineProtoUtil.decodeHash(issue.getHash()));
		}
		Set<String> existingIssueHashes = lookupHashes(hashes, pm);
		for (Issue issue : issues.getNewIssuesList()) {
			if (!existingIssueHashes.contains(AppEngineProtoUtil.decodeHash(issue.getHash()))) {
				DbIssue dbIssue = new DbIssue();
				dbIssue.setHash(AppEngineProtoUtil.decodeHash(issue.getHash()));
				dbIssue.setBugPattern(issue.getBugPattern());
				dbIssue.setPriority(issue.getPriority());
				dbIssue.setPrimaryClass(issue.getPrimaryClass());
				dbIssue.setFirstSeen(issue.getFirstSeen());
				dbIssue.setLastSeen(issue.getFirstSeen()); // ignore last seen

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
				invocation = pm.getObjectById(DbInvocation.class, invocationKey);
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

			String hash = AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash());
			DbIssue issue = findIssue(pm, hash);
			if (issue == null) {
				setResponse(resp, 404, "no such issue " + AppEngineProtoUtil.decodeHash(uploadEvalMsg.getHash()));
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

	/** for testing */
	void setPersistenceManager(PersistenceManager persistenceManager) {
		this.persistenceManager = persistenceManager;
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

    private boolean isAuthenticated(HttpServletResponse resp, PersistenceManager pm, long sessionId) throws IOException {
        SqlCloudSession session = lookupCloudSessionById(sessionId, pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return true;
        }
        return false;
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
					" where randomID == :randomIDquery");
        List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute(Long.toString(id));
        return sessions.isEmpty() ? null : sessions.get(0);
	}

	@SuppressWarnings("unchecked")
	private DbIssue findIssue(PersistenceManager pm, String hash) {
		Query query = pm.newQuery(DbIssue.class, "hash == :hashParam");
        Iterator<DbIssue> it = ((QueryResult) query.execute(hash)).iterator();
        if (!it.hasNext()) {
            return null;
        }
        return it.next();
	}

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
//        query.closeAll();
        return map;
    }

	@SuppressWarnings("unchecked")
	private Set<String> lookupHashes(Iterable<String> hashes, PersistenceManager pm) {
		Query query = pm.newQuery("select from " + DbIssue.class.getName()
				+ " where :hashes.contains(hash)");
		query.setResult("hash");
		Set<String> result = new HashSet<String>((List<String>) query.execute(hashes));
		query.closeAll();
		return result;
	}

	private PersistenceManager getPersistenceManager() {
		if (persistenceManager != null) {
			return persistenceManager;
		}

		return PMF.get().getPersistenceManager();

	}
}
