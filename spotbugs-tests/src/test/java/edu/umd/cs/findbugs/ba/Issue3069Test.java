package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3069Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3069.class",
                "org/rocksdb/AbstractImmutableNativeReference.class",
                "org/rocksdb/AbstractNativeReference.class",
                "org/rocksdb/ReadOptions.class",
                "org/rocksdb/RocksObject.class");

        assertBugInMethodAtLine("OBL_UNSATISFIED_OBLIGATION", "Issue3069", "readOptionsNotClosed", 8);
        assertBugTypeCount("OBL_UNSATISFIED_OBLIGATION", 1);
    }
}
