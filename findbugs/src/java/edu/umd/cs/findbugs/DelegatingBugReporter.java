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

import org.apache.bcel.classfile.JavaClass;

/**
 * A BugReporter which delegates all method calls to another BugReporter.
 * This is useful for customizing the behavior of another bug reporter.
 *
 * @author David Hovemeyer
 */
public class DelegatingBugReporter implements BugReporter {
	private BugReporter realBugReporter;

	public DelegatingBugReporter(BugReporter realBugReporter) {
		this.realBugReporter = realBugReporter;
	}

	public BugReporter getRealBugReporter() {
		return realBugReporter;
	}

	public void setRealBugReporter(BugReporter realBugReporter) {
		this.realBugReporter = realBugReporter;
	}

	public void setEngine(FindBugs engine) {
		realBugReporter.setEngine(engine);
	}

	public void setErrorVerbosity(int level) {
		realBugReporter.setErrorVerbosity(level);
	}

	public void setPriorityThreshold(int threshold) {
		realBugReporter.setPriorityThreshold(threshold);
	}

	public void observeClass(JavaClass javaClass) {
		realBugReporter.observeClass(javaClass);
	}

	public void reportBug(BugInstance bugInstance) {
		realBugReporter.reportBug(bugInstance);
	}

	public void logError(String message) {
		realBugReporter.logError(message);
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		realBugReporter.reportMissingClass(ex);
	}

	public void finish() {
		realBugReporter.finish();
	}

	public void reportQueuedErrors() {
		realBugReporter.reportQueuedErrors();
	}

	public void addObserver(BugReporterObserver observer) {
		realBugReporter.addObserver(observer);
	}

	public ProjectStats getProjectStats() {
		return realBugReporter.getProjectStats();
	}
}

// vim:ts=4
