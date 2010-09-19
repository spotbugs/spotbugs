package edu.umd.cs.findbugs.cloud.appEngine;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;

import java.io.IOException;
import java.net.URL;

public interface BugFiler {
    String getBugStatus(String bugUrl) throws Exception;

    URL file(BugInstance b) throws IOException, SignInCancelledException;
}
