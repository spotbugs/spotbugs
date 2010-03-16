package edu.umd.cs.findbugs.flybush;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public abstract class AbstractFlybushServlet extends HttpServlet {
    protected static final Logger LOGGER = Logger.getLogger(AbstractFlybushServlet.class.getName());

    protected PersistenceHelper persistenceHelper;

    /** for testing */
    void setPersistenceHelper(PersistenceHelper persistenceHelper) {
        this.persistenceHelper = persistenceHelper;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String helperCls = config.getServletContext().getInitParameter("edu.umd.cs.findbugs.flybush.persistenceHelper");
        try {
            persistenceHelper = (PersistenceHelper) Class.forName(helperCls).newInstance();
        } catch (Exception e) {
            throw new ServletException("Couldn't load persistence helper " + helperCls, e);
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String uri = req.getRequestURI();

        PersistenceManager pm = getPersistenceManager();

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

    protected boolean isAuthenticated(HttpServletResponse resp, PersistenceManager pm, long sessionId)
            throws IOException {
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
                "select from " + persistenceHelper.getSqlCloudSessionClass().getName() +
                " where randomID == :randomIDquery");
        List<SqlCloudSession> sessions = (List<SqlCloudSession>) query.execute(Long.toString(id));
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    protected PersistenceManager getPersistenceManager() throws IOException {
        return persistenceHelper.getPersistenceManager();
    }

}
