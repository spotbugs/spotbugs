package de.tobject.findbugs.reporter;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

public class ThrottledProgressMonitorTest {
    @Test
    public void testSetTaskName() {
        Clock clock = new Clock();
        IProgressMonitor delegate = spy(IProgressMonitor.class);
        ThrottledProgressMonitor throttled = new ThrottledProgressMonitor(delegate, clock::getCurrentTime);
        throttled.setTaskName("First");
        verify(delegate).setTaskName("First");

        clock.currentTime = ThrottledProgressMonitor.THROTTLE_SPAN - 1;
        throttled.setTaskName("Second");
        verify(delegate, never()).setTaskName("Second");

        clock.currentTime = ThrottledProgressMonitor.THROTTLE_SPAN;
        throttled.setTaskName("Third");
        verify(delegate).setTaskName("Third");
    }

    @Test
    public void testWorked() {
        Clock clock = new Clock();
        IProgressMonitor delegate = spy(IProgressMonitor.class);
        ThrottledProgressMonitor throttled = new ThrottledProgressMonitor(delegate, clock::getCurrentTime);
        throttled.worked(1);
        verify(delegate).worked(1);

        clock.currentTime = ThrottledProgressMonitor.THROTTLE_SPAN - 1;
        throttled.worked(2);
        verify(delegate, never()).worked(2);

        clock.currentTime = ThrottledProgressMonitor.THROTTLE_SPAN;
        throttled.worked(4);
        verify(delegate).worked(2 + 4);
    }

    private static final class Clock {
        private long currentTime;

        protected long getCurrentTime() {
            return currentTime;
        }
    }
}
