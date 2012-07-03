package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import com.google.protobuf.GeneratedMessage;

import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.cloud.MutableCloudTask;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UpdateIssueTimestamps.IssueGroup;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues.Builder;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;
import edu.umd.cs.findbugs.util.Util;

public class WebCloudNetworkClient {
    private static final Logger LOGGER = Logger.getLogger(WebCloudNetworkClient.class.getPackage().getName());

    /** For debugging */
    private static final boolean FORCE_UPLOAD_ALL_ISSUES = false;

    private static final int GLOBAL_HTTP_SOCKET_TIMEOUT = 5000;

    private static final int BUG_UPLOAD_PARTITION_SIZE = 5;

    /** For updating firstSeen timestamps */
    private static final int BUG_UPDATE_PARTITION_SIZE = 10;

    /**
     * This needs to stay low to prevent overloading the App Engine clusters
     * with long-running requests.
     */
    private static final int HASH_CHECK_PARTITION_SIZE = 20;

    private WebCloudClient cloudClient;

    private WebCloudNameLookup lookerupper;

    private ConcurrentMap<String, Issue> issuesByHash = new ConcurrentHashMap<String, Issue>();

    private String host;

    private Long sessionId;

    private String username;

    private volatile long earliestSeenServerTime = System.currentTimeMillis();
    private volatile long maxRecentEvaluationMillis = 0;

    private CopyOnWriteArrayList<String> timestampsToUpdate = new CopyOnWriteArrayList<String>();

    public void setCloudClient(WebCloudClient webCloudClient) {
        this.cloudClient = webCloudClient;
    }

    /** returns whether soft initialization worked and the user is now signed in */
    public boolean initialize() throws IOException {
        lookerupper = createNameLookup();
        lookerupper.softSignin();
        this.sessionId = lookerupper.getSessionId();
        this.username = lookerupper.getUsername();
        this.host = lookerupper.getHost();
        return this.sessionId != null;
    }

    public void signIn(boolean force) throws IOException {
        if (!force && sessionId != null)
            throw new IllegalStateException("already signed in");
        if (!lookerupper.signIn(cloudClient.getPlugin(), cloudClient.getBugCollection())) {
            getGuiCallback().setErrorMessage("Signing into " + cloudClient.getCloudName() + " failed!");
            return;
        }
        this.sessionId = lookerupper.getSessionId();
        this.username = lookerupper.getUsername();
        this.host = lookerupper.getHost();
        if (getUsername() == null || host == null) {
            throw new IllegalStateException("No App Engine Cloud username or hostname found! Check etc/findbugs.xml");
        }
        // now that we know our own username, we need to update all the bugs in
        // the UI to show what "our"
        // designation & comments are.
        // this might be really slow with a lot of issues. seems fine so far.
        for (BugInstance instance : cloudClient.getBugCollection().getCollection()) {
            Issue issue = issuesByHash.get(instance.getInstanceHash());
            if (issue != null && issue.getEvaluationsCount() > 0) {
                cloudClient.updateBugInstanceAndNotify(instance);
            }
        }
    }

    public void setBugLinkOnCloud(final BugInstance b, final String type, final String bugLink)
            throws IOException, SignInCancelledException {
        cloudClient.signInIfNecessary("To store the bug URL on the " + cloudClient.getCloudName()
                + ", you must sign in.");

        RetryableConnection<Object> conn = new RetryableConnection<Object>("/set-bug-link", true) {
            @Override
            public void write(OutputStream out) throws IOException {
                SetBugLink.newBuilder().setSessionId(sessionId).setHash(WebCloudProtoUtil.encodeHash(b.getInstanceHash())).setBugLinkType(type)
                    .setUrl(bugLink).build().writeTo(out);
            }

            @Override
            public Object finish(int responseCode, String responseMessage, InputStream response) {
                if (responseCode != 200) {
                    throw new IllegalStateException("server returned error code " + responseCode + " "
                            + responseMessage);
                }
                return null;
            }
        };
        conn.go();
    }

    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType) throws IOException,
            SignInCancelledException {
        setBugLinkOnCloud(b, linkType, viewUrl);

        String hash = b.getInstanceHash();
        storeIssueDetails(hash, Issue.newBuilder(getIssueByHash(hash))
                .setBugLink(viewUrl)
                .setBugLinkTypeStr(linkType)
                .build());
    }

    public void logIntoCloudForce() throws IOException {
        RetryableConnection<Object> conn = new RetryableConnection<Object>("/log-in", true) {
            @Override
            public void write(OutputStream out) throws IOException {
                LogIn logIn = LogIn.newBuilder().setSessionId(sessionId)
                        .setAnalysisTimestamp(cloudClient.getBugCollection().getAnalysisTimestamp()).build();
                logIn.writeTo(out);
            }

            @Override
            public Object finish(int responseCode, String responseMessage, InputStream response) {
                if (responseCode != 200) {
                    throw new IllegalStateException("Could not log into cloud with ID " + sessionId
                            + " - error " + responseCode + " " + responseMessage);
                }
                return null;
            }
        };
        conn.go();
    }

    public void generateHashCheckRunnables(final MutableCloudTask task, final List<String> hashes,
                                           List<Callable<Object>> tasks,
                                           final ConcurrentMap<String, BugInstance> bugsByHash) {
        final int numBugs = hashes.size();
        final AtomicInteger numberOfBugsCheckedSoFar = new AtomicInteger();
        for (int i = 0; i < numBugs; i += HASH_CHECK_PARTITION_SIZE) {
            final List<String> partition = hashes.subList(i, Math.min(i + HASH_CHECK_PARTITION_SIZE, numBugs));
            tasks.add(new Callable<Object>() {
                public Object call() throws Exception {
                    checkHashesPartition(partition, bugsByHash);
                    numberOfBugsCheckedSoFar.addAndGet(partition.size());
                    int sofar = numberOfBugsCheckedSoFar.get();
                    task.update("Checked " + sofar + " of " + numBugs, (sofar * 100.0 / numBugs));
                    return null;
                }
            });
        }
    }

    public CopyOnWriteArrayList<String> getTimestampsToUpdate() {
        return timestampsToUpdate;
    }

    public MutableCloudTask generateUpdateTimestampRunnables(List<Callable<Void>> callables)
            throws SignInCancelledException {
        List<String> timestamps = new ArrayList<String>(timestampsToUpdate);
        final int bugCount = timestamps.size();
        if (bugCount == 0)
            return null;

        final List<BugInstance> bugs = new ArrayList<BugInstance>();
        long biggestDiffMs = 0;
        boolean someZeroOnCloud = false;

        long earliestFirstSeen = Long.MAX_VALUE;
        for (String hash : timestamps) {
            BugInstance bug = cloudClient.getBugByHash(hash);
            if (bug != null) {
                bugs.add(bug);
                long firstSeenFromCloud = getFirstSeenFromCloud(bug);
                long localFirstSeen = cloudClient.getLocalFirstSeen(bug);
                long diffMs = firstSeenFromCloud - localFirstSeen;
                if (diffMs > biggestDiffMs) {
                    biggestDiffMs = diffMs;
                    earliestFirstSeen = Math.min(earliestFirstSeen, localFirstSeen);
                } else if (firstSeenFromCloud == 0 && localFirstSeen != 0)
                    someZeroOnCloud = true;
            }
        }
        if (!someZeroOnCloud && biggestDiffMs < 1000 * 60)
            // less than 1 minute off
            return null;

        // if some bugs have a zero timestamp, let's not bother telling the user
        // anything specific
        String durationStr;
        if (someZeroOnCloud)
            durationStr = "";
        else
            durationStr = " up to " + toDuration(biggestDiffMs);

        Calendar now = Calendar.getInstance();
        TimeZone timeZone = now.getTimeZone();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        String timeStr = df.format(now.getTime());
        boolean daylight = timeZone.inDaylightTime(now.getTime());
        String zoneStr = timeZone.getDisplayName(daylight, TimeZone.LONG) + " ("
                + timeZone.getDisplayName(daylight, TimeZone.SHORT) + ")";

        Calendar earliest = Calendar.getInstance(timeZone);
        earliest.setTimeInMillis(earliestFirstSeen);
        String earliestStr = df.format(earliest.getTime());

        int result = getGuiCallback().showConfirmDialog(
                "Your first-seen dates for " + bugCount + " bugs are " + durationStr
                        + " earlier than those on the " + cloudClient.getCloudName() + ".\n" + "The earliest first-seen from the local analysis is "
                        + earliestStr + "\n" + "Would you like to back date the first-seen dates on the Cloud?\n" + "\n"
                        + "Current time: " + timeStr + " " + zoneStr + "\n"
                        + "(If you're not sure the time and time zone are correct, click Cancel.)",
                cloudClient.getCloudName(), "Update",
                "Cancel");
        if (result != 0)
            return null;

        cloudClient.signInIfNecessary(null);

        // since the protocol groups bugs by timestamp, let's optimize network
        // bandwidth by sorting first. probably
        // doesn't matter.
        Collections.sort(bugs, new Comparator<BugInstance>() {
            public int compare(BugInstance o1, BugInstance o2) {
                long v1 = o1.getFirstVersion();
                long v2 = o2.getFirstVersion();
                if (v1 < v2)
                    return -1;
                if (v1 > v2)
                    return 1;
                return 0;
            }
        });

        final MutableCloudTask task = cloudClient.createTask("Updating " + cloudClient.getCloudName());
        final AtomicInteger soFar = new AtomicInteger(0);
        for (int i = 0; i < bugCount; i += BUG_UPDATE_PARTITION_SIZE) {
            final List<BugInstance> partition = bugs.subList(i, Math.min(bugCount, i + BUG_UPLOAD_PARTITION_SIZE));

            callables.add(new Callable<Void>() {
                public Void call() throws Exception {
                    updateTimestampsNow(partition);
                    int updated = soFar.addAndGet(partition.size());
                    task.update("Updated " + updated + " of " + bugCount + " timestamps", updated * 100.0 / bugCount);
                    return null;
                }
            });
        }
        return task;
    }
    
    public MutableCloudTask generateUploadRunnables(final List<BugInstance> newBugs, List<Callable<Void>> callables)
            throws SignInCancelledException {
        final int bugCount = newBugs.size();
        if (bugCount == 0)
            return null;
        if (cloudClient.getCloudTokenProperty() == null && cloudClient.getSigninState().shouldAskToSignIn())
            cloudClient.signInIfNecessary("Some bugs were not found on the " + cloudClient.getCloudName() + ".\n"
                    + "Your signin status is " +  cloudClient.getSigninState()  +"\n"
                    + "Would you like to sign in and upload them to the Cloud?");
        final MutableCloudTask task = cloudClient.createTask("Uploading to the " + cloudClient.getCloudName());
        final AtomicInteger bugsUploaded = new AtomicInteger(0);
        for (int i = 0; i < bugCount; i += BUG_UPLOAD_PARTITION_SIZE) {
            final List<BugInstance> partition = newBugs.subList(i, Math.min(bugCount, i + BUG_UPLOAD_PARTITION_SIZE));
            callables.add(new Callable<Void>() {
                public Void call() throws Exception {
                    uploadNewBugsPartition(partition);
                    bugsUploaded.addAndGet(partition.size());
                    int uploaded = bugsUploaded.get();
                    task.update("Uploaded " + uploaded + " of " + bugCount + " issues", uploaded * 100.0 / bugCount);
                    return null;
                }
            });
        }
        return task;
    }

    public long getFirstSeenFromCloud(BugInstance b) {
        String instanceHash = b.getInstanceHash();
        Issue issue = issuesByHash.get(instanceHash);

        if (issue == null)
            return Long.MAX_VALUE;
        if (WebCloudClient.DEBUG_FIRST_SEEN)
            System.out.println("First seen is " + issue.getFirstSeen() + " for " + b.getMessage());
        if (issue.getFirstSeen() == 0)
            return Long.MAX_VALUE;
        return issue.getFirstSeen();
    }

    public void storeIssueDetails(String hash, Issue issue) {
        for (Evaluation eval : issue.getEvaluationsList()) {
            if (eval.getWhen() > maxRecentEvaluationMillis) {
                maxRecentEvaluationMillis = eval.getWhen();
            }
        }
        issuesByHash.put(hash, issue);
    }

    public RecentEvaluations getRecentEvaluationsFromServer() throws IOException {
        RetryableConnection<RecentEvaluations> conn = new RetryableConnection<RecentEvaluations>("/get-recent-evaluations", true) {
            @Override
            public void write(OutputStream out) throws IOException {
                GetRecentEvaluations.Builder msgb = GetRecentEvaluations.newBuilder();
                if (sessionId != null) {
                    msgb.setSessionId(sessionId);
                }
                // I'm not sure if we really need mostRecentEvaluationMillis
                // anymore, as earliestSeenServerTime should always be later
                msgb.setTimestamp(Math.max(earliestSeenServerTime, maxRecentEvaluationMillis));

                msgb.build().writeTo(out);
            }

            @Override
            public RecentEvaluations finish(int responseCode, String responseMessage, InputStream response) throws IOException {
                if (responseCode != 200) {
                    throw new ServerReturnedErrorCodeException(responseCode, responseMessage);
                }
                RecentEvaluations evals = RecentEvaluations.parseFrom(response);
                updateMostRecentEvaluationField(evals);
                if (!(evals.hasAskAgain() && evals.getAskAgain()))
                    // only update the server last seen time if we're done checking for evals
                    earliestSeenServerTime = evals.getCurrentServerTime();
                return evals;
            }
        };
        return conn.go();
    }

    public Evaluation getMostRecentEvaluationByUser(BugInstance b, String user) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
            return null;
        Evaluation mostRecent = null;
        long when = Long.MIN_VALUE;
        for (Evaluation e : issue.getEvaluationsList()) {
            if (e.getWho().equals(user) && e.getWhen() > when) {
                mostRecent = e;
                when = e.getWhen();
            }
        }

        return mostRecent;
    }
    public Evaluation getMostRecentEvaluationBySelf(BugInstance b) {
        String myUsername = getUsername();
        return getMostRecentEvaluationByUser(b, myUsername);
    }

    public String getUsername() {
        return username;
    }

    public String getHost() {
        return host;
    }

    @SuppressWarnings({ "deprecation" })
    public void storeUserAnnotation(BugInstance bugInstance) throws SignInCancelledException, IOException {
        // store this stuff first because signIn might clobber it. this is
        // kludgy but works.
    
        BugDesignation designation = bugInstance.getNonnullUserDesignation();
        long timestamp = designation.getTimestamp();
        String designationKey = designation.getDesignationKey();
        String comment = designation.getAnnotationText();
        designation.cleanDirty();

        cloudClient.signInIfNecessary("To store your reviews on the " + cloudClient.getCloudName() + ", you must sign in first.");

        Evaluation.Builder evalBuilder = Evaluation.newBuilder().setWhen(timestamp).setDesignation(designationKey);
        if (comment != null) {
            evalBuilder.setComment(comment);
        }
        String hash = bugInstance.getInstanceHash();
        Evaluation eval = evalBuilder.build();
        UploadEvaluation uploadMsg = UploadEvaluation.newBuilder().setSessionId(sessionId).setHash(WebCloudProtoUtil.encodeHash(hash))
                .setEvaluation(eval).build();

        openPostUrl("/upload-evaluation", uploadMsg);

        // store so it shows up in cloud report, etc
        Issue issue = issuesByHash.get(hash);
        if (issue == null)
            // I think this only happens in tests -keith
            return;
        Evaluation evalToStore = username == null ? eval : Evaluation.newBuilder(eval).setWho(username).build();
        Issue.Builder issueToStore = Issue.newBuilder(issue);
        issuesByHash.put(hash, issueToStore.addEvaluations(evalToStore).build());
        cloudClient.updateBugInstanceAndNotify(bugInstance);
    }

    public @CheckForNull
    Issue getIssueByHash(String hash) {
        return issuesByHash.get(hash);
    }

    public void signOut(boolean background) {
        if (sessionId != null) {
            final long oldSessionId = sessionId;
            sessionId = null;
            Runnable logoutRequest = new Runnable() {
                public void run() {
                    try {
                        openPostUrl("/log-out/" + oldSessionId, null);
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Could not sign out", e);
                    }
                }
            };
            if (background)
                cloudClient.getBackgroundExecutor().execute(logoutRequest);
            else
                logoutRequest.run();
            
            WebCloudNameLookup.clearSavedSessionInformation();
        }
    }

    public boolean ready() {
        return host != null;
    }
    
    public Long getSessionId() {
        return sessionId;
    }

    // ========================= private methods ==========================

    protected WebCloudNameLookup createNameLookup() {
        WebCloudNameLookup nameLookup = new WebCloudNameLookup();
        nameLookup.loadProperties(cloudClient.getPlugin());
        return nameLookup;
    }

    private void updateMostRecentEvaluationField(RecentEvaluations evaluations) {
        for (Issue issue : evaluations.getIssuesList())
            for (Evaluation evaluation : issue.getEvaluationsList())
                if (evaluation.getWhen() > maxRecentEvaluationMillis)
                    maxRecentEvaluationMillis = evaluation.getWhen();
    }

    private void checkHashesPartition(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        FindIssuesResponse response = submitHashes(hashes);
        if (response.hasCurrentServerTime()
                && (response.getCurrentServerTime() < earliestSeenServerTime))
            earliestSeenServerTime = response.getCurrentServerTime();
        int count = Math.min(hashes.size(), response.getFoundIssuesCount());
        if (hashes.size() != response.getFoundIssuesCount()) {
            LOGGER.severe(String.format("Requested %d issues, got %d responses", hashes.size(),  response.getFoundIssuesCount()));
        }

        for (int j = 0; j < count; j++) {
            String hash = hashes.get(j);
            Issue issue = response.getFoundIssues(j);

            if (isEmpty(issue))
                // the issue was not found!
                continue;

            storeIssueDetails(hash, issue);

            BugInstance bugInstance;
            if (FORCE_UPLOAD_ALL_ISSUES)
                bugInstance = bugsByHash.get(hash);
            else
                bugInstance = bugsByHash.remove(hash);

            if (bugInstance == null) {
                LOGGER.warning("Server sent back issue that we don't know about: " + hash + " - " + issue);
                continue;
            }

            long firstSeen = cloudClient.getLocalFirstSeen(bugInstance);
            long cloudFirstSeen = issue.getFirstSeen();
            if (WebCloudClient.DEBUG_FIRST_SEEN)
                System.out.printf("%s %s%n", new Date(firstSeen), new Date(cloudFirstSeen));
            if (firstSeen > 0 && firstSeen < cloudFirstSeen)
                timestampsToUpdate.add(hash);

            cloudClient.updateBugInstanceAndNotify(bugInstance);
        }
    }

    private boolean isEmpty(Issue issue) {
        return !issue.hasFirstSeen() && !issue.hasLastSeen() && issue.getEvaluationsCount() == 0;
    }

    private String toDuration(long ms) {
        long weeks = ms / (1000 * 60 * 60 * 24 * 7);
        if (weeks > 0)
            return plural(weeks, "week");
        long days = ms / (1000 * 60 * 60 * 24);
        if (days > 0)
            return plural(days, "day");
        long hours = ms / (1000 * 60 * 60);
        if (hours > 0)
            return plural(hours, "hour");
        long minutes = ms / (1000 * 60);
        if (minutes > 0)
            return plural(minutes, "minute");
        return "less than a minute";
    }

    private String plural(long value, String noun) {
        return value + " " + (value == 1 ? noun : noun + "s");
    }

    private void updateTimestampsNow(final Collection<BugInstance> bugs) throws IOException {
        final UpdateIssueTimestamps.Builder builder = UpdateIssueTimestamps.newBuilder().setSessionId(sessionId);
        for (Map.Entry<Long, Set<BugInstance>> entry : groupBugsByTimestamp(bugs).entrySet()) {
            UpdateIssueTimestamps.IssueGroup.Builder groupBuilder = IssueGroup.newBuilder().setTimestamp(entry.getKey());
            for (BugInstance bugInstance : entry.getValue()) {
                groupBuilder.addIssueHashes(WebCloudProtoUtil.encodeHash(bugInstance.getInstanceHash()));
            }
            builder.addIssueGroups(groupBuilder.build());
        }
        LOGGER.finer("Updating timestamps for " + bugs.size() + " bugs in " + builder.getIssueGroupsCount() + " groups");
        RetryableConnection<Void> conn = new RetryableConnection<Void>("/update-issue-timestamps", true) {
            @Override
            public void write(OutputStream out) throws IOException {
                builder.build().writeTo(out);
            }

            @Override
            public Void finish(int responseCode, String responseMessage, InputStream response) throws IOException {
                if (responseCode != 200)
                    throw new IllegalStateException("server returned error code " + responseCode + " "
                            + responseMessage);
                return null;
            }
        };
        conn.go();
    }

    private Map<Long, Set<BugInstance>> groupBugsByTimestamp(Collection<BugInstance> bugs) {
        Map<Long, Set<BugInstance>> map = new HashMap<Long, Set<BugInstance>>();
        for (BugInstance bug : bugs) {
            long firstSeen = cloudClient.getFirstSeen(bug);
            Set<BugInstance> bugsForTimestamp = map.get(firstSeen);
            if (bugsForTimestamp == null) {
                bugsForTimestamp = new HashSet<BugInstance>();
                map.put(firstSeen, bugsForTimestamp);
            }
            bugsForTimestamp.add(bug);
        }
        return map;
    }

    private IGuiCallback getGuiCallback() {
        return cloudClient.getGuiCallback();
    }

    private FindIssuesResponse submitHashes(final List<String> bugsByHash) throws IOException {
        LOGGER.finer("Checking " + bugsByHash.size() + " bugs against App Engine Cloud");
        FindIssues.Builder msgb = FindIssues.newBuilder();
        if (sessionId != null) {
            msgb.setSessionId(sessionId);
        }

        msgb.setVersionInfo(ProtoClasses.VersionInfo.newBuilder()
                .setAppName(Version.getApplicationName())
                .setAppVersion(Version.getApplicationVersion())
                .setFindbugsVersion(Version.getReleaseWithDateIfDev()));

        final FindIssues hashList = msgb.addAllMyIssueHashes(WebCloudProtoUtil.encodeHashes(bugsByHash)).build();

        RetryableConnection<FindIssuesResponse> conn = new RetryableConnection<FindIssuesResponse>("/find-issues", true) {
            @Override
            public void write(OutputStream out) throws IOException {
                long start = System.currentTimeMillis();
                hashList.writeTo(out);
                long elapsed = System.currentTimeMillis() - start;
                LOGGER.finer("Submitted hashes (" + hashList.getSerializedSize() / 1024 + " KB) in " + elapsed + "ms ("
                        + (elapsed / bugsByHash.size()) + "ms per hash)");
            }

            @Override
            public FindIssuesResponse finish(int responseCode, String responseMessage, InputStream response)
                    throws IOException {
                long start = System.currentTimeMillis();
                if (responseCode != 200) {
                    LOGGER.info("Error " + responseCode + " : " + responseMessage);
                    throw new IOException("Response code " + responseCode + " : " + responseMessage);
                }

                FindIssuesResponse firesponse = FindIssuesResponse.parseFrom(response);
                int foundIssues = firesponse.getFoundIssuesCount();
                long elapsed = System.currentTimeMillis() - start;
                LOGGER.fine("Received " + foundIssues + " bugs from server in " + elapsed + "ms ("
                        + (elapsed / (foundIssues + 1)) + "ms per bug)");
                return firesponse;
            }
        };
        return conn.go();
    }

    private void uploadNewBugsPartition(final Collection<BugInstance> bugsToSend) throws IOException {

        LOGGER.finer("Uploading " + bugsToSend.size() + " bugs to App Engine Cloud");
        UploadIssues uploadIssues = buildUploadIssuesCommandInUIThread(bugsToSend);
        if (uploadIssues == null)
            return;
        openPostUrl("/upload-issues", uploadIssues);

        // if it worked, store the issues locally
        final List<String> hashes = new ArrayList<String>();
        for (final Issue issue : uploadIssues.getNewIssuesList()) {
            final String hash = WebCloudProtoUtil.decodeHash(issue.getHash());
            storeIssueDetails(hash, issue);
            hashes.add(hash);
        }

        // let the GUI know that things changed
        cloudClient.getBugUpdateExecutor().execute(new Runnable() {
            public void run() {
                for (String hash : hashes) {
                    BugInstance bugInstance = cloudClient.getBugByHash(hash);
                    if (bugInstance != null) {
                        cloudClient.updatedIssue(bugInstance);
                    }
                }
            }
        });
    }

    private UploadIssues buildUploadIssuesCommandInUIThread(final Collection<BugInstance> bugsToSend) {
        ExecutorService updateExecutor = cloudClient.getBugUpdateExecutor();

        Future<UploadIssues> future = updateExecutor.submit(new Callable<UploadIssues>() {
            public UploadIssues call() throws Exception {
                Builder uploadIssuesCmd = UploadIssues.newBuilder();
                if (cloudClient.getCloudTokenProperty() != null) {
                    uploadIssuesCmd.setToken(cloudClient.getCloudTokenProperty());
                    LOGGER.info("Using Cloud Token: " + cloudClient.getCloudTokenProperty());
                }
                if (sessionId != null)
                    uploadIssuesCmd.setSessionId(sessionId);

                for (BugInstance bug : bugsToSend) {
                    uploadIssuesCmd.addNewIssues(Issue.newBuilder()
                            .setHash(WebCloudProtoUtil.encodeHash(bug.getInstanceHash()))
                            .setBugPattern(bug.getType()).setPriority(bug.getPriority())
                            .setPrimaryClass(bug.getPrimaryClass().getClassName())
                            .setFirstSeen(cloudClient.getFirstSeen(bug))
                            .build());
                }
                return uploadIssuesCmd.build();
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "", e);
            return null;
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "", e);
            return null;
        }
    }

    /**
     * package-private for testing
     */
    HttpURLConnection openConnection(String url) throws IOException {
        URL u = new URL(host + url);
        return (HttpURLConnection) u.openConnection();
    }

    private void openPostUrl(String url, final GeneratedMessage uploadMsg) throws IOException {
        RetryableConnection<Void> rc = new RetryableConnection<Void>(url, true) {
            @Override
            public void write(OutputStream out) throws IOException {
                if (uploadMsg != null) {
                    uploadMsg.writeTo(out);
                } else {
                    out.write(0); // why write a null byte??
                }
            }

            @Override
            public Void finish(int responseCode, String responseMessage, InputStream response) throws IOException {
                if (responseCode != 200)
                    throw new IllegalStateException("server returned error code when opening " + url + ": "
                            + responseCode + " " + responseMessage);
                return null;
            }
        };
        rc.go();
    }

    protected abstract class RetryableConnection<RV> {
        protected final String url;
        private final boolean post;

        public RetryableConnection(String url, boolean post) {
            this.post = post;
            this.url = url;
        }

        /** NOTE: this may be called more than once if the connection fails the first time! */
        public abstract void write(OutputStream out) throws IOException;

        public abstract RV finish(int responseCode, String responseMessage, InputStream response) throws IOException;

        public RV go() throws IOException {
            RV result = null;
            HttpURLConnection conn = null;
            boolean finished = false;
            IOException firstException = null;
            IOException lastException = null;
            for (int i = 0; i < 3 && !finished; i++) {
                if (i > 0 && lastException != null)
                    LOGGER.warning("Retrying connection to " + url + " due to "
                            + lastException.getClass().getSimpleName() + ": " + lastException.getMessage());
                try {
                    conn = openConnection(url);
                    // increase the timeout by 5 seconds each iteration
                    int timeout = GLOBAL_HTTP_SOCKET_TIMEOUT;
                    if (lastException instanceof SocketTimeoutException)
                        // increase timeout by 5 seconds each iteration
                        timeout *= i;
                    conn.setConnectTimeout(timeout);
                    if (post) {
                        conn.setDoOutput(true);
                        conn.setRequestMethod("POST");
                    }
                    conn.connect();
                    OutputStream out = conn.getOutputStream();
                    write(out);
                    out.close();
                    result = finish(conn.getResponseCode(), conn.getResponseMessage(), conn.getInputStream());
                    finished = true;
                } catch (UnknownHostException ex2) {
                    UnknownHostException ex = new UnknownHostException(ex2.getMessage());
                    if (firstException == null)
                        firstException = ex;
                    lastException = ex;
                    finished = true;
                } catch (IOException ex) {
                    if (firstException == null)
                        firstException = ex;
                    lastException = ex;
                } finally {
                    if (conn != null) {
                        int responseCode;
                        try {
                            responseCode = conn.getResponseCode();
                            if (responseCode != 500 && responseCode != -1)
                                // only retry on 500 or no-response
                                finished = true;
                        } catch (IOException e) {
                            // skip this check
                        }
                        try {
                            conn.disconnect();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
            }
            if (result != null)
                return result;
            if (firstException != null)
                throw firstException;
            return null;
        }
    }
}
