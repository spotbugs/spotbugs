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

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.SystemProperties;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Set;


/**
 * An interface for describing how a bug collection interacts with the FindBugs
 * Cloud.
 * 
 * Each Cloud instance is associated with a BugCollection.
 */
public interface Cloud {
	
	CloudPlugin getPlugin();

	BugCollection getBugCollection();

	/** Get a status message for the cloud; information about any errors, and
	 * information about database synchronization 
	 */
	String getStatusMsg();

	public void addListener(CloudListener listener);
	public void removeListener(CloudListener listener);

    public void addStatusListener(CloudStatusListener cloudStatusListener);
    public void removeStatusListener(CloudStatusListener cloudStatusListener);

    /** Do we have the configuration information needed to try initializing the cloud;
	 * calling this method should have no side effects and not display any dialogs
	 * or make any network connections. 
	 * @return true if we have the needed information
	 */
	public boolean availableForInitialization();
	
	/** Attempt to initialize the cloud
	 * @return true if successful
	 */
	public boolean initialize() throws IOException;

    /** Waits until all data about this bug collection has been received from the cloud. */
    void waitUntilIssueDataDownloaded();

	
	/** Called after the bugs in the bug collection are loaded; 
	 * synchronizes them with the database */
	public void bugsPopulated();
	
	/** Initiate communication with the cloud. Clouds can implement lazy communication, where they
	 * don't initiate communication with the cloud until a request for cloud data is seen, or a call
	 * is made to {@link #waitUntilIssueDataDownloaded()}.
	 * A call to this method forces eager initiation of communication.*/
	public void initiateCommunication();

	/** Shutdown the cloud, note termination of session, close connections */
	public void shutdown();

	/** Get voting mode */
	Mode getMode();

	/** Set voting mode */
	void setMode(Mode m);

	/** has the user said they will fix this bug */
	boolean getIWillFix(BugInstance b);

	/** Does the cloud support source lines  (e.g., to FishEye) */
	boolean supportsSourceLinks();

	/** Tool tip text for "view source" button */
	String getSourceLinkToolTip(@CheckForNull BugInstance b);

	/** URL to view the source for a bug instance */
	URL getSourceLink(BugInstance b);

	/** Get user name */
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

	/* Supports links to a bug database */ 
	boolean supportsBugLinks();

	/* get the bug filing status for a bug instance */ 
	BugFilingStatus getBugLinkStatus(BugInstance b);
	
	/** has the issue been marked "will not be fixed" in a bug tracker */
	boolean getWillNotBeFixed(BugInstance b);
	
	/** does the issue have an unassigned issue in the bug tracker */
	boolean getBugIsUnassigned(BugInstance b);

	

	/** Get link for bug, either to file one or to view it */
	URL getBugLink(BugInstance b);

    URL fileBug(BugInstance bug);

	/** Note that we've initiated or completed a request to file a bug;
	 * @param b bug against which bug was filed
	 * @param bugLink if we have any information about the result of filing the bug, it should go here
	 */
	void bugFiled(BugInstance b, @CheckForNull Object bugLink);

    

	/** Supports textual summaries about the status of a bug */
	boolean supportsCloudReports();

	/**
	 * 
	 * Get the cloud report for a bug 
	 */
	String getCloudReport(BugInstance b);

	/** Supports allowing users to claim a bug */
	boolean supportsClaims();

	/** Get the user who has claimed a bug; null if no one has */
	@CheckForNull
	String claimedBy(BugInstance b);
	
	/**
	 * Claim the bug; true if no one else has already done so 
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

	UserDesignation getConsensusDesignation(BugInstance b);

	/** Update user designation and evaluation from information in bug instance and push to database */
	void storeUserAnnotation(BugInstance bugInstance);

	/** Is this bug one that gets persisted to the cloud?
	 * We may decide that we don't persist low confidence issues to the 
	 * database to avoid overloading it */
	boolean canStoreUserAnnotation(BugInstance bugInstance);

	public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes);

	public boolean supportsCloudSummaries();

    Collection<String> getProjects(String className);

    String getCloudName();

    String getBugLinkType(BugInstance instance);

    interface CloudListener {
		void issueUpdated(BugInstance bug);
		void statusUpdated();
	}

    public interface CloudStatusListener {
        void handleIssueDataDownloadedEvent();
        void handleStateChange(SigninState oldState, SigninState state);
    }

    enum SigninState { NO_SIGNIN_REQUIRED, NOT_SIGNED_IN_YET, SIGNING_IN, SIGNED_IN, SIGNIN_FAILED, SIGNED_OUT,  }

	enum UserDesignation {
		UNCLASSIFIED,
		NEEDS_STUDY,
		BAD_ANALYSIS,
		NOT_A_BUG,
		MOSTLY_HARMLESS,
		SHOULD_FIX,
		MUST_FIX,
		I_WILL_FIX,
		OBSOLETE_CODE;


		public int score() {
			switch (this) {

			case BAD_ANALYSIS:
				return -3;
			case NOT_A_BUG:
			case OBSOLETE_CODE:
				return  -2;
			case MOSTLY_HARMLESS:
				return -1;
			case SHOULD_FIX:
				return  1;
			case MUST_FIX:
			case I_WILL_FIX:
				return 2;
			default:
				return 0;
			}
		}

		/**
         * @return
         */
         public  boolean nonVoting() {
            return this == UserDesignation.OBSOLETE_CODE
              || this == UserDesignation.NEEDS_STUDY
              || this == UserDesignation.UNCLASSIFIED;
        }
	}

	enum Mode { COMMUNAL, VOTING, SECRET }

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
