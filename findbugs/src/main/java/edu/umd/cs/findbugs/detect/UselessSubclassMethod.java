/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

public class UselessSubclassMethod extends BytecodeScanningDetector implements StatelessDetector {

    enum State {
        SEEN_NOTHING, SEEN_PARM, SEEN_LAST_PARM, SEEN_INVOKE, SEEN_RETURN, SEEN_INVALID
    }

    private final BugReporter bugReporter;

    private @DottedClassName String superclassName;

    private State state;

    private int curParm;

    private int curParmOffset;

    private int invokePC;

    private Type[] argTypes;

    private Set<String> interfaceMethods = null;

    public UselessSubclassMethod(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        try {
            JavaClass cls = classContext.getJavaClass();
            superclassName = cls.getSuperclassName();
            JavaClass[] interfaces = null;
            if (cls.isClass() && ((cls.getAccessFlags() & Constants.ACC_ABSTRACT) != 0)) {
                interfaces = cls.getAllInterfaces();
                interfaceMethods = new HashSet<String>();
                for (JavaClass aInterface : interfaces) {
                    Method[] infMethods = aInterface.getMethods();
                    for (Method meth : infMethods) {
                        interfaceMethods.add(meth.getName() + meth.getSignature());
                    }
                }
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
        super.visitClassContext(classContext);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        interfaceMethods = null;
        super.visitAfter(obj);
    }

    @Override
    public void visitMethod(Method obj) {
        if ((interfaceMethods != null) && ((obj.getAccessFlags() & Constants.ACC_ABSTRACT) != 0)) {
            String curDetail = obj.getName() + obj.getSignature();
            for (String infMethodDetail : interfaceMethods) {
                if (curDetail.equals(infMethodDetail)) {
                    bugReporter.reportBug(new BugInstance(this, "USM_USELESS_ABSTRACT_METHOD", LOW_PRIORITY).addClassAndMethod(
                            getClassContext().getJavaClass(), obj));
                }
            }
        }
        super.visitMethod(obj);
    }

    @Override
    public void visitCode(Code obj) {
        try {
            String methodName = getMethodName();

            if (!"<init>".equals(methodName) && !"clone".equals(methodName)
                    && ((getMethod().getAccessFlags() & (Constants.ACC_STATIC | Constants.ACC_SYNTHETIC)) == 0)) {

                /*
                 * for some reason, access flags doesn't return Synthetic, so do
                 * this hocus pocus
                 */
                Attribute[] atts = getMethod().getAttributes();
                for (Attribute att : atts) {
                    if (att.getClass().equals(Synthetic.class)) {
                        return;
                    }
                }

                byte[] codeBytes = obj.getCode();
                if ((codeBytes.length == 0) || (codeBytes[0] != ALOAD_0)) {
                    return;
                }

                state = State.SEEN_NOTHING;
                invokePC = 0;
                super.visitCode(obj);
                if ((state == State.SEEN_RETURN) && (invokePC != 0)) {
                    // Do this check late, as it is potentially expensive
                    Method superMethod = findSuperclassMethod(superclassName, getMethod());
                    if ((superMethod == null) || differentAttributes(getMethod(), superMethod)
                            || getMethod().isProtected()
                            && !samePackage(getDottedClassName(), superclassName)) {
                        return;
                    }

                    bugReporter.reportBug(new BugInstance(this, "USM_USELESS_SUBCLASS_METHOD", LOW_PRIORITY).addClassAndMethod(
                            this).addSourceLine(this, invokePC));
                }
            }
        } catch (ClassNotFoundException cnfe) {
            bugReporter.reportMissingClass(cnfe);
        }
    }

    public String getPackage(@DottedClassName String classname) {
        int i = classname.lastIndexOf('.');
        if (i < 0) {
            return "";
        }
        return classname.substring(0,i);
    }
    public boolean samePackage(@DottedClassName String classname1, @DottedClassName String classname2) {
        return getPackage(classname1).equals(getPackage(classname2));

    }
    @Override
    public void sawOpcode(int seen) {
        switch (state) {
        case SEEN_NOTHING:
            if (seen == ALOAD_0) {
                argTypes = Type.getArgumentTypes(this.getMethodSig());
                curParm = 0;
                curParmOffset = 1;
                if (argTypes.length > 0) {
                    state = State.SEEN_PARM;
                } else {
                    state = State.SEEN_LAST_PARM;
                }
            } else {
                state = State.SEEN_INVALID;
            }
            break;

        case SEEN_PARM:
            if (curParm >= argTypes.length) {
                state = State.SEEN_INVALID;
            } else {
                String signature = argTypes[curParm++].getSignature();
                char typeChar0 = signature.charAt(0);
                if ((typeChar0 == 'L') || (typeChar0 == '[')) {
                    checkParm(seen, ALOAD_0, ALOAD, 1);
                } else if (typeChar0 == 'D') {
                    checkParm(seen, DLOAD_0, DLOAD, 2);
                } else if (typeChar0 == 'F') {
                    checkParm(seen, FLOAD_0, FLOAD, 1);
                } else if (typeChar0 == 'I' || typeChar0 == 'Z' || typeChar0 == 'S' || typeChar0 == 'C') {
                    checkParm(seen, ILOAD_0, ILOAD, 1);
                } else if (typeChar0 == 'J') {
                    checkParm(seen, LLOAD_0, LLOAD, 2);
                } else {
                    state = State.SEEN_INVALID;
                }

                if ((state != State.SEEN_INVALID) && (curParm >= argTypes.length)) {
                    state = State.SEEN_LAST_PARM;
                }

            }
            break;

        case SEEN_LAST_PARM:
            if ((seen == INVOKENONVIRTUAL) && getMethodName().equals(getNameConstantOperand())
                    && getMethodSig().equals(getSigConstantOperand())) {
                invokePC = getPC();
                state = State.SEEN_INVOKE;
            } else {
                state = State.SEEN_INVALID;
            }
            break;

        case SEEN_INVOKE:
            Type returnType = getMethod().getReturnType();
            char retSigChar0 = returnType.getSignature().charAt(0);
            if ((retSigChar0 == 'V') && (seen == RETURN)) {
                state = State.SEEN_RETURN;
            } else if (((retSigChar0 == 'L') || (retSigChar0 == '[')) && (seen == ARETURN)) {
                state = State.SEEN_RETURN;
            } else if ((retSigChar0 == 'D') && (seen == DRETURN)) {
                state = State.SEEN_RETURN;
            } else if ((retSigChar0 == 'F') && (seen == FRETURN)) {
                state = State.SEEN_RETURN;
            } else if ((retSigChar0 == 'I' || retSigChar0 == 'S' || retSigChar0 == 'C' || retSigChar0 == 'B' || retSigChar0 == 'Z')
                    && (seen == IRETURN)) {
                state = State.SEEN_RETURN;
            } else if ((retSigChar0 == 'J') && (seen == LRETURN)) {
                state = State.SEEN_RETURN;
            } else {
                state = State.SEEN_INVALID;
            }
            break;

        case SEEN_RETURN:
            state = State.SEEN_INVALID;
            break;
        default:
            break;
        }
    }

    private void checkParm(int seen, int fastOpBase, int slowOp, int parmSize) {
        if ((curParmOffset >= 1) && (curParmOffset <= 3)) {
            if (seen == (fastOpBase + curParmOffset)) {
                curParmOffset += parmSize;
            } else {
                state = State.SEEN_INVALID;
            }
        } else if (curParmOffset == 0) {
            state = State.SEEN_INVALID;
        } else if ((seen == slowOp) && (getRegisterOperand() == curParmOffset)) {
            curParmOffset += parmSize;
        } else {
            state = State.SEEN_INVALID;
        }
    }

    private Method findSuperclassMethod(@DottedClassName String superclassName, Method subclassMethod) throws ClassNotFoundException {

        String methodName = subclassMethod.getName();
        Type[] subArgs = null;
        JavaClass superClass = Repository.lookupClass(superclassName);
        Method[] methods = superClass.getMethods();
        outer: for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                if (subArgs == null) {
                    subArgs = Type.getArgumentTypes(subclassMethod.getSignature());
                }
                Type[] superArgs = Type.getArgumentTypes(m.getSignature());
                if (subArgs.length == superArgs.length) {
                    for (int j = 0; j < subArgs.length; j++) {
                        if (!superArgs[j].equals(subArgs[j])) {
                            continue outer;
                        }
                    }
                    return m;
                }
            }
        }

        if (!"Object".equals(superclassName)) {
            @DottedClassName String superSuperClassName = superClass.getSuperclassName();
            if (superSuperClassName.equals(superclassName)) {
                throw new ClassNotFoundException("superclass of " + superclassName + " is itself");
            }
            return findSuperclassMethod(superSuperClassName, subclassMethod);
        }

        return null;
    }

    HashSet<String> thrownExceptions(Method m) {
        HashSet<String> result = new HashSet<String>();
        ExceptionTable exceptionTable = m.getExceptionTable();
        if (exceptionTable != null) {
            for (String e : exceptionTable.getExceptionNames()) {
                result.add(e);
            }
        }
        return result;
    }
    private boolean differentAttributes(Method m1, Method m2) {
        if (m1.getAnnotationEntries().length > 0 || m2.getAnnotationEntries().length > 0) {
            return true;
        }
        int access1 = m1.getAccessFlags()
                & (Constants.ACC_PRIVATE | Constants.ACC_PROTECTED | Constants.ACC_PUBLIC | Constants.ACC_FINAL);
        int access2 = m2.getAccessFlags()
                & (Constants.ACC_PRIVATE | Constants.ACC_PROTECTED | Constants.ACC_PUBLIC | Constants.ACC_FINAL);


        m1.getAnnotationEntries();
        if (access1 != access2) {
            return true;
        }
        if (!thrownExceptions(m1).equals(thrownExceptions(m2))) {
            return false;
        }
        return false;
    }
}
