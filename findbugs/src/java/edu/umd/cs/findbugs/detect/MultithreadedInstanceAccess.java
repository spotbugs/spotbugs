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
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.Constants2;


public class MultithreadedInstanceAccess extends BytecodeScanningDetector implements Constants2, StatelessDetector
{
	private BugReporter bugReporter;
	private static JavaClass actionClass = null;
	private int monitorCount;
	private Set<String> alreadyReported;
//	private static ClassNotFoundException missingClassEx = null;
	
	static {
		try {
			actionClass = Repository.lookupClass("org.apache.struts.action.Action");
		} catch (ClassNotFoundException cnfe) {
			//probably would be annoying to report
//			missingClassEx = cnfe;
		}		
	}
	
	public MultithreadedInstanceAccess(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
//		if (missingClassEx != null)
//			bugReporter.reportMissingClass(missingClassEx);
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visitClassContext(ClassContext classContext) {
		try {
			JavaClass cls = classContext.getJavaClass();
			String superClsName = cls.getSuperclassName();
			if ("java.lang.Object".equals(superClsName))
				return;
			
			if ("org.apache.struts.action.Action".equals(superClsName)) {
				super.visitClassContext(classContext);
			} else if ((actionClass != null)
			       &&  cls.implementationOf(actionClass)) {
				super.visitClassContext(classContext);
			}
		} catch (Exception e) {
			//already reported
		}
	}
	
	public void visitMethod(Method obj) {
		monitorCount = 0;
		alreadyReported = new HashSet<String>();
	}
	
	public void sawField() {
		if (monitorCount > 0)
			return;
		
		ConstantFieldref fieldRef;
		Constant c = getConstantRefOperand();
		if (c instanceof ConstantFieldref) {
			fieldRef = (ConstantFieldref)c;
			
			if (fieldRef.getClass(getConstantPool()).equals( this.getClassName())) {

				ConstantPool cp = getConstantPool();
				int nameAndTypeIdx = fieldRef.getNameAndTypeIndex();
				ConstantNameAndType ntc = (ConstantNameAndType)cp.getConstant(nameAndTypeIdx);
				int nameIdx = ntc.getNameIndex();

				Field[] flds = getClassContext().getJavaClass().getFields();
								
				for (int i = 0; i < flds.length; i++) {
					if (flds[i].getNameIndex() == nameIdx) {
						if (!flds[i].isStatic()) {
							ConstantUtf8 nameCons = (ConstantUtf8)cp.getConstant(nameIdx);
							ConstantUtf8 typeCons = (ConstantUtf8)cp.getConstant(ntc.getSignatureIndex());
							
							if (alreadyReported.contains(nameCons.getBytes()))
								return;
							alreadyReported.add(nameCons.getBytes());
							bugReporter.reportBug( new BugInstance(this, "MTIA_SUSPECT_STRUTS_INSTANCE_FIELD", LOW_PRIORITY )
									.addClass(this).addSourceLine(this)
									.addField( new FieldAnnotation(getClassName(), nameCons.getBytes(), typeCons.getBytes(), false)));
						}
						break;
					}
				}
			}
		}
	}
	
	public void sawOpcode(int seen) {
		if (seen == MONITORENTER)
			monitorCount++;
		else if (seen == MONITOREXIT)
			monitorCount--;
	}

	

}
