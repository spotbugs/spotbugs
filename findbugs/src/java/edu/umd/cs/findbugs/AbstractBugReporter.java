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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * An abstract class which provides much of the functionality
 * required of all BugReporter objects.
 */
public abstract class AbstractBugReporter implements BugReporter {
	private static final boolean DEBUG_MISSING_CLASSES = Boolean.getBoolean("findbugs.debug.missingclasses");
	
	private static class Error {
		private int sequence;
		private String message;
		private Throwable cause;
		
		public Error(int sequence, String message) {
			this(sequence, message, null);
		}
		
		public Error(int sequence, String message, Throwable cause) {
			this.sequence = sequence;
			this.message = message;
			this.cause = cause;
		}
		
		public int getSequence() {
			return sequence;
		}
		
		public String getMessage() {
			return message;
		}
		
		public Throwable getCause() {
			return cause;
		}
		
		public int hashCode() {
			int hashCode = message.hashCode();
			if (cause != null) {
				hashCode += 1009 * cause.hashCode();
			}
			return hashCode;
		}
		
		//@Override
		public boolean equals(Object obj) {
			if (obj == null || obj.getClass() != this.getClass())
				return false;
			Error other = (Error) obj;
			if (!message.equals(other.message))
				return false;
			if (this.cause == other.cause)
				return true;
			if (this.cause == null || other.cause == null)
				return false;
			return this.cause.equals(other.cause);
		}
	}

	private int verbosityLevel = NORMAL;
	private int priorityThreshold;
	private boolean analysisUnderway, relaxed;
	private HashSet<String> missingClassMessageSet = new HashSet<String>();
	private LinkedList<String> missingClassMessageList = new LinkedList<String>();
	private Set<Error> errorSet = new HashSet<Error>();
	private List<BugReporterObserver> observerList = new LinkedList<BugReporterObserver>();
	private ProjectStats projectStats = new ProjectStats();
	private int errorCount;

	public void setErrorVerbosity(int level) {
		this.verbosityLevel = level;
	}

	public void setPriorityThreshold(int threshold) {
		this.priorityThreshold = threshold;
	}

	// Subclasses must override doReportBug(), not this method.
	public final void reportBug(BugInstance bugInstance) {
		if (!analysisUnderway) {
			if (FindBugsAnalysisFeatures.isRelaxedMode()) {
				relaxed = true;
			}

			analysisUnderway = true;
		}
		ClassAnnotation primaryClass = bugInstance.getPrimaryClass();
		if (primaryClass != null && !AnalysisContext.currentAnalysisContext().isApplicationClass(primaryClass.getClassName()))
				return;
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
		if (DEBUG_MISSING_CLASSES) {
			System.out.println("Missing class: " + ex.toString());
			ex.printStackTrace(System.out);
		}
		
		if (verbosityLevel == SILENT)
			return;

		String message = getMissingClassName(ex);
		if (message.indexOf("/") >= 0) {
			message = message.replace('/','.');
		}
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
	/**
	 * Report that we skipped some analysis of a method
	 * @param method
	 */
	public void reportSkippedAnalysis(MethodDescriptor method) {
		// TODO: log this
	}
	public void logError(String message) {
		if (verbosityLevel == SILENT)
			return;

		Error error = new Error(errorCount++, message);
		if (!errorSet.contains(error))
			errorSet.add(error);
	}
	
	public void logError(String message, Throwable e) {

		if (e instanceof MethodUnprofitableException) {
			// TODO: log this
			return;
		}
		if (e instanceof MissingClassException) {
			// Record the missing class, in case the exception thrower didn't.
			MissingClassException missingClassEx = (MissingClassException) e;
			ClassNotFoundException cnfe = missingClassEx.getClassNotFoundException();

			reportMissingClass(cnfe);
			// Don't report dataflow analysis exceptions due to missing classes.
			// Too much noise.
			return;
		}
		
		if (verbosityLevel == SILENT)
			return;
	
		Error error = new Error(errorCount++, message, e);
		if (!errorSet.contains(error))
			errorSet.add(error);
	}

	public void reportQueuedErrors() {
		// Report unique errors in order of their sequence
		Error[] errorList = errorSet.toArray(new Error[errorSet.size()]);
		Arrays.sort(errorList, new Comparator<Error>() {
			public int compare(Error o1, Error o2) {
				return o1.getSequence() - o2.getSequence();
			}
		});
		for (Error error : errorList) {
			reportAnalysisError(new AnalysisError(error.getMessage(), error.getCause()));
		}

		for (String aMissingClassMessageList : missingClassMessageList) {
			reportMissingClass(aMissingClassMessageList);
		}
	}

	public void addObserver(BugReporterObserver observer) {
		observerList.add(observer);
	}

	public ProjectStats getProjectStats() {
		return projectStats;
	}

	/**
	 * This should be called when a bug is reported by a subclass.
	 *
	 * @param bugInstance the bug to inform observers of
	 */
	protected void notifyObservers(BugInstance bugInstance) {
		for (BugReporterObserver aObserverList : observerList)
			aObserverList.reportBug(bugInstance);
	}

	/**
	 * Subclasses must override this.
	 * It will be called only for bugs which meet the priority threshold.
	 *
	 * @param bugInstance the bug to report
	 */
	protected abstract void doReportBug(BugInstance bugInstance);

	/**
	 * Report a queued error.
	 * 
	 * @param error the queued error
	 */
	public abstract void reportAnalysisError(AnalysisError error);

	/**
	 * Report a missing class.
	 * 
	 * @param string the name of the class
	 */
	public abstract void reportMissingClass(String string);
}

// vim:ts=4
