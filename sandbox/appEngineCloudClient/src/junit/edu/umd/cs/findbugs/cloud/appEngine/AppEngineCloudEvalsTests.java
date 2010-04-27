package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppEngineCloudEvalsTests extends AbstractAppEngineCloudTest {
	@SuppressWarnings("deprecation")
	public void testStoreUserAnnotationAfterUploading() throws Exception {
		// set up mocks
        addMissingIssue = true;
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

		MockAppEngineCloudClient cloud = createAppEngineCloudClient();

        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute
		cloud.initialize();
        cloud.pretendIssuesSyncedAndUploaded();
		cloud.storeUserAnnotation(foundIssue);

		// verify
        cloud.verifyAllConnectionsOpened();
        checkEvaluationMatches(foundIssue,
                               UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation")));
	}

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFindsOne() throws Exception {
		// set up mocks
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

		Issue issue = createIssueToReturnWithEvaluation();

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(issue, foundIssue,
                        createEvaluation("MUST_FIX", SAMPLE_DATE+250, "comment", "test@example.com"),
                        createEvaluation("MOSTLY_HARMLESS", SAMPLE_DATE+300, "new comment", "test@example.com")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(recentEvalConnection);
        cloud.initialize();
		cloud.updateEvaluationsFromServer();

		// verify
		BugDesignation primaryDesignationAfter = cloud.getPrimaryDesignation(foundIssue);
		assertNotNull(primaryDesignationAfter);
		assertEquals("new comment", primaryDesignationAfter.getAnnotationText());
		assertEquals("MOSTLY_HARMLESS", primaryDesignationAfter.getDesignationKey());
		assertEquals("test@example.com", primaryDesignationAfter.getUser());
		assertEquals(SAMPLE_DATE+300, primaryDesignationAfter.getTimestamp());

		assertEquals(Arrays.asList("Checking FindBugs Cloud for updates",
                                   "Checking FindBugs Cloud for updates...found 1"),
                     cloud.statusBarHistory);
	}

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFindsNone() throws Exception {
		// set up mocks
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		MockAppEngineCloudClient cloud = createAppEngineCloudClient(recentEvalConnection);
        cloud.initialize();
		cloud.updateEvaluationsFromServer();

		// verify
		assertEquals(Arrays.asList("Checking FindBugs Cloud for updates",
                                   ""),
                     cloud.statusBarHistory);
	}

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFails() throws Exception {
		// set up mocks
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        final HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        when(conn.getResponseCode()).thenReturn(500);

		// setup & execute
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(conn);

        cloud.initialize();
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        // verify
		assertEquals(Arrays.asList("Checking FindBugs Cloud for updates",
                                   "Checking FindBugs Cloud for updates...failed - server returned error code 500 null"),
                     cloud.statusBarHistory);
	}

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson()
			throws Exception {
		Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));

        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

        final HttpURLConnection recentEvalConnection = createResponselessConnection();
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(createFullProtoIssue(responseIssue, foundIssue,
                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+200, "comment2", "second"),
                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+300, "comment3", "first")))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		AppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, recentEvalConnection);
        cloudClient.initialize();
		cloudClient.bugsPopulated();
        cloudClient.initiateCommunication();
        cloudClient.waitUntilIssueDataDownloaded();
		cloudClient.updateEvaluationsFromServer();

		// verify
		List<BugDesignation> allUserDesignations = newList(cloudClient.getLatestDesignationFromEachUser(foundIssue));
		assertEquals(2, allUserDesignations.size());
	}

    @SuppressWarnings({"deprecation"})
    public void testStoreAnnotationBeforeFindIssues() throws Exception {
        Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        // set up mocks
        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

        final HttpURLConnection uploadEvalConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream uploadBytes = setupResponseCodeAndOutputStream(uploadEvalConnection);

        MockAppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, logInConnection, uploadEvalConnection);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*XML.*contains.*evaluations.*upload.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(cloudClient.mockGuiCallback).showMessageDialog("Uploaded 1 evaluations from XML (0 out of date, 0 already present)");

        // execute
        cloudClient.storeUserAnnotation(foundIssue);
        cloudClient.initialize();
        cloudClient.initiateCommunication();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

		// verify
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(uploadBytes.toByteArray());
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

	@SuppressWarnings("deprecation")
	public void testUploadEvaluationsFromXMLWithoutUploadingIssues() throws Exception {
        Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        // set up mocks
        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

        final HttpURLConnection uploadEvalConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream uploadBytes = setupResponseCodeAndOutputStream(uploadEvalConnection);

        MockAppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, logInConnection, uploadEvalConnection);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*XML.*contains.*evaluations.*upload.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(cloudClient.mockGuiCallback).showMessageDialog("Uploaded 1 evaluations from XML (0 out of date, 0 already present)");

        // execute
        cloudClient.initialize();
        cloudClient.initiateCommunication();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

		// verify
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(uploadBytes.toByteArray());
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

	@SuppressWarnings("deprecation")
	public void testUploadEvaluationsFromXMLAfterUploadingIssues() throws Exception {
        Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        // set up mocks
        addMissingIssue = true;

        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

		final HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

        final HttpURLConnection uploadEvalConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream uploadBytes = setupResponseCodeAndOutputStream(uploadEvalConnection);

        MockAppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, logInConnection,
                                                                          uploadConnection, uploadEvalConnection);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*XML.*contains.*evaluations.*upload.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(cloudClient.mockGuiCallback).showMessageDialog("Uploaded 1 evaluations from XML (0 out of date, 0 already present)");

        // execute
        cloudClient.initialize();
        cloudClient.initiateCommunication();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

		// verify
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(uploadBytes.toByteArray());
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

	@SuppressWarnings("deprecation")
	public void testDontUploadEvaluationsFromXMLWhenSigninFails() throws Exception {
        Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        // set up mocks
        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		when(logInConnection.getResponseCode()).thenReturn(403);

        MockAppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, logInConnection);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*XML.*contains.*evaluations.*upload.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*store.*sign in.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(cloudClient.mockGuiCallback).showMessageDialog(matches(".*Could not sign into.*"));

        // execute
        cloudClient.initialize();
        cloudClient.initiateCommunication();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

	@SuppressWarnings("deprecation")
	public void testDontUploadEvaluationsFromXMLWhenFirstEvalUploadFails() throws Exception {
        Issue responseIssue = createIssueToReturn(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));

        // set up mocks
        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

        final HttpURLConnection uploadEvalConnection = createResponselessConnection();
        when(uploadEvalConnection.getResponseCode()).thenReturn(403);

        MockAppEngineCloudClient cloudClient = createAppEngineCloudClient(findConnection, logInConnection, uploadEvalConnection);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*XML.*contains.*evaluations.*upload.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        when(cloudClient.mockGuiCallback.showConfirmDialog(matches(".*store.*sign in.*"),
                                                           anyString(), anyString(), anyString()))
                .thenReturn(IGuiCallback.YES_OPTION);
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(cloudClient.mockGuiCallback).showMessageDialog(matches(".*Could not.*XML.*server.*"));

        // execute
        cloudClient.initialize();
        cloudClient.bugsPopulated();
        cloudClient.initiateCommunication();
        boolean timedOut = !latch.await(5, TimeUnit.SECONDS);
        assertTrue(!timedOut);
    }

    // =================================== end of tests ===========================================

    private static Evaluation createEvaluation(String designation, long when, String comment, String who) {
        return Evaluation.newBuilder()
            .setWhen(when)
            .setDesignation(designation)
            .setComment(comment)
            .setWho(who)
            .build();
    }

    private static HttpURLConnection createFindIssuesConnection(InputStream response) throws IOException {
        final HttpURLConnection findConnection = mock(HttpURLConnection.class);
        when(findConnection.getInputStream()).thenReturn(response);
        setupResponseCodeAndOutputStream(findConnection);
        return findConnection;
    }

    private static Issue createIssueToReturnWithEvaluation() {
		return createIssueToReturn(Arrays.asList(createEvaluation("NOT_A_BUG", SAMPLE_DATE+200, "first comment", "test@example.com")));
	}

	private static Issue createIssueToReturn(Iterable<Evaluation> evaluations) {
		return Issue.newBuilder()
				.setFirstSeen(SAMPLE_DATE+100)
				.setLastSeen(SAMPLE_DATE+300)
				.addAllEvaluations(evaluations)
				.build();
	}

	private static Issue createFullProtoIssue(Issue prototype, BugInstance source, Evaluation... evalsToAdd) {
        return Issue.newBuilder(prototype)
				.setBugPattern(source.getAbbrev())
				.setHash(AppEngineProtoUtil.encodeHash(source.getInstanceHash()))
				.setPrimaryClass(source.getPrimaryClass().getClassName())
				.setPriority(1)
                .addAllEvaluations(Arrays.asList(evalsToAdd))
                .build();
	}

	private static void checkEvaluationMatches(BugInstance issue, UploadEvaluation uploadMsg) {
		assertEquals(555, uploadMsg.getSessionId());
		assertEquals(issue.getInstanceHash(), AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
		assertEquals(issue.getUserDesignationKey(), uploadMsg.getEvaluation().getDesignation());
		assertEquals(issue.getAnnotationText(), uploadMsg.getEvaluation().getComment());
	}

}