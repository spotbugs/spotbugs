/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
import java.util.HashSet;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analyze what type qualifiers need to be checked for particular methods.
 * 
 * @author Bill Pugh
 */
public class Analysis {
	
	/**
	 * Get Collection of TypeQualifierValues that should be checked
	 * in given method.
	 * 
	 * @param methodDescriptor MethodDescriptor identifying a method
	 * @return Collection of TypeQualifierValues that should be checked for the method
	 * @throws CheckedAnalysisException
	 */
	public static Collection<TypeQualifierValue> getRelevantTypeQualifiers(
			/*ClassContext context, Method method*/MethodDescriptor methodDescriptor)
			throws CheckedAnalysisException {
		
		HashSet<TypeQualifierValue> result = new HashSet<TypeQualifierValue>();
		
		//XMethod xMethod = XFactory.createXMethod(context.getJavaClass(), method);
		IAnalysisCache analysisCache = Global.getAnalysisCache();
		XMethod xMethod = analysisCache.getMethodAnalysis(XMethod.class, methodDescriptor);
		MethodGen methodGen = analysisCache.getMethodAnalysis(MethodGen.class, methodDescriptor);
		ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());
		
		Collection<TypeQualifierAnnotation> applicableApplicationsForMethod = TypeQualifierApplications.getApplicableApplications(xMethod);
		
		addKnownTypeQualifiers(result, applicableApplicationsForMethod);
		addKnownTypeQualifiersForParameters(result, xMethod);
		
		//CFG cfg = context.getCFG(method);
		
//		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
//			Location location = i.next();
//			Instruction ins = location.getHandle().getInstruction();
		InstructionList il = methodGen.getInstructionList();
		for (InstructionHandle handle : il.getInstructionHandles()) {
			Instruction ins = handle.getInstruction();
			if (ins instanceof FieldInstruction) {
				XField f = XFactory.createXField((FieldInstruction)ins, /*context.getConstantPoolGen()*/cpg);
				Collection<TypeQualifierAnnotation> applicableApplications = TypeQualifierApplications.getApplicableApplications(f);
				addKnownTypeQualifiers(result, applicableApplications);
			}
			else if (ins instanceof InvokeInstruction) {
				XMethod m = XFactory.createXMethod((InvokeInstruction)ins, /*context.getConstantPoolGen()*/cpg);
				Collection<TypeQualifierAnnotation> applicableApplications = TypeQualifierApplications.getApplicableApplications(m);
				addKnownTypeQualifiers(result, applicableApplications);
				addKnownTypeQualifiersForParameters(result, m);
				
			}
		}
		
		
		return result;
		
	}

	/**
     * @param result
     * @param m
     */
    private static void addKnownTypeQualifiersForParameters(HashSet<TypeQualifierValue> result, XMethod m) {
	    int numParameters = new SignatureParser(m.getSignature()).getNumParameters();
	    for(int p = 0; p < numParameters; p++)
	    	addKnownTypeQualifiers(result, TypeQualifierApplications.getApplicableApplications(m,p));
    }

	/**
     * @param result
     * @param applicableApplications
     */
    private static void addKnownTypeQualifiers(HashSet<TypeQualifierValue> result,
            Collection<TypeQualifierAnnotation> applicableApplications) {
	    for(TypeQualifierAnnotation t : applicableApplications)
	    	if (t.when != When.UNKNOWN)
	    		result.add(t.typeQualifier);
    }

}
