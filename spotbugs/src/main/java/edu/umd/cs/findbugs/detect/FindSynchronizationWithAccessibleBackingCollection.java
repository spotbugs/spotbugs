package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
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
import edu.umd.cs.findbugs.ba.ca.CallListAnalysis;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.FieldOrMethodName;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiMap;
import static edu.umd.cs.findbugs.ba.ca.CallListAnalysis.*;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Utility;
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
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FindSynchronizationWithAccessibleBackingCollection implements Detector {
    private final BugReporter bugReporter;
    private final BugAccumulator bugAccumulator;
    private final MultiMap<XField, BugInfo> lockObjects;
    private final MultiMap<XField, XMethod> declaredLockAccessors;
    private final MultiMap<XField, XField> assignedCollectionFields;
    private final MultiMap<XField, XField> assignedWrappedCollectionFields;
    private final MultiMap<XField, Method> declaredFieldExposingMethods;
    private final MultiMap<XField, XField> collectionsBackedByPublicFields;

    private static final Set<MethodDescriptor> WRAPPER_IMPLEMENTATIONS = new HashSet<>();

    static {
        String juCollections = ClassName.toSlashedClassName(java.util.Collections.class);
        WRAPPER_IMPLEMENTATIONS.addAll(Arrays.asList(
                //synchronized collections
                new MethodDescriptor(juCollections, "synchronizedCollection", "(Ljava/util/Collection;)Ljava/util/Collection;", true),
                new MethodDescriptor(juCollections, "synchronizedList", "(Ljava/util/List;)Ljava/util/List;", true),
                new MethodDescriptor(juCollections, "synchronizedMap", "(Ljava/util/Map;)Ljava/util/Map;", true),
                new MethodDescriptor(juCollections, "synchronizedSortedMap", "(Ljava/util/SortedMap;)Ljava/util/SortedMap;", true),
                new MethodDescriptor(juCollections, "synchronizedNavigableMap", "(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", true),
                new MethodDescriptor(juCollections, "synchronizedSet", "(Ljava/util/Set;)Ljava/util/Set;", true),
                new MethodDescriptor(juCollections, "synchronizedSortedSet", "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", true),
                new MethodDescriptor(juCollections, "synchronizedNavigableSet", "(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", true),
                // unmodifiable collections
                new MethodDescriptor(juCollections, "unmodifiableCollection", "(Ljava/util/Collection;)Ljava/util/Collection;", true),
                new MethodDescriptor(juCollections, "unmodifiableList", "(Ljava/util/List;)Ljava/util/List;", true),
                new MethodDescriptor(juCollections, "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;", true),
                new MethodDescriptor(juCollections, "unmodifiableSortedMap", "(Ljava/util/SortedMap;)Ljava/util/SortedMap;", true),
                new MethodDescriptor(juCollections, "unmodifiableNavigableMap", "(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", true),
                new MethodDescriptor(juCollections, "unmodifiableSet", "(Ljava/util/Set;)Ljava/util/Set;", true),
                new MethodDescriptor(juCollections, "unmodifiableSortedSet", "(Ljava/util/SortedSet;)Ljava/util/SortedSet;", true),
                new MethodDescriptor(juCollections, "unmodifiableNavigableSet", "(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", true),
                // checked collections
                new MethodDescriptor(juCollections, "checkedCollection", "(Ljava/util/Collection;Ljava/lang/Class;)Ljava/util/Collection;", true),
                new MethodDescriptor(juCollections, "checkedList", "(Ljava/util/List;Ljava/lang/Class;)Ljava/util/List;", true),
                new MethodDescriptor(juCollections, "checkedMap", "(Ljava/util/Map;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/Map;", true),
                new MethodDescriptor(juCollections, "checkedSortedMap", "(Ljava/util/SortedMap;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/SortedMap;", true),
                new MethodDescriptor(juCollections, "checkedNavigableMap", "(Ljava/util/NavigableMap;Ljava/lang/Class;Ljava/lang/Class;)Ljava/util/NavigableMap;", true),
                new MethodDescriptor(juCollections, "checkedSet", "(Ljava/util/Set;Ljava/lang/Class;)Ljava/util/Set;", true),
                new MethodDescriptor(juCollections, "checkedSortedSet", "(Ljava/util/SortedSet;Ljava/lang/Class;)Ljava/util/SortedSet;", true),
                new MethodDescriptor(juCollections, "checkedNavigableSet", "(Ljava/util/NavigableSet;Ljava/lang/Class;)Ljava/util/NavigableSet;", true),
                new MethodDescriptor(juCollections, "checkedQueue", "(Ljava/util/Queue;Ljava/lang/Class;)Ljava/util/Queue;", true)));
        // todo add Arrays.asList etc.
    }

    public FindSynchronizationWithAccessibleBackingCollection(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.lockObjects = new MultiMap<>(HashSet.class);
        this.declaredLockAccessors = new MultiMap<>(HashSet.class);
        this.assignedCollectionFields = new MultiMap<>(HashSet.class);
        this.assignedWrappedCollectionFields = new MultiMap<>(HashSet.class);
        this.declaredFieldExposingMethods = new MultiMap<>(HashSet.class);
        this.collectionsBackedByPublicFields = new MultiMap<>(HashSet.class);
    }

    private class BugInfo {
        private final ClassContext classContext;
        private final Method inMethod;
        private final Location atLocation;
        private final SourceLineAnnotation sourceLineAnnotation;

        private BugInfo(ClassContext classContext, Method inMethod, Location atLocation) {
            this.classContext = classContext;
            this.inMethod = inMethod;
            this.atLocation = atLocation;
            this.sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, inMethod, atLocation);
        }

        private BugInstance createBugInstance(Detector detector, String bugType, XField field, String message) {
            return new BugInstance(detector, bugType, NORMAL_PRIORITY)
                    .addClassAndMethod(classContext.getJavaClass(), inMethod)
                    .addSourceLine(sourceLineAnnotation)
                    .addField(field)
                    .addString(message);
        }

        private SourceLineAnnotation getSourceLineAnnotation() {
            return sourceLineAnnotation;
        }
    }

//    private static class CollectionWrapper {
//        private final String packageName;
//        private final String methodName;
//        private final String signature;
//
//        public static CollectionWrapper fromSignature(String signature) {
////            new MethodDescriptor()
//            return null;
//        }
//
//        public CollectionWrapper(String packageName, String methodName, String signature) {
//            this.packageName = packageName;
//            this.methodName = methodName;
//            this.signature = signature;
//        }
//
//        public boolean matches(String packageName, String methodName, String signature) {
//            return this.packageName.equals(packageName) && this.methodName.equals(methodName) && this.signature.equals(signature);
//        }
//    }

    private static boolean isWrapperImplementation(MethodDescriptor methodDescriptor) {
        return WRAPPER_IMPLEMENTATIONS.contains(methodDescriptor);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        for (Method method : classContext.getMethodsInCallOrder()) {
            try {
                analyzeAssignments(method, classContext);
                analyzeLockUsage(method, classContext);
                analyzeExposure(method, classContext);
            } catch (CFGBuilderException e) {

            }
        }
        System.out.println("Declared lock accessors: " + declaredLockAccessors);

//        reportBugs();
    }

    @Override
    public void report() {

    }

    private void analyzeAssignments(Method obj, ClassContext classContext) throws CFGBuilderException {
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        for (Location location : classContext.getCFG(obj).orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            // todo save all invokeinstructions

            if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                FieldInstruction fieldInstruction = (FieldInstruction) instruction;
                XField xfield = Hierarchy.findXField(fieldInstruction, cpg);
                XField field = XFactory.createXField(fieldInstruction, cpg);
                Type fieldType = fieldInstruction.getFieldType(cpg);
                int pc = handle.getPosition();
                OpcodeStack.Item item = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, pc).getStackItem(0);
                XMethod returnValueOf = item.getReturnValueOf();
                XField assignedField = item.getXField();

                if (returnValueOf != null) {
                    if (item.isNewlyAllocated()) {
                        // this comes in here: private final List<Object> view5 = Arrays.asList(collection5);
                        // reference not stored
                        // cannot be exposed if newly allocated, everything is fine
                        continue;
                    }

                    // @note Maybe also check the order of the param for the instruction: this can be stored next to methodDescriptors
                    // How do I know if this is wrapping something rather than copying it? Only check for known wrapping functions?
                    if (isWrapperImplementation(returnValueOf.getMethodDescriptor())) {
                        // @note The prev isntruction can be another instruction too
                        InstructionHandle prevHandle = location.getBasicBlock().getPredecessorOf(handle);
                        Instruction prevInstruction = prevHandle.getInstruction();
                        if (isMethodCall(prevInstruction)) { // collection wrappers
                            XField wrappedField = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, prevHandle.getPosition()).getStackItem(0).getXField();


                            // todo this could be  analyzed further
                            if (wrappedField != null && isCollection(wrappedField)) {
                                assignedWrappedCollectionFields.add(xfield, wrappedField);
                            }
                        }

                        if (prevInstruction instanceof INVOKESPECIAL) { // such as map.keySet()

                        }

                        System.out.println("Breakpoint here");
                    }
                } else if (assignedField != null) {
                    if (isCollection(assignedField)) {
                        assignedCollectionFields.add(xfield, assignedField);
                    }
                }


                System.out.println("PUTFIELD: " + xfield + " " + fieldType);

            }
        }
    }

    private boolean isInterestingField(XField field) {
        return field != null && isCollection(field) && lockObjects.containsKey(field)
                && (assignedCollectionFields.containsKey(field) || assignedWrappedCollectionFields.containsKey(field));
    }

    private boolean isMethodCall(Instruction instruction) {
        return (instruction instanceof INVOKESTATIC) || (instruction instanceof INVOKESPECIAL) || (instruction instanceof INVOKEVIRTUAL) || (instruction instanceof INVOKEINTERFACE);
    }

    private void analyzeExposure(Method method, ClassContext classContext) throws CFGBuilderException {
        analyzeMethodExposure(method, classContext);
        analyzeFieldExposure();
    }

    private void analyzeMethodExposure(Method obj, ClassContext classContext) throws CFGBuilderException {
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        for (Location location : classContext.getCFG(obj).orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            // todo What if this method is the method that assigns the wrapped collection? Should this only be done in constructors?
            if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                FieldInstruction fieldInstruction = (FieldInstruction) instruction;
                OpcodeStack.Item item = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, handle.getPosition()).getStackItem(0);
                XField assignedField = item.getXField();

                if (isInterestingField(assignedField)) {
//                    declaredFieldExposingMethods.add(assignedField, obj);
//                    MethodDescriptor methodDescriptor = new MethodDescriptor("", obj.getName(), obj.getSignature(), obj.isStatic());
//                    bugReporter.reportBug(new BugInstance(
//                            this, "SABC_SYNCHRONIZATION_WITH_PUBLIC_BACKING_COLLECTION", NORMAL_PRIORITY)
//                                    .addClassAndMethod(methodDescriptor)
//                                    .addField(assignedField)
//                            );
                }
            }

            if (instruction instanceof ARETURN) {
                OpcodeStack currentStack = OpcodeStackScanner.getStackAt(
                        classContext.getJavaClass(), obj, handle
                                .getPosition());
                XField returnedField = currentStack.getStackItem(0).getXField();

                if (isInterestingField(returnedField)) {
                    declaredFieldExposingMethods.add(returnedField, obj);
                }
            }
        }

    }

    private void analyzeFieldExposure() {
        for (XField field : assignedCollectionFields.keySet()) {
            for (XField assignedField : assignedCollectionFields.get(field)) {
                if (isInterestingField(field) && assignedField.isPublic()) {
                    collectionsBackedByPublicFields.add(field, assignedField);
                }
            }
        }

        for (XField field : assignedWrappedCollectionFields.keySet()) {
            for (XField backingField : assignedWrappedCollectionFields.get(field)) {
                if (isInterestingField(field) && backingField.isPublic()){
                    collectionsBackedByPublicFields.add(field, backingField);
                }
            }
        }

    }

    // @note Should I align this with LCK00-J and  only work with the private final fields? All other cases should be handled by LCK00-J.
    private void analyzeLockUsage(Method obj, ClassContext classContext) throws CFGBuilderException {
//        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        for (Location location : classContext.getCFG(obj).orderedLocations()) {
            InstructionHandle handle = location.getHandle();
            Instruction instruction = handle.getInstruction();

            if (instruction instanceof MONITORENTER) {
                int pc = handle.getPosition();
                OpcodeStack stack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), obj, pc);
                OpcodeStack.Item item = stack.getStackItem(0);
                XField lock = item.getXField();

                if (lock != null) {
                    if (isCollection(lock)) {
                        lockObjects.add(lock, new BugInfo(classContext, obj, location));
                    }
                }
            }
        }
    }

    private <T extends ClassMember> String concatMessage(Collection<T> members) {
        return members.stream().map(Object::toString).collect(Collectors.joining(",\n"));
    }

    private void reportBugs() {
        if (!collectionsBackedByPublicFields.keySet().isEmpty()) {
            for (XField lock : collectionsBackedByPublicFields.keySet()) {
                String backingFields = concatMessage(collectionsBackedByPublicFields.get(lock));
                for (BugInfo bugInfo : lockObjects.get(lock)) {
                    bugReporter.reportBug(bugInfo.createBugInstance(this,
                            "SABC_SYNCHRONIZATION_WITH_PUBLIC_BACKING_COLLECTION", lock, backingFields));

                }
            }
        }

    }

        /**
     * Steps:
     * Find synchronization on a lock                                   - DONE
     * Check if the lock is a collection                                - DONE
     *  - if not, continue - no other things to do
     * Check if the lock is a backing collection for another collection - In progress
     * - if not, continue - no other things to do
     * Check whoever backs it up is accessible
     * - if not, continue - no other things to do
     * Report bug and also check if the backing collection is also a backing collection for another collection and so forth
     */
//    @Override
//    public void sawOpcode(int seen) {
////        if (seen == Const.MONITORENTER) {
//            OpcodeStack.Item lock = stack.getStackItem(0);
//            XField lockObject = lock.getXField();
//
//            if (lockObject != null) {
//                if (isCollection(lockObject)) {
//                  boolean isItBackedUpByCollection = hasBackingCollection(lockObject);
//                }
//            }
//        }
//
//        if (seen == Const.PUTSTATIC || seen == Const.PUTFIELD) {
//            analyzeAssignments();
//        }

//        if (seen == Const.AASTORE)


//    }

    private boolean isCollection(Type type) {
        if (type instanceof ReferenceType) {
            ReferenceType rType = (ReferenceType) type;
            try {
                /**
                 * @note
                 * What about the case when the backing "collection" is an array? E.g., Object[], is it considered a collection?
                 * Does it pose a threat? Or is it only a problem if the type is a reference type inside? So Object[] is problematic, while int[] is not?
                 */
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

    /**
     * @note What should we check?
     * All the assignments and if any of them is a backing collection?
     */
    private boolean hasBackingCollection(XField field) {

        return false;
    }

//    private void analyzeAssignments() {
//        XMethod updateMethod = getXMethod();
//        XField updatedField = getXFieldOperand();
//
//        if (updatedField == null) {
//            return;
//        }
//
//        if (!isCollection(updatedField)) {
//            return;
//        }
//
//        OpcodeStack.Item top = stack.getStackItem(0);
//        XMethod returnValueOf = top.getReturnValueOf();
//        XField assignedField = top.getXField();
//
////        ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
//        ConstantPoolGen cpg = getClassContext().getConstantPoolGen();
//
//
////        XField f = getXFieldOperand();
////        OpcodeStack.Item item = stack.getStackItem(1);
////        if (item.getRegisterNumber() != 0) {
////            return;
////        }
//
//        if (returnValueOf != null) {
//            if (top.isNewlyAllocated()) {
//                // this comes in here: private final List<Object> view5 = Arrays.asList(collection5);
//                // reference not stored
//                // cannot be exposed if newly allocated, everything is fine
//                return;
//            }
//
//            assignedMethods.add(updatedField, returnValueOf);
//
//            // check somehow if the return value is a collection that is somehow backed up
//
//        } else if (assignedField != null) {
//            if (isCollection(assignedField)) {
//                // we got a backing collection!
//                // we should check if it is accessible
//                assignedCollectionFields.add(updatedField, assignedField);
//            }
//        }
//
//        System.out.println("Collection assignments...");
//
//        // maybe top.getReturnValueOf().getName() and getPackageName() or getSourceSignature and check for collection type
//        // What about direct mapping?
//        //   - field = anotherField?
//
//
//
////        if (isCollection(updatedField)) {
////            collectionAssignments.add(updatedField, getXFieldOperand());
////        }
//
//        //checkLockUsageInHierarchy(updatedField, updateMethod);
//
////        if (updateMethod.isPublic() || updateMethod.isProtected()) {
////            if (updatedField.isPublic()) {
////                return;
////            }
////            declaredLockAccessors.add(updatedField, updateMethod);
////        }
//
//    }

    /**
     * @note Logic:
     *  - Find a synchronization on a field that is a collection
     *    - Only collections?
     *  - Check how it was initialized
     *    - It could be initialized in the constructor
     *    - It could be initialized in the static initializer
     *    - It could be initialized in a method elsewhere
     *    - It could be initialized in a method in some other class
     *    - It could be updated somewhere else
     *    - Main idea: check any PUTFIELD that initializes this field or local variable!
     *  - If it was initialized with a collection, then check if the collection is accessible somewhere else
     *    - This is similar to the logic in LCK00-J
     *  - Report at the end of the analysis
     *
     *  Maybe:
     *   Check what was synchronized and what was accessed inside synchronization block
     *   What about import javafx.scene.control.ListView?
     */

    /**
     * @note The issue:
     * - Detect an iteration over a collection(must it be synchronized?)
     * - Check if it was iterated was this inside a synchronized block?
     *  - What if it was not? Bug?
     *  - What if it was? Check if it was synchronized on the backing collection?
     *    - It was synchronized on the backing collection: fine? What if this is only a backing collection again?
     *    - It was not synchronized on the backing collection: bug if it is accessible, then should we check if this was also just a backing collection? Trace back until when?
     */

}
