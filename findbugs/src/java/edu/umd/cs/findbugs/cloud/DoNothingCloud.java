package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;

public class DoNothingCloud implements Cloud {
    private CloudPlugin plugin;
    private BugCollection bugCollection;

    private static CloudPlugin getFallbackPlugin() {
        return new CloudPluginBuilder().setCloudid("DoNothingCloud").setDescription("Do Nothing Cloud")
                .setDetails("No comments will be stored.")
                .setClassLoader(BugCollectionStorageCloud.class.getClassLoader())
                .setCloudClass(BugCollectionStorageCloud.class)
                .setUsernameClass(NoNameLookup.class)
                .setProperties(new PropertyBundle())
                .setOnlineStorage(false)
                .createCloudPlugin();
    }

    /** Invoked via reflection */
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
        return "(cloud disabled)";
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
        return false;
    }

    public boolean initialize() {
        return true;
    }

    public void waitUntilNewIssuesUploaded() {
    }

    public void waitUntilIssueDataDownloaded() {
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
        return false;
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
        return 0;
    }

    public Set<String> getReviewers(BugInstance b) {
        return Collections.emptySet();
    }

    public long getFirstSeen(BugInstance b) {
        return 0;
    }

    public UserDesignation getConsensusDesignation(BugInstance b) {
        return null;
    }

    public boolean overallClassificationIsNotAProblem(BugInstance b) {
        return false;
    }

    public boolean canStoreUserAnnotation(BugInstance bugInstance) {
        return false;
    }

    public void storeUserAnnotation(BugInstance bugInstance) {
        throw new UnsupportedOperationException();
    }
}
