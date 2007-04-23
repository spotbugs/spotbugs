package edu.umd.cs.findbugs;

import java.util.ArrayList;

/**
 * Object recording a recoverable error that occurred during analysis.
 * 
 * @author David Hovemeyer
 */
public class AnalysisError {
	private String message;
	private String exceptionMessage;
	private String[] stackTrace;

	/**
	 * Constructor.
	 * 
	 * @param message message describing the error
	 */
	public AnalysisError(String message) {
		this(message, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param message   message describing the error
	 * @param exception exception which is the cause of the error
	 */
	public AnalysisError(String message, Throwable exception) {
		this.message = message;
		if (exception != null) {
			exceptionMessage = exception.toString();
			StackTraceElement[] exceptionStackTrace = exception.getStackTrace();
			ArrayList<String> arr = new ArrayList<String>();
			for (StackTraceElement aExceptionStackTrace : exceptionStackTrace) {
				arr.add(aExceptionStackTrace.toString());
			}
			stackTrace = arr.toArray(new String[arr.size()]);
		}
	}

	/**
	 * Set the message describing the error.
	 * 
	 * @param message message describing the error
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Get the message describing the error.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the exception message.  This is the value returned by
	 * calling toString() on the original exception object.
	 * 
	 * @param exceptionMessage the exception message
	 */
	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	/**
	 * Get the exception message.  This is the value returned by
	 * calling toString() on the original exception object.
	 */
	public String getExceptionMessage() {
		return exceptionMessage;
	}

	/**
	 * Set the stack trace elements.
	 * These are the strings returned by calling toString()
	 * on each StackTraceElement in the original exception.
	 * 
	 * @param stackTraceList the stack trace elements
	 */
	public void setStackTrace(String[] stackTraceList) {
		stackTrace = stackTraceList;
	}

	/**
	 * Get the stack trace elements.
	 * These are the strings returned by calling toString()
	 * on each StackTraceElement in the original exception.
	 */
	public String[] getStackTrace() {
		return stackTrace;
	}
}
