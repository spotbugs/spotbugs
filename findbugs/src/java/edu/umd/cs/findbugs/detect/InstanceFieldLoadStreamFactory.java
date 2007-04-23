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
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * StreamFactory for stream objects loaded from instance fields.
 *
 * @author David Hovemeyer
 */
public class InstanceFieldLoadStreamFactory implements StreamFactory {
	private String streamBaseClass;
	private String bugPatternType;

	/**
	 * Constructor.
	 * By default, Streams created by this factory
	 * will not be marked as interesting.
	 * The setBugPatternType() method should be called to
	 * make the factory produce interesting streams.
	 *
	 * @param streamBaseClass the base class of the streams
	 *                        produced by the factory
	 */
	public InstanceFieldLoadStreamFactory(String streamBaseClass) {
		this.streamBaseClass = streamBaseClass;
	}

	/**
	 * Set the bug pattern type reported for unclosed streams
	 * loaded from this field.  This makes the created
	 * streams "interesting".
	 *
	 * @param bugPatternType the bug pattern type
	 */
	public InstanceFieldLoadStreamFactory setBugPatternType(String bugPatternType) {
		this.bugPatternType = bugPatternType;
		return this;
	}

	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
							   RepositoryLookupFailureCallback lookupFailureCallback) {

		Instruction ins = location.getHandle().getInstruction();
		if (ins.getOpcode() != Constants.GETFIELD)
			return null;

		String fieldClass = type.getClassName();
		try {
			if (fieldClass.startsWith("["))
				return null;
			if (!Hierarchy.isSubtype(fieldClass, streamBaseClass))
				return null;

			Stream stream = new Stream(location, fieldClass, streamBaseClass);
			stream.setIsOpenOnCreation(true);
			stream.setOpenLocation(location);
			if (bugPatternType != null)
				stream.setInteresting(bugPatternType);

			//System.out.println("Instance field stream at " + location);
			return stream;
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
			return null;
		}
	}
}

// vim:ts=4
