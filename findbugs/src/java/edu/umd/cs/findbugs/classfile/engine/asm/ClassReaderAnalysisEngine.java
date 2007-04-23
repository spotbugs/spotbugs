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

package edu.umd.cs.findbugs.classfile.engine.asm;

import org.objectweb.asm.ClassReader;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;

/**
 * Analysis engine to produce an ASM ClassReader for a class.
 * 
 * @author David Hovemeyer
 */
public class ClassReaderAnalysisEngine implements IClassAnalysisEngine {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache,
			ClassDescriptor descriptor) throws CheckedAnalysisException {

		ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);

		ClassReader classReader = new ClassReader(classData.getData());

		return classReader;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(ClassReader.class, this);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#retainAnalysisResults()
	 */
	public boolean retainAnalysisResults() {
		// Can be recomputed easily
		return false;
	}
}
