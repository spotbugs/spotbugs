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
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Version;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class PrettyPrintBugDescriptions extends PlainPrintBugDescriptions {
	private Set<BugPattern> bugPatternSet;

	private static final String[] TABLE_COLORS = new String[]{ "#eeeeee", "#ffffff" };

	private static class BugPatternComparator implements Comparator<BugPattern> {
		public int compare(BugPattern a, BugPattern b) {
			int cmp = a.getCategory().compareTo(b.getCategory());
			if (cmp != 0) return cmp;
			cmp = a.getAbbrev().compareTo(b.getAbbrev());
			if (cmp != 0) return cmp;
			return a.getType().compareTo(b.getType());
		}
	}

	public PrettyPrintBugDescriptions(String docTitle, OutputStream out) {
		super(docTitle, out);
		this.bugPatternSet = new TreeSet<BugPattern>(new BugPatternComparator());
	}

	protected void prologue() throws IOException {
		super.prologue();

		PrintStream out = getPrintStream();

		out.println(
			"<p> This document lists the bug patterns that are reported by\n" +
			"<a href=\"" + Version.WEBSITE + "\">FindBugs</a>.&nbsp; Note that some of\n" +
			"these bug patterns may be experimental or disabled by default"
		);
	}

	protected void emit(BugPattern bugPattern) throws IOException {
		bugPatternSet.add(bugPattern);
	}

	protected void epilogue() throws IOException {
		emitSummaryTable();
		emitBugDescriptions();
		super.epilogue();
	}

	private void emitSummaryTable() {
		PrintStream out = getPrintStream();

		out.println("<h2>Summary</h2>");

		out.println("<table width=\"100%\">");

		out.println("<tr bgcolor=\"#9999ee\"><th>Description</th><th>Category</th></tr>");

		ColorAlternator colorAlternator = new ColorAlternator(TABLE_COLORS);

		for (Iterator<BugPattern> i = bugPatternSet.iterator(); i.hasNext(); ) {
			BugPattern bugPattern = i.next();
			out.print("<tr bgcolor=\"" + colorAlternator.nextColor() + "\">");
			out.print("<td><a href=\"#" + bugPattern.getType() + "\">" +
				bugPattern.getAbbrev() + ": " + bugPattern.getShortDescription() +
				"</a></td>");
			out.println("<td>" + I18N.instance().getBugCategoryDescription(bugPattern.getCategory()) + "</td></tr>");
		}

		out.println("</table>");
	}

	private void emitBugDescriptions() {
		PrintStream out = getPrintStream();

		out.println("<h2>Descriptions</h2>");

		for (Iterator<BugPattern> i = bugPatternSet.iterator(); i.hasNext(); ) {
			BugPattern bugPattern = i.next();
			out.println("<h3><a name=\"" +
				bugPattern.getType() + "\">" +
				bugPattern.getAbbrev() + ": " + bugPattern.getShortDescription() + "</a></h3>");
			out.println(bugPattern.getDetailText());
		}
	}

	public static void main(String[] args) throws Exception {
		String docTitle = "FindBugs Bug Descriptions";
		if (args.length > 0)
			docTitle = args[0];
		new PrettyPrintBugDescriptions(docTitle, System.out).print();
	}
}

// vim:ts=3
