/*
 * Contributions to SpotBugs
 * Copyright (C) 2019, Kengo TODA
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

class Profile implements XMLWriteable {
    private static final boolean MAX_CONTEXT = SystemProperties.getBoolean("findbugs.profiler.maxcontext");

    /** time in nanoseconds */
    private final AtomicLong totalTime = new AtomicLong();

    private final AtomicInteger totalCalls = new AtomicInteger();

    /** time in nanoseconds */
    private final AtomicLong maxTime = new AtomicLong();

    private final AtomicLong totalSquareMicroseconds = new AtomicLong();

    private final String className;

    Object maxContext;

    /**
     * @param className
     *            non null full qualified class name
     */
    Profile(String className) {
        this.className = className;
    }

    void handleCall(long nanoTime, Object context) {
        totalCalls.incrementAndGet();
        totalTime.addAndGet(nanoTime);
        long oldMax = maxTime.get();
        if (nanoTime > oldMax) {
            maxTime.compareAndSet(oldMax, nanoTime);
            if (MAX_CONTEXT) {
                maxContext = context;
            }
        }
        long microseconds = TimeUnit.MICROSECONDS.convert(nanoTime, TimeUnit.NANOSECONDS);
        totalSquareMicroseconds.addAndGet(microseconds * microseconds);
    }

    long getTotalTime() {
        return totalTime.get();
    }

    int getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * @param xmlOutput
     * @throws IOException
     */

    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        long time = totalTime.get();
        int callCount = totalCalls.get();
        long maxTimeMicros = TimeUnit.MICROSECONDS.convert(maxTime.get(), TimeUnit.NANOSECONDS);
        long timeMillis = TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
        long timeMicros = TimeUnit.MICROSECONDS.convert(time, TimeUnit.NANOSECONDS);

        long averageTimeMicros = timeMicros / callCount;
        long totalSquareMicros = totalSquareMicroseconds.get();
        long averageSquareMicros = totalSquareMicros / callCount;
        long timeVariance = averageSquareMicros - averageTimeMicros * averageTimeMicros;
        long timeStandardDeviation = (long) Math.sqrt(timeVariance);
        if (timeMillis > 10) {
            xmlOutput.startTag("ClassProfile");

            xmlOutput.addAttribute("name", className);
            xmlOutput.addAttribute("totalMilliseconds", String.valueOf(timeMillis));
            xmlOutput.addAttribute("invocations", String.valueOf(callCount));
            xmlOutput.addAttribute("avgMicrosecondsPerInvocation", String.valueOf(averageTimeMicros));
            xmlOutput.addAttribute("maxMicrosecondsPerInvocation", String.valueOf(maxTimeMicros));
            if (maxContext != null) {
                xmlOutput.addAttribute("maxContext", String.valueOf(maxContext));
            }
            xmlOutput.addAttribute("standardDeviationMicrosecondsPerInvocation", String.valueOf(timeStandardDeviation));
            xmlOutput.stopTag(true);
        }
    }
}