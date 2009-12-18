package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.http.*;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.HashList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList.Builder;

@SuppressWarnings("serial")
public class FlybushServlet extends HttpServlet {
	private final PersistenceManagerFactory pmf;

	public FlybushServlet() {
		this(null);
	}

	/** for testing */
	FlybushServlet(PersistenceManagerFactory pmf) {
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

	            PersistenceManager pm = getPMF().getPersistenceManager();
	            try {
	                pm.makePersistent(session);
	            } finally {
	                pm.close();
	            }

	            resp.setStatus(200);
	            resp.setContentType("text/html");
	            PrintWriter writer = resp.getWriter();
				writer.println("<h1>You are now signed in</h1>");
	            writer.println("<p style='font-size: large; font-weight: bold'>Please return to the FindBugs application window to continue.</p>");
	            writer.println("<p style='font-style: italic'>Signed in as " + user.getNickname()
	            		       + " (" + user.getEmail() + ")</p>");

	        } else {
	            resp.sendRedirect(userService.createLoginURL(uri));
	        }

		} else if (uri.startsWith("/check-auth/")) {
			long id = Long.parseLong(uri.substring("/check-auth/".length()));
			PersistenceManager pm = getPMF().getPersistenceManager();
		    String query = "select from " + SqlCloudSession.class.getName() + " where randomID == " + id + " order by date desc range 0,1";
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
			Set<String> hashSet = new HashSet<String>(hashes.getHashesList());
			PersistenceManager pm = getPMF().getPersistenceManager();
		    String query = "select from " + DbIssue.class.getName()
		    		+ " where " + formatStrings("hash", hashes.getHashesList());
		    List<DbIssue> issues = (List<DbIssue>) pm.newQuery(query).execute();
		    IssueList.Builder issueProtos = IssueList.newBuilder();
		    for (DbIssue issue : issues) {
				hashSet.remove(issue.getHash());
				issueProtos.addFoundIssues(Issue.newBuilder()
						.setBugPattern(issue.getBugPattern())
						.setPriority(issue.getPriority())
						.setRank(issue.getRank())
						.setHash(issue.getHash())
						.setFirstSeen(issue.getFirstSeen())
						.setLastSeen(issue.getLastSeen())
						.setPrimaryClass(issue.getPrimaryClass())
						.build());
			}
		    issueProtos.addAllMissingIssues(hashSet);
		    issueProtos.build().writeTo(resp.getOutputStream());

		} else {
			resp.setStatus(404);
			resp.setContentType("text/plain");
			resp.getWriter().println("Not Found");
		}
	}

	private String formatStrings(String fieldName, List<String> hashesList) {
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
