package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FindBadEndOfStreamCheckTest extends AbstractIntegrationTest {
    private static final String EOS_BUG_TYPE = "EOS_BAD_END_OF_STREAM_CHECK";


    @Test
    void testBadEndOfFileChecks() {
        performAnalysis("endOfStreamCheck/BadEndOfStreamCheck.class");

        assertBugTypeCount(EOS_BUG_TYPE, 16);

        final String className = "BadEndOfStreamCheck";
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream1", 12);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream2", 25);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream3", 39);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream4", 55);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream5", 70);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream6", 83);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream7", 97);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileInputStream8", 113);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader1", 128);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader2", 141);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader3", 155);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader4", 171);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader5", 186);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader6", 199);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader7", 213);
        assertBugInMethodAtLine(EOS_BUG_TYPE, className, "badFileReader8", 229);
    }

    @Test
    void testGoodEndOfFileChecks() {
        performAnalysis("endOfStreamCheck/GoodEndOfStreamCheck.class");

        assertNoBugType(EOS_BUG_TYPE);
    }
}
