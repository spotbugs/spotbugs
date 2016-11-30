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

package edu.umd.cs.findbugs.workflow;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.charsets.UserTextFile;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * @author bill.pugh
 */
public class CloudSyncAndReport {

    public static class CSPoptions {

        public String analysisFile;

        public String cloudSummary;

        public String cloudId;

        public int ageInHours = 22;
    }

    static class CSRCommandLine extends CommandLine {

        final CSPoptions options;

        public CSRCommandLine(CSPoptions options) {
            this.options = options;
            addOption("-cloud", "id", "id of the cloud to use");
            addOption("-recent", "hours", "maximum age in hours for an issue to be recent");
            addOption("-cloudSummary", "file", "write a cloud summary to thie file");
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String,
         * java.lang.String)
         */
        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            throw new IllegalArgumentException("Unknown option : " + option);

        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java
         * .lang.String, java.lang.String)
         */
        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
            if ("-cloud".equals(option)) {
                options.cloudId = argument;
            } else if ("-recent".equals(option)) {
                options.ageInHours = Integer.parseInt(argument);
            } else if ("-cloudSummary".equals(option)) {
                options.cloudSummary = argument;
            } else {
                throw new IllegalArgumentException("Unknown option : " + option);
            }
        }
    }

    public static void main(String[] argv) throws Exception {

        FindBugs.setNoAnalysis();
        final CSPoptions options = new CSPoptions();
        final CSRCommandLine commandLine = new CSRCommandLine(options);
        int argCount = commandLine.parse(argv, 0, 1, "Usage: " + CloudSyncAndReport.class.getName()
                + " [options] [<results1> <results2> ... <resultsn>] ");

        if (argCount < argv.length) {
            options.analysisFile = argv[argCount];
        }

        CloudSyncAndReport csr = new CloudSyncAndReport(options);
        csr.load();

        csr.sync();
        PrintWriter out = UTF8.printWriter(System.out);
        csr.report(out);
        out.flush();
        csr.shutdown();
        out.close();
    }

    final CSPoptions options;

    final SortedBugCollection bugCollection = new SortedBugCollection();

    /**
     * @param options
     */
    public CloudSyncAndReport(CSPoptions options) {
        this.options = options;

    }

    public void load() throws IOException, DocumentException {
        if (options.analysisFile == null) {
            bugCollection.readXML(UTF8.bufferedReader(System.in));
        } else {
            bugCollection.readXML(options.analysisFile);
        }
        if (options.cloudId != null && !options.cloudId.equals(bugCollection.getProject().getCloudId())) {
            bugCollection.getProject().setCloudId(options.cloudId);
            bugCollection.reinitializeCloud();
        }
    }

    public void sync() {
        Cloud cloud = bugCollection.getCloud();
        cloud.initiateCommunication();
        cloud.waitUntilIssueDataDownloaded();
    }

    static class Stats {
        int total, recent;

    }

    public void report(PrintWriter out) {
        TreeMap<BugRankCategory, Stats> stats = new TreeMap<BugRankCategory, Stats>();
        ProjectStats projectStats = bugCollection.getProjectStats();
        Collection<BugInstance> bugs = bugCollection.getCollection();
        Cloud cloud = bugCollection.getCloud();
        cloud.setMode(Cloud.Mode.COMMUNAL);

        out.printf("Cloud sync and summary report for %s%n", bugCollection.getProject().getProjectName());

        out.printf("Code dated %s%n", new Date(bugCollection.getTimestamp()));
        out.printf("Code analyzed %s%n", new Date(bugCollection.getAnalysisTimestamp()));

        out.printf("%7d total classes%n", projectStats.getNumClasses());
        out.printf("%7d total issues%n", bugs.size());
        long recentTimestamp = System.currentTimeMillis() - options.ageInHours * 3600 * 1000L;
        int allRecentIssues = 0;

        for (BugInstance b : bugs) {
            Stats s = stats.get(BugRankCategory.getRank(b.getBugRank()));
            if (s == null) {
                s = new Stats();
                stats.put(BugRankCategory.getRank(b.getBugRank()), s);
            }
            s.total++;
            long firstSeen = cloud.getFirstSeen(b);
            if (firstSeen > recentTimestamp) {
                s.recent++;
                allRecentIssues++;
            }

        }
        out.printf("%7d recent issues%n", allRecentIssues);

        if (options.cloudSummary != null && cloud.supportsCloudSummaries()) {
            try {
                PrintWriter cs = UserTextFile.printWriter(options.cloudSummary);
                cs.printf("%6s %6s %s%n", "recent", "total", "Rank category");
                for (Entry<BugRankCategory, Stats> e : stats.entrySet()) {
                    Stats s = e.getValue();
                    if (s.total > 0) {
                        cs.printf("%6d %6d %s%n", s.recent, s.total, e.getKey());
                    }
                }
                cs.println();
                cloud.printCloudSummary(cs, bugs, null);
                cs.close();
            } catch (Exception e) {
                out.println("Error writing cloud summary to " + options.cloudSummary);
                e.printStackTrace(out);
            }

        }

    }

    public void shutdown() {
        Cloud cloud = bugCollection.getCloud();
        cloud.shutdown();
    }

}
