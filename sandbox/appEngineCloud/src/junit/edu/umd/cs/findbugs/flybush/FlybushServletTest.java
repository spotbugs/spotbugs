package edu.umd.cs.findbugs.flybush;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.api.users.User;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn.Builder;

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

		initServletAndMocks();
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
    	executeGet("/browser-auth/100");
    	verify(mockResponse).sendRedirect(anyString());
    }

    public void testBrowserAuthWhenLoggedIn() throws IOException {
    	testEnvironment.setEmail("my@email.com");
    	executeGet("/browser-auth/100");
    	verify(mockResponse).setStatus(200);
    	verify(mockResponse).setContentType("text/html");
    	String outputString = new String(outputCollector.toByteArray(), "UTF-8");
		assertTrue("Should contain 'now signed in': " + outputString,
				   outputString.contains("now signed in"));
    }

	public void testCheckAuthForValidId() throws IOException {
		SqlCloudSession session = new SqlCloudSession(new User("my", "email.com"), 100, new Date(200));
		pmf.getPersistenceManager().makePersistent(session);

		executeGet("/check-auth/100");

		checkResponse(200, "OK\n100\nmy\n");
	}

	public void testCheckAuthForNonexistentId() throws IOException {
		executeGet("/check-auth/100");

		checkResponse(418, "FAIL\n");
	}

	public void testFindIssuesUnauthenticated() throws IOException {
		executePost("/find-issues", createAuthenticatedLogInMsg()
				.addMyIssueHashes("ABC").build().toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testFindIssuesNoneFound() throws IOException {
    	createCloudSession(555);
		executePost("/find-issues", createAuthenticatedLogInMsg().addMyIssueHashes("ABC").build().toByteArray());
		checkResponseCode(200);
		LogInResponse result = LogInResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(0, result.getFoundIssuesCount());
	}

	public void testFindIssuesSomeFound() throws IOException {
    	createCloudSession(555);

		DbIssue foundIssue = createDbIssue("OLD_BUG");
		pmf.getPersistenceManager().makePersistent(foundIssue);

		LogIn loginMsg = createAuthenticatedLogInMsg().addMyIssueHashes("NEW_BUG").addMyIssueHashes("OLD_BUG").build();
		executePost("/find-issues", loginMsg.toByteArray());
		LogInResponse result = LogInResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getFoundIssuesCount());

		checkIssuesEqual(foundIssue, result.getFoundIssues(0));
	}

	public void testFindIssuesWithEvaluations() throws IOException {
    	createCloudSession(555);

		DbIssue foundIssue = createDbIssue("OLD_BUG");
		DbEvaluation eval = createEvaluation(foundIssue);
		foundIssue.addEvaluation(eval);

		// apparently the evaluation is automatically persisted. throws
		// exception when attempting to persist the eval with the issue.
		pmf.getPersistenceManager().makePersistent(foundIssue);

		LogIn hashesToFind = createAuthenticatedLogInMsg().addMyIssueHashes("NEW_BUG").addMyIssueHashes("OLD_BUG").build();
		executePost("/find-issues", hashesToFind.toByteArray());
		LogInResponse result = LogInResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getFoundIssuesCount());

		// check issues
		Issue foundissueProto = result.getFoundIssues(0);
		checkIssuesEqual(foundIssue, foundissueProto);

		// check evaluations
		assertEquals(1, foundissueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval, foundissueProto.getEvaluations(0));
	}

	public void testFindLotsOfIssues() throws IOException {
		createCloudSession(555);
		List<String> queries = installQueryCollector(DbIssue.class.getName());

		LogIn.Builder loginMsg = createAuthenticatedLogInMsg();
		for (int i = 0; i < 15; i++) {
			loginMsg.addMyIssueHashes(Integer.toString(i));
		}
		executePost("/find-issues", loginMsg.build().toByteArray());
		LogInResponse result = LogInResponse.parseFrom(outputCollector.toByteArray());
		assertEquals(3, result.getFoundIssuesCount());

		assertEquals(2, queries.size());
		assertTrue(queries.get(0).contains("\"1\""));
		assertTrue(queries.get(0).contains("\"2\""));
		assertTrue(queries.get(0).contains("\"9\""));
		assertFalse(queries.get(0).contains("\"10\""));
		assertFalse(queries.get(0).contains("\"15\""));

		assertTrue(queries.get(1).contains("\"10\""));
		assertTrue(queries.get(1).contains("\"11\""));
		assertTrue(queries.get(1).contains("\"14\""));
		assertFalse(queries.get(1).contains("\"1\""));
		assertFalse(queries.get(1).contains("\"19\""));
	}

	public void testGetRecentEvaluations() throws IOException {
		DbIssue issue = createDbIssue("OLD_BUG");
		DbEvaluation eval1 = createEvaluation(issue, 100);
		DbEvaluation eval2 = createEvaluation(issue, 200);
		DbEvaluation eval3 = createEvaluation(issue, 300);
		issue.addEvaluations(eval1, eval2, eval3);

		pmf.getPersistenceManager().makePersistent(issue);

		executeGet("/get-evaluations/100");
		checkResponseCode(200);
		RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(1, result.getIssuesCount());

		// check issues
		Issue foundissueProto = result.getIssues(0);
		checkIssuesEqual(issue, foundissueProto);

		// check evaluations
		assertEquals(2, foundissueProto.getEvaluationsCount());
		checkEvaluationsEqual(eval2, foundissueProto.getEvaluations(0));
		checkEvaluationsEqual(eval3, foundissueProto.getEvaluations(1));
	}

	public void testGetRecentEvaluationsNoneFound() throws IOException {
		DbIssue issue = createDbIssue("OLD_BUG");
		DbEvaluation eval1 = createEvaluation(issue, 100);
		DbEvaluation eval2 = createEvaluation(issue, 200);
		DbEvaluation eval3 = createEvaluation(issue, 300);
		issue.addEvaluations(eval1, eval2, eval3);

		pmf.getPersistenceManager().makePersistent(issue);

		executeGet("/get-evaluations/300");
		checkResponseCode(200);
		RecentEvaluations result = RecentEvaluations.parseFrom(outputCollector.toByteArray());
		assertEquals(0, result.getIssuesCount());
	}

	public void testUploadIssueWithoutAuthenticating() throws IOException {
		Issue issue = createProtoIssue();
		UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponseCode(403);
	}

	public void testUploadIssue() throws IOException {
    	createCloudSession(555);
		Issue issue = createProtoIssue();
		UploadIssues issuesToUpload = UploadIssues.newBuilder().setSessionId(555).addNewIssues(issue).build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) pmf.getPersistenceManager()
				.newQuery("select from " + DbIssue.class.getName()).execute();
		assertEquals(1, dbIssues.size());

		checkIssuesEqual(dbIssues.get(0), issue);
	}

	public void testUploadIssuesWhichAlreadyExist() throws IOException {
    	createCloudSession(555);
		DbIssue oldDbIssue = createDbIssue("OLD_BUG");
		pmf.getPersistenceManager().makePersistent(oldDbIssue);
		Issue oldIssue = createProtoIssue("OLD_BUG");
		Issue newIssue = createProtoIssue("NEW_BUG");
		UploadIssues issuesToUpload = UploadIssues.newBuilder()
				.setSessionId(555)
				.addNewIssues(oldIssue)
				.addNewIssues(newIssue)
				.build();
		executePost("/upload-issues", issuesToUpload.toByteArray());
		checkResponse(200, "");
		List<DbIssue> dbIssues = (List<DbIssue>) pmf.getPersistenceManager()
				.newQuery("select from " + DbIssue.class.getName() + " order by hash").execute();
		assertEquals(2, dbIssues.size());

		assertEquals("NEW_BUG", dbIssues.get(0).getHash());
		assertEquals("OLD_BUG", dbIssues.get(1).getHash());
	}

	public void testUploadEvaluationNoAuth() throws IOException {
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash("MY_HASH")
				.setEvaluation(createProtoEvaluation())
				.build().toByteArray());
		checkResponse(403, "not authenticated");
	}

	public void testUploadEvaluation() throws IOException {
		createCloudSession(555);

		DbIssue dbIssue = createDbIssue("MY_HASH");
		PersistenceManager persistenceManager = pmf.getPersistenceManager();
		persistenceManager.makePersistent(dbIssue);
		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash("MY_HASH")
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponseCode(200);
		persistenceManager.refresh(dbIssue);
		assertEquals(1, dbIssue.getEvaluations().size());
		Evaluation protoEvalToCompare = Evaluation.newBuilder(protoEval).setWho("my@email.com").build();
		checkEvaluationsEqual(dbIssue.getEvaluations().get(0), protoEvalToCompare);
	}

	public void testUploadEvaluationNonexistentIssue() throws IOException {
		createCloudSession(555);

		Evaluation protoEval = createProtoEvaluation();
		executePost("/upload-evaluation", UploadEvaluation.newBuilder()
				.setSessionId(555)
				.setHash("NONEXISTENT")
				.setEvaluation(protoEval)
				.build().toByteArray());
		checkResponseCode(404);
		
	}

	// ========================= end of tests ================================


	private Evaluation createProtoEvaluation() {
		Evaluation protoEval = Evaluation.newBuilder()
				.setDesignation("MUST_FIX")
				.setComment("my comment")
				.setWhen(100)
				.build();
		return protoEval;
	}

	private Builder createAuthenticatedLogInMsg() {
		return LogIn.newBuilder().setSessionId(555).setAnalysisTimestamp(100);
	}

	/**
	 * Intercepts database queries made during the servlet execution, adding
	 * adds all query strings to the returned list.
	 *
	 * @param regex a regular expression to filter the collected queries
	 * @return a list which will be populated with query strings as the
	 * 		   servlet runs
	 */
	private List<String> installQueryCollector(final String regex) {
		PersistenceManager persistenceManager = pmf.getPersistenceManager();
		persistenceManager.makePersistentAll(createDbIssue("2"), createDbIssue("5"), createDbIssue("12"));
		PersistenceManagerFactory spyPmf = spy(pmf);
		PersistenceManager spyPersistenceManager = spy(persistenceManager);
		when(spyPmf.getPersistenceManager()).thenReturn(spyPersistenceManager);
		final List<String> queries = new ArrayList<String>();
		when(spyPersistenceManager.newQuery(anyString())).thenAnswer(new Answer<Query>() {
			public Query answer(InvocationOnMock invocation) throws Throwable {
				String query = (String) invocation.getArguments()[0];
				if (query.matches(".*" + regex + ".*")) {
					queries.add(query);
				}
				return (Query) invocation.callRealMethod();
			}
		});
		servlet.setPmf(spyPmf);
		return queries;
	}

	private void initServletAndMocks() throws IOException {
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

	private void createCloudSession(long sessionId) throws IOException {
		testEnvironment.setEmail("my@email.com");
    	executeGet("/browser-auth/" + sessionId);
    	initServletAndMocks();
	}

	private void checkEvaluationsEqual(DbEvaluation dbEval, Evaluation protoEval) {
		assertEquals(dbEval.getComment(), protoEval.getComment());
		assertEquals(dbEval.getDesignation(), protoEval.getDesignation());
		assertEquals(dbEval.getWhen(), protoEval.getWhen());
		assertEquals(dbEval.getWho(), protoEval.getWho());
	}

	private DbEvaluation createEvaluation(DbIssue issue) {
		return createEvaluation(issue, 100);
	}

	private DbEvaluation createEvaluation(DbIssue issue, int when) {
		DbEvaluation eval = new DbEvaluation();
		eval.setComment("my comment");
		eval.setDesignation("MUST_FIX");
		eval.setIssue(issue);
		eval.setWhen(when);
		eval.setWho("someone");
		return eval;
	}

	private DbIssue createDbIssue(String patternAndHash) {
		DbIssue foundIssue = new DbIssue();
		foundIssue.setHash(patternAndHash);
		foundIssue.setBugPattern(patternAndHash);
		foundIssue.setPriority(2);
		foundIssue.setPrimaryClass("my.class");
		foundIssue.setFirstSeen(100);
		foundIssue.setLastSeen(200);
		return foundIssue;
	}

	private Issue createProtoIssue() {
		return createProtoIssue("MY_BUG");
	}

	private Issue createProtoIssue(String hashAndPattern) {
		Issue.Builder issueBuilder = Issue.newBuilder();
		issueBuilder.setHash(hashAndPattern);
		issueBuilder.setBugPattern(hashAndPattern);
		issueBuilder.setPriority(2);
		issueBuilder.setPrimaryClass("my.class");
		issueBuilder.setFirstSeen(100);
		issueBuilder.setLastSeen(200);

		return issueBuilder.build();
	}

	private void checkIssuesEqual(DbIssue dbIssue, Issue protoIssue) {
		assertEquals(dbIssue.getHash(), protoIssue.getHash());
		assertEquals(dbIssue.getBugPattern(), protoIssue.getBugPattern());
		assertEquals(dbIssue.getPriority(), protoIssue.getPriority());
		assertEquals(dbIssue.getPrimaryClass(), protoIssue.getPrimaryClass());
		assertEquals(dbIssue.getFirstSeen(), protoIssue.getFirstSeen());
		assertEquals(dbIssue.getLastSeen(), protoIssue.getLastSeen());
	}

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


	private void executeGet(String requestUri) throws IOException {
		executeGet(requestUri, null);
	}

	private void executeGet(String requestUri, byte[] input) throws IOException {
		prepareRequestAndResponse(requestUri, input);

		servlet.doGet(mockRequest, mockResponse);
	}

	private void executePost(String requestUri, byte[] input)
			throws IOException {
		prepareRequestAndResponse(requestUri, input);

		servlet.doPost(mockRequest, mockResponse);
	}

	private void prepareRequestAndResponse(String requestUri, byte[] input)
			throws IOException {
		when(mockRequest.getPathInfo()).thenReturn(requestUri);
		if (input != null) {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(input);
			when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
				public int read() throws IOException {
					return inputStream.read();
				}
			});
		}
	}

	private void checkResponse(int responseCode, String expectedOutput)
			throws UnsupportedEncodingException {
		checkResponseCode(responseCode);
		verify(mockResponse).setContentType("text/plain");
		String output = new String(outputCollector.toByteArray(), "UTF-8");
		assertEquals(expectedOutput.trim(), output.replaceAll("\r", "").trim());
	}

	private void checkResponseCode(int responseCode) {
		verify(mockResponse).setStatus(responseCode);
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