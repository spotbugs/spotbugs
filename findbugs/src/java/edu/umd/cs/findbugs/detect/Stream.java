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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.ResourceCreationPoint;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.ResourceValue;
import edu.umd.cs.findbugs.ba.ResourceValueFrame;

import org.apache.bcel.Constants;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;

/**
 * A Stream object marks the location in the code where a
 * stream is created.  It also is responsible for determining
 * some aspects of how the stream state is tracked
 * by the ResourceValueAnalysis, such as when the stream
 * is opened or closed, and whether implicit exception
 * edges are significant.
 *
 * <p> TODO: change streamClass and streamBase to ObjectType
 *
 * <p> TODO: isStreamOpen() and isStreamClose() should
 * probably be abstract, so we can customize how they work
 * for different kinds of streams
 */
public class Stream extends ResourceCreationPoint {
	private String streamBase;
	private boolean isUninteresting;
	private boolean isOpenOnCreation;
	private InstructionHandle ctorHandle;
	private boolean ignoreImplicitExceptions;

	public Stream(Location location, String streamClass, String streamBase,
		boolean isUninteresting, boolean ignoreImplicitExceptions) {
		this(location, streamClass, streamBase, isUninteresting, ignoreImplicitExceptions, false);
	}

	public Stream(Location location, String streamClass, String streamBase,
		boolean isUninteresting, boolean ignoreImplicitExceptions, boolean isOpenOnCreation) {
		super(location, streamClass);
		this.streamBase = streamBase;
		this.isUninteresting = isUninteresting;
		this.ignoreImplicitExceptions = ignoreImplicitExceptions;
		this.isOpenOnCreation = isOpenOnCreation;
	}

	public String getStreamBase() { return streamBase; }

	public boolean isUninteresting() { return isUninteresting; }

	public boolean isOpenOnCreation() { return isOpenOnCreation; }

	public void setConstructorHandle(InstructionHandle handle) { this.ctorHandle = handle; }

	public InstructionHandle getConstructorHandle() { return ctorHandle; }

	public boolean ignoreImplicitExceptions() { return ignoreImplicitExceptions; }

	public boolean isStreamOpen(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, ResourceValueFrame frame) {
		if (isOpenOnCreation)
			return false;

		Instruction ins = handle.getInstruction();
		if (!(ins instanceof INVOKESPECIAL))
			return false;

		// Does this instruction open the stream?
		INVOKESPECIAL inv = (INVOKESPECIAL) ins;

		return frame.isValid()
			&& getInstanceValue(frame, inv, cpg).isInstance()
			&& matchMethod(inv, cpg, this.getResourceClass(), "<init>");
	}

	public boolean isStreamClose(BasicBlock basicBlock, InstructionHandle handle,
		ConstantPoolGen cpg, ResourceValueFrame frame,
		RepositoryLookupFailureCallback lookupFailureCallback) {

		Instruction ins = handle.getInstruction();

		if (ins instanceof INVOKEVIRTUAL) {
			// Does this instruction close the stream?
			INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

			if (!frame.isValid() ||
				!getInstanceValue(frame, inv, cpg).isInstance())
				return false;

			// It's a close if the invoked class is any subtype of the stream base class.
			// (Basically, we may not see the exact original stream class,
			// even though it's the same instance.)
			try {
				return inv.getName(cpg).equals("close")
					&& inv.getSignature(cpg).equals("()V")
					&& Hierarchy.isSubtype(inv.getClassName(cpg), streamBase);
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				return false;
			}
		}

		return false;
	}

	private ResourceValue getInstanceValue(ResourceValueFrame frame, InvokeInstruction inv,
		ConstantPoolGen cpg) {
		int numConsumed = inv.consumeStack(cpg);
		if (numConsumed == Constants.UNPREDICTABLE)
			throw new IllegalStateException();
		return frame.getValue(frame.getNumSlots() - numConsumed);
	}

	private boolean matchMethod(InvokeInstruction inv, ConstantPoolGen cpg, String className,
		String methodName) {
		return inv.getClassName(cpg).equals(className)
			&& inv.getName(cpg).equals(methodName);
	}
}

// vim:ts=3
