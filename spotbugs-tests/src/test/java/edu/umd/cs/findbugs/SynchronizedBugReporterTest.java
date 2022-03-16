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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class SynchronizedBugReporterTest {
    @Test
    public void testSetErrorVerbosity() {
        AtomicBoolean called = new AtomicBoolean();
        PrintingBugReporter inner = new PrintingBugReporter() {
            @Override
            public void setErrorVerbosity(int level) {
                assertEquals(level, BugReporter.NORMAL);
                called.set(true);
            }
        };
        new SynchronizedBugReporter(inner).setErrorVerbosity(BugReporter.NORMAL);
        assertTrue(called.get());
    }
}
