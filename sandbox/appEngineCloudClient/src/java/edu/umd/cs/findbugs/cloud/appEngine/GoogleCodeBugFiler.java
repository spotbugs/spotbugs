package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
import com.google.gdata.util.ServiceException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.BugFilingHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.util.LaunchBrowser;

public class GoogleCodeBugFiler {

	private static final String FEED_URI_BASE = "http://code.google.com/feeds/issues";
	private static final String PROJECTION = "/full";

	private Cloud cloud;
	private String issuesBaseUri;
	private URL issuesFeedUrl;

	private String defaultStatus = "New";
	private String defaultLabels = "FindBugsGenerated";
	private BugFilingHelper bugFilingHelper;

	public GoogleCodeBugFiler(Cloud cloud, String project) throws MalformedURLException {
		issuesBaseUri = FEED_URI_BASE + "/p/" + project + "/issues";
		issuesFeedUrl = makeIssuesFeedUrl(project);
		bugFilingHelper = new BugFilingHelper(cloud);
	}

	protected URL makeIssuesFeedUrl(String proj) throws MalformedURLException {
		return new URL(FEED_URI_BASE + "/p/" + proj + "/issues" + PROJECTION);
	}

	public String file(BugInstance instance)
			throws IOException, ServiceException, OAuthException, InterruptedException {

	    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
	    oauthParameters.setOAuthConsumerKey("kano.net");
	    oauthParameters.setOAuthConsumerSecret("b4EYCteCkyYvdpuIACmeHZK/");
		OAuthHmacSha1Signer oauthSigner = new OAuthHmacSha1Signer();
		GoogleOAuthHelper oauthHelper = new GoogleOAuthHelper(oauthSigner);
		oauthParameters.setScope(FEED_URI_BASE);

	    oauthHelper.getUnauthorizedRequestToken(oauthParameters);
	    String requestUrl = oauthHelper.createUserAuthorizationUrl(oauthParameters);
	    boolean openedUrl = LaunchBrowser.showDocument(new URL(requestUrl));
	    if (!openedUrl) {
	    	return null;
	    }

	    System.out.println("waiting 10 seconds...");
	    Thread.sleep(10*1000);

	    // convert the request token to a session token
	    oauthHelper.getAccessToken(oauthParameters);

		ProjectHostingService service = new ProjectHostingService("findbugs-cloud-client");
		service.setOAuthCredentials(oauthParameters, oauthSigner);

		IssuesEntry issueInserted = service.insert(issuesFeedUrl, makeNewIssue(instance));
		return issueInserted.getEditLink().getHref();
	}

	 protected URL makeIssueEntryUrl(String issueId)
	     throws MalformedURLException {
	   return new URL(issuesBaseUri + PROJECTION + "/" + issueId);
	 }

	protected IssuesEntry makeNewIssue(BugInstance bug) {
		Person author = new Person();
		author.setName(cloud.getUser());

		Owner owner = new Owner();
		owner.setUsername(new Username(cloud.getUser()));

		IssuesEntry entry = new IssuesEntry();
		entry.getAuthors().add(author);
		String messageWithPriorityTypeAbbreviation = bug.getMessageWithPriorityTypeAbbreviation();
		entry.setTitle(new PlainTextConstruct(messageWithPriorityTypeAbbreviation));
		entry.setContent(new HtmlTextConstruct(bugFilingHelper.getBugReport(bug)));
		entry.setStatus(new Status(defaultStatus));
		for (String label : defaultLabels.split(" ")) {
			entry.addLabel(new Label(label));
		}
		entry.setSendEmail(new SendEmail("True"));

		return entry;
	}
}
