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

import java.util.Comparator;
import java.util.Map;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * @author pugh
 */
public class Profiler {

	final static boolean REPORT = SystemProperties.getBoolean("profiler.report");

	private static Profiler instance = new Profiler();

	private Profiler() {
		if (REPORT) 
			System.err.println("Profiling activated");
	}

	public static Profiler getInstance() {
		return instance;
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

	ConcurrentHashMap<Class<?>, AtomicLong> profile = new ConcurrentHashMap<Class<?>, AtomicLong>();
	ConcurrentHashMap<Class<?>, AtomicInteger> callCountProfile = new ConcurrentHashMap<Class<?>, AtomicInteger>();

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
		AtomicLong counter = profile.get(c);
		if (counter == null) {
			counter = new AtomicLong();
			AtomicLong counter2 = profile.putIfAbsent(c, counter);
			if (counter2 != null) {
	            counter = counter2;
            }
		}
		counter.addAndGet(accumulatedTime);
		AtomicInteger callCount = callCountProfile.get(c);
		if (callCount == null) {
			callCount = new AtomicInteger();
			AtomicInteger counter3 = callCountProfile.putIfAbsent(c, callCount);
			if (counter3 != null) {
	            callCount = counter3;
            }
		}
		callCount.incrementAndGet();
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

	public void report() {
		if (!REPORT) {
	        return;
        }
		System.err.println("PROFILE REPORT");
		try {
			Comparator<Pair<Class<?>, AtomicLong>> c = new Comparator<Pair<Class<?>, AtomicLong>>() {

				public int compare(Pair<Class<?>, AtomicLong> o1, Pair<Class<?>, AtomicLong> o2) {
					long v1 = o1.second.get();
					long v2 = o2.second.get();
					if (v1 < v2) {
	                    return -1;
                    }
					if (v1 > v2) {
	                    return 1;
                    }
					return o1.first.getName().compareTo(o2.first.getName());
				}

			};
			TreeSet<Pair<Class<?>, AtomicLong>> treeSet = new TreeSet<Pair<Class<?>, AtomicLong>>(c);
			for (Map.Entry<Class<?>, AtomicLong> e : profile.entrySet()) {

				treeSet.add(new Pair<Class<?>, AtomicLong>(e.getKey(), e.getValue()));
			}
			Pair<Class<?>, AtomicLong> prev = null;
            System.err.printf("%8s  %8s %9s %s\n", "msecs", "#calls", "usecs/call",
					"Class");
        
			for (Pair<Class<?>, AtomicLong> e : treeSet) {
				long time = e.second.get() / 1000000;
				AtomicInteger callCount = callCountProfile.get(e.first);
				if (time > 0 && callCount != null) {
	                System.err.printf("%8d  %8d  %8d %s\n",time,  callCount.get(), e.second.get()/callCount.get()/1000,
							e.first.getSimpleName());
                }
				if (false && prev != null) {
	                System.err.println(c.compare(prev, e) + " " + prev.second.get() + "  " + e.second.get());
                }
				prev = e;
			}
			System.err.flush();
		} catch (RuntimeException e) {
			System.err.println(e);
		} finally {
			profile.clear();
			callCountProfile.clear();
			startTimes.get().clear();
		}
	}
}
