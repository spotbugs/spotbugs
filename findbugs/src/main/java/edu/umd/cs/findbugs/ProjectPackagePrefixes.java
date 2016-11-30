/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public class ProjectPackagePrefixes {

    public static class PrefixFilter {
        final String[] parts;

        PrefixFilter(String prefixes) {
            prefixes = prefixes.replace('/', '.').trim();
            if (prefixes.length() == 0) {
                parts = new String[0];
            } else {
                parts = prefixes.split("[ ,:]+");
            }
        }

        boolean matches(@DottedClassName String className) {
            if (parts.length == 0) {
                return true;
            }
            for (String p : parts) {
                if (p.length() > 0 && className.startsWith(p)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            String result = Arrays.asList(parts).toString();
            return result.substring(1, result.length() - 1);
        }

    }

    public int size() {
        return map.size();
    }

    Map<String, PrefixFilter> map = new HashMap<String, PrefixFilter>();

    Map<Set<String>, Integer> count = new HashMap<Set<String>, Integer>();

    Map<String, Integer> missingProjectCount = new TreeMap<String, Integer>();

    Map<String, Integer> rawPackageCount = new TreeMap<String, Integer>();

    int totalCount = 0;

    public void countBug(BugInstance b) {
        String packageName = b.getPrimaryClass().getPackageName();

        countPackageMember(packageName);
    }

    /**
     * @param packageName
     */
    public void countPackageMember(String packageName) {
        totalCount++;
        TreeSet<String> results = getProjects(packageName);
        incrementCount(count, results);
        incrementCount(rawPackageCount, packageName);
        if (results.size() == 0) {
            incrementCount(missingProjectCount, packageName);
        }
    }

    public PrefixFilter getFilter(String projectName) {
        return map.get(projectName);
    }

    public TreeSet<String> getProjects(@DottedClassName String className) {
        TreeSet<String> results = new TreeSet<String>();
        for (Map.Entry<String, PrefixFilter> e : map.entrySet()) {
            if (e.getValue().matches(className)) {
                results.add(e.getKey());
            }
        }
        return results;
    }

    static <T> void incrementCount(Map<T, Integer> counter, T t) {
        incrementCount(counter, t, 1);
    }

    static <T> void incrementCount(Map<T, Integer> counter, T t, int valueToAdd) {
        Integer v = counter.get(t);
        if (v == null) {
            counter.put(t, valueToAdd);
        } else {
            counter.put(t, v + valueToAdd);
        }
    }

    static final Pattern FORBIDDEN_PACKAGE_PREFIXES = Pattern.compile(SystemProperties.getProperty(
            "findbugs.forbiddenPackagePrefixes", " none ").replace(',', '|'));

    public void report() {
        System.out.println("# of items counted: " + totalCount);
        System.out.println("# of projects: " + size());
        System.out.println("By package: ");

        for (Map.Entry<String, Integer> e : rawPackageCount.entrySet()) {
            String packageName = e.getKey();
            if (e.getValue() > 5) {
                System.out.printf("%5d %s%n", e.getValue(), packageName);
            }
        }
        System.out.println("Count by project");

        for (Map.Entry<Set<String>, Integer> e : count.entrySet()) {
            Set<String> projects = e.getKey();
            if (e.getValue() > 5) {
                System.out.printf("%5d %s%n", e.getValue(), projects);
            }
        }
        System.out.println("Count by package for items not associated with a project");

        Set<String> packages = missingProjectCount.keySet();
        for (int count = 0; count < 3; count++) {
            HashSet<String> extraSuperPackages = new HashSet<String>();

            for (String p1 : packages) {
                int num = missingProjectCount.get(p1);
                if (num < 3) {
                    int x = p1.lastIndexOf('.');
                    String p2 = p1.substring(0, x);
                    if (FORBIDDEN_PACKAGE_PREFIXES.matcher(p2).matches()) {
                        continue;
                    }

                    extraSuperPackages.add(p2);
                }
            }
            for (String p1 : extraSuperPackages) {
                missingProjectCount.put(p1, 0);
            }

            for (Iterator<String> i = packages.iterator(); i.hasNext();) {
                String p1 = i.next();
                int num = missingProjectCount.get(p1);

                for (String p2 : packages) {
                    if (p2.length() < p1.length() && p1.startsWith(p2)) {
                        // p1 is a subpackage of p2
                        // System.out.printf("%s is a subpackage of %s\n", p1,
                        // p2);
                        i.remove();
                        incrementCount(missingProjectCount, p2, num);
                        break;
                    }
                }

            }
        }

        System.out.println("Count of missing files in packages not associated with a project");
        for (Map.Entry<String, Integer> e : missingProjectCount.entrySet()) {
            if (e.getValue() > 5) {
                System.out.printf("%5d %s%n", e.getValue(), e.getKey());
            }
        }
    }

    public ProjectPackagePrefixes() {
        URL u = DetectorFactoryCollection.getCoreResource("projectPaths.properties");
        if (u != null) {

            BufferedReader in = null;
            try {
                in = UTF8.bufferedReader(u.openStream());
                while (true) {
                    String s = in.readLine();
                    if (s == null) {
                        break;
                    }
                    String[] parts = s.split("=");
                    if (parts.length == 2 && !map.containsKey(parts[0])) {
                        map.put(parts[0], new PrefixFilter(parts[1]));
                    }
                }
            } catch (IOException e1) {

                AnalysisContext.logError("Error loading projects paths", e1);
            } finally {
                Util.closeSilently(in);
            }

        }

    }

}
