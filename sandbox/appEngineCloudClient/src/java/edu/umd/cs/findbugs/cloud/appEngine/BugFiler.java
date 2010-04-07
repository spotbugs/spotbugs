package edu.umd.cs.findbugs.cloud.appEngine;


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
