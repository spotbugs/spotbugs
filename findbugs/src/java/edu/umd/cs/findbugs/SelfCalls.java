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

package edu.umd.cs.findbugs;

import java.util.HashSet;
import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * Build a call graph of the self calls in a class.
 */
public class SelfCalls {
	private static final boolean DEBUG = SystemProperties.getBoolean("selfcalls.debug");

	private ClassContext classContext;
	private CallGraph callGraph;
	private HashSet<Method> calledMethodSet;
	private boolean hasSynchronization;

	/**
	 * Constructor.
	 *
	 * @param classContext the ClassContext for the class
	 */
	public SelfCalls(ClassContext classContext) {
		this.classContext = classContext;
		this.callGraph = new CallGraph();
		this.calledMethodSet = new HashSet<Method>();
		this.hasSynchronization = false;
	}

	/**
	 * Find the self calls.
	 */
	public void execute() throws CFGBuilderException {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methods = jclass.getMethods();

		if (DEBUG) System.out.println("Class has " + methods.length + " methods");

		// Add call graph nodes for all methods
		for (Method method : methods) {
			callGraph.addNode(method);
		}
		if (DEBUG) System.out.println("Added " + callGraph.getNumVertices() + " nodes to graph");

		// Scan methods for self calls
		for (Method method : methods) {
			MethodGen mg = classContext.getMethodGen(method);
			if (mg == null)
				continue;

			scan(callGraph.getNodeForMethod(method));
		}

		if (DEBUG) System.out.println("Found " + callGraph.getNumEdges() + " self calls");
	}

	/**
	 * Get the self call graph for the class.
	 */
	public CallGraph getCallGraph() {
		return callGraph;
	}

	/**
	 * Get an Iterator over self-called methods.
	 */
	public Iterator<Method> calledMethodIterator() {
		return calledMethodSet.iterator();
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

	/**
	 * Get an Iterator over all self call sites.
	 */
	public Iterator<CallSite> callSiteIterator() {
		return new Iterator<CallSite>() {
			private Iterator<CallGraphEdge> iter = callGraph.edgeIterator();

			public boolean hasNext() {
				return iter.hasNext();
			}

			public CallSite next() {
				return iter.next().getCallSite();
			}

			public void remove() {
				iter.remove();
			}
		};
	}

	/**
	 * Does this class contain any explicit synchronization?
	 */
	public boolean hasSynchronization() {
		return hasSynchronization;
	}

	/**
	 * Scan a method for self call sites.
	 *
	 * @param node the CallGraphNode for the method to be scanned
	 */
	private void scan(CallGraphNode node) throws CFGBuilderException {
		Method method = node.getMethod();
		CFG cfg = classContext.getCFG(method);

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
					Method called = isSelfCall(inv);
					if (called != null) {
						// Add edge to call graph
						CallSite callSite = new CallSite(method, block, handle);
						callGraph.createEdge(node, callGraph.getNodeForMethod(called), callSite);

						// Add to called method set
						calledMethodSet.add(called);
					}
				} else if (ins instanceof MONITORENTER || ins instanceof MONITOREXIT) {
					hasSynchronization = true;
				}
			}
		}
	}

	/**
	 * Is the given instruction a self-call?
	 */
	private Method isSelfCall(InvokeInstruction inv) {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
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
		for (Method method : methods) {
			String methodName = method.getName();
			String signature = method.getSignature();
			boolean isStatic = method.isStatic();

			if (methodName.equals(calledMethodName) &&
					signature.equals(calledMethodSignature) &&
					isStatic == isStaticCall) {
				// This method looks like a match.
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
