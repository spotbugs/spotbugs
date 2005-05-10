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

/**
 * Common super-interface for package members (fields and methods).
 * 
 * @see edu.umd.cs.findbugs.ba.XField
 * @author David Hovemeyer
 */
public interface PackageMember extends Comparable<PackageMember>  {

	/**
	 * Get the name of the field.
	 */
	public abstract String getName();

	/**
	 * Get the name of the class the field is defined in.
	 */
	public abstract String getClassName();

	/**
	 * Get the signature representing the field's type.
	 */
	public abstract String getSignature();

	/**
	 * Get the field's access flags.
	 */
	public abstract int getAccessFlags();

	/**
	 * Is this a static field?
	 */
	public abstract boolean isStatic();

	/**
	 * Is this a final field?
	 */
	public abstract boolean isFinal();

	/**
	 * Is this a public field?
	 */
	public abstract boolean isPublic();

}