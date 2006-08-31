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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import edu.umd.cs.findbugs.BugPattern;

public class PlainPrintBugDescriptions extends PrintBugDescriptions {
	private String docTitle;
	private PrintStream out;

	public PlainPrintBugDescriptions(String docTitle, OutputStream out) {
		this.docTitle = docTitle;
		this.out = new PrintStream(out);
	}

	protected String getDocTitle() { return docTitle; }

	protected PrintStream getPrintStream() { return out; }

	@Override
	protected void prologue() throws IOException {
		out.println("<html><head><title>" + docTitle + "</title>");
		header();
		out.println("</head><body>");
		beginBody();
		out.println("<h1>" + docTitle + "</h1>");
	}

	@Override
	protected void emit(BugPattern bugPattern) throws IOException {
		out.println("<h2>" + bugPattern.getAbbrev() + ": " +
			bugPattern.getShortDescription() + "</h2>");
		out.println(bugPattern.getDetailText());
	}

	@Override
	protected void epilogue() throws IOException {
		endBody();
		out.println("</body></html>");
	}

	/** Extra stuff that can be printed in the &lt;head&gt; element. */
	protected void header() throws IOException {
	}

	/** Extra stuff printed at the beginning of the &lt;body&gt; element. */
	protected void beginBody() throws IOException {
	}

	/** Extra stuff printed at the end of the &lt;body&gt; element. */
	protected void endBody() throws IOException {
	}

	public static void main(String[] args) throws Exception {
		String docTitle = "FindBugs Bug Descriptions";
		if (args.length > 0)
			docTitle = args[0];
		new PlainPrintBugDescriptions(docTitle, System.out).print();
	}
}

// vim:ts=3
