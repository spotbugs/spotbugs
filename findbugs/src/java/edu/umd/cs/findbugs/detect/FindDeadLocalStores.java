/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;
import org.apache.tools.ant.util.ClasspathUtils;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Find dead stores to local variables.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindDeadLocalStores implements Detector {

	private static final boolean DEBUG = SystemProperties.getBoolean("fdls.debug");

	// Define the name of the property that is used to exclude named local
	// variables
	// from Dead Local Storage detection...
	private static final String FINDBUGS_EXCLUDED_LOCALS_PROP_NAME = "findbugs.dls.exclusions";

	// Define a collection of excluded local variables...
	private static final Set<String> EXCLUDED_LOCALS = new HashSet<String>();

	private static final boolean DO_EXCLUDE_LOCALS = SystemProperties.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME) != null;
	static {
		// Get the value of the property...
		String exclLocalsProperty = SystemProperties.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME);

		// If we have one, then split its contents into a table...
		if (exclLocalsProperty != null) {
			EXCLUDED_LOCALS.addAll((List<String>) Arrays.asList(exclLocalsProperty.split(",")));
			EXCLUDED_LOCALS.remove("");
		}
	}

	/**
	 * System property to enable a feature that suppresses warnings if
	 * there is at least one live store on the line where the warning
	 * would be reported.  Eliminates some FPs due to inlining/duplication
	 * of finally blocks.  But, kills some legitimate warnings where
	 * there are truly multiple stores on the same line.
	 */
	private static final boolean SUPPRESS_IF_AT_LEAST_ONE_LIVE_STORE_ON_LINE =
		SystemProperties.getBoolean("findbugs.dls.suppressIfOneLiveStore");

	// private static final Set<String> classesAlreadyReportedOn = new
	// HashSet<String>();
	/**
	 * Opcodes of instructions that load constant values that often indicate
	 * defensive programming.
	 */
	private static final BitSet defensiveConstantValueOpcodes = new BitSet();
	static {
		defensiveConstantValueOpcodes.set(Constants.DCONST_0);
		defensiveConstantValueOpcodes.set(Constants.DCONST_1);
		defensiveConstantValueOpcodes.set(Constants.FCONST_0);
		defensiveConstantValueOpcodes.set(Constants.FCONST_1);
		defensiveConstantValueOpcodes.set(Constants.ACONST_NULL);
		defensiveConstantValueOpcodes.set(Constants.ICONST_0);
		defensiveConstantValueOpcodes.set(Constants.ICONST_1);
		defensiveConstantValueOpcodes.set(Constants.LDC);
	}

	private BugReporter bugReporter;

	public FindDeadLocalStores(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		if (DEBUG)
			System.out.println("Debugging FindDeadLocalStores detector");
	}

	private boolean prescreen(ClassContext classContext, Method method) {
		return true;
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		for (Method method : methodList) {
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			if (!prescreen(classContext, method))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("Error analyzing " + method.toString(), e);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error analyzing " + method.toString(), e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {

		if (DEBUG) {
			System.out.println("    Analyzing method " + classContext.getJavaClass().getClassName() + "." + method.getName());
		}

		JavaClass javaClass = classContext.getJavaClass();

		BugAccumulator accumulator = new BugAccumulator(bugReporter);
		Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow = classContext.getLiveLocalStoreDataflow(method);

		int numLocals = method.getCode().getMaxLocals();
		int[] localStoreCount = new int[numLocals];
		int[] localLoadCount = new int[numLocals];
		int[] localIncrementCount = new int[numLocals];
		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);
		BitSet liveStoreSetAtEntry = llsaDataflow.getAnalysis().getResultFact(cfg.getEntry());
		BitSet complainedAbout = new BitSet();
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

		// Get number of locals that are parameters.
		int localsThatAreParameters = PreorderVisitor.getNumberArguments(method.getSignature());
		if (!method.isStatic())
			localsThatAreParameters++;

		// Scan method to determine number of loads, stores, and increments
		// of local variables.
		countLocalStoresLoadsAndIncrements(localStoreCount, localLoadCount, localIncrementCount, cfg);
		for (int i = 0; i < localsThatAreParameters; i++)
			localStoreCount[i]++;

		// For each source line, keep track of # times
		// the line was a live store.  This can eliminate false positives
		// due to inlining of finally blocks.
		BitSet liveStoreSourceLineSet = new BitSet();

		// Scan method for
		// - dead stores
		// - stores to parameters that are dead upon entry to the method
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			BugInstance pendingBugReportAboutOverwrittenParameter = null;
			try {
				WarningPropertySet<DeadLocalStoreProperty> propertySet = new WarningPropertySet<DeadLocalStoreProperty>();
				// Skip any instruction which is not a store
				if (!isStore(location))
					continue;

				// Heuristic: exception handler blocks often contain
				// dead stores generated by the compiler.
				if (location.getBasicBlock().isExceptionHandler())
					propertySet.addProperty(DeadLocalStoreProperty.EXCEPTION_HANDLER);

				IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();

				int local = ins.getIndex();

				// Get live stores at this instruction.
				// Note that the analysis also computes which stores were
				// killed by a subsequent unconditional store.
				BitSet liveStoreSet = llsaDataflow.getAnalysis().getFactAtLocation(location);

				// Is store alive?
				boolean storeLive = llsaDataflow.getAnalysis().isStoreAlive(liveStoreSet, local);

				LocalVariableAnnotation lvAnnotation = LocalVariableAnnotation.getLocalVariableAnnotation(method, location, ins);

				SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext,
						methodGen, javaClass.getSourceFileName(), location.getHandle());

				if (DEBUG) {
					System.out.println("    Store at " + sourceLineAnnotation.getStartLine() + "@" +
							location.getHandle().getPosition() + " is " +
							(storeLive ? "live" : "dead"));
					System.out.println("Previous is: " + 	location.getHandle().getPrev());
				}
			
				// Note source lines of live stores.
				if (storeLive && sourceLineAnnotation.getStartLine() > 0) {
					liveStoreSourceLineSet.set(sourceLineAnnotation.getStartLine());
				}

				String name = lvAnnotation.getName();
				if (name.charAt(0) == '$' || name.charAt(0) == '_')
					propertySet.addProperty(DeadLocalStoreProperty.SYNTHETIC_NAME);
				if (EXCLUDED_LOCALS.contains(name))
					continue;
				propertySet.setProperty(DeadLocalStoreProperty.LOCAL_NAME, name);

							boolean isParameter = local < localsThatAreParameters;
				if (isParameter)
					propertySet.addProperty(DeadLocalStoreProperty.IS_PARAMETER);

				// Is this a store to a parameter which was dead on entry to the
				// method?
				boolean parameterThatIsDeadAtEntry = isParameter
				&& !llsaDataflow.getAnalysis().isStoreAlive(liveStoreSetAtEntry, local);
				if (parameterThatIsDeadAtEntry && !complainedAbout.get(local)) {

					// TODO: add warning properties?
					pendingBugReportAboutOverwrittenParameter = new BugInstance(this, "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN",
							storeLive ? NORMAL_PRIORITY : HIGH_PRIORITY).addClassAndMethod(methodGen,
									javaClass.getSourceFileName()).add(lvAnnotation).addSourceLine(classContext, methodGen,
											javaClass.getSourceFileName(), location.getHandle());
					complainedAbout.set(local);
				}

				if (storeLive)
					continue;

				TypeFrame typeFrame = typeDataflow.getAnalysis().getFactAtLocation(location);
				Type typeOfValue = null;
				if (typeFrame.isValid() && typeFrame.getStackDepth() > 0) {
					typeOfValue = TypeFrame.getTopType();
				}

				boolean storeOfNull = false;
				InstructionHandle prevInsHandle = location.getHandle().getPrev();
				if (prevInsHandle != null) {
					Instruction prevIns = prevInsHandle.getInstruction();
					boolean foundDeadClassInitialization = false;
					String initializationOf = null;
					if (prevIns instanceof GETSTATIC) {
							GETSTATIC getStatic = (GETSTATIC)prevIns;
							ConstantPoolGen cpg = methodGen.getConstantPool();
							foundDeadClassInitialization =  getStatic.getFieldName(cpg).startsWith("class$")
									&& getStatic.getSignature(cpg).equals("Ljava/lang/Class;");
							for (Iterator<Location> j = cfg.locationIterator(); j.hasNext();) {
								Location location2 = j.next();
								if (location2.getHandle().getPosition() + 15 == location.getHandle().getPosition()) {
									Instruction  instruction2 = location2.getHandle().getInstruction();
									if (instruction2 instanceof LDC) {
										String n = (String) ((LDC)instruction2).getValue(methodGen.getConstantPool());
										initializationOf = ClassName.toSignature(n);
									}
								}}

						}
					else if (prevIns instanceof LDC) {
						LDC ldc = (LDC) prevIns;
						Type t = ldc.getType(methodGen.getConstantPool());
						if (t.getSignature().equals("Ljava/lang/Class;")) {
							ConstantClass v = (ConstantClass) ldc.getValue(methodGen.getConstantPool());
							initializationOf = ClassName.toSignature(v.getBytes(javaClass.getConstantPool()));
							foundDeadClassInitialization = true;
						}
						
					}
					if (foundDeadClassInitialization) {
						BugInstance bugInstance = new BugInstance(this,  "DLS_DEAD_STORE_OF_CLASS_LITERAL", 
								Priorities.NORMAL_PRIORITY).addClassAndMethod(
										methodGen,
										javaClass.getSourceFileName()).add(lvAnnotation).addType(initializationOf);
						accumulator.accumulateBug(bugInstance, sourceLineAnnotation);
						continue;
					}

					if (prevIns instanceof LDC || prevIns instanceof ConstantPushInstruction)
						propertySet.addProperty(DeadLocalStoreProperty.STORE_OF_CONSTANT);
					else if (prevIns instanceof ACONST_NULL) {
						storeOfNull = true;
						propertySet.addProperty(DeadLocalStoreProperty.STORE_OF_NULL);
					}
				}

				for (Field f : javaClass.getFields()) {
					if (f.getName().equals(name)) {
						propertySet.addProperty(DeadLocalStoreProperty.SHADOWS_FIELD);
						break;
					}
				}

				if (typeOfValue instanceof BasicType || Type.STRING.equals(typeOfValue))
					propertySet.addProperty(DeadLocalStoreProperty.BASE_VALUE);

				// Ignore assignments that were killed by a subsequent
				// assignment.
				boolean killedBySubsequentStore = llsaDataflow.getAnalysis().killedByStore(liveStoreSet, local);
				if (killedBySubsequentStore)
					propertySet.addProperty(DeadLocalStoreProperty.KILLED_BY_SUBSEQUENT_STORE);

				// Ignore dead assignments of null and 0.
				// These often indicate defensive programming.
				InstructionHandle prev = location.getBasicBlock().getPredecessorOf(location.getHandle());
				int prevOpCode = -1;

				if (prev != null) {
					if (defensiveConstantValueOpcodes.get(prev.getInstruction().getOpcode())) {
						propertySet.addProperty(DeadLocalStoreProperty.DEFENSIVE_CONSTANT_OPCODE);
						prevOpCode = prev.getInstruction().getOpcode();
					}

					if (prev.getInstruction() instanceof GETFIELD) {
						InstructionHandle prev2 = prev.getPrev();

						if (prev2 != null && prev2.getInstruction() instanceof ALOAD)
							propertySet.addProperty(DeadLocalStoreProperty.CACHING_VALUE);
					}
					if (prev.getInstruction() instanceof LoadInstruction)
						propertySet.addProperty(DeadLocalStoreProperty.COPY_VALUE);
					if (prev.getInstruction() instanceof InvokeInstruction)
						propertySet.addProperty(DeadLocalStoreProperty.METHOD_RESULT);
				}
				boolean deadObjectStore = false;
				if (ins instanceof IINC) {
					// special handling of IINC

					propertySet.addProperty(DeadLocalStoreProperty.DEAD_INCREMENT);
					if (localIncrementCount[local] == 1) {
						propertySet.addProperty(DeadLocalStoreProperty.SINGLE_DEAD_INCREMENT);
					} else
						propertySet.removeProperty(DeadLocalStoreProperty.IS_PARAMETER);

				} else if (ins instanceof ASTORE && prev != null) {
					// Look for objects created but never used

					Instruction prevIns = prev.getInstruction();
					if ((prevIns instanceof INVOKESPECIAL && ((INVOKESPECIAL) prevIns).getMethodName(methodGen.getConstantPool())
							.equals("<init>"))
							|| prevIns instanceof ANEWARRAY || prevIns instanceof NEWARRAY || prevIns instanceof MULTIANEWARRAY) {
						deadObjectStore = true;

					}

				}
				if (deadObjectStore)
					propertySet.addProperty(DeadLocalStoreProperty.DEAD_OBJECT_STORE);
				else if (!killedBySubsequentStore && localStoreCount[local] == 2 && localLoadCount[local] > 0) {
					// TODO: why is this significant?

					propertySet.addProperty(DeadLocalStoreProperty.TWO_STORES_MULTIPLE_LOADS);

				} else if (!parameterThatIsDeadAtEntry && localStoreCount[local] == 1 && localLoadCount[local] == 0
						&& propertySet.containsProperty(DeadLocalStoreProperty.DEFENSIVE_CONSTANT_OPCODE)) {
					// might be final local constant
					propertySet.addProperty(DeadLocalStoreProperty.SINGLE_STORE);

				} else if (!parameterThatIsDeadAtEntry 
						&& !propertySet.containsProperty(DeadLocalStoreProperty.SHADOWS_FIELD) 
						&& localLoadCount[local] == 0) {
					// TODO: why is this significant?
					propertySet.addProperty(DeadLocalStoreProperty.NO_LOADS);

				}

				if (parameterThatIsDeadAtEntry) {
					propertySet.addProperty(DeadLocalStoreProperty.PARAM_DEAD_ON_ENTRY);
					if (pendingBugReportAboutOverwrittenParameter != null)
						pendingBugReportAboutOverwrittenParameter.setPriority(Detector.HIGH_PRIORITY);
				}

				if (localStoreCount[local] > 3)
					propertySet.addProperty(DeadLocalStoreProperty.MANY_STORES);

				int priority = propertySet.computePriority(NORMAL_PRIORITY);
				if (priority <= Detector.EXP_PRIORITY) {

					// Report the warning
					BugInstance bugInstance = new BugInstance(this, storeOfNull ? "DLS_DEAD_LOCAL_STORE_OF_NULL"
							: "DLS_DEAD_LOCAL_STORE", priority).addClassAndMethod(
									methodGen,
									javaClass.getSourceFileName()).add(lvAnnotation);

					// If in relaxed reporting mode, encode heuristic
					// information.
					if (FindBugsAnalysisFeatures.isRelaxedMode()) {
						// Add general-purpose warning properties
						WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);

						// Turn all warning properties into BugProperties
						propertySet.decorateBugInstance(bugInstance);
					}

					if (DEBUG) {
						System.out.println(javaClass.getSourceFileName() + " : " + methodGen.getName());
						System.out.println("priority: " + priority);
						System.out.println("Reporting " + bugInstance);
						System.out.println(propertySet);
					}
					accumulator.accumulateBug(bugInstance, sourceLineAnnotation);
				}
			} finally {
				if (pendingBugReportAboutOverwrittenParameter != null)
					bugReporter.reportBug(pendingBugReportAboutOverwrittenParameter);
			}
		}

		suppressWarningsIfOneLiveStoreOnLine(accumulator, liveStoreSourceLineSet);

		accumulator.reportAccumulatedBugs();
	}

	/**
	 * If feature is enabled, suppress warnings where there is at least
	 * one live store on the line where the warning would be reported.
	 * 
	 * @param accumulator            BugAccumulator containing warnings for method
	 * @param liveStoreSourceLineSet bitset of lines where at least one live store was seen
	 */
	private void suppressWarningsIfOneLiveStoreOnLine(BugAccumulator accumulator, BitSet liveStoreSourceLineSet) {
		if (!SUPPRESS_IF_AT_LEAST_ONE_LIVE_STORE_ON_LINE) {
			return;
		}

		// Eliminate any accumulated warnings for instructions
		// that (due to inlining) *can* be live stores.
	entryLoop:
		for (Iterator<? extends BugInstance> i = accumulator.uniqueBugs().iterator(); i.hasNext(); ) {
			
			for (SourceLineAnnotation annotation : accumulator.locations(i.next())) {
				if (liveStoreSourceLineSet.get(annotation.getStartLine())) {
					// This instruction can be a live store; don't report
					// it as a warning.
					i.remove();
					continue entryLoop;
				}
			}
		}
	}

	/**
	 * Count stores, loads, and increments of local variables in method whose
	 * CFG is given.
	 * 
	 * @param localStoreCount
	 *            counts of local stores (indexed by local)
	 * @param localLoadCount
	 *            counts of local loads (indexed by local)
	 * @param localIncrementCount
	 *            counts of local increments (indexed by local)
	 * @param cfg
	 *            control flow graph (CFG) of method
	 */
	private void countLocalStoresLoadsAndIncrements(int[] localStoreCount, int[] localLoadCount, int[] localIncrementCount,
			CFG cfg) {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			if (location.getBasicBlock().isExceptionHandler())
				continue;

			boolean isStore = isStore(location);
			boolean isLoad = isLoad(location);
			if (!isStore && !isLoad)
				continue;

			IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
			int local = ins.getIndex();
			if (ins instanceof IINC) {
				localStoreCount[local]++;
				localLoadCount[local]++;
				localIncrementCount[local]++;
			} else if (isStore)
				localStoreCount[local]++;
			else
				localLoadCount[local]++;
		}
	}

	/**
	 * Get the name of given local variable (if possible) and store it in the
	 * HeuristicPropertySet.
	 * 
	 * @param lvt
	 *            the LocalVariableTable
	 * @param local
	 *            index of the local
	 * @param pc
	 *            program counter value of the instruction
	 */
	private void checkLocalVariableName(LocalVariableTable lvt, int local, int pc, WarningPropertySet<DeadLocalStoreProperty> propertySet) {
		if (lvt != null) {
			LocalVariable lv = lvt.getLocalVariable(local, pc);
			if (lv != null) {
				String localName = lv.getName();
				propertySet.setProperty(DeadLocalStoreProperty.LOCAL_NAME, localName);
			}
		}

	}

	/**
	 * Is instruction at given location a store?
	 * 
	 * @param location
	 *            the location
	 * @return true if instruction at given location is a store, false if not
	 */
	private boolean isStore(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof StoreInstruction) || (ins instanceof IINC);
	}

	/**
	 * Is instruction at given location a load?
	 * 
	 * @param location
	 *            the location
	 * @return true if instruction at given location is a load, false if not
	 */
	private boolean isLoad(Location location) {
		Instruction ins = location.getHandle().getInstruction();
		return (ins instanceof LoadInstruction) || (ins instanceof IINC);
	}

	public void report() {
	}
}

//vim:ts=4
