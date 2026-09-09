package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class OverridingMethodMustInvokeSuperTest extends AbstractIntegrationTest {

    @Test
    void testInvokeSuper() {
        performAnalysis(
                "NeedsCallOfSuper.class",
                "NeedsCallOfSuper$DerivedClass.class",
                "NeedsCallOfSuper$ConcreteClass.class",
                "NeedsCallOfSuper$GenericClass.class");
        assertBugTypeCount("OVERRIDING_METHODS_MUST_INVOKE_SUPER", 1);
    }
}
