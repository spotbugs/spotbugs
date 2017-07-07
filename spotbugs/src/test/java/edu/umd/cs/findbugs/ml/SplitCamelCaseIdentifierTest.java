package edu.umd.cs.findbugs.ml;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import edu.umd.cs.findbugs.util.SplitCamelCaseIdentifier;

public class SplitCamelCaseIdentifierTest {

    @Test
    public void testSplitLowerCamelCase() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("displayGUIWindow");
        Collection<String> words = sut.split();
        checkContents(words, "display", "gui", "window");
    }

    @Test
    public void testSplit2() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("DisplayGUIWindow");
        Collection<String> words = sut.split();
        checkContents(words, "display", "gui", "window");
    }

    @Test
    public void testSplitLong() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("nowIsTheWINTEROfOURDiscontent");
        Collection<String> words = sut.split();
        checkContents(words, "now", "is", "the", "winter", "of", "our", "discontent");
    }

    @Test
    public void testAllLower() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("foobar");
        Collection<String> words = sut.split();
        checkContents(words, "foobar");
    }

    @Test
    public void testAllUpper() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("NSA");
        Collection<String> words = sut.split();
        checkContents(words, "nsa");
    }

    @Test
    public void testCapitalized() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("Maryland");
        Collection<String> words = sut.split();
        checkContents(words, "maryland");
    }

    private void checkContents(Collection<String> words, String... expected) {
        Assert.assertEquals(expected.length, words.size());
        for (String anExpected : expected) {
            Assert.assertTrue(words.contains(anExpected));
        }
    }
}
