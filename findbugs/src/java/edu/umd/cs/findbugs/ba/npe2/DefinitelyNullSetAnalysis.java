/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe2;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.CompactLocationNumbering;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A simple null-pointer analysis that keeps track of which
 * value numbers are definitely known to be null.
 * 
 * @author David Hovemeyer
 */
public class DefinitelyNullSetAnalysis extends ForwardDataflowAnalysis<DefinitelyNullSet> {
	private ValueNumberDataflow vnaDataflow;
	private CompactLocationNumbering compactLocationNumbering;
	
	/**
	 * Constructor.
	 * 
	 * @param dfs                      DepthFirstSearch for the method
	 * @param vnaDataflow              value number dataflow for the method
	 * @param compactLocationNumbering CompactLocationNumbering for the method
	 */
	public DefinitelyNullSetAnalysis(
			DepthFirstSearch dfs,
			ValueNumberDataflow vnaDataflow,
			CompactLocationNumbering compactLocationNumbering) {
		super(dfs);
		this.vnaDataflow = vnaDataflow;
		this.compactLocationNumbering = compactLocationNumbering;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#isFactValid(java.lang.Object)
	 */
	@Override
	public boolean isFactValid(DefinitelyNullSet fact) {
		return fact.isValid();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org.apache.bcel.generic.InstructionHandle, edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
	 */
	@Override
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, DefinitelyNullSet fact) throws DataflowAnalysisException {
		Location location = new Location(handle, basicBlock);
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAfterLocation(location);
		
		if (!vnaFrame.isValid()) {
			fact.setTop();
			return;
		}
		
		// ACONST_NULL obviously produces a value that is DEFINITELY NULL.
		// LDC produces values that are NOT NULL.
		// NEW produces values that are NOT NULL.
		
		short opcode = handle.getInstruction().getOpcode();
		
		if (opcode == Constants.ACONST_NULL) {
			setTOS(vnaFrame, location, fact, true);
		} else if (opcode == Constants.LDC || opcode == Constants.NEW) {
			setTOS(vnaFrame, location, fact, false);
		}
		
		// TODO: for method invocations, check return annotation

		// Note that we don't handle IFNULL and IFNONNULL here.
		// Those are handled in the edge transfer function, because we need
		// to produce different values in each of the control successors.

		
	}

	/**
	 * Set the value on top of the stack as either null or unknown.
	 * 
	 * @param vnaFrame ValueNumberFrame at Location
	 * @param location the Location
	 * @param fact     DefinitelyNullSet to modify
	 * @param isNull   true if the TOS value is definitely null, false if unknown
	 * @throws DataflowAnalysisException 
	 */
	private void setTOS(ValueNumberFrame vnaFrame, Location location, DefinitelyNullSet fact, boolean isNull)
			throws DataflowAnalysisException {

		ValueNumber valueNumber = vnaFrame.getTopValue();
		fact.set(valueNumber.getNumber(), isNull);
		if (isNull) {
			fact.setAssignedNullLocation(valueNumber.getNumber(), compactLocationNumbering.getNumber(location));
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	public void copy(DefinitelyNullSet source, DefinitelyNullSet dest) {
		dest.makeSameAs(source);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
	 */
	public DefinitelyNullSet createFact() {
		// TODO: optionally create one with null locations
		return new DefinitelyNullSet(vnaDataflow.getAnalysis().getNumValuesAllocated());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(DefinitelyNullSet result) throws DataflowAnalysisException {
		// TODO: parameter annotations?
		result.clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(java.lang.Object)
	 */
	public void initResultFact(DefinitelyNullSet result) {
		result.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
	 */
	public void makeFactTop(DefinitelyNullSet fact) {
		fact.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object, edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void meetInto(DefinitelyNullSet fact, Edge edge, DefinitelyNullSet result) throws DataflowAnalysisException {
		// TODO: use edge information (ifnull, ifnonnull, etc.)

		result.mergeWith(fact);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object, java.lang.Object)
	 */
	public boolean same(DefinitelyNullSet fact1, DefinitelyNullSet fact2) {
		return fact1.equals(fact2);
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + DefinitelyNullSetAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}
		
		DataflowTestDriver<DefinitelyNullSet, DefinitelyNullSetAnalysis> driver =
			new DataflowTestDriver<DefinitelyNullSet, DefinitelyNullSetAnalysis>() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext, org.apache.bcel.classfile.Method)
			 */
			@Override
			public Dataflow<DefinitelyNullSet, DefinitelyNullSetAnalysis> createDataflow(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getDefinitelyNullSetDataflow(method);
			}
		};
		
		driver.execute(args[0]);
	}
}
