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

import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class Lookup
        implements Constants2 {
	public static JavaClass
	        findSuperImplementor(JavaClass clazz, String name, String signature, BugReporter bugReporter) {
		try {
			JavaClass c =
			        findImplementor(Repository.getSuperClasses(clazz),
			                name, signature);
			return c;
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return clazz;
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

	public static JavaClass
	        findImplementor(JavaClass[] clazz, String name, String signature) {

		for (int i = 0; i < clazz.length; i++) {
			Method m = findImplementation(clazz[i], name, signature);
			if (m != null) {
				if ((m.getAccessFlags() & ACC_ABSTRACT) != 0)
					return null;
				else
					return clazz[i];
			}
		}
		return null;
	}

	public static Method
	        findImplementation(JavaClass clazz, String name, String signature) {
		Method[] m = clazz.getMethods();
		for (int i = 0; i < m.length; i++)
			if (m[i].getName().equals(name)
			        && m[i].getSignature().equals(signature)
			        && ((m[i].getAccessFlags() & ACC_PRIVATE) == 0)
			        && ((m[i].getAccessFlags() & ACC_STATIC) == 0)
			)
				return m[i];
		return null;
	}
}
