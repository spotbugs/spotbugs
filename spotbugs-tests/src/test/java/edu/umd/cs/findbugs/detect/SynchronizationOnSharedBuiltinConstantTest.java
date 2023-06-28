package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class SynchronizationOnSharedBuiltinConstantTest extends AbstractIntegrationTest {

    @Test
    public void lockOn_noncompliantBooleanLockObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_BOOLEAN")
                .inMethod("noncompliantBooleanLockObject")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void lockOn_noncompliantBoxedPrimitive() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE")
                .inMethod("noncompliantBoxedPrimitive")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void lockOn_compliantInteger() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE")
                .inMethod("compliantInteger")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void lockOn_noncompliantInternedStringObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_INTERNED_STRING")
                .inMethod("noncompliantInternedStringObject")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void lockOn_noncompliantStringLiteral() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantBad.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_SHARED_CONSTANT")
                .inMethod("noncompliantStringLiteral")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void lockOn_compliantStringInstance() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_SHARED_CONSTANT")
                .inMethod("compliantStringInstance")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void lockOn_compliantPrivateFinalLockObject() {
        performAnalysis("synchronizationOnSharedBuiltinConstant/SynchronizationOnSharedBuiltinConstantGood.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE")
                .inMethod("compliantPrivateFinalLockObject")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

}
