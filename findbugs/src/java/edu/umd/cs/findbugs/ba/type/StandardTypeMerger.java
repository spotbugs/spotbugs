/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.type;

import edu.umd.cs.findbugs.ba.ExtendedTypes;
import org.apache.bcel.Constants;

/**
 * The standard implementation of TypeMerger for modeling
 * the usual Java type rules.
 *
 * @author David Hovemeyer
 */
public class StandardTypeMerger implements TypeMerger, Constants, ExtendedTypes {
	private TypeRepository repos;

	/**
	 * Constructor.
	 *
	 * @param repos the TypeRepository used to create types
	 */
	public StandardTypeMerger(TypeRepository repos) {
		this.repos = repos;
	}

	/**
	 * Determine if given type is the top type.
	 * Subclasses may override to distinguish custom types.
	 *
	 * @param type the Type
	 * @return true if type is TOP, false otherwise
	 */
	protected boolean isTop(Type type) {
		return type.getTypeCode() == T_TOP;
	}

	/**
	 * Determine if given type is the bottom type.
	 * Subclasses may override to distinguish custom types.
	 *
	 * @param type the Type
	 * @return true if type is BOTTOM, false otherwise
	 */
	protected boolean isBottom(Type type) {
		return type.getTypeCode() == T_BOTTOM;
	}

	/**
	 * Determine if given type is the null type.
	 * Subclasses may override to distinguish custom types.
	 *
	 * @param type the Type
	 * @return true if type is NULL, false otherwise
	 */
	protected boolean isNull(Type type) {
		return type.getTypeCode() == T_NULL;
	}

	protected boolean isBasicType(Type type) {
		return type.isBasicType();
	}

	protected boolean isReferenceType(Type type) {
		return type.isReferenceType();
	}

	protected boolean isIntType(BasicType type) {
		return type.getTypeCode() >= T_BYTE && type.getTypeCode() <= T_INT;
	}

	protected Type mergeBasicTypes(BasicType a, BasicType b) {
		// NOTE: this is only called if the types are not equal

		// Different int types can be merged...
		if (isIntType(a)) {
			if (!isIntType(b))
				return repos.getBottomType();
			// NOTE: we rely on the fact that BCEL Constants interface
			// orders T_BYTE, T_SHORT, and T_INT in increasing
			// numerical value.
			return repos.basicTypeFromTypeCode((byte) Math.max(a.getTypeCode(), b.getTypeCode()));
		}

		// All other basic types are incompatible
		return repos.getBottomType();
	}

	protected Type mergeReferenceTypes(ReferenceType a, ReferenceType b)
	        throws ClassNotFoundException {
		// Null is a special top type for reference types
		if (isNull(a))
			return b;
		else if (isNull(b))
			return a;

		// We consider meet to be the first common superclass
		return repos.getFirstCommonSuperclass((ObjectType) a, (ObjectType) b);
	}

	public Type mergeTypes(Type a, Type b) throws ClassNotFoundException {
		if (a.equals(b))
			return a;
		else if (isTop(a))
			return b;
		else if (isTop(b))
			return a;
		else if (isBottom(a) || isBottom(b))
			return repos.getBottomType();
		else if (isBasicType(a) || isBasicType(b)) {
			if (!(isBasicType(a) && isBasicType(b)))
				return repos.getBottomType();
			else
				return mergeBasicTypes((BasicType) a, (BasicType) b);
		} else if (isReferenceType(a) && isReferenceType(b))
			return mergeReferenceTypes((ReferenceType) a, (ReferenceType) b);
		else
			return repos.getBottomType();
	}
}

// vim:ts=4
