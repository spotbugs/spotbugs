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

import java.util.Iterator;

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
import edu.umd.cs.findbugs.ba.SignatureParser;
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

			SignatureParser sigParser = new SignatureParser(xmethod.getSignature());
			int firstParamSlot = xmethod.isStatic() ? 0 : 1;

			int param = 0;
			int slot = 0;

			for (Iterator<String> i = sigParser.parameterSignatureIterator(); i.hasNext(); ) {
				String paramSig = i.next();

				// Get the TypeQualifierAnnotation for this parameter
				TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, param, typeQualifierValue);
				if (tqa != null) {
					SourceSinkInfo info = new SourceSinkInfo(SourceSinkType.PARAMETER, cfg.getLocationAtEntry());
					info.setParameterAndLocal(param, slot);

					entryFact.setValue(
							vnaFrameAtEntry.getValue(slot + firstParamSlot),
							flowValueFromWhen(tqa.when),
							info);
				}

				param++;
				slot += SignatureParser.getNumSlotsForType(paramSig);
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

		if (handle.getInstruction() instanceof InvokeInstruction) {
			// Model return value
			modelReturnValue(fact, location);
		} else if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
			// Model field loads
			modelFieldLoad(fact, location);
		}
	}

	private void modelReturnValue(TypeQualifierValueSet fact, Location location) throws DataflowAnalysisException {
		// Nothing to do if called method does not return a reference value
		InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
		String calledMethodSig = inv.getSignature(cpg);
		if (!calledMethodSig.endsWith(";")) {
			return;
		}
		
		FlowValue flowValue = null;
		SourceSinkInfo sourceInfo = new SourceSinkInfo(SourceSinkType.RETURN_VALUE_OF_CALLED_METHOD, location);

		XMethod xmethod = XFactory.createXMethod(inv, cpg);
		if (xmethod.isResolved()) {
			TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, typeQualifierValue);
			if (tqa != null) {
				flowValue = flowValueFromWhen(tqa.when);
			}
		}

		setTopOfStackValue(fact, flowValue, sourceInfo, location);
	}

	private void modelFieldLoad(TypeQualifierValueSet fact, Location location) throws DataflowAnalysisException {
		FlowValue flowValue = null;
		SourceSinkInfo sourceInfo = new SourceSinkInfo(SourceSinkType.FIELD_LOAD, location);

		XField loadedField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
		if (loadedField.isResolved()) {
			TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(loadedField, typeQualifierValue);
			if (tqa != null) {
				flowValue = flowValueFromWhen(tqa.when);
			}
		}

		setTopOfStackValue(fact, flowValue, sourceInfo, location);
	}

	private void setTopOfStackValue(TypeQualifierValueSet fact, FlowValue flowValue, SourceSinkInfo sourceInfo, Location location) throws DataflowAnalysisException {
		if (flowValue == null) {
			flowValue = FlowValue.MAYBE;
		}

		ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(location);
		if (vnaFrameAfterInstruction.isValid()) {
			ValueNumber tosValue = vnaFrameAfterInstruction.getTopValue();
			fact.setValue(tosValue, flowValue, sourceInfo);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowAnalysis#propagateAcrossPhiNode(edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet, edu.umd.cs.findbugs.ba.vna.ValueNumber, edu.umd.cs.findbugs.ba.vna.ValueNumber)
	 */
	@Override
	protected void propagateAcrossPhiNode(TypeQualifierValueSet fact, ValueNumber sourceVN, ValueNumber targetVN) {
		// Forward analysis - propagate from source to target
		fact.propagateAcrossPhiNode(sourceVN, targetVN);
	}
}
