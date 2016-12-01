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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.DUP2;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IRETURN;
import org.apache.bcel.generic.IndexedInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LRETURN;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;
import edu.umd.cs.findbugs.props.WarningProperty;
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
    @StaticConstant
    private static final Set<String> EXCLUDED_LOCALS = new HashSet<String>();

    //    private static final boolean DO_EXCLUDE_LOCALS = SystemProperties.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME) != null;

    static {
        EXCLUDED_LOCALS.add("gxp_locale");
        // Get the value of the property...
        String exclLocalsProperty = SystemProperties.getProperty(FINDBUGS_EXCLUDED_LOCALS_PROP_NAME);

        // If we have one, then split its contents into a list...
        if (exclLocalsProperty != null) {
            for (String s : exclLocalsProperty.split(",")) {
                String s2 = s.trim();
                if (s2.length() > 0) {
                    EXCLUDED_LOCALS.add(s2);
                }
            }
        }
    }

    /**
     * System property to enable a feature that suppresses warnings if there is
     * at least one live store on the line where the warning would be reported.
     * Eliminates some FPs due to inlining/duplication of finally blocks. But,
     * kills some legitimate warnings where there are truly multiple stores on
     * the same line.
     */
    private static final boolean SUPPRESS_IF_AT_LEAST_ONE_LIVE_STORE_ON_LINE = SystemProperties
            .getBoolean("findbugs.dls.suppressIfOneLiveStore");

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
        defensiveConstantValueOpcodes.set(Constants.ICONST_M1);
        defensiveConstantValueOpcodes.set(Constants.ICONST_0);
        defensiveConstantValueOpcodes.set(Constants.ICONST_1);
        defensiveConstantValueOpcodes.set(Constants.ICONST_2);
        defensiveConstantValueOpcodes.set(Constants.ICONST_3);
        defensiveConstantValueOpcodes.set(Constants.ICONST_4);
        defensiveConstantValueOpcodes.set(Constants.ICONST_5);
        defensiveConstantValueOpcodes.set(Constants.LCONST_0);
        defensiveConstantValueOpcodes.set(Constants.LCONST_1);
        defensiveConstantValueOpcodes.set(Constants.LDC);
        defensiveConstantValueOpcodes.set(Constants.LDC_W);
        defensiveConstantValueOpcodes.set(Constants.LDC2_W);
    }

    private final BugReporter bugReporter;

    public FindDeadLocalStores(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        if (DEBUG) {
            System.out.println("Debugging FindDeadLocalStores detector");
        }
    }

    private boolean prescreen(ClassContext classContext, Method method) {
        return true;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();

        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
                continue;
            }

            if (!prescreen(classContext, method)) {
                continue;
            }

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
        if (BCELUtil.isSynthetic(method) || (method.getAccessFlags() & Constants.ACC_BRIDGE) == Constants.ACC_BRIDGE) {
            return;
        }

        if (DEBUG) {
            System.out.println("    Analyzing method " + classContext.getJavaClass().getClassName() + "." + method.getName());
        }

        JavaClass javaClass = classContext.getJavaClass();
        BitSet linesMentionedMultipleTimes = classContext.linesMentionedMultipleTimes(method);
        BugAccumulator accumulator = new BugAccumulator(bugReporter);
        Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow = classContext.getLiveLocalStoreDataflow(method);

        int numLocals = method.getCode().getMaxLocals();
        int[] localStoreCount = new int[numLocals];
        int[] localLoadCount = new int[numLocals];
        int[] localIncrementCount = new int[numLocals];
        MethodGen methodGen = classContext.getMethodGen(method);
        CFG cfg = classContext.getCFG(method);
        if (cfg.isFlagSet(CFG.FOUND_INEXACT_UNCONDITIONAL_THROWERS)) {
            return;
        }
        BitSet liveStoreSetAtEntry = llsaDataflow.getAnalysis().getResultFact(cfg.getEntry());
        BitSet complainedAbout = new BitSet();
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

        // Get number of locals that are parameters.
        int localsThatAreParameters = PreorderVisitor.getNumberArguments(method.getSignature());
        if (!method.isStatic()) {
            localsThatAreParameters++;
        }

        // Scan method to determine number of loads, stores, and increments
        // of local variables.
        countLocalStoresLoadsAndIncrements(localStoreCount, localLoadCount, localIncrementCount, cfg);
        for (int i = 0; i < localsThatAreParameters; i++) {
            localStoreCount[i]++;
        }

        // For each source line, keep track of # times
        // the line was a live store. This can eliminate false positives
        // due to inlining of finally blocks.
        BitSet liveStoreSourceLineSet = new BitSet();

        // Scan method for
        // - dead stores
        // - stores to parameters that are dead upon entry to the method
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            BugInstance pendingBugReportAboutOverwrittenParameter = null;
            try {
                WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();
                // Skip any instruction which is not a store
                if (!isStore(location)) {
                    continue;
                }

                // Heuristic: exception handler blocks often contain
                // dead stores generated by the compiler.
                if (location.getBasicBlock().isExceptionHandler()) {
                    propertySet.addProperty(DeadLocalStoreProperty.EXCEPTION_HANDLER);
                }
                InstructionHandle handle = location.getHandle();
                int pc = handle.getPosition();
                IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();

                int local = ins.getIndex();

                // Get live stores at this instruction.
                // Note that the analysis also computes which stores were
                // killed by a subsequent unconditional store.
                BitSet liveStoreSet = llsaDataflow.getAnalysis().getFactAtLocation(location);

                // Is store alive?
                boolean storeLive = llsaDataflow.getAnalysis().isStoreAlive(liveStoreSet, local);

                LocalVariableAnnotation lvAnnotation = LocalVariableAnnotation.getLocalVariableAnnotation(method, location, ins);

                String sourceFileName = javaClass.getSourceFileName();
                if ("?".equals(lvAnnotation.getName())) {
                    if (sourceFileName.endsWith(".groovy")) {
                        continue;
                    }
                    if (method.getCode().getLocalVariableTable() != null) {
                        continue;
                    }
                }

                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                        sourceFileName, location.getHandle());

                if (DEBUG) {
                    System.out.println("    Store at " + sourceLineAnnotation.getStartLine() + "@"
                            + location.getHandle().getPosition() + " is " + (storeLive ? "live" : "dead"));
                    System.out.println("Previous is: " + location.getHandle().getPrev());
                }

                // Note source lines of live stores.
                if (storeLive && sourceLineAnnotation.getStartLine() > 0) {
                    liveStoreSourceLineSet.set(sourceLineAnnotation.getStartLine());
                }

                String lvName = lvAnnotation.getName();
                if (lvName.charAt(0) == '$' || lvName.charAt(0) == '_') {
                    propertySet.addProperty(DeadLocalStoreProperty.SYNTHETIC_NAME);
                }
                if (EXCLUDED_LOCALS.contains(lvName)) {
                    continue;
                }
                propertySet.setProperty(DeadLocalStoreProperty.LOCAL_NAME, lvName);

                boolean isParameter = local < localsThatAreParameters;
                if (isParameter) {
                    propertySet.addProperty(DeadLocalStoreProperty.IS_PARAMETER);
                }

                Field shadowedField = null;

                for (Field f : javaClass.getFields()) {
                    if (f.getName().equals(lvName) && f.isStatic() == method.isStatic()) {
                        shadowedField = f;
                        propertySet.addProperty(DeadLocalStoreProperty.SHADOWS_FIELD);
                        break;
                    }
                }

                // Is this a store to a parameter which was dead on entry to the
                // method?
                boolean parameterThatIsDeadAtEntry = isParameter
                        && !llsaDataflow.getAnalysis().isStoreAlive(liveStoreSetAtEntry, local);
                if (parameterThatIsDeadAtEntry && !complainedAbout.get(local)) {

                    int priority = storeLive ? LOW_PRIORITY : NORMAL_PRIORITY;
                    if (shadowedField != null) {
                        priority--;
                    }
                    pendingBugReportAboutOverwrittenParameter = new BugInstance(this, "IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN",
                            priority).addClassAndMethod(methodGen, sourceFileName).add(lvAnnotation);

                    if (shadowedField != null) {
                        pendingBugReportAboutOverwrittenParameter.addField(
                                FieldAnnotation.fromBCELField(classContext.getJavaClass(), shadowedField)).describe(
                                        FieldAnnotation.DID_YOU_MEAN_ROLE);
                    }

                    pendingBugReportAboutOverwrittenParameter.addSourceLine(classContext, methodGen, sourceFileName,
                            location.getHandle());
                    complainedAbout.set(local);
                }

                if (storeLive) {
                    continue;
                }

                TypeFrame typeFrame = typeDataflow.getAnalysis().getFactAtLocation(location);
                Type typeOfValue = null;
                if (typeFrame.isValid() && typeFrame.getStackDepth() > 0) {
                    typeOfValue = typeFrame.getTopValue();
                }

                boolean storeOfNull = false;
                InstructionHandle prevInsHandle = location.getHandle().getPrev();
                if (prevInsHandle != null) {
                    Instruction prevIns = prevInsHandle.getInstruction();
                    boolean foundDeadClassInitialization = false;
                    String initializationOf = null;
                    if (prevIns instanceof ConstantPushInstruction) {
                        continue; // not an interesting dead store
                    } else if (prevIns instanceof GETSTATIC) {
                        GETSTATIC getStatic = (GETSTATIC) prevIns;
                        ConstantPoolGen cpg = methodGen.getConstantPool();
                        foundDeadClassInitialization = getStatic.getFieldName(cpg).startsWith("class$")
                                && "Ljava/lang/Class;".equals(getStatic.getSignature(cpg));
                        for (Iterator<Location> j = cfg.locationIterator(); j.hasNext();) {
                            Location location2 = j.next();
                            if (location2.getHandle().getPosition() + 15 == location.getHandle().getPosition()) {
                                Instruction instruction2 = location2.getHandle().getInstruction();
                                if (instruction2 instanceof LDC) {
                                    Object value = ((LDC) instruction2).getValue(methodGen.getConstantPool());
                                    if (value instanceof String) {
                                        String n = (String) value;
                                        if (n.length() > 0) {
                                            initializationOf = ClassName.toSignature(n);
                                        }
                                    }
                                }
                            }
                        }

                    } else if (prevIns instanceof LDC) {
                        LDC ldc = (LDC) prevIns;
                        Type t = ldc.getType(methodGen.getConstantPool());
                        if ("Ljava/lang/Class;".equals(t.getSignature())) {
                            Object value = ldc.getValue(methodGen.getConstantPool());
                            if (value instanceof ConstantClass) {
                                ConstantClass v = (ConstantClass) value;
                                initializationOf = ClassName.toSignature(v.getBytes(javaClass.getConstantPool()));
                                foundDeadClassInitialization = true;
                            } else if (value instanceof ObjectType) {
                                ObjectType v = (ObjectType) value;
                                initializationOf = ClassName.toSignature(v.getClassName());
                                foundDeadClassInitialization = true;
                            } else {
                                AnalysisContext.logError("LDC loaded " + value + "at " + location.getHandle().getPosition() + " in " + classContext.getFullyQualifiedMethodName(method));
                            }

                        }
                        else {
                            continue; // not an interesting DLS
                        }

                    } else if (prevIns instanceof DUP2) {
                        // Check for the case where, due to the bytecode
                        // compiler, a long is needlessly stored just
                        // after we've DUP2'ed the stack and just
                        // before we return
                        Instruction cur = location.getHandle().getInstruction();
                        Instruction nxt = location.getHandle().getNext().getInstruction();
                        if (cur instanceof LSTORE && nxt instanceof LRETURN) {
                            continue; // not an interesting DLS
                        }
                    }
                    if (foundDeadClassInitialization) {
                        if ("org.apache.axis.client.Stub".equals(classContext.getJavaClass().getSuperclassName())) {
                            continue;
                        }
                        BugInstance bugInstance = new BugInstance(this, "DLS_DEAD_STORE_OF_CLASS_LITERAL",
                                Priorities.NORMAL_PRIORITY).addClassAndMethod(methodGen, sourceFileName).add(lvAnnotation)
                                .addType(initializationOf);
                        accumulator.accumulateBug(bugInstance, sourceLineAnnotation);
                        continue;
                    }

                    if (prevIns instanceof LDC || prevIns instanceof ConstantPushInstruction) {
                        propertySet.addProperty(DeadLocalStoreProperty.STORE_OF_CONSTANT);
                    } else if (prevIns instanceof ACONST_NULL) {
                        storeOfNull = true;
                        propertySet.addProperty(DeadLocalStoreProperty.STORE_OF_NULL);
                    }
                }

                if (typeOfValue instanceof BasicType || Type.STRING.equals(typeOfValue)) {
                    propertySet.addProperty(DeadLocalStoreProperty.BASE_VALUE);
                }

                // Ignore assignments that were killed by a subsequent
                // assignment.
                boolean killedBySubsequentStore = llsaDataflow.getAnalysis().killedByStore(liveStoreSet, local);
                if (killedBySubsequentStore) {
                    if (propertySet.containsProperty(DeadLocalStoreProperty.STORE_OF_NULL)
                            || propertySet.containsProperty(DeadLocalStoreProperty.STORE_OF_CONSTANT)) {
                        continue;
                    }
                    propertySet.addProperty(DeadLocalStoreProperty.KILLED_BY_SUBSEQUENT_STORE);
                }

                // Ignore dead assignments of null and 0.
                // These often indicate defensive programming.
                InstructionHandle prev = location.getBasicBlock().getPredecessorOf(location.getHandle());
                //                int prevOpCode = -1;

                if (prev != null) {
                    if (defensiveConstantValueOpcodes.get(prev.getInstruction().getOpcode())) {
                        propertySet.addProperty(DeadLocalStoreProperty.DEFENSIVE_CONSTANT_OPCODE);
                        //                        prevOpCode = prev.getInstruction().getOpcode();
                    }

                    if (prev.getInstruction() instanceof GETFIELD) {
                        InstructionHandle prev2 = prev.getPrev();

                        if (prev2 != null && prev2.getInstruction() instanceof ALOAD) {
                            propertySet.addProperty(DeadLocalStoreProperty.CACHING_VALUE);
                        }
                    }
                    if (prev.getInstruction() instanceof LoadInstruction) {
                        propertySet.addProperty(DeadLocalStoreProperty.COPY_VALUE);
                    }
                    if (prev.getInstruction() instanceof InvokeInstruction) {
                        propertySet.addProperty(DeadLocalStoreProperty.METHOD_RESULT);
                    }
                }
                boolean deadObjectStore = false;
                if (ins instanceof IINC) {
                    // special handling of IINC

                    if ("main".equals(method.getName()) && method.isStatic()
                            && "([Ljava/lang/String;)V".equals(method.getSignature())) {
                        propertySet.addProperty(DeadLocalStoreProperty.DEAD_INCREMENT_IN_MAIN);
                    }

                    InstructionHandle next = location.getHandle().getNext();
                    if (next != null && next.getInstruction() instanceof IRETURN) {
                        propertySet.addProperty(DeadLocalStoreProperty.DEAD_INCREMENT_IN_RETURN);
                    } else {
                        propertySet.addProperty(DeadLocalStoreProperty.DEAD_INCREMENT);
                    }
                    if (localIncrementCount[local] == 1) {
                        propertySet.addProperty(DeadLocalStoreProperty.SINGLE_DEAD_INCREMENT);
                    } else {
                        propertySet.removeProperty(DeadLocalStoreProperty.IS_PARAMETER);
                    }

                } else if (ins instanceof ASTORE && prev != null) {
                    // Look for objects created but never used

                    Instruction prevIns = prev.getInstruction();
                    if ((prevIns instanceof INVOKESPECIAL && "<init>".equals(((INVOKESPECIAL) prevIns).getMethodName(methodGen.getConstantPool())))
                            || prevIns instanceof ANEWARRAY
                            || prevIns instanceof NEWARRAY
                            || prevIns instanceof MULTIANEWARRAY) {
                        deadObjectStore = true;
                    } else if (prevIns instanceof DUP) {
                        propertySet.addProperty(DeadLocalStoreProperty.DUP_THEN_STORE);
                    }
                }
                if (deadObjectStore) {
                    propertySet.addProperty(DeadLocalStoreProperty.DEAD_OBJECT_STORE);
                } else if (!killedBySubsequentStore && localStoreCount[local] == 2 && localLoadCount[local] > 0) {
                    // TODO: why is this significant?

                    propertySet.addProperty(DeadLocalStoreProperty.TWO_STORES_MULTIPLE_LOADS);

                } else if (!parameterThatIsDeadAtEntry && localStoreCount[local] == 1 && localLoadCount[local] == 0
                        && propertySet.containsProperty(DeadLocalStoreProperty.DEFENSIVE_CONSTANT_OPCODE)) {
                    // might be final local constant
                    propertySet.addProperty(DeadLocalStoreProperty.SINGLE_STORE);

                } else if (!parameterThatIsDeadAtEntry && !propertySet.containsProperty(DeadLocalStoreProperty.SHADOWS_FIELD)
                        && localLoadCount[local] == 0) {
                    // TODO: why is this significant?
                    propertySet.addProperty(DeadLocalStoreProperty.NO_LOADS);
                }
                if (!storeOfNull && typeOfValue != null
                        && !propertySet.containsProperty(DeadLocalStoreProperty.EXCEPTION_HANDLER)) {
                    String signatureOfValue = typeOfValue.getSignature();
                    if ((signatureOfValue.startsWith("Ljava/sql/") || signatureOfValue.startsWith("Ljavax/sql/"))
                            && !signatureOfValue.endsWith("Exception")) {
                        propertySet.addProperty(DeadLocalStoreProperty.STORE_OF_DATABASE_VALUE);
                    }
                }

                if (parameterThatIsDeadAtEntry) {
                    propertySet.addProperty(DeadLocalStoreProperty.PARAM_DEAD_ON_ENTRY);
                    if (pendingBugReportAboutOverwrittenParameter != null) {
                        pendingBugReportAboutOverwrittenParameter.setPriority(Priorities.HIGH_PRIORITY);
                    }
                }

                if (localStoreCount[local] > 3) {
                    propertySet.addProperty(DeadLocalStoreProperty.MANY_STORES);
                }
                int occurrences = cfg.getLocationsContainingInstructionWithOffset(pc).size();
                if (occurrences > 2 || sourceLineAnnotation.getStartLine() > 0
                        && linesMentionedMultipleTimes.get(sourceLineAnnotation.getStartLine())) {
                    propertySet.addProperty(DeadLocalStoreProperty.CLONED_STORE);
                }
                String sourceFile = javaClass.getSourceFileName();
                if (Subtypes2.isJSP(javaClass)) {
                    propertySet.addProperty(DeadLocalStoreProperty.IN_JSP_PAGE);
                } else if (BCELUtil.isSynthetic(javaClass) || sourceFile != null && !sourceFile.endsWith(".java")) {
                    if (sourceFile != null && sourceFile.endsWith(".gxp") && (lvName.startsWith("gxp$") || lvName.startsWith("gxp_"))) {
                        continue;
                    }
                    propertySet.addProperty(DeadLocalStoreProperty.NOT_JAVA);
                }

                // Report the warning
                String bugPattern;
                if (storeOfNull) {
                    bugPattern = "DLS_DEAD_LOCAL_STORE_OF_NULL";
                } else if (shadowedField != null) {
                    bugPattern = "DLS_DEAD_LOCAL_STORE_SHADOWS_FIELD";
                } else if (propertySet.containsProperty(DeadLocalStoreProperty.DEAD_INCREMENT_IN_RETURN)) {
                    bugPattern = "DLS_DEAD_LOCAL_INCREMENT_IN_RETURN";
                } else {
                    bugPattern = "DLS_DEAD_LOCAL_STORE";
                }
                BugInstance bugInstance = new BugInstance(this, bugPattern, NORMAL_PRIORITY).addClassAndMethod(methodGen,
                        sourceFileName).add(lvAnnotation);

                if (shadowedField != null) {
                    bugInstance.addField(FieldAnnotation.fromBCELField(classContext.getJavaClass(), shadowedField)).describe(
                            FieldAnnotation.DID_YOU_MEAN_ROLE);
                }

                // If in relaxed reporting mode, encode heuristic
                // information.
                if (FindBugsAnalysisFeatures.isRelaxedMode()) {
                    // Add general-purpose warning properties
                    WarningPropertyUtil.addPropertiesForDataMining(propertySet, classContext, method, location);
                }
                // Turn all warning properties into BugProperties
                propertySet.decorateBugInstance(bugInstance);
                if (DEBUG) {
                    System.out.println(sourceFileName + " : " + methodGen.getName());
                    System.out.println("priority: " + bugInstance.getPriority());
                    System.out.println("Reporting " + bugInstance);
                    System.out.println(propertySet);
                }
                accumulator.accumulateBug(bugInstance, sourceLineAnnotation);

            } finally {
                if (pendingBugReportAboutOverwrittenParameter != null) {
                    bugReporter.reportBug(pendingBugReportAboutOverwrittenParameter);
                }
            }
        }

        suppressWarningsIfOneLiveStoreOnLine(accumulator, liveStoreSourceLineSet);

        accumulator.reportAccumulatedBugs();
    }

    /**
     * If feature is enabled, suppress warnings where there is at least one live
     * store on the line where the warning would be reported.
     *
     * @param accumulator
     *            BugAccumulator containing warnings for method
     * @param liveStoreSourceLineSet
     *            bitset of lines where at least one live store was seen
     */
    private void suppressWarningsIfOneLiveStoreOnLine(BugAccumulator accumulator, BitSet liveStoreSourceLineSet) {
        if (!SUPPRESS_IF_AT_LEAST_ONE_LIVE_STORE_ON_LINE) {
            return;
        }

        // Eliminate any accumulated warnings for instructions
        // that (due to inlining) *can* be live stores.
        entryLoop: for (Iterator<? extends BugInstance> i = accumulator.uniqueBugs().iterator(); i.hasNext();) {

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

            if (location.getBasicBlock().isExceptionHandler()) {
                continue;
            }

            boolean isStore = isStore(location);
            boolean isLoad = isLoad(location);
            if (!isStore && !isLoad) {
                continue;
            }

            IndexedInstruction ins = (IndexedInstruction) location.getHandle().getInstruction();
            int local = ins.getIndex();
            if (ins instanceof IINC) {
                localStoreCount[local]++;
                localLoadCount[local]++;
                localIncrementCount[local]++;
            } else if (isStore) {
                localStoreCount[local]++;
            } else {
                localLoadCount[local]++;
            }
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
     *
    private void checkLocalVariableName(LocalVariableTable lvt, int local, int pc,
            WarningPropertySet<DeadLocalStoreProperty> propertySet) {
        if (lvt != null) {
            LocalVariable lv = lvt.getLocalVariable(local, pc);
            if (lv != null) {
                String localName = lv.getName();
                propertySet.setProperty(DeadLocalStoreProperty.LOCAL_NAME, localName);
            }
        }

    }*/

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

    @Override
    public void report() {
    }
}
