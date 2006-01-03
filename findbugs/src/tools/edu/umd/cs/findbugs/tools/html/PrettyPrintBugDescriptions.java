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
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.I18N;

public class PrettyPrintBugDescriptions extends PlainPrintBugDescriptions {
	private Set<BugPattern> bugPatternSet;
	private String headerText;
	private String beginBodyText;
	private String prologueText;
	private String endBodyText;
	private boolean unabridged;

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
		this.headerText = this.beginBodyText = this.prologueText = this.endBodyText = "";
	}

	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public void setBeginBodyText(String beginBodyText) {
		this.beginBodyText = beginBodyText;
	}

	public void setPrologueText(String prologueText) {
		this.prologueText = prologueText;
	}

	public void setEndBodyText(String endBodyText) {
		this.endBodyText = endBodyText;
	}

	protected void prologue() throws IOException {
		super.prologue();
		PrintStream out = getPrintStream();
		out.println(prologueText);
	}

	protected void emit(BugPattern bugPattern) throws IOException {
		bugPatternSet.add(bugPattern);
	}

	protected void epilogue() throws IOException {
		emitSummaryTable();
		emitBugDescriptions();
		super.epilogue();
	}

	protected void header() throws IOException {
		PrintStream out = getPrintStream();
		out.println(headerText);
	}

	/** Extra stuff printed at the beginning of the &lt;body&gt; element. */
	protected void beginBody() throws IOException {
		PrintStream out = getPrintStream();
		out.println(beginBodyText);
	}

	/** Extra stuff printed at the end of the &lt;body&gt; element. */
	protected void endBody() throws IOException {
		PrintStream out = getPrintStream();
		out.println(endBodyText);
	}

	private void emitSummaryTable() {
		PrintStream out = getPrintStream();

		out.println("<h2>Summary</h2>");

		out.println("<table width=\"100%\">");

		out.println("<tr bgcolor=\"#b9b9fe\"><th>Description</th><th>Category</th></tr>");

		ColorAlternator colorAlternator = new ColorAlternator(TABLE_COLORS);

		for (BugPattern bugPattern : bugPatternSet) {
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

		for (BugPattern bugPattern : bugPatternSet) {
			out.println("<h3><a name=\"" +
					bugPattern.getType() + "\">" +
					bugPattern.getAbbrev() + ": " + bugPattern.getShortDescription() +
					" (" + bugPattern.getType() + ")" +
					"</a></h3>");
			out.println(bugPattern.getDetailText());
		}
	}

	protected boolean isEnabled(DetectorFactory factory) {
		return unabridged || super.isEnabled(factory);
	}

	public static void main(String[] args) throws Exception {
		int argCount = 0;
		boolean unabridged = false;
		
		if (argCount < args.length && args[argCount].equals("-unabridged")) {
			++argCount;
			// Unabridged mode: emit all warnings reported by at least one
			// detector, even for disabled detectors.
			unabridged = true;
		}
		
		if (Boolean.getBoolean("findbugs.bugdesc.unabridged")) {
			unabridged = true;
		}

		String docTitle = "FindBugs Bug Descriptions";
		if (argCount < args.length) {
			docTitle = args[argCount++];
		}
		PrettyPrintBugDescriptions pp = new PrettyPrintBugDescriptions(docTitle, System.out);
		
		if (argCount < args.length) {
			pp.setHeaderText(args[argCount++]);
		}
		if (argCount < args.length) {
			pp.setBeginBodyText(args[argCount++]);
		}
		if (argCount < args.length) {
			pp.setPrologueText(args[argCount++]);
		}
		if (argCount < args.length) {
			pp.setEndBodyText(args[argCount++]);
		}

		if (unabridged)
			pp.unabridged = true;

		pp.print();
	}
}

// vim:ts=3
