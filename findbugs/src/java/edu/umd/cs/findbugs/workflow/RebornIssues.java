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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.SortedBugCollection;

/**
 * Mine historical information from a BugCollection. The BugCollection should be
 * built using UpdateBugCollection to record the history of analyzing all
 * versions over time.
 *
 * @author David Hovemeyer
 * @author William Pugh
 */
public class RebornIssues {
    BugCollection bugCollection;

    public RebornIssues() {
    }

    public RebornIssues(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public void setBugCollection(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public RebornIssues execute() {

        Map<String, List<BugInstance>> map = new HashMap<String, List<BugInstance>>();
        for (BugInstance b : bugCollection.getCollection()) {
            if (b.getFirstVersion() != 0 || b.getLastVersion() != -1) {
                List<BugInstance> lst = map.get(b.getInstanceHash());
                if (lst == null) {
                    lst = new LinkedList<BugInstance>();
                    map.put(b.getInstanceHash(), lst);
                }
                lst.add(b);
            }
        }
        for (List<BugInstance> lst : map.values()) {
            if (lst.size() > 1) {
                TreeSet<Long> removalTimes = new TreeSet<Long>();
                TreeSet<Long> additionTimes = new TreeSet<Long>();

                String bugPattern = "XXX";
                for (BugInstance b : lst) {
                    bugPattern = b.getBugPattern().getType();
                    if (b.getFirstVersion() > 0) {
                        additionTimes.add(b.getFirstVersion());
                    }
                    if (b.getLastVersion() != -1) {
                        removalTimes.add(b.getLastVersion());
                    }
                }
                Iterator<Long> aI = additionTimes.iterator();
                if (!aI.hasNext()) {
                    continue;
                }
                long a = aI.next();
                loop: for (Long removed : removalTimes) {
                    while (a <= removed) {
                        if (!aI.hasNext()) {
                            break loop;
                        }
                        a = aI.next();
                    }
                    System.out.printf("%5d %5d %s%n", removed, a, bugPattern);
                }

            }
        }
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
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance(); // load plugins

        RebornIssues reborn = new RebornIssues();
        CommandLine commandLine = new CommandLine();
        int argCount = commandLine.parse(args, 0, 2, "Usage: " + RebornIssues.class.getName()
                + " [options] [<xml results> [<history]] ");

        SortedBugCollection bugCollection = new SortedBugCollection();
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }
        reborn.setBugCollection(bugCollection);
        reborn.execute();

    }
}
