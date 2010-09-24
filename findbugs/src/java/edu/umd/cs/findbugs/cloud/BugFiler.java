package edu.umd.cs.findbugs.cloud;

import edu.umd.cs.findbugs.BugInstance;

import java.io.IOException;
import java.net.URL;

public interface BugFiler {
    String getBugStatus(String bugUrl) throws Exception;

    URL file(BugInstance b) throws IOException, SignInCancelledException;
}
