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

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;

/**
 * A ResourceTracker is used with ResourceValueAnalysis to determine
 * where in a method a certain kind of resource is created, and
 * to model the effect of instructions on the state of that resource.
 *
 * @author David Hovemeyer
 * @see ResourceValueAnalysis
 */
public interface ResourceTracker <Resource> {
	/**
	 * Determine if the given instruction is the site where a resource
	 * is created.
	 *
	 * @param basicBlock basic block containing the instruction
	 * @param handle     the instruction
	 * @param cpg        the ConstantPoolGen for the method
	 * @return an opaque Resource object if it is a creation site, or
	 *         null if it is not a creation site
	 */
	public Resource isResourceCreation(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg)
			throws DataflowAnalysisException;

	/**
	 * Determine if the given instruction is the site where a resource
	 * is closed.
	 *
	 * @param basicBlock basic block containing the instruction
	 * @param handle     the instruction
	 * @param cpg        the ConstantPoolGen for the method
	 * @param resource   the resource, as returned by isResourceCreation()
	 * @param frame      the ResourceValueFrame representing the stack prior to executing
	 *                   the instruction
	 * @return true if the resource is closed here, false otherwise
	 */
	public boolean isResourceClose(BasicBlock basicBlock, InstructionHandle handle, ConstantPoolGen cpg, Resource resource,
								   ResourceValueFrame frame) throws DataflowAnalysisException;

	/**
	 * Create a ResourceValueFrameModelingVisitor to model the effect
	 * of instructions on the state of the resource.
	 *
	 * @param resource the resource we are tracking
	 * @param cpg      the ConstantPoolGen of the method
	 * @return a ResourceValueFrameModelingVisitor
	 */
	public ResourceValueFrameModelingVisitor createVisitor(Resource resource, ConstantPoolGen cpg);

	/**
	 * Determine whether the analysis should ignore exception edges
	 * on which only implicit exceptions are propagated.
	 * This allows different resource types to be tracked
	 * with varying precision.  For example, we might want
	 * to ignore implicit exceptions for stream objects,
	 * but treat them as significant for database resources.
	 *
	 * @param resource the resource being tracked
	 * @return true if implicit exceptions are significant,
	 *         false if they should be ignore
	 */
	public boolean ignoreImplicitExceptions(Resource resource);

	/**
	 * Determine whether the analysis should ignore given exception edge.
	 * This allows the analysis to customize which kinds of exceptions are
	 * significant.
	 * 
	 * @param edge     the exception edge
	 * @param resource the resource
	 * @param cpg      the ConstantPoolGen
	 * @return true if exception edge should be ignored, false if it should be considered
	 */
	public boolean ignoreExceptionEdge(Edge edge, Resource resource, ConstantPoolGen cpg);

	/**
	 * Return if the given parameter slot contains the
	 * resource instance upon entry to the method.
	 * This is for resources passed as parameters.
	 *
	 * @param resource the resource
	 * @param slot     the local variable slot
	 * @return true if the slot contains the resource instance,
	 *         false otherwise
	 */
	public boolean isParamInstance(Resource resource, int slot);
}

// vim:ts=4
