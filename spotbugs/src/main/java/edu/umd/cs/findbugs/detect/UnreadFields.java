/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AccessibleEntity;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.AvailableLoad;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.Bag;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.Util;
import edu.umd.cs.findbugs.util.Values;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class UnreadFields extends OpcodeStackDetector {
    private static final boolean DEBUG = SystemProperties.getBoolean("unreadfields.debug");

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#isContainerField(XField)} instead
     */
    @Deprecated
    public boolean isContainerField(XField f) {
        return data.isContainerField(f);
    }



    boolean hasNativeMethods;

    boolean isSerializable;

    boolean sawSelfCallInConstructor;

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    boolean publicOrProtectedConstructor;

    private final Map<String, List<BugAnnotation>> anonymousClassAnnotation = new HashMap<>();

    private final ClassDescriptor junitNestedAnnotation = DescriptorFactory.createClassDescriptor("org/junit/jupiter/api/Nested");

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#getReadFields()} instead
     */
    @Deprecated
    public Set<? extends XField> getReadFields() {
        return data.getReadFields();
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#getWrittenFields()} instead
     */
    @Deprecated
    public Set<? extends XField> getWrittenFields() {
        return data.getWrittenFields();
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#isWrittenOutsideOfInitialization(XField)} instead
     */
    @Deprecated
    public boolean isWrittenOutsideOfInitialization(XField f) {
        return data.isWrittenOutsideOfInitialization(f);
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#isWrittenDuringInitialization(XField)} instead
     */
    @Deprecated
    public boolean isWrittenDuringInitialization(XField f) {
        return data.isWrittenDuringInitialization(f);
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#isWrittenInConstructor(XField)} instead
     */
    @Deprecated
    public boolean isWrittenInConstructor(XField f) {
        return data.isWrittenInConstructor(f);
    }

    static final int DO_NOT_CONSIDER = Const.ACC_PUBLIC | Const.ACC_PROTECTED;

    final ClassDescriptor externalizable = DescriptorFactory.createClassDescriptor(java.io.Externalizable.class);

    final ClassDescriptor serializable = DescriptorFactory.createClassDescriptor(java.io.Serializable.class);

    final ClassDescriptor remote = DescriptorFactory.createClassDescriptor(java.rmi.Remote.class);

    public UnreadFields(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        AnalysisContext context = AnalysisContext.currentAnalysisContext();
        data.reflectiveFields.add(XFactory.createXField("java.lang.System", "in", "Ljava/io/InputStream;", true));
        data.reflectiveFields.add(XFactory.createXField("java.lang.System", "out", "Ljava/io/PrintStream;", true));
        data.reflectiveFields.add(XFactory.createXField("java.lang.System", "err", "Ljava/io/PrintStream;", true));
        data = context.getUnreadFieldsData();
        context.setUnreadFields(this);
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#strongEvidenceForIntendedSerialization(ClassDescriptor)} instead
     */
    @Deprecated
    public void strongEvidenceForIntendedSerialization(ClassDescriptor c) {
        data.strongEvidenceForIntendedSerialization(c);
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#existsStrongEvidenceForIntendedSerialization(ClassDescriptor)} instead
     */
    @Deprecated
    public boolean existsStrongEvidenceForIntendedSerialization(ClassDescriptor c) {
        return data.existsStrongEvidenceForIntendedSerialization(c);
    }

    @Override
    public void visit(JavaClass obj) {
        data.calledFromConstructors.clear();
        hasNativeMethods = false;
        sawSelfCallInConstructor = false;
        publicOrProtectedConstructor = false;
        isSerializable = false;
        if (obj.isAbstract()) {
            data.abstractClasses.add(getDottedClassName());
        } else {
            String superClass = obj.getSuperclassName();
            if (superClass != null) {
                data.hasNonAbstractSubClass.add(superClass);
            }
        }
        data.classesScanned.add(getDottedClassName());
        boolean superClassIsObject = Values.DOTTED_JAVA_LANG_OBJECT.equals(obj.getSuperclassName());
        if (getSuperclassName().indexOf('$') >= 0 || getSuperclassName().indexOf('+') >= 0
                || withinAnonymousClass.matcher(getDottedClassName()).find()) {
            // System.out.println("hicfsc: " + betterClassName);
            data.innerClassCannotBeStatic.add(getDottedClassName());
            // System.out.println("hicfsc: " + betterSuperclassName);
            data.innerClassCannotBeStatic.add(getDottedSuperclassName());
        }
        if (getXClass().getAnnotation(junitNestedAnnotation) != null) {
            // This class is a JUnit nested test, it can't be static
            data.innerClassCannotBeStatic.add(getDottedClassName());
        }
        // Does this class directly implement Serializable?
        String[] interface_names = obj.getInterfaceNames();
        for (String interface_name : interface_names) {
            if ("java.io.Externalizable".equals(interface_name)) {
                isSerializable = true;
            } else if ("java.io.Serializable".equals(interface_name)) {
                isSerializable = true;
                break;
            }
        }

        // Does this class indirectly implement Serializable?
        if ((!superClassIsObject || interface_names.length > 0) && !isSerializable) {
            try {
                Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
                ClassDescriptor desc = DescriptorFactory.createClassDescriptor(obj);
                if (subtypes2.getSubtypes(serializable).contains(desc) || subtypes2.getSubtypes(externalizable).contains(desc)
                        || subtypes2.getSubtypes(remote).contains(desc)) {
                    isSerializable = true;
                }
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }
        }

        // System.out.println(getDottedClassName() + " is serializable: " +
        // isSerializable);
        super.visit(obj);
    }

    public static boolean classHasParameter(JavaClass obj) {
        for (Attribute a : obj.getAttributes()) {
            if (a instanceof Signature) {
                String sig = ((Signature) a).getSignature();
                return sig.charAt(0) == '<';
            }
        }
        return false;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (hasNativeMethods) {
            data.fieldsOfSerializableOrNativeClassed.addAll(data.myFields);
            data.fieldsOfNativeClasses.addAll(data.myFields);
        }
        if (isSerializable) {
            data.fieldsOfSerializableOrNativeClassed.addAll(data.myFields);
        }
        if (sawSelfCallInConstructor) {
            data.myFields.removeAll(data.writtenInConstructorFields);
            data.writtenInInitializationFields.addAll(data.myFields);
        }
        data.myFields.clear();
        data.allMyFields.clear();
        data.calledFromConstructors.clear();
    }

    @Override
    public void visit(Field obj) {
        super.visit(obj);
        XField f = XFactory.createXField(this);
        data.allMyFields.add(f);
        String signature = obj.getSignature();
        if (!"serialVersionUID".equals(getFieldName())) {

            data.myFields.add(f);
            if ("_jspx_dependants".equals(obj.getName())) {
                data.containerFields.add(f);
            }
        }
        if (isSeleniumWebElement(signature)) {
            data.containerFields.add(f);
        }
    }

    public static boolean isSeleniumWebElement(String signature) {
        return "Lorg/openqa/selenium/RenderedWebElement;".equals(signature)
                || "Lorg/openqa/selenium/WebElement;".equals(signature);
    }

    @Override
    public void visitAnnotation(String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {
        if (!visitingField()) {
            return;
        }
        if (isInjectionAttribute(annotationClass)) {
            data.containerFields.add(XFactory.createXField(this));
        }
        if (!annotationClass.startsWith("edu.umd.cs.findbugs") && !annotationClass.startsWith("javax.lang")) {
            data.unknownAnnotation.add(XFactory.createXField(this), annotationClass);
        }

    }

    public static boolean isInjectionAttribute(@DottedClassName String annotationClass) {
        if (annotationClass.startsWith("javax.annotation.")
                || annotationClass.startsWith("jakarta.annotation.")
                || annotationClass.startsWith("javax.ejb")
                || annotationClass.startsWith("jakarta.ejb")
                || "org.apache.tapestry5.annotations.Persist".equals(annotationClass)
                || "org.jboss.seam.annotations.In".equals(annotationClass)
                || annotationClass.startsWith("javax.persistence")
                || annotationClass.startsWith("jakarta.persistence")
                || annotationClass.endsWith("SpringBean")
                || "com.google.inject.Inject".equals(annotationClass)
                || annotationClass.startsWith("com.google.") && annotationClass.endsWith(".Bind")
                        && annotationClass.hashCode() == -243168318
                || annotationClass.startsWith("org.nuxeo.common.xmap.annotation")
                || annotationClass.startsWith("com.google.gwt.uibinder.client")
                || annotationClass.startsWith("org.springframework.beans.factory.annotation")
                || "javax.ws.rs.core.Context".equals(annotationClass)
                || "jakarta.ws.rs.core.Context".equals(annotationClass)
                || "javafx.fxml.FXML".equals(annotationClass)) {
            return true;
        }
        int lastDot = annotationClass.lastIndexOf('.');
        String lastPart = annotationClass.substring(lastDot + 1);
        return lastPart.startsWith("Inject");
    }

    @Override
    public void visit(ConstantValue obj) {
        // ConstantValue is an attribute of a field, so the instance variables
        // set during visitation of the Field are still valid here
        XField f = XFactory.createXField(this);
        data.constantFields.add(f);
        data.writtenFields.add(f);
    }

    int count_aload_1;

    private int previousOpcode;

    private int previousPreviousOpcode;

    @Override
    public void visit(Code obj) {

        count_aload_1 = 0;
        previousOpcode = -1;
        previousPreviousOpcode = -1;
        data.nullTested.clear();
        seenInvokeStatic = false;
        seenMonitorEnter = getMethod().isSynchronized();
        data.staticFieldsReadInThisMethod.clear();
        super.visit(obj);
        if (Const.CONSTRUCTOR_NAME.equals(getMethodName()) && count_aload_1 > 1
                && (getClassName().indexOf('$') >= 0 || getClassName().indexOf('+') >= 0)) {
            data.needsOuterObjectInConstructor.add(getDottedClassName());
            // System.out.println(betterClassName +
            // " needs outer object in constructor");
        }
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(Method obj) {
        if (DEBUG) {
            System.out.println("Checking " + getClassName() + "." + obj.getName());
        }
        if (Const.CONSTRUCTOR_NAME.equals(getMethodName()) && (obj.isPublic() || obj.isProtected())) {
            publicOrProtectedConstructor = true;
        }
        pendingGetField = null;
        saState = 0;
        super.visit(obj);
        int flags = obj.getAccessFlags();
        if ((flags & Const.ACC_NATIVE) != 0) {
            hasNativeMethods = true;
        }
    }

    boolean seenInvokeStatic;

    boolean seenMonitorEnter;

    XField pendingGetField;

    UnreadFieldsData data = new UnreadFieldsData();
    int saState;

    @Override
    public void sawOpcode(int seen) {
        if (DEBUG) {
            System.out.println(getPC() + ": " + Const.getOpcodeName(seen) + " " + saState);
        }
        if (seen == Const.MONITORENTER) {
            seenMonitorEnter = true;
        }
        switch (saState) {
        case 0:
            if (seen == Const.ALOAD_0) {
                saState = 1;
            }
            break;
        case 1:
            if (seen == Const.ALOAD_0) {
                saState = 2;
            } else {
                saState = 0;
            }
            break;
        case 2:
            if (seen == Const.GETFIELD) {
                saState = 3;
            } else {
                saState = 0;
            }
            break;
        case 3:
            if (seen == Const.PUTFIELD) {
                saState = 4;
            } else {
                saState = 0;
            }
            break;
        default:
            break;
        }
        boolean selfAssignment = false;
        if (pendingGetField != null) {
            if (seen != Const.PUTFIELD && seen != Const.PUTSTATIC) {
                data.readFields.add(pendingGetField);
            } else if (XFactory.createReferencedXField(this).equals(pendingGetField) && (saState == 4 || seen == Const.PUTSTATIC)) {
                selfAssignment = true;
            } else {
                data.readFields.add(pendingGetField);
            }
            pendingGetField = null;
        }
        if (saState == 4) {
            saState = 0;
        }

        if (seen == Const.INVOKESTATIC && "java/util/concurrent/atomic/AtomicReferenceFieldUpdater".equals(getClassConstantOperand())
                && "newUpdater".equals(getNameConstantOperand())) {
            String fieldName = (String) stack.getStackItem(0).getConstant();
            String fieldSignature = (String) stack.getStackItem(1).getConstant();
            String fieldClass = (String) stack.getStackItem(2).getConstant();
            if (fieldName != null && fieldSignature != null && fieldClass != null) {
                XField f = XFactory.createXField(fieldClass.replace('/', '.'), fieldName, ClassName.toSignature(fieldSignature),
                        false);
                data.reflectiveFields.add(f);
            }

        }
        if (seen == Const.INVOKESTATIC && "java/util/concurrent/atomic/AtomicIntegerFieldUpdater".equals(getClassConstantOperand())
                && "newUpdater".equals(getNameConstantOperand())) {
            String fieldName = (String) stack.getStackItem(0).getConstant();
            String fieldClass = (String) stack.getStackItem(1).getConstant();
            if (fieldName != null && fieldClass != null) {
                XField f = XFactory.createXField(fieldClass.replace('/', '.'), fieldName, "I", false);
                data.reflectiveFields.add(f);
            }

        }
        if (seen == Const.INVOKESTATIC && "java/util/concurrent/atomic/AtomicLongFieldUpdater".equals(getClassConstantOperand())
                && "newUpdater".equals(getNameConstantOperand())) {
            String fieldName = (String) stack.getStackItem(0).getConstant();
            String fieldClass = (String) stack.getStackItem(1).getConstant();
            if (fieldName != null && fieldClass != null) {
                XField f = XFactory.createXField(fieldClass.replace('/', '.'), fieldName, "J", false);
                data.reflectiveFields.add(f);
            }

        }

        if (seen == Const.GETSTATIC) {
            XField f = XFactory.createReferencedXField(this);
            data.staticFieldsReadInThisMethod.add(f);
        } else if (seen == Const.INVOKESTATIC) {
            seenInvokeStatic = true;
        } else if (seen == Const.PUTSTATIC && !getMethod().isStatic()) {
            XField f = XFactory.createReferencedXField(this);
            OpcodeStack.Item valuePut = getStack().getStackItem(0);

            checkWriteToStaticFromInstanceMethod: if (f.getName().indexOf("class$") != 0) {
                int priority = LOW_PRIORITY;
                if (f.isReferenceType()) {
                    try {
                        ValueNumberDataflow vnaDataflow = getClassContext().getValueNumberDataflow(getMethod());
                        IsNullValueDataflow invDataflow = getClassContext().getIsNullValueDataflow(getMethod());
                        ValueNumberFrame vFrame = vnaDataflow.getAnalysis().getFactAtPC(vnaDataflow.getCFG(), getPC());
                        IsNullValueFrame iFrame = invDataflow.getAnalysis().getFactAtPC(invDataflow.getCFG(), getPC());
                        AvailableLoad l = new AvailableLoad(f);
                        ValueNumber[] availableLoads = vFrame.getAvailableLoad(l);
                        if (availableLoads != null && iFrame.isTrackValueNumbers()) {
                            for (ValueNumber v : availableLoads) {
                                IsNullValue knownValue = iFrame.getKnownValue(v);
                                if (knownValue == null) {
                                    continue;
                                }
                                if (knownValue.isDefinitelyNotNull()) {
                                    if (valuePut.isNull()) {
                                        priority++;
                                    } else {
                                        priority--;
                                    }
                                    break;
                                } else if (knownValue.isDefinitelyNull()) {
                                    break checkWriteToStaticFromInstanceMethod;
                                }
                            }
                        }

                    } catch (CheckedAnalysisException e) {
                        AnalysisContext.logError("foo", e);
                    }
                }

                if (!publicOrProtectedConstructor) {
                    priority--;
                }
                if (seenMonitorEnter) {
                    priority++;
                }
                if (!seenInvokeStatic && data.staticFieldsReadInThisMethod.isEmpty()) {
                    priority--;
                }
                if (getThisClass().isPublic() && getMethod().isPublic()) {
                    priority--;
                }
                if (getThisClass().isPrivate() || getMethod().isPrivate()) {
                    priority++;
                }
                if (getClassName().indexOf('$') != -1 || BCELUtil.isSynthetic(getMethod()) || f.isSynthetic()
                        || f.getName().indexOf('$') >= 0) {
                    priority++;
                }

                // Decrease priority for boolean fields used to control
                // debug/test settings
                if (f.getName().indexOf("DEBUG") >= 0 || f.getName().indexOf("VERBOSE") >= 0 && "Z".equals(f.getSignature())) {
                    priority++;
                    priority++;
                }
                // Eclipse bundles which implements start/stop *very* often
                // assigns static instances there
                if (("start".equals(getMethodName()) || "stop".equals(getMethodName()))
                        && "(Lorg/osgi/framework/BundleContext;)V".equals(getMethodSig())) {
                    try {
                        JavaClass bundleClass = Repository.lookupClass("org.osgi.framework.BundleActivator");
                        if (getThisClass().instanceOf(bundleClass)) {
                            priority++;
                        }
                        if (f.isReferenceType()) {
                            FieldDescriptor fieldInfo = f.getFieldDescriptor();
                            String dottedClass = DeepSubtypeAnalysis.getComponentClass(fieldInfo.getSignature());
                            JavaClass fieldClass = Repository.lookupClass(dottedClass);
                            if (fieldClass != null && fieldClass.instanceOf(bundleClass)) {
                                // the code "plugin = this;" unfortunately
                                // exists in the
                                // template for new Eclipse plugin classes, so
                                // nearly every one
                                // plugin has this pattern => decrease to very
                                // low prio
                                priority = Priorities.IGNORE_PRIORITY;
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        bugReporter.reportMissingClass(e);
                    }
                }
                bugAccumulator.accumulateBug(new BugInstance(this, "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", priority)
                        .addClassAndMethod(this).addField(f), this);

            }
        }

        // Store annotation for the anonymous class creation
        if (seen == Const.INVOKESPECIAL && getMethodDescriptorOperand().getName().equals(Const.CONSTRUCTOR_NAME) && ClassName.isAnonymous(
                getClassConstantOperand())) {
            List<BugAnnotation> annotation = new ArrayList<>();
            annotation.add(ClassAnnotation.fromClassDescriptor(getClassDescriptor()));
            annotation.add(MethodAnnotation.fromVisitedMethod(this));
            annotation.add(SourceLineAnnotation.fromVisitedInstruction(this));
            anonymousClassAnnotation.put(getClassDescriptorOperand().getDottedClassName(), annotation);
        }

        if (seen == Const.PUTFIELD || seen == Const.ASTORE || seen == Const.ASTORE_0 || seen == Const.ASTORE_1 || seen == Const.ASTORE_2
                || seen == Const.ASTORE_3) {
            Item item = stack.getStackItem(0);
            XMethod xMethod = item.getReturnValueOf();
            if (xMethod != null && xMethod.getName().equals(Const.CONSTRUCTOR_NAME) && ClassName.isAnonymous(xMethod.getClassName())) {
                List<BugAnnotation> annotations = anonymousClassAnnotation.get(xMethod.getClassName());
                if (annotations == null) {
                    annotations = new ArrayList<>();
                }
                if (seen == Const.PUTFIELD) {
                    annotations.add(FieldAnnotation.fromReferencedField(this));
                } else {
                    annotations.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), getRegisterOperand(), getPC(), getNextPC()));
                }
                anonymousClassAnnotation.put(xMethod.getClassName(), annotations);
            }
        }

        if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE || seen == Const.INVOKESPECIAL || seen == Const.INVOKESTATIC) {

            String sig = getSigConstantOperand();
            String invokedClassName = getClassConstantOperand();
            if (invokedClassName.equals(getClassName())
                    && (Const.CONSTRUCTOR_NAME.equals(getMethodName()) || Const.STATIC_INITIALIZER_NAME.equals(getMethodName()))) {

                data.calledFromConstructors.add(getNameConstantOperand() + ":" + sig);
            }
            int pos = PreorderVisitor.getNumberArguments(sig);
            if (stack.getStackDepth() > pos) {
                OpcodeStack.Item item = stack.getStackItem(pos);
                boolean superCall = seen == Const.INVOKESPECIAL && !invokedClassName.equals(getClassName());

                if (DEBUG) {
                    System.out.println("In " + getFullyQualifiedMethodName() + " saw call on " + item);
                }

                boolean selfCall = item.getRegisterNumber() == 0 && !superCall;
                if (selfCall && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                    sawSelfCallInConstructor = true;
                    if (DEBUG) {
                        System.out.println("Saw self call in " + getFullyQualifiedMethodName() + " to " + invokedClassName + "."
                                + getNameConstantOperand());
                    }
                }
            }
        }

        if ((seen == Const.IFNULL || seen == Const.IFNONNULL) && stack.getStackDepth() > 0) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField f = item.getXField();
            if (f != null) {
                data.nullTested.add(f);
                if (DEBUG) {
                    System.out.println(f + " null checked in " + getFullyQualifiedMethodName());
                }
            }
        }

        if ((seen == Const.IF_ACMPEQ || seen == Const.IF_ACMPNE) && stack.getStackDepth() >= 2) {
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);
            XField field1 = item1.getXField();
            if (item0.isNull() && field1 != null) {
                data.nullTested.add(field1);
            } else {
                XField field0 = item0.getXField();
                if (item1.isNull() && field0 != null) {
                    data.nullTested.add(field0);
                }
            }
        }

        computePlacesAssumedNonnull: if (seen == Const.GETFIELD || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE
                || seen == Const.INVOKESPECIAL || seen == Const.PUTFIELD || seen == Const.IALOAD || seen == Const.AALOAD || seen == Const.BALOAD
                || seen == Const.CALOAD || seen == Const.SALOAD || seen == Const.IASTORE || seen == Const.AASTORE || seen == Const.BASTORE
                || seen == Const.CASTORE
                || seen == Const.SASTORE || seen == Const.ARRAYLENGTH) {
            int pos = 0;
            switch (seen) {
            case Const.ARRAYLENGTH:
            case Const.GETFIELD:
                pos = 0;
                break;
            case Const.INVOKEVIRTUAL:
            case Const.INVOKEINTERFACE:
            case Const.INVOKESPECIAL:
                String sig = getSigConstantOperand();
                pos = PreorderVisitor.getNumberArguments(sig);
                break;
            case Const.PUTFIELD:
            case Const.IALOAD:
            case Const.AALOAD:
            case Const.BALOAD:
            case Const.CALOAD:
            case Const.SALOAD:
                pos = 1;
                break;
            case Const.IASTORE:
            case Const.AASTORE:
            case Const.BASTORE:
            case Const.CASTORE:
            case Const.SASTORE:
                pos = 2;
                break;
            default:
                throw new RuntimeException("Impossible");
            }
            if (stack.getStackDepth() >= pos) {
                OpcodeStack.Item item = stack.getStackItem(pos);
                XField f = item.getXField();
                if (f != null
                        && !f.isStatic()
                        && !data.nullTested.contains(f)
                        && !((data.writtenInConstructorFields.contains(f) || data.writtenInInitializationFields.contains(f))
                                && data.writtenNonNullFields
                                        .contains(f))) {
                    try {
                        IsNullValueDataflow invDataflow = getClassContext().getIsNullValueDataflow(getMethod());
                        IsNullValueFrame iFrame = invDataflow.getAnalysis().getFactBeforeExceptionCheck(invDataflow.getCFG(),
                                getPC());
                        if (!iFrame.isValid() || iFrame.getStackValue(pos).isDefinitelyNotNull()) {
                            break computePlacesAssumedNonnull;
                        }

                    } catch (CheckedAnalysisException e) {
                        AnalysisContext.logError("INV dataflow error when analyzing " + getMethodDescriptor(), e);
                    }
                    if (DEBUG) {
                        System.out.println("RRR: " + f + " " + data.nullTested.contains(f) + " "
                                + data.writtenInConstructorFields.contains(f) + " " + data.writtenNonNullFields.contains(f));
                    }

                    ProgramPoint p = new ProgramPoint(this);
                    Set<ProgramPoint> s = data.assumedNonNull.get(f);
                    if (s == null) {
                        s = Collections.singleton(p);
                    } else {
                        s = Util.addTo(s, p);
                    }
                    data.assumedNonNull.put(f, s);
                    if (DEBUG) {
                        System.out.println(f + " assumed non-null in " + getFullyQualifiedMethodName());
                    }
                }
            }
        }

        if (seen == Const.ALOAD_1) {
            count_aload_1++;
        } else if (seen == Const.GETFIELD || seen == Const.GETSTATIC) {
            XField f = XFactory.createReferencedXField(this);
            pendingGetField = f;
            if ("readResolve".equals(getMethodName()) && seen == Const.GETFIELD) {
                data.writtenFields.add(f);
                data.writtenNonNullFields.add(f);
            }
            if (DEBUG) {
                System.out.println("get: " + f);
            }
            if (!data.fieldAccess.containsKey(f)) {
                data.fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
            }
        } else if ((seen == Const.PUTFIELD || seen == Const.PUTSTATIC) && !selfAssignment) {
            XField f = XFactory.createReferencedXField(this);
            OpcodeStack.Item item = null;
            if (stack.getStackDepth() > 0) {
                item = stack.getStackItem(0);
                if (!item.isNull()) {
                    data.nullTested.add(f);
                }
            }
            data.writtenFields.add(f);

            boolean writtingNonNull = previousOpcode != Const.ACONST_NULL || previousPreviousOpcode == Const.GOTO;
            if (writtingNonNull) {
                data.writtenNonNullFields.add(f);
                if (DEBUG) {
                    System.out.println("put nn: " + f);
                }
            } else if (DEBUG) {
                System.out.println("put: " + f);
            }
            if (writtingNonNull && data.readFields.contains(f)) {
                data.fieldAccess.remove(f);
            } else if (!data.fieldAccess.containsKey(f)) {
                data.fieldAccess.put(f, SourceLineAnnotation.fromVisitedInstruction(this));
            }

            boolean isConstructor = Const.CONSTRUCTOR_NAME.equals(getMethodName()) || Const.STATIC_INITIALIZER_NAME.equals(getMethodName());
            if (getMethod().isStatic() == f.isStatic()
                    && (isConstructor || data.calledFromConstructors.contains(getMethodName() + ":" + getMethodSig())
                            || "init".equals(getMethodName()) || "initialize".equals(getMethodName())
                            || getMethod().isPrivate())) {

                if (isConstructor) {
                    data.writtenInConstructorFields.add(f);
                    if ("Ljava/lang/ThreadLocal;".equals(f.getSignature()) && item != null && item.isNewlyAllocated()) {
                        data.threadLocalAssignedInConstructor.put(f, new ProgramPoint(this));
                    }
                } else {
                    data.writtenInInitializationFields.add(f);
                }
                if (writtingNonNull) {
                    data.assumedNonNull.remove(f);
                }
            } else {
                data.writtenOutsideOfInitializationFields.add(f);
            }

        }
        previousPreviousOpcode = previousOpcode;
        previousOpcode = seen;
    }

    /**
     * @deprecated Use {@link edu.umd.cs.findbugs.detect.UnreadFieldsData#isReflexive(XField)} instead
     */
    @Deprecated
    public boolean isReflexive(XField f) {
        return data.isReflexive(f);
    }

    static Pattern dontComplainAbout = Pattern.compile("class[$]");

    static Pattern withinAnonymousClass = Pattern.compile("[$][0-9].*[$]");

    @Override
    public void report() {
        Set<String> fieldNamesSet = new HashSet<>();
        for (XField f : data.writtenNonNullFields) {
            fieldNamesSet.add(f.getName());
        }
        if (DEBUG) {
            System.out.println("read fields:");
            for (XField f : data.readFields) {
                System.out.println("  " + f);
            }
            if (!data.containerFields.isEmpty()) {
                System.out.println("ejb3 fields:");
                for (XField f : data.containerFields) {
                    System.out.println("  " + f);
                }
            }
            if (!data.reflectiveFields.isEmpty()) {
                System.out.println("reflective fields:");
                for (XField f : data.reflectiveFields) {
                    System.out.println("  " + f);
                }
            }

            System.out.println("written fields:");
            for (XField f : data.writtenFields) {
                System.out.println("  " + f);
            }
            System.out.println("written nonnull fields:");
            for (XField f : data.writtenNonNullFields) {
                System.out.println("  " + f);
            }

            System.out.println("assumed nonnull fields:");
            for (XField f : data.assumedNonNull.keySet()) {
                System.out.println("  " + f);
            }
        }
        Set<XField> declaredFields = new HashSet<>();
        AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
        XFactory xFactory = AnalysisContext.currentXFactory();
        for (XField f : AnalysisContext.currentXFactory().allFields()) {
            ClassDescriptor classDescriptor = f.getClassDescriptor();
            if (currentAnalysisContext.isApplicationClass(classDescriptor)
                    && !currentAnalysisContext.isTooBig(classDescriptor)) {
                declaredFields.add(f);
            }
        }
        // Don't report anything about ejb3Fields
        HashSet<XField> unknownAnotationAndUnwritten = new HashSet<>(data.unknownAnnotation.keySet());
        unknownAnotationAndUnwritten.removeAll(data.writtenFields);
        declaredFields.removeAll(unknownAnotationAndUnwritten);
        declaredFields.removeAll(data.containerFields);
        declaredFields.removeAll(data.reflectiveFields);
        declaredFields.removeIf(f -> f.isSynthetic() && !f.getName().startsWith("this$") || f.getName()
                .startsWith("_"));

        TreeSet<XField> notInitializedInConstructors = new TreeSet<>(declaredFields);
        notInitializedInConstructors.retainAll(data.readFields);
        notInitializedInConstructors.retainAll(data.writtenNonNullFields);
        notInitializedInConstructors.retainAll(data.assumedNonNull.keySet());
        notInitializedInConstructors.removeAll(data.writtenInConstructorFields);
        notInitializedInConstructors.removeAll(data.writtenInInitializationFields);

        notInitializedInConstructors.removeIf(AccessibleEntity::isStatic);

        TreeSet<XField> readOnlyFields = new TreeSet<>(declaredFields);
        readOnlyFields.removeAll(data.writtenFields);

        readOnlyFields.retainAll(data.readFields);

        TreeSet<XField> nullOnlyFields = new TreeSet<>(declaredFields);
        nullOnlyFields.removeAll(data.writtenNonNullFields);

        nullOnlyFields.retainAll(data.readFields);

        Set<XField> writeOnlyFields = declaredFields;
        writeOnlyFields.removeAll(data.readFields);

        Map<String, Integer> count = new HashMap<>();
        Bag<String> nullOnlyFieldNames = new Bag<>();
        Bag<ClassDescriptor> classContainingNullOnlyFields = new Bag<>();

        for (XField f : nullOnlyFields) {
            nullOnlyFieldNames.add(f.getName());
            classContainingNullOnlyFields.add(f.getClassDescriptor());
            int increment = 3;
            Collection<ProgramPoint> assumedNonNullAt = data.assumedNonNull.get(f);
            if (assumedNonNullAt != null) {
                increment += assumedNonNullAt.size();
            }
            for (String s : data.unknownAnnotation.get(f)) {
                Integer value = count.get(s);
                if (value == null) {
                    count.put(s, increment);
                } else {
                    count.put(s, value + increment);
                }
            }
        }
        Map<XField, Integer> maxCount = new HashMap<>();

        LinkedList<XField> assumeReflective = new LinkedList<>();
        for (XField f : nullOnlyFields) {
            int myMaxCount = 0;
            for (String s : data.unknownAnnotation.get(f)) {
                Integer value = count.get(s);
                if (value != null && myMaxCount < value) {
                    myMaxCount = value;
                }
            }
            if (myMaxCount > 0) {
                maxCount.put(f, myMaxCount);
            }
            if (myMaxCount > 15) {
                assumeReflective.add(f);
            } else if (nullOnlyFieldNames.getCount(f.getName()) > 8) {
                assumeReflective.add(f);
            } else if (classContainingNullOnlyFields.getCount(f.getClassDescriptor()) > 4) {
                assumeReflective.add(f);
            } else if (classContainingNullOnlyFields.getCount(f.getClassDescriptor()) > 2 && f.getName().length() == 1) {
                assumeReflective.add(f);
            }

        }

        readOnlyFields.removeAll(assumeReflective);
        nullOnlyFields.removeAll(assumeReflective);
        notInitializedInConstructors.removeAll(assumeReflective);

        Bag<String> notInitializedUses = new Bag<>();
        for (XField f : notInitializedInConstructors) {
            String className = f.getClassName();
            Set<ProgramPoint> assumedNonnullAt = data.assumedNonNull.get(f);
            notInitializedUses.add(className, assumedNonnullAt.size());
        }
        for (XField f : notInitializedInConstructors) {
            String className = f.getClassName();
            if (notInitializedUses.getCount(className) >= 8) {
                continue;
            }
            String fieldSignature = f.getSignature();
            if (f.isResolved() && !data.fieldsOfNativeClasses.contains(f)
                    && (fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '[')) {
                int priority = LOW_PRIORITY;

                Set<ProgramPoint> assumedNonnullAt = data.assumedNonNull.get(f);
                if (assumedNonnullAt.size() < 4) {
                    for (ProgramPoint p : assumedNonnullAt) {
                        BugInstance bug = new BugInstance(this, "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", priority)
                                .addClass(className).addField(f).addMethod(p.getMethodAnnotation());
                        bugAccumulator.accumulateBug(bug, p.getSourceLineAnnotation());
                    }
                }

            }
        }

        for (XField f : readOnlyFields) {
            //            String fieldName = f.getName();
            //            String className = f.getClassName();
            String fieldSignature = f.getSignature();
            if (f.isResolved() && !data.fieldsOfNativeClasses.contains(f)) {
                int priority = NORMAL_PRIORITY;
                if (xFactory.isReflectiveClass(f.getClassDescriptor())) {
                    priority++;
                }
                if (!(fieldSignature.charAt(0) == 'L' || fieldSignature.charAt(0) == '[')) {
                    priority++;
                }
                if (maxCount.containsKey(f)) {
                    priority++;
                }
                String pattern = "UWF_UNWRITTEN_FIELD";
                if (f.isProtected() || f.isPublic()) {
                    pattern = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD";
                }
                bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this, pattern, priority), f));
            }

        }
        for (XField f : nullOnlyFields) {
            //            String fieldName = f.getName();
            //            String className = f.getClassName();
            //            String fieldSignature = f.getSignature();
            if (DEBUG) {
                System.out.println("Null only: " + f);
                System.out.println("   : " + data.assumedNonNull.containsKey(f));
                System.out.println("   : " + data.fieldsOfSerializableOrNativeClassed.contains(f));
                System.out.println("   : " + fieldNamesSet.contains(f.getName()));
                System.out.println("   : " + data.abstractClasses.contains(f.getClassName()));
                System.out.println("   : " + data.hasNonAbstractSubClass.contains(f.getClassName()));
                System.out.println("   : " + f.isResolved());
            }
            if (!f.isResolved()) {
                continue;
            }
            if (data.fieldsOfNativeClasses.contains(f)) {
                continue;
            }
            if (DEBUG) {
                System.out.println("Ready to report");
            }
            int priority = NORMAL_PRIORITY;
            if (maxCount.containsKey(f)) {
                priority++;
            }
            if (data.abstractClasses.contains(f.getClassName())) {
                priority++;
                if (!data.hasNonAbstractSubClass.contains(f.getClassName())) {
                    priority++;
                }
            }
            // if (fieldNamesSet.contains(f.getName())) priority++;
            if (data.assumedNonNull.containsKey(f)) {
                int npPriority = priority;

                Set<ProgramPoint> assumedNonNullAt = data.assumedNonNull.get(f);
                if (assumedNonNullAt.size() > 14) {
                    npPriority += 2;
                } else if (assumedNonNullAt.size() > 6) {
                    npPriority++;
                } else {
                    priority--;
                }
                if (xFactory.isReflectiveClass(f.getClassDescriptor())) {
                    priority++;
                }
                String pattern = (f.isPublic() || f.isProtected()) ? "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"
                        : "NP_UNWRITTEN_FIELD";
                for (ProgramPoint p : assumedNonNullAt) {
                    bugAccumulator.accumulateBug(
                            new BugInstance(this, pattern, npPriority).addClassAndMethod(p.method).addField(f),
                            p.getSourceLineAnnotation());
                }

            } else {
                if (xFactory.isReflectiveClass(f.getClassDescriptor())) {
                    priority++;
                }
                if (f.isStatic()) {
                    priority++;
                }
                if (f.isFinal()) {
                    priority++;
                }
                if (data.fieldsOfSerializableOrNativeClassed.contains(f)) {
                    priority++;
                }
            }
            if (!readOnlyFields.contains(f)) {
                bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this, "UWF_NULL_FIELD", priority), f)
                        .lowerPriorityIfDeprecated());
            }
        }

        writeOnlyFields: for (XField f : writeOnlyFields) {
            String fieldName = f.getName();
            String className = f.getClassName();
            int lastDollar = Math.max(className.lastIndexOf('$'), className.lastIndexOf('+'));
            boolean isAnonymousInnerClass = (lastDollar > 0) && (lastDollar < className.length() - 1)
                    && Character.isDigit(className.charAt(lastDollar + 1));

            if (DEBUG) {
                System.out.println("Checking write only field " + className + "." + fieldName + "\t" + data.constantFields.contains(f)
                        + "\t" + f.isStatic());
            }
            if (!f.isResolved()) {
                continue;
            }
            if (dontComplainAbout.matcher(fieldName).find()) {
                continue;
            }
            if (lastDollar >= 0 && (fieldName.startsWith("this$") || fieldName.startsWith("this+"))) {
                String outerClassName = className.substring(0, lastDollar);

                try {
                    XClass thisClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, f.getClassDescriptor());

                    if (isAnonymousInnerClass) {
                        for (XField f2 : thisClass.getXFields()) {
                            if (f2 != f && f2.isPrivate() && f2.isSynthetic() && !f2.getName().startsWith("this$")
                                    && f2.getName().contains("$")) {
                                continue writeOnlyFields;
                            }
                        }
                    }
                    JavaClass outerClass = Repository.lookupClass(outerClassName);
                    if (classHasParameter(outerClass)) {
                        continue;
                    }

                    ClassDescriptor cDesc = DescriptorFactory.createClassDescriptorFromDottedClassName(outerClassName);

                    XClass outerXClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, cDesc);

                    AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();

                    Subtypes2 subtypes2 = analysisContext.getSubtypes2();

                    for (XField of : outerXClass.getXFields()) {
                        if (!of.isStatic()) {
                            String sourceSignature = of.getSourceSignature();
                            if (sourceSignature != null && "Ljava/lang/ThreadLocal;".equals(of.getSignature())) {
                                Type ofType = GenericUtilities.getType(sourceSignature);
                                if (ofType instanceof GenericObjectType) {
                                    GenericObjectType gType = (GenericObjectType) ofType;

                                    for (ReferenceType r : gType.getParameters()) {
                                        if (r instanceof ObjectType) {
                                            ClassDescriptor c = DescriptorFactory.getClassDescriptor((ObjectType) r);
                                            if (subtypes2.isSubtype(f.getClassDescriptor(), c)) {
                                                ProgramPoint p = data.threadLocalAssignedInConstructor.get(of);
                                                int priority = p == null ? NORMAL_PRIORITY : HIGH_PRIORITY;
                                                BugInstance bug = new BugInstance(this, "SIC_THREADLOCAL_DEADLY_EMBRACE",
                                                        priority).addClass(className).addField(of);
                                                if (p != null) {
                                                    bug.addMethod(p.method).add(p.getSourceLineAnnotation());
                                                }
                                                bugReporter.reportBug(bug);
                                            }
                                        }
                                    }
                                }
                            }

                        }
                    }

                    boolean outerClassIsInnerClass = false;
                    for (Field field : outerClass.getFields()) {
                        if ("this$0".equals(field.getName())) {
                            outerClassIsInnerClass = true;
                        }
                    }
                    if (outerClassIsInnerClass) {
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    bugReporter.reportMissingClass(e);
                } catch (CheckedAnalysisException e) {
                    bugReporter.logError("Error getting outer XClass for " + outerClassName, e);
                }
                if (!data.innerClassCannotBeStatic.contains(className)) {
                    boolean easyChange = !data.needsOuterObjectInConstructor.contains(className);
                    if (easyChange || !isAnonymousInnerClass) {

                        // easyChange isAnonymousInnerClass
                        // true false medium, SIC
                        // true true low, SIC_ANON
                        // false true not reported
                        // false false low, SIC_THIS
                        int priority = LOW_PRIORITY;
                        if (easyChange && !isAnonymousInnerClass) {
                            priority = NORMAL_PRIORITY;
                        }

                        BugInstance bugInstance;
                        if (isAnonymousInnerClass) {
                            bugInstance = new BugInstance(this, "SIC_INNER_SHOULD_BE_STATIC_ANON", priority);
                            List<BugAnnotation> annotations = anonymousClassAnnotation.remove(f.getClassDescriptor().getDottedClassName());
                            if (annotations != null) {
                                bugInstance.addClass(className).describe(ClassAnnotation.ANONYMOUS_ROLE);
                                bugInstance.addAnnotations(annotations);
                            } else {
                                bugInstance.addClass(className);
                            }
                        } else if (!easyChange) {
                            bugInstance = new BugInstance(this, "SIC_INNER_SHOULD_BE_STATIC_NEEDS_THIS", priority).addClass(className);
                        } else {
                            bugInstance = new BugInstance(this, "SIC_INNER_SHOULD_BE_STATIC", priority).addClass(className);
                        }

                        bugReporter.reportBug(bugInstance);

                    }
                }
            } else if (f.isResolved()) {
                if (data.constantFields.contains(f)) {
                    if (!f.isStatic()) {
                        bugReporter.reportBug(addClassFieldAndAccess(
                                new BugInstance(this, "SS_SHOULD_BE_STATIC", NORMAL_PRIORITY), f));
                    }
                } else if (data.fieldsOfSerializableOrNativeClassed.contains(f)) {
                    // ignore it
                } else if (!data.writtenFields.contains(f)) {
                    int priority = NORMAL_PRIORITY;
                    if (xFactory.isReflectiveClass(f.getClassDescriptor())) {
                        priority++;
                    }
                    bugReporter.reportBug(new BugInstance(this,
                            (f.isPublic() || f.isProtected()) ? "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD" : "UUF_UNUSED_FIELD",
                            priority).addClass(className).addField(f).lowerPriorityIfDeprecated());
                } else if (f.getName().toLowerCase().indexOf("guardian") < 0) {
                    int priority = NORMAL_PRIORITY;
                    if (xFactory.isReflectiveClass(f.getClassDescriptor())) {
                        priority++;
                    }
                    if (f.isStatic()) {
                        priority++;
                    }
                    if (f.isFinal()) {
                        priority++;
                    }
                    bugReporter.reportBug(addClassFieldAndAccess(new BugInstance(this,
                            (f.isPublic() || f.isProtected()) ? "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD" : "URF_UNREAD_FIELD",
                            priority), f));
                }
            }
        }
        bugAccumulator.reportAccumulatedBugs();
        data.fieldAccess.clear();
    }

    private BugInstance addClassFieldAndAccess(BugInstance instance, XField f) {
        if (data.writtenNonNullFields.contains(f) && data.readFields.contains(f)) {
            throw new IllegalArgumentException("No information for fields that are both read and written nonnull");
        }

        instance.addClass(f.getClassName()).addField(f);
        if (data.fieldAccess.containsKey(f)) {
            instance.add(data.fieldAccess.get(f));
        }
        return instance;
    }

}
