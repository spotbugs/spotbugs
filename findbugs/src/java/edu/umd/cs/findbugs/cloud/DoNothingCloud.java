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
    private CloudPlugin plugin;
    private BugCollection bugCollection;

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

    public CloudPlugin getPlugin() {
        return plugin;
    }

    public String getCloudName() {
        return "(no cloud selected)";
    }

    public BugCollection getBugCollection() {
        return bugCollection;
    }

    public IGuiCallback getGuiCallback() {
        return null;
    }

    public String getStatusMsg() {
        return null;
    }

    public void printCloudSummary(PrintWriter w, Iterable<BugInstance> bugs, String[] packagePrefixes) {
    }

    public void addListener(CloudListener listener) {
    }

    public void removeListener(CloudListener listener) {
    }

    public void addStatusListener(CloudStatusListener cloudStatusListener) {
    }

    public void removeStatusListener(CloudStatusListener cloudStatusListener) {
    }

    public boolean availableForInitialization() {
        return true;
    }

    public boolean initialize() {
        return true;
    }

    public void waitUntilNewIssuesUploaded() {
    }

    public void waitUntilIssueDataDownloaded() {
    }

    public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }
    public void bugsPopulated() {
    }

    public void initiateCommunication() {
    }

    public void shutdown() {
    }

    public String getUser() {
        return null;
    }

    public SigninState getSigninState() {
        return SigninState.NO_SIGNIN_REQUIRED;
    }

    public void setSaveSignInInformation(boolean save) {
    }

    public boolean isSavingSignInInformationEnabled() {
        return false;
    }

    public void signIn() throws IOException {
    }

    public void signOut() {
    }

    public Mode getMode() {
        return null;
    }

    public void setMode(Mode m) {
    }

    public boolean supportsSourceLinks() {
        return false;
    }

    public boolean supportsBugLinks() {
        return false;
    }

    public boolean supportsCloudReports() {
        return false;
    }

    public boolean supportsClaims() {
        return false;
    }

    public boolean supportsCloudSummaries() {
        return false;
    }

    public Collection<String> getProjects(String className) {
        return null;
    }

    public boolean isInCloud(BugInstance b) {
        return b.getXmlProps().isInCloud();
    }

    public boolean isOnlineCloud() {
        return "true".equals(bugCollection.getXmlCloudDetails().get("online"));
    }

    public boolean getIWillFix(BugInstance b) {
        return false;
    }

    public String getSourceLinkToolTip(@CheckForNull BugInstance b) {
        return null;
    }

    public URL getSourceLink(BugInstance b) {
        return null;
    }

    public BugFilingStatus getBugLinkStatus(BugInstance b) {
        return null;
    }

    public String getBugStatus(BugInstance b) {
        return null;
    }

    public boolean getWillNotBeFixed(BugInstance b) {
        return false;
    }

    public boolean getBugIsUnassigned(BugInstance b) {
        return false;
    }

    public URL getBugLink(BugInstance b) {
        return null;
    }

    public String getBugLinkType(BugInstance instance) {
        return null;
    }

    public URL fileBug(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    public void setBugLinkOnCloudAndStoreIssueDetails(BugInstance b, String viewUrl, String linkType)
            throws IOException, SignInCancelledException {
        throw new UnsupportedOperationException();
    }

    public void updateBugStatusCache(BugInstance b, String status) {
    }

    public void bugFiled(BugInstance b, @CheckForNull Object bugLink) {
        throw new UnsupportedOperationException();
    }

    public String getCloudReport(BugInstance b) {
        return "";
    }

    public String getCloudReportWithoutMe(BugInstance b) {
        return getCloudReport(b);
    }

    public String claimedBy(BugInstance b) {
        return null;
    }

    public boolean claim(BugInstance b) {
        throw new UnsupportedOperationException();
    }

    public long getUserTimestamp(BugInstance b) {
        return 0;
    }

    public Date getUserDate(BugInstance b) {
        return null;
    }

    public BugDesignation getPrimaryDesignation(BugInstance b) {
        return null;
    }

    public UserDesignation getUserDesignation(BugInstance b) {
        return null;
    }

    public String getUserEvaluation(BugInstance b) {
        return null;
    }

    public double getClassificationScore(BugInstance b) {
        return 0;
    }

    public double getClassificationVariance(BugInstance b) {
        return 0;
    }

    public double getClassificationDisagreement(BugInstance b) {
        return 0;
    }

    public double getPortionObsoleteClassifications(BugInstance b) {
        return 0;
    }

    public int getNumberReviewers(BugInstance b) {
        return b.getXmlProps().getReviewCount();
    }

    public Set<String> getReviewers(BugInstance b) {
        return Collections.emptySet();
    }

    public long getFirstSeen(BugInstance b) {
        long computed = getFirstSeenFromVersion(b);
        Date fromXml = b.getXmlProps().getFirstSeen();
        if (fromXml == null)
            return computed;

        long fromXmlTime = fromXml.getTime();
        if (computed == 0 && fromXmlTime > 0)
            return fromXmlTime;
        else if (fromXmlTime == 0 && computed > 0)
            return computed;

        return Math.min(fromXmlTime, computed);
    }

    public void addDateSeen(BugInstance b, long when) {
        if (when > 0)
          b.getXmlProps().setFirstSeen(new Date(when));
    }

    public long getFirstSeenFromVersion(BugInstance b) {
        long firstVersion = b.getFirstVersion();
        AppVersion v = getBugCollection().getAppVersionFromSequenceNumber(firstVersion);
        if (v == null)
            return getBugCollection().getTimestamp();
        return v.getTimestamp();
    }

    public UserDesignation getConsensusDesignation(BugInstance b) {
        String consensus = b.getXmlProps().getConsensus();
        if (consensus == null)
            return UserDesignation.UNCLASSIFIED;
        try {
            return UserDesignation.valueOf(consensus);
        } catch (IllegalArgumentException e) {
            return UserDesignation.UNCLASSIFIED;
        }
    }

    public boolean overallClassificationIsNotAProblem(BugInstance b) {
        UserDesignation consensusDesignation = getConsensusDesignation(b);
        return consensusDesignation != UserDesignation.UNCLASSIFIED && consensusDesignation.score() < 0;
    }

    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
        return false;
    }

    public void storeUserAnnotation(BugInstance bugInstance) {
        throw new UnsupportedOperationException();
    }

    public boolean communicationInitiated() {
        return false;
    }

    public boolean isInitialized() {
        return true;
    }

   
}
