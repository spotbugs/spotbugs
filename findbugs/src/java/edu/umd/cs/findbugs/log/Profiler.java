/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.EmptyStackException;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * @author pugh
 */
public class Profiler implements XMLWriteable {

    final static boolean REPORT = SystemProperties.getBoolean("profiler.report");
    final static boolean MAX_CONTEXT = SystemProperties.getBoolean("findbugs.profiler.maxcontext");

    public Profiler() {
        startTimes = new Stack<Clock>();
        profile = new ConcurrentHashMap<Class<?>, Profile>();
        if (REPORT) {
            System.err.println("Profiling activated");
        }
    }

    public static interface Filter {
        public boolean accepts(Profile p);
    }

    public static class FilterByTime implements Filter {
        private final long minNanoSeconds;

        public FilterByTime(long minNanoSeconds) {
            this.minNanoSeconds = minNanoSeconds;
        }

        @Override
        public boolean accepts(Profile p) {
            long time = p.totalTime.get();
            if (time < minNanoSeconds) {
                return false;
            }
            return true;
        }
    }

    public static class FilterByTimePerCall implements Filter {
        private final long minNanoSeconds;

        public FilterByTimePerCall(long minNanoSeconds) {
            this.minNanoSeconds = minNanoSeconds;
        }

        @Override
        public boolean accepts(Profile p) {
            int totalCalls = p.totalCalls.get();
            long time = p.totalTime.get();
            if (time / totalCalls < minNanoSeconds) {
                return false;
            }
            return true;
        }
    }

    public static class FilterByCalls implements Filter {
        private final int minCalls;

        public FilterByCalls(int minCalls) {
            this.minCalls = minCalls;
        }

        @Override
        public boolean accepts(Profile p) {
            int totalCalls = p.totalCalls.get();
            if (totalCalls < minCalls) {
                return false;
            }
            return true;
        }
    }

    public static class Profile implements XMLWriteable {
        /** time in nanoseconds */
        final AtomicLong totalTime = new AtomicLong();

        final AtomicInteger totalCalls = new AtomicInteger();

        /** time in nanoseconds */
        final AtomicLong maxTime = new AtomicLong();

        final AtomicLong totalSquareMicroseconds = new AtomicLong();

        private final String className;

        Object maxContext;

        /**
         * @param className
         *            non null full qualified class name
         */
        public Profile(String className) {
            this.className = className;
        }

        public void handleCall(long nanoTime, Object context) {
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

        public long getTotalTime() {
            return totalTime.get();
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
                xmlOutput.addAttribute("standardDeviationMircosecondsPerInvocation", String.valueOf(timeStandardDeviation));
                xmlOutput.stopTag(true);
            }
        }
    }

    static class Clock {
        final Class<?> clazz;

        long startTimeNanos;

        long accumulatedTime;

        Clock(Class<?> clazz, long currentNanoTime) {
            this.clazz = clazz;
            startTimeNanos = currentNanoTime;
        }

        void accumulateTime(long currentNanoTime) {
            accumulatedTime += currentNanoTime - startTimeNanos;
        }

        void restartClock(long currentNanoTime) {
            startTimeNanos = currentNanoTime;
        }

    }

    final Stack<Clock> startTimes;

    final ConcurrentMap<Class<?>, Profile> profile;

    final Stack<Object> context = new Stack<Object>();

    public void startContext(Object context) {
        this.context.push(context);
    }

    public void endContext(Object context) {
        Object o = this.context.pop();
        assert o == context;
    }

    private Object getContext() {
        if (context.size() == 0) {
            return "";
        }
        try {
            return context.peek();
        } catch (EmptyStackException e) {
            return "";
        }
    }
    public void start(Class<?> c) {
        long currentNanoTime = System.nanoTime();

        Stack<Clock> stack = startTimes;
        if (!stack.isEmpty()) {
            stack.peek().accumulateTime(currentNanoTime);
        }
        stack.push(new Clock(c, currentNanoTime));
        // System.err.println("push " + c.getSimpleName());

    }

    public void end(Class<?> c) {
        // System.err.println("pop " + c.getSimpleName());
        long currentNanoTime = System.nanoTime();

        Stack<Clock> stack = startTimes;
        Clock ending = stack.pop();
        if (ending.clazz != c) {
            throw new AssertionError("Asked to end timing for " + c + " but top of stack is " + ending.clazz
                    + ", remaining stack is " + stack);
        }
        ending.accumulateTime(currentNanoTime);
        if (!stack.isEmpty()) {
            Clock restarting = stack.peek();
            restarting.restartClock(currentNanoTime);
        }
        long accumulatedTime = ending.accumulatedTime;
        if (accumulatedTime == 0) {
            return;
        }
        Profile counter = profile.get(c);
        if (counter == null) {
            counter = new Profile(c.getName());
            Profile counter2 = profile.putIfAbsent(c, counter);
            if (counter2 != null) {
                counter = counter2;
            }
        }
        counter.handleCall(accumulatedTime, getContext());

    }

    public static class ClassNameComparator implements Comparator<Class<?>>, Serializable {
        final protected Profiler profiler;

        public ClassNameComparator(Profiler p) {
            this.profiler = p;
        }

        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            try {
                return c1.getSimpleName().compareTo(c2.getSimpleName());
            } catch (RuntimeException e) {
                AnalysisContext.logError("Error comparing " + c1 + " and " + c2, e);
                int i1 = System.identityHashCode(c1);
                int i2 = System.identityHashCode(c2);
                if (i1 < i2) {
                    return -1;
                }
                if (i1 > i2) {
                    return 1;
                }
                return 0;
            }
        }
    }

    public static class TotalTimeComparator extends ClassNameComparator {

        public TotalTimeComparator(Profiler p) {
            super(p);
        }

        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            long v1 = profiler.getProfile(c1).totalTime.get();
            long v2 = profiler.getProfile(c2).totalTime.get();
            if (v1 < v2) {
                return 1;
            }
            if (v1 > v2) {
                return -1;
            }
            return super.compare(c1, c2);
        }
    }

    public static class TimePerCallComparator extends ClassNameComparator {
        public TimePerCallComparator(Profiler p) {
            super(p);
        }

        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            Profile profile1 = profiler.getProfile(c1);
            Profile profile2 = profiler.getProfile(c2);
            long time1 = profile1.totalTime.get() / profile1.totalCalls.get();
            long time2 = profile2.totalTime.get() / profile2.totalCalls.get();
            if (time1 < time2) {
                return 1;
            }
            if (time1 > time2) {
                return -1;
            }
            return super.compare(c1, c2);
        }
    }

    public static class TotalCallsComparator extends ClassNameComparator {
        public TotalCallsComparator(Profiler p) {
            super(p);
        }

        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            Profile profile1 = profiler.getProfile(c1);
            Profile profile2 = profiler.getProfile(c2);
            int calls1 = profile1.totalCalls.get();
            int calls2 = profile2.totalCalls.get();
            if (calls1 < calls2) {
                return 1;
            }
            if (calls1 > calls2) {
                return -1;
            }
            return super.compare(c1, c2);
        }
    }

    /**
     * Default implementation uses {@link TotalTimeComparator} and prints out
     * class statistics based on total time spent fot a class
     */
    public void report() {
        if (!REPORT) {
            return;
        }
        report(new TotalTimeComparator(this), new FilterByTime(10000000), System.err);
    }

    /**
     * @param reportComparator
     *            non null comparator instance which will be used to sort the
     *            report statistics
     */
    public void report(Comparator<Class<?>> reportComparator, Filter filter, PrintStream stream) {
        stream.println("PROFILE REPORT");
        try {

            TreeSet<Class<?>> treeSet = new TreeSet<Class<?>>(reportComparator);
            treeSet.addAll(profile.keySet());

            stream.printf("%8s  %8s %9s %s%n", "msecs", "#calls", "usecs/call", "Class");

            for (Class<?> c : treeSet) {
                Profile p = getProfile(c);
                long time = p.totalTime.get();
                int callCount = p.totalCalls.get();
                if (filter.accepts(p)) {
                    stream.printf("%8d  %8d  %8d %s%n", Long.valueOf(TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS)),
                            Integer.valueOf(callCount),
                            Long.valueOf(TimeUnit.MICROSECONDS.convert(time / callCount, TimeUnit.NANOSECONDS)),
                            c.getSimpleName());
                }

            }
            stream.flush();
        } catch (RuntimeException e) {
            System.err.println(e);
        }
    }

    /**
     * Clears the previously accumulated data. This method is public because it
     * can be accessed explicitely from clients (like Eclipse).
     * <p>
     * There is no need to clear profiler data after each run, because a new
     * profiler instance is used for each analysis run (see
     * {@link FindBugs2#execute()}).
     */
    public void clear() {
        profile.clear();
        startTimes.clear();
    }

    public Profile getProfile(Class<?> c) {
        Profile result = profile.get(c);
        if (result == null) {
            AnalysisContext.logError("Unexpected null profile for " + c.getName(), new NullPointerException());
            result = new Profile(c.getName());
            Profile tmp = profile.putIfAbsent(c, result);
            if (tmp != null) {
                return tmp;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.xml.XMLWriteable#writeXML(edu.umd.cs.findbugs.xml
     * .XMLOutput)
     */
    @Override
    public void writeXML(XMLOutput xmlOutput) throws IOException {
        xmlOutput.startTag("FindBugsProfile");
        xmlOutput.stopTag(false);
        TreeSet<Class<?>> treeSet = new TreeSet<Class<?>>(new TotalTimeComparator(this));
        treeSet.addAll(profile.keySet());
        long totalTime = 0;
        for (Profile p : profile.values()) {
            totalTime += p.totalTime.get();
        }

        long accumulatedTime = 0;

        for (Class<?> c : treeSet) {
            Profile p = getProfile(c);
            if (p == null) {
                continue;
            }
            p.writeXML(xmlOutput);
            accumulatedTime += p.totalTime.get();
            if (accumulatedTime > 3 * totalTime / 4) {
                break;
            }
        }
        xmlOutput.closeTag("FindBugsProfile");
    }
}
