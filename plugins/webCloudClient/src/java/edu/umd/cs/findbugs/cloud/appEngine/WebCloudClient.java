package edu.umd.cs.findbugs.cloud.appEngine;

import java.io.File;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ComponentPlugin;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.BugFiler;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.MutableCloudTask;
import edu.umd.cs.findbugs.cloud.OnlineCloud;
import edu.umd.cs.findbugs.cloud.SignInCancelledException;
import edu.umd.cs.findbugs.cloud.Cloud.CloudStatusListener;
import edu.umd.cs.findbugs.cloud.Cloud.SigninState;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.WebCloudProtoUtil;
import edu.umd.cs.findbugs.cloud.username.WebCloudNameLookup;
import edu.umd.cs.findbugs.util.Util;

@SuppressWarnings({ "ThrowableInstanceNeverThrown" })
public class WebCloudClient extends AbstractCloud implements OnlineCloud {

 

    private static final Logger LOGGER = Logger.getLogger(WebCloudClient.class.getPackage().getName());

    private static final int EVALUATION_CHECK_SECS_MIN_SECS = 5 * 60;
    private static final int EVALUATION_CHECK_SECS_MAX_SECS = 2*60 * 60;
    
    private static final int MAX_RECENT_EVALUATION_PAGES = 30;

    protected final @Nonnull ExecutorService backgroundExecutorService;
    protected final @Nonnull UncaughtExceptionHandler uncaughtBackgroundExceptionHandler;

    private Timer timer;

    private WebCloudNetworkClient networkClient;

    private Map<String, String> bugStatusCache = new ConcurrentHashMap<String, String>();

    private final @CheckForNull BugFilingHelper bugFilingHelper;

    private CountDownLatch issueDataDownloaded = new CountDownLatch(1);

    protected CountDownLatch newIssuesUploaded = new CountDownLatch(1);

    private CountDownLatch bugsPopulated = new CountDownLatch(1);

    private final EvaluationsFromXmlUploader evaluationsFromXmlUploader = new EvaluationsFromXmlUploader(this);

    static @CheckForNull ComponentPlugin<BugFiler> findFirstLoadedBugFilerPlugin() {
        for (Plugin p : DetectorFactoryCollection.instance().plugins()) {
            for (ComponentPlugin<BugFiler> bf : p.getComponentPlugins(BugFiler.class)) {
                return bf;
            }
        }
        return null;
    }
    /** invoked via reflection */
    @SuppressWarnings({ "UnusedDeclaration" })
    public WebCloudClient(CloudPlugin plugin, BugCollection bugs, Properties properties) {
        super(plugin, bugs, properties);
        setNetworkClient(new WebCloudNetworkClient());
        uncaughtBackgroundExceptionHandler = getUncaughtBackgroundExceptionHandler();
        backgroundExecutorService = createBackgroundExecutorService();
        if (backgroundExecutorService.isShutdown())
            LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown at creation");

        ComponentPlugin<BugFiler> bugFilerPlugin = findFirstLoadedBugFilerPlugin();
        if (bugFilerPlugin == null) {
            this.bugFilingHelper = null;
        } else {
            this.bugFilingHelper = new BugFilingHelper(this, bugFilerPlugin);
        }
    }

    protected UncaughtExceptionHandler getUncaughtBackgroundExceptionHandler() {
        return new UncaughtExceptionHandler() {

            public void uncaughtException(Thread t, Throwable e) {
                LOGGER.log(Level.SEVERE, "Exception in background thread " + t, e);
                
            }};
    }
    protected ExecutorService createBackgroundExecutorService() {
        return Executors.newFixedThreadPool(4, new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, WebCloudClient.class.getSimpleName() + " bg");
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(uncaughtBackgroundExceptionHandler);
                return t;
            }
        });
    }

    /** package-private for testing */
    void setNetworkClient(WebCloudNetworkClient networkClient) {
        this.networkClient = networkClient;
        networkClient.setCloudClient(this);
    }

    // ====================== initialization =====================

    public void waitUntilNewIssuesUploaded() {
        checkInitialized();
        try {
            newIssuesUploaded.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
    }
    
    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        checkInitialized();
        return newIssuesUploaded.await(timeout, unit);
    }

    
    public void waitUntilIssueDataDownloaded() {
        checkInitialized();
        try {
            bugsPopulated.await();

            initiateCommunication();

            LOGGER.fine("Waiting for issue data to be downloaded");

            issueDataDownloaded.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "interrupted", e);
        }

    }

    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit) throws InterruptedException {
        checkInitialized();
        try {
            bugsPopulated.await();

        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "interrupted", e);
            return false;
        }

        initiateCommunication();

        LOGGER.fine("Waiting for issue data to be downloaded");

        return issueDataDownloaded.await(timeout, unit);
    }
    public boolean availableForInitialization() {
        return true;
    }

    boolean initialized = false;

    @Override
    public boolean isInitialized() {
        return super.isInitialized() && initialized;
    }
    @Override
    public boolean initialize() throws IOException {
        // noinspection ConstantConditions
        if (false && initialized) {
            LOGGER.warning("Already initialized " + getClass().getSimpleName());
            return true;
        }

        LOGGER.fine("Initializing " + getClass().getSimpleName());
        setSigninState(SigninState.UNAUTHENTICATED);
        
        addStatusListener(new CloudStatusListener() {
            
            public void handleStateChange(SigninState oldState, SigninState state) {
                if (oldState.canUpload() && !state.canUpload())
                    evaluationsFromXmlUploader.reset();
            }
            
            public void handleIssueDataDownloadedEvent() {
                // TODO Auto-generated method stub
                
            }
        });

        try {
            if (!super.initialize()) {
                setSigninState(SigninState.SIGNIN_FAILED);
                return false;
            }

            boolean networkInitialization = networkClient.initialize();
            initialized = true;
            if (networkInitialization) {
                setSigninState(SigninState.SIGNED_IN);
            } else {
                // soft init didn't work
                setSigninState(SigninState.UNAUTHENTICATED);
            }
            
            return true;

        } catch (UnknownHostException e) {
            initialized = true;
            setSigninState(SigninState.DISCONNECTED);
            return true;
        } catch (IOException e) {
            setSigninState(SigninState.SIGNIN_FAILED);
            throw e;
        }

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
                if (result != 0) {
                    setSigninState(SigninState.SIGNIN_DECLINED);
                    throw new SignInCancelledException();
                }
            }
            try {
                signIn();
            } catch (Exception e) {
                getGuiCallback().showMessageDialog("Could not sign into " + getCloudName() + ": "
                        + Util.getNetworkErrorMessage(e));
                setSigninState(SigninState.SIGNIN_FAILED);
                throw new SignInCancelledException(e);
            }

        } else if (getSigninState() == SigninState.SIGNING_IN) {
            // thread that set state to signing in should already hold lock
            synchronized (signInLock) {
            }
        } else if (getSigninState() == SigninState.SIGNED_IN) {
            // great!
        }
    }

    public boolean couldSignIn() {
        return getSigninState().couldSignIn();
    }


    
    @Override
    public void shutdown() {
        switch(getSigninState()) {
        case SIGNED_IN:
            signOut();
        }

        super.shutdown();
        if (timer != null)
            timer.cancel();

        backgroundExecutorService.shutdownNow();
    }

    
    private void checkInitialized() {
        if (!initialized)
            throw new IllegalStateException("Cloud not initialized");
    }
    public void bugsPopulated() {
        checkInitialized();
        evaluationsFromXmlUploader.tryUploadingLocalAnnotations(false);
        bugsPopulated.countDown();
    }

    private final Object initiationLock = new Object();

    private volatile boolean communicationInitiated = false;
    
    private void mockFailure() throws ServerReturnedErrorCodeException {
        File check = new File("/tmp/mockFindBugsCloudFailure");
        if (!check.canRead()) 
            return;
        if ( check.lastModified() + 15 * 60 * 1000 > System.currentTimeMillis())
            throw new ServerReturnedErrorCodeException(503, "mock failure due to presence of " + check);
    }

    public void initiateCommunication() {
        checkInitialized();
        bugsPopulated();

        if (communicationInitiated)
            return;

        if (!networkClient.ready())
            return;
        synchronized (initiationLock) {
            if (communicationInitiated)
                return;
            communicationInitiated = true;
            backgroundExecutorService.execute(new Runnable() {
                public void run() {
                    try {
                        mockFailure();
                        actuallyCheckBugsAgainstCloud();
                    } catch (RejectedExecutionException e) {
                        // I think this only happens on purpose -Keith
                    } catch (Throwable e) {
                        if (e instanceof ExecutionException)
                            e = e.getCause();
                        String errorMsg = Util.getNetworkErrorMessage(e);
                        getGuiCallback().showMessageDialog("Could not connect to " + getCloudName()
                                + "\n\n" +  errorMsg 
                                + "\nsignin status: " + getSigninState());
                        LOGGER.log(Level.SEVERE, "Error while checking bugs against cloud in background" , e);
                    }
                }
            });

        }
    }

    /**
     * Returns true if communication has already been initiated (and perhaps completed).
     * 
     */
    @Override
    public boolean communicationInitiated() {
        return bugsPopulated.getCount() == 0 && communicationInitiated && networkClient.ready();
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
        checkInitialized();
        SigninState oldState = getSigninState();
        synchronized (signInLock) {
            if (getSigninState() == SigninState.SIGNED_IN)
                return;
            setSigninState(SigninState.SIGNING_IN);
            MutableCloudTask task = createTask("Signing into " + getCloudName());
            try {
                try {
                    if (oldState == SigninState.DISCONNECTED) {
                        if (networkClient.initialize()) {
                            networkClient.signIn(true);
                            networkClient.logIntoCloudForce();
                            setSigninState(SigninState.SIGNED_IN);
                        } else {
                            // soft init didn't work
                            setSigninState(SigninState.UNAUTHENTICATED);
                        }
                    } else {
                        networkClient.signIn(true);
                        networkClient.logIntoCloudForce();
                        setSigninState(SigninState.SIGNED_IN);
                    }
                } catch (UnknownHostException e) {
                    getGuiCallback().showMessageDialog("Could not connect to " + getCloudName());
                    setSigninState(SigninState.DISCONNECTED);
                } catch (IOException e) {
                    setSigninState(SigninState.SIGNIN_FAILED);
                    throw e;
                } catch (RuntimeException e) {
                    setSigninState(SigninState.SIGNIN_FAILED);
                    throw e;
                }

            } finally {
                task.finished();
            }
        }
        if (getSigninState().canDownload()) {
        initiateCommunication();
        SigninState newState = getSigninState();
        if (!oldState.canUpload()  && newState.canUpload()) {
            evaluationsFromXmlUploader.tryUploadingLocalAnnotations(true);
        }
        }
    }

    public void signOut() {
        signOut(false);
    }

    public String getUser() {
        return networkClient.getUsername();
    }

    public @CheckForNull BugDesignation getPrimaryDesignation(BugInstance b) {
        checkInitialized();
        initiateCommunication();
        Evaluation e = networkClient.getMostRecentEvaluationBySelf(b);
        return e == null ? null : createBugDesignation(e);
    }
    public @CheckForNull BugDesignation getDesignationByUser(BugInstance b, String user) {
        checkInitialized();
        initiateCommunication();
        Evaluation e = networkClient.getMostRecentEvaluationByUser(b, user);
        return e == null ? null : createBugDesignation(e);
    }


    static final boolean DEBUG_FIRST_SEEN = Boolean.getBoolean("debug.first.seen");

    @Override
    public long getFirstSeen(BugInstance b) {
        initiateCommunication();
        long firstSeenFromCloud = networkClient.getFirstSeenFromCloud(b);
        long firstSeenLocally = getLocalFirstSeen(b);
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
        initiateCommunication();
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
        checkInitialized();
        initiateCommunication();
        if (getBugLinkStatus(b) == BugFilingStatus.FILE_BUG) {
            try {
                return fileBug(b);

            } catch (RuntimeException e) {
                throw e;

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
        checkInitialized();
        if (bugFilingHelper == null)
            return null;
        initiateCommunication();
        try {
            return bugFilingHelper.fileBug(bug);
        } catch (SignInCancelledException e) {
            return null;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public BugFilingStatus getBugLinkStatus(BugInstance b) {
        checkInitialized();
        initiateCommunication();
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
        checkInitialized();
        initiateCommunication();
        getNetworkClient().setBugLinkOnCloudAndStoreIssueDetails(b,viewUrl, linkType);
    }

    // ================== mutators ================

    @SuppressWarnings("deprecation")
    public void storeUserAnnotation(BugInstance bugInstance) {
        checkInitialized();
        SigninState state = getSigninState();
        if (state.canUpload() || state.shouldAskToSignIn()) {
            // no need to do this if we're not signed in yet, because it will
            // get picked up during the
            // upload-evals-from-XML step
            try {
                try {
                    networkClient.storeUserAnnotation(bugInstance);
                    bugInstance.setUserAnnotationDirty(false);
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
        return getSigninState().canDownload() && networkClient.getIssueByHash(b.getInstanceHash()) != null;
    }

    public boolean isOnlineCloud() {
        return true;
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
        if (getSigninState() == SigninState.DISCONNECTED)
            return;
        networkClient.signOut(background);
        setSigninState(SigninState.SIGNED_OUT);
        setStatusMsg("Signed out of " + getCloudName());
    }

    /** for testing */
    void pretendIssuesSyncedAndDownloaded() {
        communicationInitiated = true;
        bugsPopulated.countDown();
        issueDataDownloaded.countDown();
    }

    /** for testing */
    void pretendIssuesSyncedAndDownloadedAndUploaded() {
        pretendIssuesSyncedAndDownloaded();
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

    volatile int updateInternal = EVALUATION_CHECK_SECS_MIN_SECS;
    
    private final class GetUpdatedEvaluations extends TimerTask {
        @Override
        public void run() {
            if (CloudFactory.DEBUG)
                System.out.printf("checking for updates from server%n");
           
            try {
                int count = updateEvaluationsFromServer();
                if (count == 0)
                    updateInternal = Math.min(updateInternal*2, EVALUATION_CHECK_SECS_MAX_SECS);
                else
                    updateInternal = EVALUATION_CHECK_SECS_MIN_SECS;
            } catch (ServerReturnedErrorCodeException e) {
                updateInternal = Math.min(updateInternal*2, EVALUATION_CHECK_SECS_MAX_SECS);
                LOGGER.log(Level.WARNING, "Error during periodic review update check");
                LOGGER.log(Level.FINEST, "", e);
            } catch (IOException e) {
                updateInternal = Math.min(updateInternal*2, EVALUATION_CHECK_SECS_MAX_SECS);
                LOGGER.log(Level.WARNING, "Error during periodic review update check", e);
                LOGGER.log(Level.FINEST, "", e);
            } finally {
                scheduleUpdateEvaluationsFromServer();
            }
        }
    }
    
    private void scheduleUpdateEvaluationsFromServer() {
        if (CloudFactory.DEBUG)
            System.out.printf("Scheduling check for new evaluations from the server in %d seconds%n", updateInternal);
        timer.schedule(new GetUpdatedEvaluations(), TimeUnit.MILLISECONDS.convert(updateInternal, TimeUnit.SECONDS) );
    }
        
    
    private void startEvaluationCheckThread() {
        checkInitialized();
        if (timer != null)
            timer.cancel();
        timer = new Timer("App Engine Cloud review update checker", true);
        scheduleUpdateEvaluationsFromServer();
    }

    private static long dateMin(long timestamp1, long timestamp2) {
        if (timestamp1 < MIN_TIMESTAMP)
            return timestamp2;
        if (timestamp2 < MIN_TIMESTAMP)
            return timestamp1;
        return Math.min(timestamp1, timestamp2);

    }

    private void actuallyCheckBugsAgainstCloud() throws ExecutionException, InterruptedException {
        checkInitialized();
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
                ArrayList<String> hashes = new ArrayList<String>(bugsByHash.keySet());
                networkClient.generateHashCheckRunnables(task, hashes, tasks, bugsByHash);
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
               // TODO: Why would we do this?
               if (false) 
                   fireNewIssuesUploadedEvent();
                return;
            }
        } finally {
            task.finished();
        }

        Collection<BugInstance> newBugs = bugsByHash.values();
        boolean hasTimestampsToUpdate = !networkClient.getTimestampsToUpdate().isEmpty();
        boolean hasBugsToUpload = !newBugs.isEmpty();
        if (!getGuiCallback().isHeadless()) {
            startEvaluationCheckThread();
        };
        String cloudTokenProperty = getCloudTokenProperty();
        boolean canUpload = cloudTokenProperty != null
        || !getGuiCallback().isHeadless()
        || getSigninState() == SigninState.SIGNED_IN;
        if ((hasBugsToUpload || hasTimestampsToUpdate)) {
            if (canUpload) {
                uploadAndUpdateBugsInBackground(new ArrayList<BugInstance>(newBugs));
            } else {
                setStatusMsg("not able to automatically upload bugs to the " + getCloudName());

                fireNewIssuesUploadedEvent();
            }
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

    private <T> void executeAndWaitForAll(List<Callable<T>> tasks) throws ExecutionException, InterruptedException {
        if (backgroundExecutorService.isShutdown()) {
            LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown in executeAndWaitForAll");
            return;
        }

        try {
            List<Future<T>> results = backgroundExecutorService.invokeAll(tasks);
            for (Future<T> result : results) {
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
    int updateEvaluationsFromServer() throws IOException {
        checkInitialized();
        if (!networkClient.ready())
            return 0;
        if (issueDataDownloaded.getCount() > 0)
            return 0;
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
                                + "You have been automatically signed out of the Cloud. Any reviews\n"
                                + "you add or change will only be stored on your computer if you save the\n"
                                + "analysis via the File->Save menu.\n"
                                + "\n"
                                + "To attempt to sign back in, click the Sign In button on the left.");
            } else {
                task.failed(Util.getNetworkErrorMessage(e));
            }
            throw e;

        } catch (RuntimeException e) {
            task.failed(e.getClass().getSimpleName() + ": " + e.getMessage());
            throw e;

        } finally {
            task.finished();
        }
        if (count > 0)
            setStatusMsg(getCloudName() + ": found " + count + " updated bug reviews");
        return count;
    }

    long lastEvaluation(Issue issue) {
        long when = Long.MIN_VALUE;
        for(Evaluation e : issue.getEvaluationsList()) {
            if (when < e.getWhen())
                when = e.getWhen();
        }
        return when;
    }
    private int mergeUpdatedEvaluations(RecentEvaluations evals) {
        int found = 0;
        for (Issue updatedIssue : evals.getIssuesList()) {
            String protoHash = WebCloudProtoUtil.decodeHash(updatedIssue.getHash());
            BugInstance bugInstance = getBugByHash(protoHash);
            if (bugInstance == null) {
                if (CloudFactory.DEBUG) 
                    System.out.printf("No match for %s in %s, updated %tc%n", updatedIssue.getBugPattern(), updatedIssue.getPrimaryClass(), 
                            lastEvaluation(updatedIssue));
                continue;
            }
            Issue existingIssue = networkClient.getIssueByHash(protoHash);
            Issue issueToStore;
            if (existingIssue != null) {
                Issue oldIssue = Issue.newBuilder(existingIssue).build();
                issueToStore = mergeIssues(existingIssue, updatedIssue);
                String newHash = WebCloudProtoUtil.decodeHash(issueToStore.getHash());
                assert newHash.equals(protoHash) : newHash + " vs " + protoHash;
                if (oldIssue.equals(issueToStore)) {
                    if (CloudFactory.DEBUG) 
                        System.out.println("no new information");
                    continue;
                } else {
                    if (CloudFactory.DEBUG) 
                        System.out.printf("Got new information for %s in %s @ %tc : %s%n", issueToStore.getBugPattern(), issueToStore.getPrimaryClass() ,
                                lastEvaluation(updatedIssue), issueToStore.getHash());
                }

            } else {
                issueToStore = updatedIssue;
                System.out.printf("Got new issue %s in %s @ %tc : %s%n", issueToStore.getBugPattern(), 
                        issueToStore.getPrimaryClass() , 
                        lastEvaluation(updatedIssue), issueToStore.getHash().toStringUtf8());
                
                
            }
            networkClient.storeIssueDetails(protoHash, issueToStore);
            updateBugInstanceAndNotify(bugInstance);
            found++;
        }
        return found;
    }

    private void uploadAndUpdateBugsInBackground(final List<BugInstance> newBugs) {
        checkInitialized();
        backgroundExecutorService.execute(new Runnable() {
            public void run() {
                List<Callable<Void>> callables = new ArrayList<Callable<Void>>();
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

    public void addDateSeen(BugInstance b, long when) {
        throw new UnsupportedOperationException();
    }

}
