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

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.TransformerException;

import org.apache.bcel.classfile.JavaClass;

public abstract class BugCollectionBugReporter extends TextUIBugReporter {
	private SortedBugCollection bugCollection = new SortedBugCollection();
	private Project project;

	public BugCollectionBugReporter(Project project) {
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public BugCollection getBugCollection() {
		return bugCollection;
	}

	public void observeClass(JavaClass javaClass) {
	}

	public void logError(String message) {
		bugCollection.addError(message);
		super.logError(message);
	}

	public void reportMissingClass(ClassNotFoundException ex) {
		bugCollection.addMissingClass(getMissingClassName(ex));
		super.reportMissingClass(ex);
	}

	public void doReportBug(BugInstance bugInstance) {
		if (bugCollection.add(bugInstance))
			notifyObservers(bugInstance);
	}

	protected void generateSummary() {
		try {
			StringWriter writer = new StringWriter();
			ProjectStats stats = getProjectStats();
			stats.transformSummaryToHTML(writer);
			String html = writer.toString();
			bugCollection.setSummaryHTML(html);
		} catch (IOException e) {
			logError("Couldn't generate summary HTML: " + e.toString());
		} catch (TransformerException e) {
			logError("Couldn't generate summary HTML: " + e.toString());
		}
	}
}

// vim:ts=4
