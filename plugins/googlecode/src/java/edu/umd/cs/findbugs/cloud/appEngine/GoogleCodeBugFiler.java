package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gdata.client.authn.oauth.GoogleOAuthHelper;
import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer;
import com.google.gdata.client.projecthosting.ProjectHostingService;
import com.google.gdata.data.HtmlTextConstruct;
import com.google.gdata.data.Person;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.data.projecthosting.Label;
import com.google.gdata.data.projecthosting.Owner;
import com.google.gdata.data.projecthosting.SendEmail;
import com.google.gdata.data.projecthosting.Status;
import com.google.gdata.data.projecthosting.Username;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.BugFilingCommentHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

public class GoogleCodeBugFiler implements BugFiler {

    private static final Logger LOGGER = Logger.getLogger(GoogleCodeBugFiler.class.getName());

    static final String KEY_PROJECTHOSTING_OAUTH_TOKEN = "projecthosting_oauth_token";

    static final String KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET = "projecthosting_oauth_token_secret";

    private static final String PROJECTION = "/full";

    private static final String DEFAULT_STATUS = "New";

    private static final String DEFAULT_LABELS = "FindBugsGenerated";

    private static final Pattern PATTERN_GOOGLE_CODE_URL = Pattern
            .compile("\\s*(?:http://)?code.google.com/p/([^/\\s]*)(?:/.*)?\\s*");

    private static final Pattern URL_REGEX = Pattern.compile("http://code.google.com/p/(.*?)/issues/detail\\?id=(\\d+)");

    private Cloud cloud;
    private BugFilingCommentHelper bugFilingCommentHelper;
    private String url;

    private @CheckForNull ProjectHostingService projectHostingService;

    public GoogleCodeBugFiler(ComponentPlugin<BugFiler> plugin, Cloud cloud) {
        this.cloud = cloud;
        String url = SystemProperties.getProperty("findbugs.googlecodeURL");
        if (url == null)
            url = plugin.getProperties().getProperty("googlecodeURL");
        this.url = url;
        this.bugFilingCommentHelper = new BugFilingCommentHelper(cloud);
    }

    /** for testing */
    void setCommentHelper(BugFilingCommentHelper helper) {
        bugFilingCommentHelper = helper;
    }

    /** for testing */
    void setProjectHostingService(ProjectHostingService service) {
        projectHostingService = service;
    }

    public URL file(BugInstance b) throws IOException, SignInCancelledException {
        if (url == null) {
            url = askUserForGoogleCodeProjectName();
            if (url == null)
                return null;
        }
        Matcher m = PATTERN_GOOGLE_CODE_URL.matcher(url);
        String projectName = m.matches() ? m.group(1) : url;
        if (projectName.contains("/"))
            throw new IllegalArgumentException("Invalid Google Code project URL '" + projectName + "'");
        try {
            return fileGoogleCodeBug(b, projectName);

        } catch (ServiceException e) {
            throw newIOException(e);
        } catch (OAuthException e) {
            throw newIOException(e);
        } catch (InterruptedException e) {
            throw newIOException(e);
        }
    }

    public String getBugStatus(String bugLink) throws MalformedURLException, OAuthException, InterruptedException,
            AuthenticationException {

        Matcher m = URL_REGEX.matcher(bugLink);
        if (!m.matches()) {
            return null;
        }
        final String project = m.group(1);
        final long issueID = Long.parseLong(m.group(2));

        return initProjectHostingServiceAndExecute(new Callable<String>() {
            public String call() throws Exception {
                IssuesEntry issue = projectHostingService.getEntry(new URL("http://code.google.com/feeds/issues/p/" + project
                        + "/issues/full/" + issueID), IssuesEntry.class);
                Status status = issue.getStatus();
                if (status == null)
                    return null;
                return status.getValue();
            }
        });
    }

    // ============================= end of public methods
    // =====================================

    private IssuesEntry fileWithProject(final BugInstance instance, String project) throws IOException, ServiceException,
            OAuthException, InterruptedException {
        final URL issuesFeedUrl = getIssuesFeedUrl(project);

        if (projectHostingService == null)
            initProjectHostingService(false);

        Callable<IssuesEntry> callable = new Callable<IssuesEntry>() {
            public IssuesEntry call() throws Exception {
                return projectHostingService.insert(issuesFeedUrl, makeNewIssue(instance));
            }
        };
        return initProjectHostingServiceAndExecute(callable);
    }

    private IOException newIOException(Exception e) {
        IOException ioe = new IOException(e.getMessage());
        ioe.initCause(e);
        return ioe;
    }

    private <E> E initProjectHostingServiceAndExecute(Callable<E> callable) throws OAuthException, MalformedURLException,
            InterruptedException, AuthenticationException {
        if (projectHostingService == null)
            initProjectHostingService(false);
        try {
            return callable.call();
        } catch (AuthenticationException e) {
            return tryAgain(callable, e);
        } catch (ServiceException e) {
            return tryAgain(callable, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new MyRuntimeException(e);
        }
    }

    /** package-private for testing */
    <E> E tryAgain(Callable<E> callable, Exception e) throws OAuthException, MalformedURLException, InterruptedException,
            AuthenticationException {
        // something failed, so maybe the OAuth token is expired
        clearAuthTokenCache();
        initProjectHostingService(true);
        try {
            return callable.call();
        } catch (AuthenticationException e1) {
            clearAuthTokenCache();
            throw e1;
        } catch (Exception e1) {
            throw new MyRuntimeException(e);
        }
    }

    private void clearAuthTokenCache() {
        Preferences prefs = getPrefs();
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN);
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET);
    }

    private URL getIssuesFeedUrl(String proj) throws MalformedURLException {
        return new URL("http://code.google.com/feeds/issues/p/" + proj + "/issues" + PROJECTION);
    }

    private void initProjectHostingService(boolean forceGetNewToken) throws OAuthException, MalformedURLException,
            InterruptedException {

        OAuthHmacSha1Signer oauthSigner = new OAuthHmacSha1Signer();
        GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
        oauthParameters.setOAuthConsumerKey("findbugs.sourceforge.net");
        oauthParameters.setOAuthConsumerSecret("srA0ocqhvT4Q7UUbe1CFQDLy");

        Preferences prefs = getPrefs();
        String token = prefs.get(KEY_PROJECTHOSTING_OAUTH_TOKEN, null);
        String secret = prefs.get(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET, "");
        if (!forceGetNewToken && token != null && secret != null) {
            oauthParameters.setOAuthToken(token);
            oauthParameters.setOAuthTokenSecret(secret);
            projectHostingService = createProjectHostingService(oauthSigner, oauthParameters);
            return;
        }
        GoogleOAuthHelper oauthHelper = createOAuthHelper(oauthSigner);
        oauthParameters.setScope("http://code.google.com/feeds/issues");

        oauthHelper.getUnauthorizedRequestToken(oauthParameters);
        String requestUrl = oauthHelper.createUserAuthorizationUrl(oauthParameters);
        IGuiCallback callback = cloud.getGuiCallback();
        boolean openedUrl = callback.showDocument(new URL(requestUrl));
        if (!openedUrl) {
            throw new IllegalStateException("cannot launch browser");
        }

        callback.showMessageDialogAndWait("Please sign into your Google Account in\n" + "your web browser, then click OK.");

        // convert the request token to a session token
        token = oauthHelper.getAccessToken(oauthParameters);

        prefs.put(KEY_PROJECTHOSTING_OAUTH_TOKEN, token);
        prefs.put(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET, oauthParameters.getOAuthTokenSecret());

        projectHostingService = createProjectHostingService(oauthSigner, oauthParameters);
    }

    /** package-private for testing */
    ProjectHostingService createProjectHostingService(OAuthHmacSha1Signer oauthSigner, GoogleOAuthParameters oauthParameters)
            throws OAuthException {
        ProjectHostingService projectHostingService = new ProjectHostingService("findbugs-cloud-client");
        projectHostingService.setOAuthCredentials(oauthParameters, oauthSigner);
        return projectHostingService;
    }

    /** package-private for testing */
    GoogleOAuthHelper createOAuthHelper(OAuthHmacSha1Signer oauthSigner) {
        return new GoogleOAuthHelper(oauthSigner);
    }

    /** package-private for testing */
    Preferences getPrefs() {
        return Preferences.userNodeForPackage(GoogleCodeBugFiler.class);
    }

    private IssuesEntry makeNewIssue(BugInstance bug) {
        Person author = new Person();
        author.setName(cloud.getUser());

        Owner owner = new Owner();
        owner.setUsername(new Username(cloud.getUser()));

        IssuesEntry entry = new IssuesEntry();
        entry.getAuthors().add(author);
        entry.setTitle(new PlainTextConstruct(bugFilingCommentHelper.getBugReportSummary(bug)));
        entry.setContent(new HtmlTextConstruct(bugFilingCommentHelper.getBugReportText(bug)));
        entry.setStatus(new Status(DEFAULT_STATUS));
        for (String label : DEFAULT_LABELS.split(" ")) {
            entry.addLabel(new Label(label));
        }
        entry.setSendEmail(new SendEmail("True"));

        return entry;
    }

    private URL fileGoogleCodeBug(BugInstance b, String projectName) throws IOException, ServiceException, OAuthException,
            InterruptedException, SignInCancelledException {
        IssuesEntry googleCodeIssue;
        try {
            googleCodeIssue = fileWithProject(b, projectName);
        } catch (MyRuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException)
                throw (RuntimeException) cause;
            if (cause instanceof IOException)
                throw (IOException) cause;
            if (cause instanceof OAuthException)
                throw (OAuthException) cause;
            if (cause instanceof ServiceException)
                throw (ServiceException) cause;
            throw e;
        }
        if (googleCodeIssue == null)
            return null;

        String viewUrl = googleCodeIssue.getHtmlLink().getHref();
        if (viewUrl == null) {
            LOGGER.warning("Filed issue on Google Code, but URL is missing!");
            return null;
        }

        cloud.updateBugStatusCache(b, googleCodeIssue.getStatus().getValue());

        cloud.setBugLinkOnCloudAndStoreIssueDetails(b, viewUrl, "GOOGLE_CODE");

        return new URL(viewUrl);
    }

    private String askUserForGoogleCodeProjectName() {
        IGuiCallback guiCallback = cloud.getGuiCallback();
        Preferences prefs = getPrefs();

        String lastProject = prefs.get("last_google_code_project", "");
        String projectName = guiCallback.showQuestionDialog("Issue will be filed at Google Code.\n\n"
                + "Google Code project name:"
//              + "(pass -Dfindbugs.trackerURL=myProjectName to avoid this dialog in the future)\n"
                ,
                "Google Code Issue Tracker", lastProject);
        if (projectName == null || projectName.trim().length() == 0) {
            return null;
        }
        prefs.put("last_google_code_project", projectName);
        return projectName;
    }

    protected class MyRuntimeException extends RuntimeException {
        public MyRuntimeException(Throwable cause) {
            super(cause);
        }
    }

    public boolean ready() {
        return true;
    }
}
