package edu.umd.cs.findbugs.detect;

import java.util.Arrays;

import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.bcp.Opcode;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LocalVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Detects any method class in {@link java.util.Optional#orElse(Object)}.
 *
 * <p>Example of non-complaint code:</p>
 * <pre>
 * return Optional.ofNullable(firstMethod())
 *       // The method secondMethod() invoked always.
 *       // The result of secondMethod() is discarded, unexpected side effects are possible.
 *       .orElse(secondMethod());
 * </pre>
 *
 * <p>Implementation:</p>
 * <ol>
 *     <li>finds any method returning {@code Optional};</li>
 *     <li>finds any method not returning {@code Optional} (exit of the method chain);</li>
 *     <li>if such method is {@code Optional.orElse} and any other method call was
 *         detected in between, then report a bug.</li>
 * </ol>
 *
 * <p>This detector ignores calls to autoboxing methods like {@link Integer#valueOf(int)}.</p>
 */
public class MethodCallInOptionalOrElse extends OpcodeStackDetector {

    public static final String BUG_ID = "MOE_METHOD_INVOKED_IN_OPTIONAL_ORELSE";

    private static final Logger LOG = LoggerFactory.getLogger(MethodCallInOptionalOrElse.class);

    private static final String[] BOXING_TYPES = {
        Byte.class.getName(),
        Character.class.getName(),
        Double.class.getName(),
        Float.class.getName(),
        Integer.class.getName(),
        Long.class.getName(),
        Short.class.getName(),
    };

    private static final String JAVA_UTIL_OPTIONAL_SLASHED_CLASS_NAME = "java/util/Optional";
    private static final String JAVA_UTIL_OPTIONAL_SIGNATURE = "L" + JAVA_UTIL_OPTIONAL_SLASHED_CLASS_NAME + ";";
    private static final String JAVA_UTIL_OPTIONAL_OR_ELSE_METHOD_NAME = "orElse";
    private static final String JAVA_LANG_XXX_VALUE_OF_METHOD_NAME = "valueOf";

    private final BugAccumulator bugAccumulator;

    private XMethod calledMethod;
    private boolean inJavaLangOptionalChain;

    public MethodCallInOptionalOrElse(BugReporter bugReporter) {
        bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
        reset();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.GETSTATIC) {
            final XField field = getXFieldOperand();
            LOG.debug("[{} {}] Load static field {} of type {}", inJavaLangOptionalChain, calledMethod,
                    field.getName(), field.getSignature());
            if (JAVA_UTIL_OPTIONAL_SIGNATURE.equals(field.getSignature())) {
                enterJavaLangOptionalChain();
            }
        } else if (isRegisterLoad()) {
            final LocalVariable local = getMethod().getLocalVariableTable().getLocalVariable(getRegisterOperand(), getNextPC());
            LOG.debug("[{} {}] Load {} of type {}", inJavaLangOptionalChain, calledMethod,
                    local.getName(), local.getSignature());
            if (JAVA_UTIL_OPTIONAL_SIGNATURE.equals(local.getSignature())) {
                enterJavaLangOptionalChain();
            }
        } else if (seen == Const.PUTSTATIC) {
            final XField field = getXFieldOperand();
            LOG.debug("[{} {}] Store static field {} of type {}", inJavaLangOptionalChain, calledMethod,
                    field.getName(), field.getSignature());
            if (inJavaLangOptionalChain && JAVA_UTIL_OPTIONAL_SIGNATURE.equals(field.getSignature())) {
                reset();
            }
        } else if (isRegisterStore()) {
            final LocalVariable local = getMethod().getLocalVariableTable().getLocalVariable(getRegisterOperand(), getNextPC());
            LOG.debug("[{} {}] Store {} of type {}", inJavaLangOptionalChain, calledMethod,
                    local.getName(), local.getSignature());
            if (inJavaLangOptionalChain && JAVA_UTIL_OPTIONAL_SIGNATURE.equals(local.getSignature())) {
                reset();
            }
        } else if (isMethodCall()) {
            LOG.debug("[{} {}] Call {}::{}->{}", inJavaLangOptionalChain, calledMethod,
                    getClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand());
            if (getSigConstantOperand().endsWith(JAVA_UTIL_OPTIONAL_SIGNATURE)) {
                enterJavaLangOptionalChain();
            } else if (inJavaLangOptionalChain) {
                if (JAVA_UTIL_OPTIONAL_SLASHED_CLASS_NAME.equals(getClassConstantOperand())) {
                    if (JAVA_UTIL_OPTIONAL_OR_ELSE_METHOD_NAME.equals(getNameConstantOperand())) {
                        handleOptionalOrElseInvocation();
                    }
                    reset();
                } else if (calledMethod == null && !isBoxingMethod(getXMethodOperand())) {
                    calledMethod = getXMethodOperand();
                }
            }
        }
    }

    private void enterJavaLangOptionalChain() {
        inJavaLangOptionalChain = true;
        calledMethod = null;
    }

    private void reset() {
        inJavaLangOptionalChain = false;
        calledMethod = null;
    }

    private void handleOptionalOrElseInvocation() {
        if (calledMethod != null) {
            BugInstance bug = new BugInstance(this, BUG_ID, HIGH_PRIORITY)
                    .addCalledMethod(calledMethod)
                    .addClassAndMethod(this);
            bugAccumulator.accumulateBug(bug, this);
        }
    }

    private static boolean isBoxingMethod(XMethod method) {
        return method.isStatic()
                && JAVA_LANG_XXX_VALUE_OF_METHOD_NAME.equals(method.getName())
                && Arrays.binarySearch(BOXING_TYPES, method.getClassName()) >= 0;
    }

}
