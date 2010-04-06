package edu.umd.cs.findbugs.cloud.appEngine;

import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.util.ServiceException;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.cloud.NotSignedInException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BugFilingHelper {
    private static final Logger LOGGER = Logger.getLogger(BugFilingHelper.class.getName());

    private static final boolean USE_GOOGLE_CODE_BUG_FILER = true;

    private final AppEngineCloudClient appEngineCloudClient;
    private GoogleCodeBugFiler googleCodeBugFiler;
    private JiraBugFiler jiraBugFiler;

    public BugFilingHelper(AppEngineCloudClient appEngineCloudClient) {
        this.appEngineCloudClient = appEngineCloudClient;
        googleCodeBugFiler = new GoogleCodeBugFiler(appEngineCloudClient);
        jiraBugFiler = new JiraBugFiler(appEngineCloudClient);
    }

    public String lookupBugStatus(final BugInstance b) {
        final String hash = b.getInstanceHash();
        String status;
        ProtoClasses.Issue issue = appEngineCloudClient.getNetworkClient().getIssueByHash(hash);
        ProtoClasses.BugLinkType linkType = ProtoClasses.BugLinkType.GOOGLE_CODE; // default to googlecode
        if (issue.hasBugLinkType())
            linkType = issue.getBugLinkType();

        final BugFiler bugFiler;
        switch (linkType) {
            case GOOGLE_CODE:
                bugFiler = googleCodeBugFiler;
                break;
            case JIRA:
                bugFiler = jiraBugFiler;
                break;
            default:
                return "<unknown>";
        }
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

    public URL fileBug(BugInstance b, AppEngineCloudClient appEngineCloudClient)
            throws javax.xml.rpc.ServiceException, IOException, NotSignedInException, OAuthException,
                   InterruptedException, ServiceException {
        if (USE_GOOGLE_CODE_BUG_FILER) {
            String projectName = askUserForGoogleCodeProjectName();
            if (projectName == null)
                return null;
            return fileGoogleCodeBug(b, projectName);

        } else {
            String jiraUrl = askUserForJiraUrl();
            if (jiraUrl == null)
                return null;
            IGuiCallback callback = appEngineCloudClient.getGuiCallback();
            List<String> result = callback.showForm("", "File a bug on JIRA", Arrays.asList(
                    new IGuiCallback.FormItem("Project", null, jiraBugFiler.getProjectKeys(jiraUrl)),
                    new IGuiCallback.FormItem("Component"),
                    new IGuiCallback.FormItem("Type", null, jiraBugFiler.getIssueTypes(jiraUrl))));
            if (result == null)
                return null; // user cancelled
            RemoteIssue issue = jiraBugFiler.fileBug(jiraUrl, b, result.get(0), result.get(1), result.get(2));
            String bugUrl = jiraUrl + "/browse/" + issue.getKey();
            appEngineCloudClient.getNetworkClient().setBugLinkOnCloudAndStoreIssueDetails(
                    b, bugUrl, ProtoClasses.BugLinkType.JIRA);
            return new URL(bugUrl);
        }
    }

    private URL fileGoogleCodeBug(BugInstance b, String projectName)
            throws IOException, ServiceException, OAuthException, InterruptedException, NotSignedInException {
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
                b, viewUrl, ProtoClasses.BugLinkType.GOOGLE_CODE);

        return new URL(viewUrl);
    }

    private String askUserForJiraUrl() {
        IGuiCallback guiCallback = appEngineCloudClient.getBugCollection().getProject().getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(AppEngineCloudClient.class);

        String lastProject = prefs.get("last_jira_url", "");
        String dashboardUrl = guiCallback.showQuestionDialog(
                "Issue will be filed in JIRA.\n" +
                "\n" +
                "JIRA dashboard URL:\n" +
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
                "Google Code project name:", "Google Code Issue Tracker",
                lastProject);
        if (projectName == null || projectName.trim().length() == 0) {
            return null;
        }
        prefs.put("last_google_code_project", projectName);
        return projectName;
    }
}