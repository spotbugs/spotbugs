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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.rmi.CORBA.ClassDesc;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.AnalysisCacheToRepositoryAdapter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;

/**
 * Analysis engine to produce a BCEL JavaClass object for
 * a named class.
 * 
 * @author David Hovemeyer
 */
public class JavaClassAnalysisEngine implements IClassAnalysisEngine {
	private static final boolean DEBUG_MISSING_CLASSES =
		SystemProperties.getBoolean("findbugs.debug.missingclasses");
	private static final String JVM_VERSION = SystemProperties.getProperty("java.runtime.version");

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache,
			ClassDescriptor descriptor) throws CheckedAnalysisException {
		try {
			ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);
			JavaClass javaClass = new ClassParser(classData.getInputStream(), descriptor.toResourceName()).parse();
			if (false) {
				char jVersion = JVM_VERSION.charAt(2);
				if (jVersion < '5' && javaClass.getMajor() >= 49 || jVersion < '6' && javaClass.getMajor() >= 50)
					throw new CheckedAnalysisException(descriptor.toResourceName() + " is version " 
							+ javaClass.getMajor() + "." + javaClass.getMinor() + " but FindBugs is being run in a " + JVM_VERSION + " JVM");


			}
			// Make sure that the JavaClass object knows the repository
			// it was loaded from.
			javaClass.setRepository(Repository.getRepository());

			if (DEBUG_MISSING_CLASSES &&
					!(javaClass.getRepository() instanceof AnalysisCacheToRepositoryAdapter)) {
				throw new IllegalStateException("this should not happen");
			}

			return javaClass;
		} catch (IOException e) {
			throw new ResourceNotFoundException(descriptor.toResourceName(), e);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerClassAnalysisEngine(JavaClass.class, this);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#retainAnalysisResults()
	 */
	public boolean retainAnalysisResults() {
		// JavaClass objects must NOT be discarded - Subtypes compares
		// them by object identity.
		return true;
	}

}
