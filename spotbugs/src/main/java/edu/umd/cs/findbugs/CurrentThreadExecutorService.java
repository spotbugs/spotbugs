/*
 * Contributions to SpotBugs
 * Copyright (C) 2019, kengo
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * An {@link ExecutorService} implementation that runs command in the current thread. Instance of this class is not
 * thread safe, do not share it among multiple threads. SpotBugs uses this class to keep backward compatibility
 * (SpotBugs 3.1 run analysis on the main/current thread).
 * </p>
 *
 * @since 4.0
 */
@NotThreadSafe
class CurrentThreadExecutorService extends AbstractExecutorService {
    private static final Logger LOG = LoggerFactory.getLogger(CurrentThreadExecutorService.class);

    private boolean isShutdown = false;

    @Override
    public void shutdown() {
        if (isShutdown) {
            throw new IllegalStateException("CurrentThreadExecutorService is closed again");
        }

        isShutdown = true;
        LOG.debug("CurrentThreadExecutorService is closed, do nothing");
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public boolean isTerminated() {
        // this ExecutorService implementation does not keep commands, then
        // we can immediately terminate it after the shutdown
        return isShutdown;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        if (!isShutdown) {
            // this ExecutorService is not designed to share among threads, then simply throw IllegalStateException
            throw new IllegalStateException("awaitTermination() should be called after the shutdown");
        }
        return true;
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
