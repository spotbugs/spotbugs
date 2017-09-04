package de.tobject.findbugs.reporter;

import java.util.function.LongSupplier;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * <p>SpotBugs Eclipse Plugin uses {@link IProgressMonitor#setTaskName(String)} to tell what it is working for.
 * It also updates progress quite frequently, which costs much time to update progress bar.</p>
 *
 * <p>Both of them make performance slow, so this class introduces throttling for better performance.</p>
 *
 * @author Kengo TODA
 * @version 3.1
 */
@NotThreadSafe
class ThrottledProgressMonitor implements IProgressMonitor {
    /**
     * Span of throttling, in milliseconds.
     */
    static final int THROTTLE_SPAN = 300;
    private static final long NOT_TRIGGERED = Integer.MIN_VALUE;

    private final IProgressMonitor delegate;
    private final LongSupplier currentTimeMillis;
    private long lastRenamed = NOT_TRIGGERED;
    private long lastWorked = NOT_TRIGGERED;
    private int accumulatedWork;

    ThrottledProgressMonitor(@Nonnull IProgressMonitor delegate, @Nonnull LongSupplier currentTimeMillis) {
        Assert.isNotNull(delegate);
        Assert.isNotNull(currentTimeMillis);

        this.delegate = delegate;
        this.currentTimeMillis = currentTimeMillis;
    }

    @Override
    public void setTaskName(String name) {
        long currentTime = currentTimeMillis.getAsLong();
        if (lastRenamed == NOT_TRIGGERED || currentTime - lastRenamed >= THROTTLE_SPAN) {
            lastRenamed = currentTime;
            delegate.setTaskName(name);
        }
    }

    @Override
    public void worked(int work) {
        long currentTime = currentTimeMillis.getAsLong();
        accumulatedWork += work;
        if (lastWorked == NOT_TRIGGERED || currentTime - lastWorked >= THROTTLE_SPAN) {
            lastWorked = currentTime;
            delegate.worked(accumulatedWork);
            accumulatedWork = 0;
        }
    }

    @Override
    public void beginTask(String name, int totalWork) {
        delegate.beginTask(name, totalWork);
    }

    @Override
    public void done() {
        delegate.done();
    }

    @Override
    public void internalWorked(double work) {
        delegate.internalWorked(work);
    }

    @Override
    public boolean isCanceled() {
        return delegate.isCanceled();
    }

    @Override
    public void setCanceled(boolean value) {
        delegate.setCanceled(value);
    }

    @Override
    public void subTask(String name) {
        delegate.subTask(name);
    }
}
