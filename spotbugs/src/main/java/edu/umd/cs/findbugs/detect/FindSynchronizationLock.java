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

public class FindSynchronizationLock extends OpcodeStackDetector {
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
    private final Set<XMethod> exposingMethods;
    private final Set<XMethod> synchronizedMethods;
    private final Map<XField, XMethod> declaredLockObjects;
    private final Map<XField, XMethod> inheritedLockObjects;
    private final MultiMap<XField, XMethod> declaredLockAccessors;
    private final MultiMap<XField, XMethod> lockAccessorsInHierarchy;
    private final MultiMap<XField, XMethod> lockUsingMethodsInHierarchy;
    private final MultiMap<XField, XMethod> declaredInheritedFieldAccessingMethods;
    private final MultiMap<XField, XMethod> potentialObjectBugContainingMethods;
    private final MultiMap<XField, XMethod> potentialInheritedBugContainingMethods;
    // Backing collections
    private final Set<XField> badBackingCollections;
    private final Set<XField> inheritableBackingCollections;
    private final MultiMap<XField, BugInfo> declaredCollectionLockObjects;
    private final MultiMap<XField, BugInfo> inheritedCollectionLockObjects;
    private final MultiMap<XField, XMethod> declaredCollectionAccessors;
    private final MultiMap<XField, XMethod> collectionAccessorsInHierarchy;
    private final MultiMap<XField, XField> assignedCollectionFields;
    private final MultiMap<XField, XField> assignedWrappedCollectionFields;
    private final MultiMap<XField, Method> declaredFieldExposingMethods;
    private final MultiMap<XField, XField> collectionsBackedByPublicFields;
    private final MultiMap<XField, XMethod> declaredInheritedCollectionFieldAccessingMethods;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.exposingMethods = new HashSet<>();
        this.synchronizedMethods = new HashSet<>();
        this.declaredLockObjects = new HashMap<>();
        this.inheritedLockObjects = new HashMap<>();
        this.declaredLockAccessors = new MultiMap<>(HashSet.class);
        this.lockAccessorsInHierarchy = new MultiMap<>(HashSet.class);
        this.lockUsingMethodsInHierarchy = new MultiMap<>(HashSet.class);
        this.declaredInheritedFieldAccessingMethods = new MultiMap<>(HashSet.class);
        this.potentialObjectBugContainingMethods = new MultiMap<>(HashSet.class);
        this.potentialInheritedBugContainingMethods = new MultiMap<>(HashSet.class);

        // Backing collections
        this.badBackingCollections = new HashSet<>();
        this.inheritableBackingCollections = new HashSet<>();
        this.declaredCollectionLockObjects = new MultiMap<>(HashSet.class);
        this.inheritedCollectionLockObjects = new MultiMap<>(HashSet.class);
        this.declaredCollectionAccessors = new MultiMap<>(HashSet.class);
        this.collectionAccessorsInHierarchy = new MultiMap<>(HashSet.class);
        this.assignedCollectionFields = new MultiMap<>(HashSet.class);
        this.assignedWrappedCollectionFields = new MultiMap<>(HashSet.class);
        this.declaredFieldExposingMethods = new MultiMap<>(HashSet.class);
        this.collectionsBackedByPublicFields = new MultiMap<>(HashSet.class);
        this.declaredInheritedCollectionFieldAccessingMethods = new MultiMap<>(HashSet.class);
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

        String javaStyleClassType = "L" + getClassName() + ";";
        SignatureParser signature = new SignatureParser(xMethod.getSignature());
        if ("readResolve".equals(obj.getName()) && "()Ljava/lang/Object;".equals(obj.getSignature())
                && Subtypes2.instanceOf(getThisClass(), "java.io.Serializable")) {
            return;
        }

        if (!(xMethod.isStatic() || xMethod.isPublic())) {
            return;
        }

        String returnType = signature.getReturnTypeSignature();
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
            try {
                if (isInherited(xField)) {
                    if (xField.isProtected() || xField.isPublic() || !xField.isPrivate()) {
                        badBackingCollections.add(xField);
                    }

                    findExposureInHierarchy(xField).forEach(method -> collectionAccessorsInHierarchy
                            .add(xField, method));
                } else {
                    if (xField.isPublic()) {
                        badBackingCollections.add(xField);
                    }

                    if (xField.isProtected() && !getThisClass().isFinal()) {
                        inheritableBackingCollections.add(xField);
                    } else if (isPackagePrivate(xField)) {
                        inheritableBackingCollections.add(xField);
                    }
                }
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER && stack.getStackDepth() > 0) {
            OpcodeStack.Item lock = stack.getStackItem(0);
            XField lockObject = lock.getXField();

            if (lockObject != null) {
                XMethod xMethod = getXMethod();
                try {
                    boolean inheritedFromHierarchy = isInherited(lockObject);
                    if (inheritedFromHierarchy) {
                        inheritedLockObjects.put(lockObject, xMethod);
                        if (isCollection(lockObject)) {
                            inheritedCollectionLockObjects.add(lockObject, new BugInfo(this));
                            try {
                                analyzeAssignmentsInHierarchy(lockObject);
                            } catch (CFGBuilderException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        findExposureInHierarchy(lockObject).forEach(method -> lockAccessorsInHierarchy
                                .add(lockObject, method));

                        if (lockObject.isProtected() || lockObject.isPublic() || !lockObject.isPrivate()) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }
                    } else {
                        declaredLockObjects.put(lockObject, xMethod);
                        if (isCollection(lockObject)) {
                            declaredCollectionLockObjects.add(lockObject, new BugInfo(this));
                        }

                        if (lockObject.isPublic()) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }

                        if (lockObject.isProtected() && !getThisClass().isFinal()) {
                            potentialInheritedBugContainingMethods.add(lockObject, xMethod);
                        } else if (isPackagePrivate(lockObject)) {
                            potentialInheritedBugContainingMethods.add(lockObject, xMethod);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
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
        MultiMap<XField, XField> allBackingCollections = assignedCollectionFields;
        for (XField field : assignedWrappedCollectionFields.keySet()) {
            assignedWrappedCollectionFields.get(field).forEach(backingField -> allBackingCollections
                    .add(field, backingField));
        }

        for (XField lock : declaredCollectionLockObjects.keySet()) {
            for (XField backingCollection : allBackingCollections.get(lock)) {
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
            for (XField backingCollection : allBackingCollections.get(lock)) {
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

    private boolean isOwnMethod(Method method, Method[] ownMethods) {
        if (ownMethods.length > 0 && Arrays.asList(ownMethods).contains(method)) {
            return true;
        }

        if (isInitializerMethod(method.getName())) {
            return true;
        }

        return method.getCode() == null;
    }

    private boolean isSameAsLockObject(XField maybeLockObject, XField lockObject) {
        if (lockObject == null) {
            return false;
        }
        return lockObject.equals(maybeLockObject);
    }

    private boolean isInherited(XField lockObject) throws ClassNotFoundException {
        return !getDottedClassName().equals(lockObject.getClassName().split("\\$")[0]);
    }

    private void analyzeWrappedField(Method obj, ClassContext classContext, Location location,
            InstructionHandle handle, XField xfield, Integer stackOffset) {
        InstructionHandle prevHandle = location.getBasicBlock().getPredecessorOf(handle);
        Instruction prevInstruction = prevHandle.getInstruction();
        if (isMethodCall(prevInstruction)) {
            OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, prevHandle.getPosition());
            if (stack.getStackDepth() > stackOffset) {
                XField wrappedField =
                        stack
                                .getStackItem(stackOffset)
                                .getXField();
                if (wrappedField != null && isCollection(wrappedField)) {
                    assignedWrappedCollectionFields.add(xfield, wrappedField);
                    assignedWrappedCollectionFields.get(wrappedField).forEach(f -> assignedWrappedCollectionFields.add(xfield, f));
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
                            assignedCollectionFields.add(xfield, assignedField);
                            assignedCollectionFields.get(assignedField).forEach(f -> assignedCollectionFields.add(xfield,
                                    f));
                        }
                    } else {
                        if (isImmutableReturner(item.getSignature())) {
                            analyzeWrappedField(obj, classContext, location, handle, xfield, 0);
                        }
                    }
                }
            }

        }
    }

    private void analyzeAssignmentsInHierarchy(XField collectionLockObject) throws ClassNotFoundException,
            CFGBuilderException {
        String lockDeclaringClassName = collectionLockObject.getClassName();
        Method[] ownMethods = getThisClass().getMethods();

        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            if (!lockDeclaringClassName.equals(ClassName.toDottedClassName(declaringClass.getClassName()))) {
                continue;
            }

            for (Method method : declaringClass.getMethods()) {
                if (ownMethods.length > 0 && Arrays.asList(ownMethods).contains(method) || method.getCode() == null) {
                    continue;
                }

                ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
                analyzeAssignments(method, classContext, collectionLockObject);
            }
        }
    }

    private void checkLockUsageInHierarchy(XField field, XMethod exposingMethod) {
        try {
            if (isInherited(field)) {
                if (declaredInheritedFieldAccessingMethods.get(field).isEmpty()) {
                    findLockUsageInHierarchy(field).forEach(m -> lockUsingMethodsInHierarchy.add(field, m));
                }
                declaredInheritedFieldAccessingMethods.add(field, exposingMethod);
                if (isCollection(field)) {
                    declaredInheritedCollectionFieldAccessingMethods.add(field, exposingMethod);
                }
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
    }

    private Set<XMethod> findExposureInHierarchy(XField lockObject) throws ClassNotFoundException {
        Set<XMethod> unsafeMethods = new HashSet<>();
        Method[] ownMethods = getThisClass().getMethods();

        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            for (Method possibleAccessorMethod : declaringClass.getMethods()) {

                if (isOwnMethod(possibleAccessorMethod, ownMethods)) {
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

                            if (xfield != null && isSameAsLockObject(xfield, lockObject) && !xfield.isPublic()) {
                                XMethod unsafeXMethod = XFactory.createXMethod(declaringClass, possibleAccessorMethod);
                                unsafeMethods.add(unsafeXMethod);
                            }
                        }

                        if (instruction instanceof ARETURN) {
                            OpcodeStack currentStack = OpcodeStackScanner
                                    .getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle.getPosition());
                            if (currentStack.getStackDepth() > 0) {
                                XField xField = currentStack.getStackItem(0).getXField();
                                if (isSameAsLockObject(xField, lockObject) && !xField.isPublic()) {
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

                if (isOwnMethod(possibleAccessorMethod, new Method[0])) {
                    continue;
                }

                try {
                    ClassContext classContext = new ClassContext(declaringClass,
                            AnalysisContext.currentAnalysisContext());
                    CFG cfg = classContext.getCFG(possibleAccessorMethod);

                    for (Location location : cfg.orderedLocations()) {
                        InstructionHandle handle = location.getHandle();
                        Instruction instruction = handle.getInstruction();

                        if (instruction.getOpcode() == Const.MONITORENTER) {
                            OpcodeStack currentStack = OpcodeStackScanner
                                    .getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle.getPosition());
                            if (currentStack.getStackDepth() > 0) {
                                OpcodeStack.Item lock = currentStack.getStackItem(0);
                                XField lockObject = lock.getXField();

                                if (isSameAsLockObject(field, lockObject)) {
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

    private void reportAccessibleBackingCollectionBug(XField lock, MultiMap<XField, BugInfo> bugContainingMethods,
            XField backingCollection,
            MultiMap<XField, XMethod> collectionAccessors) {
        if (collectionAccessors.containsKey(backingCollection)) {
            String exposingMethods = buildMethodsMessage(collectionAccessors.get(backingCollection));
            for (BugInfo bugInfo : bugContainingMethods.get(lock)) {
                bugReporter.reportBug(bugInfo
                        .createBugInstance(this,
                                "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION",
                                lock, backingCollection, exposingMethods));
            }
        }
    }

    private void reportInheritedBackingCollectionBug(XField lock, MultiMap<XField, BugInfo> bugContainingMethods,
            XField backingCollection) {
        for (BugInfo bugInfo : bugContainingMethods.get(lock)) {
            bugReporter.reportBug(bugInfo
                    .createBugInstance(this,
                            "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION",
                            lock, backingCollection));
        }
    }

    private void reportBadCollectionObjectBugs(XField lock, MultiMap<XField, BugInfo> bugContainingMethods,
            XField backingCollection) {
        for (BugInfo bugInfo : bugContainingMethods.get(lock)) {
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
        assignedCollectionFields.clear();
        assignedWrappedCollectionFields.clear();
        declaredFieldExposingMethods.clear();
        collectionsBackedByPublicFields.clear();
        declaredInheritedCollectionFieldAccessingMethods.clear();
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
