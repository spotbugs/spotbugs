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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Factory for Obligation and ObligationSet objects to be
 * used in an instance of ObligationAnalysis.
 */
public class ObligationFactory {
	private Map<String, Obligation> classNameToObligationMap;
	
	static ObligationFactory instance;

	public ObligationFactory() {
		this.classNameToObligationMap = new HashMap<String, Obligation>();
		instance = this;
	}

	public int getMaxObligationTypes() {
		return classNameToObligationMap.size();
	}
	
	public Iterator<Obligation> obligationIterator() {
		return classNameToObligationMap.values().iterator();
	}

	public Obligation addObligation(String className) {
		int nextId = classNameToObligationMap.size();
		Obligation obligation = new Obligation(className, nextId);
		if (classNameToObligationMap.put(className, obligation) != null) {
			throw new IllegalStateException("Obligation " + className +
				" added multiple times");
		}
		return obligation;
	}
	
	public Obligation getObligationById(int id) {
		for (Iterator<Obligation> i = classNameToObligationMap.values().iterator(); i.hasNext();) {
			Obligation obligation = i.next();
			if (obligation.getId() == id)
				return obligation;
		}
		return null;
	}

//	public Obligation getObligation(String className) {
//		Obligation obligation = classNameToObligationMap.get(className);
//		if (obligation == null)
//			throw new IllegalArgumentException("Unknown obligation class " + className);
//		return obligation;
//	}

	public ObligationSet createObligationSet() {
		return new ObligationSet(getMaxObligationTypes());
	}
}

// vim:ts=4
