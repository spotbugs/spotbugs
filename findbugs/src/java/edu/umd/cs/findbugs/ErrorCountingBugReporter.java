package edu.umd.cs.findbugs;

import java.util.HashSet;
import java.util.Set;

/**
 * A delegating bug reporter which counts reported bug instances,
 * missing classes, and serious analysis errors.
 */
public class ErrorCountingBugReporter extends DelegatingBugReporter {
	private int bugCount;
	private int missingClassCount;
	private int errorCount;
	private Set<String> missingClassSet = new HashSet<String>();

	public ErrorCountingBugReporter(BugReporter realBugReporter) {
		super(realBugReporter);
		this.bugCount = 0;
		this.missingClassCount = 0;
		this.errorCount = 0;

		// Add an observer to record when bugs make it through
		// all priority and filter criteria, so our bug count is
		// accurate.
		realBugReporter.addObserver(new BugReporterObserver() {
			public void reportBug(BugInstance bugInstance) {
				++bugCount;
			}
		});
	}

	public int getBugCount() {
		return bugCount;
	}

	public int getMissingClassCount() {
		return missingClassCount;
	}

	public int getErrorCount() {
		return errorCount;
	}

	@Override
	public void logError(String message) {
		++errorCount;
		super.logError(message);
	}

	@Override
	public void reportMissingClass(ClassNotFoundException ex) {
		String missing = AbstractBugReporter.getMissingClassName(ex);
		if (missingClassSet.add(missing))
			++missingClassCount;
		super.reportMissingClass(ex);
	}
}
