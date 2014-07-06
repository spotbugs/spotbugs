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
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * An interface for describing how a bug collection interacts with the FindBugs
 * Cloud.
 *
 * Each Cloud instance is associated with a BugCollection.
 */
public interface Cloud {

    CloudPlugin getPlugin();

    String getCloudName();

    BugCollection getBugCollection();

    IGuiCallback getGuiCallback();

    /**
     * Get a status message for the cloud; information about any errors, and
     * information about database synchronization
     */
    String getStatusMsg();

    public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes);

    public void addListener(CloudListener listener);

    public void removeListener(CloudListener listener);

    public void addStatusListener(CloudStatusListener cloudStatusListener);

    public void removeStatusListener(CloudStatusListener cloudStatusListener);

    // ========================== initialization
    // ====================================

    /**
     * Do we have the configuration information needed to try initializing the
     * cloud; calling this method should have no side effects and not display
     * any dialogs or make any network connections.
     *
     * @return true if we have the needed information
     */
    public boolean availableForInitialization();

    /**
     * Attempt to initialize the cloud
     *
     * @return true if successful
     */
    public boolean initialize() throws IOException;

    /** Return true if the cloud has been successfully initialized */
    public boolean isInitialized();
    /**
     * Waits until all new issues have been uploaded
     */
    public void waitUntilNewIssuesUploaded();

    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Waits until all data about this bug collection has been received from the
     * cloud.
     */
    public void waitUntilIssueDataDownloaded();

    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit)
            throws InterruptedException;

    /**
     * Returns true if communication has already been initiated (and perhaps completed).
     *
     */
    public boolean communicationInitiated();
    /**
     * Called after the bugs in the bug collection are loaded; bugs should not
     * be synchronized before this method is called
     */
    public void bugsPopulated();

    /**
     * Initiate communication with the cloud. Clouds can implement lazy
     * communication, where they don't initiate communication with the cloud
     * until a request for cloud data is seen, or a call is made to
     * {@link #waitUntilIssueDataDownloaded()}. A call to this method forces
     * eager initiation of communication.
     */
    public void initiateCommunication();

    /** Shutdown the cloud, note termination of session, close connections */
    public void shutdown();

    // ================ signin / signout =================

    String getUser();

    SigninState getSigninState();

    /**
     * Whether the cloud should save login information, session ID's, etc. If
     * disabled, the user will need to re-authenticate each session.
     */
    void setSaveSignInInformation(boolean save);

    boolean isSavingSignInInformationEnabled();

    void signIn() throws IOException;

    void signOut();

    // ================================= misc settings
    // =============================

    /** Get voting mode */
    Mode getMode();

    /** Set voting mode */
    void setMode(Mode m);

    /** Does the cloud support source lines (e.g., to FishEye) */
    boolean supportsSourceLinks();

    /** Supports links to a bug database */
    boolean supportsBugLinks();

    /** Supports textual summaries about the status of a bug */
    boolean supportsCloudReports();

    /** Supports allowing users to claim a bug */
    boolean supportsClaims();

    boolean supportsCloudSummaries();

    /**
     * Get a list of names of FB projects that the given class
     * "may be a part of." Used for filing bugs.
     */
    Collection<String> getProjects(String className);

    // ===================================== bug instances
    // =============================

    /**
     * returns whether the bug is stored remotely or not. for bug collection
     * storage, always returns true
     */
    boolean isInCloud(BugInstance b);

    boolean isOnlineCloud();

    /** has the user said they will fix this bug */
    boolean getIWillFix(BugInstance b);

    /** Tool tip text for "view source" button */
    String getSourceLinkToolTip(@CheckForNull BugInstance b);

    /** URL to view the source for a bug instance */
    URL getSourceLink(BugInstance b);

    /** get the bug filing status for a bug instance */
    BugFilingStatus getBugLinkStatus(BugInstance b);

    /**
     * A textual description of the bug status (e.g., FIX_LATER, ASSIGNED,
     * OBSOLETE, WILL_NOT_FIX)
     */
    String getBugStatus(BugInstance b);

    /** has the issue been marked "will not be fixed" in a bug tracker */
    boolean getWillNotBeFixed(BugInstance b);

    /** does the issue have an unassigned issue in the bug tracker */
    boolean getBugIsUnassigned(BugInstance b);

    /** Get link for bug, either to file one or to view it */
    URL getBugLink(BugInstance b);

    String getBugLinkType(BugInstance instance);

    URL fileBug(BugInstance b);

    void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType) throws IOException,
    SignInCancelledException;

    /** Updates the local cache of bug reporting status. Does not modify server code. */
    void updateBugStatusCache(BugInstance b, String status);

    /**
     * Note that we've initiated or completed a request to file a bug;
     *
     * @param b
     *            bug against which bug was filed
     * @param bugLink
     *            if we have any information about the result of filing the bug,
     *            it should go here
     */
    void bugFiled(BugInstance b, @CheckForNull Object bugLink);

    String getCloudReport(BugInstance b);

    String getCloudReportWithoutMe(BugInstance b);

    /** Get the user who has claimed a bug; null if no one has */
    @CheckForNull
    String claimedBy(BugInstance b);

    /**
     * Claim the bug
     *
     * @return true if no one else has already done so
     */
    boolean claim(BugInstance b);

    /** Return the time the user last changed their evaluation of this bug */
    long getUserTimestamp(BugInstance b);

    Date getUserDate(BugInstance b);

    /** Get the most recent BugDesignation from the current user */
    BugDesignation getPrimaryDesignation(BugInstance b);

    /** Get the user's designation for the bug */
    UserDesignation getUserDesignation(BugInstance b);

    /** Get free text evaluation of the bug */
    String getUserEvaluation(BugInstance b);

    double getClassificationScore(BugInstance b);

    double getClassificationVariance(BugInstance b);

    double getClassificationDisagreement(BugInstance b);

    double getPortionObsoleteClassifications(BugInstance b);

    int getNumberReviewers(BugInstance b);

    Set<String> getReviewers(BugInstance b);

    long getFirstSeen(BugInstance b);

    void addDateSeen(BugInstance b, long when);

    /**
     * @return {@link UserDesignation#UNCLASSIFIED} if no consensus has been reached
     */
    UserDesignation getConsensusDesignation(BugInstance b);

    boolean overallClassificationIsNotAProblem(BugInstance b);

    // =========================== mutators ===========================

    /**
     * Is this bug one that gets persisted to the cloud? We may decide that we
     * don't persist low confidence issues to the database to avoid overloading
     * it
     */
    boolean canStoreUserAnnotation(BugInstance bugInstance);

    /**
     * Update user designation and evaluation from information in bug instance
     * and push to database
     */
    void storeUserAnnotation(BugInstance bugInstance);

    // ========================= statistics ===========================

    interface CloudListener {
        void issueUpdated(BugInstance bug);

        void statusUpdated();

        void taskStarted(CloudTask task);
    }

    interface CloudStatusListener {
        void handleIssueDataDownloadedEvent();

        void handleStateChange(SigninState oldState, SigninState state);
    }

    interface CloudTask {
        String getName();

        String getStatusLine();

        double getPercentCompleted();

        void addListener(CloudTaskListener listener);

        void removeListener(CloudTaskListener listener);

        boolean isFinished();

        void setUseDefaultListener(boolean enabled);
    }

    interface CloudTaskListener {
        void taskStatusUpdated(String statusLine, double percentCompleted);

        void taskFinished();

        void taskFailed(String message);
    }

    enum SigninState {
        NO_SIGNIN_REQUIRED, UNAUTHENTICATED, SIGNING_IN, SIGNED_IN, SIGNIN_FAILED, SIGNIN_DECLINED, SIGNED_OUT, DISCONNECTED;


        /** Can download issues without asking to sign in */
        public boolean canDownload() {
            switch (this) {
            case NO_SIGNIN_REQUIRED:
            case SIGNING_IN:
            case SIGNED_IN:
            case UNAUTHENTICATED:
                return true;
            default:
                return false;
            }

        }

        /** Can upload issues without asking to sign in */
        public boolean canUpload() {
            switch (this) {
            case NO_SIGNIN_REQUIRED:
            case SIGNING_IN:
            case SIGNED_IN:
                return true;
            default:
                return false;
            }

        }
        /** Should ask to sign in if new issues to upload found */
        public boolean shouldAskToSignIn() {
            switch (this) {
            case UNAUTHENTICATED:
            case SIGNED_OUT:
            case SIGNIN_FAILED:
                return true;
            default:
                return false;
            }
        }

        /** Could ask to sign in if new issues to upload found */
        public boolean couldSignIn() {
            switch (this) {
            case UNAUTHENTICATED:
            case DISCONNECTED:
            case SIGNED_OUT:
            case SIGNIN_FAILED:
            case SIGNIN_DECLINED:
                return true;
            default:
                return false;
            }
        }
        @edu.umd.cs.findbugs.internalAnnotations.StaticConstant
        static final ResourceBundle names = ResourceBundle.getBundle(Cloud.class.getName(), Locale.getDefault());


        @Override
        public String toString() {
            try {
                return names.getString(this.name()).trim();
            } catch (MissingResourceException e) {
                return this.name();
            }
        }
    }

    enum UserDesignation {
        UNCLASSIFIED, NEEDS_STUDY, BAD_ANALYSIS, NOT_A_BUG, MOSTLY_HARMLESS, SHOULD_FIX, MUST_FIX, I_WILL_FIX, OBSOLETE_CODE;

        public int score() {
            switch (this) {

            case BAD_ANALYSIS:
                return -3;
            case NOT_A_BUG:
            case OBSOLETE_CODE:
                return -2;
            case MOSTLY_HARMLESS:
                return -1;
            case SHOULD_FIX:
                return 1;
            case MUST_FIX:
            case I_WILL_FIX:
                return 2;
            default:
                return 0;
            }
        }

        public boolean nonVoting() {
            return this == UserDesignation.OBSOLETE_CODE || this == UserDesignation.NEEDS_STUDY
                    || this == UserDesignation.UNCLASSIFIED;
        }

        public boolean notAProblem() {
            return this.score() < 0;
        }
        public boolean shouldFix() {
            return this.score() > 0;
        }
    }

    enum Mode {
        COMMUNAL, VOTING, SECRET
    }

    enum BugFilingStatus {
        /** No bug yet filed */
        FILE_BUG(SystemProperties.getProperty("findbugs.filebug.label", "File bug")) {
            @Override
            public boolean bugIsFiled() {
                return false;
            }
        },
        /** URL was sent to browser, but request has expired */
        FILE_AGAIN("File again"),

        /** Sent a URL to a browser to file a bug, no information yet */
        BUG_PENDING("Bug pending") {
            @Override
            public boolean linkEnabled() {
                return false;
            }
        },
        /** synchronized bug instance with bug database */
        VIEW_BUG(SystemProperties.getProperty("findbugs.viewbug.label", "View bug")),

        /* Not applicable, bug linking not supported */
        NA("") {
            @Override
            public boolean linkEnabled() {
                return false;
            }

            @Override
            public boolean bugIsFiled() {
                return false;
            }
        };

        final String displayName;

        public boolean bugIsFiled() {
            return true;
        }

        public boolean linkEnabled() {
            return true;
        }

        BugFilingStatus(String name) {
            this.displayName = name;
        }

        @Override
        public String toString() {
            return displayName;
        }

    }
}
