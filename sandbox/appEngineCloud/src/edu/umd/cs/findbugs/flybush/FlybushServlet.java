package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.servlet.http.*;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

@SuppressWarnings("serial")
public class FlybushServlet extends HttpServlet {
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

	            PersistenceManager pm = PMF.get().getPersistenceManager();
	            try {
	                pm.makePersistent(session);
	            } finally {
	                pm.close();
	            }

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
			PersistenceManager pm = PMF.get().getPersistenceManager();
		    String query = "select from " + SqlCloudSession.class.getName() + " where randomID == " + id + " order by date desc range 0,1";
		    List<SqlCloudSession> sessions = (List<SqlCloudSession>) pm.newQuery(query).execute();
            PrintWriter writer = resp.getWriter();
		    if (sessions.isEmpty()) {
	            resp.setContentType("text/plain");
				writer.println("FAIL");
		    } else {
		    	writer.println("OK\n" + sessions.get(0).getRandomID() + "\n"
		    			+ sessions.get(0).getAuthor().getEmail());
		    }
		} else {
			resp.setStatus(404);
			resp.setContentType("text/plain");
			resp.getWriter().println("Not Found");
		}

	}
}
