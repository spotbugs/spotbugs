/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
import org.apache.bcel.classfile.*;

public class FindUninitializedGet extends BytecodeScanningDetector implements StatelessDetector {
	Set<FieldAnnotation> initializedFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> declaredFields = new HashSet<FieldAnnotation>();
	boolean inConstructor;
	boolean thisOnTOS = false;
	private BugReporter bugReporter;

	private static final int UNKNOWN_PRIORITY = -1;

	public FindUninitializedGet(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	@Override
         public void visit(JavaClass obj) {
		declaredFields.clear();
		super.visit(obj);
	}

	@Override
         public void visit(Field obj) {
		super.visit(obj);
		//declaredFields.add(fieldName);
		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		declaredFields.add(f);
		/*
		System.out.println("Visiting " + fieldName);
		*/
	}

	@Override
         public void visit(Method obj) {
		super.visit(obj);
		initializedFields.clear();
		/*
		System.out.println("Visiting " + methodName);
		*/
		thisOnTOS = false;
		inConstructor = getMethodName().equals("<init>")
		        && getMethodSig().indexOf(getClassName()) == -1;
		/*
		System.out.println("methodName: " + methodName);
		System.out.println("methodSig: " + methodSig);
		System.out.println("inConstructor: " + inConstructor);
		*/
	}


	@Override
         public void sawOpcode(int seen) {
		if (!inConstructor) return;

		/*
		System.out.println("thisOnTOS:" + thisOnTOS);
		System.out.println("seen:" + seen);
		*/
		if (seen == ALOAD_0) {
			thisOnTOS = true;
			/*
			System.out.println("set thisOnTOS");
			*/
			return;
		}

/*
	if (thisOnTOS && seen == GETFIELD) {
		System.out.println("Saw getfield of " + classConstant 
				+ "." + nameConstant);
		if (initializedFields.contains(nameConstant))
		    System.out.println("   initialized");
		if (declaredFields.contains(nameConstant))
		    System.out.println("   declared");
		}
*/

		if (seen == PUTFIELD && getClassConstantOperand().equals(getClassName()))
			initializedFields.add(FieldAnnotation.fromReferencedField(this));

		else if (thisOnTOS && seen == GETFIELD && getClassConstantOperand().equals(getClassName())) {
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			int nextOpcode = codeBytes[getPC() + 3];
			// System.out.println("Next opcode: " + OPCODE_NAMES[nextOpcode]);
			if (nextOpcode != POP && !initializedFields.contains(f) && declaredFields.contains(f)) {
				bugReporter.reportBug(new BugInstance(this, "UR_UNINIT_READ", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addField(f)
				        .addSourceLine(this));
				initializedFields.add(FieldAnnotation.fromReferencedField(this));
			}
		} else if (
		        (seen == INVOKESPECIAL
		        && !(getNameConstantOperand().equals("<init>")
		        && !getClassConstantOperand().equals(getClassName()))
		        )
		        || (seen == INVOKESTATIC
		        && getNameConstantOperand().equals("doPrivileged")
		        && getClassConstantOperand().equals("java/security/AccessController")
		        )
		        || (seen == INVOKEVIRTUAL
		        && getClassConstantOperand().equals(getClassName()))
		        || (seen == INVOKEVIRTUAL
		        && getNameConstantOperand().equals("start"))) {
			/*
			System.out.println("Saw invocation of "
				+ classConstant + "." + nameConstant
				+ " in " + className + "." + methodName);
			*/
			inConstructor = false;
		}

		thisOnTOS = false;
	}
}
