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

package edu.umd.cs.findbugs.detect;

import java.util.Collection;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflow;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowAnalysis;
import edu.umd.cs.findbugs.ba.jsr305.ForwardTypeQualifierDataflowFactory;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet;
import edu.umd.cs.findbugs.bcel.CFGDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.MissingClassException;

/**
 * Check JSR-305 type qualifiers.
 * 
 * @author David Hovemeyer
 */
public class CheckTypeQualifiers extends CFGDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("ctq.debug");
	private static final boolean DEBUG_DATAFLOW = SystemProperties.getBoolean("ctq.dataflow.debug");
	
	private BugReporter bugReporter;
	
	public CheckTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.bcel.CFGDetector#visitMethodCFG(edu.umd.cs.findbugs.classfile.MethodDescriptor, edu.umd.cs.findbugs.ba.CFG)
	 */
	@Override
	protected void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException {
		if (DEBUG) {
			System.out.println("CheckTypeQualifiers: checking " + methodDescriptor.toString());
		}
		
		IAnalysisCache analysisCache = Global.getAnalysisCache();
		
		Collection<TypeQualifierValue> relevantQualifiers = Analysis.getRelevantTypeQualifiers(methodDescriptor);
		ForwardTypeQualifierDataflowFactory forwardDataflowFactory =
			analysisCache.getMethodAnalysis(ForwardTypeQualifierDataflowFactory.class, methodDescriptor);
		if (DEBUG) {
			System.out.println("  Relevant type qualifiers are " + relevantQualifiers);
		}
		
		for (TypeQualifierValue typeQualifierValue : relevantQualifiers) {
			try {
				checkQualifier(methodDescriptor, cfg, typeQualifierValue, forwardDataflowFactory);
			} catch (MissingClassException e) {
				bugReporter.reportMissingClass(e.getClassDescriptor());
			} catch (CheckedAnalysisException e) {
				bugReporter.logError("Exception checking type qualifier " + typeQualifierValue.toString(), e);
			}
		}
	}

	/**
	 * Check a specific TypeQualifierValue on a method.
	 * 
     * @param methodDescriptor       MethodDescriptor of method
     * @param cfg                    CFG of method
     * @param typeQualifierValue     TypeQualifierValue to check
	 * @param forwardDataflowFactory ForwardTypeQualifierDataflowFactory used to create forward dataflow analysis objects
     */
    private void checkQualifier(
    		MethodDescriptor methodDescriptor,
    		CFG cfg,
    		TypeQualifierValue typeQualifierValue,
    		ForwardTypeQualifierDataflowFactory forwardDataflowFactory) throws CheckedAnalysisException {
    	
    	if (DEBUG) {
    		System.out.println("----------------------------------------------------------------------");
    		System.out.println("Checking type qualifier " + typeQualifierValue.toString() + " on method " + methodDescriptor.toString());
    		System.out.println("----------------------------------------------------------------------");
    	}

    	ForwardTypeQualifierDataflow forwardDataflow = forwardDataflowFactory.getDataflow(typeQualifierValue);
    	
		if (DEBUG_DATAFLOW) {
			DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis> p =
				new DataflowCFGPrinter<TypeQualifierValueSet, ForwardTypeQualifierDataflowAnalysis>(forwardDataflow);
			p.print(System.out);
		}

    }

}
