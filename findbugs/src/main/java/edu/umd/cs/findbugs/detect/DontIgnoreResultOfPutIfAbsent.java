/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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
import java.util.concurrent.ConcurrentMap;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.LiveLocalStoreAnalysis;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

public class DontIgnoreResultOfPutIfAbsent implements Detector {

    final static boolean countOtherCalls = false;

    final BugReporter bugReporter;

    final BugAccumulator accumulator;

    final ClassDescriptor concurrentMapDescriptor = DescriptorFactory.createClassDescriptor(ConcurrentMap.class);

    //    private final boolean testingEnabled;

    public DontIgnoreResultOfPutIfAbsent(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
        //        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    @Override
    public void report() {
        //
    }

    @Override
    public void visitClassContext(ClassContext classContext) {

        JavaClass javaClass = classContext.getJavaClass();
        ConstantPool pool = javaClass.getConstantPool();
        boolean found = false;
        for (Constant constantEntry : pool.getConstantPool()) {
            if (constantEntry instanceof ConstantNameAndType) {
                ConstantNameAndType nt = (ConstantNameAndType) constantEntry;
                if ("putIfAbsent".equals(nt.getName(pool))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            return;
        }

        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
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

    final static boolean DEBUG = false;

    @edu.umd.cs.findbugs.internalAnnotations.StaticConstant
    static HashSet<String> immutableClassNames = new HashSet<String>();
    static {
        immutableClassNames.add("java/lang/Integer");
        immutableClassNames.add("java/lang/Long");
        immutableClassNames.add("java/lang/String");
        immutableClassNames.add("java/util/Comparator");
    }

    private static int getPriorityForBeingMutable(Type type) {
        if (type instanceof ArrayType) {
            return HIGH_PRIORITY;
        } else if (type instanceof ObjectType) {
            UnreadFieldsData unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFieldsData();

            ClassDescriptor cd = DescriptorFactory.getClassDescriptor((ObjectType) type);
            @SlashedClassName
            String className = cd.getClassName();
            if (immutableClassNames.contains(className)) {
                return Priorities.LOW_PRIORITY;
            }

            XClass xClass = AnalysisContext.currentXFactory().getXClass(cd);
            if (xClass == null) {
                return Priorities.IGNORE_PRIORITY;
            }
            ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
            if (superclassDescriptor != null) {
                @SlashedClassName
                String superClassName = superclassDescriptor.getClassName();
                if ("java/lang/Enum".equals(superClassName)) {
                    return Priorities.LOW_PRIORITY;
                }
            }
            boolean hasMutableField = false;
            boolean hasUpdates = false;
            for (XField f : xClass.getXFields()) {
                if (!f.isStatic()) {
                    if (!f.isFinal() && !f.isSynthetic()) {
                        hasMutableField = true;
                        if (unreadFields.isWrittenOutsideOfInitialization(f)) {
                            hasUpdates = true;
                        }
                    }
                    String signature = f.getSignature();
                    if (signature.startsWith("Ljava/util/concurrent") || signature.startsWith("Ljava/lang/StringB")
                            || signature.charAt(0) == '[' || signature.indexOf("Map") >= 0 || signature.indexOf("List") >= 0
                            || signature.indexOf("Set") >= 0) {
                        hasMutableField = hasUpdates = true;
                    }

                }
            }

            if (!hasMutableField && !xClass.isInterface() && !xClass.isAbstract()) {
                return Priorities.LOW_PRIORITY;
            }
            if (hasUpdates || className.startsWith("java/util") || className.indexOf("Map") >= 0
                    || className.indexOf("List") >= 0) {
                return Priorities.HIGH_PRIORITY;
            }
            return Priorities.NORMAL_PRIORITY;

        } else {
            return Priorities.IGNORE_PRIORITY;
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
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        Dataflow<BitSet, LiveLocalStoreAnalysis> llsaDataflow = classContext.getLiveLocalStoreDataflow(method);

        MethodGen methodGen = classContext.getMethodGen(method);
        CFG cfg = classContext.getCFG(method);
        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

        String sourceFileName = javaClass.getSourceFileName();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            if (ins instanceof InvokeInstruction) {
                InvokeInstruction invoke = (InvokeInstruction) ins;
                if ("putIfAbsent".equals(invoke.getMethodName(cpg))) {
                    String signature = invoke.getSignature(cpg);
                    if ("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(signature)
                            && !(invoke instanceof INVOKESTATIC)) {
                        TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
                        Type objType = typeFrame.getStackValue(2);
                        if(extendsConcurrentMap(ClassName.toDottedClassName(ClassName.fromFieldSignature(objType.getSignature())))) {
                            InstructionHandle next = handle.getNext();
                            boolean isIgnored = next != null && next.getInstruction() instanceof POP;
                            //                        boolean isImmediateNullTest = next != null
                            //                                && (next.getInstruction() instanceof IFNULL || next.getInstruction() instanceof IFNONNULL);
                            if (countOtherCalls || isIgnored) {
                                BitSet live = llsaDataflow.getAnalysis().getFactAtLocation(location);
                                ValueNumberFrame vna = vnaDataflow.getAnalysis().getFactAtLocation(location);
                                ValueNumber vn = vna.getTopValue();

                                int locals = vna.getNumLocals();
                                //                            boolean isRetained = false;
                                for (int pos = 0; pos < locals; pos++) {
                                    if (vna.getValue(pos).equals(vn) && live.get(pos)) {
                                        BugAnnotation ba = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, vn,
                                                vnaDataflow.getFactAtLocation(location), "VALUE_OF");
                                        if (ba == null) {
                                            continue;
                                        }
                                        String pattern = "RV_RETURN_VALUE_OF_PUTIFABSENT_IGNORED";
                                        if (!isIgnored) {
                                            pattern = "UNKNOWN";
                                        }
                                        Type type = typeFrame.getTopValue();
                                        int priority = getPriorityForBeingMutable(type);
                                        BugInstance bugInstance = new BugInstance(this, pattern, priority)
                                        .addClassAndMethod(methodGen, sourceFileName).addCalledMethod(methodGen, invoke)
                                        .add(new TypeAnnotation(type)).add(ba);
                                        SourceLineAnnotation where = SourceLineAnnotation.fromVisitedInstruction(classContext,
                                                method, location);
                                        accumulator.accumulateBug(bugInstance, where);
                                        //                                    isRetained = true;
                                        break;
                                    }
                                }
                            }
                            /*
                            if (testingEnabled && (countOtherCalls && !isRetained)) {
                                int priority = LOW_PRIORITY;
                                if (!isImmediateNullTest && !isIgnored) {
                                    TypeDataflow typeAnalysis = classContext.getTypeDataflow(method);
                                    Type type = typeAnalysis.getFactAtLocation(location).getTopValue();
                                    String valueSignature = type.getSignature();
                                    if (!valueSignature.startsWith("Ljava/util/concurrent/atomic/Atomic")) {
                                        priority = Priorities.HIGH_PRIORITY;
                                    }
                                }
                                BugInstance bugInstance = new BugInstance(this, "TESTING", priority)
                                .addClassAndMethod(methodGen, sourceFileName).addString("Counting putIfAbsentCalls")
                                .addCalledMethod(methodGen, invoke);
                                SourceLineAnnotation where = SourceLineAnnotation.fromVisitedInstruction(classContext, method,
                                        location);
                                accumulator.accumulateBug(bugInstance, where);
                            }
                             */
                        }
                    } /* else if (testingEnabled && countOtherCalls) {
                        BugInstance bugInstance = new BugInstance(this, "TESTING2", Priorities.NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFileName).addCalledMethod(methodGen, invoke);
                        SourceLineAnnotation where = SourceLineAnnotation.fromVisitedInstruction(classContext, method, location);
                        accumulator.accumulateBug(bugInstance, where);

                    }
                     */
                }

            }
        }
        accumulator.reportAccumulatedBugs();
    }

    private boolean extendsConcurrentMap(@DottedClassName String className) {
        if ("java.util.concurrent.ConcurrentHashMap".equals(className)
                || className.equals(concurrentMapDescriptor.getDottedClassName())) {
            return true;
        }
        ClassDescriptor c = DescriptorFactory.createClassDescriptorFromDottedClassName(className);
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

        try {
            if (subtypes2.isSubtype(c, concurrentMapDescriptor)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }

        return false;

    }

}
