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

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * Report warnings as an XML document.
 */
public class XMLBugReporter extends BugCollectionBugReporter {
	private boolean addMessages;

	public XMLBugReporter(Project project) {
		super(project);
		this.addMessages = false;
	}

	public void setAddMessages(boolean enable) {
		this.addMessages = enable;
	}

	public void finish() {
		generateSummary();
		try {
			if (!addMessages) {
				// Plain XML output.
				// Write XML directly to the output stream.
				getBugCollection().writeXML(outputStream, getProject());
			} else {
				// XML output with messages.
				// This requires us to build a dom4j tree,
				// add the messages to it, and then
				// write the tree to the output stream.

				// Build tree
				Document document = getBugCollection().toDocument(getProject());

				// Add messages
				AddMessages addMessages = new AddMessages(getBugCollection(), document);
				addMessages.execute();

				// Write to output stream
				XMLWriter writer = new XMLWriter(outputStream, OutputFormat.createPrettyPrint());
				writer.write(document);
			}
		} catch (Exception e) {
			logError("Couldn't write XML output: " + e.toString());
		}
	}
}

// vim:ts=4
