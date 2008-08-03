/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;

import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
* Class to maintain a snapshot of a processes's time and memory usage.
* 
* This uses some JDK 1.5 APIs so must be careful that it doesn't cause
* any harm when run from 1.4.
*
* @see FindBugs
* @author Brian Cole
*/
public class Footprint {

	private long cpuTime = -1;   // in nanoseconds
	private long clockTime = -1; // in milliseconds
	private long peakMem = -1;   // in bytes
	private long collectionTime = -1; // in milliseconds

	public Footprint() {
		pullData();
	}

	/** uses deltas from base for cpuTime and clockTime (but not peakMemory) */
	public Footprint(Footprint base) {
		pullData();
		if (cpuTime >= 0) {
			cpuTime = (base.cpuTime >= 0) ? cpuTime-base.cpuTime : base.cpuTime;
		}
		if (clockTime >= 0) {
			clockTime = (base.clockTime >= 0) ? clockTime-base.clockTime : base.clockTime;
		}
		// leave peakMem alone
		if (collectionTime >= 0) {
			collectionTime = (base.collectionTime >= 0) ? collectionTime-base.collectionTime : base.collectionTime;
		}
	}

	private void pullData() {

		try {
			cpuTime = new OperatingSystemBeanWrapper().getProcessCpuTime();
		} catch (NoClassDefFoundError ncdfe) { cpuTime = -9; }
		  catch (ClassCastException cce) { cpuTime = -8; }
		  catch (Error error) { cpuTime = -7; } // catch possible Error thrown when complied by the Eclipse compiler

		clockTime = System.currentTimeMillis(); // or new java.util.Date().getTime()	;

		try {
			peakMem = new MemoryBeanWrapper().getPeakUsage();
		} catch (NoClassDefFoundError ncdfe) { peakMem = -9; }

		try {
			collectionTime = new CollectionBeanWrapper().getCollectionTime();
		} catch (NoClassDefFoundError ncdfe) { collectionTime = -9; }
	}

	public long getCpuTime() {
		return cpuTime;
	}
	public long getClockTime() {
		return clockTime;
	}
	public long getPeakMemory() {
		return peakMem;
	}
	public long getCollectionTime() {
		return collectionTime;
	}

	@Override
	public String toString() {
		return "cpuTime="+cpuTime+", clockTime="+clockTime+", peakMemory="+peakMem;
	}

	public static void main(String[] argv) {
		System.out.println(new Footprint());
	}

	// -------- begin static inner classes --------

	/** Wrapper so that possbile NoClassDefFoundError can be caught. Instantiating
	 *  this class will throw a NoClassDefFoundError on JDK 1.4 and earlier. */
	public static class MemoryBeanWrapper {
		List<MemoryPoolMXBean> mlist = ManagementFactory.getMemoryPoolMXBeans();
		//java.lang.management.MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
		//java.lang.management.MemoryUsage memUsage = memBean.getHeapMemoryUsage();

		public long getPeakUsage() {
			long sum = 0;
			// problem: sum of the peaks is not necessarily the peak of the sum.
			// For example, objects migrate from the 'eden' to the 'survivor' area.
			for (MemoryPoolMXBean mpBean : mlist) {
				try {
					java.lang.management.MemoryUsage memUsage = mpBean.getPeakUsage();
					if (memUsage != null) sum += memUsage.getUsed(); // or getCommitted()
					// System.out.println(mpBean.getType()+", "+mpBean.getName()+", "+memUsage.getUsed());
					//System.out.println("Memory type="+mpBean.getType()+", Pool name="+mpBean.getName()+", Memory usage="+mpBean.getPeakUsage());
				} catch (RuntimeException e) {
					AnalysisContext.logError("Error getting peak usage", e);
				}
			}
			// System.out.println();
			return sum;
		}
	}

	/** Wrapper so that possbile NoClassDefFoundError can be caught. Instantiating this
	 *  class will throw a NoClassDefFoundError on JDK 1.4 and earlier, or will throw a
	 *  ClassCastException on a 1.5-compliant non-sun JRE where the osBean is not a sunBean.
	 *  (If compiled by Eclipse, instantiating it will throw an unsubclassed java.lang.Error.) */
	public static class OperatingSystemBeanWrapper {
		java.lang.management.	OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
		// next line compiles fine with sun JDK 1.5 but the eclipse compiler may complain "The type
		// OperatingSystemMXBean is not accessible due to restriction on required library classes.jar"
		// depending on the contents of the .classpath file.
		com.sun.management.OperatingSystemMXBean sunBean = (com.sun.management.OperatingSystemMXBean)osBean;

		public long getProcessCpuTime() {
			return sunBean.getProcessCpuTime();
		}
	}

	/** Wrapper so that possbile NoClassDefFoundError can be caught. Instantiating
	 *  this class will throw a NoClassDefFoundError on JDK 1.4 and earlier. */
	public static class CollectionBeanWrapper {
		List<GarbageCollectorMXBean> clist = ManagementFactory.getGarbageCollectorMXBeans();

		public long getCollectionTime() {
			long sum = 0;
			for (GarbageCollectorMXBean gcBean : clist) {
				sum += gcBean.getCollectionTime();
			}
			return sum;
		}
	}

}
