/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

import org.apache.bcel.Constants;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;

/**
 * StreamFactory for streams that are created as the result
 * of calling a method on an object.
 */
public class MethodReturnValueStreamFactory implements StreamFactory {
	private ObjectType baseClassType;
	private String methodName;
	private String methodSig;
	private boolean isUninteresting;

	/**
	 * Constructor.
	 * @param baseClass base class through which the method will be
	 *   called (we check instances of the base class and all subtypes)
	 * @param methodName name of the method called
	 * @param methodSig signature of the method called
	 * @param isUninteresting true if the streams created are not interesting
	 */
	public MethodReturnValueStreamFactory(String baseClass, String methodName, String methodSig,
		boolean isUninteresting) {
		this.baseClassType = new ObjectType(baseClass);
		this.methodName = methodName;
		this.methodSig = methodSig;
		this.isUninteresting = isUninteresting;
	}

	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
		RepositoryLookupFailureCallback lookupFailureCallback) {

		try {
			Instruction ins = location.getHandle().getInstruction();

			// For now, just support instance methods
			short opcode = ins.getOpcode();
			if (opcode != Constants.INVOKESPECIAL
				&& opcode != Constants.INVOKEVIRTUAL
				&& opcode != Constants.INVOKEINTERFACE)
				return null;

			// Is invoked class a subtype of the base class we want
			InvokeInstruction inv = (InvokeInstruction) ins;
			ObjectType classType = inv.getClassType(cpg);
			if (!Hierarchy.isSubtype(classType, baseClassType))
				return null;

			// See if method name and signature match
			String methodName = inv.getMethodName(cpg);
			String methodSig = inv.getSignature(cpg);
			if (!this.methodName.equals(methodName) || !this.methodSig.equals(methodSig))
				return null;

			String streamClass = type.getClassName();
			return new Stream(location, streamClass, streamClass, isUninteresting, true, true);
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
		}

		return null;
	}
}

// vim:ts=3
