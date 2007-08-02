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

import java.util.HashSet;
import java.util.Iterator;

import javax.annotation.meta.When;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDFSOrder;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Backwards type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public class BackwardTypeQualifierDataflowAnalysis extends TypeQualifierDataflowAnalysis {
	private static final boolean PRUNE_CONFLICTING_VALUES = true; //SystemProperties.getBoolean("ctq.pruneconflicting");
	private final DepthFirstSearch dfs;
	private final ReverseDepthFirstSearch rdfs;
	private ForwardTypeQualifierDataflow forwardTypeQualifierDataflow;

	/**
	 * Constructor.
	 * 
	 * @param dfs                DepthFirstSearch on the analyzed method
	 * @param rdfs               ReverseDepthFirstSearch on the analyzed method
	 * @param xmethod            XMethod for the analyzed method
	 * @param cfg                CFG of the analyzed method
	 * @param vnaDataflow        ValueNumberDataflow on the analyzed method
	 * @param cpg                ConstantPoolGen of the analyzed method
	 * @param typeQualifierValue TypeQualifierValue representing type qualifier the analysis should check
	 */
	public BackwardTypeQualifierDataflowAnalysis(
			DepthFirstSearch dfs,
			ReverseDepthFirstSearch rdfs,
			XMethod xmethod, CFG cfg, ValueNumberDataflow vnaDataflow, ConstantPoolGen cpg,
			TypeQualifierValue typeQualifierValue) {
		super(xmethod, cfg, vnaDataflow, cpg, typeQualifierValue);
		this.dfs = dfs;
		this.rdfs = rdfs;
	}
	
	/**
	 * @param forwardTypeQualifierDataflow The forwardTypeQualifierDataflow to set.
	 */
	public void setForwardTypeQualifierDataflow(ForwardTypeQualifierDataflow forwardTypeQualifierDataflow) {
		this.forwardTypeQualifierDataflow = forwardTypeQualifierDataflow;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowAnalysis#edgeTransfer(edu.umd.cs.findbugs.ba.Edge, edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet)
	 */
	@Override
	public void edgeTransfer(Edge edge, TypeQualifierValueSet fact) throws DataflowAnalysisException {
		if (PRUNE_CONFLICTING_VALUES && forwardTypeQualifierDataflow != null) {
			pruneConflictingValues(fact, forwardTypeQualifierDataflow.getFactOnEdge(edge));
		}
		
		super.edgeTransfer(edge, fact);
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
		
		if (PRUNE_CONFLICTING_VALUES && forwardTypeQualifierDataflow != null) {
			Location location = new Location(handle, basicBlock);
			pruneConflictingValues(fact, forwardTypeQualifierDataflow.getFactAfterLocation(location));
		}
		
		super.transferInstruction(handle, basicBlock, fact);
	}

	private void pruneConflictingValues(TypeQualifierValueSet fact, TypeQualifierValueSet forwardFact) {
		if (forwardFact.isValid()) {
			HashSet<ValueNumber> valueNumbers = new HashSet<ValueNumber>();
			valueNumbers.addAll(fact.getValueNumbers());
			valueNumbers.retainAll(forwardFact.getValueNumbers());

			for (ValueNumber vn : valueNumbers) {
				if (FlowValue.valuesConflict(forwardFact.getValue(vn), fact.getValue(vn))) {
					fact.pruneValue(vn);
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getBlockOrder(edu.umd.cs.findbugs.ba.CFG)
	 */
	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReverseDFSOrder(cfg, rdfs, dfs);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isForwards()
	 */
	public boolean isForwards() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowAnalysis#registerSourceSinkLocations()
	 */
	@Override
	public void registerSourceSinkLocations() throws DataflowAnalysisException {
		registerInstructionSinks();
	}

	private void registerInstructionSinks() throws DataflowAnalysisException {
		TypeQualifierAnnotation returnValueAnnotation = null;
		if (xmethod.isReturnTypeReferenceType()) {
			returnValueAnnotation =
				TypeQualifierApplications.getApplicableApplicationConsideringSupertypes(xmethod, typeQualifierValue);
		}
		
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			
			short opcode = location.getHandle().getInstruction().getOpcode();

			if (opcode == Constants.ARETURN) {
				modelReturn(returnValueAnnotation, location);
			} else if (opcode == Constants.PUTFIELD || opcode == Constants.PUTSTATIC) {
				modelFieldStore(location);
			} else if (location.getHandle().getInstruction() instanceof InvokeInstruction) {
				modelArguments(location);
			}
		}
	}

	private void modelReturn(TypeQualifierAnnotation returnValueAnnotation, Location location) throws DataflowAnalysisException {
		When when = (returnValueAnnotation != null) ? returnValueAnnotation.when : When.UNKNOWN;
		
		// Model return statement
		ValueNumberFrame vnaFrameAtReturn = vnaDataflow.getFactAtLocation(location);
		if (vnaFrameAtReturn.isValid()) {
			ValueNumber topValue = vnaFrameAtReturn.getTopValue();
			SourceSinkInfo sink = new SourceSinkInfo(SourceSinkType.RETURN_VALUE, location, topValue, when); 
			registerSourceSink(sink);
		}
	}

	private void modelFieldStore(Location location) throws DataflowAnalysisException {
		// Model field stores
		XField writtenField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
		TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplicationConsideringSupertypes(writtenField, typeQualifierValue);
		When when = (tqa != null) ? tqa.when : When.UNKNOWN;
		
		// The ValueNumberFrame *before* the FieldInstruction should
		// have the ValueNumber of the stored value on the top of the stack.
		ValueNumberFrame vnaFrameAtStore = vnaDataflow.getFactAtLocation(location);
		if (vnaFrameAtStore.isValid()) {
			ValueNumber vn = vnaFrameAtStore.getTopValue();
			SourceSinkInfo sink = new SourceSinkInfo(SourceSinkType.FIELD_STORE, location, vn, when); 
			registerSourceSink(sink);
		}
	}

	private void modelArguments(Location location) throws DataflowAnalysisException {
		// Model arguments to called method
		InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(location);

		XMethod calledMethod = XFactory.createXMethod(inv, cpg);
		SignatureParser sigParser = new SignatureParser(calledMethod.getSignature());

		for (Iterator<String> j = sigParser.parameterSignatureIterator(); j.hasNext(); ) {
			int param = 0;

			String paramSig = j.next();

			if (SignatureParser.isReferenceType(paramSig)) {

				TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplicationConsideringSupertypes(
						calledMethod,
						param,
						typeQualifierValue);
				When when = (tqa != null) ? tqa.when : When.UNKNOWN;

				ValueNumber vn = vnaFrame.getArgument(
						inv,
						cpg,
						param,
						sigParser);

				SourceSinkInfo info = new SourceSinkInfo(SourceSinkType.ARGUMENT_TO_CALLED_METHOD, location, vn, when);
				info.setParameter(param);

				registerSourceSink(info);
			}

			param++;
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDataflowAnalysis#propagateAcrossPhiNode(edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValueSet, edu.umd.cs.findbugs.ba.vna.ValueNumber, edu.umd.cs.findbugs.ba.vna.ValueNumber)
	 */
	@Override
	protected void propagateAcrossPhiNode(TypeQualifierValueSet fact, ValueNumber sourceVN, ValueNumber targetVN) {
		// Backwards analysis - propagate value from target to source
		fact.propagateAcrossPhiNode(targetVN, sourceVN);
	}
}
