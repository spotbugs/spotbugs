package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class LazyInitTest extends AbstractIntegrationTest {

    @Test
    void fieldEqualsNull() {
        performAnalysis("LazyInit.class");
        assertBugTypeCount("LI_LAZY_INIT_STATIC", 2);
        assertBugInMethod("LI_LAZY_INIT_STATIC", "LazyInit", "getFoo");
        assertBugInMethod("LI_LAZY_INIT_STATIC", "LazyInit", "getY");
    }

    /**
     * The Yoda-style guard "null == field" is the same condition as
     * "field == null" and must be reported identically.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a>
     */
    @Test
    void nullEqualsField() {
        performAnalysis("ghIssues/issue4138/LazyInitYoda.class");
        assertBugTypeCount("LI_LAZY_INIT_STATIC", 2);
        assertBugInMethod("LI_LAZY_INIT_STATIC", "LazyInitYoda", "getFoo");
        assertBugInMethod("LI_LAZY_INIT_STATIC", "LazyInitYoda", "getY");
    }
}
