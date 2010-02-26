package edu.umd.cs.findbugs.cloud.appEngine;

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
import org.apache.commons.discovery.log.SimpleLog;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugFilingHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.util.LaunchBrowser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleCodeBugFiler implements BugFiler {

    private static final Logger LOGGER = Logger.getLogger(GoogleCodeBugFiler.class.getName());

    private static final String KEY_PROJECTHOSTING_OAUTH_TOKEN = "projecthosting_oauth_token";
    private static final String KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET = "projecthosting_oauth_token_secret";

    private static final String PROJECTION = "/full";

	private static final String DEFAULT_STATUS = "New";
	private static final String DEFAULT_LABELS = "FindBugsGenerated";

    private static final Pattern URL_REGEX = Pattern.compile("http://code.google.com/p/(.*?)/issues/detail\\?id=(\\d+)");

	private final Cloud cloud;
	private final BugFilingHelper bugFilingHelper;

    private @CheckForNull ProjectHostingService projectHostingService;

    public GoogleCodeBugFiler(Cloud cloud) {
        this.cloud = cloud;
		bugFilingHelper = new BugFilingHelper(cloud);
	}

	public IssuesEntry file(final BugInstance instance, String project)
			throws IOException, ServiceException, OAuthException, InterruptedException {
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

    public String getBugStatus(String bugLink)
            throws MalformedURLException, OAuthException, InterruptedException, AuthenticationException {
        
        Matcher m = URL_REGEX.matcher(bugLink);
        if (!m.matches()) {
            return null;
        }
        final String project = m.group(1);
        final long issueID = Long.parseLong(m.group(2));

        return initProjectHostingServiceAndExecute(new Callable<String>() {
            public String call() throws Exception {
                IssuesEntry issue = projectHostingService.getEntry(
                        new URL("http://code.google.com/feeds/issues/p/" + project + "/issues/full/" + issueID),
                        IssuesEntry.class);
                Status status = issue.getStatus();
                if (status == null)
                    return null;
                return status.getValue();
            }
        });
    }

    // ============================= end of public methods =====================================

    private <E> E initProjectHostingServiceAndExecute(Callable<E> callable)
            throws OAuthException, MalformedURLException, InterruptedException, AuthenticationException {
        if (projectHostingService == null)
            initProjectHostingService(false);
        try {
            return callable.call();
        } catch (AuthenticationException e) {
            // something failed, so maybe the OAuth token is expired
            clearAuthTokenCache();
            initProjectHostingService(true);
            try {
                return callable.call();
            } catch (AuthenticationException e1) {
                clearAuthTokenCache();
                throw e1;
            } catch (Exception e1) {
                throw new IllegalStateException(e);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void clearAuthTokenCache() {
        Preferences prefs = getPrefs();
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN);
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET);
    }

	private URL getIssuesFeedUrl(String proj) throws MalformedURLException {
		return new URL("http://code.google.com/feeds/issues" + "/p/" + proj + "/issues" + PROJECTION);
	}

    private void initProjectHostingService(boolean forceGetNewToken)
            throws OAuthException, MalformedURLException, InterruptedException {

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
            projectHostingService = new ProjectHostingService("findbugs-cloud-client");
            projectHostingService.setOAuthCredentials(oauthParameters, oauthSigner);
            return;
        }
        GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(oauthSigner);
        oauthParameters.setScope("http://code.google.com/feeds/issues");

        oauthHelper.getUnauthorizedRequestToken(oauthParameters);
        String requestUrl = oauthHelper.createUserAuthorizationUrl(oauthParameters);
        boolean openedUrl = LaunchBrowser.showDocument(new URL(requestUrl));
        if (!openedUrl) {
            throw new IllegalStateException("cannot launch browser");
        }

        IGuiCallback callback = cloud.getBugCollection().getProject().getGuiCallback();
        try {
            callback.showMessageDialogAndWait("Please sign into your Google Account in\n" +
                                       "your web browser, then click OK.");
        } catch (InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "", e);
        }

        // convert the request token to a session token
        token = oauthHelper.getAccessToken(oauthParameters);

        prefs.put(KEY_PROJECTHOSTING_OAUTH_TOKEN, token);
        prefs.put(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET, oauthParameters.getOAuthTokenSecret());

        projectHostingService = new ProjectHostingService("findbugs-cloud-client");
        projectHostingService.setOAuthCredentials(oauthParameters, oauthSigner);
    }

    private Preferences getPrefs() {
        return Preferences.userNodeForPackage(GoogleCodeBugFiler.class);
    }

    private IssuesEntry makeNewIssue(BugInstance bug) {
		Person author = new Person();
		author.setName(cloud.getUser());

		Owner owner = new Owner();
		owner.setUsername(new Username(cloud.getUser()));

		IssuesEntry entry = new IssuesEntry();
		entry.getAuthors().add(author);
        entry.setTitle(new PlainTextConstruct(bugFilingHelper.getBugReportSummary(bug)));
		entry.setContent(new HtmlTextConstruct(bugFilingHelper.getBugReportText(bug)));
		entry.setStatus(new Status(DEFAULT_STATUS));
		for (String label : DEFAULT_LABELS.split(" ")) {
			entry.addLabel(new Label(label));
		}
		entry.setSendEmail(new SendEmail("True"));

		return entry;
	}
}
