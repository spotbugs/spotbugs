package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;
import junit.framework.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockAppEngineCloudClient extends AppEngineCloudClient {
    private List<ExpectedConnection> expectedConnections = new ArrayList<ExpectedConnection>();
    private int nextConnection = 0;

    private AppEngineNameLookup mockNameLookup;
    private Long mockSessionId = null;

    public List<String> urlsRequested;
    public IGuiCallback mockGuiCallback;
    public List<String> statusBarHistory;

    public MockAppEngineCloudClient(CloudPlugin plugin, SortedBugCollection bugs, List<HttpURLConnection> mockConnections)
            throws IOException {
        super(plugin, bugs, new Properties());

        setNetworkClient(new MockAppEngineCloudNetworkClient());

        urlsRequested = Lists.newArrayList();
        for (HttpURLConnection mockConnection : mockConnections) {
            expectedConnections.add(new ExpectedConnection().withLegacyMock(mockConnection));
        }
        mockNameLookup = createMockNameLookup();
        mockGuiCallback = mock(IGuiCallback.class);
        statusBarHistory = new ArrayList<String>();

        initStatusBarHistory();
    }

    @Override
    protected ExecutorService getBugUpdateExecutor() {
        return backgroundExecutorService;
    }

    @Override
    protected IGuiCallback getGuiCallback() {
        return mockGuiCallback;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public AppEngineCloudNetworkClient createSpyNetworkClient() throws IOException {
        AppEngineCloudNetworkClient spyNetworkClient = Mockito.spy(getNetworkClient());
        Mockito.doThrow(new IOException()).when(spyNetworkClient).signIn(true);
        setNetworkClient(spyNetworkClient);
        return spyNetworkClient;
    }

    // ========================== end of public methods =========================

    private void initStatusBarHistory() {
        addListener(new CloudListener() {
            public void issueUpdated(BugInstance bug) {
            }

            public void statusUpdated() {
                statusBarHistory.add(getStatusMsg());
            }
        });
    }

    private AppEngineNameLookup createMockNameLookup() throws IOException {
        AppEngineNameLookup mockNameLookup = mock(AppEngineNameLookup.class);
        when(mockNameLookup.getHost()).thenReturn("host");
        when(mockNameLookup.getUsername()).thenReturn("test@example.com");
        when(mockNameLookup.getSessionId()).thenAnswer(new Answer<Long>() {
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                return mockSessionId;
            }
        });
        when(mockNameLookup.initialize(Mockito.<CloudPlugin>any(), Mockito.<BugCollection>any()))
                .thenAnswer(new Answer<Boolean>() {
                    public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                        mockSessionId = 555L;
                        return true;
                    }
                });
        return mockNameLookup;
    }

    public void expectConnection(String url) {
        expectedConnections.add(new ExpectedConnection().withUrl(url));
    }

    public byte[] postedData(String url) {
        return getExpectedConnection(url).getPostData();
    }

    private ExpectedConnection getExpectedConnection(String url) {
        for (ExpectedConnection expectedConnection : expectedConnections)
            if (url.equals(expectedConnection.url()))
                return expectedConnection;
        return null;
    }

    public void verifyAllConnectionsOpened() {
        if (expectedConnections.size() != nextConnection) {
            Assert.fail("some connections were not opened\n" +
                        "opened: " + expectedConnections.subList(0, nextConnection) + "\n" +
                        "missed: " + expectedConnections.subList(nextConnection, expectedConnections.size()));
        }
    }

    public void waitUntilIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        if (!newIssuesUploaded.await(timeout, unit)) {
            Assert.fail("issues uploaded event never fired after " + timeout + " " + unit.toString());
        }
    }

    private class MockAppEngineCloudNetworkClient extends AppEngineCloudNetworkClient {
        @Override
        HttpURLConnection openConnection(String url) {
            if (nextConnection >= expectedConnections.size()) {
                Assert.fail("Cannot open " + url + " - already requested all "
                            + expectedConnections.size() + " url's: " + expectedConnections);
            }
            urlsRequested.add(url);
            ExpectedConnection connection = expectedConnections.get(nextConnection);
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
        protected AppEngineNameLookup createNameLookup() {
            return mockNameLookup;
        }
    }

    private class ExpectedConnection {
        private HttpURLConnection mockConnection;
        private String url = null;
        private int responseCode = 200;

        public ExpectedConnection() {
            mockConnection = mock(HttpURLConnection.class);
            try {
                when(mockConnection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
                when(mockConnection.getResponseCode()).thenAnswer(new Answer<Integer>() {
                    public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
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

        public @CheckForNull String url() {
            return url;
        }

        public ExpectedConnection withUrl(String url) {
            this.url = url;
            return this;
        }

        public byte[] getPostData() {
            return getOutputStream().toByteArray();
        }

        private ByteArrayOutputStream getOutputStream() {
            try {
                return (ByteArrayOutputStream) mockConnection.getOutputStream();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public String toString() {
            ByteArrayOutputStream postStream = getOutputStream();
            return "/" + url() + (postStream.size() > 0 ? " <" + postStream.size() + ">" : "");
        }
    }
}
