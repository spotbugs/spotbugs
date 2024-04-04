package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class ResourceInMultipleThreadsDetectorTest extends AbstractIntegrationTest {

    @Test
    void testSafeUsages() {
        performAnalysis("commonResources/SafeAtomicFieldUsage.class",
                "commonResources/SafeSynchronizedCollectionUsage.class",
                "commonResources/SafeFieldUsages.class",
                "commonResources/SynchronizedSafeFieldUsage.class",
                "commonResources/SafeFieldGetterUsage.class",
                "commonResources/SafeBuilderPattern.class",
                "commonResources/SafePutFieldWithBuilderPattern.class",
                "commonResources/SafeFieldUsageInMainThread.class",
                "commonResources/CombinedThreadsInShutdownHook.class",
                "commonResources/CombinedThreadsInShutdownHook$1.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertNumOfBugs(0);
    }

    @Test
    void testUnsafeFieldUsages() {
        performAnalysis("commonResources/UnsafeFieldUsage.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertNumOfBugs(5);
        assertBug("UnsafeFieldUsage", "lambda$new$0", 8);
        assertBug("UnsafeFieldUsage", "createVehicle", 16);
        assertBug("UnsafeFieldUsage", "createVehicle", 17);
        assertBug("UnsafeFieldUsage", "createVehicle", 18);
        assertBug("UnsafeFieldUsage", "createVehicle", 19);
    }

    @Test
    void testUnsafeFieldUsages2() {
        performAnalysis("commonResources/UnsafeFieldUsage2.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertNumOfBugs(2);
        assertBug("UnsafeFieldUsage2", "lambda$new$0", 8);
        assertBug("UnsafeFieldUsage2", "createVehicle", 16);
    }

    @Test
    void testUnsafeFieldUsages3() {
        performAnalysis("commonResources/UnsafeFieldUsage3.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertNumOfBugs(2);
        assertBug("UnsafeFieldUsage3", "lambda$new$0", 12);
        assertBug("UnsafeFieldUsage3", "createVehicle", 8);
    }

    @Test
    void testSynchronizedUnsafeFieldUsage() {
        performAnalysis("commonResources/SynchronizedUnsafeFieldUsage.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertNumOfBugs(2);
        assertBug("SynchronizedUnsafeFieldUsage", "lambda$new$0", 8);
        assertBug("SynchronizedUnsafeFieldUsage", "createVehicle", 16);
    }

    private void assertNumOfBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD")
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
