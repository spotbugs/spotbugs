/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.findbugs;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * A ClassContext caches all of the auxiliary objects used to analyze
 * the methods of a class.  That way, they are shared by all of the
 * Detectors, rather than being recreated for each Detector.
 *
 * @author David Hovemeyer
 */
public class ClassContext {
	private JavaClass jclass;
	private IdentityHashMap<Method, MethodGen> methodGenMap = new IdentityHashMap<Method, MethodGen>();
	private IdentityHashMap<Method, CFG> cfgMap = new IdentityHashMap<Method, CFG>();
	private IdentityHashMap<Method, ValueNumberDataflow> vnaDataflowMap = new IdentityHashMap<Method, ValueNumberDataflow>();
	private IdentityHashMap<Method, DepthFirstSearch> dfsMap = new IdentityHashMap<Method, DepthFirstSearch>();
	private IdentityHashMap<Method, BitSet> bytecodeMap = new IdentityHashMap<Method, BitSet>();
	private ClassGen classGen;

	/**
	 * Constructor.
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass) {
		this.jclass = jclass;
		this.classGen = null;
	}

	/**
	 * Get the JavaClass.
	 */
	public JavaClass getJavaClass() { return jclass; }

	/**
	 * Get a MethodGen object for given method.
	 * @param method the method
	 * @return the MethodGen object for the method
	 */
	public MethodGen getMethodGen(Method method) {
		MethodGen methodGen = methodGenMap.get(method);
		if (methodGen == null) {
			if (classGen == null)
				classGen = new ClassGen(jclass);
			ConstantPoolGen cpg = classGen.getConstantPool();
			methodGen = new MethodGen(method, jclass.getClassName(), cpg);
			methodGenMap.put(method, methodGen);
		}
		return methodGen;
	}

	/**
	 * Get a CFG for given method.
	 * @param method the method
	 * @return the CFG
	 */
	public CFG getCFG(Method method) {
		CFG cfg = cfgMap.get(method);
		if (cfg == null) {
			MethodGen methodGen = getMethodGen(method);
			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			cfg = cfgBuilder.getCFG();
			cfg.assignEdgeIds(0);
			cfgMap.put(method, cfg);
		}
		return cfg;
	}

	/**
	 * Get a ValueNumberDataflow for given method.
	 * @param method the method
	 * @return the ValueNumberDataflow
	 */
	public ValueNumberDataflow getValueNumberDataflow(Method method) throws DataflowAnalysisException {
		ValueNumberDataflow vnaDataflow = vnaDataflowMap.get(method);
		if (vnaDataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberAnalysis analysis = new ValueNumberAnalysis(methodGen);
			CFG cfg = getCFG(method);
			vnaDataflow = new ValueNumberDataflow(cfg, analysis);
			vnaDataflow.execute();
			vnaDataflowMap.put(method, vnaDataflow);
		}
		return vnaDataflow;
	}

	/**
	 * Get a DepthFirstSearch for given method.
	 * @param method the method
	 * @return the DepthFirstSearch
	 */
	public DepthFirstSearch getDepthFirstSearch(Method method) {
		DepthFirstSearch dfs = dfsMap.get(method);
		if (dfs == null) {
			CFG cfg = getCFG(method);
			dfs = new DepthFirstSearch(cfg);
			dfs.search();
			dfsMap.put(method, dfs);
		}
		return dfs;
	}

	/**
	 * Get a BitSet representing the bytecodes that are used in the given method.
	 * This is useful for prescreening a method for the existence of particular instructions.
	 * Because this step doesn't require building a MethodGen, it is very
	 * fast and memory-efficient.  It may allow a Detector to avoid some
	 * very expensive analysis, which is a Big Win for the user.
	 *
	 * @param method the method
	 * @return the BitSet containing the opcodes which appear in the method
	 */
	public BitSet getBytecodeSet(Method method) {
		BitSet bytecodeSet = bytecodeMap.get(method);
		if (bytecodeSet == null) {
			final BitSet result = new BitSet();
			byte[] instructionList = method.getCode().getCode();

			// Create a callback to put the opcodes of the method's
			// bytecode instructions into the BitSet.
			BytecodeScanner.Callback callback = new BytecodeScanner.Callback() {
				public void handleInstruction(int opcode) {
					result.set(opcode, true);
				}
			};

			// Scan the method.
			BytecodeScanner scanner = new BytecodeScanner();
			scanner.scan(instructionList, callback);

			// Save the result in the map.
			bytecodeSet = result;
			bytecodeMap.put(method, bytecodeSet);
		}
		return bytecodeSet;
	}
}

// vim:ts=4
