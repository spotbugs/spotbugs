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
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * A StreamFactory for normal java.io streams that
 * are created using NEW instructions.
 */
public class IOStreamFactory implements StreamFactory {
	private ObjectType baseClassType;
	private ObjectType[] uninterestingSubclassTypeList;
	private String bugType;

	public IOStreamFactory(String baseClass, String[] uninterestingSubclassList, String bugType) {
		this.baseClassType = ObjectTypeFactory.getInstance(baseClass);
		this.uninterestingSubclassTypeList = new ObjectType[uninterestingSubclassList.length];
		for (int i = 0; i < uninterestingSubclassList.length; ++i) {
			this.uninterestingSubclassTypeList[i] = ObjectTypeFactory.getInstance(uninterestingSubclassList[i]);
		}
		this.bugType = bugType;
	}

	public Stream createStream(Location location, ObjectType type, ConstantPoolGen cpg,
							   RepositoryLookupFailureCallback lookupFailureCallback) {

		try {
			Instruction ins = location.getHandle().getInstruction();

			if (ins.getOpcode() != Constants.NEW)
				return null;

			if (Hierarchy.isSubtype(type, baseClassType)) {
				boolean isUninteresting = false;
				for (ObjectType aUninterestingSubclassTypeList : uninterestingSubclassTypeList) {
					if (Hierarchy.isSubtype(type, aUninterestingSubclassTypeList)) {
						isUninteresting = true;
						break;
					}
				}
				Stream result = new Stream(location, type.getClassName(), baseClassType.getClassName())
						.setIgnoreImplicitExceptions(true);
				if (!isUninteresting)
					result.setInteresting(bugType);
				return result;
			}
		} catch (ClassNotFoundException e) {
			lookupFailureCallback.reportMissingClass(e);
		}

		return null;
	}
}

// vim:ts=3
