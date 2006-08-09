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

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

/**
 * Analysis engine to produce the ClassNode (ASM tree format)
 * for a class.
 * 
 * @author David Hovemeyer
 */
public class ClassNodeAnalysisEngine implements IClassAnalysisEngine {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache, ClassDescriptor descriptor) throws CheckedAnalysisException {
		ClassReader classReader = analysisCache.getClassAnalysis(ClassReader.class, descriptor);
		
		ICodeBaseEntry entry = analysisCache.getClassPath().lookupResource(descriptor.toResourceName());

		// One of the less-than-ideal features of ASM is that
		// invalid classfile format is indicated by a
		// random runtime exception rather than something
		// indicative of the real problem.
		try {
			ClassNode cn = new ClassNode();
			classReader.accept(cn, 0);
			return cn;
		} catch (RuntimeException e) {
			throw new InvalidClassFileFormatException(descriptor, entry, e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(ClassNode.class, this);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#retainAnalysisResults()
	 */
	public boolean retainAnalysisResults() {
		// Perhaps we should retain these, like we do with JavaClass objects?
		return false;
	}
}
