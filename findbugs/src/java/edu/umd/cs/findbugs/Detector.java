package edu.umd.cs.findbugs;

/**
 * The interface which all bug pattern detectors must implement.
 */
public interface Detector {
	/** Normal priority for bug instances. */
	public static final int LOW_PRIORITY = 3;
	public static final int NORMAL_PRIORITY = 2;
	public static final int HIGH_PRIORITY = 1;

	/**
	 * Visit the ClassContext for a class which should be analyzed
	 * for instances of bug patterns.
	 */
	public void visitClassContext(ClassContext classContext);

	/**
	 * This method is called after all classes to be visited.
	 * It should be used by any detectors which accumulate information
	 * over all visited classes to generate results.
	 */
	public void report();
}

// vim:ts=4
