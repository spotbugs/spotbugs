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

/**
 * A BugPattern object collects all of the metadata for a particular
 * species of BugInstance.  Specifically, it stores the human-readable
 * text for displaying a bug instance.  BugPatterns derive from the
 * BugPattern elements in the "findbugs.xml" and "messages.xml"
 * found in a FindBugs plugin.
 *
 * @author David Hovemeyer
 * @see BugInstance
 */
public class BugPattern implements Comparable<BugPattern> {
	final private String type;
	final private String abbrev;
	final private String category;
	final private boolean experimental;
	final private String shortDescription;
	final private String longDescription;
	final private String detailText;
	 private String detailHTML;

	/**
	 * Constructor.
	 *
	 * @param type             the type (species) of BugInstance
	 * @param abbrev           the abbreviation or "bug code"; see {@link BugCode}
	 * @param category         the category
	 * @param experimental     true if the bug pattern is experimental
	 * @param shortDescription short one-line description of the bug species
	 * @param longDescription  longer one-line description; may contain placeholders
	 *                         for use by {@link FindBugsMessageFormat} to format BugAnnotations
	 * @param detailText       HTML text containing a full description of the bug species
	 */
	public BugPattern(String type, String abbrev, String category, boolean experimental,
					  String shortDescription, String longDescription, String detailText) {
		this.type = type;
		this.abbrev = abbrev;
		this.category = category;
		this.experimental = experimental;
		this.shortDescription = shortDescription;
		this.longDescription = longDescription;
		this.detailText = detailText;
	}

	 static final BugPattern REALLY_UNKNOWN = new BugPattern("REALLY_UNKNOWN",
			"TEST","CORRECTNESS",false,"Unknown warning; core bug patterns not found",
			"Unknown warning BUG_PATTERN in {1}; core bug patterns not found","<p>A warning was recorded, but findbugs can't find the description of this bug pattern "
+"and so can't describe it. This should occur only in cases of a bug in FindBugs or its configuration, "
+ "or perhaps if an analysis was generated using a plugin, but that plugin is not currently loaded. "
+ "</p>");
	/**
	 * Get the type (species).
	 */
	public String getType() {
		return type;
	}

	/**
	 * Get the abbreviation or "bug code".
	 */
	public String getAbbrev() {
		return abbrev;
	}

	/**
	 * Get the category.
	 */
	public String getCategory() {
		return category;
	}

	public String getCategoryAbbrev() {
		String categoryAbbrev = null;
		BugCategory bcat = I18N.instance().getBugCategory(getCategory());
		if (bcat != null)
			categoryAbbrev = bcat.getAbbrev();
		if (categoryAbbrev == null)
			categoryAbbrev = TextUIBugReporter.OTHER_CATEGORY_ABBREV;
		return categoryAbbrev;
	}

	/**
	 * Is the bug pattern experimental?
	 */
	public boolean isExperimental() {
		return experimental;
	}

	/**
	 * Get the short description.
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Get the long description.
	 */
	public String getLongDescription() {
		return longDescription;
	}

	/**
	 * Get the HTML detail text describing the bug.
	 */
	public String getDetailText() {
		return detailText;
	}

	/**
	 * Get the detail text as a complete HTML document.
	 */
	public String getDetailHTML() {
		if (detailHTML == null) {
			StringBuffer buf = new StringBuffer();
			buf.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");
			buf.append("<HTML><HEAD><TITLE>");
			buf.append(getShortDescription());
			buf.append("</TITLE></HEAD><BODY><H1>");
			buf.append(getShortDescription());
			buf.append("</H1>\n");
			buf.append(getDetailText());
			buf.append("</BODY></HTML>\n");
			detailHTML = buf.toString();
		}
		return detailHTML;
	}

	public int compareTo(BugPattern other) {
		return type.compareTo(other.type);
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof BugPattern))
			return false;
		BugPattern other = (BugPattern) o;
		return type.equals(other.type);
	}

}

// vim:ts=4
