package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;
import junit.framework.Assert;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockAppEngineCloudClient extends AppEngineCloudClient {
    public List<String> urlsRequested;
    private final Iterator<HttpURLConnection> mockConnections;
    public IGuiCallback mockGuiCallback;
    private AppEngineNameLookup mockNameLookup;
    private Long mockSessionId = null;
    public List<String> statusChanges;

    public MockAppEngineCloudClient(CloudPlugin plugin, SortedBugCollection bugs,
                                  Executor runImmediatelyExecutor, Iterator<HttpURLConnection> mockConnectionsP)
            throws IOException {
        super(plugin, bugs, new Properties(), runImmediatelyExecutor);
        this.mockConnections = mockConnectionsP;
        urlsRequested = Lists.newArrayList();
        mockNameLookup = mock(AppEngineNameLookup.class);
        when(mockNameLookup.getHost()).thenReturn("host");
        when(mockNameLookup.getUsername()).thenReturn("test@example.com");
        when(mockNameLookup.getSessionId()).thenAnswer(new Answer<Long>() {
            public Long answer(InvocationOnMock invocationOnMock) throws Throwable {
                return mockSessionId;
            }
        });
        when(mockNameLookup.initialize(Mockito.<CloudPlugin>any(), Mockito.<BugCollection>any())).thenAnswer(new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                mockSessionId = 555L;
                return true;
            }
        });


        setNetworkClient(new MockAppEngineCloudNetworkClient());
        mockGuiCallback = mock(IGuiCallback.class);
        statusChanges = new ArrayList<String>();
        addListener(new CloudListener() {
            public void issueUpdated(BugInstance bug) {
            }

            public void statusUpdated() {
                statusChanges.add(getStatusMsg());
            }
        });
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

    private class MockAppEngineCloudNetworkClient extends AppEngineCloudNetworkClient {
        @Override
        HttpURLConnection openConnection(String url) {
            System.err.println("opening " + url + " at " + Thread.currentThread().getStackTrace()[2]);
            if (!mockConnections.hasNext()) {
                Assert.fail("No mock connections left (for " + url + " - already requested URL's: " + urlsRequested + ")");
            }
            urlsRequested.add(url);
            return mockConnections.next();
        }

        @Override
        protected AppEngineNameLookup createNameLookup() {
            return mockNameLookup;
        }
    }
}
