/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import edu.umd.cs.daveho.ba.RepositoryLookupFailureCallback;

/**
 * Generic interface for bug reporter objects.
 * A BugReporter accumulates all of the information reported
 * by the analyses, which includes bug reports, and also auxiliary
 * information such as analysis errors, missing classes,
 * and class to source file mapping.
 *
 * @author David Hovemeyer
 */
public interface BugReporter extends RepositoryLookupFailureCallback {

	/** Silent error-reporting verbosity level. */
	public static final int SILENT = 0;

	/** Normal error-reporting verbosity level. */
	public static final int NORMAL = 1;

	/**
	 * Set the error-reporting verbosity level.
	 * @param level the verbosity level
	 */
	public void setErrorVerbosity(int level);

	/**
	 * Report a bug.
	 * The implementation may report the bug immediately,
	 * or queue it for later.
	 * @param bugInstance object describing the bug instance
	 */
	public void reportBug(BugInstance bugInstance);

	/**
	 * Log an error that occurs while looking for bugs.
	 * @param message the error message
	 */
	public void logError(String message);

	/**
	 * Map a class to its source file.
	 * @param className the name of the class
	 * @param sourceFileName the name of the source file
	 */
	public void mapClassToSource(String className, String sourceFileName);

	/**
	 * Get the source file for given class.
	 * @param className the name of the class
	 * @return the name of the source file, or null if we
	 *   don't have a source file for the class
	 */
	public String getSourceForClass(String className);

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
}

// vim:ts=4
