package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.Iterator;

import javax.annotation.CheckForNull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.TopType;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class FindUseOfNonSerializableValue implements Detector {

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private static final boolean DEBUG = false;

    public FindUseOfNonSerializableValue(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        boolean skip = false;
        ConstantPool constantPool = javaClass.getConstantPool();
        for(Constant c : constantPool.getConstantPool() ) {
            if (c instanceof ConstantMethodref || c instanceof ConstantInterfaceMethodref) {
                ConstantCP m = (ConstantCP) c;
                @DottedClassName String clazz = m.getClass(constantPool);
                ConstantNameAndType nt = (ConstantNameAndType) constantPool.getConstant(m.getNameAndTypeIndex(), Constants.CONSTANT_NameAndType);
                String name = nt.getName(constantPool);
                if ("setAttribute".equals(name) && "javax.servlet.http.HttpSession".equals(clazz) || ("writeObject".equals(name)
                        && ("java.io.ObjectOutput".equals(clazz)
                                || "java.io.ObjectOutputStream".equals(clazz)))) {
                    if (DEBUG) {
                        System.out.println("Found call to " + clazz + "." + name);
                    }

                    skip = false;
                    break;
                }

            }
        }
        if (skip) {
            return;
        }
        if (DEBUG) {
            System.out.println(this.getClass().getSimpleName() + " Checking " + javaClass.getClassName());
        }
        Method[] methodList = javaClass.getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            } catch (DataflowAnalysisException e) {
                // bugReporter.logError("Detector " + this.getClass().getName()
                // + " caught exception", e);
            }
            bugAccumulator.reportAccumulatedBugs();
        }
    }

    enum Use { STORE_INTO_HTTP_SESSION, PASSED_TO_WRITE_OBJECT, STORED_IN_SERIALZIED_FIELD }

    @CheckForNull Use getUse(ConstantPoolGen cpg, Instruction ins) {
        if (ins instanceof InvokeInstruction) {
            InvokeInstruction invoke = (InvokeInstruction) ins;

            String mName = invoke.getMethodName(cpg);
            String cName = invoke.getClassName(cpg);

            if ("setAttribute".equals(mName) && "javax.servlet.http.HttpSession".equals(cName)) {
                return Use.STORE_INTO_HTTP_SESSION;
            }
            if ("writeObject".equals(mName)
                    && ("java.io.ObjectOutput".equals(cName)
                            || "java.io.ObjectOutputStream".equals(cName))) {
                return Use.PASSED_TO_WRITE_OBJECT;
            }
        }
        return null;
    }
    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }
        BitSet bytecodeSet = classContext.getBytecodeSet(method);
        if (bytecodeSet == null) {
            return;
        }
        // We don't adequately model instanceof interfaces yet
        if (bytecodeSet.get(Constants.INSTANCEOF) || bytecodeSet.get(Constants.CHECKCAST)) {
            return;
        }
        CFG cfg = classContext.getCFG(method);
        TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        String sourceFile = classContext.getJavaClass().getSourceFileName();
        if (DEBUG) {
            String methodName = methodGen.getClassName() + "." + methodGen.getName();
            System.out.println("Checking " + methodName);
        }

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            InstructionHandle handle = location.getHandle();
            Instruction ins = handle.getInstruction();

            Use use = getUse(cpg, ins);
            if (use == null) {
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
            if (!(operandType instanceof ReferenceType)) {
                // Shouldn't happen - illegal bytecode
                continue;
            }
            ReferenceType refType = (ReferenceType) operandType;

            if (refType.equals(NullType.instance())) {
                continue;
            }

            try {

                double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(refType);

                if (isSerializable < 0.9) {
                    SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext,
                            methodGen, sourceFile, handle);
                    ReferenceType problem = DeepSubtypeAnalysis.getLeastSerializableTypeComponent(refType);

                    String pattern;
                    switch(use) {
                    case PASSED_TO_WRITE_OBJECT:
                        pattern = "DMI_NONSERIALIZABLE_OBJECT_WRITTEN";
                        double isRemote = DeepSubtypeAnalysis.isDeepRemote(refType);
                        if (isRemote >= 0.9) {
                            continue;
                        }
                        if (isSerializable < isRemote) {
                            isSerializable = isRemote;
                        }
                        break;
                    case STORE_INTO_HTTP_SESSION:
                        pattern = "J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION";
                        break;
                    default:
                        throw new IllegalStateException();
                    }

                    bugAccumulator.accumulateBug(new BugInstance(this, pattern,
                            isSerializable < 0.15 ? HIGH_PRIORITY : isSerializable > 0.5 ? LOW_PRIORITY : NORMAL_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile).addType(problem).describe(TypeAnnotation.FOUND_ROLE),
                    sourceLineAnnotation);

                }
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    @Override
    public void report() {
    }

}
