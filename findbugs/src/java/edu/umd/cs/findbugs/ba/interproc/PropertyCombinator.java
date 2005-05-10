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
package edu.umd.cs.findbugs.ba.interproc;

/**
 * Combine method property found at a "source" method with method property found
 * at a "target" method.  Depending on the type of method property,
 * the result may be assigned to the supertype (e.g., method return value property)
 * or the subtype (e.g., method parameter property).
 * 
 * @author David Hovemeyer
 */
public interface PropertyCombinator<Property> {
	/**
	 * Combine method properties from a "source" method to a "target" method.
	 * 
	 * @param sourceProperty property at the source method
	 * @param targetProperty property at the target method
	 * @return combined property
	 */
	public Property combine(Property sourceProperty, Property targetProperty);
}
