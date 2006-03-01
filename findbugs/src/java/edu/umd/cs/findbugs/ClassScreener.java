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

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to pre-screen class files, so that only a subset are
 * analyzed.  This supports the -onlyAnalyze command line option.
 *
 * Modified February 2006 in four ways:
 * a) don't break windows platform by hard-coding '/' as the directory separator
 * b) store list of Matchers, not Patterns, so we don't keep instantiating Matchers
 * c) fix suffix bug, so FooBar and Foo$Bar no longer match Bar
 * d) addAllowedPackage() can now handle unicode chars in filenames, though we
 *    still may not be handling every case mentioned in section 7.2.1 of the JLS
 *
 * @see FindBugs
 * @author David Hovemeyer
 */
public class ClassScreener {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.classscreener.debug");
	
	/** regular expression fragment to match a directory separator. note: could use
	 *  File.separatorChar instead, but that could be argued to be not general enough */
	private static final String SEP = "[/\\\\]"; // could include ':' for classic macOS
	private static final String START = "(?:^|"+SEP+")"; // (?:) is a non-capturing group

	private LinkedList<Matcher> patternList;

	/**
	 * Constructor.
	 * By default, the ClassScreener will match <em>all</em> class files.
	 * Once addAllowedClass() and addAllowedPackage() are called,
	 * the ClassScreener will only match the explicitly specified classes
	 * and packages.
	 */
	public ClassScreener() {
		this.patternList = new LinkedList<Matcher>();
	}

	/** replace the dots in a fully-qualified class/package name to a
	 *  regular expression fragment that will match file names.
	 * @param dotsName such as "java.io" or "java.io.File"
	 * @return regex fragment such as "java[/\\\\]io" (single backslash escaped twice)
	 */
	private static String dotsToRegex(String dotsName) {
		return dotsName.replace("$", "\\$").replace(".", SEP);
		// note: The original code used the \Q and \E regex quoting constructs to escape $.
	}
	
	/**
	 * Add the name of a class to be matched by the screener.
	 *
	 * @param className name of a class to be matched
	 */
	public void addAllowedClass(String className) {
		String classRegex = START+dotsToRegex(className)+".class$";
		if (DEBUG) System.out.println("Class regex: " + classRegex);
		patternList.add(Pattern.compile(classRegex).matcher(""));
	}

	/**
	 * Add the name of a package to be matched by the screener.
	 * All class files that appear to be in the package should be matched.
	 *
	 * @param packageName name of the package to be matched
	 */
	public void addAllowedPackage(String packageName) {
		if (packageName.endsWith(".")) {
			packageName = packageName.substring(0, packageName.length() - 1);
		}
		
		String packageRegex = START+dotsToRegex(packageName)+SEP+"\\p{javaJavaIdentifierPart}+.class$";
		if (DEBUG) System.out.println("Package regex: " + packageRegex);
		patternList.add(Pattern.compile(packageRegex).matcher(""));
	}

	/**
	 * Add the name of a prefix to be matched by the screener.
	 * All class files that appear to be in the package specified
	 * by the prefix, or a more deeply nested package, should be matched.
	 *
	 * @param prefix name of the prefix to be matched
	 */
	public void addAllowedPrefix(String prefix) {
		if (prefix.endsWith(".")) {
			prefix = prefix.substring(0, prefix.length()-1);
		}
		if (DEBUG) System.out.println("Allowed prefix: " + prefix);
		String packageRegex = START+dotsToRegex(prefix)+SEP;
		if (DEBUG) System.out.println("Prefix regex: " + packageRegex);
		patternList.add(Pattern.compile(packageRegex).matcher(""));
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
		for (Matcher matcher : patternList) {
			if (DEBUG) System.out.print("\tTrying [" + matcher.pattern());
			matcher.reset(fileName);
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
