/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.findbugs.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import java.util.*;

public class FindInconsistentSync2 implements Detector {
	private BugReporter bugReporter;
	//private HashSet<Method> lockedNonpublicMethodSet;

	public FindInconsistentSync2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		//this.lockedNonpublicMethodSet = new HashSet<Method>();
	}

	public void visitClassContext(ClassContext classContext) {
		//lockedNonpublicMethodSet.clear();

		try {
			findLockedNonpublicMethods(classContext);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindInconsistentSync2 caught exception: " + e.toString(), e);
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindInconsistentSync2 caught exception: " + e.toString(), e);
		}
	}

	public void report() {
	}

	private void findLockedNonpublicMethods(ClassContext classContext)
		throws CFGBuilderException, DataflowAnalysisException {

		SelfCalls selfCalls = new SelfCalls(classContext) {
			public boolean wantCallsFor(Method method) {
				return !method.isPublic();
			}
		};

		selfCalls.execute();

		Set<CallSite> obviouslyLockedSites = findObviouslyLockedCallSites(classContext, selfCalls);

/*
		JavaClass javaClass = classContext.getJavaClass();

		Set<Method> finished = new HashSet<Method>();
		Method[] methodList = javaClass.getMethods();

		for (int i = 0; i < methodList.length; ++i) {
			Method method = workList.removeLast();
			if (!finished.contains(method)) {
				if (allCallSitesLocked(selfCalls, method, finished)) {
					lockedNonpublicMethodSet.add(method);
				}
				finished.add(method);
			}
		}
*/
	}

	private Set<CallSite> findObviouslyLockedCallSites(ClassContext classContext, SelfCalls selfCalls)
		throws CFGBuilderException, DataflowAnalysisException {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();

		// Find all obviously locked call sites
		HashSet<CallSite> obviouslyLockedSites = new HashSet<CallSite>();
		for (Iterator<CallSite> i = selfCalls.callSiteIterator(); i.hasNext(); ) {
			CallSite callSite = i.next();
			Method method = callSite.getMethod();
			Location location = callSite.getLocation();
			InstructionHandle handle = location.getHandle();

			// Only instance method calls qualify as candidates for
			// "obviously locked"
			Instruction ins = handle.getInstruction();
			if (ins.getOpcode() == Constants.INVOKESTATIC)
				continue;

			// Get lock set for site
			LockDataflow lockDataflow = classContext.getLockDataflow(method);
			LockSet lockSet = lockDataflow.getFactAtLocation(location);

			// Get value number frame for site
			ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
			ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);

			// Find the ValueNumber of the receiver object
			int numConsumed = ins.consumeStack(cpg);
			if (numConsumed == Constants.UNPREDICTABLE)
				throw new AnalysisException("Unpredictable stack consumption: " + handle);
			ValueNumber instance = frame.getStackValue(numConsumed);

			// Is the instance locked?
			int lockCount = lockSet.getLockCount(instance.getNumber());
			if (lockCount > 0) {
				// This is a locked call site
				obviouslyLockedSites.add(callSite);
			}
		}

		return obviouslyLockedSites;
	}

/*
	private boolean allCallSitesLocked(SelfCalls selfCalls, Method method, Set<Method> finished) {
	}
*/
}

// vim:ts=4
