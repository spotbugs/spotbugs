/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugAccumulator;


import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

import java.util.*;

public class IllegalServletOutputOperationAfterCommit extends BytecodeScanningDetector {

    private final ClassDescriptor javaxResponse = DescriptorFactory.createClassDescriptor("javax/servlet/ServletResponse");
    private final ClassDescriptor jakartaResponse = DescriptorFactory.createClassDescriptor("jakarta/servlet/ServletResponse");
    private final ClassDescriptor javaOutputStream = DescriptorFactory.createClassDescriptor("java/io/OutputStream");
    private final ClassDescriptor javaWriter = DescriptorFactory.createClassDescriptor("java/io/Writer");
    ClassDescriptor servletOutJavax = DescriptorFactory.createClassDescriptor("javax/servlet/ServletOutputStream");
    ClassDescriptor servletOutJakarta = DescriptorFactory.createClassDescriptor("jakarta/servlet/ServletOutputStream");

    private final Map<String, Boolean> responseTypeCache = new HashMap<>();
    private final Map<String, Boolean> servletStreamTypeCache = new HashMap<>();
    private final Map<String, Boolean> genericOutTypeCache = new HashMap<>();

    private final BugAccumulator bugAccumulator;

    private final List<int[]> inlineFinallyRanges = new ArrayList<>();

    private Subtypes2 subtypes2 = null;

    private final int STATE_UNCOMMITTED_UNACCESSED = 1;
    private final int STATE_UNCOMMITTED_ACCESSED = 2;
    private final int STATE_COMMITTED_UNACCESSED = 3;
    private final int STATE_COMMITTED_ACCESSED = 4;

    private int currentState = STATE_UNCOMMITTED_UNACCESSED;

    private final Map<Integer, Integer> stateSnapshots = new HashMap<>();
    private boolean isDeadEnd = false;

    public IllegalServletOutputOperationAfterCommit(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        super.visitClassContext(classContext);
    }

    private void setInitialState(Method obj) {

        currentState = STATE_UNCOMMITTED_UNACCESSED;

        boolean hasServletStream = false;
        boolean hasGenericOut = false;
        boolean hasResponse = false;

        for (Type argType : obj.getArgumentTypes()) {
            if (argType instanceof BasicType) {
                continue;
            }
            String className = argType.toString().replace('.', '/');
            if (className.contains("[]")) {
                continue;
            }

            if (isResponseSafe(className)) {
                hasResponse = true;
            } else if (isServletStreamSafe(className)) {
                hasServletStream = true;
            } else if (isGenericOutSafe(className)) {
                hasGenericOut = true;
            }
        }

        if (hasServletStream || (hasGenericOut && hasResponse)) {
            currentState = STATE_UNCOMMITTED_ACCESSED;
        }
    }

    @Override
    public void visitMethod(Method obj) {

        setInitialState(obj);

        stateSnapshots.clear();
        isDeadEnd = false;

        inlineFinallyRanges.clear();

        org.apache.bcel.classfile.Code code = obj.getCode();
        if (code != null) {
            CodeException[] exceptions = code.getExceptionTable();
            if (exceptions != null) {
                Map<Integer, List<CodeException>> finallyProtectedRegions = new HashMap<>();
                for (CodeException e : exceptions) {
                    if (e.getCatchType() == 0) {
                        finallyProtectedRegions.computeIfAbsent(e.getHandlerPC(), k -> new ArrayList<>()).add(e);
                    }
                }

                for (Map.Entry<Integer, List<CodeException>> entry : finallyProtectedRegions.entrySet()) {
                    int handlerPc = entry.getKey();
                    List<CodeException> regions = entry.getValue();

                    regions.sort(Comparator.comparingInt(CodeException::getStartPC));

                    int currentPc = -1;
                    for (CodeException region : regions) {
                        if (currentPc != -1 && currentPc < region.getStartPC()) {
                            inlineFinallyRanges.add(new int[] { currentPc, region.getStartPC() });
                        }

                        if (currentPc < region.getEndPC()) {
                            currentPc = region.getEndPC();
                        }
                    }

                    if (currentPc != -1 && currentPc < handlerPc) {
                        inlineFinallyRanges.add(new int[] { currentPc, handlerPc });
                    }
                }
            }
        }

        super.visitMethod(obj);
    }

    private boolean isInsideInlineFinallyCopy(int pc) {
        for (int[] range : inlineFinallyRanges) {
            if (pc >= range[0] && pc < range[1]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void sawOpcode(int seen) {

        if (isInsideInlineFinallyCopy(getPC())) {
            return;
        }

        if (isDeadEnd && stateSnapshots.containsKey(getPC())) {
            currentState = stateSnapshots.get(getPC());
        }

        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESPECIAL) {
            String className = getClassConstantOperand();
            String methodName = getNameConstantOperand();

            boolean isResponse = isResponseSafe(className);
            boolean isStreamOrWriter = isOutSafe(className);

            if (!isResponse && !isStreamOrWriter) {
                return;
            }

            boolean isFlushOrClose = "flush".equals(methodName) || "close".equals(methodName);

            if (isFlushOrClose && !isStreamOrWriter) {
                return;
            }

            if (!isFlushOrClose && !isResponse) {
                return;
            }

            checkAndReport(methodName);

        }

        if (isIf(seen) || isSwitch(seen) || seen == Const.GOTO || seen == Const.GOTO_W) {
            int target = getBranchTarget();
            if (target > getPC()) {
                stateSnapshots.put(target, currentState);
            }
        }

        isDeadEnd = isReturn(seen);
    }

    private void checkAndReport(String methodName) {
        if (!isValidStateChange(methodName)) {

            int priority = NORMAL_PRIORITY;
            String msg = "";

            switch (currentState) {
            case STATE_UNCOMMITTED_ACCESSED:
                msg = "sends error or redirects after initializing the output builder object";
                break;
            case STATE_COMMITTED_UNACCESSED:
            case STATE_COMMITTED_ACCESSED:
                if (methodName.equals("flush") || methodName.equals("flushBuffer")) {
                    priority = LOW_PRIORITY;
                    msg = "flushes an already flushed buffer";
                } else {
                    msg = "performs illegal servlet response operation after commit";
                }
                break;
            }
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "ISOOAC_ILLEGAL_SERVLET_OUTPUT_OPERATION_AFTER_COMMIT", priority)
                    .addClassAndMethod(this).addCalledMethod(getXMethodOperand())
                    .addString(msg)
                    .addSourceLine(this), this);

        }
    }

    private boolean isValidStateChange(String methodName) {

        switch (currentState) {

        case STATE_UNCOMMITTED_UNACCESSED:
            if (isStreamOrWriterGetter(methodName)) {
                currentState = STATE_UNCOMMITTED_ACCESSED;
            } else if ("flushBuffer".equals(methodName) || isErrorOrRedirect(methodName)) {
                currentState = STATE_COMMITTED_UNACCESSED;
            }
            return true;

        case STATE_UNCOMMITTED_ACCESSED:
            if (isErrorOrRedirect(methodName)) {
                return false;
            } else if ("flushBuffer".equals(methodName) || "flush".equals(methodName) || "close".equals(methodName)) {
                currentState = STATE_COMMITTED_ACCESSED;
            }
            return true;

        case STATE_COMMITTED_UNACCESSED:
            if (isRelaxedIllegalPostCommitResponseMethod(methodName) || "flushBuffer".equals(methodName)) {
                return false;
            } else if (isStreamOrWriterGetter(methodName)) {
                currentState = STATE_UNCOMMITTED_ACCESSED;
            }
            return true;

        case STATE_COMMITTED_ACCESSED:
            if (isStrictIllegalPostCommitResponseMethod(methodName)) {
                return false;
            } else if ("flushBuffer".equals(methodName) || "flush".equals(methodName)) {
                return false;
            }
            return true;

        default:
            return true;
        }
    }

    private boolean isResponseSafe(String className) {
        if ("javax/servlet/ServletResponse".equals(className) ||
                "jakarta/servlet/ServletResponse".equals(className) ||
                "javax/servlet/http/HttpServletResponse".equals(className) ||
                "jakarta/servlet/http/HttpServletResponse".equals(className) ||
                "javax/servlet/ServletResponseWrapper".equals(className) ||
                "jakarta/servlet/ServletResponseWrapper".equals(className) ||
                "javax/servlet/http/HttpServletResponseWrapper".equals(className) ||
                "jakarta/servlet/http/HttpServletResponseWrapper".equals(className)) {
            return true;
        }

        return responseTypeCache.computeIfAbsent(className,
                k -> isSubtypeSafe(k, javaxResponse, jakartaResponse));
    }

    private boolean isServletStreamSafe(String className) {
        if ("javax/servlet/ServletOutputStream".equals(className) ||
                "jakarta/servlet/ServletOutputStream".equals(className)) {
            return true;
        }
        return servletStreamTypeCache.computeIfAbsent(className,
                k -> isSubtypeSafe(k, servletOutJavax, servletOutJakarta));
    }

    private boolean isGenericOutSafe(String className) {
        if ("java/io/OutputStream".equals(className) ||
                "java/io/Writer".equals(className) ||
                "java/io/PrintWriter".equals(className)) {
            return true;
        }
        return genericOutTypeCache.computeIfAbsent(className,
                k -> isSubtypeSafe(k, javaOutputStream, javaWriter));
    }

    private boolean isOutSafe(String className) {
        return isServletStreamSafe(className) || isGenericOutSafe(className);
    }

    private boolean isSubtypeSafe(String targetClassName, ClassDescriptor... allowedTypes) {
        ClassDescriptor targetClass = DescriptorFactory.createClassDescriptor(targetClassName);
        try {
            for (ClassDescriptor allowed : allowedTypes) {
                if (subtypes2.isSubtype(targetClass, allowed)) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
        return false;
    }

    private boolean isStrictIllegalPostCommitResponseMethod(String methodName) {
        return isStreamOrWriterGetter(methodName) ||
                isRelaxedIllegalPostCommitResponseMethod(methodName) ||
                isErrorOrRedirect(methodName);
    }

    private boolean isRelaxedIllegalPostCommitResponseMethod(String methodName) {
        return "setContentType".equals(methodName) ||
                "setCharacterEncoding".equals(methodName) ||
                "setBufferSize".equals(methodName) ||
                "reset".equals(methodName) ||
                "resetBuffer".equals(methodName) ||
                "addHeader".equals(methodName) ||
                "addIntHeader".equals(methodName) ||
                "addDateHeader".equals(methodName) ||
                "addCookie".equals(methodName) ||
                "setHeader".equals(methodName) ||
                "setIntHeader".equals(methodName) ||
                "setStatus".equals(methodName) ||
                "setDateHeader".equals(methodName) ||
                "setContentLength".equals(methodName) ||
                "setContentLengthLong".equals(methodName) ||
                "setLocale".equals(methodName);
    }

    private boolean isStreamOrWriterGetter(String methodName) {
        return "getOutputStream".equals(methodName) ||
                "getWriter".equals(methodName);
    }

    private boolean isErrorOrRedirect(String methodName) {
        return "sendError".equals(methodName) ||
                "sendRedirect".equals(methodName);
    }

}
