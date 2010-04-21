package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppEngineCloudEvalsTests extends AbstractAppEngineCloudTest {
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
	public void testGetRecentEvaluationsFindsOne() throws Exception {
		// set up mocks
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
                     cloud.statusChanges);
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
                     cloud.statusChanges);
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
                     cloud.statusChanges);
	}

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson()
			throws Exception {
		Issue responseIssue = createFoundIssue(Arrays.asList(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE+100, "comment", "first")));


        final HttpURLConnection findConnection = createFindIssuesConnection(createFindIssuesResponse(responseIssue));

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
		cloudClient.initiateCommunication();
		cloudClient.updateEvaluationsFromServer();

		// verify
		List<BugDesignation> allUserDesignations = newList(cloudClient.getLatestDesignationFromEachUser(foundIssue));
		assertEquals(2, allUserDesignations.size());
	}

    // =================================== end of tests ===========================================

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

}