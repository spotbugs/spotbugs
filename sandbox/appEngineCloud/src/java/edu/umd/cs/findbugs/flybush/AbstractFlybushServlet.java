package edu.umd.cs.findbugs.flybush;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractFlybushServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AbstractFlybushServlet.class.getName());

    private PersistenceManager persistenceManager;

    /** for testing */
    void setPersistenceManager(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        long start = System.currentTimeMillis();
        PersistenceManager pm = getPersistenceManager();
        LOGGER.warning("loading PM took " + (System.currentTimeMillis() - start) + "ms");

        try {
            handlePost(pm, req, resp, uri);
        } finally {
            pm.close();
        }
    }

    protected abstract void handlePost(PersistenceManager pm,
                                       HttpServletRequest req, HttpServletResponse resp,
                                       String uri) throws IOException;

    protected void show404(HttpServletResponse resp) throws IOException {
        setResponse(resp, 404, "Not Found");
    }

    protected void setResponse(HttpServletResponse resp, int statusCode, String textResponse)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("text/plain");
        resp.getWriter().println(textResponse);
    }

    protected boolean isAuthenticated(HttpServletResponse resp, PersistenceManager pm, long sessionId) throws IOException {
        SqlCloudSession session = lookupCloudSessionById(sessionId, pm);
        if (session == null) {
            setResponse(resp, 403, "not authenticated");
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    protected SqlCloudSession lookupCloudSessionById(long id, PersistenceManager pm) {
        Query query = pm.newQuery(
                "select from " + SqlCloudSession.class.getName() +
                " where randomID == :randomIDquery");
        List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute(Long.toString(id));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    protected PersistenceManager getPersistenceManager() {
        if (persistenceManager != null) {
            return persistenceManager;
        }

        return PMF.get().getPersistenceManager();
    }
}
