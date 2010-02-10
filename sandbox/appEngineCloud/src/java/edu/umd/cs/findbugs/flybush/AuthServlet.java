package edu.umd.cs.findbugs.flybush;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class AuthServlet extends AbstractFlybushServlet {

	private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

        String uri = req.getRequestURI();
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
	}

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        if (uri.equals("/log-in")) {
            logIn(req, resp, pm);

        } else if (uri.startsWith("/log-out/")) {
            logOut(req, resp, pm);
        }
    }

    private void browserAuth(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();

		if (user != null) {
			long id = Long.parseLong(req.getRequestURI().substring("/browser-auth/".length()));
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
		    resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
		}
	}

	private void checkAuth(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		long id = Long.parseLong(req.getRequestURI().substring("/check-auth/".length()));
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

	private void logOut(HttpServletRequest req, HttpServletResponse resp,
			PersistenceManager pm) throws IOException {
		long id = Long.parseLong(req.getRequestURI().substring("/log-out/".length()));
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
}