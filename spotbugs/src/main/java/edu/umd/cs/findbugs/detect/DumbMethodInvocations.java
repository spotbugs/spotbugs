package edu.umd.cs.findbugs.detect;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

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
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
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

    private static final MethodDescriptor DRIVER_MANAGER_GET_CONNECTION_2 =
            new MethodDescriptor("java/sql/DriverManager", "getConnection",
                    "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;", true);

    private static final MethodDescriptor DRIVER_MANAGER_GET_CONNECTION_1 =
            new MethodDescriptor("java/sql/DriverManager", "getConnection", "(Ljava/lang/String;)Ljava/sql/Connection;", true);

    private static final MethodDescriptor PROPERTIES_SET_PROPERTY =
            new MethodDescriptor("java/util/Properties", "setProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");


    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private final Map<MethodDescriptor, int[]> allFileNameStringMethods;
    private final Map<MethodDescriptor, int[]> allDatabasePasswordMethods;

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
        Map<ValueNumber, String> propertiesPasswords = Collections.emptyMap();
        ValueNumberDataflow valueNumberDataflow = null;
        try {
            valueNumberDataflow = classContext.getValueNumberDataflow(method);
            propertiesPasswords = collectPropertiesPasswords(cfg, constantDataflow, valueNumberDataflow, cpg);
        } catch (MethodUnprofitableException e) {
            // Skip Properties password tracking for unprofitable methods.
        }

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
                    reportPasswordIfConstant(frame, iins, cpg, parser, paramNumber, classContext, methodGen, sourceFile, location);
                }
            } else if (DRIVER_MANAGER_GET_CONNECTION_2.equals(md) && valueNumberDataflow != null) {
                ValueNumberFrame valueNumberFrame = valueNumberDataflow.getFactAtLocation(location);
                if (valueNumberFrame.isValid()) {
                    ValueNumber propertiesValue = valueNumberFrame.getArgument(iins, cpg, 1, parser);
                    String password = propertiesPasswords.get(propertiesValue);
                    if (password != null) {
                        reportPasswordBug(password, classContext, methodGen, sourceFile, location);
                    }
                }
            } else if (DRIVER_MANAGER_GET_CONNECTION_1.equals(md)) {
                Constant urlValue = frame.getArgument(iins, cpg, 0, parser);
                if (urlValue.isConstantString()) {
                    String password = extractJdbcPassword(urlValue.getConstantString());
                    if (password != null) {
                        reportPasswordBug(password, classContext, methodGen, sourceFile, location);
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

    private Map<ValueNumber, String> collectPropertiesPasswords(CFG cfg, ConstantDataflow constantDataflow,
            ValueNumberDataflow valueNumberDataflow, ConstantPoolGen cpg) throws DataflowAnalysisException {
        Map<ValueNumber, String> propertiesPasswords = new HashMap<>();
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            Instruction ins = location.getHandle().getInstruction();
            if (!(ins instanceof InvokeInstruction)) {
                continue;
            }
            InvokeInstruction invoke = (InvokeInstruction) ins;
            MethodDescriptor md = new MethodDescriptor(invoke, cpg);
            if (!PROPERTIES_SET_PROPERTY.equals(md)) {
                continue;
            }
            ConstantFrame frame = constantDataflow.getFactAtLocation(location);
            ValueNumberFrame valueNumberFrame = valueNumberDataflow.getFactAtLocation(location);
            if (!frame.isValid() || !valueNumberFrame.isValid()) {
                continue;
            }
            SignatureParser parser = new SignatureParser(invoke.getSignature(cpg));
            Constant key = frame.getArgument(invoke, cpg, 0, parser);
            Constant value = frame.getArgument(invoke, cpg, 1, parser);
            if (!key.isConstantString() || !value.isConstantString()) {
                continue;
            }
            if (!"password".equalsIgnoreCase(key.getConstantString())) {
                continue;
            }
            ValueNumber propertiesValue = valueNumberFrame.getInstance(invoke, cpg);
            propertiesPasswords.put(propertiesValue, value.getConstantString());
        }
        return propertiesPasswords;
    }

    private void reportPasswordIfConstant(ConstantFrame frame, InvokeInstruction invoke, ConstantPoolGen cpg,
            SignatureParser parser, int paramNumber, ClassContext classContext, MethodGen methodGen, String sourceFile,
            Location location) throws DataflowAnalysisException {
        Constant operandValue = frame.getArgument(invoke, cpg, paramNumber, parser);
        if (operandValue.isConstantString()) {
            reportPasswordBug(operandValue.getConstantString(), classContext, methodGen, sourceFile, location);
        }
    }

    private void reportPasswordBug(String password, ClassContext classContext, MethodGen methodGen, String sourceFile,
            Location location) {
        String bugType = password.isEmpty() ? "DMI_EMPTY_DB_PASSWORD" : "DMI_CONSTANT_DB_PASSWORD";
        bugAccumulator.accumulateBug(new BugInstance(this, bugType, NORMAL_PRIORITY).addClassAndMethod(methodGen, sourceFile),
                classContext, methodGen, sourceFile, location);
    }

    static String extractJdbcPassword(String url) {
        int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return null;
        }
        String query = url.substring(queryStart + 1);
        for (String part : query.split("&")) {
            int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            if ("password".equalsIgnoreCase(part.substring(0, equals))) {
                return part.substring(equals + 1);
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
