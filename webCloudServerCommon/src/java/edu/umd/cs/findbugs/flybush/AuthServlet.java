package edu.umd.cs.findbugs.flybush;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.RelyingParty;
import com.dyuproject.openid.ext.AxSchemaExtension;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;

@SuppressWarnings("serial")
public class AuthServlet extends AbstractFlybushServlet {

    static {
        // run on startup to configure DyuProject to request e-mail addresses
        // from OpenID providers
        RelyingParty.getInstance().addListener(new AxSchemaExtension().addExchange("email"));
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String uri = req.getRequestURI();
        PersistenceManager pm = getPersistenceManager();
        try {
            if (uri.startsWith("/browser-auth/")) {
                browserAuth(req, resp, pm);

            } else if (uri.startsWith("/check-auth/")) {
                checkAuth(req, resp, pm);

            } else if (uri.startsWith("/token")) {
                showToken(req, resp, pm);

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

    @SuppressWarnings({"unchecked"})
    private void browserAuth(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {
        OpenIdUser openIdUser = (OpenIdUser) req.getAttribute(OpenIdUser.ATTR_NAME);

        if (openIdUser == null) {
            setResponse(resp, 403, "OpenID authorization required");
            return;
        }
        String email = getEmail(openIdUser);

        String openidUrl = getOpenidUrl(resp, openIdUser, email);
        if (openidUrl == null)
            return;

        long id = Long.parseLong(req.getRequestURI().substring("/browser-auth/".length()));
        Date date = new Date();
        DbUser dbUser = findUser(pm, openidUrl, email);

        if (dbUser == null) {
            dbUser = createAndStoreUser(pm, openidUrl, email);
        }
        SqlCloudSession session = persistenceHelper.createSqlCloudSession(id, date, dbUser.createKeyObject(), email);
        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.makePersistent(session);
            tx.commit();
        } finally {
            if (tx.isActive())
                tx.rollback();
        }
        resp.setStatus(200);
        resp.setContentType("text/html");
        PrintWriter writer = resp.getWriter();
        writer.println("<title>" + getCloudName() + "</title>");
        writer.println("<h1>You are now signed in</h1>");
        writer.println("<p style='font-size: large; font-weight: bold'>"
                + "Please return to the FindBugs application window to continue.</p>");
        writer.println("<p style='font-style: italic'>Signed in as <strong>" + email + "</strong> (" + openidUrl + ")</p>");
    }

    private DbUser createAndStoreUser(PersistenceManager pm, String openidUrl, String email) {
        DbUser dbUser = persistenceHelper.createDbUser(openidUrl, email);

        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.makePersistent(dbUser);
            tx.commit();
        } finally {
            if (tx.isActive())
                tx.rollback();
        }
        return dbUser;
    }

    @SuppressWarnings({"unchecked"})
    private DbUser findUser(PersistenceManager pm, String openidUrl, String email) {
        Query q = pm.newQuery("select from " + persistenceHelper.getDbUserClass().getName()
                + " where openid == :openid && email == :email");
        List<DbUser> result = (List<DbUser>) q.execute(openidUrl, email);
        DbUser dbUser = result.isEmpty() ? null : result.iterator().next();
        q.closeAll();
        return dbUser;
    }

    private String getEmail(OpenIdUser openIdUser) {
        Map<String, String> axschema = AxSchemaExtension.get(openIdUser);
        return axschema == null ? null : axschema.get("email");
    }

    private String getOpenidUrl(HttpServletResponse resp, OpenIdUser openIdUser, String email) throws IOException {
        String openidUrl = openIdUser.getIdentity();
        if (openidUrl == null || email == null || !email.matches(".*@([^.]+\\.)+[^.]{2,}")) {
            setResponse(resp, 403, "Your OpenID provider for " + openidUrl + " did not provide an e-mail "
                    + "address. You need an e-mail address to use this service.");
            return null;
        }
        return openidUrl;
    }

    private void checkAuth(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        long id = Long.parseLong(req.getRequestURI().substring("/check-auth/".length()));
        SqlCloudSession sqlCloudSession = lookupCloudSessionById(id, pm);
        if (sqlCloudSession == null) {
            setResponse(resp, 418, "FAIL");
        } else {
            DbUser user = persistenceHelper.getObjectById(pm, persistenceHelper.getDbUserClass(), sqlCloudSession.getUser());
            setResponse(resp, 200, "OK\n" + sqlCloudSession.getRandomID() + "\n" + user.getEmail());
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

        DbInvocation invocation = persistenceHelper.createDbInvocation();
        invocation.setWho(session.getUser());
        invocation.setStartTime(loginMsg.getAnalysisTimestamp());

        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            invocation = pm.makePersistent(invocation);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        session.setInvocation(invocation);
        tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.makePersistent(session);
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        resp.setStatus(200);
    }
    
    private void showToken(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm)
            throws IOException {

        OpenIdUser openIdUser = (OpenIdUser) req.getAttribute(OpenIdUser.ATTR_NAME);

        if (openIdUser == null) {
            setResponse(resp, 403, "OpenID authorization required");
            return;
        }
        String email = getEmail(openIdUser);

        String openidUrl = getOpenidUrl(resp, openIdUser, email);
        if (openidUrl == null)
            return;

        DbUser user = findUser(pm, openidUrl, email);
        if (user == null) {
            user = createAndStoreUser(pm, openidUrl, email);
        }

        if (req.getParameter("generate") != null) {
            user.setUploadToken(BigInteger.valueOf(Math.abs(new SecureRandom().nextLong())).toString(16));
            Transaction tx = pm.currentTransaction();
            try {
                pm.makePersistent(user);
            } finally {
                if (tx.isActive())
                    tx.rollback();
            }
        }

        ServletOutputStream out = resp.getOutputStream();
        out.println("<title>" + getCloudName() + " - Token</title>");
        out.println("<body>");
        if (user.getUploadToken() != null) {
            out.println("Your token is: " + user.getUploadToken());
            out.println("<form action=/token>" +
                    "<input type=submit name=generate value='Regenerate Token'>" +
                    "</form>");
        } else {
            out.println("You have not yet created a command-line upload token. " +
                    "<form action=/token>" +
                    "<input type=submit name=generate value='Create Token'>" +
                    "</form>");
        }
        out.println("</body>");
    }

    @SuppressWarnings({ "unchecked" })
    private void logOut(HttpServletRequest req, HttpServletResponse resp, PersistenceManager pm) throws IOException {
        long id = Long.parseLong(req.getRequestURI().substring("/log-out/".length()));
        SqlCloudSession session = lookupCloudSessionById(id, pm);
        long deleted = 0;
        Transaction tx = pm.currentTransaction();
        tx.begin();
        try {
            pm.deletePersistent(session);
            deleted++;
            tx.commit();
        } finally {
            if (tx.isActive())
                tx.rollback();
        }
        if (deleted >= 1) {
            resp.setStatus(200);
        } else {
            setResponse(resp, 404, "no such session");
        }
    }
}
