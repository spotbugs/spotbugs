/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007, University of Maryland
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
		if (missing == null || missing.startsWith("[")) {
			return;
		}
		if (missingClassSet.add(missing))
			++missingClassCount;
		super.reportMissingClass(ex);
	}
}
