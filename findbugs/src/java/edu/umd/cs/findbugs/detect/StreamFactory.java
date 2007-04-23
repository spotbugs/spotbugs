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
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * An object that examines a Location and determines
 * if a Stream is created there.
 */
public interface StreamFactory {
	/**
	 * Determine if a Stream is created at given location.
	 *
	 * @param location              the Location
	 * @param type                  the ObjectType associated with the instruction at the location;
	 *                              the StreamResourceTracker prescreens for TypedInstructions
	 *                              that are associated with ObjectTypes, since they are
	 *                              the only instructions that could conceivably create a
	 *                              stream object
	 * @param cpg                   the ConstantPoolGen for the method
	 * @param lookupFailureCallback used to report missing
	 *                              classes in the class hierarchy
	 * @return a Stream created at the Location,
	 *         or null if no stream is created there
	 */
	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
							   RepositoryLookupFailureCallback lookupFailureCallback);
}

// vim:ts=3
