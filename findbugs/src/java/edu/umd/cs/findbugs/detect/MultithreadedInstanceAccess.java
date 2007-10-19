/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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


import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;


public class MultithreadedInstanceAccess extends BytecodeScanningDetector
{
	private static final String STRUTS_ACTION_NAME = "org.apache.struts.action.Action";
	private static final String SERVLET_NAME = "javax.servlet.Servlet";
	private BugReporter bugReporter;
	private Set<JavaClass> mtClasses;
	private String mtClassName;
	private int monitorCount;
	private boolean writingField;
	private Set<String> alreadyReported;

	public MultithreadedInstanceAccess(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	private Set<JavaClass> getMtClasses() {
		if (mtClasses != null)
			return mtClasses;

		mtClasses = new HashSet<JavaClass>();
		try {
			mtClasses.add(Repository.lookupClass(STRUTS_ACTION_NAME));
		} catch (ClassNotFoundException cnfe) {
			//probably would be annoying to report
		}		
		try {
			mtClasses.add(Repository.lookupClass(SERVLET_NAME));
		} catch (ClassNotFoundException cnfe) {
			//probably would be annoying to report
		}

		return mtClasses;
	}

	@Override
		 public void visitClassContext(ClassContext classContext) {
		try {
			JavaClass cls = classContext.getJavaClass();
			String superClsName = cls.getSuperclassName();
			if ("java.lang.Object".equals(superClsName))
				return;

			if (STRUTS_ACTION_NAME.equals(superClsName)) {
				mtClassName = STRUTS_ACTION_NAME;
				super.visitClassContext(classContext);
			}
			else if (SERVLET_NAME.equals(superClsName)) {
				mtClassName = SERVLET_NAME;
				super.visitClassContext(classContext);
			}
			else {
				for (JavaClass mtClass : getMtClasses()) {
					/* note: We could just call cls.instanceOf(mtClass) and it would work for both
					 * classes and interfaces, but if mtClass is an interface it is more efficient
					 * to call cls.implementationOf() and since we're doing this on each visit that's
					 * what we'll do.
					 * also note: implementationOf(mtClass) throws an IllegalArgumentException when
					 * mtClass is not an interface. See bug#1428253. */
					if (mtClass.isClass() ? cls.instanceOf(mtClass) : cls.implementationOf(mtClass)) {
						mtClassName = mtClass.getClassName();
						super.visitClassContext(classContext);
						return;
					}
				}
			}
		} catch (Exception e) {
			//already reported
		}
	}

	@Override
		 public void visitMethod(Method obj) {
		monitorCount = 0;
		alreadyReported = new HashSet<String>();
		writingField = false;
	}

	@Override
		 public void visitCode(Code obj) {
		if (!getMethodName().equals("<init>") && !getMethodName().equals("init"))
			super.visitCode(obj);
	}

	@Override
		 public void sawField() {
		if ((monitorCount > 0) || (!writingField))
			return;

		ConstantFieldref fieldRef;
		Constant c = getConstantRefOperand();
		if (c instanceof ConstantFieldref) {
			fieldRef = (ConstantFieldref)c;

			String className = fieldRef.getClass(getConstantPool()).replace('.', '/');
			if (className.equals( this.getClassName())) {
				ConstantPool cp = getConstantPool();
				int nameAndTypeIdx = fieldRef.getNameAndTypeIndex();
				ConstantNameAndType ntc = (ConstantNameAndType)cp.getConstant(nameAndTypeIdx);
				int nameIdx = ntc.getNameIndex();

				Field[] flds = getClassContext().getJavaClass().getFields();

				for (Field fld : flds) {
					if (fld.getNameIndex() == nameIdx) {
						if (!fld.isStatic()) {
							ConstantUtf8 nameCons = (ConstantUtf8) cp.getConstant(nameIdx);
							ConstantUtf8 typeCons = (ConstantUtf8) cp.getConstant(ntc.getSignatureIndex());

							if (alreadyReported.contains(nameCons.getBytes()))
								return;
							alreadyReported.add(nameCons.getBytes());
							bugReporter.reportBug(new BugInstance(this,
									STRUTS_ACTION_NAME.equals(mtClassName) ? "MTIA_SUSPECT_STRUTS_INSTANCE_FIELD" : "MTIA_SUSPECT_SERVLET_INSTANCE_FIELD",
									LOW_PRIORITY)
									.addField(new FieldAnnotation(getDottedClassName(), nameCons.getBytes(), typeCons.getBytes(), false))
									.addClass(this).addSourceLine(this)
									);
						}
						break;
					}
				}
			}
		}
	}

	@Override
		 public void sawOpcode(int seen) {
		if (seen == MONITORENTER)
			monitorCount++;
		else if (seen == MONITOREXIT)
			monitorCount--;

		writingField = ((seen == PUTFIELD) || (seen == PUTFIELD_QUICK) || (seen == PUTFIELD_QUICK_W));
	}



}
