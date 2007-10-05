/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilder;
import edu.umd.cs.findbugs.ba.CFGBuilderFactory;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.PruneInfeasibleExceptionEdges;
import edu.umd.cs.findbugs.ba.PruneUnconditionalExceptionThrowerEdges;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce CFG (control flow graph) objects for an analyzed method.
 * 
 * @author David Hovemeyer
 */
public class CFGFactory extends AnalysisFactory<CFG> {
	private static final boolean DEBUG_CFG = SystemProperties.getBoolean("classContext.debugCFG");

	/**
	 * Constructor.
	 */
	public CFGFactory() {
		super("control flow graph factory", CFG.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public CFG analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
		// Construct the CFG in its raw form
		MethodGen methodGen = analysisCache.getMethodAnalysis(MethodGen.class, descriptor);
		if (methodGen == null) {
			JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, descriptor.getClassDescriptor());
			Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
			JavaClassAndMethod javaClassAndMethod = new JavaClassAndMethod(jclass, method);
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportSkippedAnalysis(descriptor);
			throw new MethodUnprofitableException(javaClassAndMethod);
		}
		CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
		cfgBuilder.build();
		CFG cfg = cfgBuilder.getCFG();

		// Mark as busy while we're pruning the CFG.
		cfg.setFlag( CFG.BUSY);

		// Important: eagerly put the CFG in the analysis cache.
		// Recursively performed analyses required to prune the CFG,
		// such as TypeAnalysis, will operate on the raw CFG.
		analysisCache.eagerlyPutMethodAnalysis(CFG.class, descriptor, cfg);

		// Record method name and signature for informational purposes
		cfg.setMethodName(SignatureConverter.convertMethodSignature(methodGen));
		cfg.setMethodGen(methodGen);

		// System.out.println("CC: getting refined CFG for " + methodId);
		if (CFGFactory.DEBUG_CFG) {
			String methodId = methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature();
			ClassContext.indent();
			System.out.println("CC: getting refined CFG for " + methodId);
		}
		if (ClassContext.DEBUG) {
			String methodId = methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature();
			System.out.println("ClassContext: request to prune " + methodId);
		}

		// Remove CFG edges corresponding to failed assertions.
		boolean changed = false;
		boolean ASSUME_ASSERTIONS_ENABLED = true;
		if (ASSUME_ASSERTIONS_ENABLED) {
			LinkedList<Edge> edgesToRemove = new LinkedList<Edge>();
			for (Iterator<Edge> i = cfg.edgeIterator(); i.hasNext();) {
				Edge e = i.next();
				if (e.getType() == EdgeTypes.IFCMP_EDGE) {
					try {
						BasicBlock source = e.getSource();
						InstructionHandle last = source
						.getLastInstruction();
						Instruction lastInstruction = last.getInstruction();
						InstructionHandle prev = last.getPrev();
						Instruction prevInstruction = prev.getInstruction();
						if (prevInstruction instanceof GETSTATIC
								&& lastInstruction instanceof IFNE) {
							GETSTATIC getStatic = (GETSTATIC) prevInstruction;
							if (false) {
								System.out.println(prev);

								System.out.println(getStatic
										.getClassName(methodGen
												.getConstantPool()));
								System.out.println(getStatic
										.getFieldName(methodGen
												.getConstantPool()));
								System.out.println(getStatic
										.getSignature(methodGen
												.getConstantPool()));
								System.out.println(last);
							}
							if (getStatic.getFieldName(
									methodGen.getConstantPool()).equals(
											"$assertionsDisabled")
											&& getStatic.getSignature(
													methodGen.getConstantPool())
													.equals("Z"))
								edgesToRemove.add(e);
						}
					} catch (RuntimeException exception) {
						assert true; // ignore it
					}
				}
			}
			if (edgesToRemove.size() > 0) {
				changed = true;
				for (Edge e : edgesToRemove) {
					cfg.removeEdge(e);
				}
			}
		}
		cfg.setFlag(CFG.PRUNED_FAILED_ASSERTION_EDGES);

		final boolean PRUNE_INFEASIBLE_EXCEPTION_EDGES =
			AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS);

		if (PRUNE_INFEASIBLE_EXCEPTION_EDGES && !cfg.isFlagSet(CFG.PRUNED_INFEASIBLE_EXCEPTIONS)) {
			try {
				TypeDataflow typeDataflow = analysisCache.getMethodAnalysis(TypeDataflow.class, descriptor);
				// Exception edge pruning based on ExceptionSets.
				// Note: this is quite slow.
				PruneInfeasibleExceptionEdges pruner =
					new PruneInfeasibleExceptionEdges(cfg, methodGen, typeDataflow);
				pruner.execute();
				changed  = changed || pruner.wasCFGModified();
			} catch (DataflowAnalysisException e) {
				// FIXME: should report the error
			} catch (ClassNotFoundException e) {
				AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			}
		}
		cfg.setFlag( CFG.PRUNED_INFEASIBLE_EXCEPTIONS);

		final boolean PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES =
			!AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.CONSERVE_SPACE);

		if (PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES && !cfg.isFlagSet(CFG.PRUNED_UNCONDITIONAL_THROWERS)) {
			try {
				JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, descriptor.getClassDescriptor());
				Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
				ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, descriptor.getClassDescriptor());
				TypeDataflow typeDataflow = analysisCache.getMethodAnalysis(TypeDataflow.class, descriptor);

				PruneUnconditionalExceptionThrowerEdges pruner =
					new PruneUnconditionalExceptionThrowerEdges(
							jclass,
							method,
							methodGen,
							cfg,
							cpg,
							typeDataflow,
							AnalysisContext.currentAnalysisContext());
				pruner.execute();
				changed = changed || pruner.wasCFGModified();
			} catch (DataflowAnalysisException e) {
				// FIXME: should report the error
			}
		}
		cfg.setFlag( CFG.PRUNED_UNCONDITIONAL_THROWERS);

		// Now we are done with the CFG refining process
		cfg.setFlag(CFG.REFINED);
		cfg.clearFlag(CFG.BUSY);

		// If the CFG changed as a result of pruning, purge all analysis results
		// for the method.
		if (changed) {
			
			DepthFirstSearch dfs = new DepthFirstSearch(cfg);
			dfs.search();
			Collection<BasicBlock> unreachable = dfs.unvisitedVertices();
			if (!unreachable.isEmpty()) {
				if (DEBUG_CFG) System.out.println("Unreachable blocks");
				for(BasicBlock b : unreachable) {
					if (DEBUG_CFG) System.out.println(" removing " + b);
					cfg.removeVertex(b);
				}
			}
			Global.getAnalysisCache().purgeMethodAnalyses(descriptor);
		}

		return cfg;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerMethodAnalysisEngine(CFG.class, this);
	}
}
