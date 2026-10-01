package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class InfiniteRecursionTest extends AbstractIntegrationTest {
    @Test
    void testInfiniteRecursiveLoop() {
        performAnalysis("InfiniteRecursiveLoop.class");

        assertBugTypeCount("IL_INFINITE_RECURSIVE_LOOP", 5);
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "<init>");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "more");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "muchMore");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "equals");
        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "InfiniteRecursiveLoop", "hashCode");
    }

    @Test
    void testInfiniteRecursiveLoopFalsePositive() {
        performAnalysis("InfiniteRecursiveLoopFalsePositive.class", "InfiniteRecursiveLoopFalsePositive$Inner.class");

        assertNoBugType("IL_INFINITE_RECURSIVE_LOOP");
    }
}
