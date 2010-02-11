package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.normalizeHash;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppEngineCloudTest extends TestCase {
	private BugInstance missingIssue;
	private BugInstance foundIssue;
    private boolean addMissingIssue;

    @Override
	protected void setUp() throws Exception {
		missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
		foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");
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

	public void testLogInAndUploadIssues() throws IOException {
        addMissingIssue = true;
		// set up mocks
		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream logInOutput = setupResponseCodeAndOutputStream(logInConnection);

		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponseInputStream(createFoundIssue()));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream uploadIssuesBuffer = setupResponseCodeAndOutputStream(uploadConnection);

		// execution
		MyAppEngineCloud cloud = createAppEngineCloud(logInConnection, findIssuesConnection, uploadConnection);
		cloud.setUsername("claimer");
		cloud.bugsPopulated();

		// verify log-in
        assertEquals("/log-in", cloud.urlsRequested.get(0));
		verify(logInConnection).connect();
		LogIn logIn = LogIn.parseFrom(logInOutput.toByteArray());
        assertEquals(cloud.getBugCollection().getAnalysisTimestamp(), logIn.getAnalysisTimestamp());

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(1));
		verify(findIssuesConnection).connect();
		FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
		assertEquals(2, hashes.getMyIssueHashesCount());
		List<String> hashesFromFindIssues = AppEngineProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
		assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));
		assertTrue(hashesFromFindIssues.contains(missingIssue.getInstanceHash()));

		// verify processing of found issues
		assertEquals(100, cloud.getFirstSeen(foundIssue));
		assertEquals(500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignation);
		assertEquals("latest comment", primaryDesignation.getAnnotationText());
		assertEquals(500, primaryDesignation.getTimestamp());
		assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
		assertEquals("claimer", primaryDesignation.getUser());

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
		final HttpURLConnection conn = mock(HttpURLConnection.class);
		ByteArrayOutputStream outputCollector = setupResponseCodeAndOutputStream(conn);
		AppEngineCloud cloud = createAppEngineCloud(conn);

		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", 200, "my eval", "claimer"));

		// execute
		cloud.setUsername("claimer");
		cloud.setSessionId(100);
		cloud.storeUserAnnotation(foundIssue);

		// verify
		verify(conn).connect();
		UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(outputCollector.toByteArray());
		checkUploadedEvaluation(uploadMsg);
	}

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluations() throws Exception {
		// set up mocks
        addMissingIssue = false;
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", 200, "my eval", "claimer"));

		Issue issue = createFoundIssueWithOneEvaluation();

        final HttpURLConnection logInConnection = createResponselessConnection();

        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponseInputStream(issue));

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(issue,
                        createEvaluation("MUST_FIX", 250, "comment", "claimer"),
                        createEvaluation("MOSTLY_HARMLESS", 300, "new comment", "claimer")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		MyAppEngineCloud cloud = createAppEngineCloud(logInConnection, findConnection, recentEvalConnection);
		cloud.setUsername("claimer");
		cloud.setSessionId(100);
		cloud.bugsPopulated();
		cloud.updateEvaluationsFromServer();

		// verify
		BugDesignation primaryDesignationAfter = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignationAfter);
		assertEquals("new comment", primaryDesignationAfter.getAnnotationText());
		assertEquals("MOSTLY_HARMLESS", primaryDesignationAfter.getDesignationKey());
		assertEquals("claimer", primaryDesignationAfter.getUser());
		assertEquals(300, primaryDesignationAfter.getTimestamp());
	}

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson()
			throws Exception {
        addMissingIssue = false;
		Issue responseIssue = createFoundIssue(Arrays.asList(
                createEvaluation("NOT_A_BUG", 100, "comment", "first")));


        final HttpURLConnection logInConnection = createResponselessConnection();

        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponseInputStream(responseIssue));

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(responseIssue,
                        createEvaluation("NOT_A_BUG", 200, "comment2", "second"),

                        createEvaluation("NOT_A_BUG", 300, "comment3", "first")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		AppEngineCloud cloud = createAppEngineCloud(logInConnection, findConnection, recentEvalConnection);
		cloud.setUsername("claimer");
		cloud.setSessionId(100);
		cloud.bugsPopulated();
		cloud.updateEvaluationsFromServer();

		// verify
		List<BugDesignation> allUserDesignations = newList(cloud.getAllUserDesignations(foundIssue));
		assertEquals(2, allUserDesignations.size());
	}

    public void testFileBug() throws Exception {
        addMissingIssue = false;
		Issue responseIssue = createFoundIssue(Arrays.asList(
                createEvaluation("NOT_A_BUG", 100, "comment", "first")));

        final HttpURLConnection logInConnection = createResponselessConnection();

        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponseInputStream(responseIssue));

        final HttpURLConnection fileBugConnection = createResponselessConnection();

		// setup & execute
		AppEngineCloud cloud = createAppEngineCloud(logInConnection, findConnection, fileBugConnection);
		cloud.setUsername("claimer");
		cloud.setSessionId(100);
		cloud.bugsPopulated();
		cloud.updateEvaluationsFromServer();

		// verify
		List<BugDesignation> allUserDesignations = newList(cloud.getAllUserDesignations(foundIssue));
		assertEquals(2, allUserDesignations.size());
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

	private Issue createFoundIssue() {
		return Issue.newBuilder()
				.setFirstSeen(100)
				.setLastSeen(200)
				.addEvaluations(Evaluation.newBuilder()
						.setWho("commenter")
						.setWhen(300)
						.setComment("my comment")
						.setDesignation("NEEDS_STUDY")
						.build())
				.addEvaluations(Evaluation.newBuilder()
						.setWho("claimer")
						.setWhen(400)
						.setComment("later comment")
						.setDesignation("NOT_A_BUG")
						.build())
				.addEvaluations(Evaluation.newBuilder()
						.setWho("claimer")
						.setWhen(500)
						.setComment("latest comment")
						.setDesignation("MUST_FIX")
						.build())
				.build();
	}

	private Issue createFoundIssueWithOneEvaluation() {
		return createFoundIssue(Arrays.asList(createEvaluation("NOT_A_BUG", 200, "first comment", "claimer")));
	}

	private Issue createFoundIssue(Iterable<Evaluation> evaluations) {
		return Issue.newBuilder()
				.setFirstSeen(100)
				.setLastSeen(300)
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
		assertEquals(100, uploadMsg.getSessionId());
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

	private MyAppEngineCloud createAppEngineCloud(HttpURLConnection... connections) {
		SortedBugCollection bugs = new SortedBugCollection();
		if (addMissingIssue) bugs.add(missingIssue);
		bugs.add(foundIssue);
		final Iterator<HttpURLConnection> mockConnections = Arrays.asList(connections).iterator();
		CloudPlugin plugin = new CloudPlugin("AppEngineCloudTest", AppEngineCloud.class.getClassLoader(),
				AppEngineCloud.class, AppEngineNameLookup.class, new PropertyBundle(), "none", "none");
		Executor runImmediatelyExecutor = new Executor() {
			public void execute(Runnable command) {
				command.run();
			}
		};
		return new MyAppEngineCloud(plugin, bugs, runImmediatelyExecutor, mockConnections);
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

    private static class MyAppEngineCloud extends AppEngineCloud {
        public List<String> urlsRequested;
        private final Iterator<HttpURLConnection> mockConnections;

        public MyAppEngineCloud(CloudPlugin plugin, SortedBugCollection bugs, Executor runImmediatelyExecutor, Iterator<HttpURLConnection> mockConnections) {
            super(plugin, bugs, runImmediatelyExecutor);
            this.mockConnections = mockConnections;
            urlsRequested = Lists.newArrayList();
        }

        HttpURLConnection openConnection(String url) {
            if (!mockConnections.hasNext()) {
                fail("No connection for " + url + " (past URL's: " + urlsRequested + ")");
            }
            urlsRequested.add(url);
            return mockConnections.next();
        }
    }
}
