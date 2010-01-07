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

import junit.framework.TestCase;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogInResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;

public class AppEngineCloudTest extends TestCase {
	private BugInstance missingIssue = new BugInstance("MISSING", 2).addClass("MissingClass");
	private BugInstance foundIssue = new BugInstance("FOUND", 2).addClass("FoundClass");

	public void testBugsPopulated() throws IOException {
		final HttpURLConnection findConnection = mock(HttpURLConnection.class);
		final HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		ByteArrayOutputStream outputCollector = new ByteArrayOutputStream();
		when(findConnection.getOutputStream()).thenReturn(outputCollector);
		when(findConnection.getResponseCode()).thenReturn(200);
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
		LogInResponse issueList = LogInResponse.newBuilder()
				.addFoundIssues(foundIssueProto)
				.build();
		InputStream inputStream = new ByteArrayInputStream(issueList.toByteArray());
		when(findConnection.getInputStream()).thenReturn(inputStream);
		SortedBugCollection bugs = new SortedBugCollection();
		bugs.add(missingIssue);
		bugs.add(foundIssue);
		final Iterator<HttpURLConnection> mockConnections = Arrays.asList(findConnection, uploadConnection).iterator();
		AppEngineCloud cloud = new AppEngineCloud(bugs) {
			HttpURLConnection openConnection(String url) {
				return mockConnections.next();
			}
		};
		ByteArrayOutputStream uploadIssuesBuffer = new ByteArrayOutputStream();
		when(uploadConnection.getResponseCode()).thenReturn(200);
		when(uploadConnection.getOutputStream()).thenReturn(uploadIssuesBuffer);

		// execution
		cloud.setUsername("claimer");
		cloud.bugsPopulated();

		// verify uploaded hashes
		verify(findConnection).connect();
		LogIn hashes = LogIn.parseFrom(outputCollector.toByteArray());
		assertEquals("hash ", 2, hashes.getMyIssueHashesCount());
		assertEquals("hash did not encode correctly",
				 foundIssue.getInstanceHash(), hashes.getMyIssueHashes(0));
		assertEquals("hash did not encode correctly",
				 missingIssue.getInstanceHash(), hashes.getMyIssueHashes(1));

		// verify processing of existing issues
		assertEquals(100, cloud.getFirstSeen(foundIssue));
		assertEquals(500, cloud.getUserTimestamp(foundIssue));
		assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
		assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

		// verify uploaded issues
		UploadIssues uploadedIssues = UploadIssues.parseFrom(uploadIssuesBuffer.toByteArray());
		assertEquals(1, uploadedIssues.getNewIssuesCount());
		Issue uploadedIssue = uploadedIssues.getNewIssues(0);
		assertEquals(missingIssue.getInstanceHash(), uploadedIssue.getHash());
		assertEquals(missingIssue.getType(), uploadedIssue.getBugPattern());
		assertEquals(missingIssue.getPriority(), uploadedIssue.getPriority());
		assertEquals(missingIssue.getFirstVersion(), uploadedIssue.getFirstSeen());
		assertEquals(missingIssue.getLastVersion(), uploadedIssue.getLastSeen());
		assertEquals(missingIssue.getPrimaryClass().getClassName(), uploadedIssue.getPrimaryClass());
	}
}
