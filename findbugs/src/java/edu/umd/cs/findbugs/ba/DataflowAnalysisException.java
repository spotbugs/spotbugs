package edu.umd.cs.daveho.ba;

/**
 * Exception type to indicate a dataflow analysis failure.
 * @see Dataflow
 * @see DataflowAnalysis
 */
public class DataflowAnalysisException extends Exception {
	/**
	 * Constructor.
	 */
	public DataflowAnalysisException() { }

	/**
	 * Constructor.
	 * @param msg message describing the reason for the exception
	 */
	public DataflowAnalysisException(String msg) { super(msg); }
}

// vim:ts=4
