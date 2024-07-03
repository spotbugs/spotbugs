package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class AtomicOperationsCombinedDetectorTest extends AbstractIntegrationTest {

    private static final String OPERATIONS_ARE_NOT_ATOMIC = "AT_COMBINED_ATOMIC_OPERATIONS_ARE_NOT_ATOMIC";
    private static final String NEEDS_SYNCHRONIZATION = "AT_ATOMIC_OPERATION_NEEDS_SYNCHRONIZATION";

    @Test
    void testSafeSynchronizedList() {
        performAnalysis(
                "atomicMethods/SafeSynchronizedList.class",
                "atomicMethods/SafeSynchronizedLists.class",
                "atomicMethods/SafeSynchronizedList2.class",
                "atomicMethods/SafeSynchronizedListWithMultipleSync.class",
                "atomicMethods/SafeSynchronizedListWithMultipleSync2.class",
                "atomicMethods/SafeSynchronizedListWithMethodInit.class",
                "atomicMethods/SafeSingleSynchronizedListCall.class",
                "atomicMethods/SynchronizedListWithDifferentLock.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeSynchronizedList() {
        performAnalysis("atomicMethods/UnsafeSynchronizedList.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedList", "nums", 13);
    }

    @Test
    void testUnsafeSynchronizedListWithMultipleSync() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithMultipleSync.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithMultipleSync", "nums", 32);
    }

    @Test
    void testUnsafeSynchronizedListWithMethodInit() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithMethodInit.class");
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 1);
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeSynchronizedListWithMethodInit", "nums", 12);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithMethodInit", "nums", 17);
    }

    @Test
    void testSafeSynchronizedSet() {
        performAnalysis(
                "atomicMethods/SafeSynchronizedSet.class",
                "atomicMethods/SafeSynchronizedSet2.class",
                "atomicMethods/SafeSynchronizedSetWithUnusedVars.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeSynchronizedSet() {
        performAnalysis(
                "atomicMethods/UnsafeSynchronizedSet.class",
                "atomicMethods/UnsafeSynchronizedSortedSet.class",
                "atomicMethods/UnsafeSynchronizedNavigableSet.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 3);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSet", "nums", 13);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSortedSet", "nums", 12);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedNavigableSet", "nums", 12);
    }

    @Test
    void testUnsafeSynchronizedCollection() {
        performAnalysis("atomicMethods/UnsafeSynchronizedCollection.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedCollection", "nums", 12);
    }

    @Test
    void testUnsafeSynchronizedMap() {
        performAnalysis(
                "atomicMethods/UnsafeSynchronizedMap.class",
                "atomicMethods/UnsafeSynchronizedNavigableMap.class",
                "atomicMethods/UnsafeSynchronizedSortedMap.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 3);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedMap", "nums", 13);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedNavigableMap", "nums", 12);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSortedMap", "nums", 12);
    }

    @Test
    void testSynchronizedListInSynchronizedMethod() {
        performAnalysis(
                "atomicMethods/SynchronizedListInSynchronizedMethod.class",
                "atomicMethods/SynchronizeInSynchronizedMethod.class",
                "atomicMethods/SafeSynchronizedListWithPrivateMethods.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeSynchronizedListAndSet() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListAndSet.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListAndSet", "nums", 16);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListAndSet", "nums2", 20);
    }

    @Test
    void testPartialSynchronizedListAndSet() {
        performAnalysis("atomicMethods/PartialSynchronizedListAndSet.class", "atomicMethods/PartialSynchronizedListAndSet2.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "PartialSynchronizedListAndSet", "nums", 16);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "PartialSynchronizedListAndSet2", "nums2", 22);
    }

    @Test
    void testUnsafeSynchronizedListWithPrivateMethods() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithPrivateMethods.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithPrivateMethods", "nums", 13);
    }

    @Test
    void testUnsafeSynchronizedSetInMultipleMethods() {
        performAnalysis("atomicMethods/UnsafeSynchronizedSetInMultipleMethods.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSetInMultipleMethods", "nums", 13);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSetInMultipleMethods", "nums", 19);
    }

    @Test
    void testSafeSynchronizedMapIncrement() {
        performAnalysis("atomicMethods/SafeSynchronizedMapIncrement.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeSynchronizedMapIncrement() {
        performAnalysis("atomicMethods/UnsafeSynchronizedMapIncrement.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 1);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedMapIncrement", "map", 17);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeSynchronizedMapIncrement", "map", 21);
    }

    @Test
    void testSafeAtomicReference() {
        performAnalysis("atomicMethods/SafeAtomicReference.class", "atomicMethods/AtomicLongWithMethodParam.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeAtomicReference() {
        performAnalysis(
                "atomicMethods/UnsafeAtomicReference.class",
                "atomicMethods/UnsafeAtomicReference2.class",
                "atomicMethods/UnsafeAtomicReference3.class",
                "atomicMethods/UnsafeAtomicReference4.class",
                "atomicMethods/UnsafeAtomicReference4$Inner.class",
                "atomicMethods/UnsafeAtomicReference5.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 11);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 3);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "a", 17);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "b", 18);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "a", 22);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "b", 22);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "atomicBoolean", 18);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "atomicInteger", 18);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "atomicBoolean2", 26);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "atomicInteger2", 26);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference3", "hadException", 11);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference3", "hadException", 15);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference3", "hadException", 19);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference4", "ref", 22);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference4", "ref", 33);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference5", "atomicBoolean", 15);
    }

    @Test
    void testUnsafeAtomicReferenceValueSet() {
        performAnalysis("atomicMethods/UnsafeAtomicReferenceValueSet.class");
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 1);
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugAtFieldAtLine(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReferenceValueSet", "atomicInteger", 10);
        assertBugAtFieldAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReferenceValueSet", "atomicInteger", 15);
    }

    @Test
    void testSafeLocalAtomicInteger() {
        performAnalysis("atomicMethods/SafeLocalAtomicInteger.class", "atomicMethods/SafeLocalAtomicInteger2.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeLocalAtomicInteger() {
        performAnalysis("atomicMethods/UnsafeLocalAtomicInteger.class");
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
        assertBugInMethodAtLine(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeLocalAtomicInteger", "increment", 23);
    }

    @Test
    void testSafePresynchronizedPrivateMethod() {
        performAnalysis("atomicMethods/SafePresynchronizedPrivateMethod.class");
        assertZeroBugs();
    }

    @Test
    void testSafeAtomicValueUsage() {
        performAnalysis("atomicMethods/SafeAtomicValueUsage.class");
        assertZeroBugs();
    }

    @Test
    void testSafeAtomicInFinallyBlock() {
        performAnalysis("atomicMethods/SafeAtomicInFinallyBlock.class");
        assertZeroBugs();
    }

    @Test
    void testSafeAtomicCallsWithLambda() {
        performAnalysis("atomicMethods/SafeAtomicCallsWithLambda.class");
        assertZeroBugs();
    }

    private void assertZeroBugs() {
        assertBugTypeCount(OPERATIONS_ARE_NOT_ATOMIC, 0);
        assertBugTypeCount(NEEDS_SYNCHRONIZATION, 0);
    }
}
