/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

public class UncallableMethodOfAnonymousClass extends BytecodeScanningDetector {

	BugReporter bugReporter;

	public UncallableMethodOfAnonymousClass(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	boolean isInnerClass = false;

	@Override
	public void visit(JavaClass obj) {
		isInnerClass = false;
		String name = getClassName();
		int i = name.lastIndexOf('$');
		if (i >= 0 && i + 1 < name.length()) {
			isInnerClass = Character.isDigit(name.charAt(i + 1));
		}
	}

	boolean definedInThisClassOrSuper(JavaClass clazz, String method)
			throws ClassNotFoundException {
		if (clazz == null)
			return false;
		// System.out.println("Checking to see if " + method + " is defined in "
		// + clazz.getClassName());
		for (Method m : clazz.getMethods())
			if (method.equals(m.getName() + ":" + m.getSignature()))
				return true;

		return definedInSuperClassOrInterface(clazz, method);

	}

	boolean definedInSuperClassOrInterface(JavaClass clazz, String method)
			throws ClassNotFoundException {
		if (clazz == null)
			return false;
		JavaClass superClass = clazz.getSuperClass();
		if (definedInThisClassOrSuper(superClass, method))
			return true;
		for (JavaClass i : clazz.getInterfaces())
			if (definedInThisClassOrSuper(i, method))
				return true;
		return false;
	}

	@Override
	public void visit(Method obj) {
		try {
			if (!isInnerClass)
				return;
			if (getMethodName().equals("<init>"))
				return;
			if (getMethodName().startsWith("access$"))
				return;
			if (obj.isSynthetic())
				return;
			if (obj.isPrivate())
				return;

			JavaClass clazz = getThisClass();
			XMethod xmethod = XFactory.createXMethod(clazz, obj);
			if (!CalledMethods.isCalled(xmethod)
					&& !definedInSuperClassOrInterface(clazz, obj.getName()
							+ ":" + obj.getSignature()))
				bugReporter.reportBug(new BugInstance("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS",
						NORMAL_PRIORITY).addClassAndMethod(this));

		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		}

	}

}
