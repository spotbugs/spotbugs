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

/**
 * An object which resolves classes.
 * Each TypeRepository has a class resolved to perform
 * the following functions:
 * <ul>
 * <li> Check whether it is a class or interface
 * <li> Add links for its superclass and implemented interfaces
 * to the class hierarchy
 * </ul>
 *
 * @author David Hovemeyer
 * @see TypeRepository
 */
public interface ClassResolver {
	/**
	 * Resolve a class.
	 *
	 * @param type  the ClassType object representing the class
	 * @param repos the TypeRepository containing the type
	 * @throws ClassNotFoundException if the class cannot be resolved
	 */
	public void resolveClass(ClassType type, TypeRepository repos) throws ClassNotFoundException;
}

// vim:ts=4
