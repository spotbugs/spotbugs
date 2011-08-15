package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

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
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.BugFilingCommentHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.CloudPluginBuilder;
import junit.framework.TestCase;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleCodeBugFilingTest extends TestCase {
    private Cloud mockCloudClient;

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
        mockCloudClient = mock(AbstractCloud.class);
        when(mockCloudClient.getPlugin()).thenReturn(
                new CloudPluginBuilder().setCloudid("GoogleCodeBugFilingTest").setClassLoader(null).setCloudClass(null)
                        .setUsernameClass(null).setProperties(new PropertyBundle()).setDescription(null).setDetails(null)
                        .createCloudPlugin());
        projectHostingService = mock(ProjectHostingService.class);
        mockOAuthHelper = mock(GoogleOAuthHelper.class);
        mockGuiCallback = mock(IGuiCallback.class);
        when(mockCloudClient.getGuiCallback()).thenReturn(mockGuiCallback);
        mockPrefs = mock(Preferences.class);
        props = new Properties();
        createPreferencesToPropertiesBridge(mockPrefs, props);

        triedAgain = false;
        // filer.init(mockCloudClient, "http://code.google.com/p/test/");

        PropertyBundle componentProps = new PropertyBundle();
        componentProps.setProperty("googlecodeURL", "http://code.google.com/p/test/");
        ComponentPlugin<BugFiler> componentPlugin = new ComponentPlugin<BugFiler>(
                null, "x.y", null, null, componentProps, true, null, null);
        filer = new GoogleCodeBugFiler(componentPlugin, mockCloudClient) {
            @Override
            <E> E tryAgain(Callable<E> callable, Exception e) throws OAuthException, MalformedURLException, InterruptedException,
                    AuthenticationException {
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
            ProjectHostingService createProjectHostingService(OAuthHmacSha1Signer oauthSigner,
                    GoogleOAuthParameters oauthParameters) throws OAuthException {
                return projectHostingService;
            }
        };
        filer.setProjectHostingService(projectHostingService);
        filer.setCommentHelper(mock(BugFilingCommentHelper.class));
    }

    public void testGoogleCodeFileSuccess() throws Exception {
        // setup
        when(projectHostingService.insert(Matchers.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Matchers.<IEntry>any())).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        assertNotNull(url);
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    public void testGoogleCodeFileSuccessWithFullUrlForProjectName() throws Exception {
        // setup
        when(projectHostingService.insert(Matchers.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                        Matchers.<IEntry> any())).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    public void testGoogleCodeFileSuccessWithLongUrlForProjectName() throws Exception {
        // setup
        when(projectHostingService.insert(Matchers.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                Matchers.<IEntry>any())).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testGoogleCodeFileServiceAuthenticationExceptionOnFirstTry() throws Exception {
        // setup
        when(projectHostingService.insert(Matchers.<URL> any(), Matchers.<IEntry> any())).thenThrow(
                new AuthenticationException("Not logged in")).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        assertTrue(triedAgain);
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testGoogleCodeFileServiceDoubleException() throws Exception {
        // setup
        when(projectHostingService.insert(Matchers.<URL> any(), Matchers.<IEntry> any())).thenThrow(
                new ServiceException("Invalid request URI"));

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        try {
            filer.file(bug);
            fail();
        } catch (IOException e) {
            assertEquals("Invalid request URI", e.getMessage());
        }

        // verify
        assertTrue(triedAgain);
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testGoogleCodeAuthentication() throws Exception {
        // setup
        filer.setProjectHostingService(null);
        final AtomicReference<OAuthParameters> oauthParams = new AtomicReference<OAuthParameters>();
        when(mockOAuthHelper.createUserAuthorizationUrl(Matchers.<OAuthParameters> any())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                oauthParams.set((OAuthParameters) invocationOnMock.getArguments()[0]);
                return "http://auth.url";
            }
        });
        when(mockGuiCallback.showDocument(new URL("http://auth.url"))).thenReturn(true);
        when(mockOAuthHelper.getAccessToken(Matchers.<OAuthParameters> any())).thenAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                oauthParams.get().setOAuthTokenSecret("SECRET");
                return "TOKEN";
            }
        });

        // after authenticating..
        when(
                projectHostingService.insert(Matchers.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                        Matchers.<IEntry> any())).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        assertEquals("TOKEN", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN));
        assertEquals("SECRET", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET));
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockGuiCallback).showMessageDialogAndWait(
                "Please sign into your Google Account in\nyour web browser, then click OK.");
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    public void testGoogleCodeAuthenticationAlreadyStored() throws Exception {
        // setup
        filer.setProjectHostingService(null);
        props.setProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN, "TOKEN");
        props.setProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET, "SECRET");

        // after authenticating..
        when(
                projectHostingService.insert(Matchers.eq(new URL("http://code.google.com/feeds/issues/p/test/issues/full")),
                        Matchers.<IEntry> any())).thenAnswer(createIssueEntryAnswer());

        // execute
        BugInstance bug = new BugInstance("Blah", 2);
        URL url = filer.file(bug);

        // verify
        verify(mockOAuthHelper, Mockito.never()).createUserAuthorizationUrl(Matchers.<OAuthParameters> any());
        verify(mockOAuthHelper, Mockito.never()).getUnauthorizedRequestToken(Matchers.<OAuthParameters> any());
        verify(mockGuiCallback, Mockito.never()).showMessageDialogAndWait(Matchers.anyString());

        assertEquals("TOKEN", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN));
        assertEquals("SECRET", props.getProperty(GoogleCodeBugFiler.KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET));
        assertEquals("http://test.url", url.toExternalForm());
        verify(mockCloudClient).updateBugStatusCache(bug, "OK");
        verify(mockCloudClient).setBugLinkOnCloudAndStoreIssueDetails(bug, "http://test.url", "GOOGLE_CODE");
    }

    // =============================== end of tests ==========================

    private static void createPreferencesToPropertiesBridge(Preferences mockPrefs, final Properties props) {
        when(mockPrefs.get(Matchers.anyString(), Matchers.anyString())).thenAnswer(new Answer<String>() {
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
        }).when(mockPrefs).put(Matchers.anyString(), Matchers.anyString());
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
