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

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;
import java.io.Serializable;
import java.util.ResourceBundle;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Singleton responsible for returning localized strings for information
 * returned to the user.
 *
 * @author David Hovemeyer
 */
public class I18N {

	/** a Comparator to compare user designation keys */
	public static final Comparator<String> designationKeyComparator = new DesignationKeyComparator();

	private final ResourceBundle annotationDescriptionBundle;
	private final ResourceBundle bugCategoryDescriptionBundle;
	private final ResourceBundle userDesignationBundle;
	private final HashMap<String, BugPattern> bugPatternMap;
	private final HashMap<String, BugCode> bugCodeMap;

	private I18N() {
		annotationDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.FindBugsAnnotationDescriptions");
		bugCategoryDescriptionBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.BugCategoryDescriptions");
		userDesignationBundle = ResourceBundle.getBundle("edu.umd.cs.findbugs.UserDesignations");
		bugPatternMap = new HashMap<String, BugPattern>();
		bugCodeMap = new HashMap<String, BugCode>();
	}

	private static final I18N theInstance = new I18N();

	/**
	 * Get the single object instance.
	 */
	public static I18N instance() {
		return theInstance;
	}

	/**
	 * Register a BugPattern.
	 *
	 * @param bugPattern the BugPattern
	 */
	public void registerBugPattern(BugPattern bugPattern) {
		bugPatternMap.put(bugPattern.getType(), bugPattern);
	}

	/**
	 * Look up bug pattern.
	 *
	 * @param bugType the bug type for the bug pattern
	 * @return the BugPattern, or null if it can't be found
	 */
	public @CheckForNull BugPattern lookupBugPattern(String bugType) {
		return bugPatternMap.get(bugType);
	}

	/**
	 * Get an Iterator over all registered bug patterns.
	 */
	public Iterator<BugPattern> bugPatternIterator() {
		return bugPatternMap.values().iterator();
	}

	/**
	 * Register a BugCode.
	 *
	 * @param bugCode the BugCode
	 */
	public void registerBugCode(BugCode bugCode) {
		bugCodeMap.put(bugCode.getAbbrev(), bugCode);
	}

	/**
	 * Get a message string.
	 * This is a format pattern for describing an entire bug instance in a single line.
	 *
	 * @param key which message to retrieve
	 */
	public @NonNull String getMessage(String key) {
		BugPattern bugPattern = bugPatternMap.get(key);
		if (bugPattern == null)
			return "Error: missing bug pattern for key " + key;
		return bugPattern.getAbbrev() + ": " + bugPattern.getLongDescription();
	}

	/**
	 * Get a short message string.
	 * This is a concrete string (not a format pattern) which briefly describes
	 * the type of bug, without mentioning particular a particular class/method/field.
	 *
	 * @param key which short message to retrieve
	 */
	public @NonNull String getShortMessage(String key) {
		BugPattern bugPattern = bugPatternMap.get(key);
		if (bugPattern == null)
			return "Error: missing bug pattern for key " + key;
		return bugPattern.getAbbrev() + ": " + bugPattern.getShortDescription();
	}

	/**
	 * Get an HTML document describing the bug pattern for given key in detail.
	 *
	 * @param key which HTML details for retrieve
	 */
	public @NonNull String getDetailHTML(String key) {
		BugPattern bugPattern = bugPatternMap.get(key);
		if (bugPattern == null)
			return "Error: missing bug pattern for key " + key;
		return bugPattern.getDetailHTML();
	}

	/**
	 * Get an annotation description string.
	 * This is a format pattern which will describe a BugAnnotation in the
	 * context of a particular bug instance.  Its single format argument
	 * is the BugAnnotation.
	 *
	 * @param key the annotation description to retrieve
	 */
	public String getAnnotationDescription(String key) {
		return annotationDescriptionBundle.getString(key);
	}

	/**
	 * Get a description for given "bug type".
	 * FIXME: this is referred to elsewhere as the "bug code" or "bug abbrev".
	 * Should make the terminology consistent everywhere.
	 * In this case, the bug type refers to the short prefix code prepended to
	 * the long and short bug messages.
	 *
	 * @param shortBugType the short bug type code
	 * @return the description of that short bug type code means
	 */
	public @NonNull String getBugTypeDescription(String shortBugType) {
		BugCode bugCode = bugCodeMap.get(shortBugType);
		if (bugCode == null)
			return "Error: missing bug code for key " + shortBugType;
		return bugCode.getDescription();
	}

	/**
	 * Get the description of a bug category.
	 * Returns the category if no description can be found.
	 *
	 * @param category the category
	 * @return the description of the category
	 */
	public String getBugCategoryDescription(String category) {
		return bugCategoryDescriptionBundle.getString(category);
	}

	/**
	 * Get a Collection containing all known bug category keys.
	 * E.g., "CORRECTNESS", "MT_CORRECTNESS", "PERFORMANCE", etc.
	 *
	 * @return Collection of bug category keys.
	 */
	public Collection<String> getBugCategories() {
		List<String> result = new LinkedList<String>();
		for (Enumeration<String> e = bugCategoryDescriptionBundle.getKeys(); e.hasMoreElements(); ) {
			String key = e.nextElement();
			result.add(key);
		}
		return result;
	}

	/**
	 * Get the localized user designation string.
	 * Returns the key if no user designation can be found.
	 *
	 * @param key the user designation key
	 * @return the localized designation string
	 */
	public String getUserDesignation(String key) {
		return userDesignationBundle.getString(key);
	}

	/**
	 * Get a List containing all known user designation keys keys.
	 * E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 *
	 * @return List of user designation keys
	 */
	public List<String> getUserDesignations() {
		List<String> result = new LinkedList<String>();
		for (Enumeration<String> e = userDesignationBundle.getKeys(); e.hasMoreElements(); ) {
			String key = e.nextElement();
			result.add(key);
		}
		return result;
	}

	/**
	 * Get a List containing all known user designation keys keys.
	 * E.g., "MOSTLY_HARMLESS", "CRITICAL", "NOT_A_BUG", etc.
	 * 
	 * If <code>sort == true</code> then it will attempt to sort
	 * the List as appropriate to show the user.
	 *
	 * @return List of user designation keys
	 */
	public List<String> getUserDesignations(boolean sort) {
		List<String> result = getUserDesignations();
		if (sort) Collections.sort(result, designationKeyComparator);
		return result;
	}


	private static class DesignationKeyComparator implements Comparator<String>, Serializable {
		private static final long serialVersionUID = 1L;
		/** Returns a negative integer, zero, or a positive integer as the
		 * left key is less than, equal to, or greater than the right key. */
		public int compare(String lKey, String rKey) {
			int lCat = categoryOf(lKey);
			int catDiff = lCat - categoryOf(rKey);
			if (catDiff != 0 || lCat != 0) return catDiff;
			// if we get this far we have two unrecognized strings
			return lKey.compareTo(rKey);
		}
		private static int categoryOf(String key) {
			if (key == null) return -30;
			if (key.length() <= 0) return -29;
			switch (key.charAt(0)) {
				case 'U': if ("UNCLASSIFIED".equals(key)) return -25;
				          break;
				case 'B': if ("BAD_ANALYSIS".equals(key)) return -20;
				          break;
				case 'N': if ("NOT_A_BUG".equals(key)) return -15;
				          break;
				case 'H': if ("HARMLESS".equals(key)) return -11;
				          break;
				case 'M': if ("MOSTLY_HARMLESS".equals(key)) return -10;
				          if ("MODERATE".equals(key)) return -5;
				          break;
				case 'S': if ("SERIOUS".equals(key)) return 5;
				          break;
				case 'C': if ("CRITICAL".equals(key)) return 10;
				          break;
			}
			return 0; // between MODERATE and SERIOUS
		}
	}

}

// vim:ts=4
