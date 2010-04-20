package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.projecthosting.ProjectHostingService;
import com.google.gdata.data.IEntry;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.data.projecthosting.Status;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.BugFilingCommentHelper;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import junit.framework.TestCase;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;

import static edu.umd.cs.findbugs.cloud.appEngine.JiraBugFiler.processJiraDashboardUrl;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BugFilingTest extends TestCase {
    private AppEngineCloudClient mockCloudClient;
    private AppEngineCloudNetworkClient mockNetworkClient;
    private GoogleCodeBugFiler filer;
    private ProjectHostingService projectHostingService;
    private boolean triedAgain;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockCloudClient = mock(AppEngineCloudClient.class);
        when(mockCloudClient.getPlugin()).thenReturn(
                new CloudPlugin("BugFilingTest", null, null, null, new PropertyBundle(), null, null));
        mockNetworkClient = mock(AppEngineCloudNetworkClient.class);
        when(mockCloudClient.getNetworkClient()).thenReturn(mockNetworkClient);
        projectHostingService = mock(ProjectHostingService.class);

        triedAgain = false;
        filer = new GoogleCodeBugFiler(mockCloudClient) {
            @Override
            <E> E tryAgain(Callable<E> callable, Exception e) throws OAuthException, MalformedURLException,
                                                                     InterruptedException, AuthenticationException {
                triedAgain = true;
                try {
                    return callable.call();
                } catch (Exception e1) {
                    throw new MyRuntimeException(e1);
                }
            }
        };
        filer.setProjectHostingService(projectHostingService);
        filer.setCommentHelper(mock(BugFilingCommentHelper.class));
    }

    public void testJiraDashboardUrlProcessor() {
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("  jira.atlassian.com    "));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("http://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa;sessionId=blah"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa?blah"));
    }

    public void testGoogleCodeFileSuccess() throws Exception {
        // setup
        when(projectHostingService.insert(
                Mockito.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Mockito.<IEntry>any())).thenAnswer(new Answer<IEntry>() {
            public IEntry answer(InvocationOnMock invocationOnMock) throws Throwable {
                IssuesEntry result = new IssuesEntry();
                result.addHtmlLink("http://test.url", "en", "Test URL");
                result.setStatus(new Status("OK"));
                return result;
            }
        });

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "test");

        // verify
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    public void testGoogleCodeFileSuccessWithFullUrlForProjectName() throws Exception {
        // setup
        when(projectHostingService.insert(
                Mockito.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Mockito.<IEntry>any())).thenAnswer(new Answer<IEntry>() {
            public IEntry answer(InvocationOnMock invocationOnMock) throws Throwable {
                IssuesEntry result = new IssuesEntry();
                result.addHtmlLink("http://test.url", "en", "Test URL");
                result.setStatus(new Status("OK"));
                return result;
            }
        });

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "http://code.google.com/p/test");

        // verify
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    public void testGoogleCodeFileSuccessWithLongUrlForProjectName() throws Exception {
        // setup
        when(projectHostingService.insert(
                Mockito.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Mockito.<IEntry>any())).thenAnswer(new Answer<IEntry>() {
            public IEntry answer(InvocationOnMock invocationOnMock) throws Throwable {
                IssuesEntry result = new IssuesEntry();
                result.addHtmlLink("http://test.url", "en", "Test URL");
                result.setStatus(new Status("OK"));
                return result;
            }
        });

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "http://code.google.com/p/test/issues/list");

        // verify
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    public void testGoogleCodeFileBadUrl() throws Exception {
        BugInstance bug = new BugInstance("Blah", 2);
        try {
            filer.file(bug, "http://test!");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("Invalid Google Code project URL 'http://test!'", e.getMessage());
        }
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testGoogleCodeFileServiceDoubleException() throws Exception {
        // setup
        when(projectHostingService.insert(Mockito.<URL>any(), Mockito.<IEntry>any()))
                .thenThrow(new ServiceException("Invalid request URI"));
        IGuiCallback mockGuiCallback = mock(IGuiCallback.class);
        when(mockCloudClient.getGuiCallback()).thenReturn(mockGuiCallback);
        when(mockGuiCallback.showDocument(Mockito.<URL>any())).thenReturn(true);

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        try {
            filer.file(bug, "test");
            fail();
        } catch (IOException e) {
            assertEquals("Invalid request URI", e.getMessage());
        }

        // verify
        assertTrue(triedAgain);
    }
}
