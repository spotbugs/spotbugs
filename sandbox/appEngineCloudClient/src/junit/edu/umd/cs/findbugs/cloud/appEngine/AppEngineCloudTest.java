package edu.umd.cs.findbugs.cloud.appEngine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

public class AppEngineCloudTest extends TestCase {
	private BugInstance missingIssue;
	private BugInstance foundIssue;

	@Override
	protected void setUp() throws Exception {
		missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
		foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");
	}

	public void testFindAndUploadIssues() throws IOException {
		// set up mocks
		final HttpURLConnection findConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream outputCollector = setupResponseCodeAndOutputStream(findConnection);
		when(findConnection.getInputStream()).thenReturn(createLogInResponseInputStream(createFoundIssue()));
		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream uploadIssuesBuffer = setupResponseCodeAndOutputStream(uploadConnection);

		// execution
		AppEngineCloud cloud = createAppEngineCloud(true, findConnection, uploadConnection);
		cloud.setUsername("claimer");
		cloud.bugsPopulated();

		// verify uploaded hashes
		verify(findConnection).connect();
		LogIn hashes = LogIn.parseFrom(outputCollector.toByteArray());
		assertEquals(2, hashes.getMyIssueHashesCount());
		List<String> hashesFromLogIn = hashes.getMyIssueHashesList();
		assertTrue(hashesFromLogIn.contains(foundIssue.getInstanceHash()));
		assertTrue(hashesFromLogIn.contains(missingIssue.getInstanceHash()));

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
		UploadIssues uploadedIssues = UploadIssues.parseFrom(uploadIssuesBuffer.toByteArray());
		assertEquals(1, uploadedIssues.getNewIssuesCount());
		checkIssuesEqual(missingIssue, uploadedIssues.getNewIssues(0));
	}

	@SuppressWarnings("deprecation")
	public void testStoreUserAnnotation() throws Exception {
		// set up mocks
		final HttpURLConnection conn = mock(HttpURLConnection.class);
		ByteArrayOutputStream outputCollector = setupResponseCodeAndOutputStream(conn);
		AppEngineCloud cloud = createAppEngineCloud(true, conn);

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
		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", 200, "my eval", "claimer"));

		Issue issue = createFoundIssueWithOneEvaluation();

		final HttpURLConnection findConnection = mock(HttpURLConnection.class);
		when(findConnection.getInputStream()).thenReturn(createLogInResponseInputStream(issue));
		setupResponseCodeAndOutputStream(findConnection);

		final HttpURLConnection recentEvalConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(recentEvalConnection);
		RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(addEvaluationsToIssue(issue))
				.build();
		when(recentEvalConnection.getInputStream()).thenReturn(
				new ByteArrayInputStream(recentEvalResponse.toByteArray()));


		// setup & execute
		AppEngineCloud cloud = createAppEngineCloud(false, findConnection, recentEvalConnection);
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

	private Issue createFoundIssueWithOneEvaluation() {
		Issue issue = Issue.newBuilder()
				.setBugPattern(foundIssue.getAbbrev())
				.setHash(foundIssue.getInstanceHash())
				.setFirstSeen(100)
				.setLastSeen(300)
				.setPrimaryClass(foundIssue.getPrimaryClass().getClassName())
				.setPriority(1)
				.addEvaluations(Evaluation.newBuilder()
						.setWhen(200)
						.setDesignation("NOT_A_BUG")
						.setComment("first comment")
						.setWho("claimer")
						.build())
				.build();
		return issue;
	}

	private Issue addEvaluationsToIssue(Issue issue) {
		return Issue.newBuilder(issue)
				.addEvaluations(Evaluation.newBuilder()
					.setWhen(250)
					.setDesignation("MUST_FIX")
					.setComment("middle comment")
					.setWho("claimer")
					.build())
				.addEvaluations(Evaluation.newBuilder()
						.setWhen(300)
						.setDesignation("MOSTLY_HARMLESS")
						.setComment("new comment")
						.setWho("claimer")
						.build())
				.build();
	}

	private void checkUploadedEvaluation(UploadEvaluation uploadMsg) {
		assertEquals(100, uploadMsg.getSessionId());
		assertEquals(foundIssue.getInstanceHash(), uploadMsg.getHash());
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

	private AppEngineCloud createAppEngineCloud(boolean addMissingIssue, HttpURLConnection... connections) {
		SortedBugCollection bugs = new SortedBugCollection();
		if (addMissingIssue) bugs.add(missingIssue);
		bugs.add(foundIssue);
		final Iterator<HttpURLConnection> mockConnections = Arrays.asList(connections).iterator();
		CloudPlugin plugin = new CloudPlugin("AppEngineCloudTest", AppEngineCloud.class.getClassLoader(),
				AppEngineCloud.class, AppEngineNameLookup.class, new PropertyBundle(), "none", "none");
		return new AppEngineCloud(plugin, bugs) {
			HttpURLConnection openConnection(String url) {
				return mockConnections.next();
			}
		};
	}

	private void checkIssuesEqual(BugInstance issue, Issue uploadedIssue) {
		assertEquals(issue.getInstanceHash(), uploadedIssue.getHash());
		assertEquals(issue.getType(), uploadedIssue.getBugPattern());
		assertEquals(issue.getPriority(), uploadedIssue.getPriority());
		assertEquals(issue.getFirstVersion(), uploadedIssue.getFirstSeen());
		assertEquals(issue.getLastVersion(), uploadedIssue.getLastSeen());
		assertEquals(issue.getPrimaryClass().getClassName(), uploadedIssue.getPrimaryClass());
	}

	private InputStream createLogInResponseInputStream(Issue foundIssue) {
		LogInResponse issueList = LogInResponse.newBuilder()
				.addFoundIssues(foundIssue)
				.build();
		return new ByteArrayInputStream(issueList.toByteArray());
	}

	private Issue createFoundIssue() {
		return Issue.newBuilder()
				.setBugPattern("FOUND")
				.setPriority(2)
				.setFirstSeen(100)
				.setLastSeen(200)
				.setHash(foundIssue.getInstanceHash())
				.setPrimaryClass("MyClass")
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
}
