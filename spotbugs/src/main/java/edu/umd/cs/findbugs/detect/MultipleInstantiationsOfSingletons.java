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
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.PruneUnconditionalExceptionThrowerEdges;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.AvailableLoad;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultipleInstantiationsOfSingletons extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    private JavaClass cloneableInterface;
    private JavaClass serializableInterface;

    private boolean hasSingletonPostFix;

    private boolean isCloneable;
    private boolean implementsCloneableDirectly;
    private XMethod cloneMethod;
    private boolean cloneOnlyThrowsException;
    private boolean cloneOnlyThrowsCloneNotSupportedException;

    private boolean isSerializable;
    private boolean isInstanceAssignOk;
    private boolean hasNoFactoryMethod;
    private boolean isInstanceFieldLazilyInitialized;
    private XField instanceField;
    private final Set<XField> eagerlyInitializedFields = new HashSet<>();
    private final Map<XField, XMethod> instanceGetterMethods = new HashMap<>();
    private final List<XMethod> methodsUsingMonitor = new ArrayList<>();
    private final Map<XMethod, List<XMethod>> calledMethodsByMethods = new HashMap<>();

    public MultipleInstantiationsOfSingletons(BugReporter bugReporter) {
        this.bugReporter = bugReporter;

        try {
            cloneableInterface = Repository.getInterfaces("java.lang.Cloneable")[0];
            serializableInterface = Repository.getInterfaces("java.io.Serializable")[0];
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
    }

    @Override
    public void visit(JavaClass obj) {
        hasSingletonPostFix = false;

        isCloneable = false;
        implementsCloneableDirectly = false;
        cloneOnlyThrowsException = false;
        cloneOnlyThrowsCloneNotSupportedException = false;

        isSerializable = false;
        isInstanceAssignOk = false;
        hasNoFactoryMethod = true;
        isInstanceFieldLazilyInitialized = false;
        instanceField = null;
        cloneMethod = null;

        eagerlyInitializedFields.clear();
        instanceGetterMethods.clear();
        methodsUsingMonitor.clear();
        calledMethodsByMethods.clear();

        if (obj.getClassName().endsWith("Singleton")) {
            hasSingletonPostFix = true;
        }

        // Does this class directly implement Cloneable or Serializable?
        try {
            JavaClass[] interfaces = obj.getAllInterfaces();
            isCloneable = java.util.stream.Stream.of(interfaces).anyMatch(i -> i.equals(cloneableInterface));
            isSerializable = java.util.stream.Stream.of(interfaces).anyMatch(i -> i.equals(serializableInterface));

            implementsCloneableDirectly = java.util.stream.Stream.of(obj.getInterfaces()).anyMatch(i -> i.equals(cloneableInterface));
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }

        super.visit(obj);
    }

    @Override
    public void visit(Method obj) {
        if ("clone".equals(getMethodName()) && "()Ljava/lang/Object;".equals(getMethodSig())) {
            cloneOnlyThrowsException = PruneUnconditionalExceptionThrowerEdges.doesMethodUnconditionallyThrowException(getXMethod());
            cloneMethod = getXMethod();
        }

        super.visit(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.PUTSTATIC) {
            XField field = getXFieldOperand();
            if (isInstanceField(field, getClassName())) {
                if (Const.STATIC_INITIALIZER_NAME.equals(getMethodName())) {
                    eagerlyInitializedFields.add(field);
                }
                if (stack.getStackDepth() > 0) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    XMethod calledMethod = item.getReturnValueOf();
                    if (calledMethod != null && Const.CONSTRUCTOR_NAME.equals(calledMethod.getName())
                            && calledMethod.getClassName().equals(getDottedClassName())) {
                        isInstanceAssignOk = true;
                        instanceField = field;

                        try {
                            ValueNumberDataflow vnaDataflow = getClassContext().getValueNumberDataflow(getMethod());
                            IsNullValueDataflow invDataflow = getClassContext().getIsNullValueDataflow(getMethod());
                            ValueNumberFrame vFrame = vnaDataflow.getAnalysis().getFactAtPC(vnaDataflow.getCFG(), getPC());
                            IsNullValueFrame iFrame = invDataflow.getAnalysis().getFactAtPC(invDataflow.getCFG(), getPC());
                            AvailableLoad l = new AvailableLoad(field);
                            ValueNumber[] availableLoads = vFrame.getAvailableLoad(l);
                            if (availableLoads != null && iFrame.isTrackValueNumbers()) {
                                for (ValueNumber v : availableLoads) {
                                    IsNullValue knownValue = iFrame.getKnownValue(v);
                                    if (knownValue != null && knownValue.isDefinitelyNull()) {
                                        isInstanceFieldLazilyInitialized = true;
                                    }
                                }
                            }

                        } catch (DataflowAnalysisException | CFGBuilderException e) {
                            bugReporter.logError(String.format("Detector %s caught an exception while analyzing %s.",
                                    this.getClass().getName(), getClassContext().getJavaClass().getClassName()), e);
                        }
                    }
                }
            }
        } else if (seen == Const.ATHROW && "clone".equals(getMethodName()) && stack.getStackDepth() > 0) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null && "Ljava/lang/CloneNotSupportedException;".equals(item.getSignature()) && cloneOnlyThrowsException) {
                cloneOnlyThrowsCloneNotSupportedException = true;
            }
        } else if (seen == Const.ARETURN && stack.getStackDepth() > 0) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XMethod method = getXMethod();
            XField field = item.getXField();
            if (isInstanceField(field, getClassName()) && method.isPublic() && method.isStatic()) {
                instanceGetterMethods.put(field, method);
            } else {
                XMethod calledMethod = item.getReturnValueOf();
                SignatureParser parser = new SignatureParser(getMethodSig());
                String calledMethodReturnType = ClassName.fromFieldSignature(parser.getReturnTypeSignature());

                if (calledMethod != null && Const.CONSTRUCTOR_NAME.equals(calledMethod.getName())
                        && calledMethod.getClassName().equals(getDottedClassName())
                        && !method.isPrivate() && method.isStatic() && getClassName().equals(calledMethodReturnType)
                        && !Const.CONSTRUCTOR_NAME.equals(getMethodName()) && !Const.STATIC_INITIALIZER_NAME.equals(getMethodName())
                        && !("clone".equals(getMethodName()) && "()Ljava/lang/Object;".equals(getMethodSig()))) {
                    hasNoFactoryMethod = false;
                }
            }
        } else if (seen == Const.MONITORENTER) {
            methodsUsingMonitor.add(getXMethod());
        } else if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESPECIAL || seen == Const.INVOKESTATIC || seen == Const.INVOKEINTERFACE) {
            if (!calledMethodsByMethods.containsKey(getXMethod())) {
                calledMethodsByMethods.put(getXMethod(), new ArrayList<>());
            }
            calledMethodsByMethods.get(getXMethod()).add(getXMethodOperand());
        }
    }

    private boolean isInstanceField(XField field, String clsName) {
        String className = "L" + clsName + ";";
        return field != null && field.isPrivate() && field.isStatic() && className.equals(field.getSignature());
    }

    @Override
    public void visitAfter(JavaClass javaClass) {
        XMethod instanceGetterMethod = instanceGetterMethods.get(instanceField);
        boolean isInstanceFieldEagerlyInitialized = eagerlyInitializedFields.contains(instanceField);

        // a class is considered a singleton, if
        // - the user clearly indicated, that it's intended to be a singleton
        // OR
        // - it can be instantiated (not abstract, not interface)
        // - the instance field is assigned the contained class
        // - has no factory method (which returns a new instance and is not a constructor)
        // - has an instance getter method
        // - the instance field is either eagerly or lazily initialized

        if (!(hasSingletonPostFix
                || (!javaClass.isAbstract() && !javaClass.isInterface() && !javaClass.isRecord()
                        && isInstanceAssignOk
                        && hasNoFactoryMethod
                        && instanceGetterMethod != null
                        && (isInstanceFieldEagerlyInitialized || isInstanceFieldLazilyInitialized)))) {
            return;
        }

        for (Method m : javaClass.getMethods()) {
            if (Const.CONSTRUCTOR_NAME.equals(m.getName()) && !m.isPrivate()) {
                bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", NORMAL_PRIORITY)
                        .addClass(this)
                        .addMethod(javaClass, m));

                break;
            }
        }

        if (instanceGetterMethod != null && !hasSynchronized(instanceGetterMethod) && isInstanceFieldLazilyInitialized) {
            bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", NORMAL_PRIORITY).addClass(this)
                    .addMethod(instanceGetterMethod));
        }

        if (!cloneOnlyThrowsCloneNotSupportedException) { // no or not only CloneNotSupportedException
            if (isCloneable) {
                if (implementsCloneableDirectly) { // directly
                    bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_CLONEABLE", NORMAL_PRIORITY).addClass(this)
                            .addMethod(cloneMethod));
                } else { // indirectly
                    bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE", NORMAL_PRIORITY).addClass(this));
                }
            } else if (cloneMethod != null) {
                bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", NORMAL_PRIORITY).addClass(this)
                        .addMethod(cloneMethod));
            }
        }

        if (isSerializable) {
            if (javaClass.isEnum()) {
                int numberOfEnumValues = getNumberOfEnumValues(javaClass);
                if (numberOfEnumValues > 1) {
                    bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", NORMAL_PRIORITY).addClass(this));
                }
            } else {
                bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_SERIALIZABLE", NORMAL_PRIORITY).addClass(this));
            }
        }

        super.visitAfter(javaClass);
    }

    private boolean hasSynchronized(XMethod method) {
        if (method.isSynchronized() || methodsUsingMonitor.contains(method)) {
            return true;
        }

        if (calledMethodsByMethods.containsKey(method)) {
            List<XMethod> calledMethods = calledMethodsByMethods.get(method);
            for (XMethod cm : calledMethods) {
                if (hasSynchronized(cm)) {
                    return true;
                }
            }
        }

        return false;
    }

    private int getNumberOfEnumValues(JavaClass javaClass) {
        try {
            Class<?> clazz = Class.forName(javaClass.getClassName());
            java.lang.reflect.Method valuesMethod = clazz.getDeclaredMethod("values");
            Object[] result = (Object[]) valuesMethod.invoke(null);
            return result.length;
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            bugReporter.logError(String.format("Detector %s caught an exception while determining the number of enum values of %s.",
                    this.getClass().getName(), javaClass.getClassName()), e);
        }
        return 0;
    }
}
