package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AppEngineCloudEvalsTests extends AbstractAppEngineCloudTest {
    protected MockAppEngineCloudClient cloud;
    private Issue responseIssue;

    @SuppressWarnings({"deprecation"})
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE+200, "my eval", "test@example.com"));
        cloud = createAppEngineCloudClient();
        responseIssue = createIssueToReturn(createEvaluation("NOT_A_BUG", SAMPLE_DATE + 100, "comment", "first"));
    }

    @SuppressWarnings("deprecation")
	public void testStoreUserAnnotationAfterUploading() throws Exception {
		// set up mocks
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute
		cloud.initialize();
        cloud.pretendIssuesSyncedAndUploaded();
		cloud.storeUserAnnotation(foundIssue);

		// verify
        cloud.verifyConnections();
        checkUploadedEvaluationMatches(foundIssue,
                                       UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation")));
    }

	@SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFindsOne() throws Exception {
		// setup
        Issue prototype = createIssueToReturn(
                createEvaluation("NOT_A_BUG", SAMPLE_DATE + 200, "first comment", "test@example.com"));

        Evaluation firstEval = createEvaluation("MUST_FIX", SAMPLE_DATE + 250, "comment", "test@example.com");
        Evaluation lastEval = createEvaluation("MOSTLY_HARMLESS", SAMPLE_DATE + 300, "new comment", "test@example.com");
        RecentEvaluations recentEvalsResponse =
                RecentEvaluations.newBuilder()
                        .addIssues(fillMissingFields(prototype, foundIssue,
                                                     firstEval, lastEval))
                        .build();

        cloud.expectConnection("get-recent-evaluations").withResponse(recentEvalsResponse);

        // execute
        cloud.initialize();
		cloud.updateEvaluationsFromServer();

		// verify
        checkStoredEvaluationMatches(lastEval, cloud.getPrimaryDesignation(foundIssue));
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates...found 1");
    }

    @SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFindsNone() throws Exception {
		// setup
        cloud.expectConnection("get-recent-evaluations").withResponse(RecentEvaluations.newBuilder().build());

        // execute
        cloud.initialize();
		cloud.updateEvaluationsFromServer();

        // verify
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "");
    }

    @SuppressWarnings("deprecation")
	public void testGetRecentEvaluationsFailsWithHttpErrorCode() throws Exception {
		// setup
        cloud.expectConnection("get-recent-evaluations").withErrorCode(500);

        // execute
        cloud.initialize();
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        // verify
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates...failed - server returned error code 500 null");
    }

    @SuppressWarnings({"deprecation", "ThrowableInstanceNeverThrown"})
	public void testGetRecentEvaluationsFailsWithNetworkError() throws Exception {
		// setup
        cloud.expectConnection("get-recent-evaluations").throwsNetworkError(new IOException("blah"));

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndUploaded();
        cloud.setSigninState(Cloud.SigninState.SIGNED_IN);
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        assertEquals(Cloud.SigninState.SIGNED_OUT, cloud.getSigninState());

        // verify
        verify(cloud.mockGuiCallback).showMessageDialog(matches("(?s).*error.*signed out.*Cloud.*"));
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Signed out of FindBugs Cloud");
    }

    @SuppressWarnings({"deprecation", "ThrowableInstanceNeverThrown"})
	public void testGetRecentEvaluationsFailsWhenNotSignedIn() throws Exception {
		// setup
        cloud.expectConnection("get-recent-evaluations").throwsNetworkError(new IOException("blah"));

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndUploaded();
        cloud.setSigninState(Cloud.SigninState.SIGNED_OUT);
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        // verify no dialogs, just status bar changes
        verify(cloud.mockGuiCallback, never())
                .showMessageDialog(Mockito.anyString());
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates...failed - blah");
    }

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson()
			throws Exception {
        // setup
        RecentEvaluations recentEvalResponse = RecentEvaluations.newBuilder()
				.addIssues(fillMissingFields(responseIssue, foundIssue,
                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+200, "comment2", "second"),
                        createEvaluation("NOT_A_BUG", SAMPLE_DATE+300, "comment3", "first")))
				.build();

        cloud.expectConnection("find-issues");
        cloud.expectConnection("get-recent-evaluations").withResponse(recentEvalResponse);

        // execute
        cloud.initialize();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
		cloud.updateEvaluationsFromServer();

		// verify
        cloud.verifyConnections();
		List<BugDesignation> allUserDesignations = newList(cloud.getLatestDesignationFromEachUser(foundIssue));
		assertEquals(2, allUserDesignations.size());
	}

    @SuppressWarnings({"deprecation"})
    public void XXXtestStoreAnnotationBeforeFindIssues() throws Exception {
        // setup
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        cloud.clickYes(".*XML.*contains.*evaluations.*upload.*");
        CountDownLatch latch = cloud.getDialogLatch(
                "Uploaded 1 evaluations from XML \\(0 out of date, 0 already present\\)");

        // execute
        cloud.initialize();
        cloud.storeUserAnnotation(foundIssue);
        cloud.initiateCommunication();

        // verify
        waitForDialog(latch);
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

    @SuppressWarnings("deprecation")
	public void testUploadEvaluationsFromXMLWithoutUploadingIssues() throws Exception {
        // setup
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");
        cloud.clickYes(".*XML.*contains.*evaluations.*upload.*");
        
        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

	@SuppressWarnings("deprecation")
	public void testUploadEvaluationsFromXMLAfterUploadingIssues() throws Exception {
        // setup
        bugCollection.add(missingIssue);

        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, true));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-issues");
        cloud.expectConnection("upload-evaluation");
        cloud.clickYes(".*XML.*contains.*evaluations.*upload.*");
        
        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

	@SuppressWarnings("deprecation")
	public void testDontUploadEvaluationsFromXMLWhenSigninFails() throws Exception {
        // setup
        bugCollection.add(missingIssue);

        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, true));
        cloud.expectConnection("log-in").withErrorCode(403);

        cloud.clickYes(".*XML.*contains.*evaluations.*upload.*");
        cloud.clickYes(".*store.*sign in.*");
        CountDownLatch latch = cloud.getDialogLatch("Could not sign into.*");

        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        waitForDialog(latch);
        cloud.verifyConnections(); // no upload-evaluation
    }

    @SuppressWarnings("deprecation")
	public void testDontUploadEvaluationsFromXMLWhenFirstEvalUploadFails() throws Exception {
        // setup
        MockAppEngineCloudClient cloud = createAppEngineCloudClient();
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation").withErrorCode(403);
        cloud.clickYes(".*XML.*contains.*evaluations.*upload.*");
        cloud.clickYes(".*store.*sign in.*");
        CountDownLatch latch = cloud.getDialogLatch("Unable to upload.*XML.*cloud.*");

        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        waitForDialog(latch);
        cloud.verifyConnections();
    }

    // =================================== end of tests ===========================================

    private void waitForDialog(CountDownLatch latch) throws InterruptedException {
        assertTrue("latch timed out", latch.await(15, TimeUnit.SECONDS));
    }

    private static Evaluation createEvaluation(String designation, long when, String comment, String who) {
        return Evaluation.newBuilder()
            .setWhen(when)
            .setDesignation(designation)
            .setComment(comment)
            .setWho(who)
            .build();
    }

    private static Issue createIssueToReturn(Evaluation... evaluations) {
		return Issue.newBuilder()
				.setFirstSeen(SAMPLE_DATE+100)
				.setLastSeen(SAMPLE_DATE+300)
				.addAllEvaluations(Arrays.asList(evaluations))
				.build();
	}

	private static Issue fillMissingFields(Issue prototype, BugInstance source, Evaluation... evalsToAdd) {
        return Issue.newBuilder(prototype)
				.setBugPattern(source.getAbbrev())
				.setHash(AppEngineProtoUtil.encodeHash(source.getInstanceHash()))
				.setPrimaryClass(source.getPrimaryClass().getClassName())
				.setPriority(1)
                .addAllEvaluations(Arrays.asList(evalsToAdd))
                .build();
	}

    private static void checkStoredEvaluationMatches(Evaluation expectedEval, BugDesignation designation) {
        assertNotNull(designation);
        assertEquals(expectedEval.getComment(), designation.getAnnotationText());
        assertEquals(expectedEval.getDesignation(), designation.getDesignationKey());
        assertEquals(expectedEval.getWho(), designation.getUser());
        assertEquals(expectedEval.getWhen(), designation.getTimestamp());
    }

	private static void checkUploadedEvaluationMatches(BugInstance expectedValues, UploadEvaluation uploadMsg) {
		assertEquals(555, uploadMsg.getSessionId());
		assertEquals(expectedValues.getInstanceHash(), AppEngineProtoUtil.decodeHash(uploadMsg.getHash()));
		assertEquals(expectedValues.getUserDesignationKey(), uploadMsg.getEvaluation().getDesignation());
		assertEquals(expectedValues.getAnnotationText(), uploadMsg.getEvaluation().getComment());
	}

}