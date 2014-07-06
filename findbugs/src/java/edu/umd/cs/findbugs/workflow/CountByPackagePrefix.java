/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 *
 * @author William Pugh
 */

public class CountByPackagePrefix {

    /**
     *
     */
    private static final String USAGE = "Usage: <cmd> " + " <prefixLength> [<bugs.xml>]";

    public static void main(String[] args) throws IOException, DocumentException {

        DetectorFactoryCollection.instance();
        if (args.length != 1 && args.length != 2) {
            System.out.println(USAGE);
            return;
        }

        int prefixLength = Integer.parseInt(args[0]);
        BugCollection origCollection = new SortedBugCollection();
        if (args.length == 1) {
            origCollection.readXML(System.in);
        } else {
            origCollection.readXML(args[1]);
        }
        Map<String, Integer> map = new TreeMap<String, Integer>();
        Map<String, Integer> ncss = new TreeMap<String, Integer>();

        for (BugInstance b : origCollection.getCollection()) {
            String prefix = ClassName.extractPackagePrefix(b.getPrimaryClass().getPackageName(), prefixLength);
            Integer v = map.get(prefix);
            if (v == null) {
                map.put(prefix, 1);
            } else {
                map.put(prefix, v + 1);
            }
        }
        for (PackageStats ps : origCollection.getProjectStats().getPackageStats()) {
            String prefix = ClassName.extractPackagePrefix(ps.getPackageName(), prefixLength);

            Integer v = ncss.get(prefix);
            if (v == null) {
                ncss.put(prefix, ps.size());
            } else {
                ncss.put(prefix, v + ps.size());
            }

        }
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            String prefix = e.getKey();
            int warnings = e.getValue();
            if (warnings == 0) {
                continue;
            }
            Integer v = ncss.get(prefix);
            if (v == null || v.intValue() == 0) {
                v = 1;
            }

            int density = warnings * 1000000 / v;
            if (warnings < 3 || v < 2000) {
                System.out.printf("%4s %4d %4d %s%n", " ", warnings, v / 1000, prefix);
            } else {
                System.out.printf("%4d %4d %4d %s%n", density, warnings, v / 1000, prefix);
            }
        }

    }
}
