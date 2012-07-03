package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class WebCloudEvalsTests extends AbstractWebCloudTest {
    
    protected static final long SAMPLE_DATE = System.currentTimeMillis() - 5 * 3600 * 1000;
    protected MockWebCloudClient cloud;

    private Issue responseIssue;

    @SuppressWarnings({ "deprecation" })
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE + 200, "my eval", "test@example.com"));
        foundIssue.setUserAnnotationDirty(true);
        cloud = createWebCloudClient();
        responseIssue = createIssueToReturn(createEvaluation("NOT_A_BUG", SAMPLE_DATE + 100, "comment", "first"));
    }
    
    protected void tearDown() throws Exception {
        cloud.awaitBackgroundTasks();
        cloud.throwBackgroundException();
        super.tearDown();
    }

    @SuppressWarnings("deprecation")
    public void testStoreUserAnnotationAfterUploading() throws Exception {
        // set up mocks
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        cloud.storeUserAnnotation(foundIssue);

        // verify
        cloud.verifyConnections();
        checkUploadedEvaluationMatches(foundIssue, UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation")));
    }

    @SuppressWarnings("deprecation")
    public void testStoreUserAnnotationAfterUploadingSavesToCloudReport() throws Exception {
        // set up mocks
        foundIssue.setUserDesignation(null);
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute
        cloud.initialize();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
        foundIssue.setUserDesignation(new BugDesignation("BAD_ANALYSIS", SAMPLE_DATE + 200, "!!my eval!!", "test@example.com"));
        cloud.storeUserAnnotation(foundIssue);

        // verify
        cloud.verifyConnections();
        checkUploadedEvaluationMatches(foundIssue, UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation")));
        assertEquals("!!my eval!!", cloud.getUserEvaluation(foundIssue));
        assertTrue(cloud.getCloudReport(foundIssue).contains("!!my eval!!"));
    }

    @SuppressWarnings("deprecation")
    public void testGetRecentEvaluationsFindsOne() throws Exception {
        // setup
        Issue prototype = createIssueToReturn(createEvaluation("NOT_A_BUG", SAMPLE_DATE + 200, "first comment",
                "test@example.com"));

        Evaluation firstEval = createEvaluation("MUST_FIX", SAMPLE_DATE + 250, "comment", "test@example.com");
        Evaluation lastEval = createEvaluation("MOSTLY_HARMLESS", SAMPLE_DATE + 300, "new comment", "test@example.com");
        RecentEvaluations recentEvalsResponse = RecentEvaluations.newBuilder()
                .addIssues(fillMissingFields(prototype, foundIssue, firstEval, lastEval)).build();

        cloud.expectConnection("get-recent-evaluations").withResponse(recentEvalsResponse);

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        cloud.updateEvaluationsFromServer();

        // verify
        checkStoredEvaluationMatches(lastEval, cloud.getPrimaryDesignation(foundIssue));
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates", 
                "Checking FindBugs Cloud for updates... found 1 so far...",
                "",
                "FindBugs Cloud: found 1 updated bug reviews");
    }

    @SuppressWarnings("deprecation")
    public void testGetRecentEvaluationsAsksAgain() throws Exception {
        // setup
        foundIssue.setUserDesignation(null);
        Issue prototype = createIssueToReturn(createEvaluation("NOT_A_BUG", SAMPLE_DATE + 200, "first comment",
                "test@example.com"));

        Evaluation earlierEval = createEvaluation("MOSTLY_HARMLESS", SAMPLE_DATE + 300, "old comment", "A@example.com");
        Evaluation laterEval = createEvaluation("MUST_FIX", SAMPLE_DATE + 350, "new comment", "B@example.com");
        RecentEvaluations resp1 = RecentEvaluations.newBuilder()
                .addIssues(fillMissingFields(prototype, foundIssue, earlierEval))
                .setAskAgain(true)
                .build();
        RecentEvaluations resp2 = RecentEvaluations.newBuilder()
                .addIssues(fillMissingFields(prototype, foundIssue, laterEval))
                .setAskAgain(false)
                .build();

        cloud.expectConnection("get-recent-evaluations").withResponse(resp1);
        cloud.expectConnection("get-recent-evaluations").withResponse(resp2);

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloaded();
        cloud.updateEvaluationsFromServer();

        // verify
        cloud.verifyConnections();
        assertTrue(cloud.getCloudReport(foundIssue).contains("A@example.com"));
        assertTrue(cloud.getCloudReport(foundIssue).contains("old comment"));
        assertTrue(cloud.getCloudReport(foundIssue).contains("B@example.com"));
        assertTrue(cloud.getCloudReport(foundIssue).contains("new comment"));
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates... found 1 so far...",
                "Checking FindBugs Cloud for updates... found 2 so far...",
                "",
                "FindBugs Cloud: found 2 updated bug reviews");
    }

    @SuppressWarnings("deprecation")
    public void testGetRecentEvaluationsFindsNone() throws Exception {
        // setup
        cloud.expectConnection("get-recent-evaluations").withResponse(RecentEvaluations.newBuilder().build());

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        cloud.updateEvaluationsFromServer();

        // verify
        cloud.checkStatusBarHistory(
                "Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates... found 0 so far...",
                "");
    }

    @SuppressWarnings("deprecation")
    public void testGetRecentEvaluationsFailsWithHttpErrorCode() throws Exception {
        // setup
        cloud.expectConnection("get-recent-evaluations").withErrorCode(500).repeatIndefinitely();

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        // verify
        cloud.checkStatusBarHistory("Checking FindBugs Cloud for updates",
                "Checking FindBugs Cloud for updates... FAILED - server returned error code 500 null");
    }

    @SuppressWarnings({ "deprecation", "ThrowableInstanceNeverThrown" })
    public void testGetRecentEvaluationsFailsWithNetworkError() throws Exception {
        // setup
        cloud.expectConnection("get-recent-evaluations").throwsNetworkError(new IOException("blah"));

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        cloud.setSigninState(Cloud.SigninState.SIGNED_IN);
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        assertEquals(Cloud.SigninState.SIGNED_OUT, cloud.getSigninState());

        // verify
        Mockito.verify(cloud.mockGuiCallback).showMessageDialog(Matchers.matches("(?s).*error.*signed out.*Cloud.*"));
        cloud.checkStatusBarHistory("Checking FindBugs Cloud for updates", "Signed out of FindBugs Cloud", "");
    }

    @SuppressWarnings({ "deprecation", "ThrowableInstanceNeverThrown" })
    public void testGetRecentEvaluationsFailsWhenNotSignedIn() throws Exception {
        // setup
        cloud.expectConnection("get-recent-evaluations").throwsNetworkError(new IOException("blah"));

        // execute
        cloud.initialize();
        cloud.pretendIssuesSyncedAndDownloadedAndUploaded();
        cloud.setSigninState(Cloud.SigninState.SIGNED_OUT);
        try {
            cloud.updateEvaluationsFromServer();
            fail();
        } catch (Exception e) {
        }

        // verify no dialogs, just status bar changes
        Mockito.verify(cloud.mockGuiCallback, Mockito.never()).showMessageDialog(Mockito.anyString());
        cloud.checkStatusBarHistory("Checking FindBugs Cloud for updates", "Checking FindBugs Cloud for updates... FAILED - IOException: blah");
    }

    public void testGetRecentEvaluationsOverwritesOldEvaluationsFromSamePerson() throws Exception {
        // setup
        RecentEvaluations recentEvalResponse = RecentEvaluations
                .newBuilder()
                .addIssues(
                        fillMissingFields(responseIssue, foundIssue,
                                createEvaluation("NOT_A_BUG", SAMPLE_DATE + 200, "comment2", "second"),
                                createEvaluation("NOT_A_BUG", SAMPLE_DATE + 300, "comment3", "first"))).build();

        cloud.expectConnection("find-issues");
        cloud.expectConnection("get-recent-evaluations").withResponse(recentEvalResponse);

        foundIssue.getNonnullUserDesignation().cleanDirty();
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

    @SuppressWarnings({ "deprecation" })
    public void XXXtestStoreAnnotationBeforeFindIssues() throws Exception {
        // setup
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        cloud.clickYes(".*XML.*contains.*reviews.*upload.*");
        CountDownLatch latch = cloud.getDialogLatch("Uploaded 1 reviews from XML \\(0 out of date, 0 already present\\)");

        // execute
        cloud.initialize();
        cloud.storeUserAnnotation(foundIssue);
        cloud.initiateCommunication();

        // verify
        waitForDialog(latch);
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

    @SuppressWarnings("deprecation")
    public void testUploadEvaluationsFromXMLWhenNoNewIssuesExist() throws Exception {
        // setup
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");
        cloud.clickYes(".*XML.*contains.*reviews.*upload.*");

        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

    @SuppressWarnings("deprecation")
    public void testUploadEvaluationsChangedWhileOffline() throws Exception {
        // setup
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
        cloud.verifyConnections();

        // setup 2
        cloud.expectConnection("log-out/555");

        // execute 2
        cloud.signOut();
        assertEquals(Cloud.SigninState.SIGNED_OUT, cloud.getSigninState());

        // setup 3
        foundIssue.setUserDesignation(new BugDesignation("I_WILL_FIX", SAMPLE_DATE + 300, "new", "test@example.com"));
        foundIssue.setUserAnnotationDirty(true);
        
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation");

        // execute 3
        cloud.signIn();

        // verify 3
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        cloud.verifyConnections();
        uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("I_WILL_FIX", uploadMsg.getEvaluation().getDesignation());
        assertEquals("new", uploadMsg.getEvaluation().getComment());
    }

    @SuppressWarnings("deprecation")
    public void testLocalEvaluationsClobberedWhenNewerExistsOnCloud() throws Exception {
        // setup
        responseIssue = createIssueToReturn(createEvaluation("NOT_A_BUG", SAMPLE_DATE + 100, "comment", "test@example.com"));
        foundIssue.setUserDesignation(new BugDesignation("I_WILL_FIX", SAMPLE_DATE + 50, "new", "test@example.com"));

        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));

        // execute
        cloud.initialize();
        cloud.initiateCommunication();

        // verify
        cloud.waitUntilIssueDataDownloaded();
        cloud.verifyConnections();
        assertEquals("comment", cloud.getUserEvaluation(foundIssue));
    }

    @SuppressWarnings("deprecation")
    public void testUploadEvaluationsFromXMLAfterUploadingNewIssues() throws Exception {
        // setup
        bugCollection.add(missingIssue);

        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, true));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-issues");
        cloud.expectConnection("upload-evaluation");
        cloud.clickYes(".*XML.*contains.*reviews.*upload.*");

        // execute
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // verify
        cloud.waitForStatusMsg("1 issues from XML uploaded to cloud");
        cloud.verifyConnections();
        UploadEvaluation uploadMsg = UploadEvaluation.parseFrom(cloud.postedData("upload-evaluation"));
        assertEquals("fad2", WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals("my eval", uploadMsg.getEvaluation().getComment());
    }

    @SuppressWarnings("deprecation")
    public void testDontUploadEvaluationsFromXMLWhenSigninFails() throws Exception {
        // setup
        bugCollection.add(missingIssue);

        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, true));
        cloud.expectConnection("log-in").withErrorCode(403);

        cloud.clickYes(".*XML.*contains.*reviews.*upload.*");
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
        MockWebCloudClient cloud = createWebCloudClient();
        cloud.expectConnection("find-issues").withResponse(createFindIssuesResponseObj(responseIssue, false));
        cloud.expectConnection("log-in");
        cloud.expectConnection("upload-evaluation").withErrorCode(403);
        cloud.clickYes(".*XML.*contains.*reviews.*upload.*");
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

    // =================================== end of tests
    // ===========================================

    private void waitForDialog(CountDownLatch latch) throws InterruptedException {
        assertTrue("latch timed out", latch.await(15, TimeUnit.SECONDS));
    }

    private static Evaluation createEvaluation(String designation, long when, String comment, String who) {
        return Evaluation.newBuilder().setWhen(when).setDesignation(designation).setComment(comment).setWho(who).build();
    }

    private static Issue createIssueToReturn(Evaluation... evaluations) {
        return Issue.newBuilder().setFirstSeen(SAMPLE_DATE + 100).setLastSeen(SAMPLE_DATE + 300)
                .addAllEvaluations(Arrays.asList(evaluations)).build();
    }

    private static Issue fillMissingFields(Issue prototype, BugInstance source, Evaluation... evalsToAdd) {
        return Issue.newBuilder(prototype).setBugPattern(source.getAbbrev())
                .setHash(WebCloudProtoUtil.encodeHash(source.getInstanceHash()))
                .setPrimaryClass(source.getPrimaryClass().getClassName()).setPriority(1)
                .addAllEvaluations(Arrays.asList(evalsToAdd)).build();
    }

    private static void checkStoredEvaluationMatches(Evaluation expectedEval, BugDesignation designation) {
        assertNotNull("Did not get any designation", designation);
        assertEquals(expectedEval.getComment(), designation.getAnnotationText());
        assertEquals(expectedEval.getDesignation(), designation.getDesignationKey());
        assertEquals(expectedEval.getWho(), designation.getUser());
        assertEquals(expectedEval.getWhen(), designation.getTimestamp());
    }

    private static void checkUploadedEvaluationMatches(BugInstance expectedValues, UploadEvaluation uploadMsg) {
        assertEquals(555, uploadMsg.getSessionId());
        assertEquals(expectedValues.getInstanceHash(), WebCloudProtoUtil.decodeHash(uploadMsg.getHash()));
        assertEquals(expectedValues.getUserDesignationKey(), uploadMsg.getEvaluation().getDesignation());
        assertEquals(expectedValues.getAnnotationText(), uploadMsg.getEvaluation().getComment());
    }

}
