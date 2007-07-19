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

import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * Type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierDataflowAnalysis extends ForwardDataflowAnalysis<TypeQualifierValueSet> {

	private final XMethod xmethod;
	private final CFG cfg;
	private final ValueNumberDataflow vnaDataflow;
	private final TypeQualifierValue typeQualifierValue;
	private final ConstantPoolGen cpg;
	private TypeQualifierValueSet entryFact;

	/**
	 * Constructor.
	 * 
	 * @param dfs                DepthFirstSearch on the control-flow graph of the method being analyzed
	 * @param xmethod            XMethod object containing information about the method being analyzed
	 * @param cfg                the control-flow graph (CFG) of the method being analyzed
	 * @param vnaDataflow        ValueNumberDataflow for the method
	 * @param typeQualifierValue the TypeQualifierValue we want the dataflow analysis to check
	 */
	public TypeQualifierDataflowAnalysis(
			DepthFirstSearch dfs,
			XMethod xmethod,
			CFG cfg,
			ValueNumberDataflow vnaDataflow,
			ConstantPoolGen cpg, 
			TypeQualifierValue typeQualifierValue) {
		super(dfs);
		this.xmethod = xmethod;
		this.cfg = cfg;
		this.vnaDataflow = vnaDataflow;
		this.cpg = cpg;
		this.typeQualifierValue = typeQualifierValue;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#isFactValid(java.lang.Object)
	 */
	@Override
	public boolean isFactValid(TypeQualifierValueSet fact) {
		return fact.isValid();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org.apache.bcel.generic.InstructionHandle, edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
	 */
	@Override
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, TypeQualifierValueSet fact)
			throws DataflowAnalysisException {
		
		short opcode = handle.getInstruction().getOpcode();
		TypeQualifierAnnotation topOfStack = null;
		
		if (handle.getInstruction() instanceof InvokeInstruction) {
			// Model return value
			XMethod calledMethod = XFactory.createXMethod((InvokeInstruction) handle.getInstruction(), cpg);
			if (calledMethod.isResolved()) {
				topOfStack = TypeQualifierApplications.getApplicableApplication(calledMethod, typeQualifierValue);
			}
		} else if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
			// Model field loads
			XField loadedField = XFactory.createXField((FieldInstruction) handle.getInstruction(), cpg);
			if (loadedField.isResolved()) {
				topOfStack = TypeQualifierApplications.getApplicableApplication(loadedField, typeQualifierValue);
			}
		}
		
		if (topOfStack != null) {
			ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));
			if (vnaFrameAfterInstruction.isValid()) {
				ValueNumber topValue = vnaFrameAfterInstruction.getTopValue();
				fact.setValue(topValue, flowValueFromWhen(topOfStack.when));
			}
		}
		
	}
	
	protected FlowValue flowValueFromWhen(When when) {
		throw new UnsupportedOperationException();// XXX
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	public void copy(TypeQualifierValueSet source, TypeQualifierValueSet dest) {
		dest.makeSameAs(source);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
	 */
	public TypeQualifierValueSet createFact() {
		return new TypeQualifierValueSet();
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
					entryFact.setValue(vnaFrameAtEntry.getValue(i + firstParamSlot), flowValueFromWhen(tqa.when));
				}
			}
		}
		
		result.makeSameAs(entryFact);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isTop(java.lang.Object)
	 */
	public boolean isTop(TypeQualifierValueSet fact) {
		return fact.isTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
	 */
	public void makeFactTop(TypeQualifierValueSet fact) {
		fact.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.BasicAbstractDataflowAnalysis#edgeTransfer(edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	@Override
	public void edgeTransfer(Edge edge, TypeQualifierValueSet fact) throws DataflowAnalysisException {
		if (!fact.isValid()) {
			return;
		}
		
		if (cfg.getNumNonExceptionSucessors(edge.getSource()) > 1) {
			fact.onBranch();
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object, edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void meetInto(TypeQualifierValueSet fact, Edge edge, TypeQualifierValueSet result) throws DataflowAnalysisException {
		if (fact.isTop() || result.isBottom()) {
			// result does not change
			return;
		} else if (fact.isBottom() || result.isTop()) {
			result.makeSameAs(fact);
			return;
		}

		assert fact.isValid();
		assert result.isValid();

		result.mergeWith(fact);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object, java.lang.Object)
	 */
	public boolean same(TypeQualifierValueSet fact1, TypeQualifierValueSet fact2) {
		return fact1.equals(fact2);
	}

}
