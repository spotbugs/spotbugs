package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.MutableCloudTask;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import org.mockito.Matchers;
import org.mockito.Mockito;

import static edu.umd.cs.findbugs.cloud.Cloud.SigninState.UNAUTHENTICATED;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebCloudIssueSyncTests extends AbstractWebCloudTest {

    public void testFindIssuesAllFound() throws IOException, InterruptedException {
        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
        verify(findIssuesConnection).connect();
        FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
        assertEquals(1, hashes.getMyIssueHashesCount());
        List<String> hashesFromFindIssues = WebCloudProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
        assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));

        // verify processing of found issues
        assertEquals(SAMPLE_DATE + 100, cloud.getFirstSeen(foundIssue));
        assertEquals(SAMPLE_DATE + 500, cloud.getUserTimestamp(foundIssue));
        assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
        assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

        BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
        assertNotNull(primaryDesignation);
        assertEquals("latest comment", primaryDesignation.getAnnotationText());
        assertEquals(SAMPLE_DATE + 500, primaryDesignation.getTimestamp());
        assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
        assertEquals("test@example.com", primaryDesignation.getUser());
    }

    public void testFindIssuesNetworkFailure() throws IOException, InterruptedException {
        final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(findIssuesConn.getResponseCode()).thenReturn(500);
        when(findIssuesConn.getOutputStream()).thenReturn(outputStream);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConn);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        assertEquals(1, cloud.urlsRequested.size());
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    public void testFindIssuesNetworkFailureRetries() throws IOException, InterruptedException {
        // execution
        final MockWebCloudClient cloud = createWebCloudClient();
        cloud.expectConnection("find-issues").withErrorCode(500);
        cloud.expectConnection("find-issues").withErrorCode(200);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        cloud.verifyConnections();

        assertEquals(2, cloud.urlsRequested.size());
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
        assertEquals("/find-issues", cloud.urlsRequested.get(1));
    }

    public void testLogInAndUploadIssues() throws IOException, InterruptedException {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        ByteArrayOutputStream findIssuesOutput = setupResponseCodeAndOutputStream(findIssuesConnection);

        final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream logInOutput = setupResponseCodeAndOutputStream(logInConnection);

        HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
        ByteArrayOutputStream uploadIssuesBuffer = setupResponseCodeAndOutputStream(uploadConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection, logInConnection, uploadConnection);
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(Cloud.SigninState.SIGNED_IN, cloud.getSigninState());

        // verify find-issues
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
        verify(findIssuesConnection).connect();
        FindIssues hashes = FindIssues.parseFrom(findIssuesOutput.toByteArray());
        assertEquals(2, hashes.getMyIssueHashesCount());
        List<String> hashesFromFindIssues = WebCloudProtoUtil.decodeHashes(hashes.getMyIssueHashesList());
        assertTrue(hashesFromFindIssues.contains(foundIssue.getInstanceHash()));
        assertTrue(hashesFromFindIssues.contains(missingIssue.getInstanceHash()));

        // verify log-in
        assertEquals("/log-in", cloud.urlsRequested.get(1));
        verify(logInConnection).connect();
        LogIn logIn = LogIn.parseFrom(logInOutput.toByteArray());
        assertEquals(cloud.getBugCollection().getAnalysisTimestamp(), logIn.getAnalysisTimestamp());

        // verify processing of found issues
        assertEquals(SAMPLE_DATE + 100, cloud.getFirstSeen(foundIssue));
        assertEquals(SAMPLE_DATE + 500, cloud.getUserTimestamp(foundIssue));
        assertEquals("latest comment", cloud.getUserEvaluation(foundIssue));
        assertEquals(UserDesignation.MUST_FIX, cloud.getUserDesignation(foundIssue));

        BugDesignation primaryDesignation = cloud.getPrimaryDesignation(foundIssue);
        assertNotNull(primaryDesignation);
        assertEquals("latest comment", primaryDesignation.getAnnotationText());
        assertEquals(SAMPLE_DATE + 500, primaryDesignation.getTimestamp());
        assertEquals("MUST_FIX", primaryDesignation.getDesignationKey());
        assertEquals("test@example.com", primaryDesignation.getUser());

        // verify uploaded issues

        assertEquals("/upload-issues", cloud.urlsRequested.get(2));
        UploadIssues uploadedIssues = UploadIssues.parseFrom(uploadIssuesBuffer.toByteArray());
        assertEquals(1, uploadedIssues.getNewIssuesCount());
        checkIssuesEqual(missingIssue, uploadedIssues.getNewIssues(0));
    }

    public void testUserCancelsLogInAndUploadIssues() throws IOException, InterruptedException {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection);
        when(cloud.mockGuiCallback.showConfirmDialog(anyString(), anyString(), anyString(), anyString())).thenReturn(-1);
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(Cloud.SigninState.SIGNIN_DECLINED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    public void testDontUploadInTextMode() throws IOException, InterruptedException {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection);
        when(cloud.mockGuiCallback.isHeadless()).thenReturn(true);
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(120, TimeUnit.SECONDS);

        assertEquals(UNAUTHENTICATED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testLogInAndUploadIssuesFailsDuringSignIn() throws IOException, InterruptedException {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection);
        WebCloudNetworkClient spyNetworkClient = cloud.createSpyNetworkClient();
        Mockito.doThrow(new IOException()).when(spyNetworkClient).signIn(Matchers.anyBoolean());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);

        assertEquals(Cloud.SigninState.SIGNIN_FAILED, cloud.getSigninState());

        // verify
        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testLogInAndUploadIssuesFailsDuringFindIssues() throws Exception {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

        // execution
        MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection);
        WebCloudNetworkClient spyNetworkClient = cloud.createSpyNetworkClient();
        Mockito.doThrow(new RuntimeException())
                .when(spyNetworkClient).generateHashCheckRunnables(
                Matchers.<MutableCloudTask>any(),
                Matchers.<List<String>>any(),
                Matchers.<List<Callable<Object>>>any(),
                Matchers.<ConcurrentMap<String, BugInstance>>any());
        cloud.initialize();
        assertEquals(UNAUTHENTICATED, cloud.getSigninState());
        cloud.bugsPopulated();
        cloud.initiateCommunication();

        // this call will fail if the issues-uploaded event is not fired within 5 seconds
        cloud.waitUntilIssuesUploaded(5, TimeUnit.SECONDS);
    }

    // =================================== end of tests
    // ===========================================

    private void checkIssuesEqual(BugInstance issue, Issue uploadedIssue) {
        assertEquals(issue.getInstanceHash(), WebCloudProtoUtil.decodeHash(uploadedIssue.getHash()));
        assertEquals(issue.getType(), uploadedIssue.getBugPattern());
        assertEquals(issue.getPriority(), uploadedIssue.getPriority());
        assertEquals(0, uploadedIssue.getLastSeen());
        assertEquals(issue.getPrimaryClass().getClassName(), uploadedIssue.getPrimaryClass());
    }
}
