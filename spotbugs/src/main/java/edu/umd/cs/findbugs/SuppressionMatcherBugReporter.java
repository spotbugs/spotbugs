package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.detect.UselessSuppressionDetector;

public class SuppressionMatcherBugReporter extends FilterBugReporter {

    private final SuppressionMatcher suppressionMatcher;

    public SuppressionMatcherBugReporter(BugReporter realBugReporter, SuppressionMatcher suppressionMatcher) {
        super(realBugReporter, suppressionMatcher, false);
        this.suppressionMatcher = suppressionMatcher;
    }

    @Override
    public void finish() {
        UselessSuppressionDetector detector = new UselessSuppressionDetector();
        suppressionMatcher.validateSuppressionUsage(this, detector);
        super.finish();
    }
}
