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

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * Factory for stream objects of a particular
 * base class type returned by any method.
 * This factory helps us keep track of streams returned
 * by methods; we don't want to report them, but we do
 * want to keep track of whether or not they are closed,
 * to avoid reporting unclosed streams in the same
 * equivalence class.
 */
public class AnyMethodReturnValueStreamFactory implements StreamFactory {
	private ObjectType baseClassType;
	private String bugType;

	public AnyMethodReturnValueStreamFactory(String streamBase) {
		this.baseClassType = ObjectTypeFactory.getInstance(streamBase);
		this.bugType = null;
	}

	public AnyMethodReturnValueStreamFactory setBugType(String bugType) {
		this.bugType = bugType;
		return this;
	}

	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
							   RepositoryLookupFailureCallback lookupFailureCallback) {

		Instruction ins = location.getHandle().getInstruction();

		try {
			if (ins instanceof InvokeInstruction) {
				if (!Hierarchy.isSubtype(type, baseClassType))
					return null;

				Stream stream = new Stream(location, type.getClassName(), baseClassType.getClassName())
						.setIsOpenOnCreation(true)
						.setIgnoreImplicitExceptions(true);
				if (bugType != null)
					stream.setInteresting(bugType);

				return stream;
			}
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
		}

		return null;
	}
}

// vim:ts=4
