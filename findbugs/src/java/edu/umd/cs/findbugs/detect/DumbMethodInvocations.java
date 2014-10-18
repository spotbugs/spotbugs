package edu.umd.cs.findbugs.detect;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;
import edu.umd.cs.findbugs.util.ClassMethodSignature;

public class DumbMethodInvocations implements Detector {
    private static final ClassMethodSignature DRIVER_GET_CONNECTION =
            new ClassMethodSignature("java.sql.DriverManager", "getConnection", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/sql/Connection;");
    private static final ClassMethodSignature STRING_SUBSTRING =
            new ClassMethodSignature("java.lang.String", "substring", "(I)Ljava/lang/String;");

    private static final Set<ClassMethodSignature> FILENAME_STRING_METHODS = new HashSet<>(Arrays.asList(
            new ClassMethodSignature("java.io.File", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.File", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.RandomAccessFile", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
            new ClassMethodSignature("java.nio.file.Paths", "get", "(Ljava/lang/String;[Ljava/lang/String;)Ljava/nio/file/Path;"),
            new ClassMethodSignature("java.io.FileReader", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.FileWriter", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.FileWriter", "<init>", "(Ljava/lang/String;Z)V"),
            new ClassMethodSignature("java.io.FileInputStream", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.FileOutputStream", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.FileOutputStream", "<init>", "(Ljava/lang/String;Z)V"),
            new ClassMethodSignature("java.util.Formatter", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.util.Formatter", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
            new ClassMethodSignature("java.util.Formatter", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Locale;)V"),
            new ClassMethodSignature("java.util.jar.JarFile", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.util.jar.JarFile", "<init>", "(Ljava/lang/String;Z)V"),
            new ClassMethodSignature("java.util.zip.ZipFile", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.util.zip.ZipFile", "<init>", "(Ljava/lang/String;Ljava/nio/charset/Charset;)V"),
            new ClassMethodSignature("java.io.PrintStream", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.PrintStream", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.PrintWriter", "<init>", "(Ljava/lang/String;)V"),
            new ClassMethodSignature("java.io.PrintWriter", "<init>", "(Ljava/lang/String;Ljava/lang/String;)V")
            ));

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    public DumbMethodInvocations(BugReporter bugReporter) {
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
                bugAccumulator.reportAccumulatedBugs();
            } catch (MethodUnprofitableException mue) {
                if (SystemProperties.getBoolean("unprofitable.debug")) {
                    // don't
                    // report
                    bugReporter.logError("skipping unprofitable method in " + getClass().getName());
                }
            } catch (CFGBuilderException e) {
                bugReporter.logError("Detector " + this.getClass().getName() + " caught exception", e);
            } catch (DataflowAnalysisException e) {
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

            ConstantFrame frame = constantDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }

            ClassMethodSignature cms = new ClassMethodSignature(iins, cpg);
            if (cms.equals(DRIVER_GET_CONNECTION)) {
                Constant operandValue = frame.getTopValue();
                if (operandValue.isConstantString()) {
                    String password = operandValue.getConstantString();
                    if (password.length() == 0) {
                        bugAccumulator.accumulateBug(new BugInstance(this, "DMI_EMPTY_DB_PASSWORD", NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                    } else {
                        bugAccumulator.accumulateBug(new BugInstance(this, "DMI_CONSTANT_DB_PASSWORD", NORMAL_PRIORITY)
                        .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                    }

                }
            }

            if (cms.equals(STRING_SUBSTRING)) {

                Constant operandValue = frame.getTopValue();
                if (!operandValue.isConstantInteger()) {
                    continue;
                }
                int v = operandValue.getConstantInt();
                if (v == 0) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "DMI_USELESS_SUBSTRING", NORMAL_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile), classContext, methodGen, sourceFile, location);
                }

            } else if (FILENAME_STRING_METHODS.contains(cms)) {

                Constant operandValue = frame.getStackValue(iins.getArgumentTypes(cpg).length-1);
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

    private boolean isAbsoluteFileName(String v) {
        if (v.startsWith("/dev/")) {
            return false;
        }
        if (v.startsWith("/")) {
            return true;
        }
        if (v.startsWith("C:")) {
            return true;
        }
        if (v.startsWith("c:")) {
            return true;
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
