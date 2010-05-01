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

package edu.umd.cs.findbugs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * Accumulate warnings that may occur at multiple source locations,
 * consolidating them into a single warning.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class BugAccumulator {

	private final BugReporter reporter;
	private final boolean performAccumulation;
	private final Map<BugInstance, Data> map = new HashMap<BugInstance, Data>();
	private final HashMap<String, BugInstance> hashes = new HashMap<String, BugInstance>();
	
	static class Data {
		public Data(int priority, SourceLineAnnotation primarySource) {
	        this.priority = priority;
	        this.primarySource = primarySource;
        }
		int priority;
		SourceLineAnnotation primarySource;
		List<SourceLineAnnotation> allSource = new LinkedList<SourceLineAnnotation>();
	}
	/**
	 * Constructor.
	 * 
	 * @param reporter the BugReporter to which warnings should eventually be reported
	 */
	public BugAccumulator(BugReporter reporter) {
		this.reporter = reporter;
		performAccumulation = AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.MERGE_SIMILAR_WARNINGS);
	}

	/**
	 * Accumulate a warning at given source location.
	 * 
	 * @param bug        the warning
	 * @param sourceLine the source location
	 */
	public void accumulateBug(BugInstance bug, SourceLineAnnotation sourceLine) {
		if (sourceLine == null)
			throw new NullPointerException("Missing source line");
		if (!performAccumulation) {
			reporter.reportBug(bug.addSourceLine(sourceLine));
			return;
		}
		int priority = bug.getPriority();
		bug.setPriority(Priorities.NORMAL_PRIORITY);
		String hash = bug.getInstanceHash();
		Data d = map.get(bug);
		if (d == null) {
			BugInstance conflictingBug = hashes.get(hash);
			if (conflictingBug != null) {
				if (conflictingBug.getPriority() <= priority)
					return;
				map.remove(conflictingBug);
			}
			d = new Data(priority, sourceLine);
			map.put(bug, d);
			hashes.put(hash, bug);
		} else if (d.priority > priority) {
			if (d.priority >= Priorities.LOW_PRIORITY)
				d.allSource.clear();
			d.priority = priority;
			d.primarySource = sourceLine;
		} else if (priority >= Priorities.LOW_PRIORITY && priority > d.priority)
			return;
		d.allSource.add(sourceLine);
	}

	/**
	 * Accumulate a warning at source location currently being visited by 
	 * given BytecodeScanningDetector.
	 * 
	 * @param bug     the warning
	 * @param visitor the BytecodeScanningDetector
	 */
	public void accumulateBug(BugInstance bug, BytecodeScanningDetector visitor) {
		SourceLineAnnotation source = SourceLineAnnotation.fromVisitedInstruction(visitor);
		accumulateBug(bug, source);
	}
	

	public Iterable<? extends BugInstance> uniqueBugs() {
		return map.keySet();
	}
	
	public Iterable<? extends SourceLineAnnotation> locations(BugInstance bug) {
		return map.get(bug).allSource;
	}
	
	/**
	 * Report accumulated warnings to the BugReporter.
	 * Clears all accumulated warnings as a side-effect.
	 */
	public void reportAccumulatedBugs() {
		for(Map.Entry<BugInstance, Data> e : map.entrySet()) {
			BugInstance bug = e.getKey();
			Data d = e.getValue();
			bug.setPriority(d.priority);
			bug.addSourceLine(d.primarySource);
			d.allSource.remove(d.primarySource);
			for (SourceLineAnnotation source : d.allSource) {
				bug.addSourceLine(source);
					bug.describe(SourceLineAnnotation.ROLE_ANOTHER_INSTANCE);
			}
			reporter.reportBug(bug);
		}
		clearBugs();
	}

	/**
     * Clear all accumulated bugs without reporting them
     */
    public void clearBugs() {
    	map.clear();
    	hashes.clear();
    }

	/**
     * @param bug
     * @param classContext
     * @param method
     * @param location
     */
    public void accumulateBug(BugInstance bug, ClassContext classContext, Method method, Location location) {
	    accumulateBug(bug, SourceLineAnnotation.fromVisitedInstruction(classContext, method, location));
	    
    }

	/**
     * @param bug
     * @param classContext
     * @param methodGen
     * @param sourceFile
     * @param location
     */
    public void accumulateBug(BugInstance bug, ClassContext classContext, MethodGen methodGen, String sourceFile,
            Location location) {
    	   accumulateBug(bug, SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, location.getHandle()));
    		 
	    
    }
}
