package edu.umd.cs.findbugs.flybush;

import junit.framework.TestCase;

import static org.mockito.Mockito.*;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.api.users.User;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.HashList;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.IssueList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FlybushServletTest extends TestCase {

	private PersistenceManagerFactory pmf;
	private FlybushServlet servlet;
	private HttpServletRequest mockRequest;
	private HttpServletResponse mockResponse;
	private ByteArrayOutputStream outputCollector;
	private TestEnvironment testEnvironment;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		testEnvironment = new TestEnvironment();
		ApiProxy.setEnvironmentForCurrentThread(testEnvironment);
		ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")){});
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        initPersistenceManagerFactory();

		servlet = new FlybushServlet(pmf);
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

    @Override
    public void tearDown() throws Exception {
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService =
            (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
        super.tearDown();
    }

    public void testBrowserAuthLoginRedirect() throws IOException {
    	executeServlet("/browser-auth/100");
    	verify(mockResponse).sendRedirect(anyString());
    }
    
    public void testBrowserAuthWhenLoggedIn() throws IOException {
    	testEnvironment.setEmail("my@email.com");
    	executeServlet("/browser-auth/100");
    	verify(mockResponse).setStatus(200);
    	verify(mockResponse).setContentType("text/html");
    	String outputString = new String(outputCollector.toByteArray(), "UTF-8");
		assertTrue("Should contain 'now signed in': " + outputString, 
				   outputString.contains("now signed in"));
    }

	public void testCheckAuthForValidId() throws IOException {
		SqlCloudSession session = new SqlCloudSession(new User("my", "email.com"), 100, new Date(200));
		pmf.getPersistenceManager().makePersistent(session);
		
		executeServlet("/check-auth/100");

		expectOutput(200, "OK\n100\nmy\n");
	}

	public void testCheckAuthForNonexistentId() throws IOException {
		executeServlet("/check-auth/100");

		expectOutput(418, "FAIL\n");
	}
	
	public void testFindIssuesNoneFound() throws IOException {
		executeServlet("/find-issues", HashList.newBuilder().addHashes("ABC").build().toByteArray());
		IssueList result = IssueList.parseFrom(outputCollector.toByteArray());
		assertEquals(0, result.getFoundIssuesCount());
		assertEquals(1, result.getMissingIssuesCount());
		assertEquals("ABC", result.getMissingIssues(0));
	}
	
	public void testFindIssuesSomeFound() throws IOException {
		DbIssue foundIssue = new DbIssue();
		foundIssue.setHash("DEF");
		foundIssue.setBugPattern("MY_BUG");
		foundIssue.setPriority(2);
		foundIssue.setRank(8);
		foundIssue.setPrimaryClass("my.class");
		foundIssue.setFirstSeen(100);
		foundIssue.setLastSeen(200);
		foundIssue.setTimestamp(600);
		pmf.getPersistenceManager().makePersistent(foundIssue);
		
		HashList hashesToFind = HashList.newBuilder().addHashes("ABC").addHashes("DEF").build();
		executeServlet("/find-issues", hashesToFind.toByteArray());
		IssueList result = IssueList.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getFoundIssuesCount());
		
		Issue foundIssueResult = result.getFoundIssues(0);
		assertEquals(foundIssue.getHash(), foundIssueResult.getHash());
		assertEquals(foundIssue.getBugPattern(), foundIssueResult.getBugPattern());
		assertEquals(foundIssue.getPriority(), foundIssueResult.getPriority());
		assertEquals(foundIssue.getRank(), foundIssueResult.getRank());
		assertEquals(foundIssue.getPrimaryClass(), foundIssueResult.getPrimaryClass());
		assertEquals(foundIssue.getFirstSeen(), foundIssueResult.getFirstSeen());
		assertEquals(foundIssue.getLastSeen(), foundIssueResult.getLastSeen());
		
		assertEquals(1, result.getMissingIssuesCount());
		assertEquals("ABC", result.getMissingIssues(0));
	}

	// ========================= end of tests ================================

    /**
     * Creates a PersistenceManagerFactory on the fly, with the exact same information
     * stored in the /WEB-INF/META-INF/jdoconfig.xml file.
     */
    private void initPersistenceManagerFactory() {
        Properties newProperties = new Properties();
        newProperties.put("javax.jdo.PersistenceManagerFactoryClass",
                "org.datanucleus.store.appengine.jdo.DatastoreJDOPersistenceManagerFactory");
        newProperties.put("javax.jdo.option.ConnectionURL", "appengine");
        newProperties.put("javax.jdo.option.NontransactionalRead", "true");
        newProperties.put("javax.jdo.option.NontransactionalWrite", "true");
        newProperties.put("javax.jdo.option.RetainValues", "true");
        newProperties.put("datanucleus.appengine.autoCreateDatastoreTxns", "true");
        pmf = JDOHelper.getPersistenceManagerFactory(newProperties);
    }
    

	private void executeServlet(String requestUri) throws IOException {
		executeServlet(requestUri, null);
	}
	
	private void executeServlet(String requestUri, byte[] input) throws IOException {
		when(mockRequest.getRequestURI()).thenReturn(requestUri);
		if (input != null) {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(input); 
			when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
				public int read() throws IOException {
					// TODO Auto-generated method stub
					return inputStream.read();
				}
			});
		}

		servlet.doGet(mockRequest, mockResponse);
	}
	
	private void expectOutput(int responseCode, String expectedOutput) 
		throws UnsupportedEncodingException {
		verify(mockResponse).setContentType("text/plain");
		verify(mockResponse).setStatus(responseCode);
		String output = new String(outputCollector.toByteArray(), "UTF-8");
		assertEquals(expectedOutput, output.replaceAll("\r", ""));
	}

}

class TestEnvironment implements ApiProxy.Environment {
	private String email;

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAppId() {
		return "test";
	}

	public String getVersionId() {
		return "1.0";
	}

	public String getEmail() {
		return email;
	}

	public boolean isLoggedIn() {
		return email != null;
	}

	public boolean isAdmin() {
		throw new UnsupportedOperationException();
	}

	public String getAuthDomain() {
		return email == null ? null : "domain.com";
	}

	public String getRequestNamespace() {
		return "";
	}

	public Map<String, Object> getAttributes() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("com.google.appengine.server_url_key", "http://localhost:8080");
		return map;
	}
}