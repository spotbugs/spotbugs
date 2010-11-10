package edu.umd.cs.findbugs.flybush;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.ext.AxSchemaExtension;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil.decodeHash;

public abstract class AbstractFlybushServletTest extends TestCase {

    /** Wed, 31 Mar 2010 18:44:40 GMT */
    protected static final long SAMPLE_TIMESTAMP = 1270061080000L;

    protected HttpServletResponse mockResponse;

    protected ByteArrayOutputStream outputCollector;

    protected AbstractFlybushServlet servlet;

    protected AuthServlet authServlet;

    protected HttpServletRequest mockRequest;

    protected PersistenceHelper persistenceHelper;

    protected FlybushServletTestHelper testHelper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testHelper.setUp();
        persistenceHelper = testHelper.createPersistenceHelper(testHelper.getPersistenceManager());
        initServletAndMocks();
    }

    @Override
    protected void tearDown() throws Exception {
        testHelper.tearDown();
    }

    public void setFlybushServletTestHelper(FlybushServletTestHelper flybushServletTestHelper) {
        this.testHelper = flybushServletTestHelper;
    }

    protected abstract AbstractFlybushServlet createServlet();

    // ========================= supporting methods
    // ================================

    protected void initServletAndMocks() throws IOException, ServletException {
        authServlet = new AuthServlet();
        authServlet.setPersistenceHelper(persistenceHelper);

        servlet = createServlet();
        servlet.setPersistenceHelper(persistenceHelper);
        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        outputCollector = new ByteArrayOutputStream();
        ServletOutputStream servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                outputCollector.write(b);
            }
        };
        Mockito.when(mockResponse.getOutputStream()).thenReturn(servletOutputStream);
        Mockito.when(mockResponse.getWriter()).thenReturn(new PrintWriter(servletOutputStream, true));
    }

    protected void initOpenidUserParameter() {
        OpenIdUser user = new OpenIdUser();
        HashMap<String, String> jsonAttr = new HashMap<String, String>();
        jsonAttr.put("b", "http://some.website");
        user.fromJSON(jsonAttr);
        Map<String, String> axattr = new HashMap<String, String>();
        user.setAttribute(AxSchemaExtension.ATTR_NAME, axattr);
        axattr.put("email", "my@email.com");
        Mockito.when(mockRequest.getAttribute(OpenIdUser.ATTR_NAME)).thenReturn(user);
    }

    protected void createCloudSession(long sessionId) throws IOException, ServletException {
        initOpenidUserParameter();
        executeGet(authServlet, "/browser-auth/" + sessionId);
        initServletAndMocks();
    }

    protected void executeGet(String requestUri) throws IOException, ServletException {
        executeGet(servlet, requestUri);
    }

    protected void executeGet(AbstractFlybushServlet servlet, String requestUri) throws IOException, ServletException {
        prepareRequestAndResponse(requestUri, null);

        servlet.doGet(mockRequest, mockResponse);
    }

    protected void executePost(String requestUri, byte[] input) throws IOException {
        executePost(servlet, requestUri, input);
    }

    protected void executePost(AbstractFlybushServlet servlet, String requestUri, byte[] input) throws IOException {
        prepareRequestAndResponse(requestUri, input);

        servlet.doPost(mockRequest, mockResponse);
    }

    protected void checkResponse(int responseCode, String expectedOutput) throws UnsupportedEncodingException {
        checkResponse(responseCode);
        Mockito.verify(mockResponse, Mockito.atLeastOnce()).setContentType("text/plain");
        String output = new String(outputCollector.toByteArray(), "UTF-8");
        Assert.assertEquals(expectedOutput.trim(), output.replaceAll("\r", "").trim());
    }

    protected void checkResponse(int responseCode) {
        Mockito.verify(mockResponse).setStatus(responseCode);
    }

    // ================================ end of helper methods
    // ===============================

    protected void prepareRequestAndResponse(String requestUri, byte[] input) throws IOException {
        int qpos = requestUri.indexOf('?');
        final Map<String,String> params = Maps.newHashMap();
        String actualUri;
        if (qpos == -1) {
            actualUri = requestUri;
        } else {
            actualUri = requestUri.substring(0, qpos);
            String paramstr = requestUri.substring(qpos+1);
            parseParams(params, paramstr);
        }
        Mockito.when(mockRequest.getRequestURI()).thenReturn(actualUri);
        Mockito.when(mockRequest.getParameter(Matchers.<String>any())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock inv) throws Throwable {
                String key = (String) inv.getArguments()[0];
                return params.get(key);
            }
        });
        if (input != null) {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(input);
            Mockito.when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }
            });
        }
    }

    private void parseParams(Map<String, String> params, String paramstr) {
        for (String pair : paramstr.split("&")) {
            int epos = pair.indexOf("=");
            if (epos == -1) {
                params.put(pair, "");
            } else {
                params.put(pair.substring(0, epos), pair.substring(epos + 1));
            }
        }
    }

    protected DbEvaluation createEvaluation(DbIssue issue, String who, long when) {
        return createEvaluation(issue, who, when, "MUST_FIX", "my comment");
    }

    @SuppressWarnings({ "unchecked" })
    protected DbEvaluation createEvaluation(DbIssue issue, String who, long when, String designation, String comment) {
        DbUser user;
        Query query = getPersistenceManager().newQuery(
                "select from " + persistenceHelper.getDbUserClassname() + " where openid == :myopenid");
        List<DbUser> results = (List<DbUser>) query.execute("http://" + who);
        if (results.isEmpty()) {
            user = persistenceHelper.createDbUser("http://" + who, who);
            getPersistenceManager().makePersistent(user);

        } else {
            user = results.iterator().next();
        }
        query.closeAll();
        DbEvaluation eval = persistenceHelper.createDbEvaluation();
        eval.setComment(comment);
        eval.setDesignation(designation);
        eval.setIssue(issue);
        eval.setWhen(when);
        eval.setWho(user.createKeyObject());
        eval.setPrimaryClass(issue.getPrimaryClass());
        eval.setPackages(UpdateServlet.buildPackageList(issue.getPrimaryClass()));
        eval.setEmail(who);
        issue.addEvaluation(eval);
        return eval;
    }

    protected PersistenceManager getPersistenceManager() {
        return testHelper.getPersistenceManager();
    }

    protected DbUser getDbUser(Object user) {
        Assert.assertNotNull(user);
        return persistenceHelper.getObjectById(getPersistenceManager(), persistenceHelper.getDbUserClass(), user);
    }

    protected DbIssue createDbIssue(String patternAndHash) {
        patternAndHash = WebCloudProtoUtil.normalizeHash(patternAndHash);
        DbIssue foundIssue = persistenceHelper.createDbIssue();
        foundIssue.setHash(patternAndHash);
        foundIssue.setBugPattern(patternAndHash);
        foundIssue.setPriority(2);
        foundIssue.setPrimaryClass("my.class");
        foundIssue.setFirstSeen(SAMPLE_TIMESTAMP + 100);
        foundIssue.setLastSeen(SAMPLE_TIMESTAMP + 200);
        foundIssue.setBugLinkType("JIRA");
        foundIssue.setBugLink("http://bug.link");
        return foundIssue;
    }

    protected static void checkIssuesEqualExceptTimestamps(DbIssue dbIssue, Issue protoIssue) {
        assertEquals(dbIssue.getHash(), decodeHash(protoIssue.getHash()));
        assertEquals(dbIssue.getBugPattern(), protoIssue.getBugPattern());
        assertEquals(dbIssue.getPriority(), protoIssue.getPriority());
        assertEquals(dbIssue.getPrimaryClass(), protoIssue.getPrimaryClass());
        assertEquals(dbIssue.getBugLink(), protoIssue.hasBugLink() ? protoIssue.getBugLink() : null);
    }
}
