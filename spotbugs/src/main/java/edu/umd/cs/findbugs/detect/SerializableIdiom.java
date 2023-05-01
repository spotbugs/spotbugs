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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

public class SerializableIdiom extends OpcodeStackDetector {

    private static final boolean DEBUG = SystemProperties.getBoolean("se.debug");

    final static boolean reportTransientFieldOfNonSerializableClass = SystemProperties
            .getBoolean("reportTransientFieldOfNonSerializableClass");

    boolean sawSerialVersionUID;

    boolean isSerializable, implementsSerializableDirectly;

    boolean isExternalizable;

    boolean isGUIClass;

    boolean isEjbImplClass;

    boolean isJSPClass;

    boolean foundSynthetic;

    boolean seenTransientField;

    boolean foundSynchronizedMethods;

    boolean writeObjectIsSynchronized;

    private final BugReporter bugReporter;

    boolean isAbstract;

    private final List<BugInstance> fieldWarningList = new LinkedList<>();

    private final HashMap<String, XField> fieldsThatMightBeAProblem = new HashMap<>();

    private final HashMap<XField, Integer> transientFieldsUpdates = new HashMap<>();

    private final HashSet<XField> transientFieldsSetInConstructor = new HashSet<>();

    private final HashSet<XField> transientFieldsSetToDefaultValueInConstructor = new HashSet<>();

    private final Map<XField, BugInstance> optionalBugsInReadExternal = new HashMap();

    private Optional<XField> initializedCheckerVariable = Optional.empty();

    private boolean sawReadExternalExit = false;

    private boolean sawReadExternal;

    private boolean sawWriteExternal;

    private boolean sawReadObject;

    private boolean sawReadResolve;

    private boolean sawWriteObject;

    private boolean superClassImplementsSerializable;

    private boolean superClassHasReadObject;

    private boolean hasPublicVoidConstructor;

    private boolean superClassHasVoidConstructor;

    private boolean directlyImplementsExternalizable;

    private final boolean testingEnabled;

    // private JavaClass serializable;
    // private JavaClass collection;
    // private JavaClass map;
    // private boolean isRemote;

    public SerializableIdiom(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
        flush();
    }

    private void flush() {
        if (!isAbstract && !((sawReadExternal && sawWriteExternal) || (sawReadObject && sawWriteObject))) {
            for (BugInstance aFieldWarningList : fieldWarningList) {
                bugReporter.reportBug(aFieldWarningList);
            }
        }
        fieldWarningList.clear();
    }

    static final Pattern anonymousInnerClassNamePattern = Pattern.compile(".+\\$\\d+");

    boolean isAnonymousInnerClass;

    boolean innerClassHasOuterInstance;

    private boolean isEnum;

    @Override
    public void visit(JavaClass obj) {
        String superClassname = obj.getSuperclassName();
        // System.out.println("superclass of " + getClassName() + " is " +
        // superClassname);
        isEnum = "java.lang.Enum".equals(superClassname);
        if (isEnum) {
            return;
        }
        int flags = obj.getAccessFlags();
        isAbstract = (flags & Const.ACC_ABSTRACT) != 0 || (flags & Const.ACC_INTERFACE) != 0;
        isAnonymousInnerClass = anonymousInnerClassNamePattern.matcher(getClassName()).matches();
        innerClassHasOuterInstance = false;
        for (Field f : obj.getFields()) {
            if ("this$0".equals(f.getName())) {
                innerClassHasOuterInstance = true;
                break;
            }
        }

        sawSerialVersionUID = false;
        isSerializable = implementsSerializableDirectly = false;
        isExternalizable = false;
        directlyImplementsExternalizable = false;
        isGUIClass = false;
        isEjbImplClass = false;
        isJSPClass = false;
        seenTransientField = false;
        // boolean isEnum = obj.getSuperclassName().equals("java.lang.Enum");
        fieldsThatMightBeAProblem.clear();
        transientFieldsUpdates.clear();
        transientFieldsSetInConstructor.clear();
        transientFieldsSetToDefaultValueInConstructor.clear();
        // isRemote = false;

        // Does this class directly implement Serializable?
        String[] interface_names = obj.getInterfaceNames();
        for (String interface_name : interface_names) {
            if ("java.io.Externalizable".equals(interface_name)) {
                directlyImplementsExternalizable = true;
                isExternalizable = true;
                if (DEBUG) {
                    System.out.println("Directly implements Externalizable: " + getClassName());
                }
            } else if ("java.io.Serializable".equals(interface_name)) {
                implementsSerializableDirectly = true;
                isSerializable = true;
                if (DEBUG) {
                    System.out.println("Directly implements Serializable: " + getClassName());
                }
                break;
            }
        }

        // Does this class indirectly implement Serializable?
        if (!isSerializable) {
            if (Subtypes2.instanceOf(obj, "java.io.Externalizable")) {
                isExternalizable = true;
                if (DEBUG) {
                    System.out.println("Indirectly implements Externalizable: " + getClassName());
                }
            }
            if (Subtypes2.instanceOf(obj, "java.io.Serializable")) {
                isSerializable = true;
                if (DEBUG) {
                    System.out.println("Indirectly implements Serializable: " + getClassName());
                }
            }
        }

        hasPublicVoidConstructor = false;
        superClassHasVoidConstructor = true;
        superClassHasReadObject = false;
        superClassImplementsSerializable = isSerializable && !implementsSerializableDirectly;
        ClassDescriptor superclassDescriptor = getXClass().getSuperclassDescriptor();
        if (superclassDescriptor != null) {
            try {
                XClass superXClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, superclassDescriptor);
                if (superXClass != null) {
                    superClassImplementsSerializable = AnalysisContext
                            .currentAnalysisContext()
                            .getSubtypes2()
                            .isSubtype(superXClass.getClassDescriptor(),
                                    DescriptorFactory.createClassDescriptor(java.io.Serializable.class));
                    superClassHasVoidConstructor = false;
                    for (XMethod m : superXClass.getXMethods()) {
                        if (Const.CONSTRUCTOR_NAME.equals(m.getName()) && "()V".equals(m.getSignature()) && !m.isPrivate()) {
                            superClassHasVoidConstructor = true;
                        }
                        if ("readObject".equals(m.getName()) && "(Ljava/io/ObjectInputStream;)V".equals(m.getSignature())
                                && m.isPrivate()) {
                            superClassHasReadObject = true;
                        }
                    }
                }
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Error while analyzing " + obj.getClassName(), e);
            }
        }

        // Is this a GUI or other class that is rarely serialized?

        isGUIClass = false;
        isEjbImplClass = Subtypes2.instanceOf(obj, "javax.ejb.SessionBean") || Subtypes2.instanceOf(obj, "jakarta.ejb.SessionBean");
        isJSPClass = Subtypes2.isJSP(obj);
        isGUIClass = (Subtypes2.instanceOf(obj, "java.lang.Throwable") || Subtypes2.instanceOf(obj, "java.awt.Component")
                || Subtypes2.instanceOf(obj, "java.awt.Component$AccessibleAWTComponent")
                || Subtypes2.instanceOf(obj, "java.awt.event.ActionListener") || Subtypes2.instanceOf(obj,
                        "java.util.EventListener"));
        if (!isGUIClass) {
            JavaClass o = obj;
            while (o != null) {
                if (o.getClassName().startsWith("java.awt") || o.getClassName().startsWith("javax.swing")) {
                    isGUIClass = true;
                    break;
                }
                try {
                    o = o.getSuperClass();
                } catch (ClassNotFoundException e) {
                    break;
                }
            }

        }

        foundSynthetic = false;
        foundSynchronizedMethods = false;
        writeObjectIsSynchronized = false;

        sawReadExternal = sawWriteExternal = sawReadObject = sawReadResolve = sawWriteObject = false;
        if (isSerializable) {
            for (Method m : obj.getMethods()) {

                if ("readObject".equals(m.getName()) && "(Ljava/io/ObjectInputStream;)V".equals(m.getSignature())) {
                    sawReadObject = true;
                } else if ("readResolve".equals(m.getName()) && m.getSignature().startsWith("()")) {
                    sawReadResolve = true;
                } else if ("readObjectNoData".equals(m.getName()) && "()V".equals(m.getSignature())) {
                    sawReadObject = true;
                } else if ("writeObject".equals(m.getName()) && "(Ljava/io/ObjectOutputStream;)V".equals(m.getSignature())) {
                    sawWriteObject = true;
                }
            }
            for (Field f : obj.getFields()) {
                if (f.isTransient()) {
                    seenTransientField = true;
                }
            }
        }
    }

    private boolean strongEvidenceForIntendedSerialization() {
        return implementsSerializableDirectly
                || sawReadObject
                || sawReadResolve
                || sawWriteObject
                || seenTransientField
                || AnalysisContext.currentAnalysisContext().getUnreadFieldsData()
                        .existsStrongEvidenceForIntendedSerialization(this.getClassDescriptor());
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (isEnum) {
            return;
        }
        if (DEBUG) {
            System.out.println(getDottedClassName());
            System.out.println("  hasPublicVoidConstructor: " + hasPublicVoidConstructor);
            System.out.println("  superClassHasVoidConstructor: " + superClassHasVoidConstructor);
            System.out.println("  isExternalizable: " + isExternalizable);
            System.out.println("  isSerializable: " + isSerializable);
            System.out.println("  isAbstract: " + isAbstract);
            System.out.println("  superClassImplementsSerializable: " + superClassImplementsSerializable);
            System.out.println("  isGUIClass: " + isGUIClass);
            System.out.println("  isEjbImplClass: " + isEjbImplClass);
            System.out.println("  isJSPClass: " + isJSPClass);
        }
        if (isSerializable && !sawReadObject && !sawReadResolve && seenTransientField && !superClassHasReadObject) {
            for (Map.Entry<XField, Integer> e : transientFieldsUpdates.entrySet()) {

                XField fieldX = e.getKey();
                int priority = NORMAL_PRIORITY;
                if (transientFieldsSetInConstructor.contains(e.getKey())) {
                    priority--;
                }

                if (isGUIClass) {
                    priority++;
                }
                if (isEjbImplClass) {
                    priority++;
                }
                if (isJSPClass) {
                    priority++;
                }
                if (e.getValue() < 3) {
                    priority++;
                }
                if (transientFieldsSetToDefaultValueInConstructor.contains(e.getKey())) {
                    priority++;
                }
                if (obj.isAbstract()) {
                    priority++;
                    if (priority < Priorities.LOW_PRIORITY) {
                        priority = Priorities.LOW_PRIORITY;
                    }
                }

                try {
                    double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(fieldX.getSignature());
                    if (isSerializable < 0.6) {
                        priority++;
                    }
                } catch (ClassNotFoundException e1) {
                    // ignore it
                }

                bugReporter.reportBug(new BugInstance(this, "SE_TRANSIENT_FIELD_NOT_RESTORED", priority).addClass(getThisClass())
                        .addField(fieldX));

            }

        }
        if (isSerializable && !isExternalizable && !superClassHasVoidConstructor && !superClassImplementsSerializable) {
            int priority = implementsSerializableDirectly || seenTransientField ? HIGH_PRIORITY
                    : (sawSerialVersionUID ? NORMAL_PRIORITY : LOW_PRIORITY);
            if (isGUIClass || isEjbImplClass || isJSPClass) {
                priority++;
            }
            bugReporter.reportBug(new BugInstance(this, "SE_NO_SUITABLE_CONSTRUCTOR", priority).addClass(getThisClass()
                    .getClassName()));
        }
        // Downgrade class-level warnings if it's a GUI or EJB-implementation
        // class.
        int priority = (isGUIClass || isEjbImplClass || isJSPClass) ? LOW_PRIORITY : NORMAL_PRIORITY;
        if (obj.getClassName().endsWith("_Stub")) {
            priority++;
        }

        if (isExternalizable && !hasPublicVoidConstructor && !isAbstract) {
            bugReporter.reportBug(new BugInstance(this, "SE_NO_SUITABLE_CONSTRUCTOR_FOR_EXTERNALIZATION",
                    directlyImplementsExternalizable ? HIGH_PRIORITY : NORMAL_PRIORITY).addClass(getThisClass().getClassName()));
        }
        if (!foundSynthetic) {
            priority++;
        }
        if (seenTransientField) {
            priority--;
        }
        if (!isAnonymousInnerClass && !isExternalizable && !isGUIClass && !obj.isAbstract() && isSerializable && !isAbstract
                && !sawSerialVersionUID && !isEjbImplClass && !isJSPClass) {
            bugReporter.reportBug(new BugInstance(this, "SE_NO_SERIALVERSIONID", priority).addClass(this));
        }

        if (writeObjectIsSynchronized && !foundSynchronizedMethods) {
            bugReporter.reportBug(new BugInstance(this, "WS_WRITEOBJECT_SYNC", LOW_PRIORITY).addClass(this));
        }

        if (isExternalizable && sawReadExternal && !optionalBugsInReadExternal.isEmpty() && initializedCheckerVariable.isPresent()
                && !optionalBugsInReadExternal.containsKey(initializedCheckerVariable.get())) {
            optionalBugsInReadExternal.values().forEach(bugReporter::reportBug);
        }
    }

    @Override
    public void visit(Method obj) {
        int accessFlags = obj.getAccessFlags();
        boolean isSynchronized = (accessFlags & Const.ACC_SYNCHRONIZED) != 0;
        if (Const.CONSTRUCTOR_NAME.equals(getMethodName()) && "()V".equals(getMethodSig()) && (accessFlags & Const.ACC_PUBLIC) != 0) {
            hasPublicVoidConstructor = true;
        }
        if (!Const.CONSTRUCTOR_NAME.equals(getMethodName()) && isSynthetic(obj)) {
            foundSynthetic = true;
            // System.out.println(methodName + isSynchronized);
        }

        if ("readExternal".equals(getMethodName()) && "(Ljava/io/ObjectInput;)V".equals(getMethodSig())) {
            sawReadExternal = true;
            if (DEBUG && !obj.isPrivate()) {
                System.out.println("Non-private readExternal method in: " + getDottedClassName());
            }
        } else if ("writeExternal".equals(getMethodName()) && "(Ljava/io/Objectoutput;)V".equals(getMethodSig())) {
            sawWriteExternal = true;
            if (DEBUG && !obj.isPrivate()) {
                System.out.println("Non-private writeExternal method in: " + getDottedClassName());
            }
        } else if ("readResolve".equals(getMethodName()) && getMethodSig().startsWith("()") && isSerializable) {
            sawReadResolve = true;
            if (!"()Ljava/lang/Object;".equals(getMethodSig())) {
                bugReporter.reportBug(new BugInstance(this, "SE_READ_RESOLVE_MUST_RETURN_OBJECT", HIGH_PRIORITY)
                        .addClassAndMethod(this));
            } else if (obj.isStatic()) {
                bugReporter.reportBug(new BugInstance(this, "SE_READ_RESOLVE_IS_STATIC", HIGH_PRIORITY).addClassAndMethod(this));
            } else if (obj.isPrivate()) {
                try {
                    Set<ClassDescriptor> subtypes = AnalysisContext.currentAnalysisContext().getSubtypes2()
                            .getSubtypes(getClassDescriptor());
                    if (subtypes.size() > 1) {
                        BugInstance bug = new BugInstance(this, "SE_PRIVATE_READ_RESOLVE_NOT_INHERITED", NORMAL_PRIORITY)
                                .addClassAndMethod(this);
                        boolean nasty = false;
                        for (ClassDescriptor subclass : subtypes) {
                            if (!subclass.equals(getClassDescriptor())) {

                                XClass xSub = AnalysisContext.currentXFactory().getXClass(subclass);
                                if (xSub != null && xSub.findMethod("readResolve", "()Ljava/lang/Object;", false) == null
                                        && xSub.findMethod("writeReplace", "()Ljava/lang/Object;", false) == null) {
                                    bug.addClass(subclass).describe(ClassAnnotation.SUBCLASS_ROLE);
                                    nasty = true;
                                }
                            }
                        }
                        if (nasty) {
                            bug.setPriority(HIGH_PRIORITY);
                        } else if (!getThisClass().isPublic()) {
                            bug.setPriority(LOW_PRIORITY);
                        }
                        bugReporter.reportBug(bug);
                    }

                } catch (ClassNotFoundException e) {
                    bugReporter.reportMissingClass(e);
                }
            }

        } else if ("readObject".equals(getMethodName()) && "(Ljava/io/ObjectInputStream;)V".equals(getMethodSig())
                && isSerializable) {
            sawReadObject = true;
            if (!obj.isPrivate()) {
                bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", isExternalizable ? NORMAL_PRIORITY : HIGH_PRIORITY)
                        .addClassAndMethod(this));
            }

        } else if ("readObjectNoData".equals(getMethodName()) && "()V".equals(getMethodSig()) && isSerializable) {

            if (!obj.isPrivate()) {
                bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", isExternalizable ? NORMAL_PRIORITY : HIGH_PRIORITY)
                        .addClassAndMethod(this));
            }

        } else if ("writeObject".equals(getMethodName()) && "(Ljava/io/ObjectOutputStream;)V".equals(getMethodSig())
                && isSerializable) {
            sawWriteObject = true;
            if (!obj.isPrivate()) {
                bugReporter.reportBug(new BugInstance(this, "SE_METHOD_MUST_BE_PRIVATE", isExternalizable ? NORMAL_PRIORITY : HIGH_PRIORITY)
                        .addClassAndMethod(this));
            }
        }

        if (isSynchronized) {
            if ("readObject".equals(getMethodName()) && "(Ljava/io/ObjectInputStream;)V".equals(getMethodSig()) && isSerializable) {
                bugReporter.reportBug(new BugInstance(this, "RS_READOBJECT_SYNC", isExternalizable ? LOW_PRIORITY : NORMAL_PRIORITY)
                        .addClassAndMethod(this));
            } else if ("writeObject".equals(getMethodName()) && "(Ljava/io/ObjectOutputStream;)V".equals(getMethodSig())
                    && isSerializable) {
                writeObjectIsSynchronized = true;
            } else {
                foundSynchronizedMethods = true;
            }
        }
        super.visit(obj);

    }

    boolean isSynthetic(FieldOrMethod obj) {
        Attribute[] a = obj.getAttributes();
        for (Attribute aA : a) {
            if (aA instanceof Synthetic) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void visit(Code obj) {
        if (isSerializable) {
            super.visit(obj);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if ("readExternal".equals(getMethodName())) {
            if (seen == Const.IFEQ || seen == Const.IFNE) {
                initializedCheckerVariable = Optional.ofNullable(stack.getStackItem(0).getXField());
            } else if (seen == Const.ATHROW || seen == Const.RETURN) {
                sawReadExternalExit = true;
            }
        }

        if (seen == Const.PUTFIELD) {
            XField xField = getXFieldOperand();
            if (xField != null && xField.getClassDescriptor().equals(getClassDescriptor())) {
                Item first = stack.getStackItem(0);

                boolean isPutOfDefaultValue = first.isNull(); // huh?? ||
                // first.isInitialParameter();
                if (!isPutOfDefaultValue && first.getConstant() != null && !first.isArray()) {
                    Object constant = first.getConstant();
                    if (constant instanceof Number && ((Number) constant).intValue() == 0 || constant.equals(Boolean.FALSE)) {
                        isPutOfDefaultValue = true;
                    }
                }

                if (isPutOfDefaultValue) {
                    if (Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                        transientFieldsSetToDefaultValueInConstructor.add(xField);
                    }
                } else {
                    String nameOfField = getNameConstantOperand();

                    if (transientFieldsUpdates.containsKey(xField)) {
                        if (Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                            transientFieldsSetInConstructor.add(xField);
                        } else {
                            transientFieldsUpdates.put(xField, transientFieldsUpdates.get(xField) + 1);
                        }
                    } else if (fieldsThatMightBeAProblem.containsKey(nameOfField)) {
                        try {

                            JavaClass classStored = first.getJavaClass();
                            if (classStored == null) {
                                return;
                            }
                            double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(classStored);
                            if (isSerializable <= 0.2) {
                                XField f = fieldsThatMightBeAProblem.get(nameOfField);

                                String sig = f.getSignature();
                                // System.out.println("Field signature: " +
                                // sig);
                                // System.out.println("Class stored: " +
                                // classStored.getClassName());
                                String genSig = "L" + classStored.getClassName().replace('.', '/') + ";";
                                if (!sig.equals(genSig)) {
                                    double bias = 0.0;
                                    if (!Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                                        bias = 1.0;
                                    }
                                    int priority = computePriority(isSerializable, bias);

                                    fieldWarningList.add(new BugInstance(this, "SE_BAD_FIELD_STORE", priority)
                                            .addClass(getThisClass().getClassName()).addField(f).addType(genSig)
                                            .describe("TYPE_FOUND").addSourceLine(this));
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            // ignore it
                        }
                    }

                    if ("readExternal".equals(getMethodName())) {
                        BugInstance bug = new BugInstance(this, "SE_PREVENT_EXT_OBJ_OVERWRITE", LOW_PRIORITY)
                                .addClassAndMethod(this)
                                .addField(xField)
                                .addSourceLine(this);
                        // Collect the bugs and report them later, if the initializedCheckerVariable's value won't be changed
                        optionalBugsInReadExternal.put(xField, bug);
                        if (!initializedCheckerVariable.isPresent() || sawReadExternalExit) {
                            bugReporter.reportBug(bug);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(Field obj) {
        int flags = obj.getAccessFlags();
        String genericSignature = obj.getGenericSignature();
        if (genericSignature != null && genericSignature.startsWith("T")) {
            return;
        }
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
        Item summary = fieldSummary.getSummary(getXField());
        String fieldSig = summary.getSignature();

        if (isEjbImplClass) {
            ClassDescriptor fieldType = DescriptorFactory.createClassDescriptorFromFieldSignature(fieldSig);
            if (fieldType != null) {
                if (Subtypes2.instanceOf(fieldType, "javax.ejb.SessionContext")
                        || Subtypes2.instanceOf(fieldType, "jakarta.ejb.SessionContext")
                        || Subtypes2.instanceOf(fieldType, "javax.transaction.UserTransaction")
                        || Subtypes2.instanceOf(fieldType, "jakarta.transaction.UserTransaction")
                        || Subtypes2.instanceOf(fieldType, "javax.ejb.EJBHome")
                        || Subtypes2.instanceOf(fieldType, "jakarta.ejb.EJBHome")
                        || Subtypes2.instanceOf(fieldType, "javax.ejb.EJBObject")
                        || Subtypes2.instanceOf(fieldType, "jakarta.ejb.EJBObject")
                        || Subtypes2.instanceOf(fieldType, "javax.naming.Context")
                        || Subtypes2.instanceOf(fieldType, "jakarta.naming.Context")) {
                    if (testingEnabled && obj.isTransient()) {
                        bugReporter.reportBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClass(this)
                                .addVisitedField(this)
                                .addString("EJB implementation classes should not have fields of this type"));
                    }
                    return;
                }
            }
        }

        if (obj.isTransient()) {
            if (isSerializable && !isExternalizable) {
                seenTransientField = true;
                transientFieldsUpdates.put(getXField(), 0);
            } else if (reportTransientFieldOfNonSerializableClass) {
                bugReporter.reportBug(new BugInstance(this, "SE_TRANSIENT_FIELD_OF_NONSERIALIZABLE_CLASS", NORMAL_PRIORITY)
                        .addClass(this).addVisitedField(this));
            }
        } else if (getClassName().indexOf("ObjectStreamClass") == -1 && isSerializable && !isExternalizable
                && fieldSig.indexOf('L') >= 0 && !obj.isTransient() && !obj.isStatic()) {
            if (DEBUG) {
                System.out.println("Examining non-transient field with name: " + getFieldName() + ", sig: " + fieldSig);
            }
            XField xfield = getXField();
            Type type = TypeFrameModelingVisitor.getType(xfield);
            if (type instanceof ReferenceType) {
                try {
                    ReferenceType rtype = (ReferenceType) type;

                    double isSerializable = DeepSubtypeAnalysis.isDeepSerializable(rtype);
                    if (DEBUG) {
                        System.out.println("  isSerializable: " + isSerializable);
                    }
                    if (isSerializable < 1.0) {
                        fieldsThatMightBeAProblem.put(obj.getName(), xfield);
                    }
                    if (isSerializable < 0.9) {
                        ReferenceType problemType = DeepSubtypeAnalysis.getLeastSerializableTypeComponent(rtype);

                        // Priority is LOW for GUI classes (unless explicitly marked
                        // Serializable),
                        // HIGH if the class directly implements Serializable,
                        // NORMAL otherwise.
                        int priority = computePriority(isSerializable, 0);
                        if (!strongEvidenceForIntendedSerialization()) {
                            if (obj.getName().startsWith("this$")) {
                                priority = Math.max(priority, NORMAL_PRIORITY);
                            }
                            if (innerClassHasOuterInstance) {
                                if (isAnonymousInnerClass) {
                                    priority += 2;
                                } else {
                                    priority += 1;
                                }
                            }
                            if (isGUIClass || isEjbImplClass || isJSPClass) {
                                priority++;
                            }
                        } else if (isGUIClass || isEjbImplClass || isJSPClass) {
                            priority = Math.max(priority, NORMAL_PRIORITY);
                        }
                        if (DEBUG) {
                            System.out.println("SE_BAD_FIELD: " + getThisClass().getClassName() + " " + obj.getName() + " "
                                    + isSerializable + " " + implementsSerializableDirectly + " " + sawSerialVersionUID + " "
                                    + isGUIClass + " " + isEjbImplClass);
                            // Report is queued until after the entire class has been
                            // seen.
                        }

                        if ("this$0".equals(obj.getName())) {
                            fieldWarningList.add(new BugInstance(this, "SE_BAD_FIELD_INNER_CLASS", priority).addClass(getThisClass()
                                    .getClassName()));
                        } else if (isSerializable < 0.9) {
                            fieldWarningList.add(new BugInstance(this, "SE_BAD_FIELD", priority)
                                    .addClass(getThisClass().getClassName())
                                    .addField(xfield).addType(problemType)
                                    .describe("TYPE_FOUND"));
                        }
                    } else if (!isGUIClass && !isEjbImplClass && !isJSPClass && "this$0".equals(obj.getName())) {
                        fieldWarningList.add(new BugInstance(this, "SE_INNER_CLASS", implementsSerializableDirectly ? NORMAL_PRIORITY
                                : LOW_PRIORITY).addClass(getThisClass().getClassName()));
                    }
                } catch (ClassNotFoundException e) {
                    if (DEBUG) {
                        System.out.println("Caught ClassNotFoundException");
                    }
                    bugReporter.reportMissingClass(e);
                }
            }
        }

        if (!getFieldName().startsWith("this") && isSynthetic(obj)) {
            foundSynthetic = true;
        }
        if (!"serialVersionUID".equals(getFieldName())) {
            return;
        }
        int mask = Const.ACC_STATIC | Const.ACC_FINAL;
        if (!"I".equals(fieldSig) && !"J".equals(fieldSig)) {
            return;
        }
        if ((flags & mask) == mask && "I".equals(fieldSig)) {
            bugReporter.reportBug(new BugInstance(this, "SE_NONLONG_SERIALVERSIONID", LOW_PRIORITY).addClass(this)
                    .addVisitedField(this));
            sawSerialVersionUID = true;
            return;
        } else if ((flags & Const.ACC_STATIC) == 0) {
            bugReporter.reportBug(new BugInstance(this, "SE_NONSTATIC_SERIALVERSIONID", NORMAL_PRIORITY).addClass(this)
                    .addVisitedField(this));
            return;
        } else if ((flags & Const.ACC_FINAL) == 0) {
            bugReporter.reportBug(new BugInstance(this, "SE_NONFINAL_SERIALVERSIONID", NORMAL_PRIORITY).addClass(this)
                    .addVisitedField(this));
            return;
        }
        sawSerialVersionUID = true;
    }

    private int computePriority(double isSerializable, double bias) {
        int priority = (int) (1.9 + isSerializable * 3 + bias);

        if (strongEvidenceForIntendedSerialization()) {
            priority--;
        } else if (sawSerialVersionUID && priority > NORMAL_PRIORITY) {
            priority--;
        } else {
            priority = Math.max(priority, NORMAL_PRIORITY);
        }
        return priority;
    }

}
