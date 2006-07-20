/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.ba.ca.CallListAnalysis;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantAnalysis;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefAnalysis;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.heap.LoadAnalysis;
import edu.umd.cs.findbugs.ba.heap.LoadDataflow;
import edu.umd.cs.findbugs.ba.heap.StoreAnalysis;
import edu.umd.cs.findbugs.ba.heap.StoreDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefAnalysis;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefDataflow;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.TypeAnalysis;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.LoadedFieldSet;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysis;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * A ClassContext caches all of the auxiliary objects used to analyze
 * the methods of a class.  That way, these objects don't need to
 * be created over and over again.
 *
 * @author David Hovemeyer
 */
public class ClassContext {
	public static final boolean DEBUG = Boolean.getBoolean("classContext.debug");

	private static final int PRUNED_INFEASIBLE_EXCEPTIONS = 1;
	private static final int PRUNED_UNCONDITIONAL_THROWERS = 2;

	private static final boolean TIME_ANALYSES = Boolean.getBoolean("classContext.timeAnalyses");

	private static final boolean DEBUG_CFG = Boolean.getBoolean("classContext.debugCFG");

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */
	
	/**
	 * Unpacked code for a method.
	 * Contains set of all opcodes in the method, as well as a map
	 * of bytecode offsets to opcodes.
	 */
	private static class UnpackedCode {
		private BitSet bytecodeSet;
		private short[] offsetToBytecodeMap;
		public UnpackedCode(BitSet bytecodeSet, short[] offsetToBytecodeMap) {
			this.bytecodeSet = bytecodeSet;
			this.offsetToBytecodeMap = offsetToBytecodeMap;
		}
		
		/**
		 * @return Returns the bytecodeSet.
		 */
		public BitSet getBytecodeSet() {
			return bytecodeSet;
		}
		
		/**
		 * @return Returns the offsetToBytecodeMap.
		 */
		public short[] getOffsetToBytecodeMap() {
			return offsetToBytecodeMap;
		}
	}

	private static int depth;

	private static void indent() {
		for (int i = 0; i < depth; ++i) System.out.print("  ");
	}
	
	/**
	 * An AnalysisResult stores the result of requesting an analysis
	 * from an AnalysisFactory.  It can represent a successful outcome
	 * (where the Analysis object can be returned), or an unsuccessful
	 * outcome (where an exception was thrown trying to create the
	 * analysis).  For unsuccessful outcomes, we rethrow the original
	 * exception rather than making another attempt to create the analysis
	 * (since if it fails once, it will never succeed). 
	 */
	private static class AnalysisResult<Analysis> {
		private boolean analysisSetExplicitly;
		private Analysis analysis;
		private AnalysisException analysisException;
		private CFGBuilderException cfgBuilderException;
		private DataflowAnalysisException dataflowAnalysisException;
		
		public Analysis getAnalysis() throws CFGBuilderException, DataflowAnalysisException {
			if (analysisSetExplicitly)
				return analysis;
			if (dataflowAnalysisException != null)
				throw dataflowAnalysisException;
			if (analysisException != null)
				throw analysisException;
			if (cfgBuilderException != null)
				throw cfgBuilderException;
			throw new IllegalStateException();
		}
		
		/**
		 * Record a successful outcome, where the analysis was created.
		 * 
		 * @param analysis the Analysis
		 */
		public void setAnalysis(@Nullable Analysis analysis) {
			this.analysisSetExplicitly = true;
			this.analysis = analysis;
		}
		
		/**
		 * Record that an AnalysisException occurred while attempting
		 * to create the Analysis.
		 * 
		 * @param analysisException the AnalysisException
		 */
		public void setAnalysisException(AnalysisException analysisException) {
			this.analysisException = analysisException;
		}
		
		/**
		 * Record that a CFGBuilderException occurred while attempting
		 * to create the Analysis.
		 * 
		 * @param cfgBuilderException the CFGBuilderException
		 */
		public void setCFGBuilderException(CFGBuilderException cfgBuilderException) {
			this.cfgBuilderException = cfgBuilderException;
		}
		
		/**
		 * Record that a DataflowAnalysisException occurred while attempting
		 * to create the Analysis.
		 *  
		 * @param dataflowException the DataflowAnalysisException
		 */
		public void setDataflowAnalysisException(DataflowAnalysisException dataflowException) {
			this.dataflowAnalysisException = dataflowException;
		}
	}

	/**
	 * Abstract factory class for creating analysis objects.
	 * Handles caching of analysis results for a method.
	 */
	private abstract class AnalysisFactory <Analysis> {
		private String analysisName;
		private HashMap<Method, AnalysisResult<Analysis>> map =
			new HashMap<Method, ClassContext.AnalysisResult<Analysis>>();

		/**
		 * Constructor.
		 * 
		 * @param analysisName name of the analysis factory: for diagnostics/debugging
		 */
		public AnalysisFactory(String analysisName) {
			this.analysisName = analysisName;
		}

		/**
		 * Get the Analysis for given method.
		 * If Analysis has already been performed, the cached result is
		 * returned.
		 * 
		 * @param method the method to analyze
		 * @return the Analysis object representing the result of analyzing the method
		 * @throws CFGBuilderException       if the CFG can't be constructed for the method
		 * @throws DataflowAnalysisException if dataflow analysis fails on the method
		 */
		@CheckForNull public Analysis getAnalysis(Method method) throws CFGBuilderException, DataflowAnalysisException {
			AnalysisResult<Analysis> result = map.get(method);
			if (result == null) {
				if (TIME_ANALYSES) {
					++depth;
					indent();
					System.out.println("CC: Starting " + analysisName + " for " +
					        SignatureConverter.convertMethodSignature(jclass, method) + ":");
				}

				long begin = System.currentTimeMillis();
				
				// Create a new AnalysisResult
				result = new AnalysisResult<Analysis>();

				// Attempt to create the Analysis and store it in the AnalysisResult.
				// If an exception occurs, record it in the AnalysisResult.
				Analysis analysis;
				try {
					analysis = analyze(method);
					result.setAnalysis(analysis);
				} catch (CFGBuilderException e) {
					result.setCFGBuilderException(e);
				} catch (DataflowAnalysisException e) {
					result.setDataflowAnalysisException(e);
				} catch (AnalysisException e) {
					result.setAnalysisException(e);
				}

				if (TIME_ANALYSES) {
					long end = System.currentTimeMillis();
					indent();
					System.out.println("CC: finished " + analysisName + " in " + (end - begin) + " millis");
					--depth;
				}

				// Cache the outcome of this analysis attempt.
				map.put(method, result);
			}
			
			return result.getAnalysis();
		}

		@CheckForNull protected abstract Analysis analyze(Method method)
		        throws CFGBuilderException, DataflowAnalysisException;
	}

	private abstract class NoExceptionAnalysisFactory <Analysis> extends AnalysisFactory<Analysis> {
		public NoExceptionAnalysisFactory(String analysisName) {
			super(analysisName);
		}

		@Override
         public Analysis getAnalysis(Method method) {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			} catch (CFGBuilderException e) {
				throw new IllegalStateException("Should not happen");
			}
		}
	}

	private abstract class NoDataflowAnalysisFactory <Analysis> extends AnalysisFactory<Analysis> {
		public NoDataflowAnalysisFactory(String analysisName) {
			super(analysisName);
		}

		@Override
                 public Analysis getAnalysis(Method method) throws CFGBuilderException {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			}
		}
	}

	private static final Set<String> busyCFGSet = new HashSet<String>();

	private class CFGFactory extends AnalysisFactory<CFG> {

		public CFGFactory() {
			super("CFG construction");
		}

		@Override
                 public CFG getAnalysis(Method method) throws CFGBuilderException {
			try {
				return super.getAnalysis(method);
			} catch (DataflowAnalysisException e) {
				throw new IllegalStateException("Should not happen");
			}
		}

		public CFG getRawCFG(Method method) throws CFGBuilderException {
			return getAnalysis(method);
		}

		public CFG getRefinedCFG(Method method) throws CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			if (methodGen == null) {
				JavaClassAndMethod javaClassAndMethod = new JavaClassAndMethod(jclass, method);
				getLookupFailureCallback().reportSkippedAnalysis(javaClassAndMethod);
				throw new MethodUnprofitableException(javaClassAndMethod);
			}
			CFG cfg = getRawCFG(method);
			
			// Record method name and signature for informational purposes
			cfg.setMethodName(SignatureConverter.convertMethodSignature(methodGen));

			// HACK:
			// Due to recursive method invocations, we may get a recursive
			// request for the pruned CFG of a method.  In this case,
			// we just return the raw CFG.
			String methodId = methodGen.getClassName() + "." + methodGen.getName() + ":" + methodGen.getSignature();
			if (DEBUG_CFG) {
				indent();
				System.out.println("CC: getting refined CFG for " + methodId);
			}
			if (DEBUG) System.out.println("ClassContext: request to prune " + methodId);
			if (!busyCFGSet.add(methodId))
				return cfg;

			final boolean PRUNE_INFEASIBLE_EXCEPTION_EDGES =
				analysisContext.getBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS);
			
			if (PRUNE_INFEASIBLE_EXCEPTION_EDGES && !cfg.isFlagSet(PRUNED_INFEASIBLE_EXCEPTIONS)) {
				try {
					TypeDataflow typeDataflow = getTypeDataflow(method);
					// Exception edge pruning based on ExceptionSets.
					// Note: this is quite slow.
					new PruneInfeasibleExceptionEdges(cfg, methodGen, typeDataflow).execute();
				} catch (DataflowAnalysisException e) {
					// FIXME: should report the error
				} catch (ClassNotFoundException e) {
					getLookupFailureCallback().reportMissingClass(e);
				}
			}
			cfg.setFlags(cfg.getFlags() | PRUNED_INFEASIBLE_EXCEPTIONS);
			
			final boolean PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES =
				!analysisContext.getBoolProperty(AnalysisFeatures.CONSERVE_SPACE);

			if (PRUNE_UNCONDITIONAL_EXCEPTION_THROWER_EDGES && !cfg.isFlagSet(PRUNED_UNCONDITIONAL_THROWERS)) {
				try {
					new PruneUnconditionalExceptionThrowerEdges(methodGen, cfg, getConstantPoolGen(), analysisContext).execute();
				} catch (DataflowAnalysisException e) {
					// FIXME: should report the error
				}
			}
			cfg.setFlags(cfg.getFlags() | PRUNED_UNCONDITIONAL_THROWERS);

			busyCFGSet.remove(methodId);

			return cfg;
		}

		@Override
                 protected CFG analyze(Method method) throws CFGBuilderException {
			MethodGen methodGen = getMethodGen(method);
			if (methodGen == null) {
				JavaClassAndMethod javaClassAndMethod = new JavaClassAndMethod(jclass, method);
				getLookupFailureCallback().reportSkippedAnalysis(javaClassAndMethod);
				throw new MethodUnprofitableException(javaClassAndMethod);
			}
			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			return cfgBuilder.getCFG();
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private JavaClass jclass;
	private AnalysisContext analysisContext;
	private NoExceptionAnalysisFactory<MethodGen> methodGenFactory =
		new NoExceptionAnalysisFactory<MethodGen>("MethodGen construction") {
		@CheckForNull@Override
		protected MethodGen analyze(Method method) {
			if (method.getCode() == null)
				return null;
			String methodName = method.getName();
			if (analysisContext.getBoolProperty(AnalysisFeatures.SKIP_HUGE_METHODS)) {
				int codeLength = method.getCode().getLength();
				if (codeLength > 3000 
						|| (methodName.equals("<clinit>") || methodName.equals("getContents")) && codeLength > 1000) {
					getLookupFailureCallback().reportSkippedAnalysis(new JavaClassAndMethod(jclass, method));
					return null;
				}
			}
			return  new MethodGen(method, jclass.getClassName(), getConstantPoolGen());

		}
	};

	private CFGFactory cfgFactory = new CFGFactory();

	private AnalysisFactory<ValueNumberDataflow> vnaDataflowFactory =
	        new AnalysisFactory<ValueNumberDataflow>("value number analysis") {
		        @Override
                         protected ValueNumberDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        MethodGen methodGen = getMethodGen(method);
			        if (methodGen == null) throw new MethodUnprofitableException(getJavaClass(),method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        LoadedFieldSet loadedFieldSet = getLoadedFieldSet(method);
			        ValueNumberAnalysis analysis = new ValueNumberAnalysis(methodGen, dfs, loadedFieldSet,
							getLookupFailureCallback());
					analysis.setMergeTree(new MergeTree(analysis.getFactory()));
			        CFG cfg = getCFG(method);
			        ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, analysis);
			        vnaDataflow.execute();
			        return vnaDataflow;
		        }
	        };

	private AnalysisFactory<IsNullValueDataflow> invDataflowFactory =
	        new AnalysisFactory<IsNullValueDataflow>("null value analysis") {
		        @Override
                         protected IsNullValueDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        MethodGen methodGen = getMethodGen(method);
			        if (methodGen == null) throw new MethodUnprofitableException(getJavaClass(),method);
			        CFG cfg = getCFG(method);
			        ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        AssertionMethods assertionMethods = getAssertionMethods();

			        IsNullValueAnalysis invAnalysis = new IsNullValueAnalysis(methodGen, cfg, vnaDataflow, dfs, assertionMethods);

					// Set return value and parameter databases

					invAnalysis.setClassAndMethod(new JavaClassAndMethod(getJavaClass(), method));
					
			        IsNullValueDataflow invDataflow = new IsNullValueDataflow(cfg, invAnalysis);
			        invDataflow.execute();
			        return invDataflow;
		        }
	        };

	private AnalysisFactory<TypeDataflow> typeDataflowFactory =
	        new AnalysisFactory<TypeDataflow>("type analysis") {
		        @Override
                         protected TypeDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        MethodGen methodGen = getMethodGen(method);
			        if (methodGen == null) throw new MethodUnprofitableException(getJavaClass(),method);
			        CFG cfg = getRawCFG(method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        ExceptionSetFactory exceptionSetFactory = getExceptionSetFactory(method);

			        TypeAnalysis typeAnalysis =
			                new TypeAnalysis(methodGen, cfg, dfs, getLookupFailureCallback(), exceptionSetFactory);
			        
					if (analysisContext.getBoolProperty(AnalysisFeatures.MODEL_INSTANCEOF)) {
						typeAnalysis.setValueNumberDataflow(getValueNumberDataflow(method));
					}
					
					// Field store type database.
					// If present, this can give us more accurate type information
					// for values loaded from fields.
					typeAnalysis.setFieldStoreTypeDatabase(analysisContext.getFieldStoreTypeDatabase());
					
					TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
			        typeDataflow.execute();

			        return typeDataflow;
		        }
	        };

	private NoDataflowAnalysisFactory<DepthFirstSearch> dfsFactory =
	        new NoDataflowAnalysisFactory<DepthFirstSearch>("depth first search") {
		        @Override
                         protected DepthFirstSearch analyze(Method method) throws CFGBuilderException {
			        CFG cfg = getRawCFG(method);
			        DepthFirstSearch dfs = new DepthFirstSearch(cfg);
			        dfs.search();
			        return dfs;
		        }
	        };

	private NoDataflowAnalysisFactory<ReverseDepthFirstSearch> rdfsFactory =
	        new NoDataflowAnalysisFactory<ReverseDepthFirstSearch>("reverse depth first search") {
		        @Override
                         protected ReverseDepthFirstSearch analyze(Method method) throws CFGBuilderException {
			        CFG cfg = getRawCFG(method);
			        ReverseDepthFirstSearch rdfs = new ReverseDepthFirstSearch(cfg);
			        rdfs.search();
			        return rdfs;
		        }
	        };
	private static class UnpackedBytecodeCallback implements BytecodeScanner.Callback {
		private BitSet bytecodeSet;
		private short[] offsetToOpcodeMap;
		
		public UnpackedBytecodeCallback(int codeSize) {
			this.bytecodeSet = new BitSet();
			this.offsetToOpcodeMap = new short[codeSize];
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.ba.BytecodeScanner.Callback#handleInstruction(int, int)
		 */
		public void handleInstruction(int opcode, int index) {
			bytecodeSet.set(opcode);
			offsetToOpcodeMap[index] = (short) opcode;
		}
		
		public UnpackedCode getUnpackedCode() {
			return new UnpackedCode(bytecodeSet, offsetToOpcodeMap);
		}
	}

	private NoExceptionAnalysisFactory<UnpackedCode> unpackedCodeFactory =
	        new NoExceptionAnalysisFactory<UnpackedCode>("unpacked bytecode") {
		        @Override
                         protected UnpackedCode analyze(Method method) {

			        Code code = method.getCode();
			        if (code == null)
				        return null;

			        byte[] instructionList = code.getCode();

			        // Create callback
			        UnpackedBytecodeCallback callback = new UnpackedBytecodeCallback(instructionList.length);

			        // Scan the method.
			        BytecodeScanner scanner = new BytecodeScanner();
			        scanner.scan(instructionList, callback);

			        return callback.getUnpackedCode();
		        }
	        };

	private AnalysisFactory<LockDataflow> lockDataflowFactory =
	        new AnalysisFactory<LockDataflow>("lock set analysis") {
		        @Override
                         protected LockDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        MethodGen methodGen = getMethodGen(method);
			        if (methodGen == null) throw new MethodUnprofitableException(getJavaClass(),method);
			        ValueNumberDataflow vnaDataflow = getValueNumberDataflow(method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        CFG cfg = getCFG(method);

			        LockAnalysis analysis = new LockAnalysis(methodGen, vnaDataflow, dfs);
			        LockDataflow dataflow = new LockDataflow(cfg, analysis);
			        dataflow.execute();
			        return dataflow;
		        }
	        };

	private AnalysisFactory<LockChecker> lockCheckerFactory =
			new AnalysisFactory<LockChecker>("lock checker meta-analysis") {
				/* (non-Javadoc)
				 * @see edu.umd.cs.findbugs.ba.ClassContext.AnalysisFactory#analyze(org.apache.bcel.classfile.Method)
				 */
				@Override
                                 protected LockChecker analyze(Method method) throws CFGBuilderException,
						DataflowAnalysisException {
					LockChecker lockChecker = new LockChecker(ClassContext.this, method);
					lockChecker.execute();
					return lockChecker;
				}
			};
	        
	private AnalysisFactory<ReturnPathDataflow> returnPathDataflowFactory =
	        new AnalysisFactory<ReturnPathDataflow>("return path analysis") {
		        @Override
                         protected ReturnPathDataflow analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        CFG cfg = getCFG(method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        ReturnPathAnalysis analysis = new ReturnPathAnalysis(dfs);
			        ReturnPathDataflow dataflow = new ReturnPathDataflow(cfg, analysis);
			        dataflow.execute();
			        return dataflow;
		        }
	        };

	private AnalysisFactory<DominatorsAnalysis> nonExceptionDominatorsAnalysisFactory =
	        new AnalysisFactory<DominatorsAnalysis>("non-exception dominators analysis") {
		        @Override
                         protected DominatorsAnalysis analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        CFG cfg = getCFG(method);
			        DepthFirstSearch dfs = getDepthFirstSearch(method);
			        DominatorsAnalysis analysis = new DominatorsAnalysis(cfg, dfs, true);
			        Dataflow<java.util.BitSet, DominatorsAnalysis> dataflow =
			                new Dataflow<java.util.BitSet, DominatorsAnalysis>(cfg, analysis);
			        dataflow.execute();
			        return analysis;
		        }
	        };

	private AnalysisFactory<PostDominatorsAnalysis> nonExceptionPostDominatorsAnalysisFactory =
	        new AnalysisFactory<PostDominatorsAnalysis>("non-exception postdominators analysis") {
		        @Override
                         protected PostDominatorsAnalysis analyze(Method method) throws DataflowAnalysisException, CFGBuilderException {
			        CFG cfg = getCFG(method);
			        ReverseDepthFirstSearch rdfs = getReverseDepthFirstSearch(method);
			        PostDominatorsAnalysis analysis = new PostDominatorsAnalysis(cfg, rdfs, true);
			        Dataflow<java.util.BitSet, PostDominatorsAnalysis> dataflow =
			                new Dataflow<java.util.BitSet, PostDominatorsAnalysis>(cfg, analysis);
			        dataflow.execute();
			        return analysis;
		        }
	        };

	private AnalysisFactory<PostDominatorsAnalysis> nonImplicitExceptionPostDominatorsAnalysisFactory =
		new AnalysisFactory<PostDominatorsAnalysis>("non-implicit-exception postdominators analysis") {
			@Override
                         protected PostDominatorsAnalysis analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				CFG cfg = getCFG(method);
				PostDominatorsAnalysis analysis = new PostDominatorsAnalysis(
						cfg,
						getReverseDepthFirstSearch(method),
						new EdgeChooser() {
					public boolean choose(Edge edge) {
						return !edge.isExceptionEdge()
							||  edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG);
						}
					}
				);
				Dataflow<BitSet, PostDominatorsAnalysis> dataflow =
					new Dataflow<BitSet, PostDominatorsAnalysis>(cfg, analysis);
				dataflow.execute();
				
				return analysis;
			}
		};
			
	private NoExceptionAnalysisFactory<ExceptionSetFactory> exceptionSetFactoryFactory =
	        new NoExceptionAnalysisFactory<ExceptionSetFactory>("exception set factory") {
		        @Override
                         protected ExceptionSetFactory analyze(Method method) {
			        return new ExceptionSetFactory();
		        }
	        };

	private NoExceptionAnalysisFactory<String[]> parameterSignatureListFactory =
	        new NoExceptionAnalysisFactory<String[]>("parameter signature list factory") {
		        @Override
                         protected String[] analyze(Method method) {
			        SignatureParser parser = new SignatureParser(method.getSignature());
			        ArrayList<String> resultList = new ArrayList<String>();
			        for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();) {
				        resultList.add(i.next());
			        }
			        return resultList.toArray(new String[resultList.size()]);
		        }
	        };

	private AnalysisFactory<ConstantDataflow> constantDataflowFactory =
		new AnalysisFactory<ConstantDataflow>("constant propagation analysis") {
			@Override @CheckForNull
			protected ConstantDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				MethodGen methodGen = getMethodGen(method);
				if (methodGen == null) return null;
				ConstantAnalysis analysis = new ConstantAnalysis(
					methodGen,
					getDepthFirstSearch(method)
				);
				ConstantDataflow dataflow = new ConstantDataflow(getCFG(method), analysis);
				dataflow.execute();
				
				return dataflow;
			}
		};

	private AnalysisFactory<UnconditionalDerefDataflow> unconditionalDerefDataflowFactory =
		new AnalysisFactory<UnconditionalDerefDataflow>("unconditional deref analysis") {
			@Override @CheckForNull
			protected UnconditionalDerefDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				MethodGen methodGen = getMethodGen(method);
				if (methodGen == null)
					return null;
				CFG cfg = getCFG(method); 
				
	
				UnconditionalDerefAnalysis analysis = new UnconditionalDerefAnalysis(
						getReverseDepthFirstSearch(method),
						cfg,
						methodGen,
						getValueNumberDataflow(method),
						getTypeDataflow(method));
				UnconditionalDerefDataflow dataflow = new UnconditionalDerefDataflow(cfg, analysis);
				
				dataflow.execute();
				
				return dataflow;
			}
		};
	
	private AnalysisFactory<LoadDataflow> loadDataflowFactory =
		new AnalysisFactory<LoadDataflow>("field load analysis") {
			@Override @CheckForNull
			protected LoadDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				MethodGen methodGen = getMethodGen(method);
				if (methodGen == null)
					return null;
				LoadAnalysis analysis = new LoadAnalysis(
						getDepthFirstSearch(method),
						getConstantPoolGen()
						);
				LoadDataflow dataflow = new LoadDataflow(getCFG(method), analysis);
				dataflow.execute();
				return dataflow;
			}
		};

	private AnalysisFactory<StoreDataflow> storeDataflowFactory =
		new AnalysisFactory<StoreDataflow>("field store analysis") {
			@Override @CheckForNull
			protected StoreDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				MethodGen methodGen = getMethodGen(method);
				if (methodGen == null)
					return null;
				StoreAnalysis analysis = new StoreAnalysis(
						getDepthFirstSearch(method),
						getConstantPoolGen()
						);
				StoreDataflow dataflow = new StoreDataflow(getCFG(method), analysis);
				dataflow.execute();
				return dataflow;
			}
		};
		
	private static final BitSet fieldInstructionOpcodeSet = new BitSet();
	static {
		fieldInstructionOpcodeSet.set(Constants.GETFIELD);
		fieldInstructionOpcodeSet.set(Constants.PUTFIELD);
		fieldInstructionOpcodeSet.set(Constants.GETSTATIC);
		fieldInstructionOpcodeSet.set(Constants.PUTSTATIC);
	}

	/**
	 * Factory to determine which fields are loaded and stored
	 * by the instructions in a method, and the overall method.
	 * The main purpose is to support efficient redundant load elimination
	 * and forward substitution in ValueNumberAnalysis (there is no need to
	 * remember stores of fields that are never read,
	 * or loads of fields that are only loaded in one location).
	 * However, it might be useful for other kinds of analysis.
	 *
	 * <p> The tricky part is that in addition to fields loaded and stored
	 * with get/putfield and get/putstatic, we also try to figure
	 * out field accessed through calls to inner-class access methods.
	 */
	private NoExceptionAnalysisFactory<LoadedFieldSet> loadedFieldSetFactory =
			new NoExceptionAnalysisFactory<LoadedFieldSet>("loaded field set factory") {
				@Override
                                 protected LoadedFieldSet analyze(Method method) {
					MethodGen methodGen = getMethodGen(method);
					if (methodGen == null) return null;
					InstructionList il = methodGen.getInstructionList();

					LoadedFieldSet loadedFieldSet = new LoadedFieldSet(methodGen);

					for (InstructionHandle handle = il.getStart(); handle != null; handle = handle.getNext()) {
						Instruction ins = handle.getInstruction();
						short opcode = ins.getOpcode();
						try {
							if (opcode == Constants.INVOKESTATIC) {
								INVOKESTATIC inv = (INVOKESTATIC) ins;
								if (Hierarchy.isInnerClassAccess(inv, getConstantPoolGen())) {
									InnerClassAccess access = Hierarchy.getInnerClassAccess(inv, getConstantPoolGen());
/*
									if (access == null) {
										System.out.println("Missing inner class access in " +
											SignatureConverter.convertMethodSignature(methodGen) + " at " +
											inv);
									}
*/
									if (access != null) {
										if (access.isLoad())
											loadedFieldSet.addLoad(handle, access.getField());
										else
											loadedFieldSet.addStore(handle, access.getField());
									}
								}
							} else if (fieldInstructionOpcodeSet.get(opcode)) {
								boolean isLoad = (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC);
								XField field = Hierarchy.findXField((FieldInstruction) ins, getConstantPoolGen());
								if (field != null) {
									if (isLoad)
										loadedFieldSet.addLoad(handle, field);
									else
										loadedFieldSet.addStore(handle, field);
								}
							}
						} catch (ClassNotFoundException e) {
							analysisContext.getLookupFailureCallback().reportMissingClass(e);
						}
					}

					return loadedFieldSet;
				}
			};

	private AnalysisFactory<LiveLocalStoreDataflow> liveLocalStoreDataflowFactory =
			new AnalysisFactory<LiveLocalStoreDataflow>("live local stores analysis") {
				@Override
                                 protected LiveLocalStoreDataflow analyze(Method method)
					throws DataflowAnalysisException, CFGBuilderException {
						MethodGen methodGen = getMethodGen(method);
						if (methodGen == null) return null;
						CFG cfg = getCFG(method);

						ReverseDepthFirstSearch rdfs = getReverseDepthFirstSearch(method);

						LiveLocalStoreAnalysis analysis = new LiveLocalStoreAnalysis(methodGen, rdfs);
						LiveLocalStoreDataflow dataflow = new LiveLocalStoreDataflow(cfg, analysis);

						dataflow.execute();

						return dataflow;
				}
			};

	private AnalysisFactory<Dataflow<BlockType, BlockTypeAnalysis>> blockTypeDataflowFactory =
			new AnalysisFactory<Dataflow<BlockType, BlockTypeAnalysis>>("block type analysis") {
				@Override
                                 protected Dataflow<BlockType, BlockTypeAnalysis> analyze(Method method)
						throws DataflowAnalysisException, CFGBuilderException {
					CFG cfg = getCFG(method);
					DepthFirstSearch dfs = getDepthFirstSearch(method);

					BlockTypeAnalysis analysis = new BlockTypeAnalysis(dfs);
					Dataflow<BlockType, BlockTypeAnalysis> dataflow =
						new Dataflow<BlockType, BlockTypeAnalysis>(cfg, analysis);
					dataflow.execute();

					return dataflow;
				}
			};

	private AnalysisFactory<CallListDataflow> callListDataflowFactory =
		new AnalysisFactory<CallListDataflow>("call list analysis") {
			//@Override
			@Override
                         protected CallListDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {

				CallListAnalysis analysis = new CallListAnalysis(
						getCFG(method),
						getDepthFirstSearch(method),
						getConstantPoolGen());
				
				CallListDataflow dataflow = new CallListDataflow(getCFG(method), analysis);
				dataflow.execute();
				
				return dataflow;
			}
		};
		
	private AnalysisFactory<UnconditionalValueDerefDataflow> unconditionalValueDerefDataflowFactory =
		new AnalysisFactory<UnconditionalValueDerefDataflow>("unconditional value dereference analysis") {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.ClassContext.AnalysisFactory#analyze(org.apache.bcel.classfile.Method)
			 */
			@Override
			protected UnconditionalValueDerefDataflow analyze(Method method) throws CFGBuilderException, DataflowAnalysisException {
				UnconditionalValueDerefAnalysis analysis = new UnconditionalValueDerefAnalysis(
						getReverseDepthFirstSearch(method),
						getCFG(method),
						getMethodGen(method),
						getValueNumberDataflow(method)
						);
				
				UnconditionalValueDerefDataflow dataflow =
					new UnconditionalValueDerefDataflow(getCFG(method), analysis);
				dataflow.execute();
				
				return dataflow;
			}
		};
			
	private ClassGen classGen;
	private AssignedFieldMap assignedFieldMap;
	private AssertionMethods assertionMethods;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 *
	 * @param jclass the JavaClass
	 */
	public ClassContext(JavaClass jclass, AnalysisContext analysisContext) {
		this.jclass = jclass;
		this.analysisContext = analysisContext;
		this.classGen = null;
		this.assignedFieldMap = null;
		this.assertionMethods = null;
	}

	/**
	 * Get the JavaClass.
	 */
	public JavaClass getJavaClass() {
		return jclass;
	}
	
	/**
	 * Look up the Method represented by given MethodGen.
	 * 
	 * @param methodGen a MethodGen
	 * @return the Method represented by the MethodGen
	 */
	public Method getMethod(MethodGen methodGen) {
		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (method.getName().equals(methodGen.getName())
					&& method.getSignature().equals(methodGen.getSignature())
					&& method.getAccessFlags() == methodGen.getAccessFlags()) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Get the AnalysisContext.
	 */
	public AnalysisContext getAnalysisContext() {
		return analysisContext;
	}

	/**
	 * Get the RepositoryLookupFailureCallback.
	 *
	 * @return the RepositoryLookupFailureCallback
	 */
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return analysisContext.getLookupFailureCallback();
	}

	/**
	 * Get a MethodGen object for given method.
	 *
	 * @param method the method
	 * @return the MethodGen object for the method, or null
	 *         if the method has no Code attribute (and thus cannot be analyzed)
	 *         or if the method seems unprofitable to analyze
	 */
	@CheckForNull public MethodGen getMethodGen(Method method) {
		return methodGenFactory.getAnalysis(method);
	}

	/**
	 * Get a "raw" CFG for given method.
	 * No pruning is done, although the CFG may already be pruned.
	 *
	 * @param method the method
	 * @return the raw CFG
	 */
	public CFG getRawCFG(Method method) throws CFGBuilderException {
		return cfgFactory.getRawCFG(method);
	}

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
		return cfgFactory.getRefinedCFG(method);
	}

	/**
	 * Get the ConstantPoolGen used to create the MethodGens
	 * for this class.
	 *
	 * @return the ConstantPoolGen
	 */
	public @NonNull ConstantPoolGen getConstantPoolGen() {
		if (classGen == null)
			classGen = new ClassGen(jclass);
		return classGen.getConstantPool();
	}

	/**
	 * Get a ValueNumberDataflow for given method.
	 *
	 * @param method the method
	 * @return the ValueNumberDataflow
	 */
	public ValueNumberDataflow getValueNumberDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return vnaDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get an IsNullValueDataflow for given method.
	 *
	 * @param method the method
	 * @return the IsNullValueDataflow
	 */
	public IsNullValueDataflow getIsNullValueDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return invDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get a TypeDataflow for given method.
	 *
	 * @param method the method
	 * @return the TypeDataflow
	 */
	public TypeDataflow getTypeDataflow(Method method) throws DataflowAnalysisException, CFGBuilderException {
		return typeDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get a DepthFirstSearch for given method.
	 *
	 * @param method the method
	 * @return the DepthFirstSearch
	 */
	public DepthFirstSearch getDepthFirstSearch(Method method) throws CFGBuilderException {
		return dfsFactory.getAnalysis(method);
	}

	/**
	 * Get a ReverseDepthFirstSearch for given method.
	 *
	 * @param method the method
	 * @return the ReverseDepthFirstSearch
	 */
	public ReverseDepthFirstSearch getReverseDepthFirstSearch(Method method)
	        throws CFGBuilderException {
		return rdfsFactory.getAnalysis(method);
	}

	static MapCache<XMethod,BitSet> cachedBitsets = new MapCache<XMethod, BitSet>(64);
	/**
	 * Get a BitSet representing the bytecodes that are used in the given method.
	 * This is useful for prescreening a method for the existence of particular instructions.
	 * Because this step doesn't require building a MethodGen, it is very
	 * fast and memory-efficient.  It may allow a Detector to avoid some
	 * very expensive analysis, which is a Big Win for the user.
	 *
	 * @param method the method
	 * @return the BitSet containing the opcodes which appear in the method,
	 *          or null if the method has no code
	 */
	@CheckForNull   public BitSet getBytecodeSet(Method method) {
		return getBytecodeSet(jclass, method);
	}
	/**
	 * Get a BitSet representing the bytecodes that are used in the given method.
	 * This is useful for prescreening a method for the existence of particular instructions.
	 * Because this step doesn't require building a MethodGen, it is very
	 * fast and memory-efficient.  It may allow a Detector to avoid some
	 * very expensive analysis, which is a Big Win for the user.
	 *
	 * @param method the method
	 * @return the BitSet containing the opcodes which appear in the method,
	 *          or null if the method has no code
	 */
	@CheckForNull static public BitSet getBytecodeSet(JavaClass clazz, Method method) {

				XMethod xmethod = XFactory.createXMethod(clazz, method);
				if (cachedBitsets.containsKey(xmethod)) {
					return cachedBitsets.get(xmethod);
				}
		        Code code = method.getCode();
		        if (code == null)
			        return null;

		        byte[] instructionList = code.getCode();

		        // Create callback
		        UnpackedBytecodeCallback callback = new UnpackedBytecodeCallback(instructionList.length);

		        // Scan the method.
		        BytecodeScanner scanner = new BytecodeScanner();
		        scanner.scan(instructionList, callback);

		        UnpackedCode unpackedCode = callback.getUnpackedCode();
				BitSet result =  null;
		        if (unpackedCode != null) result =  unpackedCode.getBytecodeSet();
		        cachedBitsets.put(xmethod, result);
		        return result;
	}
	
	/**
	 * Get array mapping bytecode offsets to opcodes for given method.
	 * Array elements containing zero are either not valid instruction offsets,
	 * or contain a NOP instruction.  (It is convenient not to distinguish
	 * these cases.)
	 * 
	 * @param method the method
	 * @return map of bytecode offsets to opcodes, or null if the method has no code
	 */
	public short[] getOffsetToOpcodeMap(Method method) {
		UnpackedCode unpackedCode = unpackedCodeFactory.getAnalysis(method);
		return unpackedCode != null ? unpackedCode.getOffsetToBytecodeMap() : null;
	}

	/**
	 * Get dataflow for LockAnalysis for given method.
	 *
	 * @param method the method
	 * @return the LockDataflow
	 */
	public LockDataflow getLockDataflow(Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return lockDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get LockChecker for method.
	 * This is like LockDataflow, but may be able to avoid performing
	 * the actual dataflow analyses if the method doesn't contain
	 * explicit monitorenter/monitorexit instructions.
	 * 
	 * @param method the method
	 * @return the LockChecker
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public LockChecker getLockChecker(Method method) throws CFGBuilderException, DataflowAnalysisException {
		return lockCheckerFactory.getAnalysis(method);
	}
	
	/**
	 * Get ReturnPathDataflow for method.
	 *
	 * @param method the method
	 * @return the ReturnPathDataflow
	 */
	public ReturnPathDataflow getReturnPathDataflow(Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return returnPathDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get DominatorsAnalysis for given method,
	 * where exception edges are ignored.
	 *
	 * @param method the method
	 * @return the DominatorsAnalysis
	 */
	public DominatorsAnalysis getNonExceptionDominatorsAnalysis(Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return nonExceptionDominatorsAnalysisFactory.getAnalysis(method);
	}

	/**
	 * Get DominatorsAnalysis for given method,
	 * where implicit exception edges are ignored.
	 *
	 * @param method the method
	 * @return the DominatorsAnalysis
	 */
	public PostDominatorsAnalysis getNonImplicitExceptionDominatorsAnalysis(Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return nonImplicitExceptionPostDominatorsAnalysisFactory.getAnalysis(method);
	}

	/**
	 * Get PostDominatorsAnalysis for given method,
	 * where exception edges are ignored.
	 *
	 * @param method the method
	 * @return the PostDominatorsAnalysis
	 */
	public PostDominatorsAnalysis getNonExceptionPostDominatorsAnalysis(Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		return nonExceptionPostDominatorsAnalysisFactory.getAnalysis(method);
	}

	/**
	 * Get ExceptionSetFactory for given method.
	 *
	 * @param method the method
	 * @return the ExceptionSetFactory
	 */
	public ExceptionSetFactory getExceptionSetFactory(Method method) {
		return exceptionSetFactoryFactory.getAnalysis(method);
	}

	/**
	 * Get array of type signatures of parameters for given method.
	 *
	 * @param method the method
	 * @return an array of type signatures indicating the types
	 *         of the method's parameters
	 */
	public String[] getParameterSignatureList(Method method) {
		return parameterSignatureListFactory.getAnalysis(method);
	}

	/**
	 * Get the set of fields loaded by given method.
	 *
	 * @param method the method
	 * @return the set of fields loaded by the method
	 */
	public LoadedFieldSet getLoadedFieldSet(Method method) {
		return loadedFieldSetFactory.getAnalysis(method);
	}

	/**
	 * Get LiveLocalStoreAnalysis dataflow for given method.
	 *
	 * @param method the method
	 * @return the Dataflow object for LiveLocalStoreAnalysis on the method
	 */
	public LiveLocalStoreDataflow getLiveLocalStoreDataflow(Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		return liveLocalStoreDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get BlockType dataflow for given method.
	 *
	 * @param method the method
	 * @return the Dataflow object for BlockTypeAnalysis on the method
	 */
	public Dataflow<BlockType, BlockTypeAnalysis> getBlockTypeDataflow(Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		return blockTypeDataflowFactory.getAnalysis(method);
	}

	/**
	 * Get the assigned field map for the class.
	 *
	 * @return the AssignedFieldMap
	 * @throws ClassNotFoundException if a class lookup prevents
	 *                                the class's superclasses from being searched for
	 *                                assignable fields
	 */
	public AssignedFieldMap getAssignedFieldMap() throws ClassNotFoundException {
		if (assignedFieldMap == null) {
			assignedFieldMap = new AssignedFieldMap(this);
		}
		return assignedFieldMap;
	}

	/**
	 * Get AssertionMethods for class.
	 *
	 * @return the AssertionMethods
	 */
	public AssertionMethods getAssertionMethods() {
		if (assertionMethods == null) {
			assertionMethods = new AssertionMethods(jclass);
		}
		return assertionMethods;
	}
	
	/**
	 * Get ConstantDataflow for method.
	 * 
	 * @param method the method
	 * @return the ConstantDataflow
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public ConstantDataflow getConstantDataflow(Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		return constantDataflowFactory.getAnalysis(method);
	}
	
	/**
	 * Get the UnconditionalDerefDataflow for a method.
	 * 
	 * @param method the method
	 * @return the UnconditionalDerefDataflow for the method
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public UnconditionalDerefDataflow getUnconditionalDerefDataflow(Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		return unconditionalDerefDataflowFactory.getAnalysis(method);
	}
	
	/**
	 * Get load dataflow.
	 * 
	 * @param method the method
	 * @return the LoadDataflow
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public LoadDataflow getLoadDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
		return loadDataflowFactory.getAnalysis(method);
	}
	
	/**
	 * Get store dataflow.
	 * 
	 * @param method the method
	 * @return the StoreDataflow
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public StoreDataflow getStoreDataflow(Method method) throws CFGBuilderException, DataflowAnalysisException {
		return storeDataflowFactory.getAnalysis(method);
	}
	
	/**
	 * Get CallListDataflow for method.
	 * 
	 * @param method the method
	 * @return the CallListDataflow
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public CallListDataflow getCallListDataflow(Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		return callListDataflowFactory.getAnalysis(method);
	}
	
	public static BitSet linesMentionedMultipleTimes(Method method) {
		BitSet lineMentionedMultipleTimes = new BitSet();
		Code code = method.getCode();
		if (code == null || code.getExceptionTable() == null) return lineMentionedMultipleTimes;
		BitSet foundOnce = new BitSet();
		LineNumberTable lineNumberTable = method.getLineNumberTable();
		int lineNum = -1;
		if (lineNumberTable != null) 
			for(LineNumber  line : lineNumberTable.getLineNumberTable()) {
				int newLine = line.getLineNumber();
				if (newLine == lineNum || newLine == -1) continue;
				lineNum = newLine;
				if (foundOnce.get(lineNum))
					lineMentionedMultipleTimes.set(lineNum);
				else 
					foundOnce.set(lineNum);	
			}
		return lineMentionedMultipleTimes;
	}
	
	/**
	 * Get the UnconditionalValueDerefDataflow for a method.
	 * 
	 * @param method the method
	 * @return the UnconditionalValueDerefDataflow 
	 * @throws CFGBuilderException
	 * @throws DataflowAnalysisException
	 */
	public UnconditionalValueDerefDataflow getUnconditionalValueDerefDataflow(Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		return 	unconditionalValueDerefDataflowFactory.getAnalysis(method);
	}
}

// vim:ts=3
