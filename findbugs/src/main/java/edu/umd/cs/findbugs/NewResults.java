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

import java.io.IOException;
import java.util.Iterator;

import org.dom4j.DocumentException;

@Deprecated
public class NewResults {
    private final SortedBugCollection origCollection;

    private final SortedBugCollection newCollection;

    public NewResults(String origFilename, String newFilename) throws IOException, DocumentException {
        this(new SortedBugCollection(), new SortedBugCollection());
        origCollection.readXML(origFilename);
        newCollection.readXML(newFilename);
    }

    public NewResults(SortedBugCollection origCollection, SortedBugCollection newCollection) {
        this.origCollection = origCollection;
        this.newCollection = newCollection;
    }

    public SortedBugCollection execute() {
        SortedBugCollection result = new SortedBugCollection();

        for (Iterator<BugInstance> i = newCollection.iterator(); i.hasNext();) {
            BugInstance bugInstance = i.next();

            if (!origCollection.contains(bugInstance)) {
                result.add(bugInstance);
            }
        }

        return result;
    }

    public static void main(String[] argv) throws Exception {
        if (argv.length != 3) {
            System.err.println("Usage: " + NewResults.class.getName() + " <orig results> <new results> <output file>");
            System.exit(1);
        }

        String origFilename = argv[0];
        String newFilename = argv[1];
        String outputFilename = argv[2];

        NewResults op = new NewResults(origFilename, newFilename);

        SortedBugCollection result = op.execute();

        result.writeXML(outputFilename);
    }
}

