package edu.umd.cs.findbugs.cloud.appEngine;

import javax.xml.rpc.ServiceException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.jira.rpc.soap.beans.RemoteComponent;
import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.atlassian.jira.rpc.soap.beans.RemoteIssueType;
import com.atlassian.jira.rpc.soap.beans.RemoteProject;
import com.atlassian.jira.rpc.soap.beans.RemoteStatus;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapServiceServiceLocator;
import com.sun.deploy.services.ServiceManager;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.BugFilingHelper;
import edu.umd.cs.findbugs.cloud.Cloud;

public class JiraBugFiler implements BugFiler {
    private static final Pattern BUG_LINK_PATTERN = Pattern.compile("(.*?)/browse/(.*-\\d+).*");
    
    private final Cloud cloud;
    private final Map<String,JiraSession> sessionsByBaseUrl = new ConcurrentHashMap<String, JiraSession>();

    public JiraBugFiler(Cloud cloud) {
        this.cloud = cloud;
    }

    public String getBugStatus(String bugUrl)
            throws ServiceException, MalformedURLException, java.rmi.RemoteException {

        Matcher m = BUG_LINK_PATTERN.matcher(bugUrl);
        if (!m.matches())
            return null;
        String baseUrl = m.group(1);
        String issueKey = m.group(2);
        JiraBugFiler.JiraSession session = getJiraSession(baseUrl);
        JiraSoapService service = session.service;
        RemoteIssue issue = service.getIssue(session.token, issueKey);
        RemoteStatus[] statuses = service.getStatuses(session.token);
        for (RemoteStatus status : statuses) {
            if (status.getId().equals(issue.getStatus()))
                return status.getName();
        }
        return null;
    }

    public List<String> getProjectKeys(String baseUrl)
            throws java.rmi.RemoteException, MalformedURLException, ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        if (session == null)
            return null;
        List<String> projectKeys = new ArrayList<String>();
        for (RemoteProject project : session.service.getProjectsNoSchemes(session.token)) {
            projectKeys.add(project.getKey());
        }
        return projectKeys;
    }

    public List<String> getComponentNames(String baseUrl, String key)
            throws java.rmi.RemoteException, MalformedURLException, ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        RemoteComponent[] components = session.service.getComponents(session.token, key);
        List<String> componentNames = new ArrayList<String>();
        for (RemoteComponent component : components) {
            componentNames.add(component.getName());
        }
        return componentNames;
    }

    public RemoteIssue fileBug(String baseUrl, BugInstance b, String projectKey,
                               String componentName, String issueTypeName)
            throws MalformedURLException, ServiceException, java.rmi.RemoteException {
        JiraBugFiler.JiraSession session = getJiraSession(baseUrl);
        RemoteComponent actualComponent = findComponent(projectKey, componentName, session);
        if (actualComponent == null)
            throw new IllegalArgumentException("no component named " + componentName);
        RemoteProject project = session.service.getProjectByKey(session.token, projectKey);
        BugFilingHelper helper = new BugFilingHelper(cloud);
        
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

    public List<String> getIssueTypes(String baseUrl) throws MalformedURLException, RemoteException, ServiceException {
        JiraSession session = getJiraSession(baseUrl);
        List<String> typeNames = new ArrayList<String>();
        for (RemoteIssueType issueType : session.service.getIssueTypes(session.token)) {
            typeNames.add(issueType.getName());
        }
        return typeNames;
    }

    // ============================== end of public methods ==============================

    private String getIssueType(JiraSession session, String issueTypeName, RemoteProject project)
            throws java.rmi.RemoteException {
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

    private @CheckForNull JiraSession getJiraSession(String baseUrl)
            throws ServiceException, MalformedURLException, java.rmi.RemoteException {

        JiraBugFiler.JiraSession session = sessionsByBaseUrl.get(baseUrl);
        if (session != null)
            return session;
        session = new JiraSession();
        session.service = new JiraSoapServiceServiceLocator()
                .getJirasoapserviceV2(new URL(baseUrl + "/rpc/soap/jirasoapservice-v2"));
        session.token = getToken(baseUrl, session);
        if (session.token == null)
            return null; // user cancelled login
        sessionsByBaseUrl.put(baseUrl, session);
        return session;
    }

    private String getToken(String baseUrl, JiraSession session) throws java.rmi.RemoteException {
        IGuiCallback callback = cloud.getBugCollection().getProject().getGuiCallback();
        String usernameKey = getPreferenceskeyForJiraBaseUrl(baseUrl); // alphanumeric plus dots and dashes
        Preferences prefs = Preferences.userNodeForPackage(JiraBugFiler.class);
        String lastUsername = prefs.get(usernameKey, "");
        List<String> results = callback.showForm("Enter username and password for " + baseUrl,
                                                 "JIRA Login",
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

    private class JiraSession {
        public JiraSoapService service;
        public String token;
        public String username;

        private JiraSession() {
        }

        public JiraSession(JiraSoapService service, String token) {
            this.service = service;
            this.token = token;
        }
    }
}
