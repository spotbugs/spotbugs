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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

public class StandardTypeMerger implements TypeMerger, Constants, ExtendedTypes {
	private RepositoryLookupFailureCallback lookupFailureCallback;

	public StandardTypeMerger(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
	}

	public Type mergeTypes(Type a, Type b) throws DataflowAnalysisException {
		byte aType = a.getType(), bType = b.getType();

		if (aType == T_TOP)			// Top is the identity element
			return b;
		else if (bType == T_TOP)	// Top is the identity element
			return a;
		else if (aType == T_BOTTOM || bType == T_BOTTOM)	// Bottom meet anything is bottom
			return TypeFrame.getBottomType();
		else if (isObjectType(aType) && isObjectType(bType)) {	// Two object types!
			// Handle the Null type, which serves as a special "top"
			// value for object types.
			if (aType == T_NULL)
				return b;
			else if (bType == T_NULL)
				return a;

			// Two concrete object types.
			// According to the JVM spec, 2nd edition, §4.9.2,
			// the result of merging types is the "first common superclass".
			// Interfaces are NOT considered!
			// This will use the Repository to look up classes.
			ReferenceType aRef = (ReferenceType) a;
			ReferenceType bRef = (ReferenceType) b;
			try {
				return aRef.getFirstCommonSuperclass(bRef);
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				throw new DataflowAnalysisException("Repository lookup failure", e);
			}
		} else if (isObjectType(aType) || isObjectType(bType))	// Object meet non-object is bottom
			return TypeFrame.getBottomType();
		else if (aType == bType)	// Same non-object type?
			return a;
		else if (isIntegerType(aType) && isIntegerType(bType)) // Two different integer types - use T_INT
			return Type.INT;
		else						// Default - types are incompatible
			return TypeFrame.getBottomType();
	}

	/**
	 * Does the given typecode refer to an Object (reference) type?
	 */
	private static boolean isObjectType(byte type) {
		return type == T_OBJECT || type == T_NULL;
	}

	/**
	 * Does given typecode refer to an Integer type (other than long)?
	 */
	private static boolean isIntegerType(byte type) {
		return type == T_INT || type == T_BYTE || type == T_BOOLEAN || type == T_CHAR || type == T_SHORT;
	}

}

// vim:ts=4
