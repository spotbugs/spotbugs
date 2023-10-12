package edu.umd.cs.findbugs.ml;

import java.util.Collection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.util.SplitCamelCaseIdentifier;

class SplitCamelCaseIdentifierTest {

    @Test
    void testSplitLowerCamelCase() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("displayGUIWindow");
        Collection<String> words = sut.split();
        checkContents(words, "display", "gui", "window");
    }

    @Test
    void testSplit2() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("DisplayGUIWindow");
        Collection<String> words = sut.split();
        checkContents(words, "display", "gui", "window");
    }

    @Test
    void testSplitLong() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("nowIsTheWINTEROfOURDiscontent");
        Collection<String> words = sut.split();
        checkContents(words, "now", "is", "the", "winter", "of", "our", "discontent");
    }

    @Test
    void testAllLower() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("foobar");
        Collection<String> words = sut.split();
        checkContents(words, "foobar");
    }

    @Test
    void testAllUpper() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("NSA");
        Collection<String> words = sut.split();
        checkContents(words, "nsa");
    }

    @Test
    void testCapitalized() {
        SplitCamelCaseIdentifier sut = new SplitCamelCaseIdentifier("Maryland");
        Collection<String> words = sut.split();
        checkContents(words, "maryland");
    }

    private void checkContents(Collection<String> words, String... expected) {
        Assertions.assertEquals(expected.length, words.size());
        for (String anExpected : expected) {
            Assertions.assertTrue(words.contains(anExpected));
        }
    }
}
