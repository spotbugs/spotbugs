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

package edu.umd.cs.findbugs;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.util.ClassPath;
import org.apache.bcel.util.Repository;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * An implementation of org.apache.bcel.util.Repository that
 * uses the AnalysisCache as its backing store.
 * 
 * @author David Hovemeyer
 */
public class AnalysisCacheToRepositoryAdapter implements Repository {
	/**
	 * Constructor.
	 */
	public AnalysisCacheToRepositoryAdapter() {
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#clear()
	 */
	public void clear() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#findClass(java.lang.String)
	 */
	public JavaClass findClass(String className) {
		@SlashedClassName String slashedClassName = ClassName.toSlashedClassName(className);
		ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(slashedClassName);
		return Global.getAnalysisCache().probeClassAnalysis(JavaClass.class, classDescriptor);
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#getClassPath()
	 */
	public ClassPath getClassPath() {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#loadClass(java.lang.String)
	 */
	public JavaClass loadClass(String className) throws ClassNotFoundException {
		if (className.length() == 0) 
			throw new IllegalArgumentException("Request to load empty class");
		className = ClassName.toSlashedClassName(className);
		ClassDescriptor classDescriptor = DescriptorFactory.instance().getClassDescriptor(className);
		try {
			return Global.getAnalysisCache().getClassAnalysis(JavaClass.class, classDescriptor);
		} catch (CheckedAnalysisException e) {
			throw new ClassNotFoundException("Exception while looking for class " + className, e);
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#loadClass(java.lang.Class)
	 */
	public JavaClass loadClass(Class cls) throws ClassNotFoundException {
		return loadClass(cls.getName());
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#removeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void removeClass(JavaClass arg0) {
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.apache.bcel.util.Repository#storeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void storeClass(JavaClass cls) {
		throw new UnsupportedOperationException();
	}
}
