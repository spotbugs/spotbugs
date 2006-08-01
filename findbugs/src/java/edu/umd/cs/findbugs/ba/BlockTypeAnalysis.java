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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Dataflow analysis to determine the nesting of catch and finally
 * blocks within a method.
 *
 * @see BlockType
 * @author David Hovemeyer
 */
public class BlockTypeAnalysis implements DataflowAnalysis<BlockType> {
	private DepthFirstSearch dfs;
	private Map<BasicBlock, BlockType> startFactMap;
	private Map<BasicBlock, BlockType> resultFactMap;

	/**
	 * Constructor.
	 *
	 * @param dfs a DepthFirstSearch for the method to be analyzed
	 */
	public BlockTypeAnalysis(DepthFirstSearch dfs) {
		this.dfs = dfs;
		this.startFactMap = new HashMap<BasicBlock, BlockType>();
		this.resultFactMap = new HashMap<BasicBlock, BlockType>();
	}

	public BlockType createFact() {
		return new BlockType();
	}

	public BlockType getStartFact(BasicBlock block) {
		return lookupOrCreateFact(startFactMap, block);
	}

	public BlockType getResultFact(BasicBlock block) {
		return lookupOrCreateFact(resultFactMap, block);
	}

	public void copy(BlockType source, BlockType dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(BlockType result) throws DataflowAnalysisException {
		result.setNormal();
	}

	public void initResultFact(BlockType result) {
		makeFactTop(result);
	}

	public void makeFactTop(BlockType fact) {
		fact.setTop();
	}

	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostfixOrder(cfg, dfs);
	}

	public boolean same(BlockType fact1, BlockType fact2) {
		return fact1.sameAs(fact2);
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, BlockType start, BlockType result)
			throws DataflowAnalysisException {
		result.copyFrom(start);

		if (start.isValid()) {
			if (basicBlock.isExceptionHandler()) {
				CodeExceptionGen exceptionGen = basicBlock.getExceptionGen();
				ObjectType catchType = exceptionGen.getCatchType();
				if (catchType == null) {
					// Probably a finally block, or a synchronized block
					// exception-compensation catch block.
					result.pushFinally();
				} else {
					// Catch type was explicitly specified:
					// this is probably a programmer-written catch block
					result.pushCatch();
				}
			}
		}
	}

	public void meetInto(BlockType fact, Edge edge, BlockType result) throws DataflowAnalysisException {
		result.mergeWith(fact);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#startIteration()
	 */
	public void startIteration() {
		// nothing to do
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#finishIteration()
	 */
	public void finishIteration() {
		// nothing to do
	}

	private BlockType lookupOrCreateFact(Map<BasicBlock, BlockType> map, BasicBlock block) {
		BlockType fact = map.get(block);
		if (fact == null) {
			fact = createFact();
			map.put(block, fact);
		}
		return fact;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + BlockTypeAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		RepositoryLookupFailureCallback lookupFailureCallback = new DebugRepositoryLookupFailureCallback();

		AnalysisContext analysisContext = AnalysisContext.create(lookupFailureCallback);

		JavaClass jclass = new ClassParser(argv[0]).parse();
		ClassContext classContext = analysisContext.getClassContext(jclass);

		String methodName = SystemProperties.getProperty("blocktype.method");

		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (method.isNative() || method.isAbstract())
				continue;

			if (methodName != null && !methodName.equals(method.getName()))
				continue;

			System.out.println("Method: " + method.getName());

			CFG cfg = classContext.getCFG(method);
			DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);

			final BlockTypeAnalysis analysis = new BlockTypeAnalysis(dfs);
			Dataflow<BlockType, BlockTypeAnalysis> dataflow =
					new Dataflow<BlockType, BlockTypeAnalysis>(cfg, analysis);
			dataflow.execute();

			if (SystemProperties.getBoolean("blocktype.printcfg")) {
				CFGPrinter cfgPrinter = new CFGPrinter(cfg) {
					@Override
                                         public String blockAnnotate(BasicBlock block) {
						BlockType blockType = analysis.getResultFact(block);
						return " [Block type: " + blockType.toString() + "]";
					}
				};
				cfgPrinter.print(System.out);
			}
		}
	}
}

// vim:ts=4
