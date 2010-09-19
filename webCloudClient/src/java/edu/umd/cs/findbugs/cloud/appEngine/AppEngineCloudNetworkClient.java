package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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

import com.google.protobuf.GeneratedMessage;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.MutableCloudTask;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
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
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;

public class AppEngineCloudNetworkClient {
    private static final Logger LOGGER = Logger.getLogger(AppEngineCloudNetworkClient.class.getPackage().getName());

    /** For debugging */
    private static final boolean FORCE_UPLOAD_ALL_ISSUES = false;
    private static final int BUG_UPLOAD_PARTITION_SIZE = 10;
    /** For updating firstSeen timestamps */
    private static final int BUG_UPDATE_PARTITION_SIZE = 10;
    /** Big enough to keep total find-issues time down AND also keep individual request time down (for Google's sake) */
    private static final int HASH_CHECK_PARTITION_SIZE = 60;

    private AppEngineCloudClient cloudClient;
    private AppEngineNameLookup lookerupper;
    private ConcurrentMap<String, Issue> issuesByHash = new ConcurrentHashMap<String, Issue>();
    private String host;
    private Long sessionId;
    private String username;
    private volatile long mostRecentEvaluationMillis = 0;
    private CopyOnWriteArrayList<String> timestampsToUpdate = new CopyOnWriteArrayList<String>();

    public void setCloudClient(AppEngineCloudClient appEngineCloudClient) {
        this.cloudClient = appEngineCloudClient;
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
            getGuiCallback().setErrorMessage("Signing into FindBugs Cloud failed!");
            return;
        }
        this.sessionId = lookerupper.getSessionId();
        this.username = lookerupper.getUsername();
        this.host = lookerupper.getHost();
        if (getUsername() == null || host == null) {
            throw new IllegalStateException("No App Engine Cloud username or hostname found! Check etc/findbugs.xml");
        }
        // now that we know our own username, we need to update all the bugs in the UI to show what "our"
        // designation & comments are.
        // this might be really slow with a lot of issues. seems fine so far.
        for (BugInstance instance : cloudClient.getBugCollection().getCollection()) {
            Issue issue = issuesByHash.get(instance.getInstanceHash());
            if (issue != null && issue.getEvaluationsCount() > 0) {
                cloudClient.updateBugInstanceAndNotify(instance);
            }
        }
    }

    public void setBugLinkOnCloud(BugInstance b, String type, String bugLink) throws IOException, SignInCancelledException {
        cloudClient.signInIfNecessary("To store the bug URL on the FindBugs cloud, you must sign in.");

        HttpURLConnection conn = openConnection("/set-bug-link");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            SetBugLink.newBuilder()
                    .setSessionId(sessionId)
                    .setHash(encodeHash(b.getInstanceHash()))
                    .setBugLinkType(type)
                    .setUrl(bugLink)
                    .build()
                    .writeTo(outputStream);
            outputStream.close();
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException(
                        "server returned error code "
                        + conn.getResponseCode() + " "
                        + conn.getResponseMessage());
            }
        } finally {
            conn.disconnect();
        }
    }

    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType)
            throws IOException, SignInCancelledException {
        setBugLinkOnCloud(b, linkType, viewUrl);

        String hash = b.getInstanceHash();
        storeIssueDetails(hash,
                          Issue.newBuilder(getIssueByHash(hash))
                                  .setBugLink(viewUrl)
                                  .setBugLinkTypeStr(linkType)
                                  .build());
    }

    public void logIntoCloudForce() throws IOException {
        HttpURLConnection conn = openConnection("/log-in");
        conn.setDoOutput(true);
        conn.connect();

        OutputStream stream = conn.getOutputStream();
        LogIn logIn = LogIn.newBuilder()
                .setSessionId(sessionId)
                .setAnalysisTimestamp(cloudClient.getBugCollection().getAnalysisTimestamp())
                .build();
        logIn.writeTo(stream);
        stream.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IllegalStateException("Could not log into cloud with ID " + sessionId + " - error "
                                            + responseCode + " " + conn.getResponseMessage());
        }
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

    public MutableCloudTask generateUpdateTimestampRunnables(List<Callable<Object>> callables) throws SignInCancelledException {
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

        // if some bugs have a zero timestamp, let's not bother telling the user anything specific
        String durationStr;
        if (someZeroOnCloud) durationStr = "";
        else durationStr = " up to " + toDuration(biggestDiffMs);

        Calendar now = Calendar.getInstance();
        TimeZone timeZone = now.getTimeZone();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
        String timeStr = df.format(now.getTime());
        boolean daylight = timeZone.inDaylightTime(now.getTime());
        String zoneStr = timeZone.getDisplayName(daylight, TimeZone.LONG)
                         + " (" + timeZone.getDisplayName(daylight, TimeZone.SHORT) + ")";

        Calendar earliest = Calendar.getInstance(timeZone);
        earliest.setTimeInMillis(earliestFirstSeen);
        String earliestStr = df.format(earliest.getTime());

        int result = getGuiCallback().showConfirmDialog(
                "Your first-seen dates for " + bugCount + " bugs are " + durationStr
                + " earlier than those on the FindBugs Cloud.\n"
                + "The earliest first-seen from the local analysis is "  + earliestStr
                + "\n"
                + "Would you like to back date the first-seen dates on the Cloud?\n" +
                "\n" +
                "Current time: " + timeStr + " " +  zoneStr + "\n" +
                "(If you're not sure the time and time zone are correct, click Cancel.)",
                "FindBugs Cloud", "Update", "Cancel");
        if (result != 0)
            return null;

        cloudClient.signInIfNecessary(null);

        // since the protocol groups bugs by timestamp, let's optimize network bandwidth by sorting first. probably
        // doesn't matter.
        Collections.sort(bugs, new Comparator<BugInstance>() {
            public int compare(BugInstance o1, BugInstance o2) {
                long v1 = o1.getFirstVersion();
                long v2 = o2.getFirstVersion();
                if (v1 < v2) return -1;
                if (v1 > v2) return 1;
                return 0;
            }
        });

        final MutableCloudTask task = cloudClient.createTask("Updating FindBugs Cloud");
        final AtomicInteger soFar = new AtomicInteger(0);
        for (int i = 0; i < bugCount; i += BUG_UPDATE_PARTITION_SIZE) {
            final List<BugInstance> partition = bugs.subList(i, Math.min(bugCount, i + BUG_UPLOAD_PARTITION_SIZE));

            callables.add(new Callable<Object>() {
                public Object call() throws Exception {
                    updateTimestampsNow(partition);
                    int updated = soFar.addAndGet(partition.size());
                    task.update("Updated " + updated + " of " + bugCount + " timestamps", updated * 100.0 / bugCount);
                    return null;
                }
            });
        }
        return task;
    }

    public MutableCloudTask generateUploadRunnables(final List<BugInstance> newBugs, List<Callable<Object>> callables)
            throws SignInCancelledException {
        final int bugCount = newBugs.size();
        if (bugCount == 0)
            return null;
        cloudClient.signInIfNecessary("Some bugs were not found on the FindBugs Cloud service.\n" +
                                      "Would you like to sign in and upload them to the Cloud?");
        final MutableCloudTask task = cloudClient.createTask("Uploading to the FindBugs Cloud");
        final AtomicInteger bugsUploaded = new AtomicInteger(0);
        for (int i = 0; i < bugCount; i += BUG_UPLOAD_PARTITION_SIZE) {
            final List<BugInstance> partition = newBugs.subList(i, Math.min(bugCount, i + BUG_UPLOAD_PARTITION_SIZE));
            callables.add(new Callable<Object>() {
                public Object call() throws Exception {
                    uploadNewBugsPartition(partition);
                    bugsUploaded.addAndGet(partition.size());
                    int uploaded = bugsUploaded.get();
                    task.update("Uploaded " + uploaded + " of " + bugCount + " issues",
                                uploaded * 100.0 / bugCount);
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
        if (AppEngineCloudClient.DEBUG_FIRST_SEEN)
          System.out.println("First seen is " + issue.getFirstSeen() + " for " + b.getMessage());
        if (issue.getFirstSeen() == 0)
            return Long.MAX_VALUE;
        return issue.getFirstSeen();
    }

    public void storeIssueDetails(String hash, Issue issue) {
        for (Evaluation eval : issue.getEvaluationsList()) {
            if (eval.getWhen() > mostRecentEvaluationMillis) {
                mostRecentEvaluationMillis = eval.getWhen();
            }
        }
        issuesByHash.put(hash, issue);
    }

    public RecentEvaluations getRecentEvaluationsFromServer() throws IOException {
        HttpURLConnection conn = openConnection("/get-recent-evaluations");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            GetRecentEvaluations.Builder msgb = GetRecentEvaluations.newBuilder();
            if (sessionId != null) {
                msgb.setSessionId(sessionId);
            }
            msgb.setTimestamp(mostRecentEvaluationMillis);

            msgb.build().writeTo(outputStream);
            outputStream.close();

            if (conn.getResponseCode() != 200) {
                throw new ServerReturnedErrorCodeException(conn.getResponseCode(), conn.getResponseMessage());
            }
            return RecentEvaluations.parseFrom(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    public Evaluation getMostRecentEvaluationBySelf(BugInstance b) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
            return null;
        Evaluation mostRecent = null;
        long when = Long.MIN_VALUE;
        String myUsername = getUsername();
        for (Evaluation e : issue.getEvaluationsList()) {
            if (e.getWho().equals(myUsername) && e.getWhen() > when) {
                mostRecent = e;
                when = e.getWhen();
            }
        }

        return mostRecent;
    }

    public String getUsername() {
        return username;
    }

    @SuppressWarnings({"deprecation"})
    public void storeUserAnnotation(BugInstance bugInstance) throws SignInCancelledException {
        // store this stuff first because signIn might clobber it. this is kludgy but works.
        BugDesignation designation = bugInstance.getNonnullUserDesignation();
        long timestamp = designation.getTimestamp();
        String designationKey = designation.getDesignationKey();
        String comment = designation.getAnnotationText();

        cloudClient.signInIfNecessary("To store your evaluation on the FindBugs Cloud, you must sign in first.");

        Evaluation.Builder evalBuilder = Evaluation.newBuilder()
                .setWhen(timestamp)
                .setDesignation(designationKey);
        if (comment != null) {
            evalBuilder.setComment(comment);
        }
        String hash = bugInstance.getInstanceHash();
        Evaluation eval = evalBuilder.build();
        UploadEvaluation uploadMsg = UploadEvaluation.newBuilder()
                .setSessionId(sessionId)
                .setHash(encodeHash(hash))
                .setEvaluation(eval)
                .build();

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

    public @CheckForNull Issue getIssueByHash(String hash) {
        return issuesByHash.get(hash);
    }

    public void signOut(boolean background) {
        if (sessionId != null) {
            final long oldSessionId = sessionId;
            Runnable logoutRequest = new Runnable() {
                public void run() {
                    try {
                        openPostUrl("/log-out/" + oldSessionId, null);
                    } catch (Exception e) {
						getGuiCallback().showMessageDialog(
                                "A network error occurred while attempting to sign out of the FindBugs Cloud. \n" +
                                        "Please check your internet settings and try again.\n\n" + e.getMessage());
                        LOGGER.log(Level.SEVERE, "Could not sign out", e);
					}
                }
            };
            if (background)
                cloudClient.getBackgroundExecutor().execute(logoutRequest);
            else
                logoutRequest.run();
            sessionId = null;
            AppEngineNameLookup.clearSavedSessionInformation();
        }
    }

    public Long getSessionId() {
        return sessionId;
    }

    // ========================= private methods ==========================

    protected AppEngineNameLookup createNameLookup() {
        AppEngineNameLookup nameLookup = new AppEngineNameLookup();
        nameLookup.loadProperties(cloudClient.getPlugin());
        return nameLookup;
    }

    private void checkHashesPartition(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        FindIssuesResponse response = submitHashes(hashes);

        for (int j = 0; j < hashes.size(); j++) {
            String hash = hashes.get(j);
            Issue issue = response.getFoundIssues(j);

            if (isEmpty(issue))
                // the issue was not found!
                continue;

            storeIssueDetails(hash, issue);

            BugInstance bugInstance;
            if (FORCE_UPLOAD_ALL_ISSUES) bugInstance = bugsByHash.get(hash);
            else bugInstance = bugsByHash.remove(hash);

            if (bugInstance == null) {
                LOGGER.warning("Server sent back issue that we don't know about: " + hash + " - " + issue);
                continue;
            }

            long firstSeen = cloudClient.getLocalFirstSeen(bugInstance);
            if (firstSeen > 0 && firstSeen < issue.getFirstSeen())
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
        return value + " " + (value == 1 ? noun : noun+"s");
    }

    private void updateTimestampsNow(Collection<BugInstance> bugs) throws IOException {
        HttpURLConnection conn = openConnection("/update-issue-timestamps");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            UpdateIssueTimestamps.Builder builder = UpdateIssueTimestamps.newBuilder()
                    .setSessionId(sessionId);
            for (Map.Entry<Long, Set<BugInstance>> entry : groupBugsByTimestamp(bugs).entrySet()) {
                UpdateIssueTimestamps.IssueGroup.Builder groupBuilder = IssueGroup.newBuilder()
                        .setTimestamp(entry.getKey());
                for (BugInstance bugInstance : entry.getValue()) {
                    groupBuilder.addIssueHashes(encodeHash(bugInstance.getInstanceHash()));
                }
                builder.addIssueGroups(groupBuilder.build());
            }
            LOGGER.finer("Updating timestamps for " + bugs.size() + " bugs in " + builder.getIssueGroupsCount() + " groups");
            builder.build().writeTo(outputStream);
            outputStream.close();
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException(
                        "server returned error code "
                        + conn.getResponseCode() + " "
                        + conn.getResponseMessage());
            }
        } finally {
            conn.disconnect();
        }
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

    private FindIssuesResponse submitHashes(List<String> bugsByHash)
            throws IOException {
        LOGGER.finer("Checking " + bugsByHash.size() + " bugs against App Engine Cloud");
        FindIssues.Builder msgb = FindIssues.newBuilder();
        if (sessionId != null) {
            msgb.setSessionId(sessionId);
        }
        FindIssues hashList = msgb.addAllMyIssueHashes(AppEngineProtoUtil.encodeHashes(bugsByHash))
                .build();

        long start = System.currentTimeMillis();
        HttpURLConnection conn = openConnection("/find-issues");
        conn.setDoOutput(true);
        conn.connect();
        LOGGER.finer("Connected in " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        OutputStream stream = conn.getOutputStream();
        hashList.writeTo(stream);
        stream.close();
        long elapsed = System.currentTimeMillis() - start;
        LOGGER.finer("Submitted hashes (" + hashList.getSerializedSize() / 1024 + " KB) in " + elapsed + "ms ("
                                         + (elapsed / bugsByHash.size()) + "ms per hash)");

        start = System.currentTimeMillis();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            LOGGER.info("Error " + responseCode + ", took " + (System.currentTimeMillis() - start) + "ms");
            throw new IOException("Response code " + responseCode + " : " + conn.getResponseMessage());
        }

        InputStream instream = conn.getInputStream();
        FindIssuesResponse response = FindIssuesResponse.parseFrom(instream);
        conn.disconnect();
        int foundIssues = response.getFoundIssuesCount();
        elapsed = System.currentTimeMillis() - start;
        LOGGER.fine("Received " + foundIssues + " bugs from server in " + elapsed + "ms ("
                                         + (elapsed / (foundIssues + 1)) + "ms per bug)");
        return response;
    }

    private void uploadNewBugsPartition(final Collection<BugInstance> bugsToSend)
            throws IOException {

        LOGGER.finer("Uploading " + bugsToSend.size() + " bugs to App Engine Cloud");
        UploadIssues uploadIssues = buildUploadIssuesCommandInUIThread(bugsToSend);
        if (uploadIssues == null)
            return;
        openPostUrl("/upload-issues", uploadIssues);

        // if it worked, store the issues locally
        final List<String> hashes = new ArrayList<String>();
        for (final Issue issue : uploadIssues.getNewIssuesList()) {
            final String hash = AppEngineProtoUtil.decodeHash(issue.getHash());
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
                Builder issueList = UploadIssues.newBuilder();
                issueList.setSessionId(sessionId);
                for (BugInstance bug : bugsToSend) {
                    issueList.addNewIssues(Issue.newBuilder()
                            .setHash(encodeHash(bug.getInstanceHash()))
                            .setBugPattern(bug.getType())
                            .setPriority(bug.getPriority())
                            .setPrimaryClass(bug.getPrimaryClass().getClassName())
                            .setFirstSeen(cloudClient.getFirstSeen(bug))
                            .build());
                }
                return issueList.build();
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

    private void openPostUrl(String url, GeneratedMessage uploadMsg) {
        try {
            HttpURLConnection conn = openConnection(url);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.connect();
            try {
                OutputStream stream = conn.getOutputStream();
                if (uploadMsg != null) {
                    uploadMsg.writeTo(stream);
                } else {
                    stream.write(0);
                }
                stream.close();
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    throw new IllegalStateException(
                            "server returned error code when opening " + url + ": "
                            + conn.getResponseCode() + " "
                            + conn.getResponseMessage());
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
