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
public class ClassContext implements AnalysisFeatures {
	public static final boolean PRUNE_INFEASIBLE_EXCEPTION_EDGES = Boolean.getBoolean("cfg.prune");

	/**
	 * Only try to determine unconditional exception throwers
	 * if we're not trying to conserve space.
	 */
	public static final boolean PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES =
		!CONSERVE_SPACE;

	public static final boolean DEBUG = Boolean.getBoolean("classContext.debug");

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
	private IdentityHashMap<Method, LockDataflow> lockDataflowMap = new IdentityHashMap<Method, LockDataflow>();
	private IdentityHashMap<Method, ReturnPathDataflow> returnPathDataflowMap =
		new IdentityHashMap<Method, ReturnPathDataflow>();
	private ClassGen classGen;
	private AssignedFieldMap assignedFieldMap;

	/**
	 * Constructor.
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass, RepositoryLookupFailureCallback lookupFailureCallback) {
		if (lookupFailureCallback == null) throw new IllegalArgumentException();
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

	private static final int PRUNED_INFEASIBLE_EXCEPTIONS = 1;
	private static final int PRUNED_UNCONDITIONAL_THROWERS = 2;

	/**
	 * Get a "raw" CFG for given method.
	 * No pruning is done, although the CFG may already be pruned.
	 * @param method the method
	 * @return the raw CFG
	 */
	private CFG getRawCFG(Method method) throws CFGBuilderException {
		CFG cfg = cfgMap.get(method);
		if (cfg == null) {
			MethodGen methodGen = getMethodGen(method);
			if (DEBUG) System.out.println("Building CFG for " + methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature());
			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			cfg = cfgBuilder.getCFG();
			cfgMap.put(method, cfg);
		}
		return cfg;
	}

	/**
	 * Set to keep track of which CFGs are being pruned.
	 * Because pruning is potentially interprocedural, we have
	 * to guard against recursive (and thus infinite) invocations.
	 * This has to be static because in the interest of conserving
	 * memory, we do <em>not</em> depend on having unique
	 * a ClassContext object for a given class.  The reason is that
	 * only a fixed number of ClassContext objects are cached
	 * at any given time, and the least recently used one will be
	 * discarded when the cache becomes full.  Therefore,
	 * recursive CFG construction requests for the same class/method
	 * may be made to different ClassContext objects.
	 * <p> At some point, we will probably want to put more thought into
	 * how interprocedural analyses are integrated into FindBugs.
	 */
	private static Set<String> busyCFGSet = new HashSet<String>();

	/**
	 * Get a CFG for given method.
	 * If pruning options are in effect, pruning will be done.
	 * Because the CFG pruning can involve interprocedural analysis,
	 * it is done on a best-effort basis, so the CFG returned might
	 * not actually be pruned.
	 *
	 * @param method the method
	 * @return the CFG
	 * @throws CFGBuilderException if a CFG cannot be constructed for the method
	 */
	public CFG getCFG(Method method) throws CFGBuilderException {
		MethodGen methodGen = getMethodGen(method);

		CFG cfg = getRawCFG(method);

		// HACK:
		// Due to recursive method invocations, we may get a recursive
		// request for the pruned CFG of a method.  In this case,
		// we just return the raw CFG.
		String methodId = methodGen.getClassName()+"."+methodGen.getName()+":"+methodGen.getSignature();
		if (DEBUG) System.out.println("ClassContext: request to prune " + methodId);
		if (!busyCFGSet.add(methodId))
			return cfg;

		if (PRUNE_INFEASIBLE_EXCEPTION_EDGES && !cfg.isFlagSet(PRUNED_INFEASIBLE_EXCEPTIONS)) {
			try {
				TypeDataflow typeDataflow = getTypeDataflow(method);
				new PruneInfeasibleExceptionEdges(cfg, typeDataflow, getConstantPoolGen()).execute();
			} catch (DataflowAnalysisException e) {
				// FIXME: should report the error
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
			}
		}
		cfg.setFlags(cfg.getFlags() | PRUNED_INFEASIBLE_EXCEPTIONS);

		if (PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES && !cfg.isFlagSet(PRUNED_UNCONDITIONAL_THROWERS)) {
			try {
				new PruneUnconditionalExceptionThrowerEdges(
					methodGen, cfg, getConstantPoolGen(), lookupFailureCallback).execute();
			} catch (DataflowAnalysisException e) {
				// FIXME: should report the error
			}
		}
		cfg.setFlags(cfg.getFlags() | PRUNED_UNCONDITIONAL_THROWERS);

		busyCFGSet.remove(methodId);

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
			CFG cfg = getRawCFG(method);
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
			CFG cfg = getRawCFG(method);
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
					public void handleInstruction(int opcode, int index) {
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

	/**
	 * Get ReturnPathDataflow for method.
	 * @param method the method
	 * @return the ReturnPathDataflow
	 */
	public ReturnPathDataflow getReturnPathDataflow(Method method)
		throws CFGBuilderException, DataflowAnalysisException {

		ReturnPathDataflow dataflow = returnPathDataflowMap.get(method);
		if (dataflow == null) {
			CFG cfg = getCFG(method);
			DepthFirstSearch dfs = getDepthFirstSearch(method);
			ReturnPathAnalysis analysis = new ReturnPathAnalysis(dfs);
			dataflow = new ReturnPathDataflow(cfg, analysis);
			dataflow.execute();
			returnPathDataflowMap.put(method, dataflow);
		}
		return dataflow;
	}
}

// vim:ts=4
