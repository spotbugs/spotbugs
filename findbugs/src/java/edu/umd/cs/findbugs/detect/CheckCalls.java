/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;

/**
 * This is just for debugging method call resolution.
 * 
 * @author David Hovemeyer
 */
public class CheckCalls implements Detector, NonReportingDetector {

	private static final String METHOD = SystemProperties.getProperty("checkcalls.method");
	private static final String TARGET_METHOD = SystemProperties.getProperty("checkcalls.targetmethod");

	BugReporter bugReporter;

	public CheckCalls(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	public void visitClassContext(ClassContext classContext) {
		Method[] methodList = classContext.getJavaClass().getMethods();
		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			//System.out.println("--> " + method.getName());
			if (METHOD != null && !method.getName().equals(METHOD))
				continue;

			try {
				System.out.println("Analyzing " +
						SignatureConverter.convertMethodSignature(classContext.getJavaClass(), method)
				);
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error", e);
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, ClassNotFoundException, DataflowAnalysisException {
		CFG cfg = classContext.getCFG(method);
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			Instruction ins = location.getHandle().getInstruction();

			if (ins instanceof InvokeInstruction) {
				if (TARGET_METHOD != null
						&& !((InvokeInstruction)ins).getMethodName(classContext.getConstantPoolGen()).equals(TARGET_METHOD))
					continue;

				System.out.println("\n*******************************************************\n");

				System.out.println("Method invocation: " + location.getHandle());
				System.out.println("\tInvoking: " +
						SignatureConverter.convertMethodSignature((InvokeInstruction)ins,classContext.getConstantPoolGen()));

				JavaClassAndMethod proto = Hierarchy.findInvocationLeastUpperBound(
						(InvokeInstruction) ins,
						classContext.getConstantPoolGen());
				if (proto == null) {
					System.out.println("\tUnknown prototype method");
				} else {
					System.out.println("\tPrototype method: class=" +
							proto.getJavaClass().getClassName() + ", method=" +
							proto.getMethod());
				}
				Set<JavaClassAndMethod> calledMethodSet = Hierarchy.resolveMethodCallTargets(
						(InvokeInstruction) ins,
						classContext.getTypeDataflow(method).getFactAtLocation(location),
						classContext.getConstantPoolGen()
				);
				System.out.println("\tTarget method set: " + calledMethodSet);
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.Detector#report()
	 */
	public void report() {
	}

}
