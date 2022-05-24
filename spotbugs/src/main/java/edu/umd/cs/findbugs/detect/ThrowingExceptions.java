package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

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

        String signature = obj.getGenericSignature();
        if (signature != null) {
            String[] exceptions = StringUtils.substringsBetween(signature, "^", ";");
            if (exceptions != null) {
                exceptionStream = Arrays.stream(exceptions)
                        .filter(s -> s.charAt(0) == 'L')
                        .map(s -> s.substring(1).replace('/', '.'));
            }
        } else {
            ExceptionTable exceptionTable = obj.getExceptionTable();
            if (exceptionTable != null) {
                exceptionStream = Arrays.stream(exceptionTable.getExceptionNames());
            }
        }

        if (exceptionStream != null) {
            Optional<String> exception = exceptionStream
                    .filter(s -> "java.lang.Exception".equals(s) || "java.lang.Throwable".equals(s))
                    .findAny();
            if (exception.isPresent() && !parentThrows(obj, exception.get())) {
                exceptionThrown = exception.get();
            }
        }

        if (obj.getCode() == null) {
            if (exceptionThrown != null) {
                reportBug("java.lang.Exception".equals(exceptionThrown) ? "THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION"
                        : "THROWS_METHOD_THROWS_CLAUSE_THROWABLE", getXMethod());
            }
        }
    }

    @Override
    public void visitAfter(Code obj) {
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
                seen == Const.INVOKEVIRTUAL ||
                seen == Const.INVOKEINTERFACE ||
                seen == Const.INVOKESTATIC) {
            XMethod calledMethod = getXMethodOperand();
            if (calledMethod == null) {
                return;
            }

            String[] thrownExceptions = calledMethod.getThrownExceptions();
            if (thrownExceptions != null && Arrays.stream(thrownExceptions)
                    .anyMatch(s -> s.replace('/', '.').equals(exceptionThrown))) {
                exceptionThrown = null;
            }

        }
    }

    private void reportBug(String bugName, XMethod method) {
        bugReporter.reportBug(new BugInstance(this, bugName, LOW_PRIORITY).addClass(this).addMethod(method));
    }

    private boolean parentThrows(Method method, String exception) {
        return parentThrows(klass, method, exception);
    }

    private boolean parentThrows(JavaClass clazz, Method method, String exception) {
        JavaClass ancestor;
        boolean throwsEx = false;
        try {
            ancestor = clazz.getSuperClass();
            if (ancestor != null) {
                Optional<Method> superMethod = Arrays.stream(ancestor.getMethods())
                        .filter(m -> method.getName().equals(m.getName()) && sameSignature(method, m))
                        .findAny();
                if (superMethod.isPresent()) {
                    throwsEx = Arrays.stream(superMethod.get().getExceptionTable().getExceptionNames())
                            .anyMatch(s -> exception.equals(s));
                } else {
                    throwsEx = parentThrows(ancestor, method, exception);
                }
            }

            for (JavaClass intf : clazz.getInterfaces()) {
                Optional<Method> superMethod = Arrays.stream(intf.getMethods())
                        .filter(m -> method.getName().equals(m.getName()) && sameSignature(method, m))
                        .findAny();
                if (superMethod.isPresent()) {
                    throwsEx |= Arrays.stream(superMethod.get().getExceptionTable().getExceptionNames())
                            .anyMatch(s -> exception.equals(s));
                } else {
                    throwsEx |= parentThrows(intf, method, exception);
                }
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return throwsEx;
    }

    private boolean sameSignature(Method child, Method parent) {
        String genSig = parent.getGenericSignature();
        if (genSig == null) {
            return child.getSignature().equals(parent.getSignature());
        }

        String sig = child.getSignature();
        int genIndex = 1;
        char genSigChar = genSig.charAt(genIndex);
        int index = 1;
        char sigChar = sig.charAt(index);
        while (genSigChar != ')' && sigChar != ')') {
            switch (genSigChar) {
            default:
                if (sigChar != genSigChar) {
                    return false;
                }
                ++index;
                ++genIndex;
                break;
            case 'L':
                int genSigObjEnd = genSig.indexOf(';', genIndex);
                int sigObjEnd = sig.indexOf(';', index);
                if (!genSig.substring(genIndex, genSigObjEnd - genIndex)
                        .equals(sig.substring(index, sigObjEnd - index))) {
                    return false;
                }
                genIndex = genSigObjEnd + 1;
                index = sigObjEnd + 1;
                break;
            case 'T':
                if (sigChar != 'L') {
                    return false;
                }
                genSigObjEnd = genSig.indexOf(';', genIndex);
                sigObjEnd = sig.indexOf(';', index);
                genIndex = genSigObjEnd + 1;
                index = sigObjEnd + 1;
                break;
            }
            genSigChar = genSig.charAt(genIndex);
            sigChar = sig.charAt(index);
        }

        if (sigChar != genSigChar) {
            return false;
        }

        genSigChar = genSig.charAt(++genIndex);
        sigChar = sig.charAt(++index);

        if (genSigChar == 'T') {
            return sigChar == 'L';
        } else {
            return genSig.substring(genIndex).equals(sig.substring(index));
        }
    }
}
