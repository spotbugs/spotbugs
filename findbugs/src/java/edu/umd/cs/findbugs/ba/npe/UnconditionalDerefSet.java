/*
 * Bytecode analysis framework
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;

/**
 * @author David Hovemeyer
 * @deprecated Use UnconditionalValueDerefAnalysis instead
 */
public class UnconditionalDerefSet extends BitSet {
	private static final long serialVersionUID = 1L;
	
	private final int numParams;
	
	public UnconditionalDerefSet(int numParams) {
		this.numParams = numParams;
	}
	
	public void setTop() {
		clear();
		set(numParams);
	}
	
	public void setBottom() {
		clear();
		set(numParams + 1);
	}
	
	public boolean isValid() {
		return !isTop() && !isBottom();
	}
	
	public boolean isTop() {
		return get(numParams);
	}
	
	public boolean isBottom() {
		return get(numParams + 1);
	}
	
	@Override
         public String toString() {
		if (isTop())
			return "TOP";
		else if (isBottom())
			return "BOTTOM";
		else
			return super.toString();
	}
}
