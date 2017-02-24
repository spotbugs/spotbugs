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

package edu.umd.cs.findbugs.log;

import java.lang.reflect.Method;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author pugh
 */
public class YourKitController {

    private static final boolean ENABLED = SystemProperties.getBoolean("findbugs.yourkit.enabled");

    Object controller;

    Method advanceGeneration, captureMemorySnapshot, getStatus;

    public static final long ALLOCATION_RECORDING = 2L;

    public YourKitController() {
        if(!ENABLED){
            return;
        }
        try {
            Class<?> c = Class.forName("com.yourkit.api.Controller");
            controller = c.newInstance();
            advanceGeneration = c.getMethod("advanceGeneration", String.class);
            captureMemorySnapshot = c.getMethod("captureMemorySnapshot");
            getStatus = c.getMethod("getStatus");

        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            controller = null;
        }

    }

    public void advanceGeneration(String name) {
        if (controller == null) {
            return;
        }
        try {
            advanceGeneration.invoke(controller, name);
        } catch (Throwable e) {
            assert true;
        }
    }

    public long getStatus() {
        if (getStatus == null) {
            return 0;
        }
        try {
            return (Long) getStatus.invoke(controller);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return 0;
        }
    }

    @SuppressFBWarnings("DM_GC")
    public void captureMemorySnapshot() {
        if (controller == null) {
            return;
        }
        try {
            System.gc();
            captureMemorySnapshot.invoke(controller);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            assert true;
        }
    }

}
