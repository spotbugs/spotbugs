/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004,2005 University of Maryland
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
 * A multiset of obligations that must be cleaned up by
 * error-handling code.
 *
 * <p>See Weimer and Necula,
 * <a href="http://doi.acm.org/10.1145/1028976.1029011"
 * >Finding and preventing run-time error handling mistakes</a>,
 * OOPSLA 2004.</p>
 *
 * @author David Hovemeyer
 */
public class ObligationSet {
	private static final int INVALID_HASH_CODE = -1;

	private int[] countList;
	private int cachedHashCode;

	public ObligationSet(int maxObligationTypes) {
		this.countList = new int[maxObligationTypes];
		invalidate();
	}

	public void add(Obligation obligation) {
		invalidate();
		countList[obligation.getId()]++;
	}

	public void remove(Obligation obligation) throws NonexistentObligationException {
		int count = countList[obligation.getId()];
		if (count <= 0)
			throw new NonexistentObligationException(obligation);
		invalidate();
		countList[obligation.getId()] = count - 1;
	}

	public int getCount(Obligation obligation) {
		return countList[obligation.getId()];
	}

	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;

		ObligationSet other = (ObligationSet) o;

		if (this.countList.length != other.countList.length)
			return false;

		for (int i = 0; i < this.countList.length; ++i) {
			if (this.countList[i] != other.countList[i])
				return false;
		}

		return false;
	}
	
	public ObligationSet duplicate() {
		ObligationSet dup = new ObligationSet(countList.length);
		System.arraycopy(this.countList, 0, dup.countList, 0, countList.length);
		return dup;
	}

	public int hashCode() {
		if (cachedHashCode == INVALID_HASH_CODE) {
			int value = 0;
			for (int i = 0; i < countList.length; ++i) {
				value *= 13;
				value += countList[i];
			}
			cachedHashCode = value;
		}
		return cachedHashCode;
	}

	private void invalidate() {
		this.cachedHashCode = INVALID_HASH_CODE;
	}
}

// vim:ts=4
