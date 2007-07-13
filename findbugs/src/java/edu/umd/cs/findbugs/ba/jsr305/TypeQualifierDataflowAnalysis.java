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

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Type qualifier dataflow analysis.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierDataflowAnalysis extends ForwardDataflowAnalysis<TypeQualifierValueSet> {

	private CFG cfg;

	/**
	 * Constructor.
	 * 
	 * @param dfs DepthFirstSearch on the control-flow graph of the method being analyzed
	 */
	public TypeQualifierDataflowAnalysis(DepthFirstSearch dfs, CFG cfg) {
		super(dfs);
		this.cfg = cfg;
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
		// TODO: implement
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
		// TODO: check type qualifier annotations on method parameters 
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
		
		if (cfg.getNumNonExceptionSucessors(edge.getSource()) > 0) {
			fact.downgradeMaybeNotToUnknown();
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
