package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.findbugs.cloud.Cloud;
import org.mockito.Mockito;

public class WebCloudCallbackTests extends AbstractWebCloudTest {

    public void testWaitForIssueSyncAllFound() throws Exception {
        // set up mocks
        final HttpURLConnection findIssuesConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConn);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConn);
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                latch.countDown();
            }
        }).start();
        assertEquals(1, latch.getCount());
        cloud.initialize();
        assertEquals(1, latch.getCount());
        cloud.bugsPopulated();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    public void testWaitForIssueSyncReturnsImmediatelyWhenAlreadySynced() throws Exception {
        // set up mocks
        final HttpURLConnection findIssuesConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConn);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConn);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        final CountDownLatch latch = addStatusListenerWaiter(cloud);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                doneWaiting.set(true);
                latch.countDown();
            }
        }).start();
        assertFalse(doneWaiting.get());
        cloud.initialize();
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        Thread.sleep(1000);
        assertTrue("expected completion", doneWaiting.get());

        long start = System.currentTimeMillis();
        cloud.waitUntilIssueDataDownloaded();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 100) // should return immediately
            fail("was " + elapsed);
    }

    public void testWaitForIssueSyncNetworkFailure() throws Exception {
        // set up mocks
        final HttpURLConnection findIssuesConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(findIssuesConn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Mockito.when(findIssuesConn.getResponseCode()).thenReturn(500);
        Mockito.when(findIssuesConn.getOutputStream()).thenReturn(outputStream);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConn);
        final CountDownLatch latch = addStatusListenerWaiter(cloud);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                doneWaiting.set(true);
                latch.countDown();
            }
        }).start();
        assertFalse(doneWaiting.get());
        cloud.initialize();
        assertFalse(doneWaiting.get());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        Thread.sleep(1000);
        assertTrue("expected communications to be done", doneWaiting.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    /**
     * The UI updates when waitUntilIssueDataDownloaded returns, so this test
     * ensures that the caller doesn't wait longer than necessary.
     */
    public void testWaitForIssueSyncReturnsBeforeUpload() throws Throwable {
        addMissingIssue = true;

        // set up mocks
        final HttpURLConnection findIssuesConnection = Mockito.mock(HttpURLConnection.class);
        Mockito.when(findIssuesConnection.getInputStream())
                .thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConnection);

        final HttpURLConnection logInConnection = Mockito.mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(logInConnection);

        HttpURLConnection uploadConnection = Mockito.mock(HttpURLConnection.class);
        setupResponseCodeAndOutputStream(uploadConnection);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConnection, logInConnection, uploadConnection);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);

        final CountDownLatch latch = addStatusListenerWaiter(cloud);
        Future<Throwable> bgThreadFuture = Executors.newSingleThreadExecutor().submit(new Callable<Throwable>() {
            public Throwable call() throws Exception {
                try {
                    cloud.waitUntilIssueDataDownloaded();
                    doneWaiting.set(true);
                    assertEquals(1, cloud.urlsRequested.size());
                    assertEquals("/find-issues", cloud.urlsRequested.get(0));
                    latch.countDown(); // now the bg thread can continue
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                    return e;
                }
            }
        });
        assertFalse(doneWaiting.get());
        cloud.initialize();
        assertFalse(doneWaiting.get());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        Thread.sleep(1000);
        assertTrue("expected communcation to be done", doneWaiting.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
        assertEquals("/log-in", cloud.urlsRequested.get(1));
        assertEquals("/upload-issues", cloud.urlsRequested.get(2));

        // make any exception thrown in the bg thread is registered as a failure
        Throwable t = bgThreadFuture.get();
        if (t != null)
            throw t;
    }

    public void testIssueDataDownloadedCallback() throws IOException, InterruptedException {
        // set up mocks
        final HttpURLConnection findIssuesConn = Mockito.mock(HttpURLConnection.class);
        Mockito.when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto(), addMissingIssue));
        setupResponseCodeAndOutputStream(findIssuesConn);

        // execution
        final MockWebCloudClient cloud = createWebCloudClient(findIssuesConn);
        final CountDownLatch latch = new CountDownLatch(1);
        cloud.addStatusListener(new Cloud.CloudStatusListener() {
            public void handleIssueDataDownloadedEvent() {
                latch.countDown();
            }

            public void handleStateChange(Cloud.SigninState oldState, Cloud.SigninState state) {
            }
        });
        cloud.initialize();
        assertEquals(1, latch.getCount());
        cloud.bugsPopulated();
        cloud.initiateCommunication();
        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
    }

    private CountDownLatch addStatusListenerWaiter(MockWebCloudClient cloud) {
        final CountDownLatch latch = new CountDownLatch(1);
        cloud.addStatusListener(new Cloud.CloudStatusListener() {
            public void handleIssueDataDownloadedEvent() {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }

            public void handleStateChange(Cloud.SigninState oldState, Cloud.SigninState state) {
            }
        });
        return latch;
    }
}
