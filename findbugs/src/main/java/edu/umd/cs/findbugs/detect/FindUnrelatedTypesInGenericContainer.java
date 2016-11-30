/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassSummary;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.TestCaseDetector;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities.TypeCategory;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;
import edu.umd.cs.findbugs.props.GeneralWarningProperty;
import edu.umd.cs.findbugs.props.WarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.util.MultiMap;

/**
 * @author Nat Ayewah
 * @author William Pugh
 */
public class FindUnrelatedTypesInGenericContainer implements Detector {

    private final BugReporter bugReporter;

    private static final boolean DEBUG = SystemProperties.getBoolean("gc.debug");

    static class Info {
        public Info(ClassDescriptor interfaceForCall, int argumentIndex, int typeIndex) {
            this.interfaceForCall = interfaceForCall;
            this.argumentIndex = argumentIndex;
            this.typeIndex = typeIndex;
        }
        final ClassDescriptor interfaceForCall;
        final int argumentIndex;
        final int typeIndex;

        @Override
        public String toString() {
            return String.format("[%s %d %d]", interfaceForCall, argumentIndex, typeIndex);
        }
    }
    /**
     * Map classname, methodname and signature to an int []. Each position in
     * the int [] corresponds to an argument in the methodSignature. For each
     * argument i, the value at position i corresponds to the index of the
     * corresponding type in the class type parameters. If the argument has no
     * correspondence, then the value is -1.
     * <p>
     *
     * Get the String key by calling getCollectionsMapKey()
     */

    private final MultiMap<String, Info> callMap = new MultiMap<String, Info>(LinkedList.class);


    private void addCheckedCall(@DottedClassName String className, String methodName, String sig, int argumentParameterIndex, int typeParameterIndex) {
        ClassDescriptor c = DescriptorFactory.instance().getClassDescriptorForDottedClassName(className);
        String call = methodName+sig;
        Info info = new Info(c, argumentParameterIndex, typeParameterIndex);
        callMap.add(call, info);
    }

    private void addCheckedCall(@DottedClassName String className, String methodName, int typeParameterIndex) {
        addCheckedCall(className, methodName, "(Ljava/lang/Object;)", 0, typeParameterIndex);
    }


    public FindUnrelatedTypesInGenericContainer(BugReporter bugReporter) {
        this.bugReporter = bugReporter;

        // Collection<E>
        addCheckedCall(Collection.class.getName(), "contains", 0);
        addCheckedCall(Collection.class.getName(), "remove", 0);
        addCheckedCall(Collection.class.getName(), "containsAll", "(Ljava/util/Collection;)", 0, -1);
        addCheckedCall(Collection.class.getName(), "removeAll", "(Ljava/util/Collection;)", 0, -1);
        addCheckedCall(Collection.class.getName(), "retainAll", "(Ljava/util/Collection;)", 0, -1);

        // Dequeue<E>
        addCheckedCall("java.util.Deque", "removeFirstOccurrence", 0);
        addCheckedCall("java.util.Deque", "removeLastOccurrence", 0);

        // List<E>
        addCheckedCall(List.class.getName(), "indexOf", 0);
        addCheckedCall(List.class.getName(), "lastIndexOf", 0);

        // Vector<E>
        addCheckedCall(Vector.class.getName(), "indexOf",  "(Ljava/lang/Object;I)", 0, 0);
        addCheckedCall(Vector.class.getName(), "lastIndexOf",  "(Ljava/lang/Object;I)", 0, 0);

        // Map<K,V>
        addCheckedCall(Map.class.getName(), "containsKey", 0);
        addCheckedCall(Map.class.getName(), "containsValue", 1);
        addCheckedCall(Map.class.getName(), "get", 0);
        addCheckedCall(Map.class.getName(), "remove", 0);

        // Hashtable<K,V>
        addCheckedCall(Hashtable.class.getName(), "contains", 1);

        // ConcurrentHashMap<K,V>
        addCheckedCall(ConcurrentHashMap.class.getName(), "contains", 1);

        // ConcurrentMap<K,V>
        addCheckedCall(ConcurrentMap.class.getName(), "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall(ConcurrentMap.class.getName(), "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);


        // Multimap<K,V>
        addCheckedCall("com.google.common.collect.Multimap", "containsEntry", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall("com.google.common.collect.Multimap", "containsEntry", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);
        addCheckedCall("com.google.common.collect.Multimap", "containsKey", 0);
        addCheckedCall("com.google.common.collect.Multimap", "containsValue", 1);
        addCheckedCall("com.google.common.collect.Multimap", "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall("com.google.common.collect.Multimap", "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);
        addCheckedCall("com.google.common.collect.Multimap", "removeAll", 0);

        // Cache<K,V>
        addCheckedCall("com.google.common.cache.Cache", "invalidate", 0);

        // Multiset<E>
        addCheckedCall("com.google.common.collect.Multiset", "count", 0);
        addCheckedCall("com.google.common.collect.Multiset", "remove","(Ljava/lang/Object;I)", 0, 0);

        // Table<R,C,V>
        addCheckedCall("com.google.common.collect.Table", "contains", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall("com.google.common.collect.Table", "contains", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);
        addCheckedCall("com.google.common.collect.Table", "containsRow", 0);
        addCheckedCall("com.google.common.collect.Table", "containsColumn", 1);
        addCheckedCall("com.google.common.collect.Table", "containsValue", 2);
        addCheckedCall("com.google.common.collect.Table", "get", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall("com.google.common.collect.Table", "get", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);
        addCheckedCall("com.google.common.collect.Table", "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 0, 0);
        addCheckedCall("com.google.common.collect.Table", "remove", "(Ljava/lang/Object;Ljava/lang/Object;)", 1, 1);


        // Sets
        addCheckedCall("com.google.common.collect.Sets", "intersection", "(Ljava/util/Set;Ljava/util/Set;)", 1, -1);
        addCheckedCall("com.google.common.collect.Sets", "difference", "(Ljava/util/Set;Ljava/util/Set;)", 1, -1);
        addCheckedCall("com.google.common.collect.Sets", "symmetricDifference", "(Ljava/util/Set;Ljava/util/Set;)", 1, -1);


        // Iterables
        addCheckedCall("com.google.common.collect.Iterables", "contains", "(Ljava/lang/Iterable;Ljava/lang/Object;)", 1, 0);
        addCheckedCall("com.google.common.collect.Iterables", "removeAll", "(Ljava/lang/Iterable;Ljava/util/Collection;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterables", "retainAll", "(Ljava/lang/Iterable;Ljava/util/Collection;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterables", "elementsEqual", "(Ljava/lang/Iterable;Ljava/lang/Iterable;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterables", "frequency", "(Ljava/lang/Iterable;Ljava/lang/Object;)", 1, 0);

        // Iterators
        addCheckedCall("com.google.common.collect.Iterators", "contains", "(Ljava/util/Iterator;Ljava/lang/Object;)", 1, 0);
        addCheckedCall("com.google.common.collect.Iterators", "removeAll", "(Ljava/util/Iterator;Ljava/util/Collection;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterators", "retainAll", "(Ljava/util/Iterator;Ljava/util/Collection;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterators", "elementsEqual", "(Ljava/util/Iterator;Ljava/util/Iterator;)", 1, -1);
        addCheckedCall("com.google.common.collect.Iterators", "frequency", "(Ljava/util/Iterator;Ljava/lang/Object;)", 1, 0);


    }

    /**
     * Visit the class context
     *
     * @see edu.umd.cs.findbugs.Detector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
     */
    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (MethodUnprofitableException e) {
                assert true; // move along; nothing to see
            } catch (CFGBuilderException e) {
                String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
                        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
                bugReporter.logError(msg, e);
            } catch (DataflowAnalysisException e) {
                String msg = "Detector " + this.getClass().getName() + " caught exception while analyzing "
                        + javaClass.getClassName() + "." + method.getName() + " : " + method.getSignature();
                bugReporter.logError(msg, e);
            }
        }
    }

    /**
     * Use this to screen out methods that do not contain invocations.
     */
    public boolean prescreen(ClassContext classContext, Method method) {
        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        return bytecodeSet != null
                && (bytecodeSet.get(Constants.INVOKEINTERFACE) || bytecodeSet.get(Constants.INVOKEVIRTUAL)
                        || bytecodeSet.get(Constants.INVOKESPECIAL) || bytecodeSet.get(Constants.INVOKESTATIC) || bytecodeSet
                        .get(Constants.INVOKENONVIRTUAL));
    }

    /**
     * Methods marked with the "Synthetic" attribute do not appear in the source
     * code
     */
    private boolean isSynthetic(Method m) {
        if ((m.getAccessFlags() & Constants.ACC_SYNTHETIC) != 0) {
            return true;
        }
        Attribute[] attrs = m.getAttributes();
        for (Attribute attr : attrs) {
            if (attr instanceof Synthetic) {
                return true;
            }
        }
        return false;
    }

    @StaticConstant
    final static Set<String> baseGenericTypes = new LinkedHashSet<String>();
    static {
        baseGenericTypes.addAll(Arrays.asList(new String[] { "java.util.Map", "java.util.Collection", "java.lang.Iterable",
                "java.util.Iterator", "com.google.common.collect.Multimap", "com.google.common.collect.Multiset",
        "com.google.common.collect.Table" }));
    }

    private boolean isGenericCollection(ClassDescriptor operandClass) {
        String dottedClassName = operandClass.getDottedClassName();

        if (baseGenericTypes.contains(dottedClassName)) {
            return true;
        }

        String found = null;
        for(String c : baseGenericTypes) {
            if (Subtypes2.instanceOf(operandClass, c)) {
                found = c;
                break;
            }
        }
        if (found == null) {
            return false;
        }
        if (dottedClassName.startsWith("java.util.") || dottedClassName.startsWith("com.google.common.collect.") ) {
            return true;
        }
        try {
            XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, operandClass);

            String sig = xclass.getSourceSignature();
            if (sig == null) {
                return false;
            }

            String typeParameter = null;
            List<String> split = GenericUtilities.split(sig, true);
            if (sig.charAt(0) == '<') {
                int end = sig.indexOf(':');
                if (end > 0) {
                    typeParameter = sig.substring(1, end);
                }

            }
            if (DEBUG) {
                System.out.println(dottedClassName + " " + typeParameter + " " + split);
            }
            for (String s : split) {
                int i = s.indexOf('<');
                if (i < 0) {
                    continue;
                }
                if (s.charAt(0) != 'L') {
                    throw new IllegalStateException("unexpected non signature: " + s);
                }
                ClassDescriptor c = DescriptorFactory.createClassDescriptor(s.substring(1, i));
                String superTypeParameter = s.substring(i+1);
                if (isGenericCollection(c) && (typeParameter == null || superTypeParameter.startsWith("T" + typeParameter))) {
                    if (DEBUG) {
                        System.out.println(operandClass + " is a subtype of " + s);
                    }
                    return true;
                }
            }
            if (DEBUG) {
                System.out.println("Not a subtype");
            }


        } catch (CheckedAnalysisException e1) {
            AnalysisContext.logError("Error checking for weird generic parameterization of " + operandClass, e1);
        }
        return false;
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        if (isSynthetic(method) || !prescreen(classContext, method)) {
            return;
        }
        XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
        if (xmethod.isSynthetic()) {
            return;
        }

        BugAccumulator accumulator = new BugAccumulator(bugReporter);

        CFG cfg = classContext.getCFG(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        ValueNumberDataflow vnDataflow = classContext.getValueNumberDataflow(method);

        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }
        String fullMethodName = methodGen.getClassName() + "." + methodGen.getName();

        String sourceFile = classContext.getJavaClass().getSourceFileName();
        if (DEBUG) {
            System.out.println("\n" + fullMethodName);
        }

        // Process each instruction
        for (Iterator<Location> iter = cfg.locationIterator(); iter.hasNext();) {
            Location location = iter.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            // Only consider invoke instructions
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }

            InvokeInstruction inv = (InvokeInstruction) ins;

            XMethod invokedMethod = XFactory.createXMethod(inv, cpg);

            String invokedMethodName = invokedMethod.getName();
            String argSignature = invokedMethod.getSignature();
            argSignature = argSignature.substring(0, argSignature.indexOf(')') + 1);
            String call = invokedMethodName+argSignature;
            SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));

            Collection<Info> collection = callMap.get(call);
            if (!callMap.containsKey(call)) {
                continue;
            }
            for(Info info : collection) {
                Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
                if (DEBUG) {
                    System.out.println("at " + handle.getPosition() + " Checking call to " + info.interfaceForCall + " : " + invokedMethod);
                }
                try {
                    if (!subtypes2.isSubtype(invokedMethod.getClassDescriptor(), info.interfaceForCall)) {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    if ("java/util/Collection".equals(info.interfaceForCall.getClassName())
                            && "com.google.common.collect.Multiset".equals(invokedMethod.getClassName())) {
                        assert true;
                        // we know this is OK without needing to find definition of Multiset
                    } else {
                        AnalysisContext.reportMissingClass(e);
                        continue;
                    }
                }

                boolean allMethod;

                int typeArgument;
                if (info.typeIndex >= 0) {
                    allMethod = false;
                    typeArgument = info.typeIndex;
                } else {
                    allMethod = true;
                    typeArgument = -(1+info.typeIndex);
                }
                int pos = info.argumentIndex;


                int lhsPos;
                if (inv instanceof INVOKESTATIC) {
                    lhsPos = sigParser.getSlotsFromTopOfStackForParameter(0);
                } else {
                    lhsPos = sigParser.getTotalArgumentSize();
                }

                int stackPos = sigParser.getSlotsFromTopOfStackForParameter(pos);

                TypeFrame frame = typeDataflow.getFactAtLocation(location);
                if (!frame.isValid()) {
                    // This basic block is probably dead
                    continue;
                }


                Type operandType = frame.getStackValue(stackPos);
                if (operandType.equals(TopType.instance())) {
                    // unreachable
                    continue;
                }

                if (operandType.equals(NullType.instance())) {
                    // ignore
                    continue;
                }

                ValueNumberFrame vnFrame = vnDataflow.getFactAtLocation(location);

                if (!vnFrame.isValid()) {
                    AnalysisContext.logError("Invalid value number frame in " + xmethod);
                    continue;
                }

                ValueNumber objectVN = vnFrame.getStackValue(lhsPos);
                ValueNumber argVN = vnFrame.getStackValue(stackPos);

                if (objectVN.equals(argVN)) {
                    String bugPattern = "DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES";
                    int priority = HIGH_PRIORITY;
                    if ("removeAll".equals(invokedMethodName)) {
                        bugPattern = "DMI_USING_REMOVEALL_TO_CLEAR_COLLECTION";
                        priority = NORMAL_PRIORITY;
                    } else if (invokedMethodName.endsWith("All")) {
                        bugPattern = "DMI_VACUOUS_SELF_COLLECTION_CALL";
                        priority = NORMAL_PRIORITY;
                    }
                    if (invokedMethodName.startsWith("contains")) {
                        InstructionHandle next = handle.getNext();
                        if (next != null) {
                            Instruction nextIns = next.getInstruction();

                            if (nextIns instanceof InvokeInstruction) {
                                XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                                if ("assertFalse".equals(nextMethod.getName())) {
                                    continue;
                                }
                            }
                        }
                    }
                    accumulator.accumulateBug(
                            new BugInstance(this, bugPattern, priority)
                            .addClassAndMethod(methodGen, sourceFile)
                            .addCalledMethod(methodGen, (InvokeInstruction) ins)
                            .addOptionalAnnotation(
                                    ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, objectVN,
                                            vnFrame, "INVOKED_ON")), SourceLineAnnotation.fromVisitedInstruction(
                                                    classContext, methodGen, sourceFile, handle));
                }

                // Only consider generic...
                Type objectType = frame.getStackValue(lhsPos);
                if (!(objectType instanceof GenericObjectType)) {
                    continue;
                }

                GenericObjectType operand = (GenericObjectType) objectType;

                int expectedTypeParameters = 1;
                String simpleName = info.interfaceForCall.getSimpleName();
                if ( simpleName.toLowerCase().endsWith("map") || "Hashtable".equals(simpleName)) {
                    expectedTypeParameters = 2;
                } else if ("Table".equals(simpleName)) {
                    expectedTypeParameters = 3;
                }

                // ... containers
                if (!operand.hasParameters()) {
                    continue;
                }
                if (operand.getNumParameters() != expectedTypeParameters) {
                    continue;
                }
                ClassDescriptor operandClass = DescriptorFactory.getClassDescriptor(operand);
                if (!isGenericCollection(operandClass)) {
                    continue;
                }

                if (expectedTypeParameters == 2 &&
                        Subtypes2.instanceOf(operandClass, Map.class)
                        && !TypeFrameModelingVisitor.isStraightGenericMap(operandClass)) {
                    continue;
                }
                Type expectedType;
                if (allMethod) {
                    expectedType = operand;
                } else {
                    expectedType = operand.getParameterAt(typeArgument);
                }
                Type actualType = frame.getStackValue(stackPos);
                Type equalsType = actualType;
                if (allMethod) {
                    if (!(actualType instanceof GenericObjectType)) {
                        continue;
                    }
                    equalsType = ((GenericObjectType)actualType).getParameterAt(typeArgument);
                }


                IncompatibleTypes matchResult = compareTypes(expectedType, actualType, allMethod);

                boolean parmIsObject = "Ljava/lang/Object;".equals(expectedType.getSignature());
                boolean selfOperation = !allMethod && operand.equals(actualType) && !parmIsObject;
                if (!allMethod && !parmIsObject && actualType instanceof GenericObjectType) {

                    GenericObjectType p2 = (GenericObjectType) actualType;
                    List<? extends ReferenceType> parameters = p2.getParameters();
                    if (parameters != null && parameters.equals(operand.getParameters())) {
                        selfOperation = true;
                    }
                }

                if (!selfOperation && ( matchResult == IncompatibleTypes.SEEMS_OK || matchResult.getPriority() == Priorities.IGNORE_PRIORITY)) {
                    continue;
                }

                if (invokedMethodName.startsWith("contains") || "remove".equals(invokedMethodName)) {
                    InstructionHandle next = handle.getNext();
                    if (next != null) {
                        Instruction nextIns = next.getInstruction();

                        if (nextIns instanceof InvokeInstruction) {
                            XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                            if ("assertFalse".equals(nextMethod.getName())) {
                                continue;
                            }
                        }
                    }
                } else if ("get".equals(invokedMethodName) || "remove".equals(invokedMethodName)) {
                    InstructionHandle next = handle.getNext();
                    if (next != null) {
                        Instruction nextIns = next.getInstruction();

                        if (nextIns instanceof InvokeInstruction) {
                            XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                            if ("assertNull".equals(nextMethod.getName())) {
                                continue;
                            }
                        }
                    }
                }
                boolean noisy = false;
                if ("get".equals(invokedMethodName)) {
                    UnconditionalValueDerefDataflow unconditionalValueDerefDataflow = classContext
                            .getUnconditionalValueDerefDataflow(method);

                    UnconditionalValueDerefSet unconditionalDeref = unconditionalValueDerefDataflow.getFactAtLocation(location);
                    ValueNumberFrame vnAfter = vnDataflow.getFactAfterLocation(location);
                    ValueNumber top = vnAfter.getTopValue();
                    noisy = unconditionalDeref.getValueNumbersThatAreUnconditionallyDereferenced().contains(top);
                }
                // Prepare bug report
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                        sourceFile, handle);

                // Report a bug that mentions each of the failed arguments in
                // matches

                if (expectedType instanceof GenericObjectType) {
                    expectedType = ((GenericObjectType) expectedType).getUpperBound();
                }

                int priority = matchResult.getPriority();
                if (!operandClass.getClassName().startsWith("java/util") && priority == Priorities.HIGH_PRIORITY) {
                    priority = Math.max(priority, Priorities.NORMAL_PRIORITY);
                }
                if (TestCaseDetector.likelyTestCase(xmethod)) {
                    priority = Math.max(priority, Priorities.NORMAL_PRIORITY);
                } else if (selfOperation) {
                    priority = Priorities.HIGH_PRIORITY;
                }
                ClassDescriptor expectedClassDescriptor = DescriptorFactory
                        .createClassOrObjectDescriptorFromSignature(expectedType.getSignature());
                ClassDescriptor actualClassDescriptor = DescriptorFactory.createClassOrObjectDescriptorFromSignature(equalsType
                        .getSignature());
                ClassSummary classSummary = AnalysisContext.currentAnalysisContext().getClassSummary();
                Set<XMethod> targets = null;
                try {
                    targets = Hierarchy2.resolveVirtualMethodCallTargets(actualClassDescriptor, "equals",
                            "(Ljava/lang/Object;)Z", false, false);
                    boolean allOk = targets.size() > 0;
                    for (XMethod m2 : targets) {
                        if (!classSummary.mightBeEqualTo(m2.getClassDescriptor(), expectedClassDescriptor)) {
                            allOk = false;
                        }
                    }
                    if (allOk) {
                        priority += 2;
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
                String bugPattern = "GC_UNRELATED_TYPES";

                BugInstance bug = new BugInstance(this, bugPattern, priority)
                .addClassAndMethod(methodGen, sourceFile)
                .addFoundAndExpectedType(actualType, expectedType)
                .addCalledMethod(methodGen, (InvokeInstruction) ins)
                .addOptionalAnnotation(
                        ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, objectVN, vnFrame,
                                "INVOKED_ON"))
                                .addOptionalAnnotation(
                                        ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, argVN, vnFrame, "ARGUMENT"))
                                        .addEqualsMethodUsed(targets);
                if (noisy) {
                    WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();

                    propertySet.addProperty(GeneralWarningProperty.NOISY_BUG);
                    propertySet.decorateBugInstance(bug);
                }
                accumulator.accumulateBug(bug, sourceLineAnnotation);
            }
        }
        accumulator.reportAccumulatedBugs();
    }

    /**
     * Compare to see if the argument <code>argType</code> passed to the method
     * matches the type of the corresponding parameter. The simplest case is
     * when both are equal.
     * <p>
     * This is a conservative comparison - returns true if it cannot decide. If
     * the parameter type is a type variable (e.g. <code>T</code>) then we don't
     * know enough (yet) to decide if they do not match so return true.
     *
     * @param ignoreBaseType
     *            TODO
     */
    private IncompatibleTypes compareTypes(Type expectedType, Type actualType, boolean ignoreBaseType) {
        // XXX equality not implemented for GenericObjectType
        // if (parmType.equals(argType)) return true;
        if (expectedType == actualType) {
            return IncompatibleTypes.SEEMS_OK;
        }
        // Compare type signatures instead
        String expectedString = GenericUtilities.getString(expectedType);
        String actualString = GenericUtilities.getString(actualType);
        if (expectedString.equals(actualString)) {
            return IncompatibleTypes.SEEMS_OK;
        }

        if (expectedType.equals(Type.OBJECT))
        {
            return IncompatibleTypes.SEEMS_OK;
            // if either type is java.lang.Object, then automatically true!
            // again compare strings...
        }

        String objString = GenericUtilities.getString(Type.OBJECT);

        if (expectedString.equals(objString)) {
            return IncompatibleTypes.SEEMS_OK;
        }

        // get a category for each type
        TypeCategory expectedCat = GenericUtilities.getTypeCategory(expectedType);
        TypeCategory argCat = GenericUtilities.getTypeCategory(actualType);
        if (actualString.equals(objString) && expectedCat == TypeCategory.TYPE_VARIABLE) {
            return IncompatibleTypes.SEEMS_OK;
        }
        if (expectedCat == TypeCategory.WILDCARD) {
            return IncompatibleTypes.SEEMS_OK;
        }
        if (ignoreBaseType) {
            if (expectedCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PARAMETERIZED) {
                GenericObjectType parmGeneric = (GenericObjectType) expectedType;
                GenericObjectType argGeneric = (GenericObjectType) actualType;
                return compareTypeParameters(parmGeneric, argGeneric);
            }
            return IncompatibleTypes.SEEMS_OK;
        }

        if (actualType.equals(Type.OBJECT) && expectedCat == TypeCategory.ARRAY_TYPE) {
            return IncompatibleTypes.ARRAY_AND_OBJECT;
        }

        // -~- plain objects are easy
        if (expectedCat == TypeCategory.PLAIN_OBJECT_TYPE && argCat == TypeCategory.PLAIN_OBJECT_TYPE) {
            return IncompatibleTypes.getPriorityForAssumingCompatible(expectedType, actualType, false);
        }

        if (expectedCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PLAIN_OBJECT_TYPE) {
            return IncompatibleTypes.getPriorityForAssumingCompatible((GenericObjectType) expectedType, actualType);
        }
        if (expectedCat == TypeCategory.PLAIN_OBJECT_TYPE && argCat == TypeCategory.PARAMETERIZED) {
            return IncompatibleTypes.getPriorityForAssumingCompatible((GenericObjectType) actualType, expectedType);
        }

        // -~- parmType is: "? extends Another Type" OR "? super Another Type"
        if (expectedCat == TypeCategory.WILDCARD_EXTENDS || expectedCat == TypeCategory.WILDCARD_SUPER) {
            return compareTypes(((GenericObjectType) expectedType).getExtension(), actualType, ignoreBaseType);
        }

        // -~- Not handling type variables
        if (expectedCat == TypeCategory.TYPE_VARIABLE || argCat == TypeCategory.TYPE_VARIABLE) {
            return IncompatibleTypes.SEEMS_OK;
        }

        // -~- Array Types: compare dimensions, then base type
        if (expectedCat == TypeCategory.ARRAY_TYPE && argCat == TypeCategory.ARRAY_TYPE) {
            ArrayType parmArray = (ArrayType) expectedType;
            ArrayType argArray = (ArrayType) actualType;

            if (parmArray.getDimensions() != argArray.getDimensions()) {
                return IncompatibleTypes.ARRAY_AND_NON_ARRAY;
            }

            return compareTypes(parmArray.getBasicType(), argArray.getBasicType(), ignoreBaseType);
        }
        // If one is an Array Type and the other is not, then they
        // are incompatible. (We already know neither is java.lang.Object)
        if (expectedCat == TypeCategory.ARRAY_TYPE ^ argCat == TypeCategory.ARRAY_TYPE) {
            return IncompatibleTypes.ARRAY_AND_NON_ARRAY;
        }

        // -~- Parameter Types: compare base type then parameters
        if (expectedCat == TypeCategory.PARAMETERIZED && argCat == TypeCategory.PARAMETERIZED) {
            GenericObjectType parmGeneric = (GenericObjectType) expectedType;
            GenericObjectType argGeneric = (GenericObjectType) actualType;

            // base types should be related
            {
                IncompatibleTypes result = compareTypes(parmGeneric.getObjectType(), argGeneric.getObjectType(), ignoreBaseType);
                if (!result.equals(IncompatibleTypes.SEEMS_OK)) {
                    return result;
                }
            }
            return compareTypeParameters(parmGeneric, argGeneric);

            // XXX More to come
        }
        // If one is a Parameter Type and the other is not, then they
        // are incompatible. (We already know neither is java.lang.Object)
        if (false) {
            // not true. Consider class Foo extends ArrayList<String>
            if (expectedCat == TypeCategory.PARAMETERIZED ^ argCat == TypeCategory.PARAMETERIZED) {
                return IncompatibleTypes.SEEMS_OK; // fix this when we know what
                // we are doing here
            }
        }

        // -~- Wildcard e.g. List<*>.contains(...)
        if (expectedCat == TypeCategory.WILDCARD) {
            return IncompatibleTypes.SEEMS_OK;
        }

        // -~- Non Reference types
        // if ( parmCat == TypeCategory.NON_REFERENCE_TYPE ||
        // argCat == TypeCategory.NON_REFERENCE_TYPE )
        if (expectedType instanceof BasicType || actualType instanceof BasicType) {
            // this should not be possible, compiler will complain (pre 1.5)
            // or autobox primitive types (1.5 +)
            throw new IllegalArgumentException("checking for compatibility of " + expectedType + " with " + actualType);
        }

        return IncompatibleTypes.SEEMS_OK;

    }

    private IncompatibleTypes compareTypeParameters(GenericObjectType parmGeneric, GenericObjectType argGeneric) {
        int p = parmGeneric.getNumParameters();
        if (p != argGeneric.getNumParameters()) {
            return IncompatibleTypes.SEEMS_OK;
        }
        for (int x = 0; x < p; x++) {
            IncompatibleTypes result = compareTypes(parmGeneric.getParameterAt(x), argGeneric.getParameterAt(x), false);
            if (result != IncompatibleTypes.SEEMS_OK) {
                return result;
            }
        }
        return IncompatibleTypes.SEEMS_OK;
    }

    // old version of compare types
    private boolean compareTypesOld(Type parmType, Type argType) {
        // XXX equality not implemented for GenericObjectType
        // if (parmType.equals(argType)) return true;
        // Compare type signatures instead
        if (GenericUtilities.getString(parmType).equals(GenericUtilities.getString(argType))) {
            return true;
        }

        if (parmType instanceof GenericObjectType) {
            GenericObjectType o = (GenericObjectType) parmType;
            if (o.getTypeCategory() == GenericUtilities.TypeCategory.WILDCARD_EXTENDS) {
                return compareTypesOld(o.getExtension(), argType);
            }
        }
        // ignore type variables for now
        if (parmType instanceof GenericObjectType && !((GenericObjectType) parmType).hasParameters()) {
            return true;
        }
        if (argType instanceof GenericObjectType && !((GenericObjectType) argType).hasParameters()) {
            return true;
        }

        // Case: Both are generic containers
        if (parmType instanceof GenericObjectType && argType instanceof GenericObjectType) {
            return true;
        } else {
            // Don't consider non reference types (should not be possible)
            if (!(parmType instanceof ReferenceType && argType instanceof ReferenceType)) {
                return true;
            }

            // Don't consider non object types (for now)
            if (!(parmType instanceof ObjectType && argType instanceof ObjectType)) {
                return true;
            }

            // Otherwise, compare base types ignoring generic information
            try {
                return Repository.instanceOf(((ObjectType) argType).getClassName(), ((ObjectType) parmType).getClassName());
            } catch (ClassNotFoundException e) {
            }
        }

        return true;
    }

    /**
     * Empty
     *
     * @see edu.umd.cs.findbugs.Detector#report()
     */
    @Override
    public void report() {
    }

}
