package edu.umd.cs.findbugs;

/**
 * Generic interface for bug reporter objects.
 */
public interface BugReporter {
	/**
	 * Report a bug.
	 * The implementation may report the bug immediately,
	 * or queue it for later.
	 * @param bugInstance object describing the bug instance
	 */
	public void reportBug(BugInstance bugInstance);

	/**
	 * Finish reporting bugs.
	 * If any bug reports have been queued, calling this method
	 * will flush them.
	 */
	public void finish();
}

// vim:ts=4
