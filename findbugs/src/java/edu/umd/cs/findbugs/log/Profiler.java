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
import java.util.Comparator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * @author pugh
 */
public class Profiler implements XMLWriteable {

	final static boolean REPORT = SystemProperties.getBoolean("profiler.report");

	private static Profiler instance = new Profiler();

	private Profiler() {
		if (REPORT)
			System.err.println("Profiling activated");
	}

	public static Profiler getInstance() {
		return instance;
	}

	static class Profile {
		final AtomicLong totalTime = new AtomicLong();

		final AtomicInteger totalCalls = new AtomicInteger();

		final AtomicLong maxTime = new AtomicLong();

		final AtomicLong totalSquareMicroseconds = new AtomicLong();

		public void handleCall(long nanoTime) {
			totalCalls.incrementAndGet();
			totalTime.addAndGet(nanoTime);
			long oldMax = maxTime.get();
			if (nanoTime > oldMax)
				maxTime.compareAndSet(oldMax, nanoTime);
			long microseconds = TimeUnit.MICROSECONDS.convert(nanoTime, TimeUnit.NANOSECONDS);
			totalSquareMicroseconds.addAndGet(microseconds * microseconds);
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

	ThreadLocal<Stack<Clock>> startTimes = new ThreadLocal<Stack<Clock>>() {
		@Override
		public Stack<Clock> initialValue() {
			return new Stack<Clock>();
		}
	};

	ConcurrentHashMap<Class<?>, Profile> profile = new ConcurrentHashMap<Class<?>, Profile>();

	public void start(Class<?> c) {
		long currentNanoTime = System.nanoTime();

		Stack<Clock> stack = startTimes.get();
		if (!stack.isEmpty()) {
			stack.peek().accumulateTime(currentNanoTime);
		}
		stack.push(new Clock(c, currentNanoTime));
		// System.err.println("push " + c.getSimpleName());

	}

	public void end(Class<?> c) {
		// System.err.println("pop " + c.getSimpleName());
		long currentNanoTime = System.nanoTime();

		Stack<Clock> stack = startTimes.get();
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
			counter = new Profile();
			Profile counter2 = profile.putIfAbsent(c, counter);
			if (counter2 != null) {
				counter = counter2;
			}
		}
		counter.handleCall(accumulatedTime);

	}

	static class Pair<V1, V2> {
		final V1 first;

		final V2 second;

		Pair(V1 first, V2 second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public String toString() {
			return first + ":" + second;
		}
	}

	class TotalTimeComparator implements Comparator<Class<?>> {
		public int compare(Class<?> c1, Class<?> c2) {
			long v1 = getProfile(c1).totalTime.get();
			long v2 = getProfile(c2).totalTime.get();
			if (v1 < v2) {
				return -1;
			}
			if (v1 > v2) {
				return 1;
			}
			return c1.getName().compareTo(c2.getName());
		}
	}

	public void report() {
		if (!REPORT) {
			return;
		}
		System.err.println("PROFILE REPORT");
		try {

			TreeSet<Class<?>> treeSet = new TreeSet<Class<?>>(new TotalTimeComparator());
			treeSet.addAll(profile.keySet());

			System.err.printf("%8s  %8s %9s %s\n", "msecs", "#calls", "usecs/call", "Class");

			for (Class<?> c : treeSet) {
				Profile p = getProfile(c);
				long time = p.totalTime.get();
				int callCount = p.totalCalls.get();
				if (time > 10000000) {
					System.err.printf("%8d  %8d  %8d %s\n", TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS), callCount,
					        TimeUnit.MICROSECONDS.convert(time / callCount, TimeUnit.NANOSECONDS), c.getSimpleName());
				}

			}
			System.err.flush();
		} catch (RuntimeException e) {
			System.err.println(e);
		} finally {
			clear();
		}
	}

	/**
	 * Clears the previously accumulated data. This method is public because it can be 
	 * accessed from clients (like Eclipse)  
	 */
	public void clear() {
	    profile.clear();
	    startTimes.get().clear();
    }

	Profile getProfile(Class<?> c) {
		Profile result = profile.get(c);
		if (result == null) {
			AnalysisContext.logError("Unexpected null profile for " + c.getName(), new NullPointerException());
			result = new Profile();
			profile.putIfAbsent(c, result);
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
	public void writeXML(XMLOutput xmlOutput) throws IOException {
		xmlOutput.startTag("FindBugsProfile");
		xmlOutput.stopTag(false);
		TreeSet<Class<?>> treeSet = new TreeSet<Class<?>>(new TotalTimeComparator());
		treeSet.addAll(profile.keySet());

		for (Class<?> c : treeSet) {
			Profile p = getProfile(c);
			if (p == null)
				continue;
			long time = p.totalTime.get();
			int callCount = p.totalCalls.get();
			long maxTimeMicros = TimeUnit.MICROSECONDS.convert(p.maxTime.get(), TimeUnit.NANOSECONDS);
			long timeMillis = TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
			long timeMicros = TimeUnit.MICROSECONDS.convert(time, TimeUnit.NANOSECONDS);

			long averageTimeMicros = timeMicros / callCount;
			long totalSquareMicros = p.totalSquareMicroseconds.get();
			long averageSquareMicros = totalSquareMicros / callCount;
			long timeVariance = averageSquareMicros - averageTimeMicros * averageTimeMicros;
			long timeStandardDeviation = (long) Math.sqrt(timeVariance);
			if (timeMillis > 10) {
				xmlOutput.startTag("ClassProfile");

				xmlOutput.addAttribute("name", c.getName());
				xmlOutput.addAttribute("totalMilliseconds", String.valueOf(timeMillis));
				xmlOutput.addAttribute("invocations", String.valueOf(callCount));
				xmlOutput.addAttribute("avgMicrosecondsPerInvocation", String.valueOf(averageTimeMicros));
				xmlOutput.addAttribute("maxMicrosecondsPerInvocation", String.valueOf(maxTimeMicros));
				xmlOutput.addAttribute("standardDeviationMircosecondsPerInvocation", String.valueOf(timeStandardDeviation));
				xmlOutput.stopTag(true);

			}

		}
		xmlOutput.closeTag("FindBugsProfile");
	}
}
