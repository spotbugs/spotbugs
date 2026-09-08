package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3762Test extends AbstractIntegrationTest {

    @Test
    void test() {
        performAnalysis("androidx/lifecycle/LiveData.class",
                "androidx/lifecycle/MutableLiveData.class",
                "ghIssues/Issue3762.class");

        assertBugTypeCount("EI_EXPOSE_REP", 0);
    }
}
