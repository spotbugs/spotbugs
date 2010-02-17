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
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugFilingHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.util.LaunchBrowser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.prefs.Preferences;

public class GoogleCodeBugFiler {

	private static final String FEED_URI_BASE = "http://code.google.com/feeds/issues";
	private static final String PROJECTION = "/full";

	private static final String defaultStatus = "New";
	private static final String defaultLabels = "FindBugsGenerated";

	private final Cloud cloud;

	private final BugFilingHelper bugFilingHelper;
    private @CheckForNull ProjectHostingService projectHostingService;

	private final URL issuesFeedUrl;
    private static final String KEY_PROJECTHOSTING_OAUTH_TOKEN = "projecthosting_oauth_token";
    private static final String KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET = "projecthosting_oauth_token_secret";

    public GoogleCodeBugFiler(Cloud cloud, String project) throws MalformedURLException {
        this.cloud = cloud;
		issuesFeedUrl = getIssuesFeedUrl(project);
		bugFilingHelper = new BugFilingHelper(cloud);
	}

	public IssuesEntry file(BugInstance instance)
			throws IOException, ServiceException, OAuthException, InterruptedException {

        if (projectHostingService == null)
            initProjectHostingService(false);

        try {
            return projectHostingService.insert(issuesFeedUrl, makeNewIssue(instance));
        } catch (AuthenticationException e) {
            // something failed, so maybe the OAuth token is expired
            clearAuthTokenCache();
            initProjectHostingService(true);
            try {
                return projectHostingService.insert(issuesFeedUrl, makeNewIssue(instance));
            } catch (AuthenticationException e1) {
                clearAuthTokenCache();
                throw e1;
            }
        }
    }

    private void clearAuthTokenCache() {
        Preferences prefs = getPrefs();
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN);
        prefs.remove(KEY_PROJECTHOSTING_OAUTH_TOKEN_SECRET);
    }

    // ============================= end of public methods =====================================

	private URL getIssuesFeedUrl(String proj) throws MalformedURLException {
		return new URL(FEED_URI_BASE + "/p/" + proj + "/issues" + PROJECTION);
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
        oauthParameters.setScope(FEED_URI_BASE);

        oauthHelper.getUnauthorizedRequestToken(oauthParameters);
        String requestUrl = oauthHelper.createUserAuthorizationUrl(oauthParameters);
        boolean openedUrl = LaunchBrowser.showDocument(new URL(requestUrl));
        if (!openedUrl) {
            throw new IllegalStateException("cannot launch browser");
        }

        IGuiCallback callback = cloud.getBugCollection().getProject().getGuiCallback();
        callback.showMessageDialog("Please sign into your Google Account in\n" +
                                   "your web browser, then click OK.");

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
		entry.setStatus(new Status(defaultStatus));
		for (String label : defaultLabels.split(" ")) {
			entry.addLabel(new Label(label));
		}
		entry.setSendEmail(new SendEmail("True"));

		return entry;
	}
}
