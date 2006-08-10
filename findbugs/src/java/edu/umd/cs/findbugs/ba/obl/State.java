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

package edu.umd.cs.findbugs.ba.obl;

/**
 * Error-handling obligation analysis state.
 * This is a set of obligations and a program path on which
 * they are outstanding (not cleaned up).
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 *
 * @author David Hovemeyer
 */
public class State {
	private ObligationSet obligationSet;
	private Path path;
	
	private State() {		
	}

	public State(int maxObligationTypes, ObligationFactory factory) {
		this.obligationSet = new ObligationSet(maxObligationTypes, factory);
		this.path = new Path();
	}
	
	/**
	 * @return Returns the obligationSet.
	 */
	public ObligationSet getObligationSet() {
		return obligationSet;
	}

	/**
	 * @return Returns the path.
	 */
	public Path getPath() {
		return path;
	}
	
	public State duplicate() {
		State dup = new State();
		dup.obligationSet = this.obligationSet.duplicate();
		dup.path = this.path.duplicate();
		
		return dup;
	}

	@Override
         public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		State other = (State) o;
		return this.obligationSet.equals(other.obligationSet)
			|| this.path.equals(other.path);
	}

	@Override
         public int hashCode() {
		return obligationSet.hashCode() + (1009 * path.hashCode());
	}
	
	@Override
         public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("[");
		buf.append(obligationSet.toString());
		buf.append(",");
		buf.append(path.toString());
		buf.append("]");
		return buf.toString();
	}
}

// vim:ts=4
