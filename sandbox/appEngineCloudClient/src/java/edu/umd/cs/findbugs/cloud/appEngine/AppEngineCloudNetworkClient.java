package edu.umd.cs.findbugs.cloud.appEngine;

import com.google.protobuf.GeneratedMessage;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.FindIssuesResponse;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.GetRecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.LogIn;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.SetBugLink;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadEvaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.UploadIssues.Builder;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.encodeHash;

public class AppEngineCloudNetworkClient {
    private static final Logger LOGGER = Logger.getLogger(AppEngineCloudNetworkClient.class.getName());
    /** For debugging */
    private static final boolean FORCE_UPLOAD_ALL_ISSUES = false;
    private static final int BUG_UPLOAD_PARTITION_SIZE = 10;
    private static final int HASH_CHECK_PARTITION_SIZE = 20;

    private AppEngineCloudClient cloudClient;
    private ConcurrentMap<String, Issue> issuesByHash = new ConcurrentHashMap<String, Issue>();
    private String host;
    private long sessionId;
    private String username;
    private volatile long mostRecentEvaluationMillis = 0;

    public AppEngineCloudNetworkClient() {
    }

    public void setCloudClient(AppEngineCloudClient appEngineCloudClient) {
        this.cloudClient = appEngineCloudClient;
    }

    public boolean initialize() {
        AppEngineNameLookup lookerupper = new AppEngineNameLookup();
        if (!lookerupper.initialize(cloudClient.getPlugin(), cloudClient.getBugCollection())) {
            return false;
        }
        this.sessionId = lookerupper.getSessionId();
        this.username = lookerupper.getUsername();
        this.host = lookerupper.getHost();
        if (getUsername() == null || host == null) {
            System.err.println("No App Engine Cloud username or hostname found! Check etc/findbugs.xml");
            return false;
        }
        return true;
    }

    public void setBugLinkOnCloud(BugInstance b, String bugLink) throws IOException {
        HttpURLConnection conn = openConnection("/set-bug-link");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            SetBugLink.newBuilder()
                    .setSessionId(sessionId)
                    .setHash(encodeHash(b.getInstanceHash()))
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

    public void logIntoCloud() throws IOException {
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
            throw new IllegalStateException("Could not log into cloud - error "
                                            + responseCode + conn.getResponseMessage());
        }
    }

    public void checkHashes(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        int numBugs = hashes.size();
        for (int i = 0; i < numBugs; i += HASH_CHECK_PARTITION_SIZE) {
            cloudClient.setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud..."
                                              + (i * 100 / numBugs) + "%");
            List<String> partition = hashes.subList(i, Math.min(i + HASH_CHECK_PARTITION_SIZE, numBugs));
            checkHashesPartition(partition, bugsByHash);
        }
    }

    private void checkHashesPartition(List<String> hashes, Map<String, BugInstance> bugsByHash) throws IOException {
        FindIssuesResponse response = submitHashes(hashes);

        for (int j = 0; j < hashes.size(); j++) {
            String hash = hashes.get(j);
            Issue issue = response.getFoundIssues(j);

            storeIssueDetails(hash, issue);

            if (isEmpty(issue))
                // the issue was not found!
                continue;

            BugInstance bugInstance;
            if (FORCE_UPLOAD_ALL_ISSUES) bugInstance = bugsByHash.get(hash);
            else bugInstance = bugsByHash.remove(hash);

            if (bugInstance == null) {
                LOGGER.warning("Server sent back issue that we don't know about: " + hash + " - " + issue);
                continue;
            }

            cloudClient.updateBugInstanceAndNotify(bugInstance);
        }
    }

    private boolean isEmpty(Issue issue) {
        return !issue.hasFirstSeen() && !issue.hasLastSeen() && issue.getEvaluationsCount() == 0;
    }

    public void uploadNewBugs(List<BugInstance> newBugs) throws IOException {
        try {
            for (int i = 0; i < newBugs.size(); i += BUG_UPLOAD_PARTITION_SIZE) {
                cloudClient.setStatusMsg("Uploading " + newBugs.size()
                                                  + " new bugs to the FindBugs Cloud..." + i * 100
                                                                                           / newBugs.size() + "%");
                uploadIssues(newBugs.subList(i, Math.min(newBugs.size(), i + BUG_UPLOAD_PARTITION_SIZE)));
            }
        } finally {
            cloudClient.setStatusMsg("");
        }
    }

    public long getFirstSeenFromCloud(BugInstance b) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
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

    private FindIssuesResponse submitHashes(List<String> bugsByHash)
            throws IOException {
        LOGGER.info("Checking " + bugsByHash.size() + " bugs against App Engine Cloud");
        FindIssues hashList = FindIssues.newBuilder()
                .setSessionId(sessionId)
                .addAllMyIssueHashes(AppEngineProtoUtil.encodeHashes(bugsByHash))
                .build();

        long start = System.currentTimeMillis();
        HttpURLConnection conn = openConnection("/find-issues");
        conn.setDoOutput(true);
        conn.connect();
        LOGGER.info("Connected in " + (System.currentTimeMillis() - start) + "ms");

        start = System.currentTimeMillis();
        OutputStream stream = conn.getOutputStream();
        hashList.writeTo(stream);
        stream.close();
        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("Submitted hashes (" + hashList.getSerializedSize() / 1024 + " KB) in " + elapsed + "ms ("
                                         + (elapsed / bugsByHash.size()) + "ms per hash)");

        start = System.currentTimeMillis();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            LOGGER.info("Error " + responseCode + ", took " + (System.currentTimeMillis() - start) + "ms");
            throw new IOException("Response code " + responseCode + " : " + conn.getResponseMessage());
        }
        FindIssuesResponse response = FindIssuesResponse.parseFrom(conn.getInputStream());
        conn.disconnect();
        int foundIssues = response.getFoundIssuesCount();
        elapsed = System.currentTimeMillis() - start;
        LOGGER.info("Received " + foundIssues + " bugs from server in " + elapsed + "ms ("
                                         + (elapsed / (foundIssues + 1)) + "ms per bug)");
        return response;
    }

    private void uploadIssues(final Collection<BugInstance> bugsToSend)
            throws IOException {
        LOGGER.info("Uploading " + bugsToSend.size() + " bugs to App Engine Cloud");
        UploadIssues uploadIssues = buildUploadIssuesCommandInUIThread(bugsToSend);
        if (uploadIssues == null)
            return;
        openPostUrl(uploadIssues, "/upload-issues");

        // if it worked, store the issues locally
        for (Issue issue : uploadIssues.getNewIssuesList()) {
            storeIssueDetails(AppEngineProtoUtil.decodeHash(issue.getHash()), issue);
        }
    }

    private UploadIssues buildUploadIssuesCommandInUIThread(final Collection<BugInstance> bugsToSend) {
        ExecutorService updateExecutor = cloudClient.getBugCollection().getProject()
                .getGuiCallback().getBugUpdateExecutor(); 

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

    public RecentEvaluations getRecentEvaluationsFromServer() throws IOException {
        HttpURLConnection conn = openConnection("/get-recent-evaluations");
        conn.setDoOutput(true);
        try {
            OutputStream outputStream = conn.getOutputStream();
            GetRecentEvaluations.newBuilder()
                    .setSessionId(sessionId)
                    .setTimestamp(mostRecentEvaluationMillis)
                    .build()
                    .writeTo(outputStream);
            outputStream.close();
            if (conn.getResponseCode() != 200) {
                throw new IllegalStateException(
                        "server returned error code "
                        + conn.getResponseCode() + " "
                        + conn.getResponseMessage());
            }
            return RecentEvaluations.parseFrom(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    public Evaluation getMostRecentEvaluation(BugInstance b) {
        Issue issue = issuesByHash.get(b.getInstanceHash());
        if (issue == null)
            return null;
        Evaluation mostRecent = null;
        long when = Long.MIN_VALUE;
        for (Evaluation e : issue.getEvaluationsList())
            if (e.getWho().equals(cloudClient.getUser()) && e.getWhen() > when) {
                mostRecent = e;
                when = e.getWhen();
            }

        return mostRecent;
    }

    private void openPostUrl(GeneratedMessage uploadMsg, String url) {
        try {
            HttpURLConnection conn = openConnection(url);
            conn.setDoOutput(true);
            conn.connect();
            try {
                if (uploadMsg != null) {
                    OutputStream stream = conn.getOutputStream();
                    uploadMsg.writeTo(stream);
                    stream.close();
                    conn.getResponseCode(); // wait for response
                }
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

    public String getUsername() {
        return username;
    }

    @SuppressWarnings({"deprecation"})
    public void storeUserAnnotation(BugInstance bugInstance) {
        BugDesignation designation = bugInstance.getNonnullUserDesignation();
        Evaluation.Builder evalBuilder = Evaluation.newBuilder()
                .setWhen(designation.getTimestamp())
                .setDesignation(designation.getDesignationKey());
        String comment = designation.getAnnotationText();
        if (comment != null) {
            evalBuilder.setComment(comment);
        }
        UploadEvaluation uploadMsg = UploadEvaluation.newBuilder()
                .setSessionId(sessionId)
                .setHash(encodeHash(bugInstance.getInstanceHash()))
                .setEvaluation(evalBuilder.build())
                .build();

        openPostUrl(uploadMsg, "/upload-evaluation");
    }

    public @CheckForNull Issue getIssueByHash(String hash) {
        return issuesByHash.get(hash);
    }

    /** for testing */
    void setUsername(String username) {
        this.username = username;
    }

    /** for testing */
    void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }
}