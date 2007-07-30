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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Dataflow;
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
	private TypeQualifierValueSet entryFact;
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
		
		Location location = new Location(handle, basicBlock);
		
		if (PRUNE_CONFLICTING_VALUES && forwardTypeQualifierDataflow != null) {
			pruneConflictingValues(fact, forwardTypeQualifierDataflow.getFactAfterLocation(location));
		}
		
		if (handle.getInstruction() instanceof InvokeInstruction) {
			modelMethodArguments(fact, location);
		} else if (handle.getInstruction() instanceof FieldInstruction) {
			modelFieldStore(fact, location);
		}
		
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

    private void modelMethodArguments(TypeQualifierValueSet fact, Location location)
            throws DataflowAnalysisException {
	    InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
	    ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(location);
	    
	    XMethod calledMethod = XFactory.createXMethod(inv, cpg);
	    SignatureParser sigParser = new SignatureParser(calledMethod.getSignature());
	    
	    boolean foundParamAnnotation = false;
	    
	    for (Iterator<String> i = sigParser.parameterSignatureIterator(); i.hasNext(); ) {
	    	int param = 0;
	    	
	    	String paramSig = i.next();
	    	
	    	if (SignatureParser.isReferenceType(paramSig)) {
	    		
	    		TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(
	    				calledMethod,
	    				param,
	    				typeQualifierValue);

    			FlowValue flowValue = (tqa != null) ? flowValueFromWhen(tqa.when) : FlowValue.UNKNOWN;
    			if (tqa != null) {
        			foundParamAnnotation = true;
    			}

    			ValueNumber vn = vnaFrame.getArgument(
	    					inv,
	    					cpg,
	    					param,
	    					sigParser);

    			SourceSinkInfo info = new SourceSinkInfo(SourceSinkType.ARGUMENT_TO_CALLED_METHOD, location);
    			info.setParameter(param);

    			fact.setValue(vn, flowValue, info);
	    	}
	    	
	    	param++;
	    }
	    
	    if (Dataflow.DEBUG && foundParamAnnotation) {
	    	System.out.println("After modeling param annotations: " + fact);
	    }
    }

    private void modelFieldStore(TypeQualifierValueSet fact, Location location)
            throws DataflowAnalysisException {
	    short opcode = location.getHandle().getInstruction().getOpcode();
	    if (opcode == Constants.PUTFIELD || opcode == Constants.PUTSTATIC) {
			FlowValue flowValue = null;
	    	
	    	XField writtenField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
	    	TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(writtenField, typeQualifierValue);
	    	flowValue = (tqa != null)  ? flowValueFromWhen(tqa.when) : FlowValue.UNKNOWN;

    		// The ValueNumberFrame *before* the FieldInstruction should
    		// have the ValueNumber of the stored value on the top of the stack.
    		ValueNumberFrame vnaFrameAtStore = vnaDataflow.getFactAtLocation(location);
    		if (vnaFrameAtStore.isValid()) {
    			ValueNumber vn = vnaFrameAtStore.getTopValue();
    			fact.setValue(vn, flowValue, new SourceSinkInfo(SourceSinkType.FIELD_STORE, location));
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
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(TypeQualifierValueSet result) throws DataflowAnalysisException {
		if (entryFact == null) {
			entryFact = createEntryFact();
		}
		
		result.makeSameAs(entryFact);
	}

	private TypeQualifierValueSet createEntryFact() throws DataflowAnalysisException {
		TypeQualifierValueSet fact = createFact();
		fact.makeValid();

		if (!xmethod.isReturnTypeReferenceType()) {
			return fact;
		}

		// Find the annotation on the method, which serves to annotate the return value 
		TypeQualifierAnnotation tqa = TypeQualifierApplications.getApplicableApplication(xmethod, typeQualifierValue);
		if (tqa == null) {
			if (DEBUG_VERBOSE) {
				System.out.println("No applicable type qualifier annotation on return value");
			}
			return fact;
		}
		if (DEBUG_VERBOSE) {
			System.out.println("Return value type qualifier annotation is " + tqa);
		}
		FlowValue flowValueOfReturnValue = flowValueFromWhen(tqa.when);

		// Apply the annotation to every return value
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			
			if (location.getHandle().getInstruction().getOpcode() == Constants.ARETURN) {
				ValueNumberFrame vnaFrameAtReturn = vnaDataflow.getFactAtLocation(location);
				if (!vnaFrameAtReturn.isValid()) {
					continue;
				}
				ValueNumber topValue = vnaFrameAtReturn.getTopValue();
				fact.setValue(topValue, flowValueOfReturnValue, new SourceSinkInfo(SourceSinkType.RETURN_VALUE, location));
			}
		}
		
		if (DEBUG_VERBOSE) {
			System.out.println("Entry fact (at cfg exit): " + fact.toString());
		}
		
		return fact;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isForwards()
	 */
	public boolean isForwards() {
		return false;
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
