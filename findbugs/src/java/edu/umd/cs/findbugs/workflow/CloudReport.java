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
import java.io.PrintWriter;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.charsets.UTF8;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 *
 * @author William Pugh
 */

public class CloudReport {

    /**
     *
     */
    private static final String USAGE = "Usage: <cmd> " + "  [<bugs.xml>]";

    public static void main(String[] args) throws IOException, DocumentException {

        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance();
        if (args.length > 1) {
            System.out.println(USAGE);
            return;
        }

        BugCollection bugs = new SortedBugCollection();
        if (args.length == 0) {
            bugs.readXML(System.in);
        } else {
            bugs.readXML(args[0]);
        }
        bugs.getCloud().waitUntilIssueDataDownloaded();
        PrintWriter out = UTF8.printWriter(System.out);
        bugs.getCloud().printCloudSummary(out, bugs, new String[0]);
        out.close();

    }
}
