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
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiMap;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FindImproperSynchronization extends OpcodeStackDetector {
    private static final Map<MethodDescriptor, Integer> WRAPPER_IMPLEMENTATIONS = new HashMap<>();
    private static final Set<String> IMMUTABLE_RETURNERS = new HashSet<>();

    static {
        String juCollections = ClassName.toSlashedClassName(java.util.Collections.class);
        String juMaps = ClassName.toSlashedClassName(Map.class);
        String juLists = ClassName.toSlashedClassName(java.util.List.class);

        //synchronized collections
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedCollection",
                "(Ljava/util/Collection;)Ljava/util/Collection;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedList",
                "(Ljava/util/List;)Ljava/util/List;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedMap",
                "(Ljava/util/Map;)Ljava/util/Map;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedSortedMap",
                "(Ljava/util/SortedMap;)Ljava/util/SortedMap;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedNavigableMap",
                "(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedSet",
                "(Ljava/util/Set;)Ljava/util/Set;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedSortedSet",
                "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", true), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "synchronizedNavigableSet",
                "(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", true), 0);
        // unmodifiable collections
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "unmodifiableCollection",
                "(Ljava/util/Collection;)Ljava/util/Collection;", true), 0);
        // checked collections
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedCollection",
                "(Ljava/util/Collection;Ljava/lang/Class;)Ljava/util/Collection;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedList",
                "(Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedMap",
                "(Ljava/util/Map;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/Map;", true), 2);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedSortedMap",
                "(Ljava/util/SortedMap;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/SortedMap;", true), 2);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedNavigableMap",
                "(Ljava/util/NavigableMap;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/NavigableMap;", true), 2);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedSet",
                "(Ljava/util/Set;Ljava/lang/Class;)Ljava/util/Set;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedSortedSet",
                "(Ljava/util/SortedSet;Ljava/lang/Class;)Ljava/util/SortedSet;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedNavigableSet",
                "(Ljava/util/NavigableSet;Ljava/lang/Class;)Ljava/util/NavigableSet;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juCollections, "checkedQueue",
                "(Ljava/util/Queue;Ljava/lang/Class;)Ljava/util/Queue;", true), 1);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juMaps, "keySet", "()Ljava/util/Set;", false), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juMaps, "entrySet", "()Ljava/util/Set;", false), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juMaps, "values", "()Ljava/util/Collection;", false), 0);
        WRAPPER_IMPLEMENTATIONS.put(new MethodDescriptor(juLists, "subList", "(II)Ljava/util/List;", false), 2);

        IMMUTABLE_RETURNERS.addAll(Arrays.asList("Ljava/util/Arrays$ArrayList;",
                "Ljava/util/Collections$UnmodifiableList;",
                "Ljava/util/Collections$UnmodifiableMap;",
                "Ljava/util/Collections$UnmodifiableSortedMap;",
                "Ljava/util/Collections$UnmodifiableSet;",
                "Ljava/util/Collections$UnmodifiableNavigableMap;",
                "Ljava/util/Collections$UnmodifiableSortedSet;",
                "Ljava/util/Collections$UnmodifiableNavigableSet;"));
    }

    private final BugReporter bugReporter;
    private final Set<XMethod> exposingMethods = new HashSet<>();
    private final Set<XMethod> synchronizedMethods = new HashSet<>();
    private final Map<XField, XMethod> declaredLockObjects = new HashMap<>();
    private final Map<XField, XMethod> inheritedLockObjects = new HashMap<>();
    private final MultiMap<XField, XMethod> declaredLockAccessors = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> lockAccessorsInHierarchy = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> lockUsingMethodsInHierarchy = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> declaredInheritedFieldAccessingMethods = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> potentialObjectBugContainingMethods = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> potentialInheritedBugContainingMethods = new MultiMap<>(HashSet.class);
    // Backing collections
    private final Set<XField> badBackingCollections = new HashSet<>();
    private final Set<XField> inheritableBackingCollections = new HashSet<>();
    private final MultiMap<XField, BugInfo> declaredCollectionLockObjects = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, BugInfo> inheritedCollectionLockObjects = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> declaredCollectionAccessors = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XMethod> collectionAccessorsInHierarchy = new MultiMap<>(HashSet.class);
    private final MultiMap<XField, XField> backingCollections = new MultiMap<>(HashSet.class);

    public FindImproperSynchronization(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private static boolean isWrapperImplementation(MethodDescriptor methodDescriptor) {
        return WRAPPER_IMPLEMENTATIONS.containsKey(methodDescriptor);
    }

    private static Integer getStackOffset(MethodDescriptor methodDescriptor) {
        return WRAPPER_IMPLEMENTATIONS.get(methodDescriptor);
    }

    private static boolean isImmutableReturner(String signature) {
        return IMMUTABLE_RETURNERS.contains(signature);
    }

    @Override
    public void visit(Method obj) {
        try {
            analyzeAssignments(obj, getClassContext());
        } catch (CFGBuilderException e) {
            AnalysisContext.logError("Error building CFG for " + obj, e);
        }

        if (isInitializerMethod(obj.getName())) {
            return;
        }

        XMethod xMethod = getXMethod();
        if (xMethod.isPublic() && xMethod.isSynchronized()) {
            if (xMethod.isStatic()) {
                bugReporter.reportBug(new BugInstance(
                        this, "US_UNSAFE_STATIC_METHOD_SYNCHRONIZATION", NORMAL_PRIORITY)
                        .addClassAndMethod(this));
            }
            synchronizedMethods.add(xMethod);
        }

        if (!(xMethod.isStatic() || xMethod.isPublic())) {
            return;
        }

        if ("readResolve".equals(obj.getName()) && "()Ljava/lang/Object;".equals(obj.getSignature())
                && Subtypes2.instanceOf(getThisClass(), "java.io.Serializable")) {
            return;
        }

        SignatureParser signature = new SignatureParser(xMethod.getSignature());
        String returnType = signature.getReturnTypeSignature();
        String javaStyleClassType = "L" + getClassName() + ";";
        if (returnType.equals(javaStyleClassType)) {
            if ("clone".equals(obj.getName())) {
                return;
            }
            exposingMethods.add(xMethod);
        } else {
            String sourceSig = xMethod.getSourceSignature();
            if (sourceSig != null) {
                String typePattern = "^.*<.*" + javaStyleClassType + ".*>;$";
                GenericSignatureParser genericSignature = new GenericSignatureParser(sourceSig);
                String genericReturnValue = genericSignature.getReturnTypeSignature();
                if (genericReturnValue.matches(typePattern)) {
                    exposingMethods.add(xMethod);
                }
            }
        }

    }

    @Override
    public void visit(Field obj) {
        XField xField = getXField();
        if (isCollection(xField)) {
            if (isInherited(xField)) {
                if (!xField.isPrivate()) {
                    badBackingCollections.add(xField);
                }
                try {
                    findExposureInHierarchy(xField).forEach(method -> collectionAccessorsInHierarchy.add(xField,
                            method));
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            } else {
                if (xField.isPublic()) {
                    badBackingCollections.add(xField);
                }
                if (xField.isProtected() && !getThisClass().isFinal() || isPackagePrivate(xField)) {
                    inheritableBackingCollections.add(xField);
                }
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER && stack.getStackDepth() > 0) {
            XField lock = stack.getStackItem(0).getXField();

            if (lock != null) {
                XMethod xMethod = getXMethod();
                if (isInherited(lock)) {
                    inheritedLockObjects.put(lock, xMethod);
                    if (isCollection(lock)) {
                        inheritedCollectionLockObjects.add(lock, new BugInfo(this));
                        analyzeAssignmentsInHierarchy(lock);
                    }

                    try {
                        findExposureInHierarchy(lock).forEach(method -> lockAccessorsInHierarchy.add(lock, method));
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }

                    if (lock.isProtected() || lock.isPublic() || !lock.isPrivate()) {
                        potentialObjectBugContainingMethods.add(lock, xMethod);
                    }
                } else {
                    declaredLockObjects.put(lock, xMethod);
                    if (isCollection(lock)) {
                        declaredCollectionLockObjects.add(lock, new BugInfo(this));
                    }

                    if (lock.isPublic()) {
                        potentialObjectBugContainingMethods.add(lock, xMethod);
                    }

                    if (lock.isProtected() && !getThisClass().isFinal() || isPackagePrivate(lock)) {
                        potentialInheritedBugContainingMethods.add(lock, xMethod);
                    }
                }
            }
        }

        if ((seen == Const.PUTSTATIC || seen == Const.PUTFIELD) && !isInitializerMethod(getMethodName())) {
            XMethod updateMethod = getXMethod();
            XField updatedField = getXFieldOperand();

            if (updatedField == null) {
                return;
            }

            checkLockUsageInHierarchy(updatedField, updateMethod);

            if (updateMethod.isPublic() || updateMethod.isProtected()) {
                // backing collections
                if (isCollection(updatedField)) {
                    declaredCollectionAccessors.add(updatedField, updateMethod);
                }

                if (updatedField.isPublic()) {
                    return;
                }
                declaredLockAccessors.add(updatedField, updateMethod);
            }
        }

        if (seen == Const.ARETURN) {
            OpcodeStack.Item returnValue = stack.getStackItem(0);
            XField returnedField = returnValue.getXField();

            if (returnedField == null) {
                return;
            }

            XMethod exposingMethod = getXMethod();
            checkLockUsageInHierarchy(returnedField, exposingMethod);

            // backing collections
            if (isCollection(returnedField)) {
                declaredCollectionAccessors.add(returnedField, exposingMethod);

            }

            if (returnedField.isPublic()) {
                return;
            }

            declaredLockAccessors.add(returnedField, exposingMethod);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (!exposingMethods.isEmpty()) {
            String exposingMethodsMessage = buildMethodsMessage(exposingMethods);
            for (XMethod synchronizedMethod : synchronizedMethods) {
                bugReporter.reportBug(new BugInstance(
                        this, "US_UNSAFE_METHOD_SYNCHRONIZATION", NORMAL_PRIORITY)
                        .addClassAndMethod(synchronizedMethod)
                        .addString(exposingMethodsMessage));
            }
        }

        for (XField lock : declaredLockObjects.keySet()) {
            if (!declaredLockAccessors.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, declaredLockAccessors.get(lock), declaredLockObjects);
            } else if (!potentialInheritedBugContainingMethods.get(lock).isEmpty()) {
                reportInheritedObjectBugs(lock, potentialInheritedBugContainingMethods.get(lock));
            } else if (!potentialObjectBugContainingMethods.get(lock).isEmpty()) {
                reportObjectBugs(lock, potentialObjectBugContainingMethods.get(lock));
            }
        }

        for (XField lock : inheritedLockObjects.keySet()) {
            if (!declaredLockAccessors.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, declaredLockAccessors.get(lock), inheritedLockObjects);
            } else if (!lockAccessorsInHierarchy.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, lockAccessorsInHierarchy.get(lock), inheritedLockObjects);
            } else if (!potentialObjectBugContainingMethods.get(lock).isEmpty()) {
                reportObjectBugs(lock, potentialObjectBugContainingMethods.get(lock));
            }
        }

        for (XField lock : lockUsingMethodsInHierarchy.keySet()) {
            if (!declaredInheritedFieldAccessingMethods.get(lock).isEmpty()) {
                reportExposingLockObjectBugs(lock, lockUsingMethodsInHierarchy.get(lock), declaredInheritedFieldAccessingMethods.get(lock));
            }
        }

        // backing collections
        for (XField lock : declaredCollectionLockObjects.keySet()) {
            for (XField backingCollection : backingCollections.get(lock)) {
                if (declaredCollectionAccessors.containsKey(backingCollection)) {
                    reportAccessibleBackingCollectionBug(lock, declaredCollectionLockObjects, backingCollection,
                            declaredCollectionAccessors);
                } else if (inheritableBackingCollections.contains(backingCollection)) {
                    reportInheritedBackingCollectionBug(lock, declaredCollectionLockObjects, backingCollection);
                } else if (badBackingCollections.contains(backingCollection)) {
                    reportBadCollectionObjectBugs(lock, declaredCollectionLockObjects, backingCollection);
                }
            }
        }

        for (XField lock : inheritedCollectionLockObjects.keySet()) {
            for (XField backingCollection : backingCollections.get(lock)) {
                if (declaredCollectionAccessors.containsKey(backingCollection)) {
                    reportAccessibleBackingCollectionBug(lock, inheritedCollectionLockObjects, backingCollection,
                            declaredCollectionAccessors);
                } else if (collectionAccessorsInHierarchy.containsKey(backingCollection)) {
                    reportAccessibleBackingCollectionBug(lock, inheritedCollectionLockObjects, backingCollection,
                            collectionAccessorsInHierarchy);
                } else if (badBackingCollections.contains(backingCollection)) {
                    reportBadCollectionObjectBugs(lock, declaredCollectionLockObjects, backingCollection);
                }
            }
        }

        clearState();
    }

    private boolean isCollection(Type type) {
        if (type instanceof ReferenceType) {
            ReferenceType rType = (ReferenceType) type;
            try {
                return Subtypes2.isContainer(rType);
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
        }
        return false;
    }

    private boolean isCollection(XField field) {
        return isCollection(TypeFrameModelingVisitor.getType(field));
    }

    private boolean isMethodCall(Instruction instruction) {
        return (instruction instanceof INVOKESTATIC)
                || (instruction instanceof INVOKESPECIAL)
                || (instruction instanceof INVOKEVIRTUAL)
                || (instruction instanceof INVOKEINTERFACE);
    }

    private boolean isPackagePrivate(XField field) {
        return !(field.isPublic() || field.isProtected() || field.isPrivate());
    }

    private boolean isInitializerMethod(String methodName) {
        return Const.CONSTRUCTOR_NAME.equals(methodName) || Const.STATIC_INITIALIZER_NAME.equals(methodName);
    }

    /**
     * Check if the lock object is inherited from the current class.
     * To ensure no false positives, we compare only the outer class name if the lock object is contained in an inner
     * class.
     * Only checking equality is enough, since the lock object is a field declared in a class. That means if
     * the declaring class of the lock object is the same as the current class, the lock object is not inherited.
     *
     * @param lockObject the lock object to check for inheritance
     * @return true if the lock object is inherited from the current class, false otherwise
     */
    private boolean isInherited(XField lockObject) {
        return !getDottedClassName().equals(lockObject.getClassName().split("\\$")[0]);
    }

    /**
     * Analyze a wrapping method call and check if this the creation of a binding collection.
     * There are specific methods that use their parameters to create a new collections,
     * but they maintain a reference to the original collection.
     * When a collection is wrapped by such a method and the result is assigned to a field, the wrapped collections becomes a backing collection.
     *
     * @param currentMethod the method in which the wrapping method call is located
     * @param classContext the class context of the currently analyzed class, in which the wrapping method call is located
     * @param location the location of the wrapping method call
     * @param handle the instruction handle of the wrapping method call
     * @param assignedField the field to which the result of the wrapping method call is assigned
     * @param stackOffsetForWrapperMethodParameter the stack offset of the wrapped collection parameter of the wrapping method call
     */
    private void analyzeWrappedField(Method currentMethod, ClassContext classContext, Location location,
            InstructionHandle handle, XField assignedField, Integer stackOffsetForWrapperMethodParameter) {
        InstructionHandle prevHandle = location.getBasicBlock().getPredecessorOf(handle);
        Instruction prevInstruction = prevHandle.getInstruction();
        if (isMethodCall(prevInstruction)) {
            OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), currentMethod, prevHandle.getPosition());
            if (stack.getStackDepth() > stackOffsetForWrapperMethodParameter) {
                XField wrappedField = stack
                        .getStackItem(stackOffsetForWrapperMethodParameter)
                        .getXField();
                if (wrappedField != null && isCollection(wrappedField)) {
                    backingCollections.add(assignedField, wrappedField);
                    backingCollections.get(wrappedField)
                            .forEach(f -> backingCollections.add(assignedField, f));
                }
            }
        }
    }

    private void analyzeAssignments(Method obj, ClassContext classContext) throws CFGBuilderException {
        analyzeAssignments(obj, classContext, null);
    }

    private void analyzeAssignments(Method obj, ClassContext classContext, XField specificField) throws CFGBuilderException {
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        for (Location location : classContext.getCFG(obj).orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                XField xfield = Hierarchy.findXField((FieldInstruction) instruction, cpg);

                if (specificField != null && !specificField.equals(xfield)) {
                    continue;
                }

                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, handle.getPosition());
                if (stack.getStackDepth() > 0) {
                    OpcodeStack.Item item = stack.getStackItem(0);
                    XMethod returnValueOf = item.getReturnValueOf();
                    XField assignedField = item.getXField();

                    if (returnValueOf != null) {
                        if (item.isNewlyAllocated()) {
                            continue;
                        }

                        if (isWrapperImplementation(returnValueOf.getMethodDescriptor())) {
                            analyzeWrappedField(obj, classContext, location, handle, xfield,
                                    getStackOffset(returnValueOf.getMethodDescriptor()));
                        }
                    } else if (assignedField != null) {
                        if (isCollection(assignedField)) {
                            backingCollections.add(xfield, assignedField);
                            backingCollections.get(assignedField)
                                    .forEach(f -> backingCollections.add(xfield, f));
                        }
                    } else if (isImmutableReturner(item.getSignature())) {
                        analyzeWrappedField(obj, classContext, location, handle, xfield, 0);
                    }
                }
            }

        }
    }

    private void analyzeAssignmentsInHierarchy(XField collectionLockObject) {
        String lockDeclaringClassName = collectionLockObject.getClassName();
        Method[] ownMethods = getThisClass().getMethods();
        JavaClass[] superClasses;

        try {
            superClasses = getThisClass().getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return;
        }

        for (JavaClass declaringClass : superClasses) {
            if (!lockDeclaringClassName.equals(ClassName.toDottedClassName(declaringClass.getClassName()))) {
                continue;
            }

            for (Method method : declaringClass.getMethods()) {
                if (ownMethods.length > 0 && Arrays.asList(ownMethods).contains(method) || method.getCode() == null) {
                    continue;
                }

                ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
                try {
                    analyzeAssignments(method, classContext, collectionLockObject);
                } catch (CFGBuilderException e) {
                    AnalysisContext.logError("Error building CFG", e);
                }
            }
        }
    }

    private void checkLockUsageInHierarchy(XField field, XMethod exposingMethod) {
        if (isInherited(field) && declaredInheritedFieldAccessingMethods.get(field).isEmpty()) {
            try {
                findLockUsageInHierarchy(field).forEach(m -> lockUsingMethodsInHierarchy.add(field, m));
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
            declaredInheritedFieldAccessingMethods.add(field, exposingMethod);
        }
    }

    private Set<XMethod> findExposureInHierarchy(XField lockObject) throws ClassNotFoundException {
        Set<XMethod> unsafeMethods = new HashSet<>();
        Method[] ownMethods = getThisClass().getMethods();

        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            for (Method possibleAccessorMethod : declaringClass.getMethods()) {
                if (possibleAccessorMethod.getCode() == null || Arrays.asList(ownMethods).contains(possibleAccessorMethod)
                        || isInitializerMethod(possibleAccessorMethod.getName())) {
                    continue;
                }

                try {
                    ClassContext classContext = new ClassContext(declaringClass,
                            AnalysisContext.currentAnalysisContext());
                    ConstantPoolGen cpg = classContext.getConstantPoolGen();

                    for (Location location : classContext.getCFG(possibleAccessorMethod).orderedLocations()) {
                        InstructionHandle handle = location.getHandle();
                        Instruction instruction = handle.getInstruction();

                        if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                            FieldInstruction fieldInstruction = (FieldInstruction) instruction;
                            XField xfield = Hierarchy.findXField(fieldInstruction, cpg);

                            if (xfield != null && xfield.equals(lockObject) && !xfield.isPublic()) {
                                XMethod unsafeXMethod = XFactory.createXMethod(declaringClass, possibleAccessorMethod);
                                unsafeMethods.add(unsafeXMethod);
                            }
                        }

                        if (instruction instanceof ARETURN) {
                            OpcodeStack currentStack = OpcodeStackScanner
                                    .getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle.getPosition());
                            if (currentStack.getStackDepth() > 0) {
                                XField xField = currentStack.getStackItem(0).getXField();

                                if (xField != null && xField.equals(lockObject) && !xField.isPublic()) {
                                    XMethod unsafeXMethod = XFactory.createXMethod(declaringClass, possibleAccessorMethod);
                                    unsafeMethods.add(unsafeXMethod);
                                }
                            }
                        }

                    }
                } catch (CFGBuilderException e) {
                    AnalysisContext.logError("Error analyzing method", e);
                }
            }
        }
        return unsafeMethods;
    }

    private Set<XMethod> findLockUsageInHierarchy(XField field) throws ClassNotFoundException {
        Set<XMethod> methodsUsingFieldAsLock = new HashSet<>();
        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            for (Method possibleAccessorMethod : declaringClass.getMethods()) {

                if (possibleAccessorMethod.getCode() == null || isInitializerMethod(possibleAccessorMethod.getName())) {
                    continue;
                }

                try {
                    ClassContext classContext = new ClassContext(declaringClass,
                            AnalysisContext.currentAnalysisContext());
                    CFG cfg = classContext.getCFG(possibleAccessorMethod);

                    for (Location location : cfg.orderedLocations()) {
                        InstructionHandle handle = location.getHandle();
                        Instruction instruction = handle.getInstruction();

                        if (instruction instanceof MONITORENTER) {
                            OpcodeStack currentStack = OpcodeStackScanner
                                    .getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle.getPosition());
                            if (currentStack.getStackDepth() > 0) {
                                OpcodeStack.Item lock = currentStack.getStackItem(0);
                                XField lockObject = lock.getXField();

                                if (lockObject != null && lockObject.equals(field)) {
                                    methodsUsingFieldAsLock.add(XFactory.createXMethod(declaringClass,
                                            possibleAccessorMethod));
                                }
                            }
                        }
                    }

                } catch (CFGBuilderException e) {
                    AnalysisContext.logError("Error building CFG", e);
                }
            }
        }

        return methodsUsingFieldAsLock;
    }

    private String buildMethodsMessage(Collection<XMethod> methods) {
        return methods.stream().sorted(Comparator.comparing(ClassMember::getName)).map(Object::toString).collect(Collectors.joining(",\n"));
    }

    private void reportExposingLockObjectBugs(XField lock, Collection<XMethod> lockUsingMethods,
            Collection<XMethod> exposingMethods) {
        String exposingMethodsMessage = buildMethodsMessage(exposingMethods);
        for (XMethod lockUsingMethod : lockUsingMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(lockUsingMethod)
                    .addField(lock)
                    .addString(exposingMethodsMessage));
        }
    }

    private void reportAccessibleObjectBug(XField lock, Collection<XMethod> definedLockAccessors, Map<XField, XMethod> synchronizedMethods) {
        if (!definedLockAccessors.isEmpty()) {
            String problematicMethods = buildMethodsMessage(definedLockAccessors);
            bugReporter.reportBug(new BugInstance(
                    this, "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethods.get(lock))
                    .addField(lock)
                    .addString(problematicMethods));
        }
    }

    private void reportInheritedObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION", LOW_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethod)
                    .addField(lock));
        }
    }

    private void reportObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "US_UNSAFE_OBJECT_SYNCHRONIZATION", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethod)
                    .addField(lock));
        }
    }

    private void reportAccessibleBackingCollectionBug(XField lock, MultiMap<XField, BugInfo> possibleBugs,
            XField backingCollection,
            MultiMap<XField, XMethod> collectionAccessors) {
        if (collectionAccessors.containsKey(backingCollection)) {
            String exposingMethodsMessage = buildMethodsMessage(collectionAccessors.get(backingCollection));
            for (BugInfo bugInfo : possibleBugs.get(lock)) {
                bugReporter.reportBug(bugInfo
                        .createBugInstance(this,
                                "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION",
                                lock, backingCollection, exposingMethodsMessage));
            }
        }
    }

    private void reportInheritedBackingCollectionBug(XField lock, MultiMap<XField, BugInfo> possibleBugs,
            XField backingCollection) {
        for (BugInfo bugInfo : possibleBugs.get(lock)) {
            bugReporter.reportBug(bugInfo
                    .createBugInstance(this,
                            "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION",
                            lock, backingCollection));
        }
    }

    private void reportBadCollectionObjectBugs(XField lock, MultiMap<XField, BugInfo> possibleBugs,
            XField backingCollection) {
        for (BugInfo bugInfo : possibleBugs.get(lock)) {
            bugReporter.reportBug(bugInfo
                    .createBugInstance(this,
                            "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION",
                            lock, backingCollection));
        }
    }

    private void clearState() {
        exposingMethods.clear();
        synchronizedMethods.clear();
        declaredLockObjects.clear();
        inheritedLockObjects.clear();
        declaredLockAccessors.clear();
        lockAccessorsInHierarchy.clear();
        lockUsingMethodsInHierarchy.clear();
        declaredInheritedFieldAccessingMethods.clear();
        potentialObjectBugContainingMethods.clear();
        potentialInheritedBugContainingMethods.clear();

        // Backing collections
        declaredCollectionLockObjects.clear();
        inheritedCollectionLockObjects.clear();
        declaredCollectionAccessors.clear();
        collectionAccessorsInHierarchy.clear();
        backingCollections.clear();
        badBackingCollections.clear();
        inheritableBackingCollections.clear();
    }

    private static class BugInfo {
        private final XMethod inMethod;
        private final SourceLineAnnotation sourceLineAnnotation;

        private BugInfo(BytecodeScanningDetector detector) {
            this.inMethod = detector.getXMethod();
            this.sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(detector);
        }

        private BugInstance createBugInstance(BytecodeScanningDetector detector, String bugType, XField lock, XField assignedBackingCollection,
                String... extraMessages) {
            BugInstance bugInstance = new BugInstance(detector, bugType, NORMAL_PRIORITY)
                    .addClassAndMethod(inMethod)
                    .addField(lock)
                    .addReferencedField(FieldAnnotation.fromXField(assignedBackingCollection))
                    .addSourceLine(sourceLineAnnotation);
            for (String message : extraMessages) {
                bugInstance.addString(message);
            }

            return bugInstance;
        }
    }

}
