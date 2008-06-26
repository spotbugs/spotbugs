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

import edu.umd.cs.findbugs.ba.BasicBlock;
import org.apache.bcel.generic.ConstantPoolGen;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import javax.annotation.meta.When;

/**
 * Factory for BackwardTypeQualifierDataflow objects
 * for given type qualifier values.
 * 
 * @author David Hovemeyer
 */
public class BackwardTypeQualifierDataflowFactory
	extends TypeQualifierDataflowFactory
	<
	BackwardTypeQualifierDataflowAnalysis,
	BackwardTypeQualifierDataflow
	> {

	/**
	 * Constructor.
	 * 
	 * @param methodDescriptor MethodDescriptor of the method for which we
	 *                         want to create BackwardTypeQualifierDataflow objects
	 */
	public BackwardTypeQualifierDataflowFactory(MethodDescriptor methodDescriptor) {
		super(methodDescriptor);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowFactory#getDataflow(edu.umd.cs.findbugs.ba.DepthFirstSearch, edu.umd.cs.findbugs.ba.XMethod, edu.umd.cs.findbugs.ba.CFG, edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow, org.apache.bcel.generic.ConstantPoolGen, edu.umd.cs.findbugs.classfile.IAnalysisCache, edu.umd.cs.findbugs.classfile.MethodDescriptor, edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue)
	 */
	@Override
	protected BackwardTypeQualifierDataflow getDataflow(DepthFirstSearch dfs, XMethod xmethod, CFG cfg,
			ValueNumberDataflow vnaDataflow, ConstantPoolGen cpg, IAnalysisCache analysisCache,
			MethodDescriptor methodDescriptor, TypeQualifierValue typeQualifierValue) throws CheckedAnalysisException {
		ReverseDepthFirstSearch rdfs = analysisCache.getMethodAnalysis(ReverseDepthFirstSearch.class, methodDescriptor);
		
		BackwardTypeQualifierDataflowAnalysis analysis = new BackwardTypeQualifierDataflowAnalysis(
				dfs, rdfs, xmethod, cfg, vnaDataflow, cpg, typeQualifierValue
				);

		// Get the corresponding forward dataflow.
		// We use it to halt tracking of backwards values once we know
		// that they encounter a conflicting forward value.
		ForwardTypeQualifierDataflowFactory forwardFactory =
			analysisCache.getMethodAnalysis(ForwardTypeQualifierDataflowFactory.class, methodDescriptor);
		ForwardTypeQualifierDataflow forwardDataflow = forwardFactory.getDataflow(typeQualifierValue);
		analysis.setForwardTypeQualifierDataflow(forwardDataflow);
		analysis.registerSourceSinkLocations();
		
		BackwardTypeQualifierDataflow dataflow = new BackwardTypeQualifierDataflow(cfg, analysis);
		
		dataflow.execute();
		
		return dataflow;
	}

	@Override
	protected void populateDatabase(BackwardTypeQualifierDataflow dataflow, ValueNumberDataflow vnaDataflow, XMethod xmethod, TypeQualifierValue tqv) throws CheckedAnalysisException {
		assert TypeQualifierDatabase.USE_DATABASE;
		
		// Get the dataflow fact that propagated
		// back to the entry of the method.
		// This will contain the effective type qualifier
		// annotations on the method parameters.
		
		CFG cfg = dataflow.getCFG();
		
		assert cfg == vnaDataflow.getCFG();
		
		BasicBlock entry = dataflow.getCFG().getEntry();
		TypeQualifierValueSet entryFact = dataflow.getAnalysis().getResultFact(entry);
		
		for (int i = 0; i < xmethod.getNumParams(); i++) {
			if (TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, i, tqv) != null) {
				// this parameter already has an explicit annotation
				continue;
			}
			
			// Get the value number for this parameter
			ValueNumber paramVN = vnaDataflow.getAnalysis().getEntryValueForParameter(i);
			
			FlowValue paramFlowValue = entryFact.getValue(paramVN);
			if (paramFlowValue == FlowValue.ALWAYS || paramFlowValue == FlowValue.NEVER) {
				TypeQualifierDatabase tqdb = Global.getAnalysisCache().getDatabase(TypeQualifierDatabase.class);
				TypeQualifierAnnotation tqa = TypeQualifierAnnotation.getValue(
					tqv, paramFlowValue == FlowValue.ALWAYS ? When.ALWAYS : When.NEVER);
				tqdb.setParameter(xmethod.getMethodDescriptor(), i, tqv, tqa);
			}
		}
	}
}
