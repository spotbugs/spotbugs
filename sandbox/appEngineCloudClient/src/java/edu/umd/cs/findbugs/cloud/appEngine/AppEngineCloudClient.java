package edu.umd.cs.findbugs.cloud.appEngine;

import com.atlassian.jira.rpc.soap.beans.RemoteIssue;
import com.google.gdata.client.authn.oauth.OAuthException;
import com.google.gdata.data.projecthosting.IssuesEntry;
import com.google.gdata.util.ServiceException;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.cloud.AbstractCloud;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.NotSignedInException;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;
import edu.umd.cs.findbugs.cloud.username.AppEngineNameLookup;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;

@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public class AppEngineCloudClient extends AbstractCloud {

    private static final Logger LOGGER = Logger.getLogger(AppEngineCloudClient.class.getName());
    private static final int EVALUATION_CHECK_SECS = 5 * 60;

    private Executor backgroundExecutor;
	private @CheckForNull ExecutorService backgroundExecutorService;
	private Timer timer;

    private AppEngineCloudNetworkClient networkClient;
    private SignedInState signedInState = SignedInState.SIGNING_IN;
    private GoogleCodeBugFiler googleCodeBugFiler;
    private JiraBugFiler jiraBugFiler;
    private Map<String,String> bugStatusCache = new ConcurrentHashMap<String, String>();

    /** used via reflection */
    @SuppressWarnings({"UnusedDeclaration"})
    public AppEngineCloudClient(CloudPlugin plugin, BugCollection bugs, Properties properties) {
		this(plugin, bugs, properties, null);
        setNetworkClient(new AppEngineCloudNetworkClient());
	}

	/** for testing */
	AppEngineCloudClient(CloudPlugin plugin, BugCollection bugs,  Properties properties,
                         @CheckForNull Executor executor) {

		super(plugin, bugs, properties);
		if (executor == null) {
			backgroundExecutorService = Executors.newFixedThreadPool(10);
			backgroundExecutor = backgroundExecutorService;
			if (backgroundExecutorService.isShutdown())
	    		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown at creation");

		} else {
			backgroundExecutorService = null;
			backgroundExecutor = executor;
		}
        googleCodeBugFiler = new GoogleCodeBugFiler(this);
        jiraBugFiler = new JiraBugFiler(this);
    }

	/** package-private for testing */
    void setNetworkClient(AppEngineCloudNetworkClient networkClient) {
        this.networkClient = networkClient;
        networkClient.setCloudClient(this);
    }

    // ====================== initialization =====================

	public boolean availableForInitialization() {
		return true;
	}

	public boolean initialize() throws IOException {
        if (!super.initialize()) {
            signedInState = SignedInState.SIGNIN_FAILED;

            return false;
        }
        try {
            if (networkClient.initialize()) {

                networkClient.logIntoCloudForce();
                signedInState = SignedInState.SIGNED_IN;
                setStatusMsg("");

            } else {
                // soft init didn't work
                signedInState = SignedInState.NOT_SIGNED_IN_YET;
            }
        } catch (IOException e) {
            signedInState = SignedInState.SIGNIN_FAILED;
            return false;
        }

        if (timer != null)
			timer.cancel();
		timer = new Timer("App Engine Cloud evaluation updater", true);
		int periodMillis = EVALUATION_CHECK_SECS *1000;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				updateEvaluationsFromServer();
			}
		}, periodMillis, periodMillis);
		return true;
	}

    /**
     * @param reason if null, no question dialog will be shown, signin will be automatic
     */
    public void signInIfNecessary(@CheckForNull String reason) throws NotSignedInException {
        if (couldSignIn()) {

            if (reason != null) {
                IGuiCallback callback = getGuiCallback();
                int result = callback.showConfirmDialog(reason, "FindBugs Cloud", "Sign in", "Cancel");
                if (result != 0)
                    return;
                else
                    throw new NotSignedInException();
            }
            try {
                signIn();
            } catch (IOException e) {
                setStatusMsg("Could not sign into Cloud: " + e.getMessage());
                throw new NotSignedInException(e);
            }

        } else if (signedInState == SignedInState.SIGNING_IN) {
            // huh?
            throw new IllegalStateException("signing in");
        } else if (signedInState == SignedInState.SIGNED_IN) {
            // great!
        }
    }

    public boolean couldSignIn() {
        return signedInState == SignedInState.NOT_SIGNED_IN_YET
            || signedInState == SignedInState.SIGNIN_FAILED
            || signedInState == SignedInState.SIGNED_OUT;
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
		final ConcurrentMap<String, BugInstance> bugsByHash = new ConcurrentHashMap<String, BugInstance>();

		for(BugInstance b : bugCollection.getCollection()) {
			bugsByHash.put(b.getInstanceHash(), b);
		}

		backgroundExecutor.execute(new Runnable() {
            public void run() {
                try {
                    actuallyCheckBugsAgainstCloud(bugsByHash);

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while checking bugs against cloud in background", e);
                }
            }
        });
	}

    // =============== accessors ===================

    AppEngineCloudNetworkClient getNetworkClient() {
        return networkClient;
    }

    public SignedInState getSignedInState() {
        return signedInState;
    }

    public void setSaveSignInInformation(boolean save) {
        AppEngineNameLookup.setSaveSessionInformation(save);
        if (save) {
            Long sessionId = networkClient.getSessionId();
            if (sessionId != null) {
                AppEngineNameLookup.saveSessionInformation(sessionId);
            }
        }
    }

    public boolean isSavingSignInInformationEnabled() {
        return AppEngineNameLookup.isSavingSessionInfoEnabled();
    }

    public void signIn() throws IOException {
        signedInState = SignedInState.SIGNING_IN;
        setStatusMsg("Signing into FindBugs Cloud");
        try {
            networkClient.signIn(true);
            networkClient.logIntoCloudForce();
        } catch (IOException e) {
            signedInState = SignedInState.SIGNIN_FAILED;

            throw e;
        } catch (RuntimeException e) {
            signedInState = SignedInState.SIGNIN_FAILED;

            throw e;
        }

        signedInState = SignedInState.SIGNED_IN;
        setStatusMsg("");
    }

    public void signOut() {
        LOGGER.log(Level.INFO, "signing out", new RuntimeException("Signing out"));

        if (backgroundExecutorService != null) {
            backgroundExecutorService.shutdownNow();
            try {
                if (!backgroundExecutorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    LOGGER.warning("Waited 500ms for background executor to finish but it didn't");
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "", e);
            }
        }
        networkClient.signOut();
        signedInState = SignedInState.SIGNED_OUT;
        setStatusMsg("Signed out of FindBugs Cloud");
    }

    public String getUser() {
		return networkClient.getUsername();
	}

	public BugDesignation getPrimaryDesignation(BugInstance b) {
		Evaluation e = networkClient.getMostRecentEvaluation(b);
		return e == null ? null : createBugDesignation(e);
	}

    static final boolean DEBUG_FIRST_SEEN = false;

    public long getLocalFirstSeen(BugInstance bug) {
        return super.getFirstSeen(bug);
    }

	public long getFirstSeen(BugInstance b) {
		long firstSeenFromCloud = networkClient.getFirstSeenFromCloud(b);
		long firstSeenLocally = super.getFirstSeen(b);
		long firstSeen = dateMin(firstSeenFromCloud, firstSeenLocally);

		if (DEBUG_FIRST_SEEN) {
			System.out.println(b.getMessageWithoutPrefix());
			System.out.printf("%s %s %s\n", new Date(firstSeenFromCloud),
					new Date(firstSeenLocally), new Date(firstSeen));
			if (firstSeenFromCloud == Long.MAX_VALUE) {
				new RuntimeException("Not seen previously")
						.printStackTrace(System.out);
				networkClient.getFirstSeenFromCloud(b);
			}
		}
		return firstSeen;
	}

    @Override
	protected Iterable<BugDesignation> getAllUserDesignations(BugInstance bd) {
        Issue issue = networkClient.getIssueByHash(bd.getInstanceHash());
        if (issue == null)
            return Collections.emptyList();

        List<BugDesignation> list = new ArrayList<BugDesignation>();
        for (Evaluation eval : issue.getEvaluationsList()) {
			list.add(createBugDesignation(eval));
		}
		return list;
	}

    // ================================ bug filing =====================================

    @Override
    protected String getBugStatus(final BugInstance b) {
        final String hash = b.getInstanceHash();
        String status = bugStatusCache.get(hash);
        if (status != null) {
            return status;
        }
        Issue issue = networkClient.getIssueByHash(hash);
        ProtoClasses.BugLinkType linkType = ProtoClasses.BugLinkType.GOOGLE_CODE; // default to googlecode
        if (issue.hasBugLinkType())
            linkType = issue.getBugLinkType();

        final BugFiler bugFiler;
        switch (linkType) {
            case GOOGLE_CODE:
                bugFiler = googleCodeBugFiler;
                break;
            case JIRA:
                bugFiler = jiraBugFiler;
                break;
            default:
                return "<unknown>";
        }
        final String bugLink = issue.getBugLink();
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                String status = null;
                try {
                    status = bugFiler.getBugStatus(bugLink);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error while connecting to bug tracker", e);
                }
                if (status == null)
                    status = "<unknown>";
                bugStatusCache.put(hash, status);
                getBugUpdateExecutor().execute(new Runnable() {
                    public void run() {
                        updatedIssue(b);
                    }
                });
            }
        });
        status = "<loading...>";
        bugStatusCache.put(hash, status);
        return status;
    }

    @Override
	public URL getBugLink(BugInstance b) {
        if (getBugLinkStatus(b) == BugFilingStatus.FILE_BUG) {
		    try {
                String jiraUrl = askUserForJiraUrl();
                if (jiraUrl == null)
                    return null;
                IGuiCallback callback = bugCollection.getProject().getGuiCallback();
                List<String> result = callback.showForm("", "File a bug on JIRA", Arrays.asList(
                        new IGuiCallback.FormItem("Project", null, jiraBugFiler.getProjectKeys(jiraUrl)),
                        new IGuiCallback.FormItem("Component"),
                        new IGuiCallback.FormItem("Type", null, jiraBugFiler.getIssueTypes(jiraUrl))));
                if (result == null)
                    return null; // user cancelled
                RemoteIssue issue = jiraBugFiler.fileBug(jiraUrl, b, result.get(0), result.get(1), result.get(2));
                String bugUrl = jiraUrl + "/browse/" + issue.getKey();
                networkClient.setBugLinkOnCloudAndStoreIssueDetails(b, bugUrl, ProtoClasses.BugLinkType.JIRA);
                return new URL(bugUrl);

//                String projectName = askUserForGoogleCodeProjectName();
//                if (projectName == null)
//                    return null;
//                return fileGoogleCodeBug(b, projectName);

            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            String url = networkClient.getIssueByHash(b.getInstanceHash()).getBugLink();
            try {
                return new URL(url);
            } catch (MalformedURLException e) {
                LOGGER.log(Level.SEVERE, "Invalid bug link URL " + url, e);
                return null;
            }
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
		return true;
	}

	public void bugFiled(BugInstance b, Object bugLink) {
	}

	// ================== mutators ================

	@SuppressWarnings("deprecation")
	public void storeUserAnnotation(BugInstance bugInstance) throws NotSignedInException {
        networkClient.storeUserAnnotation(bugInstance);
	}

	@SuppressWarnings("deprecation")
    public void updateBugInstanceAndNotify(final BugInstance bugInstance) {
        getBugUpdateExecutor().execute(new Runnable() {
            public void run() {
                BugDesignation primaryDesignation = getPrimaryDesignation(bugInstance);
                if (primaryDesignation != null) {
                    bugInstance.setUserDesignation(primaryDesignation);
                }
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

	// ================== private methods ======================

	private static long dateMin(long timestamp1, long timestamp2) {
		if (timestamp1 < AbstractCloud.MIN_TIMESTAMP)
			return timestamp2;
		if (timestamp2 < MIN_TIMESTAMP)
			return timestamp1;
		return Math.min(timestamp1, timestamp2);

	}

    private IGuiCallback getGuiCallback() {
        return getBugCollection().getProject().getGuiCallback();
    }

    private void actuallyCheckBugsAgainstCloud(ConcurrentMap<String, BugInstance> bugsByHash) throws IOException {
        int numBugs = bugsByHash.size();
        setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud...");

        List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
        networkClient.generateHashCheckRunnables(new ArrayList<String>(bugsByHash.keySet()), tasks, bugsByHash);

        executeAndWaitForAll(tasks);

        Collection<BugInstance> newBugs = bugsByHash.values();
        if (!newBugs.isEmpty() || !networkClient.getTimestampsToUpdate().isEmpty()) {
            uploadAndUpdateBugsInBackground(new ArrayList<BugInstance>(newBugs));
        } else {
            setStatusMsg("All " + numBugs + " bugs are already stored in the FindBugs Cloud");
        }
    }

    private void executeAndWaitForAll(List<Callable<Object>> tasks) {
       if (backgroundExecutorService != null && backgroundExecutorService.isShutdown())
    		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown in executeAndWaitForAll");
        try {
            List<Future<Object>> results = backgroundExecutorService.invokeAll(tasks);
            for (Future<Object> result : results) {
                result.get();
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "error while starting hash check threads", e);
        } catch (ExecutionException e) {
        	if (backgroundExecutorService.isShutdown())
        		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown ", e);
        	else 	if (backgroundExecutorService.isTerminated())
       		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is termination ", e);
        	else
        		 LOGGER.log(Level.SEVERE, "execution exception", e);

        }  catch (RejectedExecutionException e) {
             	if (backgroundExecutorService.isShutdown())
             		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is shutdown ", e);
             	else 	if (backgroundExecutorService.isTerminated())
            		 LOGGER.log(Level.SEVERE, "backgroundExecutor service is termination ", e);
             	else
             		 LOGGER.log(Level.SEVERE, "Rejected execution", e);


        }
    }

    /** package-private for testing */
	void updateEvaluationsFromServer() {
		setStatusMsg("Checking FindBugs Cloud for updates");

		RecentEvaluations evals;
		try {
			evals = networkClient.getRecentEvaluationsFromServer();
		} catch (IOException e) {
            setStatusMsg("Checking FindBugs Cloud for updates...failed - " + e.getMessage());
			throw new IllegalStateException(e);
		}
		if (evals.getIssuesCount() > 0)
			setStatusMsg("Checking FindBugs Cloud for updates...found " + evals.getIssuesCount());
		else
			setStatusMsg("");
		for (Issue issue : evals.getIssuesList()) {
            String protoHash = decodeHash(issue.getHash());
            Issue existingIssue = networkClient.getIssueByHash(protoHash);
			if (existingIssue != null) {
				Issue newIssue = mergeIssues(existingIssue, issue);
                String newHash = decodeHash(newIssue.getHash());
                assert newHash.equals(protoHash) : newHash + " vs " + protoHash;
                networkClient.storeIssueDetails(protoHash, newIssue);
				BugInstance bugInstance = getBugByHash(protoHash);
				if (bugInstance != null) {
					updateBugInstanceAndNotify(bugInstance);
				}
			}
		}
	}

    private URL fileGoogleCodeBug(BugInstance b, String projectName)
            throws IOException, ServiceException, OAuthException, InterruptedException, NotSignedInException {
        IssuesEntry googleCodeIssue = googleCodeBugFiler.file(b, projectName);
        if (googleCodeIssue == null)
            return null;

        String viewUrl = googleCodeIssue.getHtmlLink().getHref();
        if (viewUrl == null) {
            LOGGER.warning("Filed issue on Google Code, but URL is missing!");
            return null;
        }

        bugStatusCache.put(b.getInstanceHash(), googleCodeIssue.getStatus().getValue());

        networkClient.setBugLinkOnCloudAndStoreIssueDetails(b, viewUrl, ProtoClasses.BugLinkType.GOOGLE_CODE);

        return new URL(viewUrl);
    }

    private String askUserForJiraUrl() {
        IGuiCallback guiCallback = bugCollection.getProject().getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(AppEngineCloudClient.class);

        String lastProject = prefs.get("last_jira_url", "");
        String dashboardUrl = guiCallback.showQuestionDialog(
                "Issue will be filed in JIRA.\n" +
                "\n" +
                "JIRA dashboard URL:\n" +
                "(ex. http://jira.atlassian.com/secure/Dashboard.jspa)", "JIRA",
                lastProject);
        if (dashboardUrl == null || dashboardUrl.trim().length() == 0) {
            return null;
        }
        dashboardUrl = processJiraDashboardUrl(dashboardUrl);
        prefs.put("last_jira_url", dashboardUrl);
        return dashboardUrl;
    }

    /** package-private for testing */
    static String processJiraDashboardUrl(String dashboardUrl) {
        dashboardUrl = dashboardUrl.trim();
        Matcher m = Pattern.compile("(?:https?://)?(.*?)(?:/secure(?:/Dashboard.jspa)?.*)?").matcher(dashboardUrl);
        if (m.matches()) {
            dashboardUrl = "http://" + m.group(1);
        }
        return dashboardUrl;
    }

    private String askUserForGoogleCodeProjectName() {
        IGuiCallback guiCallback = bugCollection.getProject().getGuiCallback();
        Preferences prefs = Preferences.userNodeForPackage(AppEngineCloudClient.class);

        String lastProject = prefs.get("last_google_code_project", "");
        String projectName = guiCallback.showQuestionDialog(
                "Issue will be filed at Google Code.\n" +
                "\n" +
                "Google Code project name:", "Google Code Issue Tracker",
                lastProject);
        if (projectName == null || projectName.trim().length() == 0) {
            return null;
        }
        prefs.put("last_google_code_project", projectName);
        return projectName;
    }

    private void uploadAndUpdateBugsInBackground(final List<BugInstance> newBugs) {
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                List<Callable<Object>> callables = new ArrayList<Callable<Object>>();
                try {
                    networkClient.generateUploadRunnables(newBugs, callables);
                    networkClient.generateUpdateTimestampRunnables(callables);
                    executeAndWaitForAll(callables);

                } catch (NotSignedInException e) {
                    // OK!
                }
                setStatusMsg("");
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

		return Issue.newBuilder(updatedIssue)
				.clearEvaluations()
				.addAllEvaluations(allEvaluations)
				.build();
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
