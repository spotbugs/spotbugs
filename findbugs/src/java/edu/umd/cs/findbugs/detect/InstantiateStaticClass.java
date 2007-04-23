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


import edu.umd.cs.findbugs.*;
import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;


public class InstantiateStaticClass extends BytecodeScanningDetector {
	private BugReporter bugReporter;

	Map<String,Boolean> isStaticClass = new HashMap<String,Boolean>();

	public InstantiateStaticClass(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
		 public void sawOpcode(int seen) {
		try {
			if ((seen == INVOKESPECIAL)
			&&  getNameConstantOperand().equals("<init>")
			&&  getSigConstantOperand().equals("()V")) {
				String clsName = getClassConstantOperand();
				if (clsName.equals("java/lang/Object"))
					return;

				//ignore superclass synthesized ctor calls
				if (getMethodName().equals("<init>") && (getPC() == 1))
					return;

				//ignore the typesafe enumerated constant pattern
				if (getMethodName().equals("<clinit>") && (getClassName().equals(clsName)))
					return;

				Boolean b = isStaticClass.get(clsName);
				if (b == null) {
					b = Boolean.valueOf(isStaticOnlyClass(clsName));
					isStaticClass.put(clsName, b);
					}
				if (b)

				bugReporter.reportBug(new BugInstance(this, "ISC_INSTANTIATE_STATIC_CLASS", LOW_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}

   private boolean isStaticOnlyClass(String clsName) throws ClassNotFoundException {
				   clsName = clsName.replace('/', '.');
				JavaClass cls = Repository.lookupClass(clsName);
				if (cls.getInterfaceNames().length > 0)
					return false;
				String superClassName = cls.getSuperclassName();
				if (!superClassName.equals("java.lang.Object"))
					return false;

				Method[] methods = cls.getMethods();
				int staticCount = 0;
	   for (Method m : methods) {
		   if (m.isStatic()) {
			   staticCount++;
			   continue;
		   }

		   if (m.getName().equals("<init>")) {
			   if (!m.getSignature().equals("()V"))
				   return false;

			   Code c = m.getCode();

			   if (c.getCode().length > 5)
				   return false;
		   } else {
			   return false;
		   }
	   }

				Field[] fields = cls.getFields();
	   for (Field f : fields) {
		   if (f.isStatic()) {
			   staticCount++;
			   continue;
		   }

		   if (!f.isPrivate())
			   return false;
	   }

				if (staticCount == 0) return false;
				return true;
				}

}
