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

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * A special Detector2 class designed to run some JUnit test code.
 * 
 * @author David Hovemeyer
 */
public class JUnitDetectorAdapter implements Detector2 {
	
	private BugReporter bugReporter;
	private Throwable throwable;
	private boolean testExecuted;
	
	private static InheritableThreadLocal<JUnitDetectorAdapter> instance =
		new InheritableThreadLocal<JUnitDetectorAdapter>();
	private static InheritableThreadLocal<RunnableWithExceptions> runnableInstance =
		new InheritableThreadLocal<RunnableWithExceptions>();
	
	public JUnitDetectorAdapter(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		instance.set(this);
	}
	
	public static JUnitDetectorAdapter instance() {
		return instance.get();
	}
	
	/**
     * @param runnable The runnable to set.
     */
    public static void setRunnable(RunnableWithExceptions runnable) {
	    runnableInstance.set(runnable);
    }

    public void finishTest() throws Throwable {
    	if (throwable != null) {
    		throw throwable;
    	}
    }
    
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#finishPass()
	 */
	public void finishPass() {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#getDetectorClassName()
	 */
	public String getDetectorClassName() {
		return this.getClass().getName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#visitClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		// Only execute the test once
		if (testExecuted) {
			return;
		}
		testExecuted= true;
		
		try {
			runnableInstance.get().run();
		} catch (Throwable e) {
//			e.printStackTrace();
			throwable = e;
		}
	}

}
