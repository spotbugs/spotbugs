/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A ClassContext caches all of the auxiliary objects used to analyze
 * the methods of a class.  That way, these objects don't need to
 * be created over and over again.
 *
 * @author David Hovemeyer
 */
public class ClassContext {
	private JavaClass jclass;
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private IdentityHashMap<Method, MethodGen> methodGenMap = new IdentityHashMap<Method, MethodGen>();
	private IdentityHashMap<Method, CFG> cfgMap = new IdentityHashMap<Method, CFG>();
	private IdentityHashMap<Method, ValueNumberDataflow> vnaDataflowMap = new IdentityHashMap<Method, ValueNumberDataflow>();
	private IdentityHashMap<Method, IsNullValueDataflow> invDataflowMap = new IdentityHashMap<Method, IsNullValueDataflow>();
	private IdentityHashMap<Method, DepthFirstSearch> dfsMap = new IdentityHashMap<Method, DepthFirstSearch>();
	private IdentityHashMap<Method, TypeDataflow> typeDataflowMap = new IdentityHashMap<Method, TypeDataflow>();
	private IdentityHashMap<Method, BitSet> bytecodeMap = new IdentityHashMap<Method, BitSet>();
	private IdentityHashMap<Method, LockCountDataflow> anyLockCountDataflowMap =
		new IdentityHashMap<Method, LockCountDataflow>();
	private IdentityHashMap<Method, LockCountDataflow> thisLockCountDataflowMap =
		new IdentityHashMap<Method, LockCountDataflow>();
	private IdentityHashMap<Method, LockDataflow> lockDataflowMap = new IdentityHashMap<Method, LockDataflow>();
	private ClassGen classGen;
	private AssignedFieldMap assignedFieldMap;

	/**
	 * Constructor.
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass, RepositoryLookupFailureCallback lookupFailureCallback) {
		this.jclass = jclass;
		this.lookupFailureCallback = lookupFailureCallback;
		this.classGen = null;
		this.assignedFieldMap = null;
	}

	/**
	 * Get the JavaClass.
	 */
	public JavaClass getJavaClass() { return jclass; }

	/**
	 * Get the RepositoryLookupFailureCallback.
	 * @return the RepositoryLookupFailureCallback
	 */
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
	}

	/**
	 * Get a MethodGen object for given method.
	 * @param method the method
	 * @return the MethodGen object for the method, or null
	 *   if the method has no Code attribute (and thus cannot be analyzed)
	 */
	public MethodGen getMethodGen(Method method) {
		MethodGen methodGen = methodGenMap.get(method);
		if (methodGen == null && method.getCode() != null) {
			ConstantPoolGen cpg = getConstantPoolGen();
			methodGen = new MethodGen(method, jclass.getClassName(), cpg);
			methodGenMap.put(method, methodGen);
		}
		return methodGen;
	}

	/**
	 * Get a CFG for given method.
	 * @param method the method
	 * @return the CFG
	 * @throws CFGBuilderException if a CFG cannot be constructed for the method
	 */
	public CFG getCFG(Method method) throws CFGBuilderException {
		CFG cfg = cfgMap.get(method);
		if (cfg == null) {
			MethodGen methodGen = getMethodGen(method);
			//System.out.println("Building CFG for " + methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature());
			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			cfg = cfgBuilder.getCFG();
			cfg.assignEdgeIds(0);
			cfgMap.put(method, cfg);
		}
		return cfg;
	}

	/**
	 * Get the ConstantPoolGen used to create the MethodGens
	 * for this class.
	 * @return the ConstantPoolGen
	 */
	public ConstantPoolGen getConstantPoolGen() {
		if (classGen == null)
			classGen = new ClassGen(jclass);
		return classGen.getConstantPool();
	}

	/**
	 * Get a ValueNumberDataflow for given method.
	 * @param method the method
	 * @return the ValueNumberDataflow
	 */
	public ValueNumberDataflow getValueNumberDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		ValueNumberDataflow vnaDataflow = vnaDataflowMap.get(method);
		if (vnaDataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			ValueNumberAnalysis analysis = new ValueNumberAnalysis(methodGen, dfs, lookupFailureCallback);
			CFG cfg = getCFG(method);
			vnaDataflow = new ValueNumberDataflow(cfg, analysis);
			vnaDataflow.execute();
			vnaDataflowMap.put(method, vnaDataflow);
		}
		return vnaDataflow;
	}

	/**
	 * Get an IsNullValueDataflow for given method.
	 * @param method the method
	 * @return the IsNullValueDataflow
	 */
	public IsNullValueDataflow getIsNullValueDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		IsNullValueDataflow invDataflow = invDataflowMap.get(method);
		if (invDataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			CFG cfg = getCFG(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);

			IsNullValueAnalysis invAnalysis = new IsNullValueAnalysis(methodGen, cfg, vnaDataflow, dfs);
			invDataflow = new IsNullValueDataflow(cfg, invAnalysis);
			invDataflow.execute();

			invDataflowMap.put(method, invDataflow);
		}
		return invDataflow;
	}

	/**
	 * Get a TypeDataflow for given method.
	 * @param method the method
	 * @return the TypeDataflow
	 */
	public TypeDataflow getTypeDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		TypeDataflow typeDataflow = typeDataflowMap.get(method);
		if (typeDataflow == null ) {
			MethodGen methodGen = getMethodGen(method);
			CFG cfg = getCFG(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);

			TypeAnalysis typeAnalysis = new TypeAnalysis(methodGen, dfs, lookupFailureCallback);
			typeDataflow = new TypeDataflow(cfg, typeAnalysis);
			typeDataflow.execute();

			typeDataflowMap.put(method, typeDataflow);
		}
		return typeDataflow;
	}

	/**
	 * Get a DepthFirstSearch for given method.
	 * @param method the method
	 * @return the DepthFirstSearch
	 */
	public DepthFirstSearch getDepthFirstSearch(Method method) throws CFGBuilderException {
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

			Code code = method.getCode();
			if (code != null) {
				byte[] instructionList = code.getCode();
	
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
			}

			// Save the result in the map.
			bytecodeSet = result;
			bytecodeMap.put(method, bytecodeSet);
		}
		return bytecodeSet;
	}

	/**
	 * Get dataflow for AnyLockCountAnalysis for given method.
	 * @param method the method
	 * @return the Dataflow
	 */
	public LockCountDataflow getAnyLockCountDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {

		LockCountDataflow dataflow = anyLockCountDataflowMap.get(method);
		if (dataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			CFG cfg = getCFG(method);

			AnyLockCountAnalysis analysis = new AnyLockCountAnalysis(methodGen, vnaDataflow, dfs);
			dataflow = new LockCountDataflow(cfg, analysis);
			dataflow.execute();

			anyLockCountDataflowMap.put(method, dataflow);
		}

		return dataflow;
	}

	/**
	 * Get dataflow for ThisLockCountAnalysis for given method.
	 * @param method the method
	 * @return the Dataflow
	 */
	public LockCountDataflow getThisLockCountDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {

		LockCountDataflow dataflow = thisLockCountDataflowMap.get(method);
		if (dataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			CFG cfg = getCFG(method);

			ThisLockCountAnalysis analysis = new ThisLockCountAnalysis(methodGen, vnaDataflow, dfs);
			dataflow = new LockCountDataflow(cfg, analysis);
			dataflow.execute();

			thisLockCountDataflowMap.put(method, dataflow);
		}

		return dataflow;
	}

	/**
	 * Get dataflow for LockAnalysis for given method.
	 * @param method the method
	 * @return the LockDataflow
	 */
	public LockDataflow getLockDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {

		LockDataflow dataflow = lockDataflowMap.get(method);
		if (dataflow == null) {
			MethodGen methodGen = getMethodGen(method);
			ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			CFG cfg = getCFG(method);

			LockAnalysis analysis = new LockAnalysis(methodGen, vnaDataflow, dfs);
			dataflow = new LockDataflow(cfg, analysis);
			dataflow.execute();

			lockDataflowMap.put(method, dataflow);
		}
		return dataflow;
	}

	/**
	 * Get the assigned field map for the class.
	 * @return the AssignedFieldMap
	 * @throws ClassNotFoundException if a class lookup prevents
	 *   the class's superclasses from being searched for
	 *   assignable fields
	 */
	public AssignedFieldMap getAssignedFieldMap() throws ClassNotFoundException {
		if (assignedFieldMap == null) {
			assignedFieldMap = new AssignedFieldMap(this);
		}
		return assignedFieldMap;
	}
}

// vim:ts=4
