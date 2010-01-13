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
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

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
		ByteArrayOutputStream outputCollector = mockConnection(findConnection);
		when(findConnection.getInputStream()).thenReturn(createLogInResponseInputStream(createFoundIssue()));
		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream uploadIssuesBuffer = mockConnection(uploadConnection);
		AppEngineCloud cloud = createAppEngineCloud(findConnection, uploadConnection);

		// execution
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
		ByteArrayOutputStream outputCollector = mockConnection(conn);
		AppEngineCloud cloud = createAppEngineCloud(conn);

		foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", 200, "my eval", "myuser"));

		// execute
		cloud.setUsername("claimer");
		cloud.setSessionId(100);
		cloud.storeUserAnnotation(foundIssue);

		// verify
		verify(conn).connect();
		UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(outputCollector.toByteArray());
		checkUploadedEvaluation(uploadMsg);
	}

	// ======================== end of tests ================================

	private void checkUploadedEvaluation(UploadEvaluation uploadMsg) {
		assertEquals(100, uploadMsg.getSessionId());
		assertEquals(foundIssue.getInstanceHash(), uploadMsg.getHash());
		assertEquals(foundIssue.getUserDesignationKey(), uploadMsg.getEvaluation().getDesignation());
		assertEquals(foundIssue.getAnnotationText(), uploadMsg.getEvaluation().getComment());
	}

	private ByteArrayOutputStream mockConnection(HttpURLConnection uploadConnection)
			throws IOException {
		ByteArrayOutputStream uploadIssuesBuffer = new ByteArrayOutputStream();
		when(uploadConnection.getResponseCode()).thenReturn(200);
		when(uploadConnection.getOutputStream()).thenReturn(uploadIssuesBuffer);
		return uploadIssuesBuffer;
	}

	private AppEngineCloud createAppEngineCloud(HttpURLConnection... connections) {
		SortedBugCollection bugs = new SortedBugCollection();
		bugs.add(missingIssue);
		bugs.add(foundIssue);
		final Iterator<HttpURLConnection> mockConnections = Arrays.asList(connections).iterator();
		return new AppEngineCloud(bugs) {
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
		Issue foundIssueProto = Issue.newBuilder()
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
		return foundIssueProto;
	}
}
