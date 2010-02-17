package edu.umd.cs.findbugs.cloud;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugDesignation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.PropertyBundle;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.cloud.Cloud.Mode;
import edu.umd.cs.findbugs.cloud.username.NoNameLookup;
import junit.framework.TestCase;

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

@SuppressWarnings({"ToArrayCallWithZeroLengthArrayArgument"})
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
        plugin = new CloudPlugin("myAbstractCloud", this.getClass().getClassLoader(), MyAbstractCloud.class,
                                 NoNameLookup.class, new PropertyBundle(), "no description", "no details");
        cloud = new MyAbstractCloud(plugin, bugCollection);
        summary = new StringWriter();
		timestampCounter = 0;
	}

	public void testPrintSummaryNoBugs() {
		printSummary();
		assertEquals("No classes were analyzed", trimWhitespace(summary.toString()));
	}

	public void testPrintSummaryOneBugNoEvals() {
		BugInstance bug1 = new BugInstance("BUG_1", 2);
		bug1.addClass("edu.umd.Test");
		assertEquals(lines(
				"Code analyzed",
				"      1 packages",
				"      1 classes",
				"      1 thousands of lines of non-commenting source statements",
				"",
				"Summary for 1 issues that are in the current view",
				"",
				"No evaluations found",
				"",
				"No bugs filed",
				"",
				"Distribution of number of reviews",
				"   1  with   0 reviews"), printSummary(bug1));
	}

	public void testPrintSummaryWithEvaluations() {
		BugInstance bug1 = createBug("BUG_1", "I_WILL_FIX", "user1");
		BugInstance bug2 = createBug("BUG_2", "NOT_A_BUG", "user");
		BugInstance bug3 = createBug("BUG_3", "I_WILL_FIX", "user");
		
		assertEquals(lines(
				"Code analyzed",
				"      1 packages",
				"      3 classes",
				"      1 thousands of lines of non-commenting source statements",
				"",
				"Summary for 3 issues that are in the current view",
				"",
				"People who have performed the most reviews",
				"rnk  num reviewer",
				"  1    2 user",
				"  2    1 user1",
				"",
				"Distribution of evaluations",
				" num designation",
				"   2 I will fix",
				"   1 not a bug",
				"",
				"No bugs filed",
				"",
				"Distribution of number of reviews",
				"   3  with   1 review"), 
				
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
		
		assertEquals(lines(
				"Code analyzed",
				"      1 packages",
				"     67 classes",
				"      7 thousands of lines of non-commenting source statements",
				"",
				"Summary for 67 issues that are in the current view",
				"",
				"People who have performed the most reviews",
				"rnk  num reviewer",
				"  1   11 user11",
				"  2   10 user10",
				"  3    9 user9",
				"  4    8 user8",
				"  5    7 user7",
				"  6    6 user6",
				"  7    5 user5",
				"  8    4 user4",
				"  9    3 user3",
				" 11    1 user",
				"Total of 12 reviewers",
				"",
				"Distribution of evaluations",
				" num designation",
				"  66 I will fix",
				"   1 not a bug",
				"",
				"No bugs filed",
				"",
				"Distribution of number of reviews",
				"  67  with   1 review"), 
				
				printSummary(bugs.toArray(new BugInstance[0])));
	}
	

	public void testPrintSummaryWithMultipleEvaluationsPerBug() {
		List<BugInstance> bugs = new ArrayList<BugInstance>();
		bugs.add(createBug("MY_SPECIAL_BUG1", "NOT_A_BUG", "user3"));
		bugs.add(createBug("MY_SPECIAL_BUG2", "NOT_A_BUG", "user3", "I_WILL_FIX", "user2"));
		bugs.add(createBug("MY_SPECIAL_BUG3", "NOT_A_BUG", "user3", "I_WILL_FIX", "user2", "MOSTLY_HARMLESS", "user1"));
		
		assertEquals(lines(
				"Code analyzed",
				"      1 packages",
				"      3 classes",
				"      1 thousands of lines of non-commenting source statements",
				"",
				"Summary for 3 issues that are in the current view",
				"",
				"People who have performed the most reviews",
				"rnk  num reviewer",
				"  1    3 user3",
				"  2    2 user2",
				"  3    1 user1",
				"",
				"Distribution of evaluations",
				" num designation",
				"   3 not a bug",
				"   2 I will fix",
				"   1 mostly harmless",
				"",
				"No bugs filed",
				"",
				"Distribution of number of reviews",
				"   1  with   1 review",
				"   1  with   2 reviews",
				"   1  with   3 reviews"), 
				
				printSummary(bugs.toArray(new BugInstance[0])));
	}

    public void testVotingModePropertyNull() {
        checkVotingMode(Mode.COMMUNAL, null);
    }

    public void testVotingModePropertyInvalid() {
        checkVotingMode(Mode.COMMUNAL, "garbage");
    }

    public void testVotingModeProperty() {
        checkVotingMode(Mode.SECRET, "SECRET");
        checkVotingMode(Mode.COMMUNAL, "COMMUNAL");
    }

    public void testSourceLinkPatternNotSpecified() {
        initializeSourceLinks("edu/umd/(.*)", null, null);
        checkSourceLink(null, "some.other.SomeClass", 0);
    }

    public void testSourceLinkPatternInvalid() {
        initializeSourceLinks("edu/umd/(.*", "http://x.y.z/%s", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkFormatInvalid() {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%M", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkURLInvalid() {
        initializeSourceLinks("edu/umd/(.*)", "://x.y.z/%s", null);
        checkSourceLink(null, "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkWithoutLineNumber() {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        checkSourceLink("http://x.y.z/cs/SomeClass.java", "edu.umd.cs.SomeClass", 0);
    }

    public void testSourceLinkWithLineNumber() {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", "http://x.y.z/%s#%d,%d");
        checkSourceLink("http://x.y.z/cs/SomeClass.java#100,90", "edu.umd.cs.SomeClass", 100);
    }

    public void testSourceLinkNoMatch() {
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        checkSourceLink(null, "some.other.SomeClass", 0);
    }

    public void testSourceLinkTooltip() {
        plugin.getProperties().setProperty("findbugs.sourcelink.tooltip", "View source link");
        initializeSourceLinks("edu/umd/(.*)", "http://x.y.z/%s", null);
        assertEquals("View source link", cloud.getSourceLinkToolTip(createBug("HI")));
    }
	
	// ============================= end of tests =============================

    private void checkSourceLink(String expectedLink, String className, int startLine) {
        BugInstance bug = createBug("ABBREV");
        bug.addSourceLine(new SourceLineAnnotation(className, "SomeClass.java", startLine, startLine, 0,0));
        URL sourceLink = cloud.getSourceLink(bug);
        if (expectedLink == null) {
            assertNull(sourceLink);
        } else {
            assertNotNull(sourceLink);
            assertEquals(expectedLink, sourceLink.toExternalForm());
        }
    }

    private void checkVotingMode(Mode expectedMode, String modeString) {
        if (modeString == null)
            assertNull(plugin.getProperties().getProperty("findbugs.cloud.votingmode"));
        else
            plugin.getProperties().setProperty("findbugs.cloud.votingmode", modeString);
        cloud.initialize();
        assertEquals(expectedMode, cloud.getMode());
    }

    private void initializeSourceLinks(String pattern, String format, String formatWithLine) {
        plugin.getProperties().setProperty("findbugs.sourcelink.pattern", pattern);
        if (format != null)
            plugin.getProperties().setProperty("findbugs.sourcelink.format", format);
        if (formatWithLine != null)
            plugin.getProperties().setProperty("findbugs.sourcelink.formatWithLine", formatWithLine);
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
			String user = designationUserPairs[i+1];
			designations.add(new BugDesignation(designation, timestampCounter ++, "my comment", user));
		}
		cloud.designations.put(bug, designations);
    	return bug;
    }

	private String lines(String... lines) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (String string : lines) {
	        if (!first) builder.append("\n");
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
			projectStats.addBug(bug);
        }

	    cloud.printCloudSummary(new PrintWriter(summary), Arrays.asList(bugs), new String[0]);
	    return trimWhitespace(summary.toString());
    }

	private String trimWhitespace(String string) {
	    return string.replaceAll("^\\s+|\\s+$", "").replace("\r", "");
    }

    private final class MyAbstractCloud extends AbstractCloud {
	    private final Map<BugInstance, List<BugDesignation>> designations = new HashMap<BugInstance, List<BugDesignation>>();

	    private MyAbstractCloud(CloudPlugin plugin, BugCollection bugs) {
		    super(plugin, bugs);
	    }

	    public void storeUserAnnotation(BugInstance bugInstance) {
	    	throw new UnsupportedOperationException();
	    }

	    public String getUser() {
	    	return "user";
	    }

	    public BugDesignation getPrimaryDesignation(BugInstance b) {
	    	throw new UnsupportedOperationException();
	    }

	    public void bugsPopulated() {
	    	throw new UnsupportedOperationException();
	    }

	    public void bugFiled(BugInstance b, Object bugLink) {
	    	throw new UnsupportedOperationException();
	    }

	    public boolean availableForInitialization() {
	    	throw new UnsupportedOperationException();
	    }

	    @Override
	    protected Iterable<BugDesignation> getAllUserDesignations(BugInstance bd) {
	    	List<BugDesignation> designationList = designations.get(bd);
			return designationList != null ? designationList : Collections.<BugDesignation>emptyList();
	    }

        public Collection<String> getProjects(String className) {
            return Collections.emptyList();
        }
    }
}
