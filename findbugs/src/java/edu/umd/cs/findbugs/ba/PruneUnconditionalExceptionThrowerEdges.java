/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

import java.util.*;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class PruneUnconditionalExceptionThrowerEdges implements EdgeTypes, AnalysisFeatures {
	private static final boolean DEBUG = Boolean.getBoolean("cfg.prune.throwers.debug");

	private MethodGen methodGen;
	private CFG cfg;
	private ConstantPoolGen cpg;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	public PruneUnconditionalExceptionThrowerEdges(MethodGen methodGen, CFG cfg, ConstantPoolGen cpg,
		RepositoryLookupFailureCallback lookupFailureCallback) {
		assert lookupFailureCallback != null;
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.cpg = cpg;
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public void execute() throws CFGBuilderException, DataflowAnalysisException {
		if (CONSERVE_SPACE) throw new IllegalStateException("This should not happen");

		HashSet<Edge> deletedEdgeSet = new HashSet<Edge>();

		if (DEBUG) System.out.println("PruneUnconditionalExceptionThrowerEdges: examining " +
			SignatureConverter.convertMethodSignature(methodGen));

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
				JavaClass javaClass = Repository.lookupClass(className);
				ClassContext classContext = AnalysisContext.instance().getClassContext(javaClass);

				if (DEBUG) System.out.println("\tlooking up method for "+ basicBlock.getExceptionThrower());
				Method method = Lookup.findExactMethod(inv, cpg);
				if (method == null) {
					if (DEBUG) System.out.println("\tNOT FOUND");
					continue;
				}
				if (DEBUG) System.out.println("\tFound " + method.toString());

				// FIXME: for now, only allow static and private methods.
				// Could also allow final methods (but would require class hierarchy
				// search).
				if (!(method.isStatic() || method.isPrivate()))
					continue;

				// Ignore abstract and native methods
				MethodGen calledMethodGen = classContext.getMethodGen(method);
				if (calledMethodGen == null)
					continue;

				// Analyze exception paths of called method
				// to see if it always throws an unhandled exception.
				CFG calledCFG = classContext.getCFG(method);
				ReturnPathDataflow pathDataflow = classContext.getReturnPathDataflow(method);
				ReturnPath pathValue = pathDataflow.getStartFact(calledCFG.getExit());

				if (pathValue.getKind() != ReturnPath.RETURNS) {
					// Method always throws an unhandled exception
					// or calls System.exit().
					// Remove the normal control flow edge from the CFG.
					Edge fallThrough = cfg.getOutgoingEdgeWithType(basicBlock, FALL_THROUGH_EDGE);
					if (fallThrough != null) {
						if (DEBUG) {
							System.out.println("\tREMOVING normal return for:");
							System.out.println("\t  Call to " + SignatureConverter.convertMethodSignature(calledMethodGen));
							System.out.println("\t  In method " + SignatureConverter.convertMethodSignature(methodGen));
						}
						deletedEdgeSet.add(fallThrough);
					}
				}
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}

		// Remove all edges marked for deletion
		for (Iterator<Edge> i = deletedEdgeSet.iterator(); i.hasNext();) {
			Edge edge = i.next();
			cfg.removeEdge(edge);
		}
	}
}

// vim:ts=4
