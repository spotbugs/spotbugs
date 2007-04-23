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

package edu.umd.cs.findbugs.ba.bcp;

/**
 * A set of Bindings, which are definitions of variables occuring
 * in a ByteCodePattern.  BindingSets are immutable; to add a binding,
 * a new cell is allocated.  (Are we CONSING yet?)
 *
 * @author David Hovemeyer
 * @see Binding
 */
public class BindingSet {
	private final Binding binding;
	private final BindingSet parent;

	/**
	 * Constructor; creates a new BindingSet as an extension of an existing one.
	 *
	 * @param binding a variable binding
	 * @param parent  the parent BindingSet, containing other bindings
	 */
	public BindingSet(Binding binding, BindingSet parent) {
		this.binding = binding;
		this.parent = parent;
	}

	/**
	 * Look for a Binding for given variable.
	 *
	 * @param varName name of the variable
	 * @return the Binding, or null if no such Binding is present in the set
	 */
	public Binding lookup(String varName) {
		if (varName.equals(binding.getVarName()))
			return binding;
		return parent != null ? parent.lookup(varName) : null;
	}

	@Override
		 public String toString() {
		StringBuffer buf = new StringBuffer();
		BindingSet cur = this;
		buf.append('[');
		while (cur != null) {
			if (cur != this)
				buf.append(", ");
			buf.append(cur.binding.toString());
			cur = cur.parent;
		}
		buf.append(']');
		return buf.toString();
	}
}

// vim:ts=4
