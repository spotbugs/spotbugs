package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.authn.oauth.OAuthParameters;
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
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static edu.umd.cs.findbugs.cloud.appEngine.JiraBugFiler.processJiraDashboardUrl;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BugFilingTest extends TestCase {
    private AppEngineCloudClient mockCloudClient;
    private AppEngineCloudNetworkClient mockNetworkClient;
    private GoogleCodeBugFiler filer;
    private ProjectHostingService projectHostingService;
    private boolean triedAgain;
    private GoogleOAuthHelper mockOAuthHelper;
    private IGuiCallback mockGuiCallback;
    private Preferences mockPrefs;
    private Properties props;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mockCloudClient = mock(AppEngineCloudClient.class);
        when(mockCloudClient.getPlugin()).thenReturn(
                new CloudPlugin("BugFilingTest", null, null, null, new PropertyBundle(), null, null));
        mockNetworkClient = mock(AppEngineCloudNetworkClient.class);
        when(mockCloudClient.getNetworkClient()).thenReturn(mockNetworkClient);
        projectHostingService = mock(ProjectHostingService.class);
        mockOAuthHelper = mock(GoogleOAuthHelper.class);
        mockGuiCallback = mock(IGuiCallback.class);
        when(mockCloudClient.getGuiCallback()).thenReturn(mockGuiCallback);
        mockPrefs = mock(Preferences.class);
        props = new Properties();
        createPreferencesToPropertiesBridge(mockPrefs, props);

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

            @Override
            GoogleOAuthHelper createOAuthHelper(OAuthHmacSha1Signer oauthSigner) {
                return mockOAuthHelper;
            }

            @Override
            Preferences getPrefs() {
                return mockPrefs;
            }

            @Override
            ProjectHostingService createProjectHostingService(OAuthHmacSha1Signer oauthSigner, GoogleOAuthParameters oauthParameters) throws OAuthException {
                return projectHostingService;
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
                Mockito.<IEntry>any())).thenAnswer(createIssueEntryAnswer());

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
                Mockito.<IEntry>any())).thenAnswer(createIssueEntryAnswer());

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
                Mockito.<IEntry>any())).thenAnswer(createIssueEntryAnswer());

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
    public void testGoogleCodeFileServiceAuthenticationExceptionOnFirstTry() throws Exception {
        // setup
        when(projectHostingService.insert(Mockito.<URL>any(), Mockito.<IEntry>any()))
                .thenThrow(new AuthenticationException("Not logged in")).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "test");

        // verify
        assertTrue(triedAgain);
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testGoogleCodeFileServiceDoubleException() throws Exception {
        // setup
        when(projectHostingService.insert(Mockito.<URL>any(), Mockito.<IEntry>any()))
                .thenThrow(new ServiceException("Invalid request URI"));

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

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testGoogleCodeAuthentication() throws Exception {
        // setup
        filer.setProjectHostingService(null);
        final AtomicReference<OAuthParameters> oauthParams = new AtomicReference<OAuthParameters>();
        when(mockOAuthHelper.createUserAuthorizationUrl(Mockito.<OAuthParameters>any())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                oauthParams.set((OAuthParameters) invocationOnMock.getArguments()[0]);
                return "http://auth.url";
            }
        });
        when(mockGuiCallback.showDocument(new URL("http://auth.url"))).thenReturn(true);
        when(mockOAuthHelper.getAccessToken(Mockito.<OAuthParameters>any())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                oauthParams.get().setOAuthTokenSecret("SECRET");
                return "TOKEN";
            }
        });

        // after authenticating..
        when(projectHostingService.insert(
                Mockito.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Mockito.<IEntry>any())).thenAnswer(createIssueEntryAnswer());


        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "test");

        // verify
        assertEquals("TOKEN", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN));
        assertEquals("SECRET", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET));
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockGuiCallback).showMessageDialogAndWait(
                "Please sign into your Google Account in\nyour web browser, then click OK.");
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void testGoogleCodeAuthenticationAlreadyStored() throws Exception {
        // setup
        filer.setProjectHostingService(null);
        props.setProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN, "TOKEN");
        props.setProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET, "SECRET");

        // after authenticating..
        when(projectHostingService.insert(
                Mockito.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Mockito.<IEntry>any())).thenAnswer(createIssueEntryAnswer());


        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug, "test");

        // verify
        verify(mockOAuthHelper, Mockito.never()).createUserAuthorizationUrl(Mockito.<OAuthParameters>any());
        verify(mockOAuthHelper, Mockito.never()).getUnauthorizedRequestToken(Mockito.<OAuthParameters>any());
        verify(mockGuiCallback, Mockito.never()).showMessageDialogAndWait(Mockito.anyString());

        assertEquals("TOKEN", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN));
        assertEquals("SECRET", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET));
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockNetworkClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    // =============================== end of tests ==========================

    private static void createPreferencesToPropertiesBridge(Preferences mockPrefs, final Properties props) {
        when(mockPrefs.get(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                return props.getProperty((String) args[0], (String) args[1]);
            }
        });
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                props.setProperty((String) args[0], (String) args[1]);
                return null;
            }
        }).when(mockPrefs).put(Mockito.anyString(), Mockito.anyString());
    }

    private Answer<IEntry> createIssueEntryAnswer() {
        return new Answer<IEntry>() {
            public IEntry answer(InvocationOnMock invocationOnMock) throws Throwable {
                IssuesEntry result = new IssuesEntry();
                result.addHtmlLink("http://test.url", "en", "Test URL");
                result.setStatus(new Status("OK"));
                return result;
            }
        };
    }
}
