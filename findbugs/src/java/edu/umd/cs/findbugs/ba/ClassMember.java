/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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

import java.io.Serializable;

/**
 * Common super-interface for class members (fields and methods).
 * 
 * @see edu.umd.cs.findbugs.ba.XField
 * @see edu.umd.cs.findbugs.ba.XMethod
 * @author David Hovemeyer
 */
public interface ClassMember extends Comparable<ClassMember>, Serializable  {

	/**
	 * Get the name of the field/method.
	 */
	public String getName();

	/**
	 * Get the name of the class the field/method is defined in.
	 */
	public String getClassName();

	/**
	 * Get the signature representing the field/method's type.
	 */
	public String getSignature();

	/**
	 * Get the field/method's access flags.
	 */
	public int getAccessFlags();

	/**
	 * Is this a static field/method?
	 */
	public boolean isStatic();

	/**
	 * Is this a final field/method?
	 */
	public boolean isFinal();

	/**
	 * Is this a public field/method?
	 */
	public boolean isPublic();

	/**
	 * Is this a protected field/method?
	 */
	public boolean isProtected();
	
	/**
	 * Is this a private field/method?
	 */
	public boolean isPrivate();
	
	/**
	 * Did we find a declaration of this member?
	 */
	public boolean isResolved();
}
