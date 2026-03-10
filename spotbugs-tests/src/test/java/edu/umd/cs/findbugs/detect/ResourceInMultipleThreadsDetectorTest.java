package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

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
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 0);
    }

    @Test
    void testUnsafeFieldUsages() {
        performAnalysis("commonResources/UnsafeFieldUsage.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 5);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage", "lambda$new$0", 8);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage", "createVehicle", 16);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage", "createVehicle", 17);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage", "createVehicle", 18);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage", "createVehicle", 19);
    }

    @Test
    void testUnsafeFieldUsages2() {
        performAnalysis("commonResources/UnsafeFieldUsage2.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 2);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage2", "lambda$new$0", 8);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage2", "createVehicle", 16);
    }

    @Test
    void testUnsafeFieldUsages3() {
        performAnalysis("commonResources/UnsafeFieldUsage3.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 2);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage3", "lambda$new$0", 12);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage3", "createVehicle", 8);
    }

    @Test
    void testUnsafeFieldUsages4() {
        performAnalysis("commonResources/UnsafeFieldUsage4.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 2);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage4", "lambda$new$0", 16);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "UnsafeFieldUsage4", "createVehicle", 12);
    }

    @Test
    void testSynchronizedUnsafeFieldUsage() {
        performAnalysis("commonResources/SynchronizedUnsafeFieldUsage.class",
                "commonResources/Vehicle.class",
                "commonResources/Vehicle$Builder.class");
        assertBugTypeCount("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", 2);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "SynchronizedUnsafeFieldUsage", "lambda$new$0", 8);
        assertBugInMethodAtLine("AT_UNSAFE_RESOURCE_ACCESS_IN_THREAD", "SynchronizedUnsafeFieldUsage", "createVehicle", 16);
    }
}
