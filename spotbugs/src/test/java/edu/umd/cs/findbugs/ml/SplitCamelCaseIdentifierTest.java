package edu.umd.cs.findbugs.ml;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.util.SplitCamelCaseIdentifier;

public class SplitCamelCaseIdentifierTest {

    SplitCamelCaseIdentifier splitter;

    SplitCamelCaseIdentifier splitter2;

    SplitCamelCaseIdentifier splitterLong;

    SplitCamelCaseIdentifier allLower;

    SplitCamelCaseIdentifier allUpper;

    SplitCamelCaseIdentifier capitalized;

    @Before
    public void setUp() {
        splitter = new SplitCamelCaseIdentifier("displayGUIWindow");
        splitter2 = new SplitCamelCaseIdentifier("DisplayGUIWindow");
        splitterLong = new SplitCamelCaseIdentifier("nowIsTheWINTEROfOURDiscontent");
        allLower = new SplitCamelCaseIdentifier("foobar");
        allUpper = new SplitCamelCaseIdentifier("NSA");
        capitalized = new SplitCamelCaseIdentifier("Maryland");
    }

    @Test
    public void testSplit() {
        Collection<String> words = splitter.split();
        checkContents(words, new String[] { "display", "gui", "window" });
    }

    @Test
    public void testSplit2() {
        Collection<String> words = splitter2.split();
        checkContents(words, new String[] { "display", "gui", "window" });
    }

    @Test
    public void testSplitLong() {
        Collection<String> words = splitterLong.split();
        checkContents(words, new String[] { "now", "is", "the", "winter", "of", "our", "discontent" });
    }

    @Test
    public void testAllLower() {
        Collection<String> words = allLower.split();
        checkContents(words, new String[] { "foobar" });
    }

    @Test
    public void testAllUpper() {
        Collection<String> words = allUpper.split();
        checkContents(words, new String[] { "nsa" });
    }

    @Test
    public void testCapitalized() {
        Collection<String> words = capitalized.split();
        checkContents(words, new String[] { "maryland" });
    }

    private void checkContents(Collection<String> words, String[] expected) {
        Assert.assertEquals(expected.length, words.size());
        for (String anExpected : expected) {
            Assert.assertTrue(words.contains(anExpected));
        }
    }
}
