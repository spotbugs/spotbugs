/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba.vna;

import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.FrameDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;

/**
 * A dataflow analysis to track the production and flow of values in the Java
 * stack frame.  See the {@link ValueNumber ValueNumber} class for an explanation
 * of what the value numbers mean, and when they can be compared.
 *
 * <p>This class is still experimental.
 *
 * @author David Hovemeyer
 * @see ValueNumber
 * @see edu.umd.cs.findbugs.ba.DominatorsAnalysis
 */
public class ValueNumberAnalysis extends FrameDataflowAnalysis<ValueNumber, ValueNumberFrame> {

	public static final boolean DEBUG = SystemProperties.getBoolean("vna.debug");

	private MethodGen methodGen;
	private ValueNumberFactory factory;
	private ValueNumberFrameModelingVisitor visitor;
	private ValueNumber[] entryLocalValueList;
	private IdentityHashMap<BasicBlock, ValueNumber> exceptionHandlerValueNumberMap;
	private ValueNumber thisValue;
	private HashMap<Location, ValueNumberFrame> factAtLocationMap;
	private HashMap<Location, ValueNumberFrame> factAfterLocationMap;
	private MergeTree mergeTree;

	public ValueNumberAnalysis(MethodGen methodGen, DepthFirstSearch dfs,
							   LoadedFieldSet loadedFieldSet,
	                           RepositoryLookupFailureCallback lookupFailureCallback) {

		super(dfs);
		this.methodGen = methodGen;
		this.factory = new ValueNumberFactory();
		ValueNumberCache cache = new ValueNumberCache();
		this.visitor = new ValueNumberFrameModelingVisitor(methodGen, factory, cache,
				loadedFieldSet,
		        lookupFailureCallback);

		int numLocals = methodGen.getMaxLocals();
		this.entryLocalValueList = new ValueNumber[numLocals];
		for (int i = 0; i < numLocals; ++i)
			this.entryLocalValueList[i] = factory.createFreshValue();

		this.exceptionHandlerValueNumberMap = new IdentityHashMap<BasicBlock, ValueNumber>();

		// For non-static methods, keep track of which value represents the
		// "this" reference
		if (!methodGen.isStatic())
			this.thisValue = entryLocalValueList[0];

		this.factAtLocationMap = new HashMap<Location, ValueNumberFrame>();
		this.factAfterLocationMap = new HashMap<Location, ValueNumberFrame>();
		if (DEBUG) System.out.println("VNA Analysis " + methodGen.getClassName() + "." + methodGen.getName() + " : " + methodGen.getSignature());
		
	}
	
	public void setMergeTree(MergeTree mergeTree) {
		this.mergeTree = mergeTree;
	}
	
	public MergeTree getMergeTree() {
		return mergeTree;
	}

	public ValueNumberFactory getFactory() {
		return factory;
	}

	public int getNumValuesAllocated() {
		return factory.getNumValuesAllocated();
	}

	public boolean isThisValue(ValueNumber value) {
		return thisValue != null && thisValue.getNumber() == value.getNumber();
	}

	public ValueNumber getThisValue() {
		return thisValue;
	}

	public ValueNumber getEntryValue(int local) {
		return entryLocalValueList[local];
	}

	public ValueNumberFrame createFact() {
		return new ValueNumberFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(ValueNumberFrame result) {
		// Change the frame from TOP to something valid.
		result.setValid();

		// At entry to the method, each local has (as far as we know) a unique value.
		int numSlots = result.getNumSlots();
		for (int i = 0; i < numSlots; ++i)
			result.setValue(i, entryLocalValueList[i]);
	}

	@Override
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ValueNumberFrame fact)
	        throws DataflowAnalysisException {

		Location location = new Location(handle, basicBlock);

		ValueNumberFrame atLocation = getFactAtLocation(location);
		copy(fact, atLocation);

		visitor.setFrameAndLocation(fact, new Location(handle, basicBlock));
		visitor.setHandle(handle);
		visitor.analyzeInstruction(handle.getInstruction());

		ValueNumberFrame afterLocation = getFactAfterLocation(location);
		copy(fact, afterLocation);
	}

	public void meetInto(ValueNumberFrame fact, Edge edge, ValueNumberFrame result) throws DataflowAnalysisException {
		if (edge.getTarget().isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.

			// Get the value number for the exception
			BasicBlock handlerBlock = edge.getTarget();
			ValueNumber exceptionValueNumber = getExceptionValueNumber(handlerBlock);

			// Set up the stack frame
			ValueNumberFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			tmpFact.pushValue(exceptionValueNumber);
			fact = tmpFact;
		}

		mergeInto(fact, result);
	}

	@Override
	protected void mergeInto(ValueNumberFrame frame, ValueNumberFrame result) throws DataflowAnalysisException {
		result.mergeAvailableLoadSets(frame);
		super.mergeInto(frame, result);
	}
	
	@Override
	protected void mergeValues(ValueNumberFrame otherFrame, ValueNumberFrame resultFrame, int slot) throws DataflowAnalysisException {
		ValueNumber value =
			mergeValues(resultFrame, slot, resultFrame.getValue(slot), otherFrame.getValue(slot));
		resultFrame.setValue(slot, value);
	}

	private ValueNumber mergeValues(ValueNumberFrame frame, int slot, ValueNumber mine, ValueNumber other)
	        throws DataflowAnalysisException {

		// Merging slot values:
		//   - Merging identical values results in no change
		//   - If the values are different, and the value in the result
		//     frame is not the result of a previous result, a fresh value
		//     is allocated.
		//   - If the value in the result frame is the result of a
		//     previous merge, IT STAYS THE SAME.
		//
		// The "one merge" rule means that merged values are essentially like
		// phi nodes.  They combine some number of other values.

		// I believe (but haven't proved) that this technique is a dumb way
		// of computing SSA.

		if (mine != frame.getValue(slot)) throw new IllegalStateException();

		if (mine.equals(other))
			return mine;

		ValueNumber mergedValue = frame.getMergedValue(slot);
		if (mergedValue == null) {
			mergedValue = factory.createFreshValue();
			mergedValue.setFlags(mine.getFlags() | other.getFlags());
			frame.setMergedValue(slot, mergedValue);
			
		}

		if (mergeTree != null) {
			mergeTree.mapInputToOutput(mine, mergedValue);
			mergeTree.mapInputToOutput(other, mergedValue);
		}

		return mergedValue;
	}

	@Override
	public ValueNumberFrame getFactAtLocation(Location location) {
		ValueNumberFrame fact = factAtLocationMap.get(location);
		if (fact == null) {
			fact = createFact();
			makeFactTop(fact);
			factAtLocationMap.put(location, fact);
		}
		return fact;
	}

	@Override
	public ValueNumberFrame getFactAfterLocation(Location location) {
		ValueNumberFrame fact = factAfterLocationMap.get(location);
		if (fact == null) {
			fact = createFact();
			makeFactTop(fact);
			factAfterLocationMap.put(location, fact);
		}
		return fact;
	}

	/**
	 * Get an Iterator over all dataflow facts that we've recorded for
	 * the Locations in the CFG.  Note that this does not include
	 * result facts (since there are no Locations corresponding to
	 * the end of basic blocks).
	 */
	public Iterator<ValueNumberFrame> factIterator() {
		return factAtLocationMap.values().iterator();
	}

	// These fields are used by the compactValueNumbers() method.
	private static class ValueCompacter {
		public final BitSet valuesUsed;
		public int numValuesUsed;
		public final int[] discovered;

		public ValueCompacter(int origNumValuesAllocated) {
			valuesUsed = new BitSet();
			numValuesUsed = 0;

			// The "discovered" array tells us the mapping of old value numbers
			// to new (which are based on order of discovery).  Negative values
			// specify value numbers which are not actually used (and thus can
			// be purged.)
			discovered = new int[origNumValuesAllocated];
			for (int i = 0; i < discovered.length; ++i)
				discovered[i] = -1;
		}

		public boolean isUsed(int number) {
			return valuesUsed.get(number);
		}

		public void setUsed(int number) {
			valuesUsed.set(number, true);
		}

		public int allocateValue() {
			return numValuesUsed++;
		}
	}

	/**
	 * Compact the value numbers assigned.
	 * This should be done only after the dataflow algorithm has executed.
	 * This works by modifying the actual ValueNumber objects assigned.
	 * After this method is called, the getNumValuesAllocated() method
	 * of this object will return a value less than or equal to the value
	 * it would have returned before the call to this method.
	 * <p/>
	 * <p> <em>This method should be called at most once</em>.
	 *
	 * @param dataflow the Dataflow object which executed this analysis
	 *                 (and has all of the block result values)
	 */
	public void compactValueNumbers(Dataflow<ValueNumberFrame, ValueNumberAnalysis> dataflow) {
		ValueCompacter compacter = new ValueCompacter(factory.getNumValuesAllocated());

		// We can get all extant Frames by looking at the values in
		// the location to value map, and also the block result values.
		for (Iterator<ValueNumberFrame> i = factIterator(); i.hasNext();) {
			ValueNumberFrame frame = i.next();
			markFrameValues(frame, compacter);
		}
		for (Iterator<ValueNumberFrame> i = resultFactIterator(); i.hasNext();) {
			ValueNumberFrame frame = i.next();
			markFrameValues(frame, compacter);
		}

		int before = factory.getNumValuesAllocated();

		// Now the factory can modify the ValueNumbers.
		factory.compact(compacter.discovered, compacter.numValuesUsed);

		int after = factory.getNumValuesAllocated();

		if (DEBUG && after < before && before > 0)
			System.out.println("Value compaction: " + after + "/" + before + " (" +
			        ((after * 100) / before) + "%)");

	}

	/**
	 * Mark value numbers in a value number frame for compaction.
	 */
	private static void markFrameValues(ValueNumberFrame frame, ValueCompacter compacter) {
		// We don't need to do anything for top and bottom frames.
		if (!frame.isValid())
			return;

		for (int j = 0; j < frame.getNumSlots(); ++j) {
			ValueNumber value = frame.getValue(j);
			int number = value.getNumber();

			if (!compacter.isUsed(number)) {
				compacter.discovered[number] = compacter.allocateValue();
				compacter.setUsed(number);
			}
		}
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("Usage: edu.umd.cs.findbugs.ba.ValueNumberAnalysis <filename>");
				System.exit(1);
			}

			DataflowTestDriver<ValueNumberFrame, ValueNumberAnalysis> driver =
			        new DataflowTestDriver<ValueNumberFrame, ValueNumberAnalysis>() {
				        @Override
                                         public Dataflow<ValueNumberFrame, ValueNumberAnalysis> createDataflow(ClassContext classContext, Method method)
				                throws CFGBuilderException, DataflowAnalysisException {
					        return classContext.getValueNumberDataflow(method);
				        }
			        };

			driver.execute(argv[0]);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ValueNumber getExceptionValueNumber(BasicBlock handlerBlock) {
		ValueNumber valueNumber = exceptionHandlerValueNumberMap.get(handlerBlock);
		if (valueNumber == null) {
			valueNumber = factory.createFreshValue();
			exceptionHandlerValueNumberMap.put(handlerBlock, valueNumber);
		}
		return valueNumber;
	}
}

// vim:ts=4
