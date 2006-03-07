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

	private long cpuTime = -1;
	private long clockTime = -1;
	private long heap = -1;

	public Footprint() {
		pullData();
	}
	
	public Footprint(Footprint base) {
		pullData();
		cpuTime -= base.cpuTime;
		clockTime -= base.clockTime;
		heap -= base.heap;
	}
	
	private void pullData() {

		try {
			cpuTime = new OperatingSystemBeanWrapper().getProcessCpuTime();
		} catch (NoClassDefFoundError ncdfe) { cpuTime = -9; }
	      catch (ClassCastException cce) { cpuTime = -8; }
	      catch (Error error) { cpuTime = -7; } // catch the Error thrown when complied by the Eclipse compiler

		clockTime = new java.util.Date().getTime() ;		

		try {
			heap = new MemoryBeanWrapper().getHeapMemoryUsage();
		} catch (NoClassDefFoundError ncdfe) { heap = -9; }
	}

	public long getCpuTime() {
		return cpuTime;
	}
	public long getClockTime() {
		return clockTime;
	}
	public long getHeap() {
		return heap;
	}

	public String toString() {
		return "cpuTime="+cpuTime+", clockTime="+clockTime+", heap="+heap;
	}
	
	public static void main(String[] argv) {
		System.out.println(new Footprint());
	}
	
	// -------- begin static inner classes --------

	/** Wrapper so that possbile NoClassDefFoundError can be caught. Instantiating
	 *  this class will throw a NoClassDefFoundError on JDK 1.4 and earlier. */
	public static class MemoryBeanWrapper {
		java.lang.management.	MemoryMXBean memBean =
			java.lang.management.ManagementFactory.getMemoryMXBean();
		java.lang.management.MemoryUsage memUsage = memBean.getHeapMemoryUsage();
	  
		public long getHeapMemoryUsage() {
		  return memUsage.getUsed(); // gets current usage--some stuff may have been reclaimed
		}
	}
	
	/** Wrapper so that possbile NoClassDefFoundError can be caught. Instantiating this
	 *  class will throw a NoClassDefFoundError os JDK 1.4 and earlier, or will throw a
	 *  ClassCastException on a non-sun 1.5-compliant JDK where the osBean is not a sunBean.
	 *  (If compiled by Eclipse, instantiating it will throw an unsubclassed java.lang.Error.) */
	public static class OperatingSystemBeanWrapper {
		java.lang.management.	OperatingSystemMXBean osBean =
			java.lang.management.ManagementFactory.getOperatingSystemMXBean();
		// next line compiles fine with sun JDK 1.5 but the eclipse compiler complains: The type
		// OperatingSystemMXBean is not accessible due to restriction on required library classes.jar
		com.sun.management.OperatingSystemMXBean sunBean = (com.sun.management.OperatingSystemMXBean)osBean;
	  
		public long getProcessCpuTime() {
			return sunBean.getProcessCpuTime();
		}
	}
	
}
