/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

public class ResourceValue {
	private ResourceValue() {
	}

	private static final ResourceValue instance = new ResourceValue();
	private static final ResourceValue notInstance = new ResourceValue();

	public static ResourceValue instance() {
		return instance;
	}

	public static ResourceValue notInstance() {
		return notInstance;
	}

	public static ResourceValue merge(ResourceValue a, ResourceValue b) {
		if (a == notInstance && b == notInstance)
			return notInstance;
		else
			return instance;
	}

	public boolean isInstance() {
		return this == instance;
	}

	@Override
         public String toString() {
		return (this == instance) ? "I" : "-";
	}
}

// vim:ts=4
