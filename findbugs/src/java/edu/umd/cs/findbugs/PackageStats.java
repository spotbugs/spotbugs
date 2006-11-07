/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, Mike Fagan <mfagan@tde.com>
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

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.umd.cs.findbugs.xml.XMLOutput;
import edu.umd.cs.findbugs.xml.XMLWriteable;

/**
 * Class to store package bug statistics.
 *
 * @author Mike Fagan
 * @author Jay Dunning
 */
public class PackageStats implements XMLWriteable {

	public static class ClassStats implements XMLWriteable, Cloneable {
		private String name;
		private boolean isInterface;
		// nBugs[0] is total; nBugs[n] is total for bug priority n
		private int[] nBugs = new int[] { 0, 0, 0, 0, 0 };
		private int size;

		public ClassStats(String name) {
			this.name = name;
		}
		
		@Override
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				// can't happen
				throw new AssertionError(e);
			}
		}

		public void setInterface(boolean isInterface) {
			this.isInterface = isInterface;
		}
		
		public void setSize(int size) {
			this.size = size;
		}
		
		public void addError(BugInstance bug) {
			++nBugs[bug.getPriority()];
			++nBugs[0];
		}
		public int getTotalBugs() {
			return nBugs[0];
		}
		public int getBugsAtPriority(int p) {
			return nBugs[p];
		}
		public int size() {
			return size;
		}
		public String getName() {
			return name;
		}
		public void writeXML(XMLOutput xmlOutput) throws IOException {
			xmlOutput.startTag("ClassStats");

			xmlOutput.addAttribute("class", name);
			xmlOutput.addAttribute("interface", String.valueOf(isInterface));
			xmlOutput.addAttribute("size", String.valueOf(size));
			xmlOutput.addAttribute("bugs", String.valueOf(nBugs[0]));
			writeBugPriorities(xmlOutput, nBugs);			

			xmlOutput.stopTag(true);
		}

		/**
		 * 
		 */
		public void clearBugCounts() {
			for(int i = 0; i < nBugs.length; i++) nBugs[i] = 0;
			
		}
	}

	public static final String ELEMENT_NAME = "PackageStats";
	public static final int ALL_ERRORS = 0;
	private final String packageName;
	// nBugs[0] is total; nBugs[n] is total for bug priority n
	private int[] nBugs = new int[] { 0, 0, 0, 0, 0 };
	private int size;

	// list of errors for this package
	//private LinkedList<BugInstance> packageErrors = new LinkedList<BugInstance>();

	// all classes and interfaces in this package
	private SortedMap<String, ClassStats> packageMembers = 
		new TreeMap<String, ClassStats>();

	public PackageStats(String packageName) {
		this.packageName = packageName;
	}

	public Collection<ClassStats> getClassStats() {
		return packageMembers.values();
	}
	public int getTotalBugs() {
		return nBugs[0];
	}
	public int size() {
		return size;
	}
	public int getBugsAtPriority(int p) {
		return nBugs[p];
	}
	private ClassStats getClassStats(String name) {
		ClassStats result = packageMembers.get(name);
		if ( result == null ) {
			result = new ClassStats(name);
			packageMembers.put(name, result);
		}

		return result;
	}
	
	public ClassStats getClassStatsOrNull(String name) {
		ClassStats result = packageMembers.get(name);
		return result;
	}
	public void addError(BugInstance bug) {
		if (bug.getPriority() >= nBugs.length) return;
		++nBugs[bug.getPriority()];
		++nBugs[0];

		getClassStats(bug.getPrimaryClass().getClassName()).addError(bug);
	}

	public void addClass(String name, boolean isInterface, int size) {
		ClassStats classStats = getClassStats(name);
		classStats.setInterface(isInterface);
		classStats.setSize(size);
		this.size += size;
	}

	public String getPackageName() {
		return packageName;
	}

	public void writeXML(XMLOutput xmlOutput) throws IOException{

		xmlOutput.startTag(ELEMENT_NAME);

		xmlOutput.addAttribute("package", packageName);
		xmlOutput.addAttribute("total_bugs", String.valueOf(nBugs[0]));
		xmlOutput.addAttribute("total_types",
			String.valueOf(packageMembers.size()));
		xmlOutput.addAttribute("total_size", String.valueOf(size));
		writeBugPriorities(xmlOutput, nBugs);

		xmlOutput.stopTag(false);

		for (ClassStats classStats : packageMembers.values()) {
			classStats.writeXML(xmlOutput);
		}

		xmlOutput.closeTag(ELEMENT_NAME);
	}

	/**
	 *  Add priority attributes to a started tag. 
	 *  Each priority at offset n, where n &gt; 0, is output using
	 *  attribute priority_n if the value at offset n is greater than
	 *  zero. 
	 *  
	 *  @param xmlOutput an output stream for which startTag has been
	 *				   called but stopTag has not.
	 *  @param bugs	  an array for which the element at offset n is 
	 *				   the number of bugs for priority n.
	 */
	public static void writeBugPriorities(XMLOutput xmlOutput, int[] bugs) throws IOException {
		int i = bugs.length;
		while ( --i > 0 ) {
			if ( bugs[i] > 0 ) {
				xmlOutput.addAttribute("priority_" + i, 
					String.valueOf(bugs[i]));
			}
		}
	}

	/**
	 * 
	 */
	public void clearBugCounts() {
		for(int i = 0; i < nBugs.length; i++)
			nBugs[i] = 0;
		
		for(ClassStats classStats :  packageMembers.values()) {
			classStats.clearBugCounts();
		}
		
	}
}

// vim:ts=4
