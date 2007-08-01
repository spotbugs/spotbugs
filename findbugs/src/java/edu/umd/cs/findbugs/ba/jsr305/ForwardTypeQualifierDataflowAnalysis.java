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

import javax.annotation.meta.When;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
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
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowAnalysis#registerSourceSinkLocations()
	 */
	@Override
	public void registerSourceSinkLocations() throws DataflowAnalysisException {
		registerParameterSources();
		registerInstructionSources();
	}

	private void registerInstructionSources() throws DataflowAnalysisException {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			short opcode = location.getHandle().getInstruction().getOpcode();
			
			if (location.getHandle().getInstruction() instanceof InvokeInstruction) {
				// Model return value
				registerReturnValueSource(location);
			} else if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
				// Model field loads
				registerFieldLoadSource(location);
			}
		}
	}

	private void registerReturnValueSource(Location location) throws DataflowAnalysisException {
		// Nothing to do if called method does not return a reference value
		InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
		String calledMethodSig = inv.getSignature(cpg);
		if (!calledMethodSig.endsWith(";")) {
			return;
		}


		XMethod xmethod = XFactory.createXMethod(inv, cpg);
		if (xmethod.isResolved()) {
			TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, typeQualifierValue);
			When when = (tqa != null) ? tqa.when : When.UNKNOWN;
			registerTopOfStackSource(SourceSinkType.RETURN_VALUE_OF_CALLED_METHOD, location, when);
		}
	}

	private void registerFieldLoadSource(Location location) throws DataflowAnalysisException {
		XField loadedField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
		if (loadedField.isResolved()) {
			TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(loadedField, typeQualifierValue);
			When when = (tqa != null) ? tqa.when : When.UNKNOWN;
			registerTopOfStackSource(SourceSinkType.FIELD_LOAD, location, when);
		}

	}

	private void registerTopOfStackSource(SourceSinkType sourceSinkType, Location location, When when) throws DataflowAnalysisException {
		ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(location);
		if (vnaFrameAfterInstruction.isValid()) {
			ValueNumber tosValue = vnaFrameAfterInstruction.getTopValue();
			SourceSinkInfo sourceSinkInfo = new SourceSinkInfo(sourceSinkType, location, tosValue, when);
			registerSourceSink(sourceSinkInfo);
		}
	}

	private void registerParameterSources() {
		ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());

		SignatureParser sigParser = new SignatureParser(xmethod.getSignature());
		int firstParamSlot = xmethod.isStatic() ? 0 : 1;

		int param = 0;
		int slotOffset = 0;

		for (Iterator<String> i = sigParser.parameterSignatureIterator(); i.hasNext(); ) {
			String paramSig = i.next();

			// Get the TypeQualifierAnnotation for this parameter
			SourceSinkInfo info;
			TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, param, typeQualifierValue);
			When when = (tqa != null) ? tqa.when : When.UNKNOWN;
			ValueNumber vn = vnaFrameAtEntry.getValue(slotOffset + firstParamSlot);
			info = new SourceSinkInfo(SourceSinkType.PARAMETER, cfg.getLocationAtEntry(), vn, when);
			info.setParameterAndLocal(param, slotOffset + firstParamSlot);
			registerSourceSink(info);

			param++;
			slotOffset += SignatureParser.getNumSlotsForType(paramSig);
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
