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

package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.OverridingMethodsMustInvokeSuper;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugInstance.XmlProps;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PackageStats;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.cloud.username.NameLookup;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Multiset;

/**
 * @author William Pugh
 */
public abstract class AbstractCloud implements Cloud {

    public static long MIN_TIMESTAMP = new Date(96, 0, 23).getTime();

    protected static final boolean THROW_EXCEPTION_IF_CANT_CONNECT = false;

    private static final Mode DEFAULT_VOTING_MODE = Mode.COMMUNAL;

    private static final Logger LOGGER = Logger.getLogger(AbstractCloud.class.getName());

    private static final String LEADERBOARD_BLACKLIST = SystemProperties.getProperty("findbugs.leaderboard.blacklist");

    private static final Pattern LEADERBOARD_BLACKLIST_PATTERN;

    static {
        Pattern p = null;
        if (LEADERBOARD_BLACKLIST != null) {
            try {
                p = Pattern.compile(LEADERBOARD_BLACKLIST.replace(',', '|'));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not load leaderboard blacklist pattern", e);
            }
        }
        LEADERBOARD_BLACKLIST_PATTERN = p;

    }

    protected final CloudPlugin plugin;

    protected final BugCollection bugCollection;

    protected final PropertyBundle properties;

    @CheckForNull
    private Pattern sourceFileLinkPattern;

    private String sourceFileLinkFormat;

    private String sourceFileLinkFormatWithLine;

    private String sourceFileLinkToolTip;

    private final CopyOnWriteArraySet<CloudListener> listeners = new CopyOnWriteArraySet<CloudListener>();

    private final CopyOnWriteArraySet<CloudStatusListener> statusListeners = new CopyOnWriteArraySet<CloudStatusListener>();

    private Mode mode = Mode.COMMUNAL;

    private String statusMsg;

    private SigninState signinState = SigninState.UNAUTHENTICATED;
    private boolean issueDataDownloaded = false;

    protected AbstractCloud(CloudPlugin plugin, BugCollection bugs, Properties properties) {
        this.plugin = plugin;
        this.bugCollection = bugs;
        this.properties = plugin.getProperties().copy();
        if (!properties.isEmpty()) {
            this.properties.loadProperties(properties);
        }
    }

    boolean abstractCloudInitialized = false;
    @Override
    public boolean isInitialized() {
        return abstractCloudInitialized;
    }
    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean initialize() throws IOException {
        abstractCloudInitialized = true;
        String modeString = getCloudProperty("votingmode");
        Mode newMode = DEFAULT_VOTING_MODE;
        if (modeString != null) {
            try {
                newMode = Mode.valueOf(modeString.toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "No such voting mode " + modeString, e);
            }
        }
        setMode(newMode);

        String sp = properties.getProperty("findbugs.sourcelink.pattern");
        String sf = properties.getProperty("findbugs.sourcelink.format");
        String sfwl = properties.getProperty("findbugs.sourcelink.formatWithLine");

        String stt = properties.getProperty("findbugs.sourcelink.tooltip");
        if (sp != null && sf != null) {
            try {
                this.sourceFileLinkPattern = Pattern.compile(sp);
                this.sourceFileLinkFormat = sf;
                this.sourceFileLinkToolTip = stt;
                this.sourceFileLinkFormatWithLine = sfwl;
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Could not compile pattern " + sp, e);
                if (THROW_EXCEPTION_IF_CANT_CONNECT) {
                    throw e;
                }
            }
        }
        return true;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    @Override
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public CloudPlugin getPlugin() {
        return plugin;
    }

    @Override
    public BugCollection getBugCollection() {
        return bugCollection;
    }

    @Override
    public boolean supportsBugLinks() {
        return false;
    }

    @Override
    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType)
            throws IOException, SignInCancelledException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBugStatusCache(BugInstance b, String status) {
        throw new UnsupportedOperationException();
    }
    @Override
    public boolean supportsClaims() {
        return false;
    }

    @Override
    public boolean supportsCloudReports() {
        return true;
    }

    @Override
    public String claimedBy(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean claim(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URL getBugLink(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBugLinkType(BugInstance instance) {
        return null;
    }

    @Override
    public URL fileBug(BugInstance bug) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BugFilingStatus getBugLinkStatus(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    public boolean canSeeCommentsByOthers(BugInstance bug) {
        switch (getMode()) {
        case SECRET:
            return false;
        case COMMUNAL:
            return true;
        case VOTING:
            return hasVoted(bug);
        }
        throw new IllegalStateException();
    }

    public boolean hasVoted(BugInstance bug) {
        for (BugDesignation bd : getLatestDesignationFromEachUser(bug)) {
            if (getUser().equals(bd.getUser())) {
                return true;
            }
        }
        return false;
    }

    public String notInCloudMsg(BugInstance b) {

        if (!!isOnlineCloud()) {
            return "off line cloud";
        }
        if (getSigninState().canDownload()) {
            return "disconnected from cloud";
        }
        if (!issueDataDownloaded) {
            return "Waiting for issue data...";
        }
        return  "Issue not recorded in cloud";

    }
    @Override
    public String getCloudReport(BugInstance b) {
        return getSelectiveCloudReport(b, Collections.<String>emptySet());
    }

    @Override
    public String getCloudReportWithoutMe(BugInstance b) {
        String user = getUser();
        Set<String> usersToExclude = user == null ? Collections.<String>emptySet() :  Collections.singleton(user);
        return getSelectiveCloudReport(b, usersToExclude);
    }

    @Override
    public void bugsPopulated() {
        issueDataDownloaded = false;
    }

    private String getSelectiveCloudReport(BugInstance b, Set<String> usersToExclude) {
        if (!isInCloud(b)) {
            return notInCloudMsg(b);
        }
        initiateCommunication();
        SimpleDateFormat format = new SimpleDateFormat("MM/dd, yyyy", Locale.ENGLISH);
        StringBuilder builder = new StringBuilder();
        long firstSeen = getFirstSeen(b);
        builder.append(String.format("First seen %s%n", format.format(new Date(firstSeen))));
        builder.append("\n");

        I18N i18n = I18N.instance();
        boolean canSeeCommentsByOthers = canSeeCommentsByOthers(b);
        if (canSeeCommentsByOthers && supportsBugLinks()) {
            BugFilingStatus bugLinkStatus = getBugLinkStatus(b);
            if (bugLinkStatus != null && bugLinkStatus.bugIsFiled()) {

                builder.append("\nBug status is ").append(getBugStatus(b));
                if (getBugIsUnassigned(b)) {
                    builder.append("\nBug is unassigned");
                }

                builder.append("\n\n");
            }
        }
        String me = getUser();
        for (BugDesignation d : getLatestDesignationFromEachUser(b)) {
            if (!usersToExclude.contains(d.getUser())
                    && (me != null && me.equals(d.getUser()) || canSeeCommentsByOthers)) {
                builder.append(String.format("%s@ %s: %s%n", d.getUser() == null ? "" : d.getUser() + " ",
                        format.format(new Date(d.getTimestamp())),
                        i18n.getUserDesignation(d.getDesignationKey())));
                String annotationText = d.getAnnotationText();
                if (annotationText != null && annotationText.length() > 0) {
                    builder.append(annotationText);
                    builder.append("\n\n");
                }
            }
        }
        return builder.toString();
    }

    protected boolean issueDataHasBeenDownloaded() {
        return false;
    }

    @Override
    public String getBugStatus(BugInstance b) {
        return null;
    }

    protected abstract Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance bd);

    @Override
    public Date getUserDate(BugInstance b) {
        return new Date(getUserTimestamp(b));
    }

    @Override
    public void addListener(CloudListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(CloudListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addStatusListener(CloudStatusListener listener) {
        if (listener == null) {
            throw new NullPointerException();
        }
        if (!statusListeners.contains(listener)) {
            statusListeners.add(listener);
        }
    }

    @Override
    public void removeStatusListener(CloudStatusListener listener) {
        statusListeners.remove(listener);
    }

    @Override
    public String getStatusMsg() {
        return statusMsg;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean getIWillFix(BugInstance b) {
        return getUserDesignation(b) == UserDesignation.I_WILL_FIX;
    }

    @Override
    public UserDesignation getConsensusDesignation(BugInstance b) {
        if (b == null) {
            throw new NullPointerException("null bug instance");
        }
        Multiset<UserDesignation> designations = new Multiset<UserDesignation>();
        int count = 0;
        int totalCount = 0;
        double total = 0.0;
        int isAProblem = 0;
        int notAProblem = 0;
        for (BugDesignation designation : getLatestDesignationFromEachUser(b)) {
            UserDesignation d = UserDesignation.valueOf(designation.getDesignationKey());
            if (d == UserDesignation.I_WILL_FIX) {
                d = UserDesignation.MUST_FIX;
            } else if (d == UserDesignation.UNCLASSIFIED) {
                continue;
            }
            switch (d) {
            case I_WILL_FIX:
            case MUST_FIX:
            case SHOULD_FIX:
                isAProblem++;
                break;
            case BAD_ANALYSIS:
            case NOT_A_BUG:
            case MOSTLY_HARMLESS:
            case OBSOLETE_CODE:
                notAProblem++;
                break;
            default:
                break;
            }
            designations.add(d);
            totalCount++;
            if (d.nonVoting()) {
                continue;
            }
            count++;
            total += d.score();
        }
        if (totalCount == 0) {
            return UserDesignation.UNCLASSIFIED;
        }
        UserDesignation mostCommonVotingDesignation = null;
        UserDesignation mostCommonDesignation = null;

        for (Map.Entry<UserDesignation, Integer> e : designations.entriesInDecreasingFrequency()) {
            UserDesignation d = e.getKey();
            if (mostCommonVotingDesignation == null && !d.nonVoting()) {
                mostCommonVotingDesignation = d;
                if (e.getValue() > count / 2) {
                    return d;
                }
            }
            if (mostCommonDesignation == null && d != UserDesignation.UNCLASSIFIED) {
                mostCommonDesignation = d;
                if (e.getValue() > count / 2) {
                    return d;
                }
            }
        }

        double score = total / count;
        if (score >= UserDesignation.SHOULD_FIX.score() || isAProblem > notAProblem) {
            return UserDesignation.SHOULD_FIX;
        }
        if (score <= UserDesignation.NOT_A_BUG.score()) {
            return UserDesignation.NOT_A_BUG;
        }
        if (score <= UserDesignation.MOSTLY_HARMLESS.score() || notAProblem > isAProblem) {
            return UserDesignation.MOSTLY_HARMLESS;
        }
        return UserDesignation.NEEDS_STUDY;

    }

    @Override
    public boolean overallClassificationIsNotAProblem(BugInstance b) {
        UserDesignation consensusDesignation = getConsensusDesignation(b);
        return consensusDesignation.notAProblem();
    }

    @Override
    public double getClassificationScore(BugInstance b) {

        int count = 0;
        double total = 0.0;
        for (BugDesignation designation : getLatestDesignationFromEachUser(b)) {
            UserDesignation d = UserDesignation.valueOf(designation.getDesignationKey());
            if (d.nonVoting()) {
                continue;
            }
            count++;
            total += d.score();
        }
        return total / count;

    }

    @Override
    public double getClassificationVariance(BugInstance b) {

        int count = 0;
        double total = 0.0;
        double totalSquares = 0.0;
        for (BugDesignation designation : getLatestDesignationFromEachUser(b)) {
            UserDesignation d = UserDesignation.valueOf(designation.getDesignationKey());
            if (d.nonVoting()) {
                continue;
            }
            count++;
            total += d.score();
            totalSquares += d.score() * d.score();
        }
        double average = total / count;
        return totalSquares / count - average * average;

    }

    @Override
    public double getPortionObsoleteClassifications(BugInstance b) {
        int count = 0;
        double total = 0.0;
        for (BugDesignation designation : getLatestDesignationFromEachUser(b)) {
            count++;
            UserDesignation d = UserDesignation.valueOf(designation.getDesignationKey());
            if (d == UserDesignation.OBSOLETE_CODE) {
                total++;
            }
        }
        return total / count;
    }

    @Override
    public int getNumberReviewers(BugInstance b) {
        int count = 0;
        Iterable<BugDesignation> designations = getLatestDesignationFromEachUser(b);
        // noinspection UnusedDeclaration
        for (BugDesignation designation : designations) {
            count++;
        }
        return count;
    }

    @Override
    @SuppressWarnings("boxing")
    public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes) {

        Multiset<String> evaluations = new Multiset<String>();
        Multiset<String> designations = new Multiset<String>();
        Multiset<String> bugStatus = new Multiset<String>();

        int issuesWithThisManyReviews[] = new int[100];
        I18N i18n = I18N.instance();

        int packageCount = 0;
        int classCount = 0;
        int ncss = 0;
        ProjectStats projectStats = bugCollection.getProjectStats();
        for (PackageStats ps : projectStats.getPackageStats()) {
            int num = ps.getNumClasses();
            if (ClassName.matchedPrefixes(packagePrefixes, ps.getPackageName()) && num > 0) {
                packageCount++;
                ncss += ps.size();
                classCount += num;
            }
        }

        if (classCount == 0) {
            w.println("No classes were analyzed");
            return;
        }
        if (packagePrefixes != null && packagePrefixes.length > 0) {
            String lst = Arrays.asList(packagePrefixes).toString();
            w.println("Code analyzed in " + lst.substring(1, lst.length() - 1));
        } else {
            w.println("Code analyzed");
        }
        w.printf("%,7d packages%n%,7d classes%n", packageCount, classCount);
        if (ncss > 0) {
            w.printf("%,7d thousands of lines of non-commenting source statements%n", (ncss + 999) / 1000);
        }
        w.println();
        int count = 0;
        for (BugInstance bd : bugs) {

            count++;
            HashSet<String> reviewers = new HashSet<String>();
            String status = supportsBugLinks() && getBugLinkStatus(bd).bugIsFiled() ? getBugStatus(bd) : null;
            if (status != null) {
                bugStatus.add(status);
            }

            for (BugDesignation d : getLatestDesignationFromEachUser(bd)) {
                if (reviewers.add(d.getUser())) {
                    evaluations.add(d.getUser());
                    designations.add(i18n.getUserDesignation(d.getDesignationKey()));
                }
            }

            int numReviews = Math.min(reviewers.size(), issuesWithThisManyReviews.length - 1);
            issuesWithThisManyReviews[numReviews]++;

        }
        if (count == getBugCollection().getCollection().size()) {
            w.printf("Summary for %d issues%n%n", count);
        } else {
            w.printf("Summary for %d issues that are in the current view%n%n", count);
        }
        if (evaluations.numKeys() == 0) {
            w.println("No reviews found");
        } else {
            w.println("People who have performed the most reviews");
            printLeaderBoard(w, evaluations, 9, getUser(), true, "reviewer");
            w.println();
            w.println("Distribution of reviews");
            printLeaderBoard(w, designations, 100, " --- ", false, "designation");
        }

        if (supportsBugLinks()) {
            if (bugStatus.numKeys() == 0) {
                w.println();
                w.println("No bugs filed");
            } else {
                w.println();
                w.println("Distribution of bug status");
                printLeaderBoard(w, bugStatus, 100, " --- ", false, "status of filed bug");
            }
        }
        w.println();
        w.println("Distribution of number of reviews");
        for (int i = 0; i < issuesWithThisManyReviews.length; i++) {
            if (issuesWithThisManyReviews[i] > 0) {
                w.printf("%4d  with %3d review", issuesWithThisManyReviews[i], i);
                if (i != 1) {
                    w.print("s");
                }
                w.println();

            }
        }
    }

    @SuppressWarnings("boxing")
    public static void printLeaderBoard2(PrintWriter w, Multiset<String> evaluations, int maxRows, String alwaysPrint,
            String format, String title) {
        int row = 1;
        int position = 0;
        int previousScore = -1;
        boolean foundAlwaysPrint = false;

        for (Map.Entry<String, Integer> e : evaluations.entriesInDecreasingFrequency()) {
            int num = e.getValue();
            if (num != previousScore) {
                position = row;
                previousScore = num;
            }
            String key = e.getKey();
            if (LEADERBOARD_BLACKLIST_PATTERN != null && LEADERBOARD_BLACKLIST_PATTERN.matcher(key).matches()) {
                continue;
            }

            boolean shouldAlwaysPrint = key.equals(alwaysPrint);
            if (row <= maxRows || shouldAlwaysPrint) {
                w.printf(format, position, num, key);
            }

            if (shouldAlwaysPrint) {
                foundAlwaysPrint = true;
            }
            row++;
            if (row >= maxRows) {
                if (alwaysPrint == null) {
                    break;
                }
                if (foundAlwaysPrint) {
                    w.printf("Total of %d %ss%n", evaluations.numKeys(), title);
                    break;
                }
            }

        }
    }

    @Override
    public boolean supportsCloudSummaries() {
        return true;
    }

    @Override
    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
        return true;
    }

    @Override
    public double getClassificationDisagreement(BugInstance b) {
        return 0;
    }

    @Override
    public UserDesignation getUserDesignation(BugInstance b) {
        BugDesignation bd = getPrimaryDesignation(b);
        if (bd == null) {
            return UserDesignation.UNCLASSIFIED;
        }
        return UserDesignation.valueOf(bd.getDesignationKey());
    }

    @Override
    public String getUserEvaluation(BugInstance b) {
        BugDesignation bd = getPrimaryDesignation(b);
        if (bd == null) {
            return "";
        }
        String result = bd.getAnnotationText();
        if (result == null) {
            return "";
        }
        return result;
    }

    @Override
    public long getUserTimestamp(BugInstance b) {
        BugDesignation bd = getPrimaryDesignation(b);
        if (bd == null) {
            return Long.MAX_VALUE;
        }
        return bd.getTimestamp();

    }

    @Override
    public long getFirstSeen(BugInstance b) {
        return getLocalFirstSeen(b);
    }

    @Override
    public void addDateSeen(BugInstance b, long when) {
        throw new UnsupportedOperationException();
    }


    // ==================== end of public methods ==================

    protected void updatedStatus() {
        for (CloudListener listener : listeners) {
            try {
                listener.statusUpdated();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing callback " + listener, e);
            }
        }
    }

    public void updatedIssue(BugInstance bug) {
        for (CloudListener listener : listeners) {
            try {
                listener.issueUpdated(bug);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error executing callback " + listener, e);
            }
        }
    }

    protected void fireIssueDataDownloadedEvent() {
        issueDataDownloaded = true;
        for (CloudStatusListener statusListener : statusListeners) {
            statusListener.handleIssueDataDownloadedEvent();
        }
    }

    @Override
    public SigninState getSigninState() {
        return signinState;
    }

    public void setSigninState(SigninState state) {
        SigninState oldState = this.signinState;
        if (oldState == state) {
            return;
        }
        LOGGER.log(Level.FINER, "State " + oldState + " -> " + state, new Throwable("Change in login state at:"));
        this.signinState = state;
        for (CloudStatusListener statusListener : statusListeners) {
            statusListener.handleStateChange(oldState, state);
        }
    }

    public BugInstance getBugByHash(String hash) {
        for (BugInstance instance : bugCollection.getCollection()) {
            if (instance.getInstanceHash().equals(hash)) {
                return instance;
            }
        }
        return null;
    }

    protected NameLookup getUsernameLookup() throws IOException {
        NameLookup lookup;
        try {
            lookup = plugin.getUsernameClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain username", e);
        }
        if (!lookup.signIn(plugin, bugCollection)) {
            throw new RuntimeException("Unable to obtain username");
        }
        return lookup;

    }

    public MutableCloudTask createTask(final String name) {
        MutableCloudTask task = new MutableCloudTask(name);
        for (CloudListener listener : listeners) {
            listener.taskStarted(task);
        }
        task.setDefaultListener(new CloudTaskListener() {
            @Override
            public void taskStatusUpdated(String statusLine, double percentCompleted) {
                setStatusMsg(name + "... " + statusLine);
            }

            @Override
            public void taskFinished() {
                setStatusMsg("");
            }

            @Override
            public void taskFailed(String message) {
                setStatusMsg(name + "... FAILED - " + message);
            }
        });
        if (task.isUsingDefaultListener()) {
            setStatusMsg(name);
        }
        return task;
    }

    public void setStatusMsg(String newMsg) {
        this.statusMsg = newMsg;
        updatedStatus();
    }

    private static void printLeaderBoard(PrintWriter w, Multiset<String> evaluations, int maxRows, String alwaysPrint,
            boolean listRank, String title) {
        if (listRank) {
            w.printf("%3s %4s %s%n", "rnk", "num", title);
        } else {
            w.printf("%4s %s%n", "num", title);
        }
        printLeaderBoard2(w, evaluations, maxRows, alwaysPrint, listRank ? "%3d %4d %s%n" : "%2$4d %3$s%n", title);
    }

    protected String getCloudProperty(String propertyName) {
        return properties.getProperty("findbugs.cloud." + propertyName);
    }

    @Override
    public boolean supportsSourceLinks() {
        return sourceFileLinkPattern != null;
    }

    @Override
    @SuppressWarnings("boxing")
    public @CheckForNull URL getSourceLink(BugInstance b) {
        if (sourceFileLinkPattern == null) {
            return null;
        }

        SourceLineAnnotation src = b.getPrimarySourceLineAnnotation();
        String fileName = src.getSourcePath();
        int startLine = src.getStartLine();
        int endLine = src.getEndLine();
        java.util.regex.Matcher m = sourceFileLinkPattern.matcher(fileName);
        boolean isMatch = m.matches();
        if (isMatch) {
            try {
                URL link;
                if (startLine > 0) {
                    link = new URL(String.format(sourceFileLinkFormatWithLine, m.group(1),
                            startLine, startLine - 10, endLine));
                } else {
                    link = new URL(String.format(sourceFileLinkFormat, m.group(1)));
                }
                return link;
            } catch (Exception e) {
                AnalysisContext.logError("Error generating source link for " + src, e);
            }
        }

        return null;

    }

    @Override
    public String getSourceLinkToolTip(BugInstance b) {
        return sourceFileLinkToolTip;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.cloud.Cloud#getBugIsUnassigned(edu.umd.cs.findbugs
     * .BugInstance)
     */
    @Override
    public boolean getBugIsUnassigned(BugInstance b) {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.cloud.Cloud#getWillNotBeFixed(edu.umd.cs.findbugs
     * .BugInstance)
     */
    @Override
    public boolean getWillNotBeFixed(BugInstance b) {
        return false;
    }

    @Override
    public Set<String> getReviewers(BugInstance b) {
        HashSet<String> result = new HashSet<String>();
        for (BugDesignation d : getLatestDesignationFromEachUser(b)) {
            result.add(d.getUser());
        }
        return result;
    }

    @Override
    public IGuiCallback getGuiCallback() {
        return getBugCollection().getProject().getGuiCallback();
    }

    @Override
    public String getCloudName() {
        return getPlugin().getDescription();
    }

    @Override
    public boolean communicationInitiated() {
        return !isOnlineCloud();
    }

    public long getLocalFirstSeen(BugInstance b) {
        long firstVersion = b.getFirstVersion();
        AppVersion v = getBugCollection().getAppVersionFromSequenceNumber(firstVersion);
        if (v == null) {
            return getBugCollection().getTimestamp();
        }
        long firstSeen = v.getTimestamp();
        if (b.hasXmlProps()) {
            XmlProps props = b.getXmlProps();
            Date propsFirstSeen = props.getFirstSeen();
            if (propsFirstSeen != null && firstSeen > propsFirstSeen.getTime()) {
                firstSeen = propsFirstSeen.getTime();
            }
        }
        return firstSeen;
    }

}
