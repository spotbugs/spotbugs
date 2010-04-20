package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.cloud.Cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AppEngineCloudCallbackTests extends AbstractAppEngineCloudTest {

    public void testWaitForIssueSyncAllFound() throws Exception {
		// set up mocks
		final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto()));
        setupResponseCodeAndOutputStream(findIssuesConn);

		// execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConn);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                doneWaiting.set(true);
            }
        }).start();
        assertFalse(doneWaiting.get());
        cloud.initialize();
        assertFalse(doneWaiting.get());
		cloud.bugsPopulated();
        Thread.sleep(10);
        assertTrue(doneWaiting.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

	public void testWaitForIssueSyncReturnsImmediatelyWhenAlreadySynced() throws Exception {
		// set up mocks
		final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto()));
        setupResponseCodeAndOutputStream(findIssuesConn);

		// execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConn);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                doneWaiting.set(true);
            }
        }).start();
        assertFalse(doneWaiting.get());
        cloud.initialize();
		cloud.bugsPopulated();
        Thread.sleep(10);
        assertTrue(doneWaiting.get());

        long start = System.currentTimeMillis();
        cloud.waitUntilIssueDataDownloaded();
        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 10) // should return immediately
            fail("was " + elapsed);
	}

	public void testWaitForIssueSyncNetworkFailure() throws Exception {
		// set up mocks
		final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(findIssuesConn.getResponseCode()).thenReturn(500);
        when(findIssuesConn.getOutputStream()).thenReturn(outputStream);

        // execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConn);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);
        new Thread(new Runnable() {
            public void run() {
                cloud.waitUntilIssueDataDownloaded();
                doneWaiting.set(true);
            }
        }).start();
        assertFalse(doneWaiting.get());
        cloud.initialize();
        assertFalse(doneWaiting.get());
        cloud.bugsPopulated();
        Thread.sleep(10);
        assertTrue(doneWaiting.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}

    /**
     * The UI updates when waitUntilIssueDataDownloaded returns, so this test
     * ensures that the caller doesn't wait longer than necessary. 
     */
	public void testWaitForIssueSyncReturnsBeforeUpload() throws Throwable {
        addMissingIssue = true;

		// set up mocks
		final HttpURLConnection findIssuesConnection = mock(HttpURLConnection.class);
        when(findIssuesConnection.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto()));
        setupResponseCodeAndOutputStream(findIssuesConnection);

		final HttpURLConnection logInConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(logInConnection);

		HttpURLConnection uploadConnection = mock(HttpURLConnection.class);
		setupResponseCodeAndOutputStream(uploadConnection);

		// execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConnection, logInConnection, uploadConnection);
        final AtomicBoolean doneWaiting = new AtomicBoolean(false);

        // ensure synchronization between threads for the test
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
        Thread.sleep(10);
        assertTrue(doneWaiting.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
        assertEquals("/log-in", cloud.urlsRequested.get(1));
        assertEquals("/upload-issues", cloud.urlsRequested.get(2));

        // make any exception thrown in the bg thread is registered as a failure
        Throwable t = bgThreadFuture.get();
        if (t != null)
            throw t;
    }

	public void testIssueDataDownloadedCallback() throws IOException {
		// set up mocks
		final HttpURLConnection findIssuesConn = mock(HttpURLConnection.class);
        when(findIssuesConn.getInputStream()).thenReturn(createFindIssuesResponse(createFoundIssueProto()));
        setupResponseCodeAndOutputStream(findIssuesConn);

		// execution
		final MockAppEngineCloudClient cloud = createAppEngineCloudClient(findIssuesConn);
        final AtomicBoolean synced = new AtomicBoolean(false);
        cloud.addStatusListener(new Cloud.CloudStatusListener() {
            public void handleIssueDataDownloadedEvent() {
                synced.set(true);
            }

            public void handleStateChange(Cloud.SigninState oldState, Cloud.SigninState state) {
            }
        });
        cloud.initialize();
        assertFalse(synced.get());
		cloud.bugsPopulated();
        assertTrue(synced.get());

        assertEquals("/find-issues", cloud.urlsRequested.get(0));
	}
}