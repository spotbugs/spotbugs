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
 * Interface for an object that merges types for dataflow analysis.
 *
 * @author David Hovemeyer
 */
public interface TypeMerger {
	/**
	 * Merge two types.
	 * The merged type is one which any value of
	 * either of the given types can be assigned to.
	 *
	 * @param a a Type
	 * @param b another Type
	 * @return the Type resulting from merging a and b
	 */
	public Type mergeTypes(Type a, Type b) throws ClassNotFoundException;
}

// vim:ts=4
