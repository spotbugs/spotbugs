/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import java.util.Iterator;

/**
 * Special ReferenceType representing the type of a caught exception.
 * Keeps track of the entire set of exceptions that can be caught,
 * and whether they are explicit or implicit.
 */
public class ExceptionObjectType extends ReferenceType implements Constants, ExtendedTypes {
	private ExceptionSet exceptionSet;

	/**
	 * Constructor.
	 * @param type type code
	 * @param sig type signature
	 * @param exceptionSet the set of exceptions
	 */
	private ExceptionObjectType(byte type, String sig, ExceptionSet exceptionSet) {
		super(type, sig);
		this.exceptionSet = exceptionSet;
	}

	/**
	 * Initialize object from an exception set.
	 * @param exceptionSet the exception set
	 * @return a Type that is a supertype of all of the exceptions in
	 *   the exception set
	 */
	public static Type fromExceptionSet(ExceptionSet exceptionSet) throws ClassNotFoundException {
		Type commonSupertype = exceptionSet.getCommonSupertype();
		if (commonSupertype.getType() != T_OBJECT)
			return commonSupertype;

		ObjectType exceptionSupertype = (ObjectType) commonSupertype;
		return new ExceptionObjectType(T_EXCEPTION, exceptionSupertype.getSignature(), exceptionSet);
	}

	/**
	 * Return the exception set.
	 * @return the ExceptionSet
	 */
	public ExceptionSet getExceptionSet() {
		return exceptionSet;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("<exception:");
		boolean first = true;
		for (Iterator<ThrownException> i = exceptionSet.iterator(); i.hasNext(); ) {
			if (first)
				first = false;
			else
				buf.append(',');
			buf.append(i.next().getType().toString());
		}
		buf.append(">");
		return buf.toString();
	}
}

// vim:ts=4
