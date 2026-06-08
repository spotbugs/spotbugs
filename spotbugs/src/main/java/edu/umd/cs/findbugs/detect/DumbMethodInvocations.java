package edu.umd.cs.findbugs.detect;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.StackConsumer;
import org.apache.bcel.generic.StackProducer;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.detect.BuildStringPassthruGraph.MethodParameter;
import edu.umd.cs.findbugs.detect.BuildStringPassthruGraph.StringPassthruDatabase;

public class DumbMethodInvocations implements Detector {
    private static final MethodDescriptor STRING_SUBSTRING =
            new MethodDescriptor("java/lang/String", "substring", "(I)Ljava/lang/String;");
    private static final MethodDescriptor STRINGBUILDER_SUBSTRING =
            new MethodDescriptor("java/lang/StringBuilder", "substring", "(I)Ljava/lang/String;");
    private static final MethodDescriptor STRINGBUFFER_SUBSTRING =
            new MethodDescriptor("java/lang/StringBuffer", "substring", "(I)Ljava/lang/String;");


    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private final Map<MethodDescriptor, int[]> allFileNameStringMethods;
    private final Map<MethodDescriptor, int[]> allDatabasePasswordMethods;

    private Map<String, String> staticStringFields = Collections.emptyMap();

    public DumbMethodInvocations(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);

        StringPassthruDatabase database = Global.getAnalysisCache().getDatabase(StringPassthruDatabase.class);
        allFileNameStringMethods = database.getFileNameStringMethods();
        allDatabasePasswordMethods = database.findLinkedMethods(Collections.singleton(new MethodParameter(new MethodDescriptor(
                "java/sql/DriverManager", "getConnection",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;", true), 2)));
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        staticStringFields = buildStaticStringFields(classContext.getJavaClass(), classContext.getConstantPoolGen());
        Method[] methodList = classContext.getJavaClass().getMethods();

        for (Method method : methodList) {
            if (method.getCode() == null) {
                continue;
            }

            try {
                analyzeMethod(classContext, method);
                bugAccumulator.reportAccumulatedBugs();
            } catch (MethodUnprofitableException mue) {
                if (SystemProperties.getBoolean("unprofitable.debug")) {
                    // don't
                    // report
                    bugReporter.logError("skipping unprofitable method in " + getClass().getName());
                }
            } catch (CFGBuilderException | DataflowAnalysisException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        ConstantDataflow constantDataflow = classContext.getConstantDataflow(method);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            Instruction ins = location.getHandle().getInstruction();
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }
            InvokeInstruction iins = (InvokeInstruction) ins;

            SignatureParser parser = new SignatureParser(iins.getSignature(cpg));

            ConstantFrame frame = constantDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }

            MethodDescriptor md = new MethodDescriptor(iins, cpg);
            if (allDatabasePasswordMethods.containsKey(md)) {
                for (int paramNumber : allDatabasePasswordMethods.get(md)) {
                    Constant operandValue = frame.getArgument(iins, cpg, paramNumber, parser);
                    String password = resolveDatabasePasswordString(operandValue, location, iins, cpg, paramNumber,
                            parser);
                    if (password == null) {
                        continue;
                    }
                    if (password.isEmpty()) {
                        bugAccumulator.accumulateBug(new BugInstance(this, "DMI_EMPTY_DB_PASSWORD", NORMAL_PRIORITY)
                                .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                    } else {
                        bugAccumulator.accumulateBug(new BugInstance(this, "DMI_CONSTANT_DB_PASSWORD", NORMAL_PRIORITY)
                                .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                    }
                }
            }

            if (md.equals(STRING_SUBSTRING)
                    || md.equals(STRINGBUILDER_SUBSTRING)
                    || md.equals(STRINGBUFFER_SUBSTRING)) {

                Constant operandValue = frame.getTopValue();
                if (!operandValue.isConstantInteger()) {
                    continue;
                }
                int v = operandValue.getConstantInt();
                if (v == 0) {
                    String bugType = "DMI_USELESS_SUBSTRING";
                    if (md.equals(STRINGBUILDER_SUBSTRING) || md.equals(STRINGBUFFER_SUBSTRING)) {
                        bugType = "DMI_MISLEADING_SUBSTRING";
                    }

                    bugAccumulator.accumulateBug(new BugInstance(this, bugType, NORMAL_PRIORITY)
                            .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                }

            } else if (allFileNameStringMethods.containsKey(md)) {

                for (int paramNumber : allFileNameStringMethods.get(md)) {
                    Constant operandValue = frame.getArgument(iins, cpg, paramNumber, parser);
                    if (!operandValue.isConstantString()) {
                        continue;
                    }
                    String v = operandValue.getConstantString();
                    if (isAbsoluteFileName(v) && !v.startsWith("/etc/") && !v.startsWith("/dev/")
                            && !v.startsWith("/proc")) {
                        int priority = NORMAL_PRIORITY;
                        if (v.startsWith("/tmp")) {
                            priority = LOW_PRIORITY;
                        } else if (v.indexOf("/home") >= 0) {
                            priority = HIGH_PRIORITY;
                        }
                        bugAccumulator.accumulateBug(new BugInstance(this, "DMI_HARDCODED_ABSOLUTE_FILENAME", priority)
                                .addClassAndMethod(methodGen, sourceFile).addString(v).describe("FILE_NAME"), classContext,
                                methodGen, sourceFile, location);
                    }
                }

            }

        }
    }

    private static Map<String, String> buildStaticStringFields(JavaClass javaClass, ConstantPoolGen cpg) {
        Map<String, String> result = new HashMap<>();
        for (Field field : javaClass.getFields()) {
            if (!field.isStatic() || !Type.STRING.equals(Type.getType(field.getSignature()))) {
                continue;
            }
            ConstantValue constantValue = field.getConstantValue();
            if (constantValue != null) {
                org.apache.bcel.classfile.Constant constant = cpg.getConstant(constantValue.getConstantValueIndex());
                if (constant instanceof ConstantString) {
                    result.put(field.getName(),
                            ((ConstantString) constant).getBytes(cpg.getConstantPool()));
                }
            }
        }
        Method clinit = null;
        for (Method method : javaClass.getMethods()) {
            if (Const.STATIC_INITIALIZER_NAME.equals(method.getName())) {
                clinit = method;
                break;
            }
        }
        if (clinit == null || clinit.getCode() == null) {
            return result;
        }
        MethodGen methodGen = new MethodGen(clinit, javaClass.getClassName(), cpg);
        InstructionList instructionList = methodGen.getInstructionList();
        String pendingString = null;
        for (InstructionHandle handle = instructionList.getStart(); handle != null; handle = handle.getNext()) {
            Instruction instruction = handle.getInstruction();
            if (instruction instanceof LDC) {
                Object value = ((LDC) instruction).getValue(cpg);
                if (value instanceof String) {
                    pendingString = (String) value;
                } else {
                    pendingString = null;
                }
            } else if (instruction instanceof PUTSTATIC && pendingString != null) {
                PUTSTATIC putStatic = (PUTSTATIC) instruction;
                if (putStatic.getClassName(cpg).equals(javaClass.getClassName())
                        && Type.STRING.equals(Type.getType(putStatic.getSignature(cpg)))) {
                    result.put(putStatic.getName(cpg), pendingString);
                }
                pendingString = null;
            } else if (instruction instanceof StackConsumer || instruction instanceof StackProducer) {
                pendingString = null;
            }
        }
        return result;
    }

    private @CheckForNull String resolveDatabasePasswordString(Constant operandValue, Location location,
            InvokeInstruction invokeInstruction, ConstantPoolGen cpg, int paramNumber, SignatureParser parser) {
        if (operandValue.isConstantString()) {
            return operandValue.getConstantString();
        }
        return stringFromStaticFieldOnStack(location, invokeInstruction, cpg, paramNumber, parser, staticStringFields);
    }

    private static @CheckForNull String stringFromStaticFieldOnStack(Location location, InvokeInstruction invokeInstruction,
            ConstantPoolGen cpg, int paramNumber, SignatureParser parser, Map<String, String> staticStringFields) {
        if (staticStringFields.isEmpty()) {
            return null;
        }
        int targetSlot = parser.getSlotsFromTopOfStackForParameter(paramNumber);
        InstructionHandle handle = location.getHandle().getPrev();
        int producersSeen = 0;
        while (handle != null) {
            Instruction instruction = handle.getInstruction();
            if (instruction instanceof StackProducer) {
                int produce = ((StackProducer) instruction).produceStack(cpg);
                if (produce == Const.UNPREDICTABLE) {
                    return null;
                }
                for (int i = produce - 1; i >= 0; i--) {
                    if (producersSeen == targetSlot) {
                        return stringFromProducer(instruction, cpg, staticStringFields);
                    }
                    producersSeen++;
                }
            }
            if (instruction instanceof StackConsumer) {
                int consume = ((StackConsumer) instruction).consumeStack(cpg);
                if (consume == Const.UNPREDICTABLE) {
                    return null;
                }
                producersSeen = Math.max(0, producersSeen - consume);
            }
            handle = handle.getPrev();
        }
        return null;
    }

    private static @CheckForNull String stringFromProducer(Instruction instruction, ConstantPoolGen cpg,
            Map<String, String> staticStringFields) {
        if (instruction instanceof GETSTATIC) {
            GETSTATIC getStatic = (GETSTATIC) instruction;
            if (!Type.STRING.equals(Type.getType(getStatic.getSignature(cpg)))) {
                return null;
            }
            return staticStringFields.get(getStatic.getName(cpg));
        }
        if (instruction instanceof LDC) {
            Object value = ((LDC) instruction).getValue(cpg);
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    private boolean isAbsoluteFileName(String v) {
        if (v.startsWith("/dev/")) {
            return false;
        }
        if (v.startsWith("/")) {
            return true;
        }
        if (v.startsWith("\\\\")) {
            // UNC pathname like \\Server\share\...
            return true;
        }
        if (v.length() >= 2 && v.charAt(1) == ':') {
            char driveletter = v.charAt(0);
            if ((driveletter >= 'A' && driveletter <= 'Z') || (driveletter >= 'a' && driveletter <= 'z')) {
                return true;
            }
        }
        try {
            File f = new File(v);
            return f.isAbsolute();
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void report() {
    }

}
