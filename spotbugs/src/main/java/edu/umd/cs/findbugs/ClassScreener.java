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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to pre-screen class files, so that only a subset are analyzed. This
 * supports the -onlyAnalyze command line option.
 *
 * Modified February 2006 in four ways: a) don't break windows platform by
 * hard-coding '/' as the directory separator b) store list of Matchers, not
 * Patterns, so we don't keep instantiating Matchers c) fix suffix bug, so
 * FooBar and Foo$Bar no longer match Bar d) addAllowedPackage() can now handle
 * unicode chars in filenames, though we still may not be handling every case
 * mentioned in section 7.2.1 of the JLS
 *
 * @see FindBugs
 * @author David Hovemeyer
 */
public class ClassScreener implements IClassScreener {
    private static final Logger LOG = LoggerFactory.getLogger(ClassScreener.class);

    /**
     * regular expression fragment to match a directory separator. note: could
     * use File.separatorChar instead, but that could be argued to be not
     * general enough
     */
    private static final String SEP = "[/\\\\]"; // could include ':' for
    // classic macOS

    private static final String START = "(?:^|" + SEP + ")"; // (?:) is a
    // non-capturing
    // group

    /**
     * regular expression fragment to match a char of a class or package name.
     * Actually, we just allow any char except a dot or a directory separator.
     */
    private static final String JAVA_IDENTIFIER_PART = "[^./\\\\]";

    private final LinkedList<Matcher> includePatternList;

    private final LinkedList<Matcher> excludePatternList;

    /**
     * Constructor. By default, the ClassScreener will match <em>all</em> class
     * files. Once addAllowedClass() and addAllowedPackage() are called, the
     * ClassScreener will only match the explicitly included classes and
     * packages unless explicitly excluded.
     */
    public ClassScreener() {
        includePatternList = new LinkedList<>();
        excludePatternList = new LinkedList<>();
    }

    /**
     * replace the dots in a fully-qualified class/package name to a regular
     * expression fragment that will match file names.
     *
     * @param dotsName
     *            such as "java.io" or "java.io.File"
     * @return regex fragment such as "java[/\\\\]io" (single backslash escaped
     *         twice)
     */
    private static String dotsToRegex(String dotsName) {
        /*
         * oops, next line requires JDK 1.5 return dotsName.replace("$",
         * "\\$").replace(".", SEP); could use String.replaceAll(regex, repl)
         * but that can be problematic--javadoc says "Note that backslashes (\)
         * and dollar signs ($) in the replacement string may cause the results
         * to be different than if it were being treated as a literal
         * replacement"
         */
        String tmp = dotsName.replace("$", "\\$");
        return tmp.replace(".", SEP);
        // note: The original code used the \Q and \E regex quoting constructs
        // to escape $.
    }

    /**
     * Add the name of a class to be matched by the screener.
     *
     * @param className
     *            name of a class to be matched
     */
    public void addAllowedClass(String className) {
        LOG.debug("Allowed class: {}", className);
        if (className.startsWith("!")) {
            excludePatternList.add(classMatcher(className.substring(1)));
        } else {
            includePatternList.add(classMatcher(className));
        }
    }

    private static Matcher classMatcher(String className) {
        String classRegex = START + dotsToRegex(className) + ".class$";
        LOG.debug("Class regex: {}", classRegex);
        return Pattern.compile(classRegex).matcher("");
    }

    /**
     * Add the name of a package to be matched by the screener. All class files
     * that appear to be in the package should be matched.
     *
     * @param packageName
     *            name of the package to be matched
     */
    public void addAllowedPackage(String packageName) {
        if (packageName.endsWith(".")) {
            packageName = packageName.substring(0, packageName.length() - 1);
        }
        LOG.debug("Allowed package: {}", packageName);
        if (packageName.startsWith("!")) {
            excludePatternList.add(packageMatcher(packageName.substring(1)));
        } else {
            includePatternList.add(packageMatcher(packageName));
        }
    }

    private static Matcher packageMatcher(String packageName) {
        String packageRegex = START + dotsToRegex(packageName) + SEP + JAVA_IDENTIFIER_PART + "+.class$";
        LOG.debug("Package regex: {}", packageRegex);
        return Pattern.compile(packageRegex).matcher("");
    }

    /**
     * Add the name of a prefix to be matched by the screener. All class files
     * that appear to be in the package specified by the prefix, or a more
     * deeply nested package, should be matched.
     *
     * @param prefix
     *            name of the prefix to be matched
     */
    public void addAllowedPrefix(String prefix) {
        if (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        LOG.debug("Allowed prefix: {}", prefix);
        if (prefix.startsWith("!")) {
            excludePatternList.add(prefixMatcher(prefix.substring(1)));
        } else {
            includePatternList.add(prefixMatcher(prefix));
        }
    }

    private static Matcher prefixMatcher(String prefix) {
        String packageRegex = START + dotsToRegex(prefix) + SEP;
        LOG.debug("Prefix regex: {}", packageRegex);
        return Pattern.compile(packageRegex).matcher("");
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.IClassScreener#matches(java.lang.String)
     */
    @Override
    public boolean matches(String fileName) {
        // First go through exclusions if any
        if (!excludePatternList.isEmpty()) {
            LOG.debug("Matching negative: {}", fileName);

            for (Matcher matcher : excludePatternList) {
                matcher.reset(fileName);
                if (matcher.find()) {
                    LOG.debug("\\tTrying not [{}]: yes!", matcher.pattern());
                    return false;
                }
                LOG.debug("\\tTrying not [{}]: no", matcher.pattern());
            }
        }

        // Special case: if no classes or packages have been defined,
        // then the screener matches all class files.
        if (includePatternList.isEmpty()) {
            return true;
        }

        LOG.debug("Matching: {}", fileName);

        for (Matcher matcher : includePatternList) {
            matcher.reset(fileName);
            if (matcher.find()) {
                LOG.debug("\\tTrying [{}]: yes!", matcher.pattern());
                return true;
            }
            LOG.debug("\\tTrying [{}]: no", matcher.pattern());
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.IClassScreener#vacuous()
     */
    @Override
    public boolean vacuous() {
        return includePatternList.isEmpty() && excludePatternList.isEmpty();
    }
}
