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

import java.util.Iterator;
import java.util.LinkedList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to pre-screen class files, so that only a subset are
 * analyzed.  This supports the -onlyAnalyze command line option.
 *
 * @see FindBugs
 * @author David Hovemeyer
 */
public class ClassScreener {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.classscreener.debug");

	private LinkedList<Pattern> patternList;

	/**
	 * Constructor.
	 * By default, the ClassScreener will match <em>all</em> class files.
	 * Once addAllowedClass() and addAllowedPackage() are called,
	 * the ClassScreener will only match the explicitly specified classes
	 * and packages.
	 */
	public ClassScreener() {
		this.patternList = new LinkedList<Pattern>();
	}

	/**
	 * Add the name of a class that should be matched by the screener.
	 *
	 * @param className name of a class that should be matched
	 */
	public void addAllowedClass(String className) {
		String classRegex = "\\Q" + className.replace('.', '/') + "\\E\\.class$";
		if (DEBUG) System.out.println("Class regex: " + classRegex);
		patternList.add(Pattern.compile(classRegex));
	}

	/**
	 * Add the name of a package that should be matched by the screener.
	 * All class files that appear to be in the package should be matched.
	 *
	 * @param packageName name of the package that should be matched
	 */
	public void addAllowedPackage(String packageName) {
		// Note: \u0024 is the dollar sign ("$")
		String packageRegex = "\\Q" + packageName.replace('.', '/') + "\\E" +
			"\\/[A-Za-z_\\u0024][A-Za-z_\\u0024\\d]*\\.class$";
		if (DEBUG) System.out.println("Package regex: " + packageRegex);
		patternList.add(Pattern.compile(packageRegex));
	}

	/**
	 * Add the name of a package that should be matched by the screener.
	 * All class files that appear to be in the package should be matched.
	 *
	 * @param packageName name of the package that should be matched
	 */
	public void addAllowedPrefix(String prefix) {
		if (DEBUG) System.out.println("Allowed prefix: " + prefix);
		// Note: \u0024 is the dollar sign ("$")
		String packageRegex = "\\Q" + prefix.replace('.', '/') + "\\E";
		if (DEBUG) System.out.println("Package regex: " + packageRegex);
		patternList.add(Pattern.compile(packageRegex));
	}

	/**
	 * Return whether or not the name of the given file matches.
	 */
	public boolean matches(String fileName) {
		// Special case: if no classes or packages have been defined,
		// then the screener matches all class files.
		if (patternList.isEmpty())
			return true;

		if (DEBUG) System.out.println("Matching: " + fileName);

		// Scan through list of regexes
		for (Iterator<Pattern> i = patternList.iterator(); i.hasNext(); ) {
			Pattern pattern = i.next();
			if (DEBUG) System.out.print("\tTrying [" + pattern.toString());
			Matcher matcher = pattern.matcher(fileName);
			if (matcher.find()) {
				if (DEBUG) System.out.println("]: yes!");
				return true;
			}
			if (DEBUG) System.out.println("]: no");
		}
		return false;
	}
}

// vim:ts=4
