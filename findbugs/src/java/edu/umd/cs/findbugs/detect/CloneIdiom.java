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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.PruneUnconditionalExceptionThrowerEdges;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

public class CloneIdiom extends DismantleBytecode implements Detector, StatelessDetector {

	private ClassDescriptor cloneDescriptor = DescriptorFactory.createClassDescriptor("java/lang/Cloneable");
	
	boolean isCloneable,hasCloneMethod;
	MethodAnnotation cloneMethodAnnotation;
	boolean referencesCloneMethod;
	boolean invokesSuperClone;
	boolean checksInstanceOfCloneable;
	
	boolean isFinal;
	boolean cloneOnlyThrowsException;

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

	@Override
		 public void visit(Code obj) {
		if (getMethodName().equals("clone") &&
				getMethodSig().startsWith("()"))
			super.visit(obj);
	}

	@Override
		 public void sawOpcode(int seen) {
		if (seen == INSTANCEOF && getClassConstantOperand().equals("java/lang/Cloneable"))
			checksInstanceOfCloneable = true;
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
		cloneOnlyThrowsException = false;
		isCloneable = false;
		check = false;
		isFinal = obj.isFinal();
		if (obj.isInterface()) return;
		if (obj.isAbstract()) return;
		// Does this class directly implement Cloneable?
		String[] interface_names = obj.getInterfaceNames();
		for (String interface_name : interface_names) {
			if (interface_name.equals("java.lang.Cloneable")) {
				implementsCloneableDirectly = true;
				isCloneable = true;
				break;
			}
		}

	
			Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
			try {
				 if (subtypes2.isSubtype(getClassDescriptor(), cloneDescriptor)) 
					 isCloneable = true;
	            if (subtypes2.isSubtype(DescriptorFactory.createClassDescriptorFromDottedClassName(obj.getSuperclassName()), cloneDescriptor)) 
	            	implementsCloneableDirectly = false;
	            	
            } catch (ClassNotFoundException e) {
	           bugReporter.reportMissingClass(e);
            }

		hasCloneMethod = false;
		referencesCloneMethod = false;
		check = true;
		super.visit(obj);
	}

	@Override
		 public void visitAfter(JavaClass obj) {
		if (!check) return;
		if (cloneOnlyThrowsException) return;
		if (implementsCloneableDirectly && !hasCloneMethod) {
			if (!referencesCloneMethod)
				bugReporter.reportBug(new BugInstance(this, "CN_IDIOM", NORMAL_PRIORITY)
						.addClass(this));
		}

		if (hasCloneMethod && isCloneable && !invokesSuperClone && !isFinal && obj.isPublic()) {
			int priority = LOW_PRIORITY;
			if (obj.isPublic() || obj.isProtected()) 
				priority = NORMAL_PRIORITY;
			try {
				Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
				if (!subtypes2.getDirectSubtypes(getClassDescriptor()).isEmpty())
	            	priority--;
            } catch (ClassNotFoundException e) {
	           bugReporter.reportMissingClass(e);
            }
			bugReporter.reportBug(new BugInstance(this, "CN_IDIOM_NO_SUPER_CALL", priority)
					.addClass(this)
					.addMethod(cloneMethodAnnotation));
		} else if (hasCloneMethod && !isCloneable && !cloneOnlyThrowsException && !obj.isAbstract()) {
			int priority = Priorities.NORMAL_PRIORITY;
			if (referencesCloneMethod) priority--;
			
			bugReporter.reportBug(new BugInstance(this, "CN_IMPLEMENTS_CLONE_BUT_NOT_CLONEABLE", priority)
			.addClass(this)
			.addMethod(cloneMethodAnnotation));
		}

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
		cloneOnlyThrowsException = 
			PruneUnconditionalExceptionThrowerEdges.doesMethodUnconditionallyThrowException(XFactory.createXMethod(this));
		//ExceptionTable tbl = obj.getExceptionTable();
		//throwsExceptions = tbl != null && tbl.getNumberOfExceptions() > 0;
	}

	/* (non-Javadoc)
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    public void report() {
	   // do nothing
	    
    }
}
