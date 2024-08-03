package edu.umd.cs.findbugs;

import java.io.PrintStream;

/**
 * The interface provides configurable methods to {@link TextUICommandLine}.
 */
interface ConfigurableBugReporter extends BugReporter {
    void setRankThreshold(int threshold);

    void setUseLongBugCodes(boolean useLongBugCodes);

    void setOutputStream(PrintStream outputStream);
}
