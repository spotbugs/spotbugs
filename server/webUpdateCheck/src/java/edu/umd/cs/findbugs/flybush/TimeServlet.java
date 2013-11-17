package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Date;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeServlet extends AbstractFlybushUpdateServlet {

   
    
    @Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.setContentType("text/plain");
        resp.getWriter().println("OK @ " + new Date() + " aka " + System.currentTimeMillis());
    }
    @Override protected void handlePost(PersistenceManager pm, HttpServletRequest req, HttpServletResponse resp, String uri)
            throws IOException {
        // TODO Auto-generated method stub
        
    }

}
