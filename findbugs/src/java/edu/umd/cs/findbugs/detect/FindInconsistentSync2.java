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
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import java.util.HashSet;
import java.util.Iterator;

public class FindInconsistentSync2 implements Detector {
	private BugReporter bugReporter;
	private HashSet<Method> lockedNonpublicMethodSet;

	public FindInconsistentSync2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.lockedNonpublicMethodSet = new HashSet<Method>();
	}

	public void visitClassContext(ClassContext classContext) {
		lockedNonpublicMethodSet.clear();

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

		// Find all obviously locked self-call sites
		for (Iterator<CallSite> i = selfCalls.callSiteIterator(); i.hasNext(); ) {
			CallSite callSite = i.next();

			Method method = callSite.getMethod();

			// Get the LockDataflow for the site
			LockDataflow lockDataflow = classContext.getLockDataflow(method);
			LockSet lockSet = lockDataflow.getFactAtLocation(callSite.getLocation());

			// Find the ValueNumber of the instance
		}

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

/*
	private boolean allCallSitesLocked(SelfCalls selfCalls, Method method, Set<Method> finished) {
	}
*/
}

// vim:ts=4
