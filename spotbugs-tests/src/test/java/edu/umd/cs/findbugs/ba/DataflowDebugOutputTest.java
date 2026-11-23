package edu.umd.cs.findbugs.ba;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.Type;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Detailed dataflow output must only appear when {@link Dataflow#DEBUG} is enabled explicitly.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3785">GitHub issue</a>
 */
class DataflowDebugOutputTest {
    /** Same threshold as {@code Dataflow.MAX_ITERS}. */
    private static final int MAX_ITERS = SystemProperties.getInt("dataflow.maxiters", 97);

    /** {@code Dataflow.execute()} gives up at this iteration. */
    private static final int ITERATION_LIMIT = MAX_ITERS + 9;

    private static final long TIMEOUT_SECONDS = 30;

    private boolean debugWas;
    private PrintStream originalOut;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureStdout() {
        // Dataflow.DEBUG is static and shared by all tests running in this JVM, so always restore it.
        debugWas = Dataflow.DEBUG;
        Dataflow.DEBUG = false;
        originalOut = System.out;
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restore() {
        System.setOut(originalOut);
        Dataflow.DEBUG = debugWas;
    }

    private String takeOutput() {
        String output = captured.toString(StandardCharsets.UTF_8);
        captured.reset();
        return output;
    }

    @Test
    void convergingPastThresholdWithDebugOffIsSilent() throws DataflowAnalysisException {
        // Stops changing a few iterations after the threshold, but before the final limit.
        Dataflow<Counter, CountingAnalysis> dataflow = newDataflow(new CountingAnalysis(MAX_ITERS + 3, -1));
        dataflow.execute();

        int iterations = dataflow.getNumIterations();
        assertTrue(iterations > MAX_ITERS && iterations < ITERATION_LIMIT,
                "expected convergence between the threshold and the limit, got " + iterations);
        assertEquals("", takeOutput());
        assertFalse(Dataflow.DEBUG, "execute() must not modify Dataflow.DEBUG");
    }

    @Test
    void exceedingThresholdWithDebugOffLogsNothingForAOrB() throws DataflowAnalysisException {
        // A passes the threshold and then fails with an exception other than "Too many iterations".
        // This used to leave Dataflow.DEBUG == true for every later analysis.
        Dataflow<Counter, CountingAnalysis> a = newDataflow(new CountingAnalysis(Integer.MAX_VALUE, MAX_ITERS + 2));
        assertThrows(IllegalStateException.class, a::execute);
        String outputOfA = takeOutput();
        boolean debugAfterA = Dataflow.DEBUG;

        newDataflow(new CountingAnalysis(3, -1)).execute();
        String outputOfB = takeOutput();

        assertAll(
                () -> assertEquals("", outputOfA, "A must not log after passing the threshold"),
                () -> assertFalse(debugAfterA, "execute() must not modify Dataflow.DEBUG"),
                () -> assertEquals("", outputOfB, "B must not inherit debug output from A"));
    }

    @Test
    void concurrentAnalysisWhileAIsPastThresholdStaysSilent() throws Exception {
        CountDownLatch aPastThreshold = new CountDownLatch(1);
        CountDownLatch bFinished = new CountDownLatch(1);

        // A never converges. Its first transfer() after the threshold is a normal analysis step: it signals there and
        // waits until B has finished, so B runs entirely while A is still inside execute().
        CountingAnalysis analysisA = new CountingAnalysis(Integer.MAX_VALUE, -1) {
            @Override
            void onFirstTransferPastThreshold() throws InterruptedException {
                aPastThreshold.countDown();
                assertTrue(bFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "B did not finish");
            }
        };

        ExecutorService executor = Executors.newSingleThreadExecutor();
        String outputOfAUntilSignal;
        String outputOfB;
        try {
            Future<?> a = executor.submit(() -> {
                newDataflow(analysisA).execute();
                return null;
            });
            assertTrue(aPastThreshold.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "A did not pass the threshold");
            assertFalse(a.isDone(), "A must still be inside execute()");
            outputOfAUntilSignal = takeOutput();

            // A is blocked in transfer(), so everything written now comes from B.
            try {
                newDataflow(new CountingAnalysis(3, -1)).execute();
                outputOfB = takeOutput();
            } finally {
                bFinished.countDown();
            }

            ExecutionException thrown = assertThrows(ExecutionException.class, () -> a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertInstanceOf(DataflowAnalysisException.class, thrown.getCause(), "A should stop after too many iterations");
        } finally {
            executor.shutdownNow();
        }

        String outputOfARest = takeOutput();
        assertAll(
                () -> assertEquals("", outputOfAUntilSignal, "A must not log after passing the threshold"),
                () -> assertEquals("", outputOfB, "B must not log while A is past the threshold"),
                () -> assertEquals("", outputOfARest, "A must not log for the rest of its execution"),
                () -> assertFalse(Dataflow.DEBUG, "execute() must not modify Dataflow.DEBUG"));
    }

    @Test
    void nonConvergingAnalysisReportsFinalException() {
        Dataflow<Counter, CountingAnalysis> dataflow = newDataflow(new CountingAnalysis(Integer.MAX_VALUE, -1));
        DataflowAnalysisException e = assertThrows(DataflowAnalysisException.class, dataflow::execute);

        assertTrue(e.getMessage().startsWith("Too many iterations (" + ITERATION_LIMIT + ") in dataflow when analyzing "),
                e.getMessage());
        assertTrue(e.getMessage().contains("loop"), e.getMessage());
        assertEquals("", takeOutput());
        assertFalse(Dataflow.DEBUG);
    }

    @Test
    void explicitDebugStillProducesOutput() throws DataflowAnalysisException {
        Dataflow.DEBUG = true;
        newDataflow(new CountingAnalysis(3, -1)).execute();

        String output = takeOutput();
        assertTrue(output.contains("Executing DataflowDebugOutputTest$CountingAnalysis on Fake.loop()"), output);
        assertTrue(output.contains("iteration: 1"), output);
        assertTrue(output.contains("Quiescence achieved"), output);
        assertTrue(Dataflow.DEBUG, "explicit debug must stay enabled");
    }

    @Test
    void explicitDebugReportsTooManyIterationsOnce() {
        Dataflow.DEBUG = true;
        Dataflow<Counter, CountingAnalysis> dataflow = newDataflow(new CountingAnalysis(Integer.MAX_VALUE, -1));
        assertThrows(DataflowAnalysisException.class, dataflow::execute);

        String output = takeOutput();
        String marker = "Too many iterations DataflowDebugOutputTest$CountingAnalysis on Fake.loop()";
        assertTrue(output.contains(marker), output);
        assertEquals(output.indexOf(marker), output.lastIndexOf(marker), "diagnostics must be reported only once");
        assertTrue(Dataflow.DEBUG, "explicit debug must stay enabled");
    }

    /**
     * Builds {@code entry -> head <-> body, head -> exit}, so every iteration sees a back edge.
     */
    private static Dataflow<Counter, CountingAnalysis> newDataflow(CountingAnalysis analysis) {
        CFG cfg = new CFG();
        BasicBlock entry = cfg.getEntry();
        BasicBlock head = cfg.allocate();
        BasicBlock body = cfg.allocate();
        BasicBlock exit = cfg.getExit();
        cfg.createEdge(entry, head, EdgeTypes.FALL_THROUGH_EDGE);
        cfg.createEdge(head, body, EdgeTypes.FALL_THROUGH_EDGE);
        cfg.createEdge(body, head, EdgeTypes.GOTO_EDGE);
        cfg.createEdge(head, exit, EdgeTypes.IFCMP_EDGE);
        cfg.setMethodGen(new MethodGen(Const.ACC_PUBLIC | Const.ACC_STATIC, Type.VOID, Type.NO_ARGS, null,
                "loop", "Fake", new InstructionList(), new ConstantPoolGen()));

        DepthFirstSearch dfs = new DepthFirstSearch(cfg);
        dfs.search();
        analysis.setDfs(dfs);
        return new Dataflow<>(cfg, analysis);
    }

    static final class Counter {
        boolean top = true;
        int value;

        @Override
        public String toString() {
            return top ? "TOP" : Integer.toString(value);
        }
    }

    /** Increments the fact on every block while the iteration number is below {@code growUntilIteration}. */
    static class CountingAnalysis extends BasicAbstractDataflowAnalysis<Counter> {
        private final int growUntilIteration;
        private final int failAtIteration;
        private DepthFirstSearch dfs;
        private int iteration;
        private boolean passedThreshold;

        /**
         * @param growUntilIteration facts keep changing while the iteration number is below this value
         * @param failAtIteration iteration at which the analysis throws, or -1 for never
         */
        CountingAnalysis(int growUntilIteration, int failAtIteration) {
            this.growUntilIteration = growUntilIteration;
            this.failAtIteration = failAtIteration;
        }

        void setDfs(DepthFirstSearch dfs) {
            this.dfs = dfs;
        }

        /** Called once, from the first transfer() after the iteration threshold has been exceeded. */
        void onFirstTransferPastThreshold() throws InterruptedException {
        }

        @Override
        public void startIteration() {
            if (++iteration == failAtIteration) {
                throw new IllegalStateException("simulated analysis failure");
            }
        }

        @Override
        public Counter createFact() {
            return new Counter();
        }

        @Override
        public void copy(Counter source, Counter dest) {
            dest.top = source.top;
            dest.value = source.value;
        }

        @Override
        public void initEntryFact(Counter fact) {
            fact.top = false;
            fact.value = 0;
        }

        @Override
        public void makeFactTop(Counter fact) {
            fact.top = true;
            fact.value = 0;
        }

        @Override
        public boolean isTop(Counter fact) {
            return fact.top;
        }

        @Override
        public boolean isForwards() {
            return true;
        }

        @Override
        public BlockOrder getBlockOrder(CFG cfg) {
            return new ReversePostOrder(cfg, dfs);
        }

        @Override
        public boolean same(Counter fact1, Counter fact2) {
            return fact1.top == fact2.top && fact1.value == fact2.value;
        }

        @Override
        public void meetInto(Counter fact, Edge edge, Counter result) {
            if (fact.top) {
                return;
            }
            if (result.top) {
                copy(fact, result);
            } else {
                result.value = Math.max(result.value, fact.value);
            }
        }

        @Override
        public void transfer(BasicBlock basicBlock, InstructionHandle end, Counter start, Counter result) {
            if (iteration > MAX_ITERS && !passedThreshold) {
                passedThreshold = true;
                try {
                    onFirstTransferPastThreshold();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            }
            copy(start, result);
            if (!start.top && iteration < growUntilIteration) {
                result.value++;
            }
        }
    }
}
