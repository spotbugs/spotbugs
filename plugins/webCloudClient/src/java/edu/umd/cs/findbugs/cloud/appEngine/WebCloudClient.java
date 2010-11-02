package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.MutableCloudTask;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;
import edu.umd.cs.findbugs.util.Util;

@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
public class WebCloudClient extends AbstractCloud {

    private static final Logger LOGGER = Logger.getLogger(WebCloudClient.class.getPackage().getName());

    private static final int EVALUATION_CHECK_SECS = 5 * 60;
    private static final int MAX_RECENT_EVALUATION_PAGES = 30;

    protected ExecutorService backgroundExecutorService;

    private Timer timer;

    private WebCloudNetworkClient networkClient;

    private Map<String, String> bugStatusCache = new ConcurrentHashMap<String, String>();

    private final @CheckForNull BugFilingHelper bugFilingHelper;

    private CountDownLatch issueDataDownloaded = new CountDownLatch(1);

    protected CountDownLatch newIssuesUploaded = new CountDownLatch(1);

    private CountDownLatch bugsPopulated = new CountDownLatch(1);

    private final EvaluationsFromXmlUploader evaluationsFromXmlUploader = new EvaluationsFromXmlUploader(this);

    static ComponentPlugin<BugFiler> foo(String name) {
        if (name == null)
            return null;
        Set<String> names = Collections.singleton(name);
        for (Plugin p : DetectorFactoryCollection.instance().plugins()) {
            for(ComponentPlugin<BugFiler> bf : p.getComponentPlugins(BugFiler.class)) {
                if (bf.isNamed(names))
                    return bf;
            }
        }
        throw new IllegalArgumentException("Unable to construct bug filer " + name);
    }
    /** invoked via reflection */
    @SuppressWarnings({ "UnusedDeclaration" })
    public WebCloudClient(CloudPlugin plugin, BugCollection bugs, Properties properties) {
        super(plugin, bugs, properties);
        setNetworkClient(new WebCloudNetworkClient());
        backgroundExecutorService = Executors.newFixedThreadPool(4, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, WebCloudClient.class.getSimpleName() + " bg");
                t.setDaemon(true);
                return t;
            }
        });
        if (backgroundExecutorService.isShutdown())
            LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown at creation");

        String bugFiler = properties.getProperty("bugFiler");
        if (bugFiler == null) {
            this.bugFilingHelper = null;
        } else {
            this.bugFilingHelper = new BugFilingHelper(this, foo(bugFiler));
        }
    }

    /** package-private for testing */
    void setNetworkClient(WebCloudNetworkClient networkClient) {
        this.networkClient = networkClient;
        networkClient.setCloudClient(this);
    }

    // ====================== initialization =====================

    public String getCloudName() {
        return getPlugin().getDescription();
    }

    public void waitUntilNewIssuesUploaded() {
        try {
            newIssuesUploaded.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
    }

    public void waitUntilIssueDataDownloaded() {
        try {
            bugsPopulated.await();

            initiateCommunication();

            LOGGER.fine("Waiting for issue data to be downloaded");

            issueDataDownloaded.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "interrupted", e);
        }

    }

    public boolean availableForInitialization() {
        return true;
    }

    boolean initialized = false;

    @Override
    public boolean initialize() throws IOException {
        // noinspection ConstantConditions
        if (false && initialized) {
            LOGGER.warning("Already initialized " + getClass().getSimpleName());
            return true;
        }

        LOGGER.fine("Initializing " + getClass().getSimpleName());
        setSigninState(SigninState.UNAUTHENTICATED);

        try {
            if (!super.initialize()) {
                setSigninState(SigninState.SIGNIN_FAILED);

                return false;
            }
            if (networkClient.initialize()) {

                setSigninState(SigninState.SIGNED_IN);

            } else {
                // soft init didn't work
                setSigninState(SigninState.UNAUTHENTICATED);
            }
        } catch (IOException e) {
            setSigninState(SigninState.SIGNIN_FAILED);
            return false;
        }

        if (!getGuiCallback().isHeadless()) {
            startEvaluationCheckThread();
        }
        initialized = true;
        return true;
    }

    /**
     * @param reason
     *            if null, no question dialog will be shown, signin will be
     *            automatic
     */
    public void signInIfNecessary(@CheckForNull String reason) throws SignInCancelledException {
        if (couldSignIn()) {

            if (reason != null) {
                IGuiCallback callback = getGuiCallback();
                int result = callback.showConfirmDialog(reason, getCloudName(), "Sign in", "Cancel");
                if (result != 0)
                    throw new SignInCancelledException();
            }
            try {
                signIn();
            } catch (Exception e) {
                getGuiCallback().showMessageDialog("Could not sign into " + getCloudName() + ": " + e.getMessage());
                throw new SignInCancelledException(e);
            }

        } else if (getSigninState() == SigninState.SIGNING_IN) {
            synchronized (signInLock) {
            }
        } else if (getSigninState() == SigninState.SIGNED_IN) {
            // great!
        }
    }

    public boolean couldSignIn() {
        return getSigninState() == SigninState.UNAUTHENTICATED || getSigninState() == SigninState.SIGNIN_FAILED
                || getSigninState() == SigninState.SIGNED_OUT;
    }

    @Override
    public void shutdown() {

        super.shutdown();
        if (timer != null)
            timer.cancel();

        if (backgroundExecutorService != null) {
            backgroundExecutorService.shutdownNow();
        }
    }

    public void bugsPopulated() {
        evaluationsFromXmlUploader.tryUploadingLocalAnnotations(false);
        bugsPopulated.countDown();
    }

    private final Object initiationLock = new Object();

    private volatile boolean communicationInitiated = false;

    public void initiateCommunication() {
        bugsPopulated();

        if (communicationInitiated)
            return;
        synchronized (initiationLock) {
            if (communicationInitiated)
                return;
            communicationInitiated = true;
            backgroundExecutorService.execute(new Runnable() {
                public void run() {
                    try {
                        actuallyCheckBugsAgainstCloud();
                    } catch (Exception e) {
                        getGuiCallback().showMessageDialog("Error while checking bugs against " + getCloudName()
                                + "\n\n" + e.getMessage());
                        LOGGER.log(Level.SEVERE, "Error while checking bugs against cloud in background " , e);
                    }
                }
            });

        }
    }

    // =============== accessors ===================

    protected WebCloudNetworkClient getNetworkClient() {
        return networkClient;
    }

    public void setSaveSignInInformation(boolean save) {
        WebCloudNameLookup.setSaveSessionInformation(save);
        if (save) {
            Long sessionId = networkClient.getSessionId();
            if (sessionId != null) {
                WebCloudNameLookup.saveSessionInformation(sessionId);
            }
        }
    }

    public boolean isSavingSignInInformationEnabled() {
        return WebCloudNameLookup.isSavingSessionInfoEnabled();
    }

    private final Object signInLock = new Object();

    public void signIn() throws IOException {
        SigninState oldState = getSigninState();
        synchronized (signInLock) {
            if (getSigninState() == SigninState.SIGNED_IN)
                return;
            setSigninState(SigninState.SIGNING_IN);
            MutableCloudTask task = createTask("Signing into " + getCloudName());
            try {
                try {
                    networkClient.signIn(true);
                    networkClient.logIntoCloudForce();
                } catch (IOException e) {
                    setSigninState(SigninState.SIGNIN_FAILED);
                    throw e;

                } catch (RuntimeException e) {
                    setSigninState(SigninState.SIGNIN_FAILED);
                    throw e;
                }

                setSigninState(SigninState.SIGNED_IN);
            } finally {
                task.finished();
            }
        }
        SigninState newState = getSigninState();
        if (oldState == SigninState.SIGNED_OUT && newState == SigninState.SIGNED_IN) {
            evaluationsFromXmlUploader.tryUploadingLocalAnnotations(true);
        }
    }

    public void signOut() {
        signOut(false);
    }

    public String getUser() {
        return networkClient.getUsername();
    }

    public BugDesignation getPrimaryDesignation(BugInstance b) {
        Evaluation e = networkClient.getMostRecentEvaluationBySelf(b);
        return e == null ? null : createBugDesignation(e);
    }

    static final boolean DEBUG_FIRST_SEEN = Boolean.getBoolean("debug.first.seen");

    public long getLocalFirstSeen(BugInstance bug) {
        return super.getFirstSeen(bug);
    }

    @Override
    public long getFirstSeen(BugInstance b) {

        long firstSeenFromCloud = networkClient.getFirstSeenFromCloud(b);
        long firstSeenLocally = super.getFirstSeen(b);
        long firstSeen = dateMin(firstSeenFromCloud, firstSeenLocally);

        if (DEBUG_FIRST_SEEN) {
            System.out.println(b.getMessageWithoutPrefix());
            System.out.printf("%s %s %s\n", new Date(firstSeenFromCloud), new Date(firstSeenLocally), new Date(firstSeen));
            if (firstSeenFromCloud == Long.MAX_VALUE) {
                new RuntimeException("Not seen previously").printStackTrace(System.out);
                networkClient.getFirstSeenFromCloud(b);
            }
        }
        return firstSeen;
    }

    @Override
    protected Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance bd) {
        Issue issue = networkClient.getIssueByHash(bd.getInstanceHash());
        if (issue == null)
            return Collections.emptyList();

        Map<String, BugDesignation> map = new HashMap<String, BugDesignation>();
        for (Evaluation eval : sortEvals(issue.getEvaluationsList())) {
            map.put(eval.getWho(), createBugDesignation(eval));
        }
        return sortDesignations(map.values());
    }

    // ================================ bug filing
    // =====================================

    @Override
    public String getBugStatus(final BugInstance b) {
        if (bugFilingHelper == null)
            return null;
        final String hash = b.getInstanceHash();
        String status = bugStatusCache.get(hash);
        if (status != null) {
            return status;
        }
        return bugFilingHelper.lookupBugStatus(b);
    }

    @Override
    public URL getBugLink(BugInstance b) {
        if (getBugLinkStatus(b) == BugFilingStatus.FILE_BUG) {
            try {
                return fileBug(b);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

        } else {
            Issue issue = networkClient.getIssueByHash(b.getInstanceHash());
            if (issue == null)
                return null;
            String url = issue.getBugLink();
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.SEVERE, "Invalid bug link URL " + url, e);
                return null;
            }
        }
    }

    @Override
    public String getBugLinkType(BugInstance instance) {
        Issue issue = networkClient.getIssueByHash(instance.getInstanceHash());
        return issue != null && issue.hasBugLinkTypeStr() ? issue.getBugLinkTypeStr() : null;
    }

    @Override
    public URL fileBug(BugInstance bug) {
        try {
            return bugFilingHelper.fileBug(bug);
        } catch (SignInCancelledException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public BugFilingStatus getBugLinkStatus(BugInstance b) {
        Issue issue = networkClient.getIssueByHash(b.getInstanceHash());
        if (issue == null)
            return BugFilingStatus.NA;
        return issue.hasBugLink() ? BugFilingStatus.VIEW_BUG : BugFilingStatus.FILE_BUG;
    }

    @Override
    public boolean supportsBugLinks() {
        return bugFilingHelper != null && bugFilingHelper.bugFilingAvailable();
    }

    public void bugFiled(BugInstance b, Object bugLink) {
    }

    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType) throws IOException,
    SignInCancelledException {
        getNetworkClient().setBugLinkOnCloudAndStoreIssueDetails(b,viewUrl, linkType);
    }

    // ================== mutators ================

    @SuppressWarnings("deprecation")
    public void storeUserAnnotation(BugInstance bugInstance) {
        SigninState state = getSigninState();
        if (state == SigninState.SIGNED_IN || state == SigninState.UNAUTHENTICATED) {
            // no need to do this if we're not signed in yet, because it will
            // get picked up during the
            // upload-evals-from-XML step
            try {
                try {
                    networkClient.storeUserAnnotation(bugInstance);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } catch (SignInCancelledException e) {
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void updateBugInstanceAndNotify(final BugInstance bugInstance) {
        BugDesignation primaryDesignation = getPrimaryDesignation(bugInstance);
        long userTimestamp = bugInstance.getUserTimestamp();
        if (primaryDesignation != null && (userTimestamp == Long.MAX_VALUE || primaryDesignation.getTimestamp() > userTimestamp)) {
            bugInstance.setUserDesignation(primaryDesignation);
        }
        getBugUpdateExecutor().execute(new Runnable() {
            public void run() {
                // update anyway - the cloud report may have changed
                updatedIssue(bugInstance);
            }
        });
    }

    protected ExecutorService getBugUpdateExecutor() {
        return getGuiCallback().getBugUpdateExecutor();
    }

    public Collection<String> getProjects(String className) {
        return Collections.emptyList();
    }

    public boolean isInCloud(BugInstance b) {
        return networkClient.getIssueByHash(b.getInstanceHash()) != null;
    }

    // ============================== misc =====================================

    public ExecutorService getBackgroundExecutor() {
        return backgroundExecutorService;
    }

    public void updateBugStatusCache(final BugInstance b, String status) {
        bugStatusCache.put(b.getInstanceHash(), status);
        getBugUpdateExecutor().execute(new Runnable() {
            public void run() {
                updatedIssue(b);
            }
        });
    }

    private void signOut(boolean background) {
        networkClient.signOut(background);
        setSigninState(SigninState.SIGNED_OUT);
        setStatusMsg("Signed out of " + getCloudName());
    }

    /** for testing */
    void pretendIssuesSyncedAndUploaded() {
        communicationInitiated = true;
        bugsPopulated.countDown();
        issueDataDownloaded.countDown();
        fireNewIssuesUploadedEvent();
    }

    private List<BugDesignation> sortDesignations(Collection<BugDesignation> bugDesignations) {
        List<BugDesignation> designations = new ArrayList<BugDesignation>(bugDesignations);
        Collections.sort(designations, new Comparator<BugDesignation>() {
            public int compare(BugDesignation o1, BugDesignation o2) {
                return Util.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });
        return designations;
    }

    private List<Evaluation> sortEvals(List<Evaluation> evaluationsList) {
        List<Evaluation> evals = new ArrayList<Evaluation>(evaluationsList);
        Collections.sort(evals, new Comparator<Evaluation>() {
            public int compare(Evaluation o1, Evaluation o2) {
                return Util.compare(o1.getWhen(), o2.getWhen());
            }
        });
        return evals;
    }

    private void startEvaluationCheckThread() {
        if (timer != null)
            timer.cancel();
        timer = new Timer("App Engine Cloud evaluation updater", true);
        int periodMillis = EVALUATION_CHECK_SECS * 1000;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateEvaluationsFromServer();
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error during periodic evaluation check", e);
                }
            }
        }, periodMillis, periodMillis);
    }

    private static long dateMin(long timestamp1, long timestamp2) {
        if (timestamp1 < MIN_TIMESTAMP)
            return timestamp2;
        if (timestamp2 < MIN_TIMESTAMP)
            return timestamp1;
        return Math.min(timestamp1, timestamp2);

    }

    private void actuallyCheckBugsAgainstCloud() throws ExecutionException, InterruptedException {
        ConcurrentHashMap<String, BugInstance> bugsByHash = new ConcurrentHashMap<String, BugInstance>();

        for (BugInstance b : bugCollection.getCollection()) {
            bugsByHash.put(b.getInstanceHash(), b);
        }

        int numBugs = bugsByHash.size();
        MutableCloudTask task = createTask("Checking " + getCloudName());
        try {
            LOGGER.info("Checking " + numBugs + " bugs against the " + getCloudName() + "...");

            boolean exceptionThrown = true;
            try {
                List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
                networkClient.generateHashCheckRunnables(task, new ArrayList<String>(bugsByHash.keySet()), tasks, bugsByHash);
                executeAndWaitForAll(tasks);
                exceptionThrown = false;

            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Find issues interrupted!", e);

            } finally {
                issueDataDownloaded.countDown();
                fireIssueDataDownloadedEvent();
                if (exceptionThrown)
                    fireNewIssuesUploadedEvent();
            }

            if (getSigninState() == SigninState.SIGNIN_FAILED) {
                fireNewIssuesUploadedEvent();
                return;
            }
        } finally {
            task.finished();
        }

        Collection<BugInstance> newBugs = bugsByHash.values();
        boolean hasTimestampsToUpdate = !networkClient.getTimestampsToUpdate().isEmpty();
        boolean hasBugsToUpload = !newBugs.isEmpty();
        if ((hasBugsToUpload || hasTimestampsToUpdate)
                && (getCloudTokenProperty() != null
                || !getGuiCallback().isHeadless())) {
            uploadAndUpdateBugsInBackground(new ArrayList<BugInstance>(newBugs));
        } else {
            fireNewIssuesUploadedEvent();
            setStatusMsg("All " + numBugs + " bugs are already stored in the " + getCloudName());
        }

    }

    public String getCloudTokenProperty() {
        String value = SystemProperties.getProperty("findbugs.cloud.token");
        if (value == null)
            return null;
        return value.trim();
    }

    private void fireNewIssuesUploadedEvent() {
        LOGGER.log(Level.FINER, "new issues uploaded", new Throwable());
        newIssuesUploaded.countDown();
    }

    private void executeAndWaitForAll(List<Callable<Object>> tasks) throws ExecutionException, InterruptedException {
        if (backgroundExecutorService != null && backgroundExecutorService.isShutdown())
            LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown in executeAndWaitForAll");

        try {
            List<Future<Object>> results = backgroundExecutorService.invokeAll(tasks);
            for (Future<Object> result : results) {
                result.get();
            }

        } catch (RejectedExecutionException e) {
            if (backgroundExecutorService.isShutdown())
                LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown ", e);
            else if (backgroundExecutorService.isTerminated())
                LOGGER.log(Level.SEVERE, "backgroundExecutor service is terminated ", e);
            else
                LOGGER.log(Level.SEVERE, "Rejected execution", e);

        }
    }

    /** package-private for testing */
    void updateEvaluationsFromServer() throws IOException {
        MutableCloudTask task = createTask("Checking " + getCloudName() + " for updates");

        int count = 0;
        RecentEvaluations evals;
        try {
            int i = 0;
            do {
                evals = networkClient.getRecentEvaluationsFromServer();
                count += mergeUpdatedEvaluations(evals);
                task.update("found " + count + " so far...", 0);
            } while (evals.hasAskAgain() && evals.getAskAgain() && ++i < MAX_RECENT_EVALUATION_PAGES);

        } catch (ServerReturnedErrorCodeException e) {
            task.failed(e.getMessage());
            throw e;

        } catch (IOException e) {
            if (getSigninState() == SigninState.SIGNED_IN) {
                signOut(true);
                getGuiCallback().showMessageDialog(
                        "A network error occurred while checking the " + getCloudName() + " for updates.\n"
                                + "\n"
                                + "You have been automatically signed out of the Cloud. Any comments or \n"
                                + "evaluations you make will only be stored on your computer if you save the\n"
                                + "analysis via the File->Save menu.\n"
                                + "\n"
                                + "To sign back in, click the " + getCloudName() + " box in the lower right corner\n"
                                + "of the FindBugs window. Any changes you make while offline will be uploaded\n"
                                + "to the server upon signin.");
            } else {
                task.failed(e.getMessage());
            }
            throw e;

        } catch (RuntimeException e) {
            task.failed(e.getMessage());
            throw e;

        } finally {
            task.finished();
        }
        if (count > 0)
            setStatusMsg(getCloudName() + ": found " + count + " updated bug evaluations");
    }

    private int mergeUpdatedEvaluations(RecentEvaluations evals) {
        int found = 0;
        for (Issue updatedIssue : evals.getIssuesList()) {
            String protoHash = WebCloudProtoUtil.decodeHash(updatedIssue.getHash());
            BugInstance bugInstance = getBugByHash(protoHash);
            if (bugInstance == null)
                continue;
            Issue existingIssue = networkClient.getIssueByHash(protoHash);
            Issue issueToStore;
            if (existingIssue != null) {
                issueToStore = mergeIssues(existingIssue, updatedIssue);
                String newHash = WebCloudProtoUtil.decodeHash(issueToStore.getHash());
                assert newHash.equals(protoHash) : newHash + " vs " + protoHash;

            } else {
                issueToStore = updatedIssue;
            }
            networkClient.storeIssueDetails(protoHash, issueToStore);
            updateBugInstanceAndNotify(bugInstance);
            found++;
        }
        return found;
    }

    private void uploadAndUpdateBugsInBackground(final List<BugInstance> newBugs) {
        backgroundExecutorService.execute(new Runnable() {
            public void run() {
                List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
                MutableCloudTask taskA = null;
                MutableCloudTask taskB = null;
                try {
                    taskA = networkClient.generateUploadRunnables(newBugs, callables);
                    taskB = networkClient.generateUpdateTimestampRunnables(callables);
                    executeAndWaitForAll(callables);

                } catch (SignInCancelledException e) {
                    // OK!
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "", e);
                } finally {
                    if (taskA != null)
                        taskA.finished();
                    if (taskB != null)
                        taskB.finished();
                }
                fireNewIssuesUploadedEvent();
            }
        });
    }

    private BugDesignation createBugDesignation(Evaluation e) {
        return new BugDesignation(e.getDesignation(), e.getWhen(), e.getComment(), e.getWho());
    }

    private Issue mergeIssues(Issue existingIssue, Issue updatedIssue) {
        List<Evaluation> allEvaluations = new ArrayList<Evaluation>();
        allEvaluations.addAll(existingIssue.getEvaluationsList());
        allEvaluations.addAll(updatedIssue.getEvaluationsList());
        removeAllButLatestEvaluationPerUser(allEvaluations);

        return Issue.newBuilder(updatedIssue).clearEvaluations().addAllEvaluations(allEvaluations).build();
    }

    private void removeAllButLatestEvaluationPerUser(List<Evaluation> allEvaluations) {
        Set<String> seenUsernames = new HashSet<String>();
        for (ListIterator<Evaluation> it = reverseIterator(allEvaluations); it.hasPrevious();) {
            Evaluation evaluation = it.previous();
            boolean isNewUsername = seenUsernames.add(evaluation.getWho());
            if (!isNewUsername)
                it.remove();
        }
    }

    private <E> ListIterator<E> reverseIterator(List<E> list) {
        return list.listIterator(list.size());
    }
}
