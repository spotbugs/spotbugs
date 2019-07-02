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

package edu.umd.cs.findbugs;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.io.IO;

/**
 * Bug rankers are used to compute a bug rank for each bug instance. Bug ranks
 * 1-20 are for bugs that are visible to users. Bug rank 1 is more the most
 * relevant/scary bugs. A bug rank greater than 20 is for issues that should not
 * be shown to users.
 *
 *
 * The following bug rankers may exist:
 * <ul>
 * <li>core bug ranker (loaded from etc/bugrank.txt)
 * <li>a bug ranker for each plugin (loaded from {@literal <plugin>}/etc/bugrank.txt)
 * <li>A global adjustment ranker (loaded from plugins/adjustBugrank.txt)
 * </ul>
 *
 * A bug ranker is comprised of a list of bug patterns, bug kinds and bug
 * categories. For each, either an absolute or relative bug rank is provided. A
 * relative rank is one preceded by a + or -.
 *
 * For core bug detectors, the bug ranker search order is:
 * <ul>
 * <li>global adjustment bug ranker
 * <li>core bug ranker
 * </ul>
 *
 * For third party plugins, the bug ranker search order is:
 * <ul>
 * <li>global adjustment bug ranker
 * <li>plugin adjustment bug ranker
 * <li>core bug ranker
 * </ul>
 *
 * The overall search order is
 * <ul>
 * <li>Bug patterns, in search order across bug rankers
 * <li>Bug kinds, in search order across bug rankers
 * <li>Bug categories, in search order across bug rankers
 * </ul>
 *
 * Search stops at the first absolute bug rank found, and the result is the sum
 * of all of relative bug ranks plus the final absolute bug rank. Since all bug
 * categories are defined by the core bug ranker, we should always find an
 * absolute bug rank.
 *
 * @see BugRankCategory
 * @see Priorities
 * @see edu.umd.cs.findbugs.annotations.Confidence
 *
 * @author Bill Pugh
 */
public class BugRanker {
    /** Maximum value for user visible ranks (least relevant) */
    public static final int VISIBLE_RANK_MAX = 20;
    /** Minimum value for user visible ranks (most relevant) */
    public static final int VISIBLE_RANK_MIN = 1;

    private static final Logger LOG = LoggerFactory.getLogger(BugRanker.class);

    static class Scorer {
        private final HashMap<String, Integer> adjustment = new HashMap<>();

        private final HashSet<String> isRelative = new HashSet<>();

        int get(String key) {
            Integer v = adjustment.get(key);
            if (v == null) {
                return 0;
            }
            return v;
        }

        boolean isRelative(String key) {
            return !adjustment.containsKey(key) || isRelative.contains(key);
        }

        void storeAdjustment(String key, String value) {
            for (String k : key.split(",")) {
                char firstChar = value.charAt(0);
                if (firstChar == '+') {
                    value = value.substring(1);
                }

                int v = Integer.parseInt(value);
                adjustment.put(k, v);
                if (firstChar == '+' || firstChar == '-') {
                    isRelative.add(k);
                }


            }
        }
    }

    /**
     * @param u
     *            may be null. In this case, a default value will be used for
     *            all bugs
     * @throws IOException
     */
    BugRanker(@CheckForNull URL u) throws IOException {
        if (u == null) {
            return;
        }
        try (BufferedReader in = UTF8.bufferedReader(IO.openNonCachedStream(u))) {
            while (true) {
                String s = in.readLine();
                if (s == null) {
                    break;
                }

                s = s.trim();
                if (s.length() == 0) {
                    continue;
                }

                String parts[] = s.split(" ");
                if (parts.length != 3) {
                    AnalysisContext.logError("Can't parse bug rank line: '" + s + "'. "
                            + "Valid line must contain 3 parts separated by spaces.");
                    continue;
                }
                String rank = parts[0];
                String kind = parts[1];
                String what = parts[2];
                if ("BugPattern".equals(kind)) {
                    bugPatterns.storeAdjustment(what, rank);
                } else if ("BugKind".equals(kind)) {
                    bugKinds.storeAdjustment(what, rank);
                } else if ("Category".equals(kind)) {
                    bugCategories.storeAdjustment(what, rank);
                } else {
                    AnalysisContext.logError("Can't parse rank kind from line: '" + s + "'. "
                            + "Valid kind must be either 'BugPattern', 'BugKind' or 'Category'.");
                }
            }
        }
    }

    private final Scorer bugPatterns = new Scorer();

    private final Scorer bugKinds = new Scorer();

    private final Scorer bugCategories = new Scorer();

    /**
     *
     */
    public static final String FILENAME = "bugrank.txt";

    public static final String ADJUST_FILENAME = "adjustBugrank.txt";

    private static int priorityAdjustment(int priority) {
        switch (priority) {
        case Priorities.HIGH_PRIORITY:
            return 0;
        case Priorities.NORMAL_PRIORITY:
            return 2;
        case Priorities.LOW_PRIORITY:
            return 5;
        default:
            return 10;
        }
    }

    private static int adjustRank(int patternRank, int priority) {
        int priorityAdjustment = priorityAdjustment(priority);
        if (patternRank > VISIBLE_RANK_MAX) {
            return patternRank + priorityAdjustment;
        }
        return Math.max(VISIBLE_RANK_MIN, Math.min(patternRank + priorityAdjustment, VISIBLE_RANK_MAX));
    }

    private static int rankBugPattern(BugPattern bugPattern, BugRanker... rankers) {
        String type = bugPattern.getType();
        int rank = 0;
        for (BugRanker b : rankers) {
            if (b != null) {
                rank += b.bugPatterns.get(type);
                if (!b.bugPatterns.isRelative(type)) {
                    return rank;
                }
            }
        }
        String kind = bugPattern.getAbbrev();
        for (BugRanker b : rankers) {
            if (b != null) {
                rank += b.bugKinds.get(kind);
                if (!b.bugKinds.isRelative(kind)) {
                    return rank;
                }
            }
        }
        String category = bugPattern.getCategory();
        for (BugRanker b : rankers) {
            if (b != null) {
                rank += b.bugCategories.get(category);
                if (!b.bugCategories.isRelative(category)) {
                    return rank;
                }
            }
        }
        return rank;
    }



    private static BugRanker getCoreRanker() {
        Plugin corePlugin = PluginLoader.getCorePluginLoader().getPlugin();
        return corePlugin.getBugRanker();
    }

    public static int findRank(BugInstance bug) {
        int patternRank = findRank(bug.getBugPattern(), bug.getDetectorFactory());
        return adjustRank(patternRank, bug.getPriority());
    }

    public static int findRank(BugPattern bugPattern, int priority) {
        int patternRank = findRank(bugPattern, null);
        return adjustRank(patternRank, priority);
    }


    private static AnalysisLocal<HashMap<BugPattern, Integer>> rankForBugPattern = new AnalysisLocal<HashMap<BugPattern, Integer>>() {
        @Override
        protected HashMap<BugPattern, Integer> initialValue() {
            return new HashMap<>();
        }
    };


    public static int findRank(BugPattern pattern, @CheckForNull DetectorFactory detectorFactory) {
        boolean haveCache = Global.getAnalysisCache() != null;
        if (haveCache) {
            Integer cachedResult = rankForBugPattern.get().get(pattern);
            if (cachedResult != null) {
                return cachedResult;
            }
        }

        int rank;
        if (detectorFactory == null) {
            rank = findRankUnknownPlugin(pattern);
        } else {
            Plugin plugin = detectorFactory.getPlugin();
            BugRanker pluginRanker = plugin.getBugRanker();
            BugRanker coreRanker = getCoreRanker();

            if (pluginRanker == coreRanker) {
                rank = rankBugPattern(pattern, coreRanker);
            } else {
                rank = rankBugPattern(pattern, pluginRanker, coreRanker);
            }
        }
        if (haveCache) {
            rankForBugPattern.get().put(pattern, rank);
        }
        return rank;
    }

    private static int findRankUnknownPlugin(BugPattern pattern) {

        List<BugRanker> rankers = new ArrayList<>();
        pluginLoop: for (Plugin plugin : Plugin.getAllPlugins()) {
            if (plugin.isCorePlugin()) {
                continue;
            }
            for (DetectorFactory df : plugin.getDetectorFactories()) {

                if (df.getReportedBugPatterns().contains(pattern)) {
                    LOG.debug("Bug rank match {} {} for {}", plugin, df, pattern);
                    rankers.add(plugin.getBugRanker());
                    continue pluginLoop;
                }
            }
            LOG.debug("plugin {} doesn't match {}", plugin, pattern);

        }
        rankers.add(getCoreRanker());

        return rankBugPattern(pattern, rankers.toArray(new BugRanker[0]));
    }

    public static void trimToMaxRank(BugCollection origCollection, int maxRank) {
        origCollection.getCollection().removeIf(b -> BugRanker.findRank(b) > maxRank);
    }
}
