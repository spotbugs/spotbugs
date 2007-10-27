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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.classfile.IClassObserver;

/**
 * Generic interface for bug reporter objects.
 * A BugReporter accumulates all of the information reported
 * by the analyses, which includes bug reports, and also auxiliary
 * information such as analysis errors, missing classes,
 * and class to source file mapping.
 *
 * @author David Hovemeyer
 */
public interface BugReporter extends RepositoryLookupFailureCallback, IClassObserver {

	/**
	 * Silent error-reporting verbosity level.
	 */
	public static final int SILENT = 0;

	/**
	 * Normal error-reporting verbosity level.
	 */
	public static final int NORMAL = 1;

	/**
	 * Set the error-reporting verbosity level.
	 *
	 * @param level the verbosity level
	 */
	public void setErrorVerbosity(int level);

	/**
	 * Set the priority threshold.
	 *
	 * @param threshold bug instances must be at least as important as
	 *                  this priority to be reported
	 */
	public void setPriorityThreshold(int threshold);

	/**
	 * Report a bug.
	 * The implementation may report the bug immediately,
	 * or queue it for later.
	 *
	 * @param bugInstance object describing the bug instance
	 */
	public void reportBug(@NonNull BugInstance bugInstance);

	/**
	 * Finish reporting bugs.
	 * If any bug reports have been queued, calling this method
	 * will flush them.
	 */
	public void finish();

	/**
	 * Report any accumulated error messages.
	 */
	public void reportQueuedErrors();

	/**
	 * Add an observer.
	 *
	 * @param observer the observer
	 */
	public void addObserver(BugReporterObserver observer);

	/**
	 * Get ProjectStats object used to store statistics about
	 * the overall project being analyzed.
	 */
	public ProjectStats getProjectStats();

	/**
	 * Get the real bug reporter at the end of a chain of delegating bug reporters.
	 * All non-delegating bug reporters should simply "return this".
	 * 
	 * @return the real bug reporter at the end of the chain, or
	 *          this object if there is no delegation
	 */
	public BugReporter getRealBugReporter();
}

// vim:ts=4
