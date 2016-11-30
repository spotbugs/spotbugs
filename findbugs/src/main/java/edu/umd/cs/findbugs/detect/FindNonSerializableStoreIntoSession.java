package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
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

public class FindNonSerializableStoreIntoSession implements Detector {

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private static final boolean DEBUG = false;

    public FindNonSerializableStoreIntoSession(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Method[] methodList = classContext.getJavaClass().getMethods();

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

            if (!(ins instanceof INVOKEINTERFACE)) {
                continue;
            }

            INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;
            String mName = invoke.getMethodName(cpg);
            if (!"setAttribute".equals(mName)) {
                continue;
            }
            String cName = invoke.getClassName(cpg);
            if (!"javax.servlet.http.HttpSession".equals(cName)) {
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

                    bugAccumulator.accumulateBug(new BugInstance(this, "J2EE_STORE_OF_NON_SERIALIZABLE_OBJECT_INTO_SESSION",
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
