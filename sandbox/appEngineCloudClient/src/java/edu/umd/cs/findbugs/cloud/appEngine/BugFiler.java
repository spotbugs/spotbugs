package edu.umd.cs.findbugs.cloud.appEngine;

import javax.xml.rpc.ServiceException;
import java.net.MalformedURLException;

import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.util.AuthenticationException;

/**
 * Created by IntelliJ IDEA.
 * User: keith
 * Date: Feb 26, 2010
 * Time: 2:02:32 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BugFiler {
    String getBugStatus(String bugUrl) throws Exception;
}
