/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.meta.When;


import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.ba.ch.InterproceduralCallGraph;
import edu.umd.cs.findbugs.ba.ch.InterproceduralCallGraphEdge;
import edu.umd.cs.findbugs.ba.ch.InterproceduralCallGraphVertex;
import edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.util.Iterator;

/**
 * Find relevant type qualifiers needing to be checked
 * for a given method.
 * 
 * @author William Pugh
 */
public class Analysis {
	private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug.analysis");

	/**
	 * This system property enables additional work to try
	 * to detect all *effective* type qualifiers (direct,
	 * inherited, and default) applied to methods and called
	 * methods.
	 * 
	 * This step uses an interprocedural call graph.
	 */
	public static final boolean FIND_EFFECTIVE_RELEVANT_QUALIFIERS = 
		true; //SystemProperties.getBoolean("ctq.findeffective");
	public static final boolean DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS =
		FIND_EFFECTIVE_RELEVANT_QUALIFIERS && SystemProperties.getBoolean("ctq.findeffective.debug");

	/**
	 * Find relevant type qualifiers needing to be checked
	 * for a given method.
	 * 
	 * @param methodDescriptor a method
	 * @return Collection of relevant type qualifiers needing to be checked
	 * @throws CheckedAnalysisException
	 */
	public static Collection<TypeQualifierValue> getRelevantTypeQualifiers(
			MethodDescriptor methodDescriptor)
			throws CheckedAnalysisException {

		final HashSet<TypeQualifierValue> result = new HashSet<TypeQualifierValue>();

		XMethod xmethod = XFactory.createXMethod(methodDescriptor);
		
		if (FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
			if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
				System.out.println("**** Finding effective type qualifiers for " + xmethod);
			}
			
			//
			// This will take care of methods using fields annotated with
			// a type qualifier.
			//
			getDirectlyRelevantTypeQualifiers(xmethod, result);
			
			//
			// For all known type qualifiers, find the effective (direct, inherited,
			// or default) type qualifier annotations
			// on the method and all methods directly called by the method.
			//
			InterproceduralCallGraph callGraph = Global.getAnalysisCache().getDatabase(InterproceduralCallGraph.class);
			InterproceduralCallGraphVertex v = callGraph.lookupVertex(methodDescriptor);
			if (v != null) {
				addEffectiveRelevantQualifiers(result, xmethod);
				
				Iterator<InterproceduralCallGraphEdge> i = callGraph.outgoingEdgeIterator(v);
				while (i.hasNext()) {
					InterproceduralCallGraphVertex called = i.next().getTarget();
					if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
						System.out.println("  " + xmethod + " calls " + called.getXmethod());
					}
					addEffectiveRelevantQualifiers(result, called.getXmethod());
				}
				
				if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
					System.out.println("===> result: " + result);
				}
			}
		} else {
			//
			// XXX: this code can go away eventually
			//
			
			if (methodDescriptor.isStatic()) {
				getDirectlyRelevantTypeQualifiers(xmethod, result);
			} else {

				// Instance method - must consider type qualifiers inherited from superclasses

				InheritanceGraphVisitor visitor = new OverriddenMethodsVisitor(xmethod) {
					/* (non-Javadoc)
					 * @see edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor#visitOverriddenMethod(edu.umd.cs.findbugs.ba.XMethod)
					 */

					@Override
					protected boolean visitOverriddenMethod(XMethod xmethod) {
						getDirectlyRelevantTypeQualifiers(xmethod, result);
						return true;
					}
				};

				try {
					AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypes(xmethod.getClassDescriptor(), visitor);
				} catch (ClassNotFoundException e) {
					AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
					return (Collection<TypeQualifierValue>) Collections.EMPTY_SET;
				} catch (AnalysisException e) {
					AnalysisContext.currentAnalysisContext().getLookupFailureCallback().logError(
						"Error getting relevant type qualifiers for " + xmethod.toString(), e);
					return (Collection<TypeQualifierValue>) Collections.EMPTY_SET;
				}
			}	
		}

		return result;
		
	}

	private static void addEffectiveRelevantQualifiers(HashSet<TypeQualifierValue> result, XMethod xmethod) {
		if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
			System.out.println("  Finding effective qualifiers for " + xmethod);
		}
		
		for (TypeQualifierValue tqv : TypeQualifierValue.getAllKnownTypeQualifiers()) {
			if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
				System.out.print("    " + tqv + "...");
			}
			
			TypeQualifierAnnotation tqa;
			boolean add = false;
			
			tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, tqv);
			if (tqa != null) {
				add = true;
			}
			
			if (!add) {
				for (int i = 0; i < xmethod.getNumParams(); i++) {
					tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, i, tqv);
					if (tqa != null) {
						add = true;
						break;
					}
				}
			}
			
			if (add) {
				result.add(tqv);
			}
			
			if (DEBUG_FIND_EFFECTIVE_RELEVANT_QUALIFIERS) {
				System.out.println(add ? "YES" : "NO");
			}
		}
	}

	/**
	 * Update the set of directly-relevant type qualifiers
	 * for given method.
	 * 
	 * @param database the DirectlyRelevantTypeQualifiersDatabase
	 * @param xmethod a method
	 * @param defaultTypeQualifiers additional directly-relevant type qualifiers
	 *                              for the method
	 */
	private static void addDirectlyRelevantTypeQualifiers(DirectlyRelevantTypeQualifiersDatabase database, XMethod xmethod, Set<TypeQualifierValue> defaultTypeQualifiers) {
		Set<TypeQualifierValue> qualifiers = new HashSet<TypeQualifierValue>();
		qualifiers.addAll(database.getDirectlyRelevantTypeQualifiers(xmethod.getMethodDescriptor()));
		qualifiers.addAll(defaultTypeQualifiers);
		database.setDirectlyRelevantTypeQualifiers(xmethod.getMethodDescriptor(), new ArrayList<TypeQualifierValue>(qualifiers));
	}

//	private static void propagateInheritedAnnotations() {
//		// TODO
//	}

	private static void getDirectlyRelevantTypeQualifiers(XMethod xmethod,
		HashSet<TypeQualifierValue> result) {
		result.addAll(AnalysisContext.currentAnalysisContext().getDirectlyRelevantTypeQualifiersDatabase().getDirectlyRelevantTypeQualifiers(xmethod.getMethodDescriptor()));

	}

	/**
	 * @param result
	 * @param m
	 */
	public static void addKnownTypeQualifiersForParameters(HashSet<TypeQualifierValue> result, XMethod m) {
		int numParameters = new SignatureParser(m.getSignature()).getNumParameters();
		for (int p = 0; p < numParameters; p++) {
			addKnownTypeQualifiers(result, TypeQualifierApplications.getApplicableApplications(m, p));
		}
	}

	/**
     * @param result
     * @param applicableApplications
     */
    public static void addKnownTypeQualifiers(HashSet<TypeQualifierValue> result,
            Collection<TypeQualifierAnnotation> applicableApplications) {
	    for(TypeQualifierAnnotation t : applicableApplications)
	    	if (t.when != When.UNKNOWN)
	    		result.add(t.typeQualifier);
    }

}
