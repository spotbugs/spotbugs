/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.util.Bag;

public class TreemapVisualization {

    HashSet<String> buggyPackages = new HashSet<String>();

    HashSet<String> interiorPackages = new HashSet<String>();

    Bag<String> goodCodeSize = new Bag<String>(new TreeMap<String, Integer>());

    Bag<String> goodCodeCount = new Bag<String>(new TreeMap<String, Integer>());

    public void addInteriorPackages(String packageName) {
        String p = superpackage(packageName);
        if (p.length() > 0) {
            interiorPackages.add(p);
            addInteriorPackages(p);
        }
    }

    private static String superpackage(String packageName) {
        int i = packageName.lastIndexOf('.');
        if (i == -1) {
            return "";
        }
        String p = packageName.substring(0, i);
        return p;
    }

    public boolean isInteriorPackage(String packageName) {
        return interiorPackages.contains(packageName);
    }

    public void cleanCode(String packageName, int loc, int classes) {
        String superpackage = superpackage(packageName);
        if (buggyPackages.contains(superpackage) || interiorPackages.contains(superpackage) || superpackage.length() == 0) {
            goodCodeCount.add(packageName, classes);
            goodCodeSize.add(packageName, loc);
            if (superpackage.length() > 0) {
                interiorPackages.add(superpackage);
            }

        } else {
            cleanCode(superpackage, loc, classes);
        }
    }

    public void generateTreeMap(BugCollection bugCollection) {
        for (PackageStats p : bugCollection.getProjectStats().getPackageStats()) {
            if (p.getTotalBugs() > 0) {
                buggyPackages.add(p.getPackageName());
                addInteriorPackages(p.getPackageName());

            }
        }

        for (PackageStats p : bugCollection.getProjectStats().getPackageStats()) {
            if (p.getTotalBugs() == 0) {
                cleanCode(p.getPackageName(), p.size(), p.getClassStats().size());
            }
        }
        System.out.println("LOC\tTypes\tH\tHM\tDensity");
        System.out.println("INTEGER\tINTEGER\tINTEGER\tINTEGER\tFLOAT");
        for (PackageStats p : bugCollection.getProjectStats().getPackageStats()) {
            if (p.getTotalBugs() > 0) {
                int high = p.getBugsAtPriority(Priorities.HIGH_PRIORITY);
                int normal = p.getBugsAtPriority(Priorities.NORMAL_PRIORITY);
                System.out.printf("%d\t%d\t%d\t%d\t%g\t\t%s", p.size(), p.getClassStats().size(), high, high + normal,
                        (high + normal) * 1000.0 / p.size(), p.getPackageName().substring(11).replace('.', '\t'));
                if (isInteriorPackage(p.getPackageName())) {
                    System.out.print("\t*");
                }
                System.out.println();
            }
        }
        for (Map.Entry<String, Integer> e : goodCodeSize.entrySet()) {
            System.out.printf("%d\t%d\t%d\t%d\t%g\t\t%s%n", e.getValue(), goodCodeCount.getCount(e.getKey()), 0, 0, 0.0, e
                    .getKey().substring(11).replace('.', '\t'));

        }

    }

    public static void main(String[] args) throws Exception {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance(); // load plugins

        SortedBugCollection bugCollection = new SortedBugCollection();
        int argCount = 0;
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }

        new TreemapVisualization().generateTreeMap(bugCollection);

    }
}
