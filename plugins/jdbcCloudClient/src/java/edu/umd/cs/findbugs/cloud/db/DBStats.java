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

package edu.umd.cs.findbugs.cloud.db;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.util.FractionalMultiset;
import edu.umd.cs.findbugs.util.MergeMap;
import edu.umd.cs.findbugs.util.Multiset;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pwilliam
 */
public class DBStats {
    enum BUG_STATUS {
        ACCEPTED, ASSIGNED, FIXED, FIX_LATER, NEW, VERIFIED, VERIFIER_ASSIGNED, WILL_NOT_FIX, DUPLICATE;
        public static int score(String name) {
            try {
                BUG_STATUS value = valueOf(name);
                return value.score();
            } catch (RuntimeException e) {
                return 0;
            }
        }

        public static int stage(String name) {
            try {
                BUG_STATUS value = valueOf(name);
                return value.stage();
            } catch (RuntimeException e) {
                return 0;
            }
        }

        public int score() {
            switch (this) {
            case NEW:
                return 0;

            case ACCEPTED:
            case DUPLICATE:
            case WILL_NOT_FIX:
                return 1;

            case ASSIGNED:
            case FIXED:
            case FIX_LATER:
            case VERIFIED:
            case VERIFIER_ASSIGNED:
                return 2;

            default:
                throw new IllegalStateException();
            }
        }

        public int stage() {
            switch (this) {
            case NEW:
            case DUPLICATE:
                return 0;

            case ASSIGNED:
                return 1;

            case WILL_NOT_FIX:
            case ACCEPTED:
                return 2;

            case FIX_LATER:
                return 3;
            case FIXED:
                return 4;
            case VERIFIED:
            case VERIFIER_ASSIGNED:
                return 5;

            default:
                throw new IllegalStateException();
            }
        }
    }

    static Timestamp bucketByHour(Timestamp t) {
        Timestamp result = new Timestamp(t.getTime());
        result.setSeconds(0);
        result.setMinutes(0);
        result.setNanos(0);
        return result;
    }

    static class TimeSeries<K, V extends Comparable<? super V>> implements Comparable<TimeSeries<K, V>> {
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((k == null) ? 0 : k.hashCode());
            result = prime * result + ((v == null) ? 0 : v.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof TimeSeries))
                return false;
            TimeSeries<K, V> other = (TimeSeries) obj;
            return Util.nullSafeEquals(this.k, other.k) && Util.nullSafeEquals(this.v, other.v);
        }

        final K k;

        final int keyHash;

        final V v;

        public TimeSeries(K k, V v) {
            this.k = k;
            this.keyHash = System.identityHashCode(k);
            this.v = v;
        }

        @Override
        public String toString() {
            return v + " " + k;
        }

        public int compareTo(TimeSeries<K, V> o) {
            if (o == this)
                return 0;
            int result = v.compareTo(o.v);
            if (result != 0)
                return result;
            if (keyHash < o.keyHash)
                return -1;
            return 1;
        }
    }

    public static void main(String args[]) throws Exception {
        Map<String, String> officeLocation = new HashMap<String, String>();

        URL u = DetectorFactoryCollection.getCoreResource("offices.properties");
        if (u != null) {
            BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
            while (true) {
                String s = in.readLine();
                if (s == null)
                    break;
                if (s.trim().length() == 0)
                    continue;
                int x = s.indexOf(':');

                if (x == -1)
                    continue;
                String office = s.substring(0, x);
                for (String person : s.substring(x + 1).split(" "))
                    officeLocation.put(person, office);

            }
            in.close();
        }
        I18N i18n = I18N.instance();

        DBCloud cloud = new DBCloud(null, null, new Properties());
        cloud.initialize();
        Connection c = cloud.getConnection();

        Map<Integer, BugRankCategory> bugRank = new HashMap<Integer, BugRankCategory>();
        Map<Integer, String> bugPattern = new HashMap<Integer, String>();
        Map<String, Integer> detailedBugRank = new HashMap<String, Integer>();

        PreparedStatement ps = c.prepareStatement("SELECT id, hash, bugPattern, priority FROM findbugs_issue");
        ResultSet rs = ps.executeQuery();
        DetectorFactoryCollection detectorfactory = DetectorFactoryCollection.instance();
        while (rs.next()) {
            int col = 1;
            int id = rs.getInt(col++);
            String hash = rs.getString(col++);
            String bugType = rs.getString(col++);
            int priority = rs.getInt(col++);
            BugPattern pattern = detectorfactory.lookupBugPattern(bugType);
            if (pattern != null) {
                int rank = BugRanker.findRank(pattern, priority);
                bugRank.put(id, BugRankCategory.getRank(rank));
                detailedBugRank.put(hash, rank);
                bugPattern.put(id, pattern.getType());
            }
        }
        rs.close();
        ps.close();

        ps = c.prepareStatement("SELECT who,  jvmLoadTime, findbugsLoadTime, analysisLoadTime, initialSyncTime, timestamp, numIssues"
                + " FROM findbugs_invocation");

        MergeMap.MinMap<String, Timestamp> firstUse = new MergeMap.MinMap<String, Timestamp>();
        MergeMap.MinMap<String, Timestamp> reviewers = new MergeMap.MinMap<String, Timestamp>();
        MergeMap.MinMap<String, Timestamp> uniqueReviews = new MergeMap.MinMap<String, Timestamp>();

        HashSet<String> participants = new HashSet<String>();
        Multiset<String> invocations = new Multiset<String>();
        Multiset<String> participantsPerOffice = new Multiset<String>(new TreeMap<String, Integer>());
        rs = ps.executeQuery();
        int invocationCount = 0;
        long invocationTotal = 0;
        long loadTotal = 0;
        while (rs.next()) {
            int col = 1;
            String who = rs.getString(col++);
            int jvmLoad = rs.getInt(col++);
            int fbLoad = rs.getInt(col++);
            int analysisLoad = rs.getInt(col++);
            int dbSync = rs.getInt(col++);
            Timestamp when = rs.getTimestamp(col++);
            int numIssues = rs.getInt(col++);
            invocationCount++;
            invocationTotal += jvmLoad + fbLoad + analysisLoad + dbSync;
            loadTotal += fbLoad + analysisLoad + dbSync;
            firstUse.put(who, when);
            if (numIssues > 3000)
                invocations.add(who);
            if (participants.add(who)) {
                String office = officeLocation.get(who);
                if (office == null)
                    office = "unknown";
                participantsPerOffice.add(office);
            }

        }
        rs.close();
        ps.close();

        ps = c.prepareStatement("SELECT id, issueId, who, designation, timestamp FROM findbugs_evaluation ORDER BY timestamp DESC");
        rs = ps.executeQuery();

        Multiset<String> issueReviewedBy = new Multiset<String>();

        Multiset<String> allIssues = new Multiset<String>();
        Multiset<String> scariestIssues = new Multiset<String>();
        Multiset<String> scaryIssues = new Multiset<String>();
        Multiset<String> troublingIssues = new Multiset<String>();
        Multiset<Integer> scoreForIssue = new Multiset<Integer>();
        Multiset<Integer> squareScoreForIssue = new Multiset<Integer>();
        Multiset<Integer> reviewsForIssue = new Multiset<Integer>();
        Multiset<Integer> scoredReviews = new Multiset<Integer>();

        HashSet<String> issueReviews = new HashSet<String>();
        HashSet<Integer> missingRank = new HashSet<Integer>();

        while (rs.next()) {
            int col = 1;
            int id = rs.getInt(col++);
            int issueId = rs.getInt(col++);
            String who = rs.getString(col++);
            String designation = rs.getString(col++);
            UserDesignation d = UserDesignation.valueOf(designation);
            designation = getDesignationTitle(i18n, d);
            int score = d.score();

            if (d != UserDesignation.OBSOLETE_CODE) {
                scoreForIssue.add(issueId, score);
                squareScoreForIssue.add(issueId, score * score);
                scoredReviews.add(issueId);
            }

            reviewsForIssue.add(issueId);
            Timestamp when = rs.getTimestamp(col++);
            BugRankCategory rank = bugRank.get(issueId);
            reviewers.put(who, when);
            String issueReviewer = who + "-" + issueId;
            if (issueReviews.add(issueReviewer)) {
                uniqueReviews.put(issueReviewer, when);
                allIssues.add(designation);
                issueReviewedBy.add(who);
                if (rank != null)
                    switch (rank) {
                    case SCARIEST:
                        scariestIssues.add(designation);
                        break;
                    case SCARY:
                        scaryIssues.add(designation);
                        break;
                    case TROUBLING:
                        troublingIssues.add(designation);
                        break;

                    }
                else {
                    if (missingRank.add(id)) {
                        System.out.println("No rank for " + id);
                    }

                }
            }

        }
        rs.close();
        ps.close();

        PrintWriter scariestBugs = new PrintWriter("bugReportsForScariestIssues.csv");
        scariestBugs.println("assignedTo,status,rank,note");
        Multiset<String> bugStatus = new Multiset<String>();
        HashSet<String> bugsSeen = new HashSet<String>();
        Multiset<String> bugScore = new Multiset<String>();
        FractionalMultiset<String> patternScore = new FractionalMultiset<String>();
        Multiset<String> patternCount = new Multiset<String>();
        FractionalMultiset<String> patternVariance = new FractionalMultiset<String>();
        FractionalMultiset<Integer> issueVariance = new FractionalMultiset<Integer>();
        FractionalMultiset<Integer> issueScore = new FractionalMultiset<Integer>();

        Multiset<String> bugsFiled = new Multiset<String>();
        ps = c.prepareStatement("SELECT bugReportId,hash,status, whoFiled,assignedTo, postmortem, timestamp FROM findbugs_bugreport ORDER BY timestamp DESC");
        rs = ps.executeQuery();
        while (rs.next()) {
            int col = 1;
            String id = rs.getString(col++);
            String hash = rs.getString(col++);

            String status = rs.getString(col++);

            String who = rs.getString(col++);
            String assignedTo = rs.getString(col++);
            String postmortem = rs.getString(col++);

            Timestamp when = rs.getTimestamp(col++);
            if (!bugsSeen.add(id))
                continue;
            Integer rank = detailedBugRank.get(hash);
            if (rank == null) {
                System.out.println("Could not find hash " + hash + " for " + id);
            }
            if (assignedTo != null && !"NEW".equals(status) && (rank != null && rank <= 4 || postmortem != null)) {
                if (postmortem != null)
                    scariestBugs.printf("%s,%s,%s,%d,POSTMORTEM%n", assignedTo, id, status, rank);
                else
                    scariestBugs.printf("%s,%s,%s,%d%n", assignedTo, id, status, rank);
            }

            if (!id.equals(DBCloud.PENDING) && !id.equals(DBCloud.NONE)) {
                bugStatus.add(status);
                bugsFiled.add(who);
                bugScore.add(who, BUG_STATUS.score(status));
            }
        }

        rs.close();
        ps.close();
        c.close();
        scariestBugs.close();
        Multiset<String> overallEvaluation = new Multiset<String>();
        for (Map.Entry<Integer, Integer> e : scoreForIssue.entrySet()) {
            int value = e.getValue();
            Integer issue = e.getKey();
            int num = scoredReviews.getCount(issue);
            if (num == 0)
                continue;
            double average = value / (double) num;
            int score = (int) Math.round(average);
            double square = squareScoreForIssue.getCount(issue) / (double) num;
            double variance = square - average * average;

            String pattern = bugPattern.get(issue);
            patternCount.add(pattern);
            patternScore.add(pattern, average);
            patternVariance.add(pattern, variance);
            issueVariance.add(issue, variance);
            issueScore.add(issue, average);

            // System.out.printf("%s %2d %2d%n", score, value, num);
            overallEvaluation.add(getDesignationTitle(i18n, getDesignationFromScore(score)));

        }

        patternScore.turnTotalIntoAverage(patternCount);
        patternVariance.turnTotalIntoAverage(patternCount);

        printPatterns("patternScore.csv", "average,variance,rank,count,pattern", patternScore, patternVariance, patternCount);

        issueScore.turnTotalIntoAverage(reviewsForIssue);
        issueVariance.turnTotalIntoAverage(reviewsForIssue);

        PrintWriter out1 = new PrintWriter("issueVariance.csv");
        out1.println("variance,average,count,key,pattern");
        for (Map.Entry<Integer, Double> e1 : issueVariance.entriesInDecreasingOrder()) {
            Integer key = e1.getKey();
            int elementCount = reviewsForIssue.getCount(key);
            Double v = e1.getValue();
            if (elementCount >= 3 && v >= 0.5)
                out1.printf("%3.1f,%3.1f,%d,%d,%s%n", v, issueScore.getValue(key), elementCount, key, bugPattern.get(key));

        }
        out1.close();

        System.out.printf("%6d invocations%n", invocationCount);
        System.out.printf("%6d invocations time (secs)%n", invocationTotal / invocationCount / 1000);
        System.out.printf("%6d load time (secs)%n", loadTotal / invocationCount / 1000);
        System.out.println();

        printTimeSeries("users.csv", "Unique users", firstUse);
        printTimeSeries("reviewers.csv", "Unique reviewers", reviewers);
        printTimeSeries("reviews.csv", "Total reviews", uniqueReviews);

        PrintWriter out = new PrintWriter("bug_status.csv");
        out.println("Status,Number of bugs");
        printMultiset(out, "Bug status", bugStatus);
        out.close();

        out = new PrintWriter("reviews_by_category.csv");
        out.println("Category,Number of reviews");
        printMultisetContents(out, "", allIssues);
        out.close();

        out = new PrintWriter("overall_review_of_issue.csv");
        out.println("Category,Number of issues");
        printMultisetContents(out, "", overallEvaluation);
        out.close();

        out = new PrintWriter("reviews_by_rank_and_category.csv");
        out.println("Rank,Category,Number of reviews");
        printMultisetContents(out, "Scariest,", scariestIssues);
        printMultisetContents(out, "Scary,", scaryIssues);
        printMultisetContents(out, "Troubling,", troublingIssues);
        out.close();

        out = new PrintWriter("bugs_filed.csv");
        out.println("rank,bugs filed,who");
        AbstractCloud.printLeaderBoard2(out, bugsFiled, 200, null, "%s,%s,%s\n", "participants per office");
        out.close();

        out = new PrintWriter("bug_score.csv");
        out.println("rank,bug score,who");
        AbstractCloud.printLeaderBoard2(out, bugScore, 200, null, "%s,%s,%s\n", "participants per office");
        out.close();

        out = new PrintWriter("most_participants_by_office.csv");
        out.println("rank,participants,office");
        AbstractCloud.printLeaderBoard2(out, participantsPerOffice, 100, null, "%s,%s,%s\n", "participants per office");
        out.close();

        out = new PrintWriter("most_issues_reviewed_individual.csv");
        out.println("rank,reviews,reviewers");
        AbstractCloud.printLeaderBoard2(out, issueReviewedBy, 10000, null, "%s,%s,%s\n", "num issues reviewed");
        out.close();

    }

    private static void printPatterns(String filename, String header, FractionalMultiset<String> average,
            FractionalMultiset<String> variance, Multiset<String> count) throws FileNotFoundException {
        DetectorFactoryCollection i18n = DetectorFactoryCollection.instance();
        PrintWriter out = new PrintWriter(filename);
        out.println(header);
        for (Map.Entry<String, Double> e : average.entriesInDecreasingOrder()) {
            String key = e.getKey();
            BugPattern pattern = i18n.lookupBugPattern(key);
            if (pattern != null)
                out.printf("%1.1f,%1.1f,%d,%d,%s%n", e.getValue(), variance.getValue(key), BugRanker.findRank(pattern, 1),
                        count.getCount(key), key);
        }
        out.close();
    }

    /**
     * @param value
     */
    private static UserDesignation getDesignationFromScore(int value) {
        if (value <= -3)
            return UserDesignation.BAD_ANALYSIS;
        else
            switch (value) {
            case -2:
                return UserDesignation.NOT_A_BUG;
            case -1:
                return UserDesignation.MOSTLY_HARMLESS;
            case 0:
                return UserDesignation.NEEDS_STUDY;
            case 1:
                return UserDesignation.SHOULD_FIX;
            default:
                return UserDesignation.MUST_FIX;
            }
    }

    /**
     * @param i18n
     * @param d
     * @return
     */
    private static String getDesignationTitle(I18N i18n, edu.umd.cs.findbugs.cloud.Cloud.UserDesignation d) {
        String designation;
        switch (d) {
        case OBSOLETE_CODE:
            designation = "obsolete code";
            break;
        case MUST_FIX:
            designation = "Must fix";
            break;
        case SHOULD_FIX:
            designation = "Should fix";
            break;
        default:
            designation = i18n.getUserDesignation(d.name());
        }
        return designation;
    }

    /**
     * @param out
     *            TODO
     * @param allIssues
     */
    private static void printMultiset(PrintWriter out, String title, Multiset<String> allIssues) {
        printMultisetContents(out, "", allIssues);

    }

    /**
     * @param allIssues
     */
    private static void printMultisetContents(PrintWriter out, String prefix, Multiset<String> allIssues) {
        for (Map.Entry<String, Integer> e : allIssues.entrySet())
            out.printf("%s%s,%d%n", prefix, e.getKey(), e.getValue());
    }

    final static Date fixitStart = new Date("May 11, 2009");

    private static void printTimeSeries(String filename, String title, MergeMap.MinMap<String, Timestamp> firstUse)
            throws FileNotFoundException {
        PrintWriter out = new PrintWriter(filename);
        out.println(title + ",time,full time");
        TreeSet<TimeSeries<String, Timestamp>> series = new TreeSet<TimeSeries<String, Timestamp>>();
        for (Map.Entry<String, Timestamp> e : firstUse.entrySet()) {
            series.add(new TimeSeries<String, Timestamp>(e.getKey(), e.getValue()));
        }

        Multiset<Timestamp> counter = new Multiset<Timestamp>(new TreeMap<Timestamp, Integer>());
        for (TimeSeries<String, Timestamp> t : series) {
            counter.add(bucketByHour(t.v));
        }
        int total = 0;
        SimpleDateFormat format = new SimpleDateFormat("h a EEE");
        SimpleDateFormat defaultFormat = new SimpleDateFormat();
        for (Map.Entry<Timestamp, Integer> e : counter.entrySet()) {
            Timestamp time = e.getKey();
            total += e.getValue();
            if (time.after(fixitStart))
                out.printf("%d,%s,%s%n", total, format.format(time), defaultFormat.format(time));
        }
        out.close();

    }

}
