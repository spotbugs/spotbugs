/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import java.util.*;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;

/**
 * An abstract class which provides much of the functionality
 * required of all BugReporter objects.
 */
public abstract class AbstractBugReporter implements BugReporter {
	private static class Error {
		private String message;
		private Throwable cause;
		
		public Error(String message) {
			this.message = message;
		}
		
		public Error(String message, Throwable cause) {
			this.message = message;
			this.cause = cause;
		}
		
		public String getMessage() {
			return message;
		}
		
		public Throwable getCause() {
			return cause;
		}
	}

	private FindBugs engine;
	private int verbosityLevel = NORMAL;
	private int priorityThreshold;
	private boolean analysisUnderway, relaxed;
	private HashSet<String> missingClassMessageSet = new HashSet<String>();
	private LinkedList<String> missingClassMessageList = new LinkedList<String>();
	private LinkedList<Error> errorMessageList = new LinkedList<Error>();
	private List<BugReporterObserver> observerList = new LinkedList<BugReporterObserver>();
	private ProjectStats projectStats = new ProjectStats();

	public void setEngine(FindBugs engine) {
		this.engine = engine;
	}

	public FindBugs getEngine() {
		return engine;
	}

	public void setErrorVerbosity(int level) {
		this.verbosityLevel = level;
	}

	public void setPriorityThreshold(int threshold) {
		this.priorityThreshold = threshold;
	}

	// Subclasses must override doReportBug(), not this method.
	public final void reportBug(BugInstance bugInstance) {
		if (!analysisUnderway) {
			if (AnalysisContext.currentAnalysisContext().getBoolProperty(
					FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
				relaxed = true;
			}

			analysisUnderway = true;
		}
		
		if (bugInstance.getPriority() <= priorityThreshold || relaxed)
			doReportBug(bugInstance);
	}

	public static String getMissingClassName(ClassNotFoundException ex) {
		String message = ex.getMessage();

		// Try to decode the error message by extracting the class name.
		String className = ClassNotFoundExceptionParser.getMissingClassName(ex);
		if (className != null)
			return className;

		// Just return the entire message.
		// It hopefully will still make sense to the user.
		return message;
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		if (verbosityLevel == SILENT)
			return;

		String message = getMissingClassName(ex);

		if (message.startsWith("[")) {
			// Sometimes we see methods called on array classes.
			// Obviously, these don't exist as class files.
			// So, we should just ignore the exception.
			// Really, we should fix the class/method search interfaces
			// to be much more intelligent in resolving method
			// implementations.
			return;
		}

		if (!missingClassMessageSet.contains(message)) {
			missingClassMessageSet.add(message);
			missingClassMessageList.add(message);
		}
	}

	public void logError(String message) {
		if (verbosityLevel == SILENT)
			return;

		errorMessageList.add(new Error(message));
	}
	
	public void logError(String message, Throwable e) {
		if (verbosityLevel == SILENT)
			return;
		
		errorMessageList.add(new Error(message, e));
	}

	public void reportQueuedErrors() {
		if (errorMessageList.isEmpty() && missingClassMessageList.isEmpty())
			return;

		beginReport();
		if (!errorMessageList.isEmpty()) {
			reportLine("The following errors occured during analysis:");
			for (Iterator<Error> i = errorMessageList.iterator(); i.hasNext();) {
				Error error = i.next();
				reportLine("\t" + error.getMessage());
				Throwable cause = error.getCause();
				if (cause != null) {
					reportLine("\t\t" + cause.toString());
					StackTraceElement[] stackTrace = cause.getStackTrace();
					for (int j = 0; j < stackTrace.length; ++j) {
						reportLine("\t\t\t" + stackTrace[j].toString());
					}
				}
			}
		}
		if (!missingClassMessageList.isEmpty()) {
			reportLine("The following classes needed for analysis were missing:");
			for (Iterator<String> i = missingClassMessageList.iterator(); i.hasNext();)
				reportLine("\t" + i.next());
		}
		endReport();
	}

	public void addObserver(BugReporterObserver observer) {
		observerList.add(observer);
	}

	public ProjectStats getProjectStats() {
		return projectStats;
	}

	/**
	 * This should be called when a bug is reported by a subclass.
	 */
	protected void notifyObservers(BugInstance bugInstance) {
		Iterator<BugReporterObserver> i = observerList.iterator();
		while (i.hasNext())
			i.next().reportBug(bugInstance);
	}

	/**
	 * Subclasses must override this.
	 * It will be called only for bugs which meet the priority threshold.
	 */
	protected abstract void doReportBug(BugInstance bugInstance);

	public abstract void beginReport();

	public abstract void reportLine(String msg);

	public abstract void endReport();
}

// vim:ts=4
