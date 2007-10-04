/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.BitSet;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RET;
import org.apache.bcel.generic.StoreInstruction;

/**
 * Dataflow analysis to find live stores of locals.
 * This is just a backward analysis to see which loads
 * reach stores of the same local.
 *
 * <p> This analysis also computes which stores that were
 * killed by a subsequent store on any subsequent reachable path.
 * (The FindDeadLocalStores detector uses this information
 * to reduce false positives.)
 *
 * @author David Hovemeyer
 */
public class LiveLocalStoreAnalysis extends BackwardDataflowAnalysis<BitSet>
		implements Debug {
	private int topBit ;
	private int killedByStoreOffset;

	public LiveLocalStoreAnalysis(MethodGen methodGen, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
		super(rdfs, dfs);
		this.topBit = methodGen.getMaxLocals() * 2;
		this.killedByStoreOffset = methodGen.getMaxLocals();
	}

	public BitSet createFact() {
		return new BitSet();
	}

	public void copy(BitSet source, BitSet dest) {
		dest.clear();
		dest.or(source);
	}

	public void initEntryFact(BitSet result) throws DataflowAnalysisException {
		result.clear();
	}

	public void makeFactTop(BitSet fact) {
		fact.clear();
		fact.set(topBit);
	}

	public boolean same(BitSet fact1, BitSet fact2) {
		return fact1.equals(fact2);
	}

	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		verifyFact(fact);
		verifyFact(result);

		if (isTop(fact)) {
			// Nothing to do, result stays the same
		} else if (isTop(result)) {
			// Result is top, so it takes the value of fact
			copy(fact, result);
		} else {
			// Meet is union
			result.or(fact);
		}

		verifyFact(result);
	}

	@Override
		 public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BitSet fact)
		throws DataflowAnalysisException {
		if (!isFactValid(fact)) return;

		Instruction ins = handle.getInstruction();

		if (ins instanceof StoreInstruction) {
			// Local is stored: any live stores on paths leading
			// to this instruction are now dead

			LocalVariableInstruction store = (LocalVariableInstruction) ins;
			int local = store.getIndex();
			fact.clear(local);
			fact.set(local + killedByStoreOffset);
		}

		if (ins instanceof LoadInstruction || ins instanceof IINC || ins instanceof RET) {
			// Local is loaded: it will be live on any path leading
			// to this instruction

			IndexedInstruction load = (IndexedInstruction) ins;
			int local = load.getIndex();
			fact.set(local);
			fact.clear(local + killedByStoreOffset);
		}

		if (!isFactValid(fact)) throw new IllegalStateException("Fact become invalid");
	}

	@Override
		 public boolean isFactValid(BitSet fact) {
		verifyFact(fact);
		return !isTop(fact);
	}

	/**
     * @param fact
     */
    private void verifyFact(BitSet fact) {
	    if (VERIFY_INTEGRITY) {
			if (isTop(fact) && fact.nextSetBit(0) < topBit)
				throw new IllegalStateException();
		}
    }

	@Override
		 public String factToString(BitSet fact) {
		if (isTop(fact))
			return "[TOP]";
		StringBuilder buf = new StringBuilder("[ ");
		boolean empty = true;
		for(int i = 0; i < killedByStoreOffset; i++) {
			boolean killedByStore = killedByStore(fact, i);
			boolean storeAlive = isStoreAlive(fact, i);
			if (!storeAlive && !killedByStore) continue;
			if (!empty) buf.append(", ");
			empty = false;
			buf.append(i);
			if (storeAlive)
				buf.append("L");
			if (killedByStore)
				buf.append("k");
		}
		buf.append("]");
		return buf.toString();
	}

	/**
	 * Return whether or not given fact is the special TOP value.
	 */
	public boolean isTop(BitSet fact) {
		return fact.get(topBit);
	}

	/**
	 * Return whether or not a store of given local is alive.
	 *
	 * @param fact  a dataflow fact created by this analysis
	 * @param local the local
	 */
	public boolean isStoreAlive(BitSet fact, int local) {
		return fact.get(local);
	}

	/**
	 * Return whether or not a store of given local was killed
	 * by a subsequent (dominated) store.
	 */
	public boolean killedByStore(BitSet fact, int local) {
		return fact.get(local + killedByStoreOffset);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + LiveLocalStoreAnalysis.class.getName() +
				" <classfile>");
			System.exit(1);
		}

		String filename = argv[0];

		DataflowTestDriver<BitSet,LiveLocalStoreAnalysis> driver =
			new DataflowTestDriver<BitSet, LiveLocalStoreAnalysis>() {

			@Override
						 public Dataflow<BitSet, LiveLocalStoreAnalysis> createDataflow(ClassContext classContext, Method method)
					throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getLiveLocalStoreDataflow(method);
			}
		};

		driver.execute(filename);
	}
}

// vim:ts=4
