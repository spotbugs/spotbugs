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

        BugInstance bug1 = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10)
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

        BugInstance bug1 = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment())
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

        BugInstance bug = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10)
                .addClass("SampleClass");

        BugInstance bug2 = new BugInstance(bugPattern2.getType(), bugPattern2.getPriorityAdjustment()).addInt(3)
                .addClass("ExampleClass");

        BugInstance bug3 = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment()).addInt(10)
                .addClass("TestClass");

        BugInstance bug4 = new BugInstance(bugPattern2.getType(), bugPattern2.getPriorityAdjustment()).addInt(10)
                .addClass("SimpleClass");

        BugInstance bug5 = new BugInstance(bugPattern2.getType(), bugPattern2.getPriorityAdjustment()).addInt(10)
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

        BugInstance bug = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment())
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
    void testRuleFullDescriptionHandlesNullDetailText() {
        String type = "TEST_NULL_DETAIL_TYPE";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", null, "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment())
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test that rule handles null detail text properly - null values should be converted to empty strings */
        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test that fullDescription exists and null detail text is handled as empty string */
        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        /* null values should be converted to empty strings */
        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertTrue(fullDescText.isEmpty());
    }

    @Test
    void testRuleFullDescriptionHandlesEmptyDetailText() {
        String type = "TEST_EMPTY_DETAIL_TYPE";

        BugPattern bugPattern = new BugPattern(type, "abbrev", "category", false, "shortDescription",
                "longDescription", "", "https://example.com/help.html", 0);
        DetectorFactoryCollection.instance().registerBugPattern(bugPattern);

        BugCollection bugCollection = new SortedBugCollection();

        BugInstance bug = new BugInstance(bugPattern.getType(), bugPattern.getPriorityAdjustment())
                .addInt(10).addClass("TestClass");

        SourceLineAnnotation lineAnnotation = new SourceLineAnnotation("TestFile", "Test.java", 1, 1, 0, 0);
        bug.addSourceLine(lineAnnotation);

        bugCollection.add(bug);
        bugCollection.bugsPopulated();

        BugCollectionAnalyser analyser = new BugCollectionAnalyser(bugCollection);

        /* test that rule handles empty detail text properly - empty strings should be valid */
        JsonArray rules = analyser.getRules();
        assertThat(rules.size(), is(1));

        JsonObject ruleJson = rules.get(0).getAsJsonObject();
        assertThat(ruleJson.get("id").getAsString(), is(type));

        /* test that fullDescription exists and empty detail text is preserved */
        assertTrue(ruleJson.has("fullDescription"));
        JsonObject fullDescription = ruleJson.get("fullDescription").getAsJsonObject();
        assertTrue(fullDescription.has("text"));

        String fullDescText = fullDescription.get("text").getAsString();

        assertNotNull(fullDescText);
        assertTrue(fullDescText.isEmpty());
    }
}
