/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.UseAnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonCollector;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder;
import edu.umd.cs.findbugs.ba.npe.NullValueUnconditionalDeref;
import edu.umd.cs.findbugs.ba.npe.PointerUsageRequiringNonNullValue;
import edu.umd.cs.findbugs.ba.npe.RedundantBranch;
import edu.umd.cs.findbugs.ba.npe.ReturnPathType;
import edu.umd.cs.findbugs.ba.npe.ReturnPathTypeDataflow;
import edu.umd.cs.findbugs.ba.npe.UsagesRequiringNonNullValues;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.props.GeneralWarningProperty;
import edu.umd.cs.findbugs.props.WarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;
import edu.umd.cs.findbugs.visitclass.Util;

/**
 * A Detector to find instructions where a NullPointerException might be raised.
 * We also look for useless reference comparisons involving null and non-null
 * values.
 *
 * @author David Hovemeyer
 * @author William Pugh
 * @see edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis
 */
public class NoiseNullDeref implements Detector, UseAnnotationDatabase, NullDerefAndRedundantComparisonCollector {

    public static final boolean DEBUG = SystemProperties.getBoolean("fnd.debug");

    private static final boolean DEBUG_NULLARG = SystemProperties.getBoolean("fnd.debug.nullarg");

    //    private static final boolean DEBUG_NULLRETURN = SystemProperties.getBoolean("fnd.debug.nullreturn");

    private static final boolean MARK_DOOMED = SystemProperties.getBoolean("fnd.markdoomed", true);

    private static final String METHOD = SystemProperties.getProperty("fnd.method");

    private static final String CLASS = SystemProperties.getProperty("fnd.class");

    // Fields
    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    // Transient state
    private ClassContext classContext;

    private Method method;

    private ValueNumberDataflow vnaDataflow;

    public NoiseNullDeref(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;

        String currentMethod = null;

        JavaClass jclass = classContext.getJavaClass();
        String className = jclass.getClassName();
        String superClassName = jclass.getSuperclassName();
        if (superClassName.endsWith("ProtocolMessage")) {
            return;
        }
        if (CLASS != null && !className.equals(CLASS)) {
            return;
        }
        Method[] methodList = jclass.getMethods();
        for (Method method : methodList) {
            try {
                if (method.isAbstract() || method.isNative() || method.getCode() == null) {
                    continue;
                }

                currentMethod = SignatureConverter.convertMethodSignature(jclass, method);

                if (METHOD != null && !method.getName().equals(METHOD)) {
                    continue;
                }
                if (DEBUG || DEBUG_NULLARG) {
                    System.out.println("Checking for NP in " + currentMethod);
                }
                analyzeMethod(classContext, method);
            } catch (MissingClassException e) {
                bugReporter.reportMissingClass(e.getClassNotFoundException());
            } catch (DataflowAnalysisException e) {
                bugReporter.logError("While analyzing " + currentMethod + ": FindNullDeref caught dae exception", e);
            } catch (CFGBuilderException e) {
                bugReporter.logError("While analyzing " + currentMethod + ": FindNullDeref caught cfgb exception", e);
            }
            bugAccumulator.reportAccumulatedBugs();
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {
        if (DEBUG || DEBUG_NULLARG) {
            System.out.println("Pre FND ");
        }

        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }

        // UsagesRequiringNonNullValues uses =
        // classContext.getUsagesRequiringNonNullValues(method);
        this.method = method;

        if (DEBUG || DEBUG_NULLARG) {
            System.out.println("FND: " + SignatureConverter.convertMethodSignature(methodGen));
        }

        findPreviouslyDeadBlocks();

        vnaDataflow = classContext.getValueNumberDataflow(method);

        // Create a NullDerefAndRedundantComparisonFinder object to do the
        // actual
        // work. It will call back to report null derefs and redundant null
        // comparisons
        // through the NullDerefAndRedundantComparisonCollector interface we
        // implement.
        NullDerefAndRedundantComparisonFinder worker = new NullDerefAndRedundantComparisonFinder(classContext, method, this);
        worker.execute();

    }

    /**
     * Find set of blocks which were known to be dead before doing the null
     * pointer analysis.
     *
     * @return set of previously dead blocks, indexed by block id
     * @throws CFGBuilderException
     * @throws DataflowAnalysisException
     */
    private BitSet findPreviouslyDeadBlocks() throws DataflowAnalysisException, CFGBuilderException {
        BitSet deadBlocks = new BitSet();
        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
        for (Iterator<BasicBlock> i = vnaDataflow.getCFG().blockIterator(); i.hasNext();) {
            BasicBlock block = i.next();
            ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(block);
            if (vnaFrame.isTop()) {
                deadBlocks.set(block.getLabel());
            }
        }

        return deadBlocks;
    }

    static class CheckCallSitesAndReturnInstructions {
    }

    @Override
    public void report() {
    }

    public boolean skipIfInsideCatchNull() {
        return classContext.getJavaClass().getClassName().indexOf("Test") >= 0 || method.getName().indexOf("test") >= 0
                || method.getName().indexOf("Test") >= 0;
    }

    /**
     * @deprecated Use
     *             {@link #foundNullDeref(Location,ValueNumber,IsNullValue,ValueNumberFrame,boolean)}
     *             instead
     */
    @Override
    @Deprecated
    public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue, ValueNumberFrame vnaFrame) {
        foundNullDeref(location, valueNumber, refValue, vnaFrame, true);
    }

    @Override
    public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue, ValueNumberFrame vnaFrame,
            boolean isConsistent) {
        if (!refValue.isNullOnComplicatedPath23()) {
            return;
        }
        WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();
        if (valueNumber.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
            return;
        }

        boolean onExceptionPath = refValue.isException();
        if (onExceptionPath) {
            propertySet.addProperty(GeneralWarningProperty.ON_EXCEPTION_PATH);
        }
        BugAnnotation variable = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, valueNumber, vnaFrame,
                "VALUE_OF");
        addPropertiesForDereferenceLocations(propertySet, Collections.singleton(location));
        Instruction ins = location.getHandle().getInstruction();
        BugAnnotation cause;
        final ConstantPoolGen cpg = classContext.getConstantPoolGen();

        if (ins instanceof InvokeInstruction) {
            InvokeInstruction iins = (InvokeInstruction) ins;
            XMethod invokedMethod = XFactory.createXMethod((InvokeInstruction) ins, cpg);
            cause = MethodAnnotation.fromXMethod(invokedMethod);
            cause.setDescription(MethodAnnotation.METHOD_CALLED);

            if ("close".equals(iins.getMethodName(cpg)) && "()V".equals(iins.getSignature(cpg))) {
                propertySet.addProperty(NullDerefProperty.CLOSING_NULL);
            }
        } else if (ins instanceof FieldInstruction) {
            FieldInstruction fins = (FieldInstruction) ins;
            XField referencedField = XFactory.createXField(fins, cpg);
            cause = FieldAnnotation.fromXField(referencedField);

        } else {
            cause = new StringAnnotation(ins.getName());
        }

        boolean caught = inCatchNullBlock(location);
        if (caught && skipIfInsideCatchNull()) {
            return;
        }

        int basePriority = Priorities.NORMAL_PRIORITY;

        if (!refValue.isNullOnComplicatedPath2()) {
            basePriority--;
        }
        reportNullDeref(propertySet, location, "NOISE_NULL_DEREFERENCE", basePriority, cause, variable);

    }

    private void reportNullDeref(WarningPropertySet<WarningProperty> propertySet, Location location, String type, int priority,
            BugAnnotation cause, @CheckForNull BugAnnotation variable) {

        BugInstance bugInstance = new BugInstance(this, type, priority).addClassAndMethod(classContext.getJavaClass(), method);
        bugInstance.add(cause);
        if (variable != null) {
            bugInstance.add(variable);
        } else {
            bugInstance.add(new LocalVariableAnnotation("?", -1, -1));
        }
        bugInstance.addSourceLine(classContext, method, location).describe("SOURCE_LINE_DEREF");

        if (FindBugsAnalysisFeatures.isRelaxedMode()) {
            WarningPropertyUtil.addPropertiesForDataMining(propertySet, classContext, method, location);
        }
        addPropertiesForDereferenceLocations(propertySet, Collections.singleton(location));

        propertySet.decorateBugInstance(bugInstance);

        bugReporter.reportBug(bugInstance);
    }

    public static boolean isThrower(BasicBlock target) {
        InstructionHandle ins = target.getFirstInstruction();
        int maxCount = 7;
        while (ins != null) {
            if (maxCount-- <= 0) {
                break;
            }
            Instruction i = ins.getInstruction();
            if (i instanceof ATHROW) {
                return true;
            }
            if (i instanceof InstructionTargeter || i instanceof ReturnInstruction) {
                return false;
            }
            ins = ins.getNext();
        }
        return false;
    }

    @Override
    public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch) {

    }

    @Override
    public void foundGuaranteedNullDeref(@Nonnull Set<Location> assignedNullLocationSet, @Nonnull Set<Location> derefLocationSet,
            SortedSet<Location> doomedLocations, ValueNumberDataflow vna, ValueNumber refValue,
            @CheckForNull BugAnnotation variableAnnotation, NullValueUnconditionalDeref deref, boolean npeIfStatementCovered) {
    }

    private void addPropertiesForDereferenceLocations(WarningPropertySet<WarningProperty> propertySet,
            Collection<Location> derefLocationSet) {
        boolean derefOutsideCatchBlock = false;
        boolean allDerefsAtDoomedLocations = true;
        for (Location loc : derefLocationSet) {
            if (!inCatchNullBlock(loc)) {
                derefOutsideCatchBlock = true;
            }

            if (!isDoomed(loc)) {
                allDerefsAtDoomedLocations = false;
            }
        }

        if (allDerefsAtDoomedLocations) {
            // Add a WarningProperty
            propertySet.addProperty(DoomedCodeWarningProperty.DOOMED_CODE);
        }
        boolean uniqueDereferenceLocations = uniqueLocations(derefLocationSet);

        if (!derefOutsideCatchBlock) {
            if (!uniqueDereferenceLocations || skipIfInsideCatchNull()) {
                propertySet.addProperty(GeneralWarningProperty.FALSE_POSITIVE);
            } else {
                propertySet.addProperty(NullDerefProperty.DEREFS_IN_CATCH_BLOCKS);
            }
        }
        if (!uniqueDereferenceLocations) {
            // Add a WarningProperty
            propertySet.addProperty(NullDerefProperty.DEREFS_ARE_CLONED);
        }

        addPropertiesForMethodContainingWarning(propertySet);
    }

    private boolean uniqueLocations(Collection<Location> derefLocationSet) {
        boolean uniqueDereferenceLocations = false;
        LineNumberTable table = method.getLineNumberTable();
        if (table == null) {
            uniqueDereferenceLocations = true;
        } else {
            BitSet linesMentionedMultipleTimes = classContext.linesMentionedMultipleTimes(method);
            for (Location loc : derefLocationSet) {
                int lineNumber = table.getSourceLine(loc.getHandle().getPosition());
                if (!linesMentionedMultipleTimes.get(lineNumber)) {
                    uniqueDereferenceLocations = true;
                }
            }
        }
        return uniqueDereferenceLocations;
    }

    private void addPropertiesForMethodContainingWarning(WarningPropertySet<WarningProperty> propertySet) {
        XMethod xMethod = XFactory.createXMethod(classContext.getJavaClass(), method);

        boolean uncallable = !AnalysisContext.currentXFactory().isCalledDirectlyOrIndirectly(xMethod) && xMethod.isPrivate();

        if (uncallable) {
            propertySet.addProperty(GeneralWarningProperty.IN_UNCALLABLE_METHOD);
        }
    }

    private boolean isDoomed(Location loc) {
        if (!MARK_DOOMED) {
            return false;
        }

        ReturnPathTypeDataflow rptDataflow;
        try {
            rptDataflow = classContext.getReturnPathTypeDataflow(method);

            ReturnPathType rpt = rptDataflow.getFactAtLocation(loc);

            return !rpt.canReturnNormally();
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Error getting return path type", e);
            return false;
        }
    }

    String getDescription(Location loc, ValueNumber refValue) {
        PointerUsageRequiringNonNullValue pu;
        try {
            UsagesRequiringNonNullValues usages = classContext.getUsagesRequiringNonNullValues(method);
            pu = usages.get(loc, refValue, vnaDataflow);
            if (pu == null) {
                return "SOURCE_LINE_DEREF";
            }
            return pu.getDescription();
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("Error getting UsagesRequiringNonNullValues for " + method, e);
            return "SOURCE_LINE_DEREF";
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Error getting UsagesRequiringNonNullValues for " + method, e);
            return "SOURCE_LINE_DEREF";
        }

    }

    boolean inCatchNullBlock(Location loc) {
        int pc = loc.getHandle().getPosition();
        int catchSize = Util.getSizeOfSurroundingTryBlock(classContext.getJavaClass().getConstantPool(), method.getCode(),
                "java/lang/NullPointerException", pc);
        if (catchSize < Integer.MAX_VALUE) {
            return true;
        }
        catchSize = Util.getSizeOfSurroundingTryBlock(classContext.getJavaClass().getConstantPool(), method.getCode(),
                "java/lang/Exception", pc);
        if (catchSize < 5) {
            return true;
        }
        catchSize = Util.getSizeOfSurroundingTryBlock(classContext.getJavaClass().getConstantPool(), method.getCode(),
                "java/lang/RuntimeException", pc);
        if (catchSize < 5) {
            return true;
        }
        catchSize = Util.getSizeOfSurroundingTryBlock(classContext.getJavaClass().getConstantPool(), method.getCode(),
                "java/lang/Throwable", pc);
        if (catchSize < 5) {
            return true;
        }
        return false;

    }
}
