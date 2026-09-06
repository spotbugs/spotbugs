package edu.umd.cs.findbugs.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class FutureValueTest {

    @Test
    void setThenGetCompletesFuture() throws Exception {
        FutureValue<String> future = new FutureValue<>();

        future.set("done");

        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
        assertEquals("done", future.get());
        assertEquals("done", future.get(1, TimeUnit.SECONDS));
    }

    @Test
    void timeoutMethodsBehaveAsExpected() throws Exception {
        FutureValue<String> future = new FutureValue<>();

        assertThrows(TimeoutException.class, () -> future.get(1, TimeUnit.MILLISECONDS));
        assertEquals("fallback", future.get(1, TimeUnit.MILLISECONDS, "fallback"));
        assertFalse(future.isDone());
    }

    @Test
    void cancellationPreventsGetAndSet() {
        FutureValue<String> future = new FutureValue<>();

        assertTrue(future.cancel(true));
        assertFalse(future.cancel(true));
        assertTrue(future.isCancelled());
        assertFalse(future.isDone());
        assertThrows(RuntimeException.class, future::get);
        assertThrows(RuntimeException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertThrows(RuntimeException.class, () -> future.get(1, TimeUnit.SECONDS, "fallback"));
        assertThrows(IllegalStateException.class, () -> future.set("value"));
    }

    @Test
    void cannotSetTwice() {
        FutureValue<String> future = new FutureValue<>();

        future.set("first");

        assertThrows(IllegalStateException.class, () -> future.set("second"));
    }
}
