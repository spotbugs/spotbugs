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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;


public class InstantiateStaticClass extends BytecodeScanningDetector implements Constants2 {
	private BugReporter bugReporter;
	
	public InstantiateStaticClass(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
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
				
				JavaClass cls = Repository.lookupClass(clsName);
				if (cls.getInterfaceNames().length > 0)
					return;
				String superClassName = cls.getSuperclassName();
				if (!superClassName.equals("java.lang.Object"))
					return;
				
				Method[] methods = cls.getMethods();
				for (int i = 0; i < methods.length; i++) {
					Method m = methods[i];
					if (m.isStatic())
						continue;
					
					if (m.getName().equals("<init>")) {
						if (!m.getSignature().equals("()V"))
							return;
						
						Code c = m.getCode();

						if (c.getCode().length > 5)
							return;
					} else {
						return;
					}
				}
				
				Field[] fields = cls.getFields();
				for (int i = 0; i < fields.length; i++) {
					Field f = fields[i];
					if (f.isStatic())
						continue;
					
					if (!f.isPrivate())
						return;
				}
				
				bugReporter.reportBug(new BugInstance(this, "ISC_INSTANTIATE_STATIC_CLASS", LOW_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
			}
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}

}
