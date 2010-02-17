package edu.umd.cs.findbugs.cloud.appEngine;

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
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Evaluation;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.Issue;
import edu.umd.cs.findbugs.cloud.appEngine.protobuf.ProtoClasses.RecentEvaluations;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import static edu.umd.cs.findbugs.cloud.appEngine.protobuf.AppEngineProtoUtil.decodeHash;

public class AppEngineCloudClient extends AbstractCloud {

    private static final Logger LOGGER = Logger.getLogger(AppEngineCloudClient.class.getName());
    private static final int EVALUATION_CHECK_SECS = 5 * 60;

    private Executor backgroundExecutor;
	private @CheckForNull ExecutorService backgroundExecutorService;
	private Timer timer;

    private AppEngineCloudNetworkClient networkClient;
    private LoggedInState loggedInState = LoggedInState.LOGGING_IN;

    public AppEngineCloudClient(CloudPlugin plugin, BugCollection bugs) {
		this(plugin, bugs, null);
        setNetworkClient(new AppEngineCloudNetworkClient());
	}

	/** for testing */
	AppEngineCloudClient(CloudPlugin plugin, BugCollection bugs,
                         @CheckForNull Executor executor) {
		super(plugin, bugs);
		if (executor == null) {
			backgroundExecutorService = Executors.newCachedThreadPool();
			backgroundExecutor = backgroundExecutorService;
		} else {
			backgroundExecutorService = null;
			backgroundExecutor = executor;
		}
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

	public boolean initialize() {
        if (!super.initialize()) {
            loggedInState = LoggedInState.LOGIN_FAILED;

            return false;
        }
        setStatusMsg("Signing into FindBugs Cloud");
        if (!networkClient.initialize()) {
            loggedInState = LoggedInState.LOGIN_FAILED;

            setStatusMsg("");
            return false;
        }

        loggedInState = LoggedInState.LOGGED_IN;

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
		final Map<String, BugInstance> bugsByHash = new HashMap<String, BugInstance>();

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

    public LoggedInState getLoggedInState() {
        return loggedInState;
    }

    public String getUser() {
		return networkClient.getUsername();
	}

	public BugDesignation getPrimaryDesignation(BugInstance b) {
		Evaluation e = networkClient.getMostRecentEvaluation(b);
		return e == null ? null : createBugDesignation(e);
	}

	public long getFirstSeen(BugInstance b) {
        long firstSeenFromCloud = networkClient.getFirstSeenFromCloud(b);
        long firstSeenLocally = super.getFirstSeen(b);
        return Math.min(firstSeenFromCloud, firstSeenLocally);
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
	public URL getBugLink(BugInstance b) {
        if (getBugLinkStatus(b) == BugFilingStatus.FILE_BUG) {
		    try {
                return fileBug(b);

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
	public void storeUserAnnotation(BugInstance bugInstance) {
        networkClient.storeUserAnnotation(bugInstance);
	}

	@SuppressWarnings("deprecation")
    public void updateBugInstanceAndNotify(final BugInstance bugInstance) {
        getBugCollection().getProject().getGuiCallback().getBugUpdateExecutor().execute(new Runnable() {
            public void run() {
                BugDesignation primaryDesignation = getPrimaryDesignation(bugInstance);
                if (primaryDesignation != null) {
                    bugInstance.setUserDesignation(primaryDesignation);
                    updatedIssue(bugInstance);
                }
            }
        });
    }

	public Collection<String> getProjects(String className) {
		return Collections.emptyList();
	}

	// ================== private methods ======================

    private void actuallyCheckBugsAgainstCloud(Map<String, BugInstance> bugsByHash) throws IOException {
        int numBugs = bugsByHash.size();
        setStatusMsg("Checking " + numBugs + " bugs against the FindBugs Cloud...");
        networkClient.logIntoCloud();

        networkClient.checkHashes(new ArrayList<String>(bugsByHash.keySet()), bugsByHash);

        Collection<BugInstance> newBugs = bugsByHash.values();
        if (!newBugs.isEmpty()) {
            uploadBugsInBackground(new ArrayList<BugInstance>(newBugs));
        } else {
            setStatusMsg("All " + numBugs + " bugs are already stored in the FindBugs Cloud");
        }
    }

    /** package-private for testing */
	void updateEvaluationsFromServer() {
		setStatusMsg("Checking FindBugs Cloud for updates");

		RecentEvaluations evals;
		try {
			evals = networkClient.getRecentEvaluationsFromServer();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		if (evals.getIssuesCount() > 0)
			setStatusMsg("Checking FindBugs Cloud for updates...found " + evals.getIssuesCount());
		else
			setStatusMsg("");
		for (Issue issue : evals.getIssuesList()) {
			Issue existingIssue = networkClient.getIssueByHash(decodeHash(issue.getHash()));
			if (existingIssue != null) {
				Issue newIssue = mergeIssues(existingIssue, issue);
				assert newIssue.getHash().equals(issue.getHash());
                networkClient.storeIssueDetails(decodeHash(issue.getHash()), newIssue);
				BugInstance bugInstance = getBugByHash(decodeHash(issue.getHash()));
				if (bugInstance != null) {
					updateBugInstanceAndNotify(bugInstance);
				}
			}
		}
	}

    private URL fileBug(BugInstance b) throws IOException, ServiceException, OAuthException, InterruptedException {
        IssuesEntry googleCodeIssue = fileBugOnGoogleCode(b);
        if (googleCodeIssue == null)
            return null;

        String viewUrl = googleCodeIssue.getHtmlLink().getHref();
        if (viewUrl == null) {
            LOGGER.warning("Filed issue on Google Code, but URL is missing!");
            return null;
        }

        networkClient.setBugLinkOnCloud(b, viewUrl);

        String hash = b.getInstanceHash();
        networkClient.storeIssueDetails(hash,
                                        Issue.newBuilder(networkClient.getIssueByHash(hash))
                                                .setBugLink(viewUrl)
                                                .build());

        return new URL(viewUrl);
    }

    private IssuesEntry fileBugOnGoogleCode(BugInstance b)
            throws IOException, ServiceException, OAuthException, InterruptedException {
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
        GoogleCodeBugFiler bugFiler = new GoogleCodeBugFiler(this, projectName);
        IssuesEntry issue = bugFiler.file(b);
        if (issue == null)
            return null;
        return issue;
    }

    private void uploadBugsInBackground(final List<BugInstance> newBugs) {
        backgroundExecutor.execute(new Runnable() {
            public void run() {
                try {
                    networkClient.uploadNewBugs(newBugs);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error while uploading new bugs", e);
                }
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

		return Issue.newBuilder(existingIssue)
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
