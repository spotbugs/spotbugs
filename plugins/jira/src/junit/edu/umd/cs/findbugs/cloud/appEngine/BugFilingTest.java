package edu.umd.cs.findbugs.cloud.appEngine;

import junit.framework.TestCase;

import static edu.umd.cs.findbugs.cloud.appEngine.JiraBugFiler.processJiraDashboardUrl;

public class BugFilingTest extends TestCase {
    public void testJiraDashboardUrlProcessor() {
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("  jira.atlassian.com    "));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("http://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/"));
        assertEquals("http://jira.atlassian.com", processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa"));
        assertEquals("http://jira.atlassian.com",
                processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa;sessionId=blah"));
        assertEquals("http://jira.atlassian.com",
                processJiraDashboardUrl("https://jira.atlassian.com/secure/Dashboard.jspa?blah"));
    }

}
