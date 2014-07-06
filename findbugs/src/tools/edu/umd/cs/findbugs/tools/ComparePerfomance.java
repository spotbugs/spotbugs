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

package edu.umd.cs.findbugs.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.umd.cs.findbugs.xml.XMLUtil;

/**
 * @author pugh
 */
public class ComparePerfomance {

    final int num;
    final Map<String, int[]> performance = new TreeMap<String, int[]>();

    ComparePerfomance(String [] args) throws DocumentException, IOException {
        num = args.length;
        for(int i = 0; i < args.length; i++) {
            foo(new File(args[i]), i);
        }

    }


    public int[] getRecord(String className) {
        int [] result = performance.get(className);
        if (result != null) {
            return result;
        }
        result = new int[num];
        performance.put(className, result);
        return result;
    }

    public  void foo(File f, int i) throws DocumentException, IOException {
        Document doc;
        SAXReader reader = new SAXReader();

        String fName = f.getName();
        InputStream in = new FileInputStream(f);
        try {
            if (fName.endsWith(".gz")) {
                in = new GZIPInputStream(in);
            }
            doc = reader.read(in);
            Node summary = doc.selectSingleNode("/BugCollection/FindBugsSummary");
            double cpu_seconds = Double.parseDouble(summary.valueOf("@cpu_seconds"));
            putStats("cpu_seconds", i, (int) (cpu_seconds * 1000));
            double gc_seconds = Double.parseDouble(summary.valueOf("@gc_seconds"));
            putStats("gc_seconds", i, (int) (gc_seconds * 1000));

            List<Node> profileNodes = XMLUtil.selectNodes(doc, "/BugCollection/FindBugsSummary/FindBugsProfile/ClassProfile");
            for(Node n : profileNodes) {
                String name = n.valueOf("@name");
                int totalMilliseconds = Integer.parseInt(n.valueOf("@totalMilliseconds"));
                int invocations = Integer.parseInt(n.valueOf("@invocations"));
                putStats(name, i, totalMilliseconds);
                //            System.out.printf("%6d %10d %s%n", invocations, totalMilliseconds, simpleName);
            }
        } finally {
            in.close();
        }
    }


    /**
     * @param name
     * @param i
     * @param totalMilliseconds
     */
    public void putStats(String name, int i, int totalMilliseconds) {
        int [] stats = getRecord(name);
        stats[i] = totalMilliseconds;
    }

    public void print() {
        for(Map.Entry<String, int[]> e : performance.entrySet()) {
            String name = e.getKey();
            int lastDot = name.lastIndexOf('.');
            String simpleName = name.substring(lastDot+1);
            System.out.printf("%s,%s", name, simpleName);
            for(int x : e.getValue()) {
                System.out.printf(",%d", x);
            }
            System.out.println();
        }
    }


    public static void main(String args[])  throws Exception {
        ComparePerfomance p = new ComparePerfomance(args);
        p.print();
    }

}
