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

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * @author William Pugh
 */
public class Churn {
    BugCollection bugCollection;

    int fixRate = -1;

    public Churn() {
    }

    public Churn(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    public void setBugCollection(BugCollection bugCollection) {
        this.bugCollection = bugCollection;
    }

    String getKey(BugInstance b) {
        if (false) {
            return b.getType();
        }
        String result = b.getCategoryAbbrev();
        if ("C".equals(result) || "N".equals(result)) {
            return result;
        }
        return "O";

        // return b.getPriorityAbbreviation() + "-" + b.getType();
    }

    static class Data {
        int persist, fixed;

        int maxRemovedAtOnce() {
            int count = 0;
            for (int c : lastCount.values()) {
                if (count < c) {
                    count = c;
                }
            }
            return count;
        }

        Map<Long, Integer> lastCount = new HashMap<Long, Integer>();

        void update(BugInstance bug) {
            if (bug.isDead()) {
                fixed++;
            } else {
                persist++;
            }
            final long lastVersion = bug.getLastVersion();
            if (lastVersion != -1) {
                Integer v = lastCount.get(lastVersion);
                if (v == null) {
                    lastCount.put(lastVersion, 0);
                } else {
                    lastCount.put(lastVersion, v + 1);
                }
            }
        }
    }

    Map<String, Data> data = new TreeMap<String, Data>();

    Data all = new Data();

    int[] aliveAt;

    int[] diedAfter;

    public Churn execute() {

        data.put("all", all);
        aliveAt = new int[(int) bugCollection.getSequenceNumber() + 1];
        diedAfter = new int[(int) bugCollection.getSequenceNumber() + 1];

        for (Iterator<BugInstance> j = bugCollection.iterator(); j.hasNext();) {
            BugInstance bugInstance = j.next();

            String key = getKey(bugInstance);
            Data d = data.get(key);
            if (d == null) {
                data.put(key, d = new Data());
            }
            d.update(bugInstance);
            all.update(bugInstance);

            long first = bugInstance.getFirstVersion();
            long last = bugInstance.getLastVersion();

            if (last != -1) {
                System.out.printf("%3d #fixed %s%n", last, key);
            }
            if (first != 0 && last != -1) {
                int lifespan = (int) (last - first + 1);

                System.out.printf("%3d #age %s%n", lifespan, key);
                System.out.printf("%3d %3d #spread %s%n", first, last, key);
                diedAfter[lifespan]++;
                for (int t = 1; t < lifespan; t++) {
                    aliveAt[t]++;
                }
            } else if (first != 0) {
                int lifespan = (int) (bugCollection.getSequenceNumber() - first + 1);
                for (int t = 1; t < lifespan; t++) {
                    aliveAt[t]++;
                }
            }
        }
        return this;
    }

    public void dump(PrintStream out) {
        for (int t = 1; t < aliveAt.length; t++) {
            if (aliveAt[t] != 0) {
                System.out.printf("%3d%% %4d %5d %3d #decay%n", diedAfter[t] * 100 / aliveAt[t], diedAfter[t], aliveAt[t], t);
            }
        }
        System.out.printf("%7s %3s %5s %5s %5s  %s%n", "chi", "%", "const", "fix", "max", "kind");
        double fixRate;
        if (this.fixRate == -1) {
            fixRate = ((double) all.fixed) / (all.fixed + all.persist);
        } else {
            fixRate = this.fixRate / 100.0;
        }
        double highFixRate = fixRate + 0.05;
        double lowFixRate = fixRate - 0.05;
        for (Map.Entry<String, Data> e : data.entrySet()) {
            Data d = e.getValue();
            int total = d.persist + d.fixed;
            if (total < 2) {
                continue;
            }

            double rawFixRate = ((double) d.fixed) / total;

            double chiValue;
            if (lowFixRate <= rawFixRate && rawFixRate <= highFixRate) {
                chiValue = 0;
            } else {
                double baseFixRate;

                if (rawFixRate < lowFixRate) {
                    baseFixRate = lowFixRate;
                } else {
                    baseFixRate = highFixRate;
                }
                double expectedFixed = baseFixRate * total;
                double expectedPersist = (1 - baseFixRate) * total;
                chiValue = (d.fixed - expectedFixed) * (d.fixed - expectedFixed) / expectedFixed + (d.persist - expectedPersist)
                        * (d.persist - expectedPersist) / expectedPersist;
                if (rawFixRate < lowFixRate) {
                    chiValue = -chiValue;
                }
            }

            System.out.printf("%7d %3d %5d %5d %5d %s%n", (int) chiValue, d.fixed * 100 / total, d.persist, d.fixed,
                    d.maxRemovedAtOnce(), e.getKey());
        }

    }

    class ChurnCommandLine extends CommandLine {

        ChurnCommandLine() {
            this.addOption("-fixRate", "percentage", "expected fix rate for chi test");
        }

        @Override
        public void handleOption(String option, String optionalExtraPart) {
            throw new IllegalArgumentException("unknown option: " + option);
        }

        @Override
        public void handleOptionWithArgument(String option, String argument) {
            if ("-fixRate".equals(option)) {
                fixRate = Integer.parseInt(argument);
            } else {
                throw new IllegalArgumentException("unknown option: " + option);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        DetectorFactoryCollection.instance(); // load plugins

        Churn churn = new Churn();
        ChurnCommandLine commandLine = churn.new ChurnCommandLine();
        int argCount = commandLine
                .parse(args, 0, 2, "Usage: " + Churn.class.getName() + " [options] [<xml results> [<history]] ");

        SortedBugCollection bugCollection = new SortedBugCollection();
        if (argCount < args.length) {
            bugCollection.readXML(args[argCount++]);
        } else {
            bugCollection.readXML(System.in);
        }
        churn.setBugCollection(bugCollection);
        churn.execute();
        PrintStream out = System.out;
        try {
            if (argCount < args.length) {
                out = UTF8.printStream(new FileOutputStream(args[argCount++]), true);
            }
            churn.dump(out);
        } finally {
            out.close();
        }

    }
}
