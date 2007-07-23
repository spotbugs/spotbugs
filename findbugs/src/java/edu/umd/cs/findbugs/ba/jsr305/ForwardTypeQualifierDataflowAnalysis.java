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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReversePostOrder;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Forward type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public class ForwardTypeQualifierDataflowAnalysis extends TypeQualifierDataflowAnalysis {

	private static final boolean DEBUG_VERBOSE = SystemProperties.getBoolean("ctq.dataflow.debug.verbose");
	private final DepthFirstSearch dfs;
	private TypeQualifierValueSet entryFact;
	
	/**
	 * Constructor.
	 * 
	 * @param dfs                DepthFirstSearch on the analyzed method
	 * @param xmethod            XMethod for the analyzed method
	 * @param cfg                CFG of the analyzed method
	 * @param vnaDataflow        ValueNumberDataflow on the analyzed method
	 * @param cpg                ConstantPoolGen of the analyzed method
	 * @param typeQualifierValue TypeQualifierValue representing type qualifier the analysis should check
	 */
	public ForwardTypeQualifierDataflowAnalysis(
			DepthFirstSearch dfs,
			XMethod xmethod, CFG cfg, ValueNumberDataflow vnaDataflow, ConstantPoolGen cpg,
			TypeQualifierValue typeQualifierValue) {
		super(xmethod, cfg, vnaDataflow, cpg, typeQualifierValue);
		this.dfs = dfs;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getBlockOrder(edu.umd.cs.findbugs.ba.CFG)
	 */
	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostOrder(cfg, dfs);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isForwards()
	 */
	public boolean isForwards() {
		return true;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(TypeQualifierValueSet result) throws DataflowAnalysisException {
		if (entryFact == null) {
			entryFact = createFact();
			entryFact.makeValid();
			
			ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());

			int firstParamSlot = xmethod.isStatic() ? 0 : 1;
			for (int i = 0; i < xmethod.getNumParams(); i++) {
				// Get the TypeQualifierAnnotation for this parameter
				TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, i, typeQualifierValue);
				if (tqa != null) {
					entryFact.setValue(
							vnaFrameAtEntry.getValue(i + firstParamSlot),
							flowValueFromWhen(tqa.when),
							cfg.getLocationAtEntry());
				}
			}
		}
		
		result.makeSameAs(entryFact);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org.apache.bcel.generic.InstructionHandle, edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
	 */
	@Override
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, TypeQualifierValueSet fact)
			throws DataflowAnalysisException {
		
		if (!fact.isValid()) {
			return;
		}
		
		Location location = new Location(handle, basicBlock);
		short opcode = handle.getInstruction().getOpcode();
		TypeQualifierAnnotation topOfStack = null;
		Location sourceLoc = null;
		
		if (handle.getInstruction() instanceof InvokeInstruction) {
			// Model return value
			XMethod calledMethod = XFactory.createXMethod((InvokeInstruction) handle.getInstruction(), cpg);
			if (calledMethod.isResolved()) {
				if (DEBUG_VERBOSE) {
					System.out.print("  checking annotation on " + calledMethod.toString() + " ==> ");
				}
				topOfStack = TypeQualifierApplications.getApplicableApplication(calledMethod, typeQualifierValue);
				if (DEBUG_VERBOSE) {
					System.out.println(topOfStack != null ? topOfStack.toString() : "<none>");
				}
				sourceLoc = location;
			}
		} else if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
			// Model field loads
			XField loadedField = XFactory.createXField((FieldInstruction) handle.getInstruction(), cpg);
			if (loadedField.isResolved()) {
				topOfStack = TypeQualifierApplications.getApplicableApplication(loadedField, typeQualifierValue);
				sourceLoc = location;
			}
		}
		
		if (topOfStack != null) {
			assert sourceLoc != null;
			
			ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));
			if (vnaFrameAfterInstruction.isValid()) {
				ValueNumber topValue = vnaFrameAfterInstruction.getTopValue();
				FlowValue flowValue = flowValueFromWhen(topOfStack.when);
				if (DEBUG_VERBOSE) {
					System.out.println("  Setting value " + topValue + " ==> " + flowValue);
				}
				fact.setValue(topValue, flowValue, sourceLoc);
				if (DEBUG_VERBOSE) {
					System.out.println("  fact = " + factToString(fact));
				}
			}
		}
		
	}

}
