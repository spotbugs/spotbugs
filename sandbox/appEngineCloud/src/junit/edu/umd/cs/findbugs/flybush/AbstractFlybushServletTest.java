package edu.umd.cs.findbugs.flybush;

import com.dyuproject.openid.OpenIdUser;
import com.dyuproject.openid.ext.AxSchemaExtension;
import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import junit.framework.TestCase;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.*;

public abstract class AbstractFlybushServletTest extends TestCase {

	protected HttpServletResponse mockResponse;
	protected ByteArrayOutputStream outputCollector;
	protected TestEnvironment testEnvironment;
	protected PersistenceManager persistenceManager;

    private AbstractFlybushServlet servlet;
    protected AuthServlet authServlet;
	protected HttpServletRequest mockRequest;
	private PersistenceManager actualPersistenceManager;
    protected PersistenceHelper persistenceHelper;

    @Override
	protected void setUp() throws Exception {
		super.setUp();
		testEnvironment = new TestEnvironment();
		ApiProxy.setEnvironmentForCurrentThread(testEnvironment);
		ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")){});
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        initPersistenceManager();

		initServletAndMocks();
	}

    @Override
    protected void tearDown() throws Exception {
    	actualPersistenceManager.close();
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService =
            (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
        super.tearDown();
    }

    protected abstract AbstractFlybushServlet createServlet();

	// ========================= supporting methods ================================

	protected void initServletAndMocks() throws IOException, ServletException {
        authServlet = new AuthServlet();
        authServlet.setPersistenceHelper(persistenceHelper);

		servlet = createServlet();
        servlet.setPersistenceHelper(persistenceHelper);
		mockRequest = mock(HttpServletRequest.class);
		mockResponse = mock(HttpServletResponse.class);
		outputCollector = new ByteArrayOutputStream();
		ServletOutputStream servletOutputStream = new ServletOutputStream() {
			public void write(int b) throws IOException {
				outputCollector.write(b);
			}
		};
		when(mockResponse.getOutputStream()).thenReturn(servletOutputStream);
		when(mockResponse.getWriter()).thenReturn(new PrintWriter(servletOutputStream, true));
	}

    protected void initOpenidUserParameter() {
        OpenIdUser user = new OpenIdUser();
        HashMap<String, String> jsonAttr = new HashMap<String, String>();
        jsonAttr.put("b", "http://some.website");
        user.fromJSON(jsonAttr);
        Map<String, String> axattr = new HashMap<String, String>();
        user.setAttribute(AxSchemaExtension.ATTR_NAME, axattr);
        axattr.put("email", "my@email.com");
        when(mockRequest.getAttribute(OpenIdUser.ATTR_NAME)).thenReturn(user);
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

	protected void checkResponse(int responseCode, String expectedOutput)
			throws UnsupportedEncodingException {
		checkResponse(responseCode);
		verify(mockResponse, atLeastOnce()).setContentType("text/plain");
		String output = new String(outputCollector.toByteArray(), "UTF-8");
		assertEquals(expectedOutput.trim(), output.replaceAll("\r", "").trim());
	}

	protected void checkResponse(int responseCode) {
		verify(mockResponse).setStatus(responseCode);
	}

    // ================================ end of helper methods ===============================

	/**
     * Creates a PersistenceManagerFactory on the fly, with the exact same information
     * stored in the /WEB-INF/META-INF/jdoconfig.xml file.
     */
    protected void initPersistenceManager() {
        Properties newProperties = new Properties();
        newProperties.put("javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory");
        newProperties.put("javax.jdo.option.ConnectionURL", "appengine");
        newProperties.put("javax.jdo.option.NontransactionalRead", "true");
        newProperties.put("javax.jdo.option.NontransactionalWrite", "true");
        newProperties.put("javax.jdo.option.RetainValues", "true");
        newProperties.put("datanucleus.appengine.autoCreateDatastoreTxns", "true");
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(newProperties);
		actualPersistenceManager = pmf.getPersistenceManager();
		persistenceManager = spy(actualPersistenceManager);
        persistenceHelper = new AppEnginePersistenceHelper() {
            public PersistenceManager getPersistenceManager() {
                return persistenceManager;
            }
        };

		doNothing().when(persistenceManager).close();
    }

	private void prepareRequestAndResponse(String requestUri, byte[] input)
			throws IOException {
		when(mockRequest.getRequestURI()).thenReturn(requestUri);
		if (input != null) {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(input);
			when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
				public int read() throws IOException {
					return inputStream.read();
				}
			});
		}
	}

    protected AppEngineDbEvaluation createEvaluation(DbIssue issue, String who, int when) {
        AppEngineDbUser user = new AppEngineDbUser("http://" + who, who);
        persistenceManager.makePersistent(user);
        AppEngineDbEvaluation eval = new AppEngineDbEvaluation();
        eval.setComment("my comment");
        eval.setDesignation("MUST_FIX");
        eval.setIssue(issue);
        eval.setWhen(when);
        eval.setWho(user.createKeyObject());
        return eval;
    }

    protected DbUser getDbUser(Object user) {
        assertNotNull(user);
        return persistenceHelper.getObjectById(persistenceManager, AppEngineDbUser.class, user);
    }
}

