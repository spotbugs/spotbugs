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

import java.util.ArrayList;
import java.util.Map;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.ProjectPackagePrefixes;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.util.Bag;

public class TestingGround {
    BugCollection bugCollection;

    public TestingGround() {
    }

    public TestingGround(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public void setBugCollection(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public TestingGround execute() {
        ProjectPackagePrefixes foo = new ProjectPackagePrefixes();

        for (BugInstance b : bugCollection.getCollection()) {
            foo.countBug(b);
        }
        foo.report();

        return this;
    }

    static class CommandLine extends edu.umd.cs.findbugs.config.CommandLine {

        @Override
        public void handleOption(String option, String optionalExtraPart) {
            throw new IllegalArgumentException("unknown option: " + option);
        }

        @Override
        public void handleOptionWithArgument(String option, String argument) {
            throw new IllegalArgumentException("unknown option: " + option);
        }
    }

    public static void main(String[] args) throws Exception {
        DetectorFactoryCollection.instance(); // load plugins

        CommandLine commandLine = new CommandLine();
        int argCount = commandLine.parse(args, 0, 2, "Usage: " + TestingGround.class.getName() + " [options] [<xml results>] ");

        SortedBugCollection bugCollection = new SortedBugCollection();
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }
        ArrayList<Bag<String>> live = new ArrayList<Bag<String>>();
        ArrayList<Bag<String>> died = new ArrayList<Bag<String>>();
        Bag<String> allBugs = new Bag<String>();
        for (int i = 0; i <= bugCollection.getSequenceNumber(); i++) {
            live.add(new Bag<String>());
            died.add(new Bag<String>());
        }
        for (BugInstance b : bugCollection) {
            int first = (int) b.getFirstVersion();
            int buried = (int) b.getLastVersion() + 1;
            int finish = buried;
            if (finish == 0) {
                finish = (int) bugCollection.getSequenceNumber();
            }

            String bugPattern = b.getBugPattern().getType();
            allBugs.add(bugPattern);

            for (int i = first; i <= finish; i++) {
                live.get(i).add(bugPattern);
            }
            if (buried > 0) {
                died.get(buried).add(bugPattern);
            }
        }
        for (int i = 0; i < bugCollection.getSequenceNumber(); i++) {
            for (Map.Entry<String, Integer> e : died.get(i).entrySet()) {
                Integer buried = e.getValue();
                int total = live.get(i).getCount(e.getKey());
                if (buried > 30 && buried * 3 > total) {
                    System.out.printf("%d/%d died at %d for %s%n", buried, total, i, e.getKey());
                }
            }

        }
        SortedBugCollection results = bugCollection.createEmptyCollectionWithMetadata();
        for (BugInstance b : bugCollection) {
            int buried = (int) b.getLastVersion() + 1;
            String bugPattern = b.getBugPattern().getType();

            if (buried > 0) {
                int buriedCount = died.get(buried).getCount(bugPattern);
                int total = live.get(buried).getCount(bugPattern);
                if (buriedCount > 30 && buriedCount * 3 > total) {
                    continue;
                }
            }
            int survied = live.get((int) bugCollection.getSequenceNumber()).getCount(bugPattern);
            if (survied == 0 && allBugs.getCount(bugPattern) > 100) {
                continue;
            }

            results.add(b, false);
        }
        if (argCount == args.length) {
            results.writeXML(System.out);
        } else {
            results.writeXML(args[argCount++]);

        }

    }
}
