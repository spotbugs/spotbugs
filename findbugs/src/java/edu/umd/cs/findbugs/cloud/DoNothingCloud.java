package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;

/**
 * Doesn't do much. Relies on the {@link edu.umd.cs.findbugs.BugInstance.XmlProps}
 * read from the analysis XML file, if present.
 */
public class DoNothingCloud implements Cloud {
    private final CloudPlugin plugin;
    private final BugCollection bugCollection;

    private static CloudPlugin getFallbackPlugin() {
        return new CloudPluginBuilder().setCloudid("edu.umd.cs.findbugs.cloud.doNothingCloud").setDescription("Do Nothing Cloud")
                .setDetails("No reviews will be stored.")
                .setClassLoader(BugCollectionStorageCloud.class.getClassLoader())
                .setCloudClass(BugCollectionStorageCloud.class)
                .setUsernameClass(NoNameLookup.class)
                .setProperties(new PropertyBundle())
                .setOnlineStorage(false)
                .createCloudPlugin();
    }

    /** Invoked via reflection */
    @SuppressWarnings({"UnusedDeclaration"})
    public DoNothingCloud(CloudPlugin plugin, BugCollection bc, Properties props) {
        this.plugin = plugin;
        this.bugCollection = bc;
    }

    public DoNothingCloud(BugCollection bc) {
        this(getFallbackPlugin(), bc, new Properties());
    }

    @Override
    public CloudPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getCloudName() {
        return "(no cloud selected)";
    }

    @Override
    public BugCollection getBugCollection() {
        return bugCollection;
    }

    @Override
    public IGuiCallback getGuiCallback() {
        return null;
    }

    @Override
    public String getStatusMsg() {
        return null;
    }

    @Override
    public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes) {
    }

    @Override
    public void addListener(CloudListener listener) {
    }

    @Override
    public void removeListener(CloudListener listener) {
    }

    @Override
    public void addStatusListener(CloudStatusListener cloudStatusListener) {
    }

    @Override
    public void removeStatusListener(CloudStatusListener cloudStatusListener) {
    }

    @Override
    public boolean availableForInitialization() {
        return true;
    }

    @Override
    public boolean initialize() {
        return true;
    }

    @Override
    public void waitUntilNewIssuesUploaded() {
    }

    @Override
    public void waitUntilIssueDataDownloaded() {
    }

    @Override
    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }
    @Override
    public void bugsPopulated() {
    }

    @Override
    public void initiateCommunication() {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public String getUser() {
        return null;
    }

    @Override
    public SigninState getSigninState() {
        return SigninState.NO_SIGNIN_REQUIRED;
    }

    @Override
    public void setSaveSignInInformation(boolean save) {
    }

    @Override
    public boolean isSavingSignInInformationEnabled() {
        return false;
    }

    @Override
    public void signIn() throws IOException {
    }

    @Override
    public void signOut() {
    }

    @Override
    public Mode getMode() {
        return null;
    }

    @Override
    public void setMode(Mode m) {
    }

    @Override
    public boolean supportsSourceLinks() {
        return false;
    }

    @Override
    public boolean supportsBugLinks() {
        return false;
    }

    @Override
    public boolean supportsCloudReports() {
        return false;
    }

    @Override
    public boolean supportsClaims() {
        return false;
    }

    @Override
    public boolean supportsCloudSummaries() {
        return false;
    }

    @Override
    public Collection<String> getProjects(String className) {
        return null;
    }

    @Override
    public boolean isInCloud(BugInstance b) {
        return b.getXmlProps().isInCloud();
    }

    @Override
    public boolean isOnlineCloud() {
        return "true".equals(bugCollection.getXmlCloudDetails().get("online"));
    }

    @Override
    public boolean getIWillFix(BugInstance b) {
        return false;
    }

    @Override
    public String getSourceLinkToolTip(@CheckForNull BugInstance b) {
        return null;
    }

    @Override
    public URL getSourceLink(BugInstance b) {
        return null;
    }

    @Override
    public BugFilingStatus getBugLinkStatus(BugInstance b) {
        return null;
    }

    @Override
    public String getBugStatus(BugInstance b) {
        return null;
    }

    @Override
    public boolean getWillNotBeFixed(BugInstance b) {
        return false;
    }

    @Override
    public boolean getBugIsUnassigned(BugInstance b) {
        return false;
    }

    @Override
    public URL getBugLink(BugInstance b) {
        return null;
    }

    @Override
    public String getBugLinkType(BugInstance instance) {
        return null;
    }

    @Override
    public URL fileBug(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType)
            throws IOException, SignInCancelledException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateBugStatusCache(BugInstance b, String status) {
    }

    @Override
    public void bugFiled(BugInstance b, @CheckForNull Object bugLink) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCloudReport(BugInstance b) {
        return "";
    }

    @Override
    public String getCloudReportWithoutMe(BugInstance b) {
        return getCloudReport(b);
    }

    @Override
    public String claimedBy(BugInstance b) {
        return null;
    }

    @Override
    public boolean claim(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUserTimestamp(BugInstance b) {
        return 0;
    }

    @Override
    public Date getUserDate(BugInstance b) {
        return null;
    }

    @Override
    public BugDesignation getPrimaryDesignation(BugInstance b) {
        return null;
    }

    @Override
    public UserDesignation getUserDesignation(BugInstance b) {
        return null;
    }

    @Override
    public String getUserEvaluation(BugInstance b) {
        return null;
    }

    @Override
    public double getClassificationScore(BugInstance b) {
        return 0;
    }

    @Override
    public double getClassificationVariance(BugInstance b) {
        return 0;
    }

    @Override
    public double getClassificationDisagreement(BugInstance b) {
        return 0;
    }

    @Override
    public double getPortionObsoleteClassifications(BugInstance b) {
        return 0;
    }

    @Override
    public int getNumberReviewers(BugInstance b) {
        return b.getXmlProps().getReviewCount();
    }

    @Override
    public Set<String> getReviewers(BugInstance b) {
        return Collections.emptySet();
    }

    @Override
    public long getFirstSeen(BugInstance b) {
        long computed = getFirstSeenFromVersion(b);
        Date fromXml = b.getXmlProps().getFirstSeen();
        if (fromXml == null) {
            return computed;
        }

        long fromXmlTime = fromXml.getTime();
        if (computed == 0 && fromXmlTime > 0) {
            return fromXmlTime;
        } else if (fromXmlTime == 0 && computed > 0) {
            return computed;
        }

        return Math.min(fromXmlTime, computed);
    }

    @Override
    public void addDateSeen(BugInstance b, long when) {
        if (when > 0) {
            b.getXmlProps().setFirstSeen(new Date(when));
        }
    }

    public long getFirstSeenFromVersion(BugInstance b) {
        long firstVersion = b.getFirstVersion();
        AppVersion v = getBugCollection().getAppVersionFromSequenceNumber(firstVersion);
        if (v == null) {
            return getBugCollection().getTimestamp();
        }
        return v.getTimestamp();
    }

    @Override
    public UserDesignation getConsensusDesignation(BugInstance b) {
        String consensus = b.getXmlProps().getConsensus();
        if (consensus == null) {
            return UserDesignation.UNCLASSIFIED;
        }
        try {
            return UserDesignation.valueOf(consensus);
        } catch (IllegalArgumentException e) {
            return UserDesignation.UNCLASSIFIED;
        }
    }

    @Override
    public boolean overallClassificationIsNotAProblem(BugInstance b) {
        UserDesignation consensusDesignation = getConsensusDesignation(b);
        return consensusDesignation != UserDesignation.UNCLASSIFIED && consensusDesignation.score() < 0;
    }

    @Override
    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
        return false;
    }

    @Override
    public void storeUserAnnotation(BugInstance bugInstance) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean communicationInitiated() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }


}
