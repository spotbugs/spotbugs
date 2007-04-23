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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * Stream factory for streams created by loading a value
 * from a static field.  This is mainly to handle
 * System.in, System.out, and System.err.
 */
public class StaticFieldLoadStreamFactory implements StreamFactory {
	public String streamBaseClass;
	public String className;
	public String fieldName;
	public String fieldSig;

	/**
	 * Constructor.
	 * Created Stream objects will be marked as uninteresting.
	 *
	 * @param streamBaseClass the base class of the stream objects created
	 *                        by the factory
	 * @param className       name of the class containing the static field
	 * @param fieldName       name of the static field
	 * @param fieldSig        signature of the static field
	 */
	public StaticFieldLoadStreamFactory(String streamBaseClass, String className,
										String fieldName, String fieldSig) {
		this.streamBaseClass = streamBaseClass;
		this.className = className;
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
	}

	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
							   RepositoryLookupFailureCallback lookupFailureCallback) {

		Instruction ins = location.getHandle().getInstruction();
		if (ins.getOpcode() != Constants.GETSTATIC)
			return null;

		GETSTATIC getstatic = (GETSTATIC) ins;
		if (!className.equals(getstatic.getClassName(cpg))
				|| !fieldName.equals(getstatic.getName(cpg))
				|| !fieldSig.equals(getstatic.getSignature(cpg)))
			return null;

		return new Stream(location, type.getClassName(), streamBaseClass)
				.setIgnoreImplicitExceptions(true)
				.setIsOpenOnCreation(true);
	}
}

// vim:ts=3
