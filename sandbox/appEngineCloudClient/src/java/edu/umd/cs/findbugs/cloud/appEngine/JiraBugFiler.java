package edu.umd.cs.findbugs.cloud.appEngine;

import javax.xml.rpc.ServiceException;
import java.net.MalformedURLException;
import java.net.URL;

import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.atlassian.jira.rpc.soap.beans.RemoteStatus;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapService;
import com.atlassian.jira.rpc.soap.jirasoapservice_v2.JiraSoapServiceServiceLocator;

public class JiraBugFiler {
    public static void main(String[] args) throws MalformedURLException, java.rmi.RemoteException, ServiceException {
        String status = new JiraBugFiler().getBugStatus();
        System.out.println(status);
    }
    
    public String getBugStatus() throws ServiceException, MalformedURLException, java.rmi.RemoteException {
        JiraSoapService service = new JiraSoapServiceServiceLocator()
                .getJirasoapserviceV2(new URL("http://jira.atlassian.com/rpc/soap/jirasoapservice-v2"));
        String token = service.login("keithkml", "jira");
        RemoteIssue issue = service.getIssue(token, "JRA-12152");
        RemoteStatus[] statuses = service.getStatuses(token);
        return statuses[Integer.parseInt(issue.getStatus())].getName();
    }
}
