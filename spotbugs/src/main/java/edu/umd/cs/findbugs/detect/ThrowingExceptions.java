package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.commons.lang3.StringUtils;


public class ThrowingExceptions extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public ThrowingExceptions(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private JavaClass klass;
    private String exceptionThrown = null;

    @Override
    public void visit(JavaClass obj) {
        exceptionThrown = null;
        klass = obj;
    }

    @Override
    public void visit(Method obj) {
        exceptionThrown = null;

        if (obj.isSynthetic()) {
            return;
        }

        Stream<String> exceptionStream = null;

        // If the method is generic or is a method of a generic class, then first check the generic signature to avoid detection
        // of generic descendants of Exception or Throwable as Exception or Throwable itself.
        String signature = obj.getGenericSignature();
        String[] exceptions = null;
        if (signature != null) {
            exceptions = StringUtils.substringsBetween(signature, "^", ";");
            if (exceptions != null) {
                exceptionStream = Arrays.stream(exceptions)
                        .filter(s -> s.charAt(0) == 'L')
                        .map(s -> s.substring(1).replace('/', '.'));
            }
        }

        // If the method is not generic or it does not throw a generic exception then it has no exception specification in its generic
        // signature.
        if (signature == null || exceptions == null) {
            ExceptionTable exceptionTable = obj.getExceptionTable();
            if (exceptionTable != null) {
                exceptionStream = Arrays.stream(exceptionTable.getExceptionNames());
            }
        }

        // If the method throws Throwable or Exception because its ancestor throws the same exception then ignore it.
        if (exceptionStream != null) {
            Optional<String> exception = exceptionStream
                    .filter(s -> "java.lang.Exception".equals(s) || "java.lang.Throwable".equals(s))
                    .findAny();
            if (exception.isPresent() && !parentThrows(obj, exception.get())) {
                exceptionThrown = exception.get();
            }
        }

        // If the method's body is empty then we report the bug immediately.
        if (obj.getCode() == null) {
            if (exceptionThrown != null) {
                reportBug("java.lang.Exception".equals(exceptionThrown) ? "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION"
                        : "THROWS_METHOD_THROWS_CLAUSE_THROWABLE", getXMethod());
            }
        }
    }

    @Override
    public void visitAfter(Code obj) {
        // For methods with bodies we report the exception after checking their body.
        if (exceptionThrown != null) {
            reportBug("java.lang.Exception".equals(exceptionThrown) ? "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION"
                    : "THROWS_METHOD_THROWS_CLAUSE_THROWABLE", getXMethod());
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                if ("Ljava/lang/RuntimeException;".equals(item.getSignature())) {
                    reportBug("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", getXMethod());
                }
            }
        } else if (exceptionThrown != null &&
                (seen == Const.INVOKEVIRTUAL ||
                        seen == Const.INVOKEINTERFACE ||
                        seen == Const.INVOKESTATIC)) {

            // If the method throws Throwable or Exception because it invokes another method throwing such
            // exceptions then ignore this bug by resetting exceptionThrown to null.
            XMethod calledMethod = getXMethodOperand();
            if (calledMethod == null) {
                return;
            }

            String[] thrownExceptions = calledMethod.getThrownExceptions();
            if (thrownExceptions != null && Arrays.stream(thrownExceptions)
                    .map(s -> s.replace('/', '.'))
                    .anyMatch(exceptionThrown::equals)) {
                exceptionThrown = null;
            }

        }
    }

    private void reportBug(String bugName, XMethod method) {
        bugReporter.reportBug(new BugInstance(this, bugName, LOW_PRIORITY).addClass(this).addMethod(method));
    }

    private boolean parentThrows(@NonNull Method method, @DottedClassName String exception) {
        return parentThrows(klass, method, exception);
    }

    private boolean parentThrows(@NonNull JavaClass clazz, @NonNull Method method, @DottedClassName String exception) {
        JavaClass ancestor;
        boolean throwsEx = false;
        try {
            ancestor = clazz.getSuperClass();
            if (ancestor != null) {
                Optional<Method> superMethod = Arrays.stream(ancestor.getMethods())
                        .filter(m -> method.getName().equals(m.getName()) && signatureMatches(method, m))
                        .findAny();
                if (superMethod.isPresent()) {
                    throwsEx = Arrays.stream(superMethod.get().getExceptionTable().getExceptionNames())
                            .anyMatch(exception::equals);
                } else {
                    throwsEx = parentThrows(ancestor, method, exception);
                }
            }

            for (JavaClass intf : clazz.getInterfaces()) {
                Optional<Method> superMethod = Arrays.stream(intf.getMethods())
                        .filter(m -> method.getName().equals(m.getName()) && signatureMatches(method, m))
                        .findAny();
                if (superMethod.isPresent()) {
                    throwsEx |= Arrays.stream(superMethod.get().getExceptionTable().getExceptionNames())
                            .anyMatch(exception::equals);
                } else {
                    throwsEx |= parentThrows(intf, method, exception);
                }
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return throwsEx;
    }

    private boolean signatureMatches(Method child, Method parent) {
        String genSig = parent.getGenericSignature();
        if (genSig == null) {
            return child.getSignature().equals(parent.getSignature());
        }

        String sig = child.getSignature();

        SignatureParser genSP = new SignatureParser(genSig);
        SignatureParser sp = new SignatureParser(sig);

        if (genSP.getNumParameters() != sp.getNumParameters()) {
            return false;
        }

        String[] gArgs = genSP.getArguments();
        String[] args = sp.getArguments();

        for (int i = 0; i < sp.getNumParameters(); ++i) {
            if (gArgs[i].charAt(0) == 'T') {
                if (args[i].charAt(0) != 'L') {
                    return false;
                }
            } else {
                if (!gArgs[i].equals(args[i])) {
                    return false;
                }
            }
        }

        String gRet = genSP.getReturnTypeSignature();
        String ret = sp.getReturnTypeSignature();

        return (gRet.charAt(0) == 'T' && ret.charAt(0) == 'L') ||
                gRet.equals(ret);
    }
}
