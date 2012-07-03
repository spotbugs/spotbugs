package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import junit.framework.Assert;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.protobuf.GeneratedMessage;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;

class MockWebCloudClient extends WebCloudClient {
    
    private static final Logger LOGGER = Logger.getLogger(MockWebCloudClient.class.getPackage().getName());

    private List<ExpectedConnection> expectedConnections = new ArrayList<ExpectedConnection>();

    private int nextConnection = 0;

    private WebCloudNameLookup mockNameLookup;

    private Long mockSessionId = null;

    public List<String> urlsRequested;

    public IGuiCallback mockGuiCallback;

    public List<String> statusMsgHistory = new CopyOnWriteArrayList<String>();

    private final Object statusMsgLock = new Object();

    public MockWebCloudClient(CloudPlugin plugin, SortedBugCollection bugs, List<HttpURLConnection> mockConnections)
            throws IOException {
        super(plugin, bugs, new Properties());

        setNetworkClient(new MockWebCloudNetworkClient());

        urlsRequested = new ArrayList<String>();
        for (HttpURLConnection mockConnection : mockConnections) {
            expectedConnections.add(new ExpectedConnection().withLegacyMock(mockConnection));
        }
        mockNameLookup = createMockNameLookup();
        mockGuiCallback = Mockito.mock(IGuiCallback.class);
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Runnable r = (Runnable) args[0];
                r.run();
                return null;
            }
        }).when(mockGuiCallback).invokeInGUIThread(Matchers.isA(Runnable.class));

        initStatusBarHistory();
        initialized = true;
    }

    private ConcurrentLinkedQueue<Throwable> backgroundExceptions = new ConcurrentLinkedQueue<Throwable>();
    
    protected UncaughtExceptionHandler getuUncaughtBackgroundExceptionHandler() {
        return new UncaughtExceptionHandler() {
            public void uncaughtException(Thread arg0, Throwable arg1) {
                backgroundExceptions.add(arg1);
                LOGGER.log(Level.WARNING, "background exeception in " + arg0.getName(), arg1);
            }    
        };
    }
    
    public void throwBackgroundException() throws Exception {
        Throwable t = backgroundExceptions.poll();
        if (t == null) 
            return;
        if (t instanceof Exception) throw (Exception) t;
        if (t instanceof Error) throw (Error) t;
        AssertionError ae = new AssertionError("Weird throwable");
        ae.initCause(t);
        throw ae;
    }
    
    public void awaitBackgroundTasks() throws InterruptedException {
        backgroundExecutorService.awaitTermination(3, TimeUnit.SECONDS);
        backgroundExecutorService.shutdown();
        backgroundExecutorService.awaitTermination(5, TimeUnit.SECONDS);
        
    }
    @Override
    public String getCloudName() {
        return "FindBugs Cloud";
    }

    @Override
    protected ExecutorService getBugUpdateExecutor() {
        return backgroundExecutorService;
    }

    @Override
    public IGuiCallback getGuiCallback() {
        return mockGuiCallback;
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public WebCloudNetworkClient createSpyNetworkClient() throws IOException {
        WebCloudNetworkClient spyNetworkClient = Mockito.spy(getNetworkClient());
        Mockito.doThrow(new IOException()).when(spyNetworkClient).signIn(true);
        setNetworkClient(spyNetworkClient);
        return spyNetworkClient;
    }

    @Override
    public void setSigninState(SigninState state) {
        super.setSigninState(state);
    }

    public ExpectedConnection expectConnection(String url) {
        ExpectedConnection connection = new ExpectedConnection();
        expectedConnections.add(connection.withUrl(url));
        return connection;
    }

    /**
     * Returns POST data submitted for the given URL. If the URL was expected &
     * requested more than once, this will return only the data from the LATEST
     * one.
     */
    public byte[] postedData(String url) throws IOException {
        return getLatestExpectedConnection(url).getPostData();
    }

    public void verifyConnections() {
        if (expectedConnections.size() != nextConnection) {
            Assert.fail("some connections were not opened\n" + "opened: " + expectedConnections.subList(0, nextConnection) + "\n"
                    + "missed: " + expectedConnections.subList(nextConnection, expectedConnections.size()));
        }
    }

    public void waitUntilIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        if (!newIssuesUploaded.await(timeout, unit)) {
            Assert.fail("issues uploaded event never fired after " + timeout + " " + unit.toString());
        }
    }

    /**
     * Returns a {@link CountDownLatch} that waits for a IGuiCallback
     * showMessageDialog call with a message matching the given regex.
     */
    public CountDownLatch getDialogLatch(final String dialogRegex) {
        final CountDownLatch latch = new CountDownLatch(1);
        Mockito.doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                String message = (String) invocationOnMock.getArguments()[0];
                boolean match = Pattern.compile(dialogRegex).matcher(message).find();
                if (match)
                    latch.countDown();
                return null;
            }
        }).when(mockGuiCallback).showMessageDialog(Matchers.anyString());
        return latch;
    }

    public void clickYes(String regex) {
        Mockito.when(mockGuiCallback.showConfirmDialog(Matchers.matches(regex), Matchers.anyString(), Matchers.anyString(), Matchers.anyString())).thenReturn(
                IGuiCallback.YES_OPTION);
    }

    // ========================== end of public methods
    // =========================

    private void initStatusBarHistory() {
        addListener(new CloudListener() {
            public void issueUpdated(BugInstance bug) {
            }

            public void statusUpdated() {
                String statusMsg = getStatusMsg();

                if (!statusMsgHistory.isEmpty()) {
                    String last = statusMsgHistory.get(statusMsgHistory.size() - 1);
                    if (statusMsg.equals(last))
                        return;
                }
                statusMsgHistory.add(statusMsg);
                synchronized (statusMsgLock) {
                    statusMsgLock.notifyAll();
                }
            }

            public void taskStarted(CloudTask task) {
            }
        });
    }

    private WebCloudNameLookup createMockNameLookup() throws IOException {
        WebCloudNameLookup mockNameLookup = Mockito.mock(WebCloudNameLookup.class);
        Mockito.when(mockNameLookup.getHost()).thenReturn("host");
        Mockito.when(mockNameLookup.getUsername()).thenReturn("test@example.com");
        Mockito.when(mockNameLookup.getSessionId()).thenAnswer(new Answer<Long>() {
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                return mockSessionId;
            }
        });
        Mockito.when(mockNameLookup.signIn(Matchers.<CloudPlugin> any(), Matchers.<BugCollection> any())).thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                mockSessionId = 555L;
                return true;
            }
        });
        return mockNameLookup;
    }

    private ExpectedConnection getLatestExpectedConnection(String url) {
        for (int i = expectedConnections.size() - 1; i >= 0; i--) {
            ExpectedConnection expectedConnection = expectedConnections.get(i);
            if (url.equals(expectedConnection.url()))
                return expectedConnection;
        }
        return null;
    }

    public void checkStatusBarHistory(String... expectedStatusLines) {
        Assert.assertEquals(Arrays.asList(expectedStatusLines), statusMsgHistory);
    }

    public void waitForStatusMsg(String regex) throws InterruptedException {
        Pattern pattern = Pattern.compile(regex);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 15 * 1000) {
            synchronized (statusMsgLock) {
                statusMsgLock.wait(1000);
                for (String status : statusMsgHistory) {
                    if (pattern.matcher(status).matches())
                        return;
                }
            }
        }
        if (statusMsgHistory.isEmpty())
            Assert.fail("Expected " + regex + ", didn't see any status messages");
        Assert.fail("Did not see status message " + regex + " in:\n" + statusMsgHistory);
    }

    private class MockWebCloudNetworkClient extends WebCloudNetworkClient {
        @Override
        HttpURLConnection openConnection(String url) {
            ExpectedConnection connection = null;
            if (nextConnection >= expectedConnections.size()) {
                ExpectedConnection lastConnection = expectedConnections.get(expectedConnections.size() - 1);
                if (lastConnection.isRepeatIndefinitely())
                    connection = lastConnection;
                else
                    Assert.fail("Cannot open " + url + " - already requested all " + expectedConnections.size() + " url's: "
                            + expectedConnections);
            }
            urlsRequested.add(url);
            if (connection == null)
                connection = expectedConnections.get(nextConnection);
            nextConnection++;
            String expectedUrl = connection.url();
            if (expectedUrl != null) {
                expectedUrl = "/" + expectedUrl;
                if (!expectedUrl.equals(url)) {
                    Assert.fail("Expected '" + expectedUrl + "' but '" + url + "' was requested");
                }
            }
            System.err.println("opening " + url + " at " + Thread.currentThread().getStackTrace()[2]);
            return connection.mockConnection;
        }

        @Override
        protected WebCloudNameLookup createNameLookup() {
            return mockNameLookup;
        }
    }

    public class ExpectedConnection {
        private HttpURLConnection mockConnection;

        private String url = null;

        private int responseCode = 200;

        private InputStream responseStream;

        private IOException networkError = null;

        private ByteArrayOutputStream postDataStream;

        private CountDownLatch latch = new CountDownLatch(1);
        private boolean repeatIndefinitely = false;

        public ExpectedConnection() {
            mockConnection = Mockito.mock(HttpURLConnection.class);
            postDataStream = new ByteArrayOutputStream();
            try {
                Mockito.when(mockConnection.getOutputStream()).thenAnswer(new Answer<Object>() {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        if (networkError != null)
                            throw networkError;
                        return postDataStream;
                    }
                });
                Mockito.when(mockConnection.getInputStream()).thenAnswer(new Answer<InputStream>() {
                    public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        return responseStream;
                    }
                });
                Mockito.when(mockConnection.getResponseCode()).thenAnswer(new Answer<Integer>() {
                    public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                        latch.countDown();
                        return responseCode;
                    }
                });
            } catch (IOException e) {
            }
        }

        public ExpectedConnection withLegacyMock(HttpURLConnection mockConnection) {
            this.mockConnection = mockConnection;
            return this;
        }

        public @CheckForNull
        String url() {
            return url;
        }

        public ExpectedConnection withUrl(String url) {
            this.url = url;
            return this;
        }

        public ExpectedConnection withResponse(GeneratedMessage response) {
            if (responseStream != null)
                throw new IllegalStateException("Already have response stream");
            responseStream = new ByteArrayInputStream(response.toByteArray());
            return this;
        }

        public byte[] getPostData() throws IOException {
            return getOutputStream().toByteArray();
        }

        public ExpectedConnection withErrorCode(int code) {
            this.responseCode = code;
            return this;
        }

        public void throwsNetworkError(IOException e) {
            networkError = e;
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        public void repeatIndefinitely() {
            this.repeatIndefinitely = true;
        }

        public boolean isRepeatIndefinitely() {
            return repeatIndefinitely;
        }

        @Override
        public String toString() {
            ByteArrayOutputStream postStream;
            try {
                postStream = getOutputStream();
            } catch (IOException e) {
               return e.toString();
            }
            return "/" + url() + (postStream.size() > 0 ? " <" + postStream.size() + ">" : "");
        }

        // ====================== end of public methods =======================

        private ByteArrayOutputStream getOutputStream() throws IOException {
                return (ByteArrayOutputStream) mockConnection.getOutputStream();
        }
    }
}
