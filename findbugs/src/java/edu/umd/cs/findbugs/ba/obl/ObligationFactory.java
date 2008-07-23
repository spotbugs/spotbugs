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

import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.Global;
import org.apache.bcel.generic.Type;

/**
 * Factory for Obligation and ObligationSet objects to be
 * used in an instance of ObligationAnalysis.
 */
public class ObligationFactory {
	private Map<String, Obligation> classNameToObligationMap;

//	// XXX: this is just for debugging.
//	static ObligationFactory lastInstance;

	@SuppressWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
	public ObligationFactory() {
		this.classNameToObligationMap = new HashMap<String, Obligation>();
//		lastInstance = this;
	}

	public int getMaxObligationTypes() {
		return classNameToObligationMap.size();
	}

	public Iterator<Obligation> obligationIterator() {
		return classNameToObligationMap.values().iterator();
	}

	/**
	 * Look up an Obligation by type.
	 * This returns the first Obligation that is a supertype
	 * of the type given (meaning that the given type could
	 * be an instance of the returned Obligation).
	 * 
	 * @param type a type
	 * @return an Obligation that is a supertype of the given type,
	 *         or null if there is no such Obligation
	 * @throws ClassNotFoundException
	 */
	public Obligation getObligationByType(ObjectType type)
			throws ClassNotFoundException {
		for (Iterator<Obligation> i = obligationIterator(); i.hasNext(); ) {
			Obligation obligation = i.next();
			if (Hierarchy.isSubtype(type, obligation.getType()))
				return obligation;
		}
		return null;
	}

	/**
	 * Get array of Obligation types corresponding to
	 * the parameters of the given method.
	 * 
	 * @param xmethod a method
	 * @return array of Obligation types for each of the method's parameters;
	 *         a null element means the corresponding parameter is not
	 *         an Obligation type
	 */
	public Obligation[] getParameterObligationTypes(XMethod xmethod) {
		Type[] paramTypes = Type.getArgumentTypes(xmethod.getSignature());
		Obligation[] result = new Obligation[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			if (!(paramTypes[i] instanceof ObjectType)) {
				continue;
			}
			try {
				result[i] = getObligationByType((ObjectType) paramTypes[i]);
			} catch (ClassNotFoundException e) {
				Global.getAnalysisCache().getErrorLogger().reportMissingClass(e);
			}
		}
		return result;
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
		for (Obligation obligation : classNameToObligationMap.values()) {
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
		return new ObligationSet(/*getMaxObligationTypes(), */this);
	}
}

// vim:ts=4
