/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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
import java.util.Iterator;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Add an annotation string to every BugInstance in a BugCollection.
 */
public class AddAnnotation {
	private BugCollection bugCollection;
	private Project project;
	private String annotation;

	public AddAnnotation(BugCollection bugCollection, Project project, String annotation) {
		this.bugCollection = bugCollection;
		this.project = project;
		this.annotation = annotation;
	}

	public AddAnnotation(String resultsFile, String annotation)
			throws IOException, DocumentException {
		this(new SortedBugCollection(), new Project(), annotation);
		bugCollection.readXML(resultsFile, project);
	}

	public BugCollection getBugCollection() {
		return bugCollection;
	}

	public Project getProject() {
		return project;
	}

	public void execute() {
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();

			// Don't add the annotation if it is already present
			if (bugInstance.annotationTextContainsWord(this.annotation))
				continue;

			String annotation = bugInstance.getAnnotationText();
			StringBuilder buf = new StringBuilder();
			if (!annotation.equals("")) {
				buf.append(annotation);
				buf.append('\n');
			}
			buf.append(this.annotation);
			bugInstance.setAnnotationText(buf.toString());
		}
	}

	@SuppressWarnings("DM_EXIT")
	public static void main(String[] argv) throws Exception {
		if (argv.length != 2) {
			System.err.println("Usage: " + AddAnnotation.class.getName() + " <results file> <annotation>");
			System.exit(1);
		}

		String filename = argv[0];
		String annotation = argv[1];

		AddAnnotation addAnnotation = new AddAnnotation(filename, annotation);
		addAnnotation.execute();

		addAnnotation.getBugCollection().writeXML(filename, addAnnotation.getProject());
	}
}

// vim:ts=3
