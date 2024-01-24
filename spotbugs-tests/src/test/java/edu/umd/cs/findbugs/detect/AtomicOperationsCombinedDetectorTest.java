package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class AtomicOperationsCombinedDetectorTest extends AbstractIntegrationTest {

    public static final String OPERATIONS_ARE_NOT_ATOMIC = "AT_COMBINED_ATOMIC_OPERATIONS_ARE_NOT_ATOMIC";
    public static final String NEEDS_SYNCHRONIZATION = "AT_ATOMIC_OPERATION_NEEDS_SYNCHRONIZATION";

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
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedList", "addAndPrintNumbers", 13);
    }

    @Test
    void testUnsafeSynchronizedListWithMultipleSync() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithMultipleSync.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithMultipleSync", "addAndPrintNumbers", 32);
    }

    @Test
    void testUnsafeSynchronizedListWithMethodInit() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithMethodInit.class");
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 1);
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeSynchronizedListWithMethodInit", "init", 12);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithMethodInit", "addAndPrintNumbers", 17);
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
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 3);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSet", "addAndPrintNumbers", 13);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSortedSet", "addAndPrintNumbers", 12);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedNavigableSet", "addAndPrintNumbers", 12);
    }

    @Test
    void testUnsafeSynchronizedCollection() {
        performAnalysis("atomicMethods/UnsafeSynchronizedCollection.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedCollection", "addAndPrintNumbers", 12);
    }

    @Test
    void testUnsafeSynchronizedMap() {
        performAnalysis(
                "atomicMethods/UnsafeSynchronizedMap.class",
                "atomicMethods/UnsafeSynchronizedNavigableMap.class",
                "atomicMethods/UnsafeSynchronizedSortedMap.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 3);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedMap", "addAndPrintNumbers", 13);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedNavigableMap", "addAndPrintNumbers", 12);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSortedMap", "addAndPrintNumbers", 12);
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
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListAndSet", "addAndPrintNumbers", 16);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListAndSet", "addAndPrintNumbers", 20);
    }

    @Test
    void testPartialSynchronizedListAndSet() {
        performAnalysis("atomicMethods/PartialSynchronizedListAndSet.class", "atomicMethods/PartialSynchronizedListAndSet2.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "PartialSynchronizedListAndSet", "addAndPrintNumbers", 16);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "PartialSynchronizedListAndSet2", "addAndPrintNumbers", 22);
    }

    @Test
    void testUnsafeSynchronizedListWithPrivateMethods() {
        performAnalysis("atomicMethods/UnsafeSynchronizedListWithPrivateMethods.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedListWithPrivateMethods", "addAndPrintNumbers", 13);
    }

    @Test
    void testUnsafeSynchronizedSetInMultipleMethods() {
        performAnalysis("atomicMethods/UnsafeSynchronizedSetInMultipleMethods.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 2);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSetInMultipleMethods", "addAndPrintNumbers", 13);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedSetInMultipleMethods", "removeAndPrintNumbers", 19);
    }

    @Test
    void testSafeSynchronizedMapIncrement() {
        performAnalysis("atomicMethods/SafeSynchronizedMapIncrement.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeSynchronizedMapIncrement() {
        performAnalysis("atomicMethods/UnsafeSynchronizedMapIncrement.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 1);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeSynchronizedMapIncrement", "increment", 17);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeSynchronizedMapIncrement", "getCount", 21);
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
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 11);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 3);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "update", 17);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference", "add", 22);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "incrementAtomicInteger", 18);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "incrementAtomicInteger", 18);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "incrementAtomicInteger2", 26);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference2", "incrementAtomicInteger2", 26);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference3", "preparation", 11);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference3", "run", 15);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference3", "after", 19);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference4", "initProcessing", 22);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReference4", "finishProcessing", 33);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReference5", "lambda$incrementAtomicInteger$0", 15);
    }

    @Test
    void testUnsafeAtomicReferenceValueSet() {
        performAnalysis("atomicMethods/UnsafeAtomicReferenceValueSet.class");
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 1);
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertBug(NEEDS_SYNCHRONIZATION, "UnsafeAtomicReferenceValueSet", "init", 10);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeAtomicReferenceValueSet", "update", 15);
    }

    @Test
    void testSafeLocalAtomicInteger() {
        performAnalysis("atomicMethods/SafeLocalAtomicInteger.class", "atomicMethods/SafeLocalAtomicInteger2.class");
        assertZeroBugs();
    }

    @Test
    void testUnsafeLocalAtomicInteger() {
        performAnalysis("atomicMethods/UnsafeLocalAtomicInteger.class");
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 1);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
        assertBug(OPERATIONS_ARE_NOT_ATOMIC, "UnsafeLocalAtomicInteger", "increment", 23);
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
        assertNumOfBugs(OPERATIONS_ARE_NOT_ATOMIC, 0);
        assertNumOfBugs(NEEDS_SYNCHRONIZATION, 0);
    }

    private void assertNumOfBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String bugType, String className, String methodName, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
