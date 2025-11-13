package edu.umd.cs.findbugs.sarif;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;

class BugCollectionAnalyserTest {

    @Test
    void testGetRulesGetResultsGetCweTaxonomyNoBug() {
        BugCollection bugCollection = new SortedBugCollection();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test taxonomy */
        assertNull(analyser.getCweTaxonomy());

        /* test rules */
        assertThat(analyser.getRules().size(), is(0));

        /* test result */
        assertThat(analyser.getResults().size(), is(0));
    }

    @Test
    void testGetRulesGetResultsGetCweTaxonomyOneBugWithCwe() {
        int cweid = 78;
        String type = "TYPE";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription", "longDescription",
                "detailText", "https://example.com/help.html", cweid);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug1 = new BugInstance(bugPattern.getType(), 0).addInt(10)
                .addClass("SampleClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("SimpleTest", "Test.java", 1, 3, 0, 0);
        bug1.addSourceLine(lineAnnotation);

        bugCollection.add(bug1);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test taxonomy */
        JsonObject taxonomyJson = analyser.getCweTaxonomy();
        JsonArray taxaJson = taxonomyJson.get("taxa").getAsJsonArray();

        assertThat(taxonomyJson.get("name").getAsString(), is("CWE"));

        JsonObject taxonJson = taxaJson.get(0).getAsJsonObject();
        assertThat(taxonJson.get("id").getAsString(), is(String.valueOf(cweid)));
        assertThat(taxonJson.get("defaultConfiguration").getAsJsonObject().get("level").getAsString(), is("error"));

        /* test rules */
        JsonObject ruleJson = analyser.getRules().get(0).getAsJsonObject();
        JsonObject cweRelationshipJson = ruleJson.get("relationships").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject target = cweRelationshipJson.get("target").getAsJsonObject();

        assertThat(ruleJson.get("id").getAsString(), is(type));
        assertThat(target.get("id").getAsString(), is(String.valueOf(cweid)));
        assertThat(cweRelationshipJson.get("kinds").getAsJsonArray().get(0).getAsString(), is("superset"));

        /* test result */
        JsonObject resultJson = analyser.getResults().get(0).getAsJsonObject();

        assertThat(resultJson.get("ruleId").getAsString(), is(type));
    }

    @Test
    void testGetRulesGetResultsGetCweTaxonomyOneBugNoCweId() {
        String type = "TYPE";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", "detailText", "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug1 = new BugInstance(bugPattern.getType(), 0)
                .addInt(10)
                .addClass("SampleClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("SimpleTest", "Test.java", 1, 3, 0, 0);
        bug1.addSourceLine(lineAnnotation);

        bugCollection.add(bug1);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test taxonomy */
        assertNull(analyser.getCweTaxonomy());

        /* test rules */
        JsonObject ruleJson = analyser.getRules().get(0).getAsJsonObject();

        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test result */
        JsonObject resultJson = analyser.getResults().get(0).getAsJsonObject();

        assertThat(resultJson.get("ruleId").getAsString(), is(type));
    }

    @Test
    void testGetRulesGetResultsGetCweTaxonomyMultipleBugsWithAndWithoutCweId() {

        BugPattern bugPattern = new BugPattern("TYPE_NO_CWE", "abbrev", "category", false, "shortDescription",
                "longDescription", "detailText", "https://example.com/help.html", 0);
        BugPattern bugPattern2 = new BugPattern("TYPE_WITH_CWE", "abbrev", "category", false, "shortDescription",
                "longDescription",
                "detailText", "https://example.com/help.html", 0);

        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern2);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), 0).addInt(10)
                .addClass("SampleClass");

        BugInstance bug2 = new BugInstance(bugPattern2.getType(), 0).addInt(3)
                .addClass("ExampleClass");

        BugInstance bug3 = new BugInstance(bugPattern.getType(), 0).addInt(10)
                .addClass("TestClass");

        BugInstance bug4 = new BugInstance(bugPattern2.getType(), 0).addInt(10)
                .addClass("SimpleClass");

        BugInstance bug5 = new BugInstance(bugPattern2.getType(), 0).addInt(10)
                .addClass("NoClass");

        bug.addSourceLine(new SourceLineAnnotation("SimpleTest", "Test.java", 1, 3, 0, 0));
        bug2.addSourceLine(new SourceLineAnnotation("SimpleTest2", "Test2.java", 56, 3, 0, 0));
        bug3.addSourceLine(new SourceLineAnnotation("SimpleTest3", "Test3.java", 0, 0, 367, 956));
        bug4.addSourceLine(new SourceLineAnnotation("SimpleTest", "Test4.java", 4389, 47, 0, 0));
        bug5.addSourceLine(new SourceLineAnnotation("SimpleTest", "Test5.java", 0, 0, 1, 3));

        bugCollection.add(bug);
        bugCollection.add(bug2);
        bugCollection.add(bug3);
        bugCollection.add(bug4);
        bugCollection.add(bug5);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test taxonomy */
        assertNull(analyser.getCweTaxonomy());

        /* test rules */
        assertThat(analyser.getRules().size(), is(2));

        /* test result */
        assertThat(analyser.getResults().size(), is(5));
    }

    @Test
    void testRuleContainsFullDescriptionFromDetailText() {
        String type = "TEST_FULL_DESC_TYPE";
        String detailText = "This is a test detail text with HTML markup.";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", detailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test that rule contains fullDescription with text */
        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test that fullDescription exists and contains the detail text */
        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(detailText));
    }

    @Test
    void testRuleFullDescriptionHandlesEmptyDetailText() {
        String type = "TEST_EMPTY_DETAIL_TYPE";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", "", "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test that rule handles empty detail text properly - empty strings should get template text */
        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test that fullDescription exists and empty detail text gets template text */
        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is("No detailed description available for this bug pattern."));
    }

    @Test
    void testRuleFullDescriptionHandlesHtmlDetailText() {
        String type = "TEST_HTML_DETAIL_TYPE";
        String htmlDetailText = "<p>This is a <strong>test</strong> with <em>HTML</em> markup.</p>";
        String expectedPlainText = "This is a test with HTML markup.";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", htmlDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test that rule converts HTML detail text to plain text */
        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test that fullDescription exists and HTML is converted to plain text */
        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesCdataSection() {
        String type = "TEST_CDATA_TYPE";
        String cdataDetailText = "<![CDATA[\n<p>\nThis plugin contains all of the standard SpotBugs detectors.\n</p>\n]]>";
        String expectedPlainText = "This plugin contains all of the standard SpotBugs detectors. ]]>";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", cdataDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesCodeBlocks() {
        String type = "TEST_CODE_BLOCK_TYPE";
        String codeDetailText =
                "<p>Example code:</p><pre><code>public void foo() {\n    int x = 3;\n    x = x;\n}</code></pre><p>Such assignments are useless.</p>";
        String expectedPlainText = "Example code: \npublic void foo() {\n    int x = 3;\n    x = x;\n}\nSuch assignments are useless.";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", codeDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesTable() {
        String type = "TEST_TABLE_TYPE";
        String tableDetailText = "<p>Mapping of corresponding Java System properties:</p>" +
                "<table>" +
                "<tr><th>Environment variable</th><th>Property</th></tr>" +
                "<tr><td>JAVA_HOME</td><td>java.home</td></tr>" +
                "<tr><td>USER</td><td>user.name</td></tr>" +
                "</table>";
        String expectedPlainText =
                "Mapping of corresponding Java System properties: \nEnvironment variable Property JAVA_HOME java.home USER user.name";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", tableDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesListsAndLinks() {
        String type = "TEST_LIST_LINK_TYPE";
        String listDetailText = "<p>A few key points to keep in mind:</p>" +
                "<ul>" +
                "<li>Use meaningful prefixes or namespaces</li>" +
                "<li>Use descriptive names: <a href=\"https://example.com\">see documentation</a></li>" +
                "<li>Follow naming conventions</li>" +
                "</ul>";
        String expectedPlainText =
                "A few key points to keep in mind: \n  * Use meaningful prefixes or namespaces \n  * Use descriptive names: see documentation \n  * Follow naming conventions";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", listDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesComplexNestedHtml() {
        String type = "TEST_COMPLEX_HTML_TYPE";
        String complexDetailText =
                "<p>This code creates a <code>java.util.Random</code> object, uses it to generate one random number, and then discards " +
                        "the Random object. This produces <em>mediocre quality</em> random numbers and is <strong>inefficient</strong>.</p>" +
                        "<p>If it is important that the generated Random numbers not be guessable, you <em>must</em> not create a new Random for each random "
                        +
                        "number; the values are too easily guessable. You should strongly consider using a <code>java.security.SecureRandom</code> instead.</p>";
        String expectedPlainText =
                "This code creates a java.util.Random object, uses it to generate one random \nnumber, and then discards the Random object. This produces mediocre quality \nrandom numbers and is inefficient. \nIf it is important that the generated Random numbers not be guessable, you must \nnot create a new Random for each random number; the values are too easily \nguessable. You should strongly consider using a java.security.SecureRandom \ninstead.";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", complexDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }

    @Test
    void testRuleFullDescriptionHandlesBlockquoteAndBr() {
        String type = "TEST_BLOCKQUOTE_BR_TYPE";
        String blockquoteDetailText = "<p>From the JavaDoc for the compareTo method in the Comparable interface:</p>" +
                "<blockquote>" +
                "It is strongly recommended, but not strictly required that <code>(x.compareTo(y)==0) == (x.equals(y))</code>.<br/>" +
                "Generally speaking, any class that implements the Comparable interface and violates this condition<br>" +
                "should clearly indicate this fact." +
                "</blockquote>";
        String expectedPlainText =
                "From the JavaDoc for the compareTo method in the Comparable interface: \nIt is strongly recommended, but not strictly required that (x.compareTo(y)==0) \n== (x.equals(y)).Generally speaking, any class that implements the Comparable \ninterface and violates this conditionshould clearly indicate this fact.";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", blockquoteDetailText, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), Priorities.NORMAL_PRIORITY)
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertThat(fullDescText, is(expectedPlainText));
    }
}
