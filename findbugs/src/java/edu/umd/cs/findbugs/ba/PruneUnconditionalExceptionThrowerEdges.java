/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.ch.Subtypes;

public class PruneUnconditionalExceptionThrowerEdges implements EdgeTypes {
	private static final boolean DEBUG = SystemProperties.getBoolean("cfg.prune.throwers.debug");

	private MethodGen methodGen;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private AnalysisContext analysisContext;
	private boolean cfgModified;
	
	private  static final BitSet RETURN_OPCODE_SET = new BitSet();
	static {
		RETURN_OPCODE_SET.set(Constants.ARETURN);
		RETURN_OPCODE_SET.set(Constants.IRETURN);
		RETURN_OPCODE_SET.set(Constants.LRETURN);
		RETURN_OPCODE_SET.set(Constants.DRETURN);
		RETURN_OPCODE_SET.set(Constants.FRETURN);
		RETURN_OPCODE_SET.set(Constants.RETURN);
	}

	public PruneUnconditionalExceptionThrowerEdges(MethodGen methodGen, CFG cfg, ConstantPoolGen cpg,
	                                               AnalysisContext analysisContext) {
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.cpg = cpg;
		this.analysisContext = analysisContext;
	}

	static Map<XMethod,Boolean> cachedResults = new HashMap<XMethod,Boolean>();
	public void execute() throws CFGBuilderException, DataflowAnalysisException {
		AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
		if (currentAnalysisContext.getBoolProperty(AnalysisFeatures.CONSERVE_SPACE))
			throw new IllegalStateException("This should not happen");

		Set<Edge> deletedEdgeSet = new HashSet<Edge>();

		if (DEBUG)
			System.out.println("PruneUnconditionalExceptionThrowerEdges: examining " +
			        SignatureConverter.convertMethodSignature(methodGen));
		 Subtypes subtypes = AnalysisContext.currentAnalysisContext()
			.getSubtypes();
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			if (!basicBlock.isExceptionThrower())
				continue;

			Instruction exceptionThrower = basicBlock.getExceptionThrower().getInstruction();
			if (!(exceptionThrower instanceof InvokeInstruction))
				continue;

			InvokeInstruction inv = (InvokeInstruction) exceptionThrower;
			try {
				String className = inv.getClassName(cpg);
				if (className.startsWith("["))
					continue;
				String methodSig = inv.getSignature(cpg);
				if (!methodSig.endsWith("V")) 
					continue;
				JavaClass javaClass = Repository.lookupClass(className);
				
				if (DEBUG) System.out.println("\tlooking up method for " + basicBlock.getExceptionThrower());
				JavaClassAndMethod classAndMethod = Hierarchy.findExactMethod(inv, cpg);
				if (classAndMethod == null) {
					if (DEBUG) System.out.println("\tNOT FOUND");
					continue;
				}
				Method method = classAndMethod.getMethod();
				XMethod xMethod = XFactory.createXMethod(javaClass, method);
				if (DEBUG) System.out.println("\tFound " + xMethod);

				// FIXME: for now, only allow static and private methods.
				// Could also allow final methods (but would require class hierarchy
				// search).
				if (!(method.isStatic() || method.isPrivate() || method.isFinal() || javaClass.isFinal() || !subtypes.hasSubtypes(javaClass)))
					continue;
				
				// Ignore abstract and native methods
				if (method.getCode() == null) continue;
				Boolean isUnconditionalThrower = cachedResults.get(xMethod);
				
				
				if (isUnconditionalThrower == null) {
					isUnconditionalThrower = Boolean.FALSE;
					try {
						ClassContext classContext = currentAnalysisContext.getClassContext(javaClass);
						BitSet bytecodeSet = classContext.getBytecodeSet(method);
						if (bytecodeSet != null) {

							if (DEBUG) System.out.println("\tChecking " + xMethod);
							isUnconditionalThrower = Boolean.valueOf(!bytecodeSet.intersects(RETURN_OPCODE_SET));
							if (DEBUG && isUnconditionalThrower) {
								System.out.println("Return opcode set: " + RETURN_OPCODE_SET);
								System.out.println("Code opcode set: " + bytecodeSet);
							}
						}
					} catch (Exception e) {
						// ignore it
					}

					cachedResults.put(xMethod, isUnconditionalThrower);

				}
				if (false && isUnconditionalThrower.booleanValue()) {
					ClassContext classContext = analysisContext.getClassContext(javaClass);
				    MethodGen calledMethodGen = classContext.getMethodGen(method);
					// Ignore abstract and native methods

					if (calledMethodGen == null)
						continue;

					// Analyze exception paths of called method
					// to see if it always throws an unhandled exception.
					CFG calledCFG = classContext.getCFG(method);
					ReturnPathDataflow pathDataflow = classContext
							.getReturnPathDataflow(method);
					ReturnPath pathValue = pathDataflow.getStartFact(calledCFG
							.getExit());

					isUnconditionalThrower = pathValue.getKind() != ReturnPath.RETURNS;
					// System.out.println("isThrower: " + result + " " + method.getCode().getLength() + " " + method);
					if (true) cachedResults.put(xMethod, isUnconditionalThrower);
				}

				if (isUnconditionalThrower.booleanValue()) {
					// Method always throws an unhandled exception
					// Remove the normal control flow edge from the CFG.
					Edge fallThrough = cfg.getOutgoingEdgeWithType(basicBlock,
							FALL_THROUGH_EDGE);
					if (fallThrough != null) {
						if (DEBUG) {
							System.out.println("\tREMOVING normal return for: " + xMethod);
						}
						deletedEdgeSet.add(fallThrough);
					}
				}
			} catch (ClassNotFoundException e) {
				analysisContext.getLookupFailureCallback().reportMissingClass(e);
			}
		}

		// Remove all edges marked for deletion
		for (Edge edge : deletedEdgeSet) {
			cfg.removeEdge(edge);
			cfgModified = true;
		}
	}
	
	/**
	 * Return whether or not the CFG was modified.
	 * 
	 * @return true if CFG was modified, false otherwise
	 */
	public boolean wasCFGModified() {
		return cfgModified;
	}
}

// vim:ts=4
