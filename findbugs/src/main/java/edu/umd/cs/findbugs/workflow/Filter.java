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

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCategory;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.ExcludingHashesBugReporter;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.PackageStats.ClassStats;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SloppyBugComparator;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.util.Util;

/**
 * Java main application to filter/transform an XML bug collection or bug
 * history collection.
 *
 * @author William Pugh
 */
public class Filter {
    static class FilterCommandLine extends CommandLine {
        /**
         *
         */
        public static final long MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000L;

        Pattern classPattern, bugPattern, callsPattern;

        public boolean notSpecified = false;

        public boolean not = false;

        int duration;

        long first;

        String firstAsString;

        long after;

        String afterAsString;

        long before;

        String beforeAsString;

        int maxRank = Integer.MAX_VALUE;

        long maybeMutated;

        String maybeMutatedAsString;

        long last;

        String lastAsString;

        String trimToVersionAsString;

        String fixedAsString; // alternate way to specify 'last'

        long present;

        String presentAsString;

        long absent;

        String absentAsString;

        String annotation;

        HashSet<String> hashesFromFile;

        public boolean sloppyUniqueSpecified = false;

        public boolean sloppyUnique = false;

        public boolean purgeHistorySpecified = false;

        public boolean purgeHistory = false;

        public boolean activeSpecified = false;

        public boolean active = false;

        public boolean notAProblem = false;

        public boolean notAProblemSpecified = false;

        public boolean shouldFix = false;

        public boolean shouldFixSpecified = false;

        public boolean hasField = false;

        public boolean hasFieldSpecified = false;

        public boolean hasLocal = false;

        public boolean hasLocalSpecified = false;

        public boolean applySuppression = false;

        public boolean applySuppressionSpecified = false;

        public boolean withSource = false;

        public boolean withSourceSpecified = false;

        public boolean knownSource = false;

        public boolean knownSourceSpecified = false;

        public boolean introducedByChange = false;

        public boolean introducedByChangeSpecified = false;

        public boolean removedByChange = false;

        public boolean removedByChangeSpecified = false;

        public boolean newCode = false;

        public boolean newCodeSpecified = false;

        public boolean hashChanged = false;

        public boolean hashChangedSpecified = false;

        public boolean removedCode = false;

        public boolean removedCodeSpecified = false;

        public boolean dontUpdateStats = false;

        public boolean dontUpdateStatsSpecified = false;

        public int maxAge = 0;

        public boolean maxAgeSpecified = false;

        public boolean withMessagesSpecified = false;

        public boolean withMessages = false;

        private final List<Matcher> includeFilter = new LinkedList<Matcher>();

        private final List<Matcher> excludeFilter = new LinkedList<Matcher>();

        HashSet<String> excludedInstanceHashes = new HashSet<String>();

        Set<String> designationKey = new HashSet<String>();

        Set<String> categoryKey = new HashSet<String>();

        SortedSet<BugInstance> uniqueSloppy;

        int priority = 3;

        FilterCommandLine() {

            addSwitch("-not", "reverse (all) switches for the filter");
            addSwitchWithOptionalExtraPart("-knownSource", "trurh", "Only issues that have known source locations");
            addSwitchWithOptionalExtraPart("-withSource", "truth", "only warnings for which source is available");
            addSwitchWithOptionalExtraPart("-hashChanged", "truth",
                    "only warnings for which the stored hash is not the same as the calculated hash");
            addOption("-excludeBugs", "baseline bug collection", "exclude bugs already contained in the baseline bug collection");
            addOption("-exclude", "filter file", "exclude bugs matching given filter");
            addOption("-include", "filter file", "include only bugs matching given filter");

            addOption("-annotation", "text", "allow only warnings containing this text in a user annotation");
            addSwitchWithOptionalExtraPart("-withMessages", "truth", "generated XML should contain textual messages");
            addOption("-maxDuration", "# versions", "only issues present in at most this many versions");
            addOption("-after", "when", "allow only warnings that first occurred after this version");
            addOption("-before", "when", "allow only warnings that first occurred before this version");
            addOption("-first", "when", "allow only warnings that first occurred in this version");
            addOption("-last", "when", "allow only warnings that last occurred in this version");
            addOption("-trimToVersion", "when", "trim bug collection to exclude information about versions after this one");
            addOption("-fixed", "when", "allow only warnings that last occurred in the previous version (clobbers last)");
            addOption("-present", "when", "allow only warnings present in this version");
            addOption("-absent", "when", "allow only warnings absent in this version");
            addOption("-maybeMutated", "when", "allow only warnings that might have mutated/fixed/born in this version");
            addSwitchWithOptionalExtraPart("-hasField", "truth", "allow only warnings that are annotated with a field");
            addSwitchWithOptionalExtraPart("-hasLocal", "truth", "allow only warnings that are annotated with a local variable");
            addSwitchWithOptionalExtraPart("-active", "truth", "allow only warnings alive in the last sequence number");
            addSwitch("-applySuppression", "exclude warnings that match the suppression filter");

            addSwitch("-purgeHistory", "remove all version history");
            addSwitchWithOptionalExtraPart("-sloppyUnique", "truth", "select only issues thought to be unique by the sloppy bug comparator ");
            makeOptionUnlisted("-sloppyUnique");
            addSwitchWithOptionalExtraPart("-introducedByChange", "truth",
                    "allow only warnings introduced by a change of an existing class");
            addSwitchWithOptionalExtraPart("-removedByChange", "truth",
                    "allow only warnings removed by a change of a persisting class");
            addSwitchWithOptionalExtraPart("-newCode", "truth", "allow only warnings introduced by the addition of a new class");
            addSwitchWithOptionalExtraPart("-removedCode", "truth", "allow only warnings removed by removal of a class");
            addOption("-priority", "level", "allow only warnings with this priority or higher");
            makeOptionUnlisted("-priority");
            addOption("-confidence", "level", "allow only warnings with this confidence or higher");
            addOption("-maxRank", "rank", "allow only warnings with this rank or lower");
            addOption("-maxAge", "days", "Only issues that and in the cloud and weren't first seen more than this many days ago");
            addSwitchWithOptionalExtraPart("-notAProblem", "truth",
                    "Only issues with a consensus view that they are not a problem");
            addSwitchWithOptionalExtraPart("-shouldFix", "truth", "Only issues with a consensus view that they should be fixed");

            addOption("-class", "pattern", "allow only bugs whose primary class name matches this pattern");
            addOption("-calls", "pattern", "allow only bugs that involve a call to a method that matches this pattern (matches with method class or name)");

            addOption("-bugPattern", "pattern", "allow only bugs whose type matches this pattern");
            addOption("-category", "category", "allow only warnings with a category that starts with this string");
            addOption("-designation", "designation",
                    "allow only warnings with this designation (e.g., -designation SHOULD_FIX,MUST_FIX)");
            addSwitch("-dontUpdateStats",
                    "used when withSource is specified to only update bugs, not the class and package stats");
            addOption("-hashes", "hash file", "only bugs with instance hashes contained in the hash file");

        }

        public static long getVersionNum(BugCollection collection, String val,
                boolean roundToLaterVersion) {
            if (val == null) {
                return -1;
            }
            Map<String, AppVersion> versions = new HashMap<String, AppVersion>();
            SortedMap<Long, AppVersion> timeStamps = new TreeMap<Long, AppVersion>();

            for (Iterator<AppVersion> i = collection.appVersionIterator(); i.hasNext();) {
                AppVersion v = i.next();
                versions.put(v.getReleaseName(), v);
                timeStamps.put(v.getTimestamp(), v);
            }
            // add current version to the maps
            AppVersion v = collection.getCurrentAppVersion();
            versions.put(v.getReleaseName(), v);
            timeStamps.put(v.getTimestamp(), v);

            return getVersionNum(versions,  timeStamps,  val,
                    roundToLaterVersion,  v.getSequenceNumber());
        }

        public static long getVersionNum(Map<String, AppVersion> versions, SortedMap<Long, AppVersion> timeStamps, String val,
                boolean roundToLaterVersion, long currentSeqNum) {
            if (val == null) {
                return -1;
            }
            long numVersions = currentSeqNum + 1;

            if ("last".equals(val) || "lastVersion".equals(val)) {
                return numVersions - 1;
            }

            AppVersion v = versions.get(val);
            if (v != null) {
                return v.getSequenceNumber();
            }
            try {
                long time = 0;
                if (val.endsWith("daysAgo")) {
                    time = System.currentTimeMillis() - MILLISECONDS_PER_DAY
                            * Integer.parseInt(val.substring(0, val.length() - 7));
                } else {
                    time = Date.parse(val);
                }
                return getAppropriateSeq(timeStamps, time, roundToLaterVersion);
            } catch (Exception e) {
                try {
                    long version = Long.parseLong(val);
                    if (version < 0) {
                        version = numVersions + version;
                    }
                    return version;
                } catch (NumberFormatException e1) {
                    throw new IllegalArgumentException("Could not interpret version specification of '" + val + "'");
                }
            }
        }

        // timeStamps contains 0 10 20 30
        // if roundToLater == true, ..0 = 0, 1..10 = 1, 11..20 = 2, 21..30 = 3,
        // 31.. = Long.MAX
        // if roundToLater == false, ..-1 = Long.MIN, 0..9 = 0, 10..19 = 1,
        // 20..29 = 2, 30..39 = 3, 40 .. = 4
        static private long getAppropriateSeq(SortedMap<Long, AppVersion> timeStamps, long when, boolean roundToLaterVersion) {
            if (roundToLaterVersion) {
                SortedMap<Long, AppVersion> geq = timeStamps.tailMap(when);
                if (geq.isEmpty()) {
                    return Long.MAX_VALUE;
                }
                return geq.get(geq.firstKey()).getSequenceNumber();
            } else {
                SortedMap<Long, AppVersion> leq = timeStamps.headMap(when);
                if (leq.isEmpty()) {
                    return Long.MIN_VALUE;
                }
                return leq.get(leq.lastKey()).getSequenceNumber();
            }
        }

        private long minFirstSeen;

        edu.umd.cs.findbugs.filter.Filter suppressionFilter;


        void adjustFilter(Project project, BugCollection collection) {
            suppressionFilter = project.getSuppressionFilter();

            if (maxAgeSpecified) {
                minFirstSeen = collection.getAnalysisTimestamp() - maxAge * MILLISECONDS_PER_DAY;
            }
            first = getVersionNum(collection, firstAsString, true);
            maybeMutated = getVersionNum(collection, maybeMutatedAsString, true);
            last = getVersionNum(collection, lastAsString, true);
            before = getVersionNum(collection, beforeAsString, true);
            after = getVersionNum(collection, afterAsString, false);
            present = getVersionNum(collection, presentAsString, true);
            absent = getVersionNum(collection, absentAsString, true);

            if (sloppyUniqueSpecified) {
                uniqueSloppy = new TreeSet<BugInstance>(new SloppyBugComparator());
            }

            long fixed = getVersionNum(collection, fixedAsString, true);
            if (fixed >= 0)
            {
                last = fixed - 1; // fixed means last on previous sequence (ok
                // if -1)
            }
        }

        boolean accept(BugCollection collection, BugInstance bug) {
            boolean result = evaluate(collection, bug);
            if (not) {
                return !result;
            }
            return result;
        }

        boolean evaluate(BugCollection collection, BugInstance bug) {

            for (Matcher m : includeFilter) {
                if (!m.match(bug)) {
                    return false;
                }
            }
            for (Matcher m : excludeFilter) {
                if (m.match(bug)) {
                    return false;
                }
            }
            if (excludedInstanceHashes.contains(bug.getInstanceHash())) {
                return false;
            }
            if (annotation != null && bug.getAnnotationText().indexOf(annotation) == -1) {
                return false;
            }
            if (bug.getPriority() > priority) {
                return false;
            }
            if (firstAsString != null && bug.getFirstVersion() != first) {
                return false;
            }
            if (afterAsString != null && bug.getFirstVersion() <= after) {
                return false;
            }
            if (beforeAsString != null && bug.getFirstVersion() >= before) {
                return false;
            }
            if (hashesFromFile != null && !hashesFromFile.contains(bug.getInstanceHash())) {
                return false;
            }
            long lastSeen = bug.getLastVersion();
            if (lastSeen < 0) {
                lastSeen = collection.getSequenceNumber();
            }
            long thisDuration = lastSeen - bug.getFirstVersion();
            if (duration > 0 && thisDuration > duration) {
                return false;
            }
            if ((lastAsString != null || fixedAsString != null) && (last < 0 || bug.getLastVersion() != last)) {
                return false;
            }
            if (presentAsString != null && !bugLiveAt(bug, present)) {
                return false;
            }
            if (absentAsString != null && bugLiveAt(bug, absent)) {
                return false;
            }

            if (hasFieldSpecified && (hasField != (bug.getPrimaryField() != null))) {
                return false;
            }
            if (hasLocalSpecified && (hasLocal != (bug.getPrimaryLocalVariableAnnotation() != null))) {
                return false;
            }

            if (maxRank < Integer.MAX_VALUE && BugRanker.findRank(bug) > maxRank) {
                return false;
            }

            if (activeSpecified && active == bug.isDead()) {
                return false;
            }
            if (removedByChangeSpecified && bug.isRemovedByChangeOfPersistingClass() != removedByChange) {
                return false;
            }
            if (introducedByChangeSpecified && bug.isIntroducedByChangeOfExistingClass() != introducedByChange) {
                return false;
            }
            if (newCodeSpecified && newCode != (!bug.isIntroducedByChangeOfExistingClass() && bug.getFirstVersion() != 0)) {
                return false;
            }
            if (removedCodeSpecified && removedCode != (!bug.isRemovedByChangeOfPersistingClass() && bug.isDead())) {
                return false;
            }

            if (bugPattern != null && !bugPattern.matcher(bug.getType()).find()) {
                return false;
            }
            if (classPattern != null && !classPattern.matcher(bug.getPrimaryClass().getClassName()).find()) {
                return false;
            }
            if (callsPattern != null) {
                MethodAnnotation m = bug.getAnnotationWithRole(MethodAnnotation.class, MethodAnnotation.METHOD_CALLED);
                if (m == null) {
                    return false;
                }
                if (!callsPattern.matcher(m.getClassName()).find()  && !callsPattern.matcher(m.getMethodName()).find()) {
                    return false;
                }
            }

            if (maybeMutatedAsString != null && !(atMutationPoint(bug) && mutationPoints.contains(getBugLocation(bug)))) {
                return false;
            }

            BugPattern thisBugPattern = bug.getBugPattern();
            if (!categoryKey.isEmpty() && thisBugPattern != null && !categoryKey.contains(thisBugPattern.getCategory())) {
                return false;
            }
            if (!designationKey.isEmpty() && !designationKey.contains(bug.getUserDesignationKey())) {
                return false;
            }

            if (hashChangedSpecified) {
                if (bug.isInstanceHashConsistent() == hashChanged) {
                    return false;
                }
            }
            if (applySuppressionSpecified && applySuppression && suppressionFilter.match(bug)) {
                return false;
            }
            SourceLineAnnotation primarySourceLineAnnotation = bug.getPrimarySourceLineAnnotation();

            if (knownSourceSpecified) {
                if (primarySourceLineAnnotation.isUnknown() == knownSource) {
                    return false;
                }
            }
            if (withSourceSpecified) {
                if (sourceSearcher.findSource(primarySourceLineAnnotation) != withSource) {
                    return false;
                }
            }

            Cloud cloud = collection.getCloud();
            if (maxAgeSpecified) {
                long firstSeen = cloud.getFirstSeen(bug);
                if (!cloud.isInCloud(bug)) {
                    return false;
                }
                if (firstSeen < minFirstSeen) {
                    return false;
                }
            }

            if (notAProblemSpecified && notAProblem != (cloud.getConsensusDesignation(bug).score() < 0)) {
                return false;
            }
            if (shouldFixSpecified && shouldFix != (cloud.getConsensusDesignation(bug).score() > 0)) {
                return false;
            }

            if (sloppyUniqueSpecified) {
                boolean unique = uniqueSloppy.add(bug);
                if (unique != sloppyUnique) {
                    return false;
                }
            }

            return true;
        }

        private void addDesignationKey(String argument) {
            I18N i18n = I18N.instance();

            for (String x : argument.split("[,|]")) {
                for (String designationKey : i18n.getUserDesignationKeys()) {
                    if (designationKey.equals(x) || i18n.getUserDesignation(designationKey).equals(x)) {
                        this.designationKey.add(designationKey);
                        break;
                    }

                }
            }
        }

        private void addCategoryKey(String argument) {
            DetectorFactoryCollection i18n = DetectorFactoryCollection.instance();

            for (String x : argument.split("[,|]")) {
                for (BugCategory category : i18n.getBugCategoryObjects()) {
                    if (category.getAbbrev().equals(x) || category.getCategory().equals(x)) {
                        this.categoryKey.add(category.getCategory());
                        break;
                    }
                }
            }
        }

        private boolean bugLiveAt(BugInstance bug, long now) {
            if (now < bug.getFirstVersion()) {
                return false;
            }
            if (bug.isDead() && bug.getLastVersion() < now) {
                return false;
            }
            return true;
        }

        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
            option = option.substring(1);
            if (optionExtraPart.length() == 0) {
                setField(option, true);
            } else {
                setField(option, Boolean.parseBoolean(optionExtraPart));
            }
            setField(option + "Specified", true);
        }

        private void setField(String fieldName, boolean value) {
            try {
                Field f = FilterCommandLine.class.getField(fieldName);
                f.setBoolean(this, value);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {

            if ("-priority".equals(option) || "-confidence".equals(option)) {
                priority = parsePriority(argument);
            }

            else if ("-maxRank".equals(option)) {
                maxRank = Integer.parseInt(argument);
            } else if ("-first".equals(option)) {
                firstAsString = argument;
            } else if ("-maybeMutated".equals(option)) {
                maybeMutatedAsString = argument;
            } else if ("-last".equals(option)) {
                lastAsString = argument;
            } else if ("-trimToVersion".equals(option)) {
                trimToVersionAsString = argument;
            } else if ("-maxDuration".equals(option)) {
                duration = Integer.parseInt(argument);
            } else if ("-fixed".equals(option)) {
                fixedAsString = argument;
            } else if ("-after".equals(option)) {
                afterAsString = argument;
            } else if ("-before".equals(option)) {
                beforeAsString = argument;
            } else if ("-present".equals(option)) {
                presentAsString = argument;
            } else if ("-absent".equals(option)) {
                absentAsString = argument;
            } else if ("-category".equals(option)) {
                addCategoryKey(argument);
            } else if ("-designation".equals(option)) {
                addDesignationKey(argument);
            } else if ("-class".equals(option)) {
                classPattern = Pattern.compile(argument.replace(',', '|'));
            } else if ("-calls".equals(option)) {
                callsPattern = Pattern.compile(argument.replace(',', '|'));
            } else if ("-bugPattern".equals(option)) {
                bugPattern = Pattern.compile(argument);
            } else if ("-annotation".equals(option)) {
                annotation = argument;
            } else if ("-excludeBugs".equals(option)) {
                try {
                    ExcludingHashesBugReporter.addToExcludedInstanceHashes(excludedInstanceHashes, argument);
                } catch (DocumentException e) {
                    throw new IllegalArgumentException("Error processing include file: " + argument, e);
                }
            } else if ("-include".equals(option)) {
                try {
                    includeFilter.add(new edu.umd.cs.findbugs.filter.Filter(argument));
                } catch (FilterException e) {
                    throw new IllegalArgumentException("Error processing include file: " + argument, e);
                }
            } else if ("-exclude".equals(option)) {
                try {
                    excludeFilter.add(new edu.umd.cs.findbugs.filter.Filter(argument));
                } catch (FilterException e) {
                    throw new IllegalArgumentException("Error processing include file: " + argument, e);
                }
            } else if ("-maxAge".equals(option)) {
                maxAge = Integer.parseInt(argument);
                maxAgeSpecified = true;
            } else if ("-hashes".equals(option)) {
                hashesFromFile = new HashSet<String>();
                BufferedReader in = null;
                try {
                    in = new BufferedReader(UTF8.fileReader(argument));
                    while (true) {
                        String h = in.readLine();
                        if (h == null) {
                            break;
                        }
                        hashesFromFile.add(h);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error reading hashes from " + argument, e);
                } finally {
                    Util.closeSilently(in);
                }
            } else {
                throw new IllegalArgumentException("can't handle command line argument of " + option);
            }

        }

        HashSet<String> mutationPoints;

        /**
         * Do any prep work needed to perform bug filtering
         *
         * @param origCollection
         */
        public void getReady(SortedBugCollection origCollection) {
            if (maybeMutatedAsString != null) {
                HashSet<String> addedIssues = new HashSet<String>();
                HashSet<String> removedIssues = new HashSet<String>();
                for (BugInstance b : origCollection) {
                    if (b.getFirstVersion() == maybeMutated) {
                        addedIssues.add(getBugLocation(b));
                    } else if (b.getLastVersion() == maybeMutated - 1) {
                        removedIssues.add(getBugLocation(b));
                    }
                }
                addedIssues.remove(null);
                addedIssues.retainAll(removedIssues);
                mutationPoints = addedIssues;
            }

        }

        /**
         * @param b
         * @return
         */
        private boolean atMutationPoint(BugInstance b) {
            return b.getFirstVersion() == maybeMutated || b.getLastVersion() == maybeMutated - 1;
        }

        /**
         * @param b
         * @return
         */
        private String getBugLocation(BugInstance b) {
            String point;
            MethodAnnotation m = b.getPrimaryMethod();
            FieldAnnotation f = b.getPrimaryField();
            if (m != null) {
                point = m.toString();
            } else if (f != null) {
                point = f.toString();
            } else {
                point = null;
            }
            return point;
        }

    }

    public static int parsePriority(String argument) {
        int i = " HMLE".indexOf(argument);
        if (i == -1) {
            i = " 1234".indexOf(argument);
        }
        if (i == -1) {
            throw new IllegalArgumentException("Bad priority: " + argument);
        }
        return i;
    }

    static SourceSearcher sourceSearcher;


    public static void main(String[] args) throws Exception {
        FindBugs.setNoAnalysis();
        DetectorFactoryCollection.instance();

        final FilterCommandLine commandLine = new FilterCommandLine();

        int argCount = commandLine.parse(args, 0, 2, "Usage: " + Filter.class.getName()
                + " [options] [<orig results> [<new results]] ");
        SortedBugCollection origCollection = new SortedBugCollection();

        if (argCount == args.length) {
            origCollection.readXML(System.in);
        } else {
            origCollection.readXML(args[argCount++]);
        }
        boolean verbose = argCount < args.length;
        SortedBugCollection resultCollection = origCollection.createEmptyCollectionWithMetadata();
        Project project = resultCollection.getProject();
        int passed = 0;
        int dropped = 0;
        resultCollection.setWithMessages(commandLine.withMessages);
        if (commandLine.hashChangedSpecified) {
            origCollection.computeBugHashes();
        }
        commandLine.adjustFilter(project, resultCollection);
        ProjectStats projectStats = resultCollection.getProjectStats();
        projectStats.clearBugCounts();
        if (commandLine.classPattern != null) {
            projectStats.purgeClassesThatDontMatch(commandLine.classPattern);
        }
        sourceSearcher = new SourceSearcher(project);

        long trimToVersion = -1;
        if (commandLine.trimToVersionAsString != null) {
            Map<String, AppVersion> versions = new HashMap<String, AppVersion>();
            SortedMap<Long, AppVersion> timeStamps = new TreeMap<Long, AppVersion>();

            for (Iterator<AppVersion> i = origCollection.appVersionIterator(); i.hasNext();) {
                AppVersion v = i.next();
                versions.put(v.getReleaseName(), v);
                timeStamps.put(v.getTimestamp(), v);
            }
            // add current version to the maps
            AppVersion v = resultCollection.getCurrentAppVersion();
            versions.put(v.getReleaseName(), v);
            timeStamps.put(v.getTimestamp(), v);

            trimToVersion = edu.umd.cs.findbugs.workflow.Filter.FilterCommandLine.getVersionNum(versions, timeStamps,
                    commandLine.trimToVersionAsString, true, v.getSequenceNumber());
            if (trimToVersion < origCollection.getSequenceNumber()) {
                String name = resultCollection.getAppVersionFromSequenceNumber(trimToVersion).getReleaseName();
                long timestamp = resultCollection.getAppVersionFromSequenceNumber(trimToVersion).getTimestamp();
                resultCollection.setReleaseName(name);
                resultCollection.setTimestamp(timestamp);
                resultCollection.trimAppVersions(trimToVersion);
            }

        }

        if (commandLine.maxAgeSpecified || commandLine.notAProblemSpecified || commandLine.shouldFixSpecified) {

            Cloud cloud = origCollection.getCloud();
            SigninState signinState = cloud.getSigninState();
            if (!signinState.canDownload()) {
                disconnect(verbose, commandLine, resultCollection,  cloud.getCloudName() + " state is " + signinState
                        + "; ignoring filtering options that require cloud access");

            } else if (!cloud.waitUntilIssueDataDownloaded(20, TimeUnit.SECONDS)) {
                if (verbose) {
                    System.out.println("Waiting for cloud information required for filtering");
                }
                if (!cloud.waitUntilIssueDataDownloaded(60, TimeUnit.SECONDS)) {
                    disconnect(verbose, commandLine, resultCollection,
                            "Unable to connect to cloud; ignoring filtering options that require cloud access");
                }
            }
        }

        commandLine.getReady(origCollection);

        for (BugInstance bug : origCollection.getCollection()) {
            if (commandLine.accept(origCollection, bug)) {
                if (trimToVersion >= 0) {
                    if (bug.getFirstVersion() > trimToVersion) {
                        dropped++;
                        continue;
                    } else if (bug.getLastVersion() >= trimToVersion) {
                        bug.setLastVersion(-1);
                        bug.setRemovedByChangeOfPersistingClass(false);
                    }
                }
                resultCollection.add(bug, false);
                passed++;
            } else {
                dropped++;
            }
        }

        if (commandLine.purgeHistorySpecified && commandLine.purgeHistory) {
            resultCollection.clearAppVersions();
            for (BugInstance bug : resultCollection.getCollection()) {
                bug.clearHistory();
            }


        }
        if (verbose) {
            System.out.println(passed + " warnings passed through, " + dropped + " warnings dropped");
        }
        if (commandLine.withSourceSpecified && commandLine.withSource && !commandLine.dontUpdateStats
                && projectStats.hasClassStats()) {
            for (PackageStats stats : projectStats.getPackageStats()) {
                Iterator<ClassStats> i = stats.getClassStats().iterator();
                while (i.hasNext()) {
                    String className = i.next().getName();
                    if (sourceSearcher.sourceNotFound.contains(className) || !sourceSearcher.sourceFound.contains(className)
                            && !sourceSearcher.findSource(SourceLineAnnotation.createReallyUnknown(className))) {
                        i.remove();
                    }
                }
            }

        }
        projectStats.recomputeFromComponents();
        if (argCount == args.length) {
            assert !verbose;
            resultCollection.writeXML(System.out);
        } else {
            resultCollection.writeXML(args[argCount++]);

        }

    }



    private static void disconnect(boolean verbose, final FilterCommandLine commandLine, SortedBugCollection resultCollection,
            String msg) {
        if (verbose) {
            System.out.println(msg);
        }
        resultCollection.addError(msg);
        commandLine.maxAgeSpecified = commandLine.notAProblemSpecified = commandLine.shouldFixSpecified = false;
    }

}

