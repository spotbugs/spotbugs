/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author pugh
 */
public class FutureValue<V> implements Future<V> {

    final CountDownLatch latch = new CountDownLatch(1);

    V value;

    volatile boolean canceled;

    public synchronized boolean cancel(boolean arg0) {
        if (latch.getCount() == 0)
            return false;
        canceled = true;
        latch.countDown();
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get()
     */
    public V get() throws InterruptedException {
        latch.await();
        if (canceled)
            throw new RuntimeException("Canceled");
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    public V get(long arg0, TimeUnit arg1) throws InterruptedException, TimeoutException {
        if (!latch.await(arg0, arg1))
            throw new TimeoutException();
        if (canceled)
            throw new RuntimeException("Canceled");
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    public V get(long arg0, TimeUnit arg1, V valueOnTimeout) throws InterruptedException {
        if (!latch.await(arg0, arg1))
            return valueOnTimeout;
       
        if (canceled)
            throw new RuntimeException("Canceled");
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#isCancelled()
     */
    public boolean isCancelled() {
        return canceled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.concurrent.Future#isDone()
     */
    public boolean isDone() {
        return !canceled && latch.getCount() == 0;
    }

    public synchronized void set(V value) {
        if (canceled)
            throw new IllegalStateException("Already cancelled");
        if (latch.getCount() == 0)
            throw new IllegalStateException("Already set");
        this.value = value;
        latch.countDown();
    }

}
