/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2006 University of Maryland
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

import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public abstract class BugCollectionBugReporter extends TextUIBugReporter implements Debug {
	private SortedBugCollection bugCollection;
	private Project project;

	public BugCollectionBugReporter(Project project) {
		this.project = project;
		this.bugCollection = new SortedBugCollection(getProjectStats());

		bugCollection.setTimestamp(System.currentTimeMillis());
	}

	public Project getProject() {
		return project;
	}

	public BugCollection getBugCollection() {
		return bugCollection;
	}

	public void observeClass(ClassDescriptor classDescriptor) {
	}

	@Override
	public void logError(String message) {
		bugCollection.addError(message);
		super.logError(message);
	}

	@Override
	public void logError(String message, Throwable e) {
		if (e instanceof MissingClassException) {
			MissingClassException e2 = (MissingClassException)e;
			reportMissingClass(e2.getClassNotFoundException());
			return;
		}
		if (e instanceof MethodUnprofitableException) {
			// TODO: log this
			return;
		}
		bugCollection.addError(message, e);
		super.logError(message, e);
	}

	@Override
	public void reportMissingClass(ClassNotFoundException ex) {
		bugCollection.addMissingClass(getMissingClassName(ex));
		super.reportMissingClass(ex);
	}

	@Override
	public void doReportBug(BugInstance bugInstance) {
		if (VERIFY_INTEGRITY) checkBugInstance(bugInstance);
		if (bugCollection.add(bugInstance))
			notifyObservers(bugInstance);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugReporter#getRealBugReporter()
	 */
	@Override
	public BugReporter getRealBugReporter() {
		return this;
	}
}

// vim:ts=4
