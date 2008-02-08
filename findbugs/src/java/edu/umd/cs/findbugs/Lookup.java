/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import javax.annotation.CheckForNull;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class Lookup
		implements Constants2 {
	
	
	private static Subtypes2 subtypes2() {
		return AnalysisContext.currentAnalysisContext().getSubtypes2();
	}

	private static XClass getXClass(ClassDescriptor c) throws CheckedAnalysisException {
		return Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
	}

	public static XClass findSuperImplementor(XClass clazz, String name, String signature, boolean isStatic,
	        BugReporter bugReporter) {

		try {
			return findSuperImplementor(clazz, name, signature, isStatic);
		} catch (MissingClassException e) {
			bugReporter.reportMissingClass(e.getClassDescriptor());
			return clazz;
		} catch (CheckedAnalysisException e) {
			bugReporter.logError("Error finding " + clazz + "." + name + signature, e);
			return clazz;
		}

	}

	public static XClass findImplementor(XClass clazz, String name, String signature, boolean isStatic,
	        BugReporter bugReporter) {

		try {
			return findImplementor(clazz, name, signature, isStatic);
		} catch (MissingClassException e) {
			bugReporter.reportMissingClass(e.getClassDescriptor());
			return clazz;
		} catch (CheckedAnalysisException e) {
			bugReporter.logError("Error finding " + clazz + "." + name + signature, e);
			return clazz;
		}

	}

	public static XClass findSuperImplementor(XClass clazz, String name, String signature, boolean isStatic)
	        throws CheckedAnalysisException {

		ClassDescriptor superclassDescriptor = clazz.getSuperclassDescriptor();
		if (superclassDescriptor == null) return clazz;
		return findImplementor(getXClass(superclassDescriptor), name, signature, isStatic);
	}

	public static XClass findImplementor(XClass clazz, String name, String signature, boolean isStatic)
	        throws CheckedAnalysisException {
		XMethod m = clazz.findMethod(name, signature, isStatic);
		if (m != null)
			return clazz;
		return findSuperImplementor(clazz, name, signature, isStatic);
	}
	
	public static @CheckForNull
	JavaClass findSuperDefiner(JavaClass clazz, String name, String signature, BugReporter bugReporter) {
		try {
			JavaClass c = clazz;
			while (true) {
				c = c.getSuperClass();
				if (c == null)
					return null;
				Method m = findImplementation(c, name, signature);
				if (m != null) {
					return c;
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return null;
		}
	}
	public static @CheckForNull JavaClass
			findSuperImplementor(JavaClass clazz, String name, String signature, BugReporter bugReporter) {
		try {
			JavaClass c = clazz;
			while (true) {
				c = c.getSuperClass();
				if (c == null)
					return null;
				Method m = findImplementation(c, name, signature);
				if (m != null) {
					if ((m.getAccessFlags() & ACC_ABSTRACT) != 0)
						return null;
					else
						return c;
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return null;
		}
	}

	public static String
			findSuperImplementor(String clazz, String name, String signature, BugReporter bugReporter) {
		try {
			JavaClass c =
					findImplementor(Repository.getSuperClasses(clazz),
							name, signature);
			return (c != null) ? c.getClassName() : clazz;
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return clazz;
		}
	}

	public static @CheckForNull JavaClass
			findImplementor(JavaClass[] clazz, String name, String signature) {

		for (JavaClass aClazz : clazz) {
			Method m = findImplementation(aClazz, name, signature);
			if (m != null) {
				if ((m.getAccessFlags() & ACC_ABSTRACT) != 0)
					return null;
				else
					return aClazz;
			}
		}
		return null;
	}

	public static Method
			findImplementation(JavaClass clazz, String name, String signature) {
		Method[] m = clazz.getMethods();
		for (Method aM : m)
			if (aM.getName().equals(name)
					&& aM.getSignature().equals(signature)
					&& ((aM.getAccessFlags() & ACC_PRIVATE) == 0)
					&& ((aM.getAccessFlags() & ACC_STATIC) == 0)
					)
				return aM;
		return null;
	}
}
