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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Find relevant type qualifiers needing to be checked
 * for a given method.
 * 
 * @author William Pugh
 */
public class Analysis {
	private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug.analysis");
	
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
		
		final IAnalysisCache analysisCache = Global.getAnalysisCache();
		final HashSet<TypeQualifierValue> result = new HashSet<TypeQualifierValue>();

		XMethod xmethod = XFactory.createXMethod(methodDescriptor);

		if (methodDescriptor.isStatic()) {
			getDirectlyRelevantTypeQualifiers(xmethod, analysisCache, result);
		} else {
			
			// Instance method - must consider type qualifiers inherited from superclasses
			
			InheritanceGraphVisitor visitor = new OverriddenMethodsVisitor(xmethod) {
				/* (non-Javadoc)
				 * @see edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor#visitOverriddenMethod(edu.umd.cs.findbugs.ba.XMethod)
				 */
				@Override
				protected boolean visitOverriddenMethod(XMethod xmethod) {
	                getDirectlyRelevantTypeQualifiers(xmethod, analysisCache, result);
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
		
		return result;
		
	}

	private static void getDirectlyRelevantTypeQualifiers(XMethod xmethod, IAnalysisCache analysisCache,
            HashSet<TypeQualifierValue> result)  {
		result.addAll(AnalysisContext.currentAnalysisContext().getDirectlyRelevantTypeQualifiersDatabase().getDirectlyRelevantTypeQualifiers(xmethod.getMethodDescriptor()));
	   
    }

	/**
     * @param result
     * @param m
     */
    public static void addKnownTypeQualifiersForParameters(HashSet<TypeQualifierValue> result, XMethod m) {
	    int numParameters = new SignatureParser(m.getSignature()).getNumParameters();
	    for(int p = 0; p < numParameters; p++)
	    	addKnownTypeQualifiers(result, TypeQualifierApplications.getApplicableApplications(m,p));
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
