/*
 * Generate HTML file containing bug descriptions
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

package edu.umd.cs.findbugs.tools.html;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Iterator;

public class PrintBugDescriptions {
	private String docTitle;
	private PrintStream out;

	public PrintBugDescriptions(String docTitle, OutputStream out) {
		this.docTitle = docTitle;
		this.out = new PrintStream(out);
	}

	public void print() throws IOException {
		// Ensure bug patterns are loaded
		DetectorFactoryCollection.instance();

		prologue();

		Iterator<BugPattern> i = I18N.instance().bugPatternIterator();
		while (i.hasNext()) {
			BugPattern bugPattern = i.next();
			emit(bugPattern);
		}

		epilogue();
	}

	private void prologue() throws IOException {
		out.println("<html><head><title>" + docTitle + "</title></head><body>");
		out.println("<h1>" + docTitle + "</h1>");
	}

	private void emit(BugPattern bugPattern) throws IOException {
		out.println("<h2>" + bugPattern.getAbbrev() + ": " +
			bugPattern.getShortDescription() + "</h2>");
		out.println(bugPattern.getDetailText());
	}

	private void epilogue() throws IOException {
		out.println("</body></html>");
	}

	public static void main(String[] args) throws Exception {
		String docTitle = "FindBugs Bug Descriptions";
		if (args.length > 0)
			docTitle = args[0];
		new PrintBugDescriptions(docTitle, System.out).print();
	}
}

// vim:ts=3
