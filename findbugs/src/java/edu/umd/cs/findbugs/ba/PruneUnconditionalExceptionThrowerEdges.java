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
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;

public class PruneUnconditionalExceptionThrowerEdges implements EdgeTypes {
	private static final boolean DEBUG = SystemProperties.getBoolean("cfg.prune.throwers.debug");
	private static final boolean DEBUG_DIFFERENCES = SystemProperties.getBoolean("cfg.prune.throwers.differences.debug");

	private MethodGen methodGen;
	private Method method;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private AnalysisContext analysisContext;
	private boolean cfgModified;
	private ClassContext classContext;
	private JavaClass javaClass;

	private  static final BitSet RETURN_OPCODE_SET = new BitSet();
	static {
		RETURN_OPCODE_SET.set(Constants.ARETURN);
		RETURN_OPCODE_SET.set(Constants.IRETURN);
		RETURN_OPCODE_SET.set(Constants.LRETURN);
		RETURN_OPCODE_SET.set(Constants.DRETURN);
		RETURN_OPCODE_SET.set(Constants.FRETURN);
		RETURN_OPCODE_SET.set(Constants.RETURN);
	}

	public PruneUnconditionalExceptionThrowerEdges(ClassContext classContext, JavaClass javaClass, Method method,
			MethodGen methodGen, CFG cfg, ConstantPoolGen cpg, AnalysisContext analysisContext) {
		this.classContext = classContext;
		this.javaClass = javaClass;
		this.methodGen = methodGen;
		this.method = method;
		this.cfg = cfg;
		this.cpg = cpg;
		this.analysisContext = analysisContext;
	}

	static AnalysisLocal<Map<XMethod,Boolean>> cachedResults = new AnalysisLocal<Map<XMethod,Boolean>>() {
		@Override
		public Map<XMethod,Boolean> initialValue() { 
			return new HashMap<XMethod,Boolean>();
		}
	};

	public void execute() throws CFGBuilderException, DataflowAnalysisException {
		AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
		if (currentAnalysisContext.getBoolProperty(AnalysisFeatures.CONSERVE_SPACE))
			throw new IllegalStateException("This should not happen");

		Set<Edge> deletedEdgeSet = new HashSet<Edge>();
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

		if (DEBUG)
			System.out.println("PruneUnconditionalExceptionThrowerEdges: examining " +
					SignatureConverter.convertMethodSignature(methodGen));
		Subtypes subtypes = AnalysisContext.currentAnalysisContext()
		.getSubtypes();
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			if (!basicBlock.isExceptionThrower())
				continue;

			InstructionHandle instructionHandle = basicBlock.getExceptionThrower();
			Instruction exceptionThrower = instructionHandle.getInstruction();
			if (!(exceptionThrower instanceof InvokeInstruction))
				continue;

			InvokeInstruction inv = (InvokeInstruction) exceptionThrower;
			boolean foundThrower = false;
			boolean foundNonThrower = false;


			String className = inv.getClassName(cpg);
			if (DEBUG) System.out.println("\tlooking up method for " + instructionHandle + " in " + className);

			Location loc = new Location(instructionHandle, basicBlock);
			TypeFrame typeFrame = typeDataflow.getFactAtLocation(loc);
			Boolean oldIsUnconditionalThrower = null;
			XMethod primaryXMethod = null;
			Set<JavaClassAndMethod> targetSet = null;
			try {

			{
			 primaryXMethod = XFactory.createXMethod(inv, cpg);
			JavaClass primaryJavaClass = Repository.lookupClass(primaryXMethod.getClassName());

			JavaClassAndMethod primaryClassAndMethod = Hierarchy.findMethod(primaryJavaClass, primaryXMethod.getName(), primaryXMethod.getSignature(), Hierarchy.CONCRETE_METHOD);
				if (primaryClassAndMethod == null) {
					if (DEBUG) System.out.println("\tNOT FOUND");
					continue;
				}
				Method method = primaryClassAndMethod.getMethod();
				if (DEBUG) System.out.println("\tFound " + primaryXMethod);

				if (!(method.isStatic() || method.isPrivate() || method.isFinal() || primaryJavaClass.isFinal() || !subtypes.hasSubtypes(primaryJavaClass))) {
					if (!Repository.instanceOf(methodGen.getClassName(), primaryJavaClass)) continue;
				}
				oldIsUnconditionalThrower = doesMethodUnconditionallyThrowException(primaryXMethod, primaryJavaClass, method);
			}
			if (className.startsWith("["))
				continue;
			String methodSig = inv.getSignature(cpg);
			if (!methodSig.endsWith("V")) 
				continue;

				targetSet = Hierarchy.resolveMethodCallTargets(inv, typeFrame, cpg);

				for(JavaClassAndMethod classAndMethod : targetSet) {

					Method method = classAndMethod.getMethod();
					XMethod xMethod = XFactory.createXMethod(classAndMethod);

					if (DEBUG) System.out.println("\tFound " + xMethod);

					if (false) {
						if (!(method.isStatic() || method.isPrivate() || method.isFinal() || javaClass.isFinal() || !subtypes.hasSubtypes(javaClass))) {

							if (!Repository.instanceOf(methodGen.getClassName(), javaClass)) continue;
						}
					}

					// Ignore abstract and native methods
					if (method.getCode() == null) continue;
					Boolean isUnconditionalThrower = doesMethodUnconditionallyThrowException(xMethod, javaClass, method);
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
						if (true) cachedResults.get().put(xMethod, isUnconditionalThrower);
					}
					if (isUnconditionalThrower) {
						foundThrower = true;
						if (DEBUG) System.out.println("Found thrower");
					}
					else {
						foundNonThrower = true;
						if (DEBUG) System.out.println("Found non thrower");
					}

				}
			} catch (ClassNotFoundException e) {
				analysisContext.getLookupFailureCallback().reportMissingClass(e);
			}
			boolean newResult = foundThrower && !foundNonThrower;
			if (DEBUG_DIFFERENCES && oldIsUnconditionalThrower != null && oldIsUnconditionalThrower.booleanValue() != newResult) {
				System.out.println("Found place where old pruner and new pruner diverge: ");
				System.out.println(" oldResult: " + oldIsUnconditionalThrower);
				System.out.println(" newResult: " + newResult);
				System.out.println(" foundThrower: " + foundThrower);
				System.out.println(" foundNonThrower: " + foundNonThrower);
				System.out.println("In : " + SignatureConverter.convertMethodSignature(methodGen));
				System.out.println("Call to :"+ primaryXMethod);
				if (targetSet != null) for(JavaClassAndMethod jcm : targetSet)
					System.out.println(jcm);
				System.out.println();

			}
			if (newResult) {
				// Method always throws an unhandled exception
				// Remove the normal control flow edge from the CFG.
				Edge fallThrough = cfg.getOutgoingEdgeWithType(basicBlock,
						FALL_THROUGH_EDGE);
				if (fallThrough != null) {
					if (DEBUG) {
						System.out.println("\tREMOVING normal return for: " + XFactory.createXMethod(inv, cpg));
					}
					deletedEdgeSet.add(fallThrough);
				}
			}

		}

		// Remove all edges marked for deletion
		for (Edge edge : deletedEdgeSet) {
			cfg.removeEdge(edge);
			cfgModified = true;
		}
	}

	/**
	 * @param xMethod
	 * @param javaClass
	 * @param method
	 * @return true if method unconditionally throws
	 */
	static public  Boolean doesMethodUnconditionallyThrowException(XMethod xMethod, JavaClass javaClass, Method method) {
		if (javaClass == null) throw new IllegalArgumentException("javaClass is null");
		Boolean isUnconditionalThrower = cachedResults.get().get(xMethod);

		if (isUnconditionalThrower == null) {
			isUnconditionalThrower = Boolean.FALSE;
			try {
				ClassContext classContext = AnalysisContext.currentAnalysisContext().getClassContext(javaClass);
				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				if (bytecodeSet != null) {

					if (DEBUG) System.out.println("\tChecking " + xMethod);
					isUnconditionalThrower = Boolean.valueOf(!bytecodeSet.intersects(RETURN_OPCODE_SET));
					if (DEBUG && isUnconditionalThrower) {
						System.out.println("Is unconditional thrower");
						System.out.println("Return opcode set: " + RETURN_OPCODE_SET);
						System.out.println("Code opcode set: " + bytecodeSet);
					}
				}
			} catch (Exception e) {
				if (DEBUG) e.printStackTrace();
			}

			cachedResults.get().put(xMethod, isUnconditionalThrower);

		}
		return isUnconditionalThrower;
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

//vim:ts=4
