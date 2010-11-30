package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.rpc.ServiceException;

import com.atlassian.jira.rpc.soap.beans.RemoteComponent;
import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.atlassian.jira.rpc.soap.beans.RemoteIssueType;
import com.atlassian.jira.rpc.soap.beans.RemoteProject;
import com.atlassian.jira.rpc.soap.beans.RemoteStatus;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapServiceServiceLocator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.BugFilingCommentHelper;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

public class JiraBugFiler implements BugFiler {
    private static final Logger LOGGER = Logger.getLogger(JiraBugFiler.class.getName());

    private static final Pattern BUG_LINK_PATTERN = Pattern.compile("(.*?)/browse/(.*-\\d+).*");

    private Cloud cloud;

    private final Map<String, JiraSession> sessionsByBaseUrl = new ConcurrentHashMap<String, JiraSession>();

    private String url;

    public JiraBugFiler(ComponentPlugin<BugFiler> plugin, Cloud cloud) {
        this.cloud = cloud;
        this.url = SystemProperties.getProperty("findbugs.jiraURL");
    }

    public URL file(BugInstance b) throws IOException, SignInCancelledException {
        if (url == null) {
            url = askUserForJiraUrl();
            if (url == null)
                return null;
        }

        String trackerUrl = processJiraDashboardUrl(url);

        try {
            return actuallyFile(b, trackerUrl);
        } catch (ServiceException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }

    public String getBugStatus(String bugUrl) throws ServiceException, MalformedURLException, java.rmi.RemoteException {

        Matcher m = BUG_LINK_PATTERN.matcher(bugUrl);
        if (!m.matches())
            return null;
        String baseUrl = m.group(1);
        String issueKey = m.group(2);
        JiraBugFiler.JiraSession session = getJiraSession(baseUrl);
        if (session == null) // maybe user cancelled signin
            return null;
        JiraSoapService service = session.service;
        RemoteIssue issue = service.getIssue(session.token, issueKey);
        RemoteStatus[] statuses = service.getStatuses(session.token);
        for (RemoteStatus status : statuses) {
            if (status.getId().equals(issue.getStatus()))
                return status.getName();
        }
        return null;
    }

    // ============================== end of public methods ==============================

    private List<String> getProjectKeys(String baseUrl) throws java.rmi.RemoteException, MalformedURLException, ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        if (session == null)
            return null;
        List<String> projectKeys = new ArrayList<String>();
        for (RemoteProject project : session.service.getProjectsNoSchemes(session.token)) {
            projectKeys.add(project.getKey());
        }
        return projectKeys;
    }

    @SuppressWarnings("UnusedDeclaration")
    private List<String> getComponentNames(String baseUrl, String key) throws java.rmi.RemoteException, MalformedURLException,
            ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        RemoteComponent[] components = session.service.getComponents(session.token, key);
        List<String> componentNames = new ArrayList<String>();
        for (RemoteComponent component : components) {
            componentNames.add(component.getName());
        }
        return componentNames;
    }

    private RemoteIssue fileBug(String baseUrl, BugInstance b, String projectKey, String componentName, String issueTypeName)
            throws MalformedURLException, ServiceException, java.rmi.RemoteException {
        JiraBugFiler.JiraSession session = getJiraSession(baseUrl);
        RemoteComponent actualComponent = findComponent(projectKey, componentName, session);
        if (actualComponent == null)
            throw new IllegalArgumentException("no component named " + componentName);
        RemoteProject project = session.service.getProjectByKey(session.token, projectKey);
        BugFilingCommentHelper helper = new BugFilingCommentHelper(cloud);

        RemoteIssue issue = new RemoteIssue();
        issue.setReporter(session.username);
        issue.setAssignee(session.username);
        issue.setType(getIssueType(session, issueTypeName, project));
        issue.setProject(projectKey);
        issue.setSummary(helper.getBugReportSummary(b));
        issue.setDescription(helper.getBugReportText(b));
        issue.setComponents(new RemoteComponent[] { actualComponent });
        return session.service.createIssue(session.token, issue);
    }

    @CheckForNull
    private List<String> getIssueTypes(String baseUrl) throws MalformedURLException, RemoteException, ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        if (session == null)
            return null;
        List<String> typeNames = new ArrayList<String>();
        for (RemoteIssueType issueType : session.service.getIssueTypes(session.token)) {
            typeNames.add(issueType.getName());
        }
        return typeNames;
    }

    private String getIssueType(JiraSession session, String issueTypeName, RemoteProject project) throws java.rmi.RemoteException {
        String projectId = project.getId();
        String issueTypeId = null;
        for (RemoteIssueType issueType : session.service.getIssueTypesForProject(session.token, projectId)) {
            if (issueType.getName().equals(issueTypeName)) {
                issueTypeId = issueType.getId();
                break;
            }
        }
        if (issueTypeId == null)
            issueTypeId = "1";
        return issueTypeId;
    }

    private @CheckForNull
    JiraSession getJiraSession(String baseUrl) throws ServiceException, MalformedURLException, java.rmi.RemoteException {

        JiraBugFiler.JiraSession session = sessionsByBaseUrl.get(baseUrl);
        if (session != null)
            return session;
        session = new JiraSession();
        session.service = new JiraSoapServiceServiceLocator().getJirasoapserviceV2(new URL(baseUrl
                + "/rpc/soap/jirasoapservice-v2"));
        session.token = getToken(baseUrl, session);
        if (session.token == null)
            return null; // user cancelled login
        sessionsByBaseUrl.put(baseUrl, session);
        return session;
    }

    private String getToken(String baseUrl, JiraSession session) throws java.rmi.RemoteException {
        IGuiCallback callback = cloud.getBugCollection().getProject().getGuiCallback();
        String usernameKey = getPreferenceskeyForJiraBaseUrl(baseUrl); // alphanumeric
                                                                       // plus
                                                                       // dots
                                                                       // and
                                                                       // dashes
        Preferences prefs = Preferences.userNodeForPackage(JiraBugFiler.class);
        String lastUsername = prefs.get(usernameKey, "");
        List<String> results = callback.showForm(
                "Enter JIRA username and password for \n" + baseUrl,
                "JIRA",
                Arrays.asList(new IGuiCallback.FormItem("Username", lastUsername),
                        new IGuiCallback.FormItem("Password").password()));
        if (results == null)
            return null;
        String username = results.get(0);
        String password = results.get(1);

        String token = session.service.login(username, password);
        session.username = username;
        prefs.put(usernameKey, username);
        return token;
    }

    private String getPreferenceskeyForJiraBaseUrl(String url) {
        return "jira_last_user_" + url.replaceAll("[^A-Za-z0-9_.-]", "");
    }

    private RemoteComponent findComponent(String projectKey, String componentName, JiraSession session)
            throws java.rmi.RemoteException {

        RemoteComponent[] components = session.service.getComponents(session.token, projectKey);
        RemoteComponent actualComponent = null;
        for (RemoteComponent component : components) {
            if (component.getName().equals(componentName)) {
                actualComponent = component;
                break;
            }
        }
        return actualComponent;
    }

    private URL actuallyFile(BugInstance b, final String trackerUrl) throws ServiceException, IOException, SignInCancelledException {
        IGuiCallback callback = cloud.getGuiCallback();
        List<String> issueTypes = getIssueTypes(trackerUrl);
        if (issueTypes == null)
            return null;
        List<String> result = callback.showForm("", "File a bug on JIRA",
                Arrays.asList(new IGuiCallback.FormItem("Project", null, getProjectKeys(trackerUrl)),
                        new IGuiCallback.FormItem("Component") {
                            private String lastProjectKey = "";
                            public List<String> componentNames = new ArrayList<String>();

                            @Override
                            public List<String> getPossibleValues() {
                                if (getItems() != null) {
                                    String newProjectKey = getItems().get(0).getCurrentValue();
                                    if (newProjectKey != null && !lastProjectKey.equals(newProjectKey)) {
                                        this.lastProjectKey = newProjectKey;
                                        try {
                                            componentNames = getComponentNames(trackerUrl, newProjectKey);
                                        } catch (Exception e) {
                                            LOGGER.log(Level.SEVERE, "Error connecting to JIRA at " + trackerUrl, e);
                                            componentNames = new ArrayList<String>();
                                        }
                                    }
                                }
                                return componentNames;
                            }
                        },
                        new IGuiCallback.FormItem("Type", null, issueTypes)));
        if (result == null)
            return null; // user cancelled
        RemoteIssue issue = fileBug(trackerUrl, b, result.get(0), result.get(1), result.get(2));
        String bugUrl = trackerUrl + "/browse/" + issue.getKey();
        cloud.setBugLinkOnCloudAndStoreIssueDetails(b, bugUrl, "JIRA");
        return new URL(bugUrl);
    }

    private String askUserForJiraUrl() {
        IGuiCallback guiCallback = cloud.getBugCollection().getProject().getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(JiraBugFiler.class);

        String lastProject = prefs.get("last_jira_url", "");
        String dashboardUrl = guiCallback.showQuestionDialog("Issue will be filed in JIRA.\n" + "\n"
                + "Type your project's JIRA dashboard URL below.\n" + "(ex. http://jira.atlassian.com/secure/Dashboard.jspa)",
                "JIRA", lastProject);
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

    private class JiraSession {
        public JiraSoapService service;

        public String token;

        public String username;

        private JiraSession() {
        }
    }

    public boolean ready() {
//        return url != null;
        return true;
    }
}
