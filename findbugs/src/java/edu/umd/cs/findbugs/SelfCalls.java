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

package edu.umd.cs.findbugs;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

public class SelfCalls {
	private static final boolean DEBUG = Boolean.getBoolean("selfcalls.debug");

	private ClassContext classContext;
	private HashMap<CallSite, Method> selfCallToMethodMap = new HashMap<CallSite, Method>();
	private IdentityHashMap<Method, Set<CallSite>> methodToSelfCallMap = new IdentityHashMap<Method, Set<CallSite>>();
	private boolean hasSynchronization;

	public SelfCalls(ClassContext classContext) {
		this.classContext = classContext;
		hasSynchronization = false;
	}

	public void execute() throws CFGBuilderException {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methods = jclass.getMethods();
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];
			if (method.isAbstract() || method.isNative())
				continue;

			MethodGen mg = classContext.getMethodGen(method);
			if (mg == null)
				continue;
			CFG cfg = classContext.getCFG(method);

			scan(method, mg, cfg);
		}

		if (DEBUG) System.out.println("Found " + selfCallToMethodMap.size() + " self calls");
	}

	public Iterator<Method> calledMethodIterator() {
		return methodToSelfCallMap.keySet().iterator();
	}

	/**
	 * Determine whether we are interested in calls for the
	 * given method.  Subclasses may override.  The default version
	 * returns true for every method.
	 *
	 * @param method the method
	 * @return true if we want call sites for the method, false if not
	 */
	public boolean wantCallsFor(Method method) {
		return true;
	}

	public Set<CallSite> getCallSites(Method called) {
		Set<CallSite> callSiteSet = methodToSelfCallMap.get(called);
		if (callSiteSet == null) {
			callSiteSet = new HashSet<CallSite>();
			methodToSelfCallMap.put(called, callSiteSet);
		}
		return callSiteSet;
	}

	/** Does this class contain any explicitl synchronization? */
	public boolean hasSynchronization() { return hasSynchronization; }

	private void scan(Method method, MethodGen mg, CFG cfg) {

		if (method.isSynchronized())
			hasSynchronization = true;

		Iterator<BasicBlock> i = cfg.blockIterator();
		while (i.hasNext()) {
			BasicBlock block = i.next();
			Iterator<InstructionHandle> j = block.instructionIterator();
			while (j.hasNext()) {
				InstructionHandle handle = j.next();

				Instruction ins = handle.getInstruction();
				if (ins instanceof InvokeInstruction) {
					InvokeInstruction inv = (InvokeInstruction) ins;
					Method called = isSelfCall(inv, mg);
					if (called != null) {
						CallSite callSite = new CallSite(method, block, handle);

						// Map call site to self-called method
						selfCallToMethodMap.put(callSite, called);

						// Map method to call site
						Set<CallSite> callSiteSet = getCallSites(called);
						callSiteSet.add(callSite);
					}
				} else if (ins instanceof MONITORENTER || ins instanceof MONITOREXIT) {
					hasSynchronization = true;
				}
			}
		}
	}

	private Method isSelfCall(InvokeInstruction inv, MethodGen mg) {
		ConstantPoolGen cpg = mg.getConstantPool();
		JavaClass jclass = classContext.getJavaClass();

		String calledClassName = inv.getClassName(cpg);

		// FIXME: is it possible we would see a superclass name here?
		// Not a big deal for now, as we are mostly just interested in calls
		// to private methods, for which we will definitely see the right
		// called class name.
		if (!calledClassName.equals(jclass.getClassName()))
			return null;

		String calledMethodName = inv.getMethodName(cpg);
		String calledMethodSignature = inv.getSignature(cpg);
		boolean isStaticCall = (inv instanceof INVOKESTATIC);

		// Scan methods for one that matches.
		Method[] methods = jclass.getMethods();
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];

			String methodName = method.getName();
			String signature = method.getSignature();
			boolean isStatic = method.isStatic();

			if (methodName.equals(calledMethodName) && 
				signature.equals(calledMethodSignature) &&
				isStatic == isStaticCall) {
				// This method looks like a match.
				//MethodGen called = classContext.getMethodGen(method);
				return wantCallsFor(method) ? method : null;
			}
		}

		// Hmm...no matching method found.
		// This is almost certainly because the named method
		// was inherited from a superclass.
		if (DEBUG) System.out.println("No method found for " + calledClassName + "." + calledMethodName + " : " + calledMethodSignature);
		return null;
	}
}

// vim:ts=4
