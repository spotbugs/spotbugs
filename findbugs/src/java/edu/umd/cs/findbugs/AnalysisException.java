package edu.umd.cs.findbugs;

/**
 * A kind of runtime exception that can be thrown to indicate
 * a fatal error in an analysis.  It would be nice to make this a
 * checked exception, but we can't throw those from BCEL visitors.
 */
public class AnalysisException extends RuntimeException {
	/**
	 * Constructor.
	 * @param message reason for the error
	 */
	public AnalysisException(String message) {
		super(message);
	}
}

// vim:ts=4
