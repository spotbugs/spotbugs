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

import org.apache.bcel.generic.ConstantPoolGen;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import java.util.Iterator;
import javax.annotation.meta.When;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.ReturnInstruction;

/**
 * Factory for producing ForwardTypeQualifierDataflow objects
 * for various kinds of type qualifiers.
 * 
 * @author David Hovemeyer
 */
public class ForwardTypeQualifierDataflowFactory
	extends TypeQualifierDataflowFactory
	<
	ForwardTypeQualifierDataflowAnalysis,
	ForwardTypeQualifierDataflow
	> {

	/**
	 * Constructor.
	 * 
	 * @param methodDescriptor MethodDescriptor of method being analyzed
	 */
	public ForwardTypeQualifierDataflowFactory(MethodDescriptor methodDescriptor) {
		super(methodDescriptor);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowFactory#getDataflow(edu.umd.cs.findbugs.ba.DepthFirstSearch, edu.umd.cs.findbugs.ba.XMethod, edu.umd.cs.findbugs.ba.CFG, edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow, org.apache.bcel.generic.ConstantPoolGen, edu.umd.cs.findbugs.classfile.IAnalysisCache, edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	@Override
	protected ForwardTypeQualifierDataflow getDataflow(DepthFirstSearch dfs, XMethod xmethod, CFG cfg,
		ValueNumberDataflow vnaDataflow, ConstantPoolGen cpg, IAnalysisCache analysisCache, MethodDescriptor methodDescriptor,
		TypeQualifierValue typeQualifierValue) throws DataflowAnalysisException {
		ForwardTypeQualifierDataflowAnalysis analysis = new ForwardTypeQualifierDataflowAnalysis(
			dfs, xmethod, cfg, vnaDataflow, cpg, typeQualifierValue);
		analysis.registerSourceSinkLocations();

		ForwardTypeQualifierDataflow dataflow = new ForwardTypeQualifierDataflow(cfg, analysis);
		dataflow.execute();

		return dataflow;
	}

	@Override
	protected void populateDatabase(ForwardTypeQualifierDataflow dataflow, ValueNumberDataflow vnaDataflow, XMethod xmethod, TypeQualifierValue tqv) throws CheckedAnalysisException {
		assert TypeQualifierDatabase.USE_DATABASE;
		
		if (xmethod.getSignature().endsWith(")V")) {
			return;
		}
		
		// If there is no effective type qualifier annotation
		// on the method's return value, and we computed an
		// "interesting" (ALWAYS or NEVER) value for the return
		// value, add it to the database.
		
		TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, tqv);
		if (tqa == null) {
			// Find the meet of the flow values at all instructions
			// which return a value.
			FlowValue effectiveFlowValue = null;
			
			CFG cfg = dataflow.getCFG();
			Iterator<Location> i = cfg.locationIterator();
			while (i.hasNext()) {
				Location loc = i.next();
				InstructionHandle handle = loc.getHandle();
				Instruction ins = handle.getInstruction();
				if (ins instanceof ReturnInstruction && !(ins instanceof RETURN)) {
					ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(loc);
					ValueNumber topVN = vnaFrame.getTopValue();

					TypeQualifierValueSet flowSet = dataflow.getFactAtLocation(loc);
					FlowValue topFlowValue = flowSet.getValue(topVN);
					
					if (effectiveFlowValue == null) {
						effectiveFlowValue = topFlowValue;
					} else {
						effectiveFlowValue = FlowValue.meet(effectiveFlowValue, topFlowValue);
					}
				}
			}
			
			if (effectiveFlowValue == FlowValue.ALWAYS || effectiveFlowValue == FlowValue.NEVER) {
				TypeQualifierDatabase tqdb = Global.getAnalysisCache().getDatabase(TypeQualifierDatabase.class);
				
				tqa = TypeQualifierAnnotation.getValue(tqv, effectiveFlowValue == FlowValue.ALWAYS ? When.ALWAYS : When.NEVER);
				tqdb.setReturnValue(xmethod.getMethodDescriptor(), tqv, tqa);
			}
		}
	}
}
