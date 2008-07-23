/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

//import edu.umd.cs.findbugs.SystemProperties;

import java.util.Collection;
import org.apache.bcel.generic.ReferenceType;

//import edu.umd.cs.findbugs.ba.ch.Subtypes2;
//import edu.umd.cs.findbugs.classfile.Global;
//import edu.umd.cs.findbugs.classfile.IAnalysisCache;
//import edu.umd.cs.findbugs.util.StringMatcher;
//import edu.umd.cs.findbugs.util.TypeMatcher;
//import java.util.Collection;
//import java.util.Iterator;
//import java.util.LinkedList;
//import org.apache.bcel.generic.ReferenceType;
//import org.apache.bcel.generic.Type;

/**
 * ObligationPolicyDatabaseEntry which adds or deletes
 * (almost certainly deletes) obligations
 * passed as method parameters.
 * This is useful for dealing with wrapper methods to close
 * resources (which are quite common, despite what
 * the Weimer/Necula paper says :-)
 * 
 * @author David Hovemeyer
 */
public class MatchObligationParametersEntry implements ObligationPolicyDatabaseEntry {

	public ObligationPolicyDatabaseEntryType getEntryType() {
		throw new UnsupportedOperationException("Do not use this class");
	}

	public boolean getActions(ReferenceType receiverType, String methodName, String signature, boolean isStatic, Collection<ObligationPolicyDatabaseAction> actionList) {
		throw new UnsupportedOperationException("Do not use this class");
	}
//	private static final boolean DEBUG = SystemProperties.getBoolean("oa.debug.db.entry");
//	
//	private TypeMatcher receiverType;
//	private StringMatcher methodName;
//	private ObligationPolicyDatabaseActionType actionType;
//	
//	public MatchObligationParametersEntry(TypeMatcher receiverType, StringMatcher methodName, ObligationPolicyDatabaseActionType actionType) {
//		this.receiverType = receiverType;
//		this.methodName = methodName;
//		this.actionType = actionType;
//	}
//
//	public boolean getActions(ReferenceType receiverType,
//			String methodName, String signature, boolean isStatic, Collection<ObligationPolicyDatabaseAction> actionList) {
//		if (!this.receiverType.matches(receiverType)
//			|| !this.methodName.matches(methodName)) {
//			return false;
//		}
//		
//		// See if any of the method's parameters are obligation types.
//		Type[] paramTypes = Type.getArgumentTypes(signature);
//		if (paramTypes.length == 0) {
//			// no parameters
//			return false;
//		}
//		
//		IAnalysisCache analysisCache = Global.getAnalysisCache();
//		ObligationPolicyDatabase database = analysisCache.getDatabase(ObligationPolicyDatabase.class);
//		Subtypes2 subtypes2 = analysisCache.getDatabase(Subtypes2.class);
//		
//		if (DEBUG) {
//			System.out.println("Check call to " + receiverType + "." + methodName + ":" + signature);
//		}
//		
//		// For known Obligation types...
//		LinkedList<ObligationPolicyDatabaseAction> toAdd = new LinkedList<ObligationPolicyDatabaseAction>();
//		for (Iterator<Obligation> i = database.getFactory().obligationIterator(); i.hasNext(); ) {
//			Obligation obligation = i.next();
//			// For each method parameter...
//			for (Type paramType : paramTypes) {
//				if (paramType instanceof ReferenceType) {
//					// Is parameter type an obligation type?
//					try {
//						if (DEBUG) {
//							System.out.print("  check param " + paramType + " instanceof " + obligation.getType() + "...");
//						}
//						boolean isSubtype = subtypes2.isSubtype((ReferenceType) paramType, obligation.getType());
//						if (DEBUG) {
//							System.out.println(isSubtype ? "FOUND" : "no");
//						}
//						if (isSubtype) {
//							// Parameter is an obligation type.
//							// Add an appropriate action.
//							toAdd.add(new ObligationPolicyDatabaseAction(actionType, obligation));
//						}
//					} catch (ClassNotFoundException e) {
//						if (DEBUG) {
//							System.out.println("[ClassNotFoundException");
//						}
//						// Hmm...
//						analysisCache.getErrorLogger().reportMissingClass(e);
//					}
//				}
//			}
//		}
//		
//		actionList.addAll(toAdd);
//		
////		if (receiverType.getSignature().contains("IOUtilities") && toAdd.isEmpty()) {
////			throw new IllegalStateException();
////		}
//		
//		return !toAdd.isEmpty();
//	}
//
//	@Override
//	public String toString() {
//		return "(" + receiverType + "," + "parameters of " + methodName + "," + actionType + ")";
//	}

}
