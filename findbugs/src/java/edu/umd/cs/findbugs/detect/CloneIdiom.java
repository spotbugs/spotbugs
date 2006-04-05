/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

public class CloneIdiom extends DismantleBytecode implements Detector, StatelessDetector {

	boolean /*isCloneable,*/ hasCloneMethod;
	MethodAnnotation cloneMethodAnnotation;
	boolean referencesCloneMethod;
	boolean invokesSuperClone;
	boolean isFinal;

	boolean check;
	//boolean throwsExceptions;
	boolean implementsCloneableDirectly;
	private BugReporter bugReporter;

	public CloneIdiom(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}


	public void report() {
	}

	@Override
         public void visit(Code obj) {
		if (getMethodName().equals("clone") &&
		        getMethodSig().startsWith("()"))
			super.visit(obj);
	}

	@Override
         public void sawOpcode(int seen) {
		if (seen == INVOKESPECIAL
		        && getNameConstantOperand().equals("clone")
		        && getSigConstantOperand().startsWith("()")) {
			/*
			System.out.println("Saw call to " + nameConstant
						+ ":" + sigConstant
						+ " in " + betterMethodName);
			*/
			invokesSuperClone = true;
		}
	}

	@Override
         public void visit(JavaClass obj) {
		implementsCloneableDirectly = false;
		invokesSuperClone = false;
		//isCloneable = false;
		check = false;
		isFinal = obj.isFinal();
		if (obj.isInterface()) return;
		if (obj.isAbstract()) return;
		// Does this class directly implement Cloneable?
		String[] interface_names = obj.getInterfaceNames();
		for (String interface_name : interface_names) {
			if (interface_name.equals("java.lang.Cloneable")) {
				implementsCloneableDirectly = true;
				//isCloneable = true;
				break;
			}
		}

		try {
			//isCloneable = Repository.implementationOf(obj, "java.lang.Cloneable");
			JavaClass superClass = obj.getSuperClass();
			if (superClass != null && Repository.implementationOf(superClass, "java.lang.Cloneable"))
				implementsCloneableDirectly = false;
		} catch (ClassNotFoundException e) {
			// ignore
		}
		hasCloneMethod = false;
		referencesCloneMethod = false;
		check = true;
		super.visit(obj);
	}

	@Override
         public void visitAfter(JavaClass obj) {
		if (!check) return;
		if (implementsCloneableDirectly && !hasCloneMethod) {
			if (!referencesCloneMethod)
				bugReporter.reportBug(new BugInstance(this, "CN_IDIOM", NORMAL_PRIORITY)
				        .addClass(this));
		}

		if (hasCloneMethod && !invokesSuperClone && !isFinal && obj.isPublic()) {
			bugReporter.reportBug(new BugInstance(this, "CN_IDIOM_NO_SUPER_CALL", (obj.isPublic() || obj.isProtected()) ?
			        NORMAL_PRIORITY : LOW_PRIORITY)
			        .addClass(this)
			        .addMethod(cloneMethodAnnotation));
		}

		/*
		if (!isCloneable && hasCloneMethod) {
			if (throwsExceptions)
			System.out.println("has public clone method that throws exceptions and class is not Cloneable: " + betterClassName) ;
			else System.out.println("has public clone method but is not Cloneable: " + betterClassName) ;
			}
		*/
	}

	@Override
         public void visit(ConstantNameAndType obj) {
		String methodName = obj.getName(getConstantPool());
		String methodSig = obj.getSignature(getConstantPool());
		if (!methodName.equals("clone")) return;
		if (!methodSig.startsWith("()")) return;
		referencesCloneMethod = true;
	}

	@Override
         public void visit(Method obj) {
		if (obj.isAbstract()) return;
		if (!obj.isPublic()) return;
		if (!getMethodName().equals("clone")) return;
		if (!getMethodSig().startsWith("()")) return;
		hasCloneMethod = true;
		cloneMethodAnnotation = MethodAnnotation.fromVisitedMethod(this);
		//ExceptionTable tbl = obj.getExceptionTable();
		//throwsExceptions = tbl != null && tbl.getNumberOfExceptions() > 0;
	}
}
