package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INSTANCEOF;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;
import org.apache.bcel.generic.TypedInstruction;

import edu.umd.cs.findbugs.Analyze;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.Util;

public class FindBadCast2 implements Detector {

    private final BugReporter bugReporter;

    private final Set<String> concreteCollectionClasses = new HashSet<String>();

    private final Set<String> abstractCollectionClasses = new HashSet<String>();

    private final Set<String> veryAbstractCollectionClasses = new HashSet<String>();

    private static final boolean DEBUG = SystemProperties.getBoolean("bc.debug");

    public FindBadCast2(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        veryAbstractCollectionClasses.add("java.util.Collection");
        veryAbstractCollectionClasses.add("java.util.Iterable");
        abstractCollectionClasses.add("java.util.Collection");
        abstractCollectionClasses.add("java.util.List");
        abstractCollectionClasses.add("java.util.Set");
        abstractCollectionClasses.add("java.util.SortedSet");
        abstractCollectionClasses.add("java.util.SortedMap");
        abstractCollectionClasses.add("java.util.Map");
        concreteCollectionClasses.add("java.util.LinkedHashMap");
        concreteCollectionClasses.add("java.util.LinkedHashSet");
        concreteCollectionClasses.add("java.util.HashMap");
        concreteCollectionClasses.add("java.util.HashSet");
        concreteCollectionClasses.add("java.util.TreeMap");
        concreteCollectionClasses.add("java.util.TreeSet");
        concreteCollectionClasses.add("java.util.ArrayList");
        concreteCollectionClasses.add("java.util.LinkedList");
        concreteCollectionClasses.add("java.util.Hashtable");
        concreteCollectionClasses.add("java.util.Vector");

    }

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

    public boolean prescreen(ClassContext classContext, Method method) {
        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        return bytecodeSet != null && (bytecodeSet.get(Constants.CHECKCAST) || bytecodeSet.get(Constants.INSTANCEOF));
    }

    private Set<ValueNumber> getParameterValueNumbers(ClassContext classContext, Method method, CFG cfg)
            throws DataflowAnalysisException, CFGBuilderException {
        ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
        ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());
        Set<ValueNumber> paramValueNumberSet = new HashSet<ValueNumber>();
        int firstParam = method.isStatic() ? 0 : 1;
        for (int i = firstParam; i < vnaFrameAtEntry.getNumLocals(); ++i) {
            paramValueNumberSet.add(vnaFrameAtEntry.getValue(i));
        }
        return paramValueNumberSet;
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        if (BCELUtil.isSynthetic(method) || !prescreen(classContext, method)) {
            return;
        }
        BugAccumulator accumulator = new BugAccumulator(bugReporter);

        CFG cfg = classContext.getCFG(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        IsNullValueDataflow isNullDataflow = classContext.getIsNullValueDataflow(method);
        Set<ValueNumber> paramValueNumberSet = null;

        ValueNumberDataflow vnaDataflow = null;

        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }
        String methodName = methodGen.getClassName() + "." + methodGen.getName();
        String sourceFile = classContext.getJavaClass().getSourceFileName();
        if (DEBUG) {
            System.out.println("Checking " + methodName);
        }

        Set<SourceLineAnnotation> haveInstanceOf = new HashSet<SourceLineAnnotation>();
        Set<SourceLineAnnotation> haveCast = new HashSet<SourceLineAnnotation>();
        Set<SourceLineAnnotation> haveMultipleInstanceOf = new HashSet<SourceLineAnnotation>();
        Set<SourceLineAnnotation> haveMultipleCast = new HashSet<SourceLineAnnotation>();
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            if (!(ins instanceof CHECKCAST) && !(ins instanceof INSTANCEOF)) {
                continue;
            }

            SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                    sourceFile, handle);
            if (ins instanceof CHECKCAST) {
                if (!haveCast.add(sourceLineAnnotation)) {
                    haveMultipleCast.add(sourceLineAnnotation);
                    if (DEBUG) {
                        System.out.println("Have multiple casts for " + sourceLineAnnotation);
                    }
                }
            } else {
                if (!haveInstanceOf.add(sourceLineAnnotation)) {
                    haveMultipleInstanceOf.add(sourceLineAnnotation);
                    if (DEBUG) {
                        System.out.println("Have multiple instanceof for " + sourceLineAnnotation);
                    }
                }
            }
        }
        BitSet linesMentionedMultipleTimes = classContext.linesMentionedMultipleTimes(method);
        LineNumberTable lineNumberTable = methodGen.getLineNumberTable(methodGen.getConstantPool());
        Map<BugAnnotation, String> instanceOfChecks = new HashMap<BugAnnotation, String>();
        String constantClass = null;
        boolean methodInvocationWasGeneric = false;

        int pcForConstantClass = -1;
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            InstructionHandle handle = location.getHandle();
            int pc = handle.getPosition();
            Instruction ins = handle.getInstruction();

            boolean wasMethodInvocationWasGeneric = methodInvocationWasGeneric;
            methodInvocationWasGeneric = false;
            if (ins instanceof InvokeInstruction) {
                InvokeInstruction iinv = (InvokeInstruction) ins;
                XMethod m = XFactory.createXMethod(iinv, cpg);
                if (m != null) {
                    String sourceSignature = m.getSourceSignature();
                    methodInvocationWasGeneric = sourceSignature != null
                            && (sourceSignature.startsWith("<") || sourceSignature.indexOf("java/lang/Class") >= 0);
                    if (DEBUG && methodInvocationWasGeneric) {
                        System.out.println(m + " has source signature " + sourceSignature);
                    }
                }

            }
            if (ins instanceof LDC) {
                LDC ldc = (LDC) ins;
                Object value = ldc.getValue(cpg);
                if (value instanceof ConstantClass) {
                    ConstantClass cc = (ConstantClass) value;
                    constantClass = cc.getBytes(classContext.getJavaClass().getConstantPool());
                    pcForConstantClass = pc;
                }
            }

            if (!(ins instanceof CHECKCAST) && !(ins instanceof INSTANCEOF)) {
                continue;
            }

            boolean isCast = ins instanceof CHECKCAST;
            int occurrences = cfg.getLocationsContainingInstructionWithOffset(pc).size();
            boolean split = occurrences > 1;
            if (lineNumberTable != null) {
                int line = lineNumberTable.getSourceLine(handle.getPosition());
                if (line > 0 && linesMentionedMultipleTimes.get(line)) {
                    split = true;
                }
            }

            IsNullValueFrame nullFrame = isNullDataflow.getFactAtLocation(location);
            if (!nullFrame.isValid()) {
                continue;
            }
            IsNullValue operandNullness = nullFrame.getTopValue();
            if (DEBUG) {
                String kind = isCast ? "checkedCast" : "instanceof";
                System.out.println(kind + " at pc: " + pc + " in " + methodName);
                System.out.println(" occurrences: " + occurrences);
                System.out.println("XXX: " + operandNullness);
            }

            if (split && !isCast) {
                // don't report this case; it might be infeasible due to
                // inlining
                continue;
            }

            TypeFrame frame = typeDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }

            Type operandType = frame.getTopValue();
            if (operandType.equals(TopType.instance())) {
                // unreachable
                continue;
            }
            boolean operandTypeIsExact = frame.isExact(frame.getStackLocation(0));
            final Type castType = ((TypedInstruction) ins).getType(cpg);

            if (!(castType instanceof ReferenceType)) {
                // This shouldn't happen either
                continue;
            }
            String castSig = castType.getSignature();

            if (operandType.equals(NullType.instance()) || operandNullness.isDefinitelyNull()) {
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                        sourceFile, handle);
                assert castSig.length() > 1;
                if (!isCast) {
                    accumulator.accumulateBug(new BugInstance(this, "NP_NULL_INSTANCEOF", split ? LOW_PRIORITY : NORMAL_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile).addType(castSig), sourceLineAnnotation);
                }
                continue;

            }
            if (!(operandType instanceof ReferenceType)) {
                // Shouldn't happen - illegal bytecode
                continue;
            }
            final ReferenceType refType = (ReferenceType) operandType;
            boolean impliesByGenerics = typeDataflow.getAnalysis().isImpliedByGenericTypes(refType);

            if (impliesByGenerics && !isCast) {
                continue;
            }

            final boolean typesAreEqual = refType.equals(castType);
            if (isCast && typesAreEqual) {
                // System.out.println("self-cast to " +
                // castType.getSignature());
                continue;
            }

            String refSig = refType.getSignature();
            String castSig2 = castSig;
            String refSig2 = refSig;
            while (castSig2.charAt(0) == '[' && refSig2.charAt(0) == '[') {
                castSig2 = castSig2.substring(1);
                refSig2 = refSig2.substring(1);
            }

            SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                    sourceFile, handle);

            if (refSig2.charAt(0) != 'L' || castSig2.charAt(0) != 'L') {
                if (castSig2.charAt(0) == '['
                        && ("Ljava/io/Serializable;".equals(refSig2) || "Ljava/lang/Object;".equals(refSig2) || "Ljava/lang/Cloneable;".equals(refSig2))) {
                    continue;
                }
                if (refSig2.charAt(0) == '['
                        && ("Ljava/io/Serializable;".equals(castSig2) || "Ljava/lang/Object;".equals(castSig2) || "Ljava/lang/Cloneable;".equals(castSig2))) {
                    continue;
                }
                int priority = HIGH_PRIORITY;
                if (split && (castSig2.endsWith("Error;") || castSig2.endsWith("Exception;"))) {
                    priority = LOW_PRIORITY;
                }
                // report bug only if types are not equal, see bug 3598482
                if(!typesAreEqual){
                    bugReporter.reportBug(new BugInstance(this, isCast ? "BC_IMPOSSIBLE_CAST" : "BC_IMPOSSIBLE_INSTANCEOF", priority)
                    .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(refType, castType)
                    .addSourceLine(sourceLineAnnotation));
                }
                continue;
            }

            if (!operandTypeIsExact && "Ljava/lang/Object;".equals(refSig2)) {
                continue;
            }
            /*
            if (false && isCast && haveMultipleCast.contains(sourceLineAnnotation) || !isCast
                    && haveMultipleInstanceOf.contains(sourceLineAnnotation)) {
                // skip; might be due to JSR inlining
                continue;
            }*/
            String castName = castSig2.substring(1, castSig2.length() - 1).replace('/', '.');
            String refName = refSig2.substring(1, refSig2.length() - 1).replace('/', '.');

            if (vnaDataflow == null) {
                vnaDataflow = classContext.getValueNumberDataflow(method);
            }
            ValueNumberFrame vFrame = vnaDataflow.getFactAtLocation(location);
            if (paramValueNumberSet == null) {
                paramValueNumberSet = getParameterValueNumbers(classContext, method, cfg);
            }
            ValueNumber valueNumber = vFrame.getTopValue();
            BugAnnotation valueSource = ValueNumberSourceInfo.findAnnotationFromValueNumber(method, location, valueNumber, vFrame,
                    "VALUE_OF");
            // XXX call below causes 86% of all OpcodeStackDetector.EarlyExitException (getPC() == targetPC) thrown (13000 on java* JDK7 classes)
            BugAnnotation source = BugInstance.getSourceForTopStackValue(classContext, method, location);
            boolean isParameter = paramValueNumberSet.contains(valueNumber) && source instanceof LocalVariableAnnotation;

            try {
                JavaClass castJavaClass = Repository.lookupClass(castName);
                JavaClass refJavaClass = Repository.lookupClass(refName);

                boolean upcast = Repository.instanceOf(refJavaClass, castJavaClass);
                if (upcast || typesAreEqual) {
                    if (!isCast) {
                        accumulator.accumulateBug(new BugInstance(this, "BC_VACUOUS_INSTANCEOF", NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(refType, castType),
                        sourceLineAnnotation);
                    }
                } else {
                    boolean castMayThrow = !Repository.instanceOf(refJavaClass, castJavaClass);
                    boolean downCast = Repository.instanceOf(castJavaClass, refJavaClass);

                    if (!operandTypeIsExact && "java.lang.Object".equals(refName)) {
                        continue;
                    }
                    double rank = 0.0;
                    boolean castToConcreteCollection = concreteCollectionClasses.contains(castName)
                            && abstractCollectionClasses.contains(refName);
                    boolean castToAbstractCollection = abstractCollectionClasses.contains(castName)
                            && veryAbstractCollectionClasses.contains(refName);
                    int position = location.getHandle().getPosition();
                    int catchSize = Util.getSizeOfSurroundingTryBlock(classContext.getJavaClass().getConstantPool(), method.getCode(),
                            "java/lang/ClassCastException", position);



                    if (!operandTypeIsExact) {
                        rank = Analyze.deepInstanceOf(refJavaClass, castJavaClass);
                        if (castToConcreteCollection && rank > 0.6) {
                            rank = (rank + 0.6) / 2;
                        } else if (castToAbstractCollection && rank > 0.3) {
                            rank = (rank + 0.3) / 2;
                        }
                    }
                    /*
                    if (false) {
                        System.out.println("Rank:\t" + rank + "\t" + refName + "\t" + castName);
                    }
                     */
                    boolean completeInformation = (!castJavaClass.isInterface() && !refJavaClass.isInterface())
                            || refJavaClass.isFinal() || castJavaClass.isFinal();
                    if (DEBUG) {
                        System.out.println(" In " + classContext.getFullyQualifiedMethodName(method));
                        System.out.println("At pc: " + handle.getPosition());
                        System.out.println("cast from " + refName + " to " + castName);
                        System.out.println("  cast may throw: " + castMayThrow);
                        System.out.println("  is downcast: " + downCast);
                        System.out.println("  operand type is exact: " + operandTypeIsExact);

                        System.out.println("  complete information: " + completeInformation);
                        System.out.println("  isParameter: " + valueNumber);
                        System.out.println("  score: " + rank);
                        System.out.println("  source is: " + valueSource);
                        if (catchSize < Integer.MAX_VALUE) {
                            System.out.println("  catch block size is: " + catchSize);
                        }
                        if (constantClass != null) {
                            System.out.println("  constant class " + constantClass + " at " + pcForConstantClass);
                        }
                        if (handle.getPrev() == null) {
                            System.out.println("  prev is null");
                        } else {
                            System.out.println("  prev is " + handle.getPrev());
                        }
                    }
                    if (!isCast && castMayThrow && valueSource != null) {
                        String oldCheck = instanceOfChecks.get(valueSource);
                        if (oldCheck == null) {
                            instanceOfChecks.put(valueSource, castName);
                        } else if (!oldCheck.equals(castName)) {
                            instanceOfChecks.put(valueSource, "");
                        }
                    }
                    if (!downCast && completeInformation || operandTypeIsExact) {
                        String bugPattern;
                        if (isCast) {
                            if (downCast && operandTypeIsExact) {
                                if ("[Ljava/lang/Object;".equals(refSig) && source instanceof MethodAnnotation
                                        && "toArray".equals(((MethodAnnotation) source).getMethodName())
                                        && "()[Ljava/lang/Object;".equals(((MethodAnnotation) source).getMethodSignature())) {
                                    bugPattern = "BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY";
                                } else {
                                    bugPattern = "BC_IMPOSSIBLE_DOWNCAST";
                                }
                            } else {
                                bugPattern = "BC_IMPOSSIBLE_CAST";
                            }
                        } else {
                            bugPattern = "BC_IMPOSSIBLE_INSTANCEOF";
                        }

                        bugReporter.reportBug(new BugInstance(this, bugPattern, isCast ? HIGH_PRIORITY : NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile)
                        .addFoundAndExpectedType(refType, castType).addOptionalUniqueAnnotations(valueSource, source)
                        .addSourceLine(sourceLineAnnotation));
                    } else if (isCast && rank < 0.9
                            && !valueNumber.hasFlag(ValueNumber.ARRAY_VALUE)) {

                        int priority = NORMAL_PRIORITY;

                        @CheckForNull String oldCheck = instanceOfChecks.get(valueSource);
                        if (DEBUG) {
                            System.out.println("Old check: " + oldCheck);
                        }
                        if (castName.equals(oldCheck)) {
                            priority += 1;
                        } else if ("".equals(oldCheck)) {
                            priority += 1;
                            if (!(source instanceof LocalVariableAnnotation)) {
                                continue;
                            }
                        }

                        if (rank > 0.75) {
                            priority += 2;
                        } else if (rank > 0.5) {
                            priority += 1;
                        } else if (rank > 0.25) {
                            priority += 0;
                        } else {
                            priority--;
                        }



                        if (DEBUG) {
                            System.out.println(" priority a: " + priority);
                        }
                        if (methodGen.getClassName().startsWith(refName) || methodGen.getClassName().startsWith(castName)) {
                            priority += 1;
                        }
                        if (DEBUG) {
                            System.out.println(" priority b: " + priority);
                        }
                        if (castJavaClass.isInterface() && !castToAbstractCollection) {
                            priority++;
                        }
                        if (DEBUG) {
                            System.out.println(" priority c: " + priority);
                        }
                        if (castToConcreteCollection && veryAbstractCollectionClasses.contains(refName)) {
                            priority--;
                        }
                        if (DEBUG) {
                            System.out.println(" priority d: " + priority);
                        }
                        if (priority <= LOW_PRIORITY && !castToAbstractCollection && !castToConcreteCollection
                                && (refJavaClass.isInterface() || refJavaClass.isAbstract())) {
                            priority++;
                        }
                        if (DEBUG) {
                            System.out.println(" priority e: " + priority);
                        }
                        if (DEBUG) {
                            System.out.println(" ref name: " + refName);
                        }
                        if ("compareTo".equals(methodGen.getName())) {
                            priority++;
                        } else if (methodGen.isPublic() && isParameter && !castName.equals(oldCheck)) {
                            priority--;
                        }
                        if (wasMethodInvocationWasGeneric && valueNumber.hasFlag(ValueNumber.RETURN_VALUE)) {
                            continue;
                        }
                        if (constantClass != null && pcForConstantClass +20 >= pc && valueNumber.hasFlag(ValueNumber.RETURN_VALUE)
                                && ClassName.toDottedClassName(constantClass).equals(castName)) {
                            priority += 2;
                        }
                        if (DEBUG) {
                            System.out.println(" priority f: " + priority);
                        }
                        if (source instanceof MethodAnnotation) {
                            MethodAnnotation m = (MethodAnnotation) source;
                            XMethod xm = m.toXMethod();
                            if (xm != null && (xm.isPrivate() || xm.isStatic()) && priority == Priorities.LOW_PRIORITY) {
                                continue;
                            }
                        }

                        if (valueNumber.hasFlag(ValueNumber.RETURN_VALUE) &&  priority < Priorities.LOW_PRIORITY) {
                            priority = Priorities.LOW_PRIORITY;
                        }
                        if (DEBUG) {
                            System.out.println(" priority g: " + priority);
                        }

                        if (DEBUG) {
                            System.out.println(" priority h: " + priority);
                        }

                        if (catchSize < 15) {
                            return;
                        }
                        if (catchSize < 25) {
                            priority++;
                        }
                        if (DEBUG) {
                            System.out.println(" priority i: " + priority);
                        }


                        if (priority < HIGH_PRIORITY) {
                            priority = HIGH_PRIORITY;
                        }
                        if (priority <= LOW_PRIORITY) {
                            String bug = "BC_UNCONFIRMED_CAST";
                            if (valueNumber.hasFlag(ValueNumber.RETURN_VALUE) || valueSource instanceof MethodAnnotation) {
                                bug = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE";
                            } else if (castToConcreteCollection) {
                                bug = "BC_BAD_CAST_TO_CONCRETE_COLLECTION";
                            } else if (castToAbstractCollection) {
                                bug = "BC_BAD_CAST_TO_ABSTRACT_COLLECTION";
                            }

                            BugInstance bugInstance = new BugInstance(this, bug, priority)
                            .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(refType, castType)
                            .addOptionalAnnotation(valueSource);

                            accumulator.accumulateBug(bugInstance, sourceLineAnnotation);
                        }

                    }

                }
            } catch (ClassNotFoundException e) {
                if (DEBUG) {
                    e.printStackTrace(System.out);
                }
                if (isCast && "[Ljava/lang/Object;".equals(refSig) && source instanceof MethodAnnotation
                        && "toArray".equals(((MethodAnnotation) source).getMethodName())
                        && "()[Ljava/lang/Object;".equals(((MethodAnnotation) source).getMethodSignature())) {
                    bugReporter.reportBug(new BugInstance(this,  "BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", HIGH_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile)
                    .addFoundAndExpectedType(refType, castType).addOptionalUniqueAnnotations(valueSource, source)
                    .addSourceLine(sourceLineAnnotation));
                }


            }
        }
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void report() {
    }

}
