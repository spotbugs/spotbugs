/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;

import edu.umd.cs.findbugs.ba.ClassContext;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.bcel.classfile.Method;

/**
 * Detector to find private methods that are never called.
 */
public class FindUncalledPrivateMethods extends BytecodeScanningDetector {
	private BugReporter bugReporter;
	private String className;
	private HashSet<MethodAnnotation> definedPrivateMethods, calledMethods;

	public FindUncalledPrivateMethods(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitMethod(Method obj) {
		super.visitMethod(obj);
		if (obj.isPrivate() 
				&& !methodName.equals("writeReplace")
				&& !methodName.equals("readResolve")
				&& !methodName.equals("readObject")
				&& !methodName.equals("writeObject")
				&& !methodName.equals("<init>")
				)
			definedPrivateMethods.add(MethodAnnotation.fromVisitedMethod(this));
	}

	public void sawOpcode(int seen) {
		switch (seen) {
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKESTATIC:
			if (betterClassConstant.equals(className)) {
				MethodAnnotation called = new MethodAnnotation(betterClassConstant, nameConstant, sigConstant);
				calledMethods.add(called);
			}
			break;
		default:
			break;
		}
	}

	public void visitClassContext(ClassContext classContext) {
		definedPrivateMethods = new HashSet<MethodAnnotation>();
		calledMethods = new HashSet<MethodAnnotation>();
		className = classContext.getJavaClass().getClassName();

		super.visitClassContext(classContext);

		definedPrivateMethods.removeAll(calledMethods);

		for (Iterator<MethodAnnotation> i = definedPrivateMethods.iterator(); i.hasNext(); ) {
			BugInstance bugInstance = new BugInstance("UPM_UNCALLED_PRIVATE_METHOD", NORMAL_PRIORITY)
				.addClass(this)
				.addMethod(i.next());
			bugReporter.reportBug(bugInstance);
		}

		definedPrivateMethods = null;
		calledMethods = null;
	}
}

// vim:ts=4
