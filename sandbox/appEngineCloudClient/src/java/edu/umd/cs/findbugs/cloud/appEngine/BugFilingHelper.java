package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.util.ServiceException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;

public class BugFilingHelper {
    private static final Logger LOGGER = Logger.getLogger(BugFilingHelper.class.getName());
    private static final Pattern PATTERN_GOOGLE_CODE_URL = Pattern.compile(
            "\\s*(?:http://)?code.google.com/p/([^/\\s]*)(?:/.*)?\\s*");

    private final AppEngineCloudClient appEngineCloudClient;
    private PropertyBundle properties;
    private GoogleCodeBugFiler googleCodeBugFiler;
    private JiraBugFiler jiraBugFiler;

    public BugFilingHelper(AppEngineCloudClient appEngineCloudClient, PropertyBundle properties) {
        this.appEngineCloudClient = appEngineCloudClient;
        this.properties = properties;
        googleCodeBugFiler = new GoogleCodeBugFiler(appEngineCloudClient);
        jiraBugFiler = new JiraBugFiler(appEngineCloudClient);
    }

    public String lookupBugStatus(final BugInstance b) {
        final String hash = b.getInstanceHash();
        String status;
        ProtoClasses.Issue issue = appEngineCloudClient.getNetworkClient().getIssueByHash(hash);
        String linkType = "GOOGLE_CODE"; // default to googlecode
        if (issue.hasBugLinkTypeStr())
            linkType = issue.getBugLinkTypeStr();

        final BugFiler bugFiler;
        if (linkType.equals("GOOGLE_CODE"))
            bugFiler = googleCodeBugFiler;
        else if (linkType.equals("JIRA"))
            bugFiler = jiraBugFiler;
        else
            return "<unknown>";

        final String bugLink = issue.getBugLink();
        appEngineCloudClient.getBackgroundExecutor().execute(new Runnable() {
            public void run() {
                String status = null;
                try {
                    status = bugFiler.getBugStatus(bugLink);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while connecting to bug tracker", e);
                }
                if (status == null)
                    status = "<unknown>";
                appEngineCloudClient.updateBugStatusCache(b, status);
            }
        });
        status = "<loading...>";
        appEngineCloudClient.updateBugStatusCache(b, status);
        return status;
    }

    public URL fileBug(BugInstance b, String bugLinkType)
            throws javax.xml.rpc.ServiceException, IOException, SignInCancelledException, OAuthException,
                   InterruptedException, ServiceException {
        String trackerUrl = properties.getProperty("cloud.bugTrackerUrl");
        if (trackerUrl != null && trackerUrl.trim().length() == 0)
            trackerUrl = null;

        if ("GOOGLE_CODE".equals(bugLinkType)) {
            if (trackerUrl == null)
                trackerUrl = askUserForGoogleCodeProjectName();
            if (trackerUrl == null)
                return null;
            Matcher m = PATTERN_GOOGLE_CODE_URL.matcher(trackerUrl);
            String projectName = m.matches() ? m.group(1) : trackerUrl;
            return fileGoogleCodeBug(b, projectName);

        } else if ("JIRA".equals(bugLinkType)) {
            if (trackerUrl == null)
                trackerUrl = askUserForJiraUrl();
            if (trackerUrl == null)
                return null;
            trackerUrl = processJiraDashboardUrl(trackerUrl);

            IGuiCallback callback = appEngineCloudClient.getGuiCallback();
            List<String> issueTypes = jiraBugFiler.getIssueTypes(trackerUrl);
            if (issueTypes == null)
                return null;
            List<String> result = callback.showForm("", "File a bug on JIRA", Arrays.asList(
                    new IGuiCallback.FormItem("Project", null, jiraBugFiler.getProjectKeys(trackerUrl)),
                    new IGuiCallback.FormItem("Component"),
                    new IGuiCallback.FormItem("Type", null, issueTypes)));
            if (result == null)
                return null; // user cancelled
            RemoteIssue issue = jiraBugFiler.fileBug(trackerUrl, b, result.get(0), result.get(1), result.get(2));
            String bugUrl = trackerUrl + "/browse/" + issue.getKey();
            appEngineCloudClient.getNetworkClient().setBugLinkOnCloudAndStoreIssueDetails(b, bugUrl, "JIRA");
            return new URL(bugUrl);

        } else {
            throw new IllegalArgumentException("Unknown issue tracker " + bugLinkType);
        }
    }

    private URL fileGoogleCodeBug(BugInstance b, String projectName)
            throws IOException, ServiceException, OAuthException, InterruptedException, SignInCancelledException {
        IssuesEntry googleCodeIssue = googleCodeBugFiler.file(b, projectName);
        if (googleCodeIssue == null)
            return null;

        String viewUrl = googleCodeIssue.getHtmlLink().getHref();
        if (viewUrl == null) {
            LOGGER.warning("Filed issue on Google Code, but URL is missing!");
            return null;
        }

        appEngineCloudClient.updateBugStatusCache(b, googleCodeIssue.getStatus().getValue());

        appEngineCloudClient.getNetworkClient().setBugLinkOnCloudAndStoreIssueDetails(
                b, viewUrl, "GOOGLE_CODE");

        return new URL(viewUrl);
    }

    private String askUserForJiraUrl() {
        IGuiCallback guiCallback = appEngineCloudClient.getBugCollection().getProject().getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(AppEngineCloudClient.class);

        String lastProject = prefs.get("last_jira_url", "");
        String dashboardUrl = guiCallback.showQuestionDialog(
                "Issue will be filed in JIRA.\n" +
                "\n" +
                "Type your project's JIRA dashboard URL below.\n" +
                "(ex. http://jira.atlassian.com/secure/Dashboard.jspa)", "JIRA",
                lastProject);
        if (dashboardUrl == null || dashboardUrl.trim().length() == 0) {
            return null;
        }
        dashboardUrl = processJiraDashboardUrl(dashboardUrl);
        prefs.put("last_jira_url", dashboardUrl);
        return dashboardUrl;
    }

    /**
     * package-private for testing
     */
    static String processJiraDashboardUrl(String dashboardUrl) {
        dashboardUrl = dashboardUrl.trim();
        Matcher m = Pattern.compile("(?:https?://)?(.*?)(?:/secure(?:/Dashboard.jspa)?.*)?").matcher(dashboardUrl);
        if (m.matches()) {
            dashboardUrl = "http://" + m.group(1);
        }
        return dashboardUrl;
    }

    private String askUserForGoogleCodeProjectName() {
        IGuiCallback guiCallback = appEngineCloudClient.getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(AppEngineCloudClient.class);

        String lastProject = prefs.get("last_google_code_project", "");
        String projectName = guiCallback.showQuestionDialog(
                "Issue will be filed at Google Code.\n" +
                "\n" +
                "Type your Google Code project name:", "Google Code Issue Tracker",
                lastProject);
        if (projectName == null || projectName.trim().length() == 0) {
            return null;
        }
        prefs.put("last_google_code_project", projectName);
        return projectName;
    }
}