/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.objectweb.asm.ClassReader;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.asm.FBClassReader;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ClassNameMismatchException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.RecomputableClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;

/**
 * Analysis engine to produce the ClassInfo for a loaded class.
 * We parse just enough information from the classfile to
 * get the needed information.
 * 
 * @author David Hovemeyer
 */
public class ClassInfoAnalysisEngine implements IClassAnalysisEngine<XClass> {
	/*
	private static final boolean USE_ASM_CLASS_PARSER = SystemProperties.getBoolean("findbugs.classparser.asm");
	static {
		if (USE_ASM_CLASS_PARSER) {
			System.out.println("Using ClassParserUsingASM");
		}
	}
	*/

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public ClassInfo analyze(IAnalysisCache analysisCache,
			ClassDescriptor descriptor) throws CheckedAnalysisException {

		if (descriptor instanceof ClassInfo) return (ClassInfo) descriptor;
		ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);

		
		// Read the class info

		FBClassReader reader = analysisCache.getClassAnalysis(FBClassReader.class, descriptor);
		ClassParserInterface parser  = new ClassParserUsingASM(reader, descriptor, classData.getCodeBaseEntry());
		
		ClassInfo.Builder classInfoBuilder = new ClassInfo.Builder();
		parser.parse(classInfoBuilder);
		ClassInfo classInfo = classInfoBuilder.build();

		if (!classInfo.getClassDescriptor().equals(descriptor)) {
			throw new ClassNameMismatchException(
					descriptor,
					classInfo.getClassDescriptor(),
					classData.getCodeBaseEntry());
		}
		return classInfo;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(XClass.class, this);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#canRecompute()
	 */
	public boolean canRecompute() {
		// ClassInfo objects serve as XClass objects,
		// which we want interned.  So, they are never purged from the cache.
	    return false;
	}
}
