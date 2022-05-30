package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class BadVisibilityOnSharedPrimitiveVariablesTest extends AbstractIntegrationTest {

    @Test
    public void failurePath_fieldWithBadVisibility_whenOtherMethodHasSynchronizedBlock() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlockAndBadVisibilityOnField.class");

        assertSPVNumOfBugs(1);
        assertSPVBug("SynchronizedBlockAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenOtherMethodIsSynchronized() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethodAndBadVisibilityOnField.class");

        assertSPVNumOfBugs(1);
        assertSPVBug("SynchronizedMethodAndBadVisibilityOnField", "shutdown", 17);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenClassExtendsThread() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityThread.class");

        assertSPVNumOfBugs(1);
        assertSPVBug("FieldWithBadVisibilityThread", "shutdown", 18);
    }

    @Test
    public void failurePath_fieldWithBadVisibility_whenClassImplementsRunnable() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/FieldWithBadVisibilityRunnable.class");

        assertSPVNumOfBugs(1);
        assertSPVBug("FieldWithBadVisibilityRunnable", "shutdown", 18);
    }

    @Test
    public void happyPath_atomicField() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/AtomicField.class");

        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_volatileField() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/VolatileField.class");

        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_synchronizedBlock() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedBlock.class");

        assertSPVNumOfBugs(0);
    }

    @Test
    public void happyPath_synchronizedMethod() {
        SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("multithreaded/sharedPrimitiveVariables/SynchronizedMethod.class");

        assertSPVNumOfBugs(0);
    }

    private void assertSPVNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertSPVBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SPV_BAD_VISIBILITY_ON_SHARED_PRIMITIVE_VARIABLES")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
