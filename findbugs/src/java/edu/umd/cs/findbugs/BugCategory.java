/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs;

/**
 * A BugCategory object collects all of the metadata
 * for a category of bugs. BugCategories derive from
 * the BugCategory elements in messages*.xml files.
 */
public class BugCategory implements Comparable {
	final private String category;
	final private String shortDescription;
	 private String abbrev;
	 private String detailText;

	/**
	 * Constructor.
	 *
	 * @param category         the category
	 * @param shortDescription short (a word or three) description of the bug species
	 * @param abbrev           the abbreviation (typically a single capital letter)
	 * @param detailText       full description of the bug category (no HTML markup, may be null)
	 */
	public BugCategory(String category, String shortDescription, String abbrev, String detailText) {
		this.category = category;
		this.shortDescription = shortDescription;
		this.abbrev = abbrev;
		this.detailText = detailText;
	}

	/**
	 * Constructor.
	 *
	 * @param category         the category
	 * @param shortDescription short (a word or three) description of the bug species
	 */
	public BugCategory(String category, String shortDescription) {
		this(category, shortDescription, null, null);
	}

	/**
	 * Get the category.
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Get the short description (usually a word or three)
	 */
	public String getShortDescription() {
		return shortDescription;
	}

	/**
	 * Get the abbreviation (usually a single capital letter).
	 * May be null, but shouldn't be if the XML is correct.
	 */
	public String getAbbrev() {
		return abbrev;
	}

	/**
	 * Get the detail text describing the category.
	 * note: no HTML markup allowed, may be null
	 */
	public String getDetailText() {
		return detailText;
	}

	/**
	 * Set the abbreviation (typically a single capital letter)
	 */
	public void setAbbrev(String abbrev) {
		this.abbrev = abbrev;
	}

	/**
	 * Set the detail text describing the category.
	 * note: no HTML markup allowed, may be null
	 */
	public void setDetailText(String detailText) {
		this.detailText = detailText;
	}


	public int compareTo(Object o) {
		if (!(o instanceof BugCategory)) return -1;
		BugCategory other = (BugCategory)o;
		return category.compareTo(other.category);
	}

	public int hashCode() {
		return category.hashCode();
	}

	public boolean equals(Object o) {
		if (!(o instanceof BugCategory)) return false;
		BugCategory other = (BugCategory)o;
		return category.equals(other.category);
	}

	/** suitable for debugging. will be ugly if detailText has multiple lines */
	public String toString() {
		return "BugCategory["+category+"]{short="+shortDescription+",abbrev="+abbrev+",details="+detailText+'}';
	}
	
}
