package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.OpcodeStack;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.PruneUnconditionalExceptionThrowerEdges;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

import java.util.Map;
import java.util.HashMap;

public class MultipleInstantiationsOfSingletons extends OpcodeStackDetector {

    private enum Methods {
        CONSTRUCTOR,
        CLONE,
        INSTANCE_GETTER
    };

    private final BugReporter bugReporter;
    private final ClassDescriptor cloneDescriptor = DescriptorFactory.createClassDescriptor(java.lang.Cloneable.class);
    private final ClassDescriptor serializableDescriptor = DescriptorFactory.createClassDescriptor(java.io.Serializable.class);

    boolean isSingleton;
    boolean isConstructorPrivate;
    boolean isGetterMethodSynchronized;

    boolean isCloneable;
    boolean implementsCloneableDirectly;
    boolean hasCloneMethod;
    boolean cloneOnlyThrowsException;
    boolean cloneOnlyThrowsCloneNotSupportedException;

    boolean isSerializable;

    Map<Methods, XMethod> methods;
    
    public MultipleInstantiationsOfSingletons(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(JavaClass obj) {
        isSingleton = false;
        isConstructorPrivate = false;
        isGetterMethodSynchronized = false;

        isCloneable = false;
        implementsCloneableDirectly = false;
        hasCloneMethod = false;
        cloneOnlyThrowsException = false;
        cloneOnlyThrowsCloneNotSupportedException = false;

        isSerializable = false;

        methods = new HashMap<>();

        if (obj.getClassName().endsWith("Singleton")) {
            isSingleton = true;
        }

        // Does this class directly implement Cloneable or Serializable?
        String[] interface_names = obj.getInterfaceNames();
        for (String interface_name : interface_names) {
            if ("java.lang.Cloneable".equals(interface_name)) {
                isCloneable = true;
                implementsCloneableDirectly = true;
            }
            if ("java.lang.Serializable".equals(interface_name)) {
                isSerializable = true;
            }
        }

        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        try {
            if (subtypes2.isSubtype(getClassDescriptor(), cloneDescriptor)) {
                isCloneable = true;
            }
            if (subtypes2.isSubtype(DescriptorFactory.createClassDescriptorFromDottedClassName(obj.getSuperclassName()), cloneDescriptor)) {
                implementsCloneableDirectly = false;
            }

            if (subtypes2.isSubtype(getClassDescriptor(), serializableDescriptor)) {
                isSerializable = true;
            }

        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }

        super.visit(obj);
    }

    @Override
    public void visit(Method obj) {
        if (obj.getName().equals("clone")) {
            cloneOnlyThrowsException = PruneUnconditionalExceptionThrowerEdges.doesMethodUnconditionallyThrowException(XFactory.createXMethod(this));
            methods.put(Methods.CLONE, getXMethod());
            hasCloneMethod = true;
        }

        super.visit(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        if (Const.CONSTRUCTOR_NAME.equals(getMethod().getName())) {
            methods.put(Methods.CONSTRUCTOR, getXMethod());
            if (getMethod().isPrivate()) {
                isConstructorPrivate = true;
            } else {
                isConstructorPrivate = false;
            }
        }

        if (seen == Const.ATHROW) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item != null) {
                if (item.getSignature().equals("Ljava/lang/CloneNotSupportedException;") && cloneOnlyThrowsException) {
                    cloneOnlyThrowsCloneNotSupportedException = true;
                }
            }
        }

        if (seen == Const.ARETURN) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = item.getXField();
            if (field != null) {
                String className = "L" + getClassName() + ";";
                if (field.isPrivate() && field.isStatic() && field.getSignature().equals(className)) {
                    isSingleton = true;
                    isGetterMethodSynchronized = getMethod().isSynchronized();
                    methods.put(Methods.INSTANCE_GETTER, getXMethod());
                }
            }
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (!isSingleton) {
            return;
        }

        if (!isConstructorPrivate) {
            bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", NORMAL_PRIORITY).addClass(this).addMethod(
                    methods.get(Methods.CONSTRUCTOR)));
        }

        if (!isGetterMethodSynchronized) {
            bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", NORMAL_PRIORITY).addClass(this).addMethod(
                    methods.get(Methods.INSTANCE_GETTER)));
        }

        if (isCloneable) {
            if (implementsCloneableDirectly) { // directly
                bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_CLONEABLE", NORMAL_PRIORITY).addClass(this).addMethod(methods
                        .get(Methods.CLONE)));
            } else { // indirectly
                if (!cloneOnlyThrowsCloneNotSupportedException) { // no or not only CloneNotSupportedException
                    bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE", NORMAL_PRIORITY).addClass(this)
                            .addMethod(methods.get(Methods.CLONE)));
                }
            }
        } else if (hasCloneMethod) {
            bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", NORMAL_PRIORITY).addClass(this).addMethod(methods
                    .get(Methods.CLONE)));
        }

        if (isSerializable) {
            bugReporter.reportBug(new BugInstance(this, "SING_SINGLETON_IMPLEMENTS_SERIALIZEABLE", NORMAL_PRIORITY).addClass(this).addMethod(
                    methods.get(Methods.CONSTRUCTOR)));
        }

        super.visitAfter(obj);
    }
}
