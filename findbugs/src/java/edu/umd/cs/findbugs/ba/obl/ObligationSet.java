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

import java.util.Arrays;


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

	private final short[] countList;
//	private final short[] whereCreated;
	private final ObligationFactory factory;
	private int cachedHashCode;

	public ObligationSet(/*int maxObligationTypes, */ObligationFactory factory) {
		this.countList = new short[factory.getMaxObligationTypes()];
//		this.whereCreated = new short[factory.getMaxObligationTypes()];
		this.factory = factory;
		invalidate();
	}

	public void add(Obligation obligation) {
		invalidate();
		countList[obligation.getId()]++;
	}

	public void remove(Obligation obligation) {
		invalidate();
		countList[obligation.getId()]--;  // = (short)(count - 1);
	}

	public int getCount(int id) {
		return countList[id];
	}

//	public int getCount(Obligation obligation) {
//		return getCount(obligation.getId());
//	}

//	/**
//	 * Called upon the first creation of given kind of obligation on path.
//	 * 
//	 * @param obligation    an Obligation
//	 * @param basicBlockId  BasicBlock id of first creation of the given obligation type
//	 */
//	public void setWhereCreated(Obligation obligation, int basicBlockId) {
//		assert getCount(obligation.getId()) == 1;
//		invalidate();
//
//		if (basicBlockId > Short.MAX_VALUE) {
//			whereCreated[obligation.getId()] = -1;
//			return;
//		}
//		
//		whereCreated[obligation.getId()] = (short) basicBlockId;
//	}
	
//	/**
//	 * Find out where the first instance of given obligation type was created.
//	 * 
//	 * @param obligation an obligation
//	 * @return id of basic block where created, or -1 if the basic block couldn't be stored
//	 */
//	public int getWhereCreated(Obligation obligation) {
//		return whereCreated[obligation.getId()];
//	}

	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;

		ObligationSet other = (ObligationSet) o;

		if (!Arrays.equals(this.countList, other.countList)
			/*|| !Arrays.equals(this.whereCreated, other.whereCreated)*/) {
			return false;
		}

		return true;
	}

	/*
	 * NOTE: this string is incorporated into a StringAnnotation when
	 * reporting OBL_ warnings, so the output needs to be
	 * user-friendly.
	 */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("{");
		int count = 0;
		for (int i = 0; i < countList.length; ++i) {
			if (countList[i] == 0)
				continue;
			if (count > 0)
				buf.append(",");
			buf.append(factory.getObligationById(i).toString());
			buf.append(" x ");
			buf.append(countList[i]);
			++count;
		}
		buf.append("}");
		//buf.append("@" + System.identityHashCode(this));
		return buf.toString();
	}
	
	public void copyFrom(ObligationSet other) {
		System.arraycopy(other.countList, 0, this.countList, 0, other.countList.length);
//		System.arraycopy(other.whereCreated, 0, this.whereCreated, 0, other.whereCreated.length);
		invalidate();
	}

	public ObligationSet duplicate() {
		ObligationSet dup = new ObligationSet(/*countList.length, */factory);
		dup.copyFrom(this);
		return dup;
	}

	@Override
	public int hashCode() {
		if (cachedHashCode == INVALID_HASH_CODE) {
			int value = 0;
			for (int i = 0; i < countList.length; ++i) {
				value += (13 * i * (countList[i]/* + whereCreated[i]*/));
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
