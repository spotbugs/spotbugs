/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class InstantiateStaticClass extends BytecodeScanningDetector {
	private BugReporter bugReporter;

	public InstantiateStaticClass(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void sawOpcode(int seen) {
		try {
			if ((seen == INVOKESPECIAL) && getNameConstantOperand().equals("<init>") && getSigConstantOperand().equals("()V")) {
				XClass xClass = getXClassOperand();
				if (xClass == null) return;
				String clsName = getClassConstantOperand();
				if (clsName.equals("java/lang/Object"))
					return;

				// ignore superclass synthesized ctor calls
				if (getMethodName().equals("<init>") && (getPC() == 1))
					return;

				// ignore the typesafe enumerated constant pattern
				if (getMethodName().equals("<clinit>") && (getClassName().equals(clsName)))
					return;

				if (isStaticOnlyClass(xClass))
					bugReporter.reportBug(new BugInstance(this, "ISC_INSTANTIATE_STATIC_CLASS", LOW_PRIORITY).addClassAndMethod(
					        this).addSourceLine(this));
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}

	private boolean isStaticOnlyClass(XClass xClass) throws ClassNotFoundException {

			if (xClass.getInterfaceDescriptorList().length > 0)
				return false;
			ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
			if (superclassDescriptor == null) return false;
			String superClassName = superclassDescriptor.getClassName();
			if (!superClassName.equals("java/lang/Object"))
				return false;
			int staticCount = 0;

			List<? extends XMethod> methods = xClass.getXMethods();
			for (XMethod m : methods) {
				if (m.isStatic()) {
					staticCount++;
				} else if (!m.getName().equals("<init>") || !m.getSignature().equals("()V"))
					return false;
			}

			List<? extends XField> fields = xClass.getXFields();
			for (XField f : fields) {
				if (f.isStatic()) {
					staticCount++;
				} else if (!f.isPrivate())
					return false;
			}

			if (staticCount == 0)
				return false;
			return true;

	}

}
