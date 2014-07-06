package edu.umd.cs.findbugs.cloud;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.cloud.Cloud.Mode;
import edu.umd.cs.findbugs.cloud.Cloud.UserDesignation;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;

@SuppressWarnings({ "ToArrayCallWithZeroLengthArrayArgument" })
public class AbstractCloudTest extends TestCase {

    private BugCollection bugCollection;

    private MyAbstractCloud cloud;

    private StringWriter summary;

    private ProjectStats projectStats;

    private int timestampCounter;

    private CloudPlugin plugin;

    @Override
    public void setUp() {
        projectStats = new ProjectStats();
        bugCollection = new SortedBugCollection(projectStats);
        plugin = new CloudPluginBuilder().setCloudid("myAbstractCloud").setClassLoader(this.getClass().getClassLoader())
                .setCloudClass(MyAbstractCloud.class).setUsernameClass(NoNameLookup.class).setProperties(new PropertyBundle())
                .setDescription("no description").setDetails("no details").createCloudPlugin();
        cloud = new MyAbstractCloud(plugin, bugCollection, new Properties());
        summary = new StringWriter();
        timestampCounter = 0;
    }

    public void testCountReviewersTwo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1", "NOT_A_BUG", "user2", "NOT_A_BUG", "user1");
        assertEquals(2, cloud.getNumberReviewers(bug1));
    }

    public void testCountReviewersNone() throws Exception {
        BugInstance bug1 = createBug("BUG_1");
        assertEquals(0, cloud.getNumberReviewers(bug1));
    }

    public void testOverallClassificationNotAProblemNoEvals() throws Exception {
        BugInstance bug1 = createBug("BUG_1");
        assertEquals(UserDesignation.UNCLASSIFIED, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemNeedsStudy() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "NEEDS_STUDY", "user1");
        assertEquals(UserDesignation.NEEDS_STUDY, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemYes() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "MUST_FIX", "user2");
        assertEquals(UserDesignation.MUST_FIX, cloud.getConsensusDesignation(bug1));

    }

    public void testOverallClassificationNotAProblemUnanimousYes() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1", "MUST_FIX", "user2");
        assertEquals(UserDesignation.MUST_FIX, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemYesAndNo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1", "NOT_A_BUG", "user2");
        assertEquals(UserDesignation.NEEDS_STUDY, cloud.getConsensusDesignation(bug1));

    }

    public void testOverallClassificationNotAProblemNo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "NOT_A_BUG", "user2");
        assertEquals(UserDesignation.NOT_A_BUG, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemUnanimousNo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "BAD_ANALYSIS", "user1", "NOT_A_BUG", "user2");
        assertEquals(UserDesignation.NOT_A_BUG, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemMostlyNo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "BAD_ANALYSIS", "user1", "MUST_FIX", "user2", "NOT_A_BUG", "user3");
        assertTrue(cloud.getConsensusDesignation(bug1).score() < 0);
    }

    public void testOverallClassificationNotAProblemMostlyYes() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "BAD_ANALYSIS", "user1", "MUST_FIX", "user2", "SHOULD_FIX", "user3");
        assertTrue(cloud.getConsensusDesignation(bug1).score() > 0);
    }

    public void testOverallClassificationNotAProblemChangedMindNo() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1", "MUST_FIX", "user1", "NOT_A_BUG", "user1");
        assertEquals(UserDesignation.NOT_A_BUG, cloud.getConsensusDesignation(bug1));
    }

    public void testOverallClassificationNotAProblemChangedMindYes() throws Exception {
        BugInstance bug1 = createBug("BUG_1", "NOT_A_BUG", "user1", "MUST_FIX", "user1", "I_WILL_FIX", "user1");
        assertEquals(UserDesignation.MUST_FIX, cloud.getConsensusDesignation(bug1));

    }

    public void testPrintSummaryNoBugs() {
        printSummary();
        assertEquals("No classes were analyzed", trimWhitespace(summary.toString()));
    }

    public void assertEqualText(String[] expectedLines, String actual) {
        String actualLines[] = actual.split("\n");
        for (int i = 0; i < expectedLines.length && i < actualLines.length; i++) {
            assertEquals("line " + (i + 1), expectedLines[i].trim(), actualLines[i].trim());
        }
        int diff = actualLines.length - expectedLines.length;
        if (diff < 0) {
            fail((-diff) + " more lines than expected "
                    + Arrays.asList(actualLines).subList(expectedLines.length, actualLines.length));
        } else if (diff > 0) {
            fail((diff) + " missing lines " + Arrays.asList(expectedLines).subList(actualLines.length, expectedLines.length));
        }
    }

    public void testPrintSummaryOneBugNoEvals() {
        BugInstance bug1 = new BugInstance("BUG_1", 2);
        bug1.addClass("edu.umd.Test");
        assertEqualText(new String[] { "Code analyzed", "      1 packages", "      1 classes",
                "      1 thousands of lines of non-commenting source statements", "", "Summary for 1 issues", "",
                "No reviews found", "",
                // "No bugs filed",
                // "",
                "Distribution of number of reviews", "   1  with   0 reviews" }, printSummary(bug1));
    }

    public void testPrintSummaryWithEvaluations() {
        BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1");
        BugInstance bug2 = createBug("BUG_2", "NOT_A_BUG", "user");
        BugInstance bug3 = createBug("BUG_3", "I_WILL_FIX", "user");

        assertEqualText(new String[] { "Code analyzed", "      1 packages", "      3 classes",
                "      1 thousands of lines of non-commenting source statements", "", "Summary for 3 issues", "",
                "People who have performed the most reviews", "rnk  num reviewer", "  1    2 user", "  2    1 user1", "",
                "Distribution of reviews", " num designation", "   2 I will fix", "   1 not a bug", "",
                "Distribution of number of reviews", "   3  with   1 review" },

                printSummary(bug1, bug2, bug3));
    }

    public void testPrintSummaryWithMoreThan9Reviewers() {
        List<BugInstance> bugs = new ArrayList<BugInstance>();
        bugs.add(createBug("MY_SPECIAL_BUG", "NOT_A_BUG", "user"));
        for (int i = 1; i <= 11; i++) {
            // user1 reviews 1 bug, user2 reviews 2 bugs, etc
            for (int j = 0; j < i; j++) {
                bugs.add(createBug("BUG_" + i + "_" + j, "I_WILL_FIX", "user" + i));
            }
        }

        assertEqualText(new String[] { "Code analyzed", "      1 packages", "     67 classes",
                "      7 thousands of lines of non-commenting source statements", "", "Summary for 67 issues", "",
                "People who have performed the most reviews", "rnk  num reviewer", "  1   11 user11", "  2   10 user10",
                "  3    9 user9", "  4    8 user8", "  5    7 user7", "  6    6 user6", "  7    5 user5", "  8    4 user4",
                "  9    3 user3", " 11    1 user", "Total of 12 reviewers", "", "Distribution of reviews",
                " num designation", "  66 I will fix", "   1 not a bug", "", "Distribution of number of reviews",
        "  67  with   1 review" },

        printSummary(bugs.toArray(new BugInstance[0])));
    }

    public void testPrintSummaryWithMultipleEvaluationsPerBug() {
        List<BugInstance> bugs = new ArrayList<BugInstance>();
        bugs.add(createBug("MY_SPECIAL_BUG1", "NOT_A_BUG", "user3"));
        bugs.add(createBug("MY_SPECIAL_BUG2", "NOT_A_BUG", "user3", "I_WILL_FIX", "user2"));
        bugs.add(createBug("MY_SPECIAL_BUG3", "NOT_A_BUG", "user3", "I_WILL_FIX", "user2", "MOSTLY_HARMLESS", "user1"));

        assertEqualText(new String[] { "Code analyzed", "      1 packages", "      3 classes",
                "      1 thousands of lines of non-commenting source statements", "", "Summary for 3 issues", "",
                "People who have performed the most reviews", "rnk  num reviewer", "  1    3 user3", "  2    2 user2",
                "  3    1 user1", "", "Distribution of reviews", " num designation", "   3 not a bug", "   2 I will fix",
                "   1 mostly harmless", "", "Distribution of number of reviews", "   1  with   1 review",
                "   1  with   2 reviews", "   1  with   3 reviews" },

                printSummary(bugs.toArray(new BugInstance[0])));
    }

    public void testVotingModePropertyNull() throws Exception {
        checkVotingMode(Mode.COMMUNAL, null);
    }

    public void testVotingModePropertyInvalid() throws Exception {
        checkVotingMode(Mode.COMMUNAL, "garbage");
    }

    public void testVotingModeProperty() throws Exception {
        checkVotingMode(Mode.SECRET, "SECRET");
        checkVotingMode(Mode.COMMUNAL, "COMMUNAL");
    }

    public void testSourceLinkPatternNotSpecified() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", null, null);
        checkSourceLink(null, "some.other.SomeClass", 0);
    }

    public void testSourceLinkPatternInvalid() throws Exception {
        initializeSourceLinks("edu/umd/(.*", "http://x.y.z/%s", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkFormatInvalid() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%M", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkURLInvalid() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", "://x.y.z/%s", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkWithoutLineNumber() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        checkSourceLink("http://x.y.z/cs/SomeClass.java", "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkWithLineNumber() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", "http://x.y.z/%s#%d,%d");
        checkSourceLink("http://x.y.z/cs/SomeClass.java#100,90", "edu.umd.cs.SomeClass", 100);
    }

    public void testSourceLinkNoMatch() throws Exception {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        checkSourceLink(null, "some.other.SomeClass", 0);
    }

    public void testSourceLinkTooltip() throws Exception {
        plugin.getProperties().setProperty("findbugs.sourcelink.tooltip", "View source link");
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        assertEquals("View source link", cloud.getSourceLinkToolTip(createBug("HI")));
    }

    // ============================= end of tests =============================

    private void checkSourceLink(String expectedLink, String className, int startLine) {
        BugInstance bug = createBug("ABBREV");
        bug.addSourceLine(new SourceLineAnnotation(className, "SomeClass.java", startLine, startLine, 0, 0));
        URL sourceLink = cloud.getSourceLink(bug);
        if (expectedLink == null) {
            assertNull(sourceLink);
        } else {
            assertNotNull(sourceLink);
            assertEquals(expectedLink, sourceLink.toExternalForm());
        }
    }

    private void checkVotingMode(Mode expectedMode, String modeString) throws IOException {
        if (modeString == null) {
            plugin.getProperties().getProperties().remove("findbugs.cloud.votingmode");
        } else {
            plugin.getProperties().setProperty("findbugs.cloud.votingmode", modeString);
        }
        cloud = new MyAbstractCloud(plugin, bugCollection, new Properties());
        cloud.initialize();
        assertEquals(expectedMode, cloud.getMode());
    }

    private void initializeSourceLinks(String pattern, String format, String formatWithLine) throws IOException {
        plugin.getProperties().setProperty("findbugs.sourcelink.pattern", pattern);
        if (format != null) {
            plugin.getProperties().setProperty("findbugs.sourcelink.format", format);
        }
        if (formatWithLine != null) {
            plugin.getProperties().setProperty("findbugs.sourcelink.formatWithLine", formatWithLine);
        }
        cloud = new MyAbstractCloud(plugin, bugCollection, new Properties());
        cloud.initialize();
    }

    private BugInstance createBug(String abbrev, String... designationUserPairs) {
        assertTrue(designationUserPairs.length % 2 == 0);

        BugInstance bug = new BugInstance(abbrev, 2);
        bug.setInstanceHash(abbrev);
        bug.addClass("edu.umd.Test" + abbrev);
        List<BugDesignation> designations = new ArrayList<BugDesignation>();
        for (int i = 0; i < designationUserPairs.length; i += 2) {
            String designation = designationUserPairs[i];
            String user = designationUserPairs[i + 1];
            designations.add(new BugDesignation(designation, timestampCounter++, "my comment", user));
        }
        cloud.designations.put(bug, designations);
        return bug;
    }

    private String lines(String... lines) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String string : lines) {
            if (!first) {
                builder.append("\n");
            }
            builder.append(string);
            first = false;
        }
        return builder.toString();
    }

    private String printSummary(BugInstance... bugs) {
        for (BugInstance bug : bugs) {
            bugCollection.add(bug);
            ClassAnnotation cls = bug.getPrimaryClass();
            projectStats.addClass(cls.getClassName(), cls.getSourceFileName(), false, 100);
        }

        cloud.printCloudSummary(new PrintWriter(summary), Arrays.asList(bugs), new String[0]);
        return trimWhitespace(summary.toString());
    }

    private String trimWhitespace(String string) {
        return string.replaceAll("^\\s+|\\s+$", "").replace("\r", "");
    }

    private final class MyAbstractCloud extends AbstractCloud {
        private final Map<BugInstance, List<BugDesignation>> designations = new HashMap<BugInstance, List<BugDesignation>>();

        private MyAbstractCloud(CloudPlugin plugin, BugCollection bugs, Properties properties) {
            super(plugin, bugs, properties);
        }

        @Override
        public void storeUserAnnotation(BugInstance bugInstance) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getUser() {
            return "user";
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
        public void signIn() {
        }

        @Override
        public void signOut() {
        }

        @Override
        public BugDesignation getPrimaryDesignation(BugInstance b) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void bugsPopulated() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void initiateCommunication() {

        }

        @Override
        public void bugFiled(BugInstance b, Object bugLink) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean availableForInitialization() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void waitUntilIssueDataDownloaded() {
        }
        @Override
        public boolean waitUntilNewIssuesUploaded(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public void waitUntilNewIssuesUploaded() {

        }
        @Override
        public boolean waitUntilIssueDataDownloaded(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public Collection<String> getProjects(String className) {
            return Collections.emptyList();
        }

        @Override
        public boolean isInCloud(BugInstance b) {
            return true;
        }

        @Override
        public boolean isOnlineCloud() {
            return false;
        }

        @Override
        public String getCloudName() {
            return "test";
        }

        public Iterable<UserDesignation> getDesignations(BugInstance b) {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.cloud.Cloud#fileBug(edu.umd.cs.findbugs.BugInstance
         * , ProtoClasses.BugLinkType)
         */
        @Override
        public URL fileBug(BugInstance bug) {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.cloud.Cloud#getBugIsUnassigned(edu.umd.cs.findbugs
         * .BugInstance)
         */
        @Override
        public boolean getBugIsUnassigned(BugInstance b) {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.cloud.Cloud#getReviewers(edu.umd.cs.findbugs.
         * BugInstance)
         */
        @Override
        public Set<String> getReviewers(BugInstance b) {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.cloud.Cloud#getWillNotBeFixed(edu.umd.cs.findbugs
         * .BugInstance)
         */
        @Override
        public boolean getWillNotBeFixed(BugInstance b) {
            throw new UnsupportedOperationException();
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * edu.umd.cs.findbugs.cloud.AbstractCloud#getLatestDesignationFromEachUser
         * (edu.umd.cs.findbugs.BugInstance)
         */
        @Override
        protected Iterable<BugDesignation> getLatestDesignationFromEachUser(BugInstance b) {
            Map<String, BugDesignation> map = new HashMap<String, BugDesignation>();
            List<BugDesignation> list = designations.get(b);
            if (list == null) {
                return Collections.emptyList();
            }
            for (BugDesignation bd : list) {
                BugDesignation old = map.get(bd.getUser());
                if (old == null || old.getTimestamp() < bd.getTimestamp()) {
                    map.put(bd.getUser(), bd);
                }
            }
            return map.values();
        }



    }
}
