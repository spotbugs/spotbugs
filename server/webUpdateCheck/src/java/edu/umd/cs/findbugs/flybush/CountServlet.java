package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CountServlet extends AbstractFlybushUpdateServlet {

    protected static final Logger LOGGER = Logger.getLogger(CountServlet.class
            .getName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setStatus(200);
        resp.setContentType("text/plain");
        count(persistenceHelper.getDbUsageEntryClass(), resp);
        count(persistenceHelper.getDbPluginUpdateXmlClass(), resp);
        count(persistenceHelper.getDbUsageSummaryClass(), resp);
    }

    private void count(Class<?> c, HttpServletResponse resp) throws IOException {
        String kind = c.getSimpleName();
        int count = persistenceHelper.count(kind);

        resp.getWriter().println("count of " + kind + " = " + count);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void handlePost(PersistenceManager pm, HttpServletRequest req,
            HttpServletResponse resp, String uri) throws IOException {
        throw new RuntimeException();

    }

}
