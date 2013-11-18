package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeServlet extends HttpServlet {

    
    @Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(200);
        resp.setContentType("text/plain");
        resp.getWriter().println("OK @ " + new Date() + " aka " + System.currentTimeMillis());
    }
  

}
