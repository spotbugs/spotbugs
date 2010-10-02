package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.net.URL;

import edu.umd.cs.findbugs.BugInstance;

public interface BugFiler {

    String getBugStatus(String bugUrl) throws Exception;

    URL file(BugInstance b) throws IOException, SignInCancelledException;

    boolean ready();

}
