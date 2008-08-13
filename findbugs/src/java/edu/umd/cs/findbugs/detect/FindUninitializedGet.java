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


import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;

public class FindUninitializedGet extends BytecodeScanningDetector implements StatelessDetector {
	Set<FieldAnnotation> initializedFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> declaredFields = new HashSet<FieldAnnotation>();
	Set<FieldAnnotation> containerFields = new HashSet<FieldAnnotation>();
	Collection<BugInstance> pendingBugs = new LinkedList<BugInstance>();
	boolean inConstructor;
	boolean thisOnTOS = false;
	private BugReporter bugReporter;

	private static final int UNKNOWN_PRIORITY = -1;

	public FindUninitializedGet(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
	public void visit(JavaClass obj) {
		pendingBugs.clear();
		declaredFields.clear();
		containerFields.clear();
		super.visit(obj);
	}

	@Override
	public void visit(Field obj) {
		super.visit(obj);
		FieldAnnotation f = FieldAnnotation.fromVisitedField(this);
		declaredFields.add(f);
		

	}
	@Override
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {
		if (!visitingField()) return;
		if (UnreadFields.isInjectionAttribute(annotationClass)) {
			containerFields.add(FieldAnnotation.fromVisitedField(this));
		}
	
	}

	@Override
	public void visit(Method obj) {
		super.visit(obj);
		initializedFields.clear();

		thisOnTOS = false;
		inConstructor = getMethodName().equals("<init>")
		&& getMethodSig().indexOf(getClassName()) == -1;

	}


	@Override
	public void visit(Code obj) {
		super.visit(obj);
		for(BugInstance bug : pendingBugs) {
			bugReporter.reportBug(bug);
		}
		pendingBugs.clear();
	}

	@Override
	public void sawBranchTo(int target) {
		Iterator<BugInstance> i = pendingBugs.iterator();
		while (i.hasNext()) {
			BugInstance bug = i.next();
			if (bug.getPrimarySourceLineAnnotation().getStartBytecode()>= target)
				i.remove();
		}
	}
	@Override
	public void sawOpcode(int seen) {
		if (!inConstructor) return;

		if (seen == ALOAD_0) {
			thisOnTOS = true;
			/*
			System.out.println("set thisOnTOS");
			 */
			return;
		}

		if (seen == PUTFIELD && getClassConstantOperand().equals(getClassName()))
			initializedFields.add(FieldAnnotation.fromReferencedField(this));

		else if (thisOnTOS && seen == GETFIELD && getClassConstantOperand().equals(getClassName())) {
			UnreadFields unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFields();
			XField xField = XFactory.createReferencedXField(this);
			FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
			int nextOpcode = codeBytes[getPC() + 3];
			// System.out.println("Next opcode: " + OPCODE_NAMES[nextOpcode]);
			if (nextOpcode != POP
					&& !initializedFields.contains(f) 
					&& declaredFields.contains(f)
					&& !containerFields.contains(f)) {
				pendingBugs.add(new BugInstance(this, "UR_UNINIT_READ", unreadFields.getReadFields().contains(xField)  ? NORMAL_PRIORITY : LOW_PRIORITY)
				.addClassAndMethod(this)
				.addField(f)
				.addSourceLine(this));
				initializedFields.add(f);
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

			inConstructor = false;
		}

		thisOnTOS = false;
	}
}
