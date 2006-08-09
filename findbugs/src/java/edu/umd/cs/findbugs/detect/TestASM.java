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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.detect;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * @author David Hovemeyer
 */
public class TestASM implements Detector2 {
	
	public TestASM(BugReporter bugReporter) {
		
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#finishPass()
	 */
	public void finishPass() {
		// do nothing
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#getDetectorClassName()
	 */
	public String getDetectorClassName() {
		return getClass().getName();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector2#visitClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public void visitClass(ClassDescriptor classDescriptor)
			throws CheckedAnalysisException {
		
		System.out.println("TestASM visiting " + classDescriptor);
		
		ClassReader cr = Global.getAnalysisCache().getClassAnalysis(ClassReader.class, classDescriptor);
		ClassNode cn = new ClassNode();
		
		cr.accept(cn, 0);
		
	}

}
