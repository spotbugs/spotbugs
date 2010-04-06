package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;
import junit.framework.TestCase;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static edu.umd.cs.findbugs.cloud.appEngine.BugFilingHelper.processJiraDashboardUrl;
import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.normalizeHash;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppEngineCloudClientTest extends TestCase {
    private static final long SAMPLE_DATE = 1200000000L * 1000L; // Thu, 10 Jan 2008 21:20:00 GMT

    private BugInstance missingIssue;
    private BugInstance foundIssue;
    private boolean addMissingIssue;

    @Override
	protected void setUp() throws Exception {
		missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
		foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");
        missingIssue.setInstanceHash("fad1");
        foundIssue.setInstanceHash("fad2");
        addMissingIssue = false;
	}

	public static void testEncodeDecodeHash() {
		checkHashEncodeRoundtrip("9e107d9d372bb6826bd81d3542a419d6");
		checkHashEncodeRoundtrip("83ab7e45f39c7a7a84e5e63b95beeb5");
		checkHashEncodeRoundtrip("1fe8e2bc5f1cceae0bf5954e7b5e84ac");
		checkHashEncodeRoundtrip("6977735a4a0f8036778b223cd9f9c1f0");
		checkHashEncodeRoundtrip("9ba282b1a7b049fa3c5b068941c25977");
		checkHashEncodeRoundtrip("6606a054edd331799ed567b4efd539a6");
		checkHashEncodeRoundtrip("6f2130edc682a1cb0db9b709179593d9");
		checkHashEncodeRoundtrip("ffffffffffffffffffffffffffffffff");
		checkHashEncodeRoundtrip("0");
		checkHashEncodeRoundtrip("1");
	}

	public void testNormalizeHash() {
		assertEquals("0", normalizeHash("0"));
		assertEquals("0", normalizeHash("000000000"));
		assertEquals("1", normalizeHash("000000000000001"));
		assertEquals("fffffffffffffffffffffffffffffff", normalizeHash("0fffffffffffffffffffffffffffffff"));
	}

	public void testNormalizeHashMakesLowercase() {
		assertEquals("f", normalizeHash("F"));
		assertEquals("fffffffffffffffffffffffffffffff", normalizeHash("0FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
		assertEquals("ffffffffffffffffffffffffffffffff", normalizeHash("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"));
	}

	public void testLogInAllIssuesFound() throws IOException {
        addMissingIssue = false;
		// set up mocks

		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponseInputStream(createFoundIssueProto()));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

		// execution
		MyAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection);
        when(cloud.mockGuiCallback.showConfirmDialog(anyString(), anyString(), Mockito.anyInt())).thenReturn(0);
        cloud.initialize();
		cloud.bugsPopulated();

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
		verify(findIssuesConnection).connect();
		FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
		assertEquals(1, hashes.getMyIssueHashesCount());
		List<String> hashesFromFindIssues = AppEngineProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
		assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));

		// verify processing of found issues
		assertEquals(SAMPLE_DATE+100, cloud.getFirstSeen(foundIssue));
		assertEquals(SAMPLE_DATE+500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignation);
		assertEquals("latest comment", primaryDesignation.getAnnotationText());
		assertEquals(SAMPLE_DATE+500, primaryDesignation.getTimestamp());
		assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
		assertEquals("test@example.com", primaryDesignation.getUser());
	}

	public void testLogInAndUploadIssues() throws IOException {
        addMissingIssue = true;
		// set up mocks

		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponseInputStream(createFoundIssueProto()));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream logInOutput = setupResponseCodeAndOutputStream(logInConnection);

		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream uploadIssuesBuffer = setupResponseCodeAndOutputStream(uploadConnection);

		// execution
		MyAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection, logInConnection, uploadConnection);
        when(cloud.mockGuiCallback.showConfirmDialog(anyString(), anyString(), Mockito.anyInt())).thenReturn(0);
        cloud.initialize();
		cloud.bugsPopulated();

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
		verify(findIssuesConnection).connect();
		FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
		assertEquals(2, hashes.getMyIssueHashesCount());
		List<String> hashesFromFindIssues = AppEngineProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
		assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));
		assertTrue(hashesFromFindIssues.contains(missingIssue.getInstanceHash()));

		// verify log-in
        assertEquals("/log-in", cloud.urlsRequested.get(1));
		verify(logInConnection).connect();
		LogIn logIn = LogIn.parseFrom(logInOutput.toByteArray());
        assertEquals(cloud.getBugCollection().getAnalysisTimestamp(), logIn.getAnalysisTimestamp());

		// verify processing of found issues
		assertEquals(SAMPLE_DATE+100, cloud.getFirstSeen(foundIssue));
		assertEquals(SAMPLE_DATE+500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignation);
		assertEquals("latest comment", primaryDesignation.getAnnotationText());
		assertEquals(SAMPLE_DATE+500, primaryDesignation.getTimestamp());
		assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
		assertEquals("test@example.com", primaryDesignation.getUser());

		// verify uploaded issues

        assertEquals("/upload-issues", cloud.urlsRequested.get(2));
        UploadIssues uploadedIssues = UploadIssues.parseFrom(uploadIssuesBuffer.toByteArray());
		assertEquals(1, uploadedIssues.getNewIssuesCount());
		checkIssuesEqual(missingIssue, uploadedIssues.getNewIssues(0));
	}

	@SuppressWarnings("deprecation")
	public void testStoreUserAnnotation() throws Exception {
		// set up mocks
        addMissingIssue = true;

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

		final HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream outputCollector = setupResponseCodeAndOutputStream(uploadConnection);

        // execute
		AppEngineCloudClient cloudClient = createAppEngineCloudClient(logInConnection, uploadConnection);
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));
		cloudClient.initialize();
		cloudClient.storeUserAnnotation(foundIssue);

		// verify
		verify(uploadConnection).connect();
		UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(outputCollector.toByteArray());
		checkUploadedEvaluation(uploadMsg);
	}

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluations() throws Exception {
		// set up mocks
        addMissingIssue = false;
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

		Issue issue = createFoundIssueWithOneEvaluation();

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(issue,
                        createEvaluation("MUST_FIX", SAMPLE_DATE+250, "comment", "test@example.com"),
                        createEvaluation("MOSTLY_HARMLESS", SAMPLE_DATE+300, "new comment", "test@example.com")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		MyAppEngineCloudClient cloud = createAppEngineCloudClient(recentEvalConnection);
        cloud.initialize();
		cloud.updateEvaluationsFromServer();

		// verify
		BugDesignation primaryDesignationAfter = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignationAfter);
		assertEquals("new comment", primaryDesignationAfter.getAnnotationText());
		assertEquals("MOSTLY_HARMLESS", primaryDesignationAfter.getDesignationKey());
		assertEquals("test@example.com", primaryDesignationAfter.getUser());
		assertEquals(SAMPLE_DATE+300, primaryDesignationAfter.getTimestamp());
	}

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson()
			throws Exception {
        addMissingIssue = false;
		Issue responseIssue = createFoundIssue(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));


        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponseInputStream(responseIssue));

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(responseIssue,
                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+200, "comment2", "second"),

                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+300, "comment3", "first")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		AppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, recentEvalConnection);
        cloudClient.initialize();
		cloudClient.bugsPopulated();
		cloudClient.updateEvaluationsFromServer();

		// verify
		List<BugDesignation> allUserDesignations = newList(cloudClient.getAllUserDesignations(foundIssue));
		assertEquals(2, allUserDesignations.size());
	}

    public void testJiraDashboardUrlProcessor() {
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("  jira.atlassian.com    "));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("http://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa;sessionId=blah"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa?blah"));
    }

    // =================================== end of tests ===========================================

    private HttpURLConnection createResponselessConnection() throws IOException {
        final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConnection);
        return logInConnection;
    }

    private Evaluation createEvaluation(String designation, long when, String comment, String who) {
        return Evaluation.newBuilder()
            .setWhen(when)
            .setDesignation(designation)
            .setComment(comment)
            .setWho(who)
            .build();
    }

    private HttpURLConnection createFindIssuesConnection(InputStream response) throws IOException {
        final HttpURLConnection findConnection = mock(HttpURLConnection.class);
        when(findConnection.getInputStream()).thenReturn(response);
        setupResponseCodeAndOutputStream(findConnection);
        return findConnection;
    }

	private static void checkHashEncodeRoundtrip(String hash) {
		assertEquals(hash, AppEngineProtoUtil.decodeHash(AppEngineProtoUtil.encodeHash(hash)));
	}

	private <E> List<E> newList(Iterable<E> iterable) {
		List<E> result = new ArrayList<E>();
		for (E item : iterable) {
			result.add(item);
		}
		return result;
	}

	private Issue createFoundIssueProto() {
		return Issue.newBuilder()
				.setFirstSeen(SAMPLE_DATE+100)
				.setLastSeen(SAMPLE_DATE+200)
				.addEvaluations(Evaluation.newBuilder()
						.setWho("commenter")
						.setWhen(SAMPLE_DATE+300)
						.setComment("my comment")
						.setDesignation("NEEDS_STUDY")
						.build())
				.addEvaluations(Evaluation.newBuilder()
						.setWho("test@example.com")
						.setWhen(SAMPLE_DATE+400)
						.setComment("later comment")
						.setDesignation("NOT_A_BUG")
						.build())
				.addEvaluations(Evaluation.newBuilder()
						.setWho("test@example.com")
						.setWhen(SAMPLE_DATE+500)
						.setComment("latest comment")
						.setDesignation("MUST_FIX")
						.build())
				.build();
	}

	private Issue createFoundIssueWithOneEvaluation() {
		return createFoundIssue(Arrays.asList(createEvaluation("NOT_A_BUG", SAMPLE_DATE+200, "first comment", "test@example.com")));
	}

	private Issue createFoundIssue(Iterable<Evaluation> evaluations) {
		return Issue.newBuilder()
				.setFirstSeen(SAMPLE_DATE+100)
				.setLastSeen(SAMPLE_DATE+300)
				.addAllEvaluations(evaluations)
				.build();
	}

	private Issue createFullProtoIssue(Issue issue, Evaluation... evalsToAdd) {
        return Issue.newBuilder(issue)
				.setBugPattern(foundIssue.getAbbrev())
				.setHash(AppEngineProtoUtil.encodeHash(foundIssue.getInstanceHash()))
				.setPrimaryClass(foundIssue.getPrimaryClass().getClassName())
				.setPriority(1)
                .addAllEvaluations(Arrays.asList(evalsToAdd))
                .build();
	}

	private void checkUploadedEvaluation(UploadEvaluation uploadMsg) {
		assertEquals(555, uploadMsg.getSessionId());
		assertEquals(foundIssue.getInstanceHash(), AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
		assertEquals(foundIssue.getUserDesignationKey(), uploadMsg.getEvaluation().getDesignation());
		assertEquals(foundIssue.getAnnotationText(), uploadMsg.getEvaluation().getComment());
	}

	private ByteArrayOutputStream setupResponseCodeAndOutputStream(HttpURLConnection uploadConnection)
			throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		when(uploadConnection.getResponseCode()).thenReturn(200);
		when(uploadConnection.getOutputStream()).thenReturn(outputStream);
		return outputStream;
	}

	private MyAppEngineCloudClient createAppEngineCloudClient(HttpURLConnection... connections) throws IOException {
		SortedBugCollection bugs = new SortedBugCollection();
		if (addMissingIssue) bugs.add(missingIssue);
		bugs.add(foundIssue);
		final Iterator<HttpURLConnection> mockConnections = Arrays.asList(connections).iterator();
		CloudPlugin plugin = new CloudPlugin("AppEngineCloudClientTest", AppEngineCloudClient.class.getClassLoader(),
				AppEngineCloudClient.class, AppEngineNameLookup.class, new PropertyBundle(), "none", "none");
		Executor runImmediatelyExecutor = new Executor() {
			public void execute(Runnable command) {
				command.run();
			}
		};
		return new MyAppEngineCloudClient(plugin, bugs, runImmediatelyExecutor, mockConnections);
	}

	private void checkIssuesEqual(BugInstance issue, Issue uploadedIssue) {
		assertEquals(issue.getInstanceHash(), AppEngineProtoUtil.decodeHash(uploadedIssue.getHash()));
		assertEquals(issue.getType(), uploadedIssue.getBugPattern());
		assertEquals(issue.getPriority(), uploadedIssue.getPriority());
		assertEquals(0, uploadedIssue.getLastSeen());
		assertEquals(issue.getPrimaryClass().getClassName(), uploadedIssue.getPrimaryClass());
	}

	private InputStream createFindIssuesResponseInputStream(Issue foundIssue) {
		FindIssuesResponse.Builder issueList = FindIssuesResponse.newBuilder();
        if (addMissingIssue)
            issueList.addFoundIssues(Issue.newBuilder().build());

        issueList.addFoundIssues(foundIssue);
		return new ByteArrayInputStream(issueList.build().toByteArray());
	}

    private static class MyAppEngineCloudClient extends AppEngineCloudClient {
        public List<String> urlsRequested;
        private final Iterator<HttpURLConnection> mockConnections;
        private IGuiCallback mockGuiCallback;
        private AppEngineNameLookup mockNameLookup;
        private Long mockSessionId = null;

        public MyAppEngineCloudClient(CloudPlugin plugin, SortedBugCollection bugs,
                                      Executor runImmediatelyExecutor, Iterator<HttpURLConnection> mockConnectionsP)
                throws IOException {
            super(plugin, bugs, new Properties(), runImmediatelyExecutor);
            this.mockConnections = mockConnectionsP;
            urlsRequested = Lists.newArrayList();
            mockNameLookup = mock(AppEngineNameLookup.class);
            when(mockNameLookup.getHost()).thenReturn("host");
            when(mockNameLookup.getUsername()).thenReturn("test@example.com");
            when(mockNameLookup.getSessionId()).thenAnswer(new Answer<Long>() {
                public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                    return mockSessionId;
                }
            });
            when(mockNameLookup.initialize(Mockito.<CloudPlugin>any(), Mockito.<BugCollection>any())).thenAnswer(new Answer<Boolean>() {
                public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                    mockSessionId = 555L;
                    return true;
                }
            });


            setNetworkClient(new AppEngineCloudNetworkClient() {
                @Override
                HttpURLConnection openConnection(String url) {
                    System.err.println("opening " + url);
                    if (!mockConnections.hasNext()) {
                        fail("No mock connections left (for " + url + " - already requested URL's: " + urlsRequested + ")");
                    }
                    urlsRequested.add(url);
                    return mockConnections.next();
                }

                @Override
                protected AppEngineNameLookup createNameLookup() {
                    return mockNameLookup;
                }
            });
            mockGuiCallback = mock(IGuiCallback.class);
        }

        @Override
        protected ExecutorService getBugUpdateExecutor() {
            return backgroundExecutorService;
        }

        @Override
        protected IGuiCallback getGuiCallback() {
            return mockGuiCallback;
        }
    }
}
