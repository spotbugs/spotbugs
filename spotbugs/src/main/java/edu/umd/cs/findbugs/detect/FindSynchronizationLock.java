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
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.OpcodeStackScanner;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiMap;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FindSynchronizationLock extends OpcodeStackDetector {

    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String STATIC_METHOD_BUG = "PFL_BAD_STATIC_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String ACCESSIBLE_OBJECT_BUG = "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String INHERITED_OBJECT_BUG = "PFL_BAD_INHERITED_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String EXPOSING_LOCK_OBJECT_BUG = "PFL_BAD_EXPOSING_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private final BugReporter bugReporter;
    private final HashSet<Method> exposingMethods;
    private final HashSet<XMethod> synchronizedMethods;
    private final HashMap<XField, XMethod> declaredLockObjects;
    private final HashMap<XField, XMethod> inheritedLockObjects;
    private final MultiMap<XField, XMethod> declaredLockAccessors;
    private final MultiMap<XField, XMethod> lockAccessorsInHierarchy;
    private final MultiMap<XField, XMethod> lockUsingMethodsInHierarchy;
    private final MultiMap<XField, XMethod> declaredFieldReturningMethods;
    private final MultiMap<XField, XMethod> potentialObjectBugContainingMethods;
    private final MultiMap<XField, XMethod> potentialInheritedBugContainingMethods;
    private JavaClass currentClass;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.exposingMethods = new HashSet<>();
        this.synchronizedMethods = new HashSet<>();
        this.declaredLockObjects = new HashMap<>();
        this.inheritedLockObjects = new HashMap<>();
        this.declaredLockAccessors = new MultiMap<>(HashSet.class);
        this.lockAccessorsInHierarchy = new MultiMap<>(HashSet.class);
        this.lockUsingMethodsInHierarchy = new MultiMap<>(HashSet.class);
        this.declaredFieldReturningMethods = new MultiMap<>(HashSet.class);
        this.potentialObjectBugContainingMethods = new MultiMap<>(HashSet.class);
        this.potentialInheritedBugContainingMethods = new MultiMap<>(HashSet.class);
    }

    @Override
    public void visit(JavaClass obj) {
        currentClass = obj;
    }

    @Override
    public void visit(Method obj) {
        XMethod xMethod = getXMethod();
        if (xMethod.isPublic() && xMethod.isSynchronized()) {
            if (xMethod.isStatic()) {
                bugReporter.reportBug(new BugInstance(this, STATIC_METHOD_BUG, NORMAL_PRIORITY).addClassAndMethod(this));
            }
            synchronizedMethods.add(xMethod);
        }

        if (!(xMethod.isStatic() || xMethod.isPublic())) {
            return;
        }

        String sourceSig = xMethod.getSourceSignature();
        if (sourceSig != null) {
            GenericSignatureParser signature = new GenericSignatureParser(sourceSig);
            String genericReturnValue = signature.getReturnTypeSignature();
            if (genericReturnValue.contains(getClassName())) {
                exposingMethods.add(obj);
            }
        } else {
            SignatureParser signature = new SignatureParser(obj.getSignature());
            String returnType = signature.getReturnTypeSignature();
            if (returnType.contains(getClassName())) {
                exposingMethods.add(obj);
            }
        }

    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            OpcodeStack.Item lock = stack.getStackItem(0);
            XField lockObject = lock.getXField();

            if (lockObject != null) {
                XMethod xMethod = getXMethod();
                try {
                    boolean inheritedFromHierarchy = inheritsFromHierarchy(lockObject);
                    if (inheritedFromHierarchy) {
                        inheritedLockObjects.put(lockObject, xMethod);
                        findExposureInHierarchy(lockObject).forEach(method -> lockAccessorsInHierarchy.add(lockObject, method));

                        if (isPossiblyPublicObjectBug(lockObject)) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        } else if (isPossiblyNonPublicObjectBug(lockObject)) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }
                    } else {
                        declaredLockObjects.put(lockObject, xMethod);
                        if (isPossiblyPublicObjectBug(lockObject)) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }

                        if (isPossiblyProtectedInheritedBug(lockObject)) {
                            potentialInheritedBugContainingMethods.add(lockObject, xMethod);
                        } else if (isPossiblyPackagePrivateInheritedBug(lockObject)) {
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

            if (updateMethod.isPublic() || updateMethod.isProtected()) { /* @note:  What happens if this a private or protected method? */
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

            if (returnedField.isPublic()) {
                return;
            }

            declaredLockAccessors.add(returnedField, exposingMethod);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (!exposingMethods.isEmpty()) {
            String exposingMethodsMessage = exposingMethods.stream().map(Method::toString).collect(Collectors.joining(",\n"));
            for (XMethod synchronizedMethod : synchronizedMethods) {
                BugInstance bugInstance = new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY).addClassAndMethod(synchronizedMethod).addString(
                        exposingMethodsMessage);
                bugReporter.reportBug(bugInstance);
            }
        }

        /**
         * How to refactor the detector logic:
         * Accessible lock:
         *  Declared in the CURRENT class
         *      Used as lock in the CURRENT class -- report as a bug in the CURRENT class
         *          - it is exposed by method(accessor or getter) in the current class:     ACCESSIBLE_OBJECT_BUG   -- mark: field/lock, method, usage of the lock, exposing methods
         *          - it is public:                                                         OBJECT_BUG              -- mark: field/lock, method, usage of the lock and the class where it was used
         *          - it is protected and the class is not final:                           INHERITED_OBJECT_BUG    -- mark: field/lock, method, usage of the lock -- lower priority
         *          - it is protected and the class is final:                               NOT a bug
         *          - it is private and NOT final:                                          maybe object bug        -- mark: field/lock, method, usage of the lock
         *      NOT used as a lock in the CURRENT class: Should we report this? We cannot know if it is used as a lock in a descendant(looking from the perspective of the current class)
         *          - it is public:                                                         OBJECT_BUG
         *          - it is protected and the class is not final:                           INHERITED_OBJECT_BUG
         *          - it is protected and the class is final:                               NOT a bug
         *          - it is private and NOT final:                                          INHERITED_OBJECT_BUG
         *  Declared in the CURRENT but used as lock in a DESCENDANT class -- report as a bug in the DESCENDANT class
         *      - it is exposed by a method(accessor or getter) in the DESCENDANT class:    ACCESSIBLE_OBJECT_BUG   -- mark: field/lock, method, usage of the lock, exposing methods
         *      - it is NOT exposed here(comes from a parent), but exposed in PARENT:       ACCESSIBLE_OBJECT_BUG   -- mark: field/lock, method, usage of the lock -- maybe lower priority, other bugs already created
         *      - it is NOT exposed here(comes from a parent):                              DO not report!          -- mark: field/lock, method, usage of the lock
         *      - it is public:                                                             OBJECT_BUG              -- mark: field/lock, method, usage of the lock
         *  It is exposed in the CURRENT class:
         *      - check if it was used as lock in hierarchy                                 EXPOSING_LOCK_OBJECT_BUG -- mark: field/lock, method that exposed the lock
         *      - exposed by PARENT do not use as lock here
         *  Declared elsewhere, comes as a method parameter and used as a lock in the CURRENT class:
         *      - report it:                                                                OBJECT_BUG              -- mark: field/lock, method, usage of the lock
         *
         * package private
         *
         *  NOTE:
         *      - INHERITED_OBJECT_BUGs are reported BOTH in the declaring class and (if present) in the class that uses as a lock
         *        as two different bugs, but I think this is fine
         *      - messages should be stated in the bug description so that they are clear in BOTH reported on the CURRENT class and DESCENDANT class
         *
         *      Summarizing the logic:
         *
         * x - means the column applies
         * ^ - means that another value from the column is applied/the column does not apply
         * _ - means we don't care about the column because other columns already apply
         *
         * |     Declared      |     Used as Lock    |               Visibility                |   Made Accessible   |  Description/Notes                        |
         * | Here  |     Up    | Up  | Here  | Down  |  Public | Protected | Package | Private | Up  | Here  | Down  |                                           |
         * |-------|-----------|---------------------|---------|-----------|---------|---------|-----|-------|-------|-------------------------------------------|
         * |   x   |     ^     |  _  |   x   |   _   |    x    |     ^     |    ^    |    ^    |  _  |   _   |   _   | OBJECT_BUG - It is public so report it, in other scenarios we are not interested
         * |   x   |     ^     |  _  |   x   |   _   |    _    |     _     |    _    |    _    |  ^  |   x   |   ^   | ACCESSIBLE_OBJECT_BUG - Don't care about hierarchy now, it is a problem here
         * |   x   |     ^     |  _  |   x   |   _   |    ^    |     x     |    ^    |    ^    |  _  |   _   |   _   | INHERITED_OBJECT_BUG - Lower priority - ONLY if class is NOT final
         * |   x   |     ^     |  _  |   x   |   _   |    ^    |     x     |    ^    |    ^    |  _  |   _   |   _   | Not a bug - if class IS final
         * |   x   |     ^     |  _  |   x   |   _   |    ^    |     ^     |    x    |    ^    |  _  |   _   |   _   | INHERITED_OBJECT_BUG - Lower priority - class is final: classes in the package can access it, it can be a problem if not used with care
         * |   x   |     ^     |  _  |   x   |   _   |    ^    |     ^     |    x    |    ^    |  _  |   _   |   _   | INHERITED_OBJECT_BUG - Lower priority - class is NOT final: similar to protected
         * |   x   |     ^     |  _  |   x   |   _   |    ^    |     ^     |    ^    |    x    |  _  |   _   |   _   | Not a bug
         * |   ^   |     ^     |  _  |   x   |   _   |    _    |     _     |    _    |    _    |  _  |   _   |   _   | OBJECT_BUG - it comes as a method parameter; not safe
         * |   ^   |     x     |  _  |   x   |   _   |    ^    |     x     |    ^    |    ^    |  ^  |   x   |   ^   | ACCESSIBLE_OBJECT_BUG - comes from a parent, but exposed here
         * |   ^   |     x     |  _  |   x   |   _   |    ^    |     ^     |    x    |    ^    |  ^  |   x   |   ^   | ACCESSIBLE_OBJECT_BUG - comes from a parent or from the package, but exposed here
         * |   ^   |     x     |  _  |   x   |   _   |    _    |     _     |    _    |    ^    |  x  |   ^   |   _   | ACCESSIBLE_OBJECT_BUG - check the method exposing the lock from parents
         * |   ^   |     x     |  _  |   x   |   _   |    _    |     _     |    _    |    _    |  _  |   _   |   x   | Not a bug, because we cannot detect it here
         * |   ^   |     x     |  x  |   ^   |   _   |    ^    |     x     |    ^    |    ^    |  ^  |   x   |   ^   | EXPOSING_LOCK_OBJECT_BUG - check lock usage in parents, this lock was already reported with a lower priority bug
         * |   ^   |     x     |  x  |   ^   |   _   |    ^    |     ^     |    x    |    ^    |  ^  |   x   |   ^   | EXPOSING_LOCK_OBJECT_BUG - check lock usage in parents, this lock was already reported with a lower priority bug
         * |   ^   |     x     |  ^  |   ^   |   x   |    _    |     _     |    _    |    _    |  _  |   _   |   _   | Not a bug, because we cannot(detect here at least) say anything about it, since it is not used as a lock
         *
         * Granny cases from the view of the current(PARENT) class:
         * Granny case: declared in GRANNY, used as lock in PARENT, exposed in CHILD(or lower)
         * |   ^   |     x     |  _  |   x   |   _   |    _    |     _     |    _    |    _    |  _  |   _   |   x   | Shift the roles one to the right(parent -> child), that is already in the table
         * Granny case 2: declared in GRANNY, used as lock in CHILD, exposed in PARENT(or upper, doesn't matter)
         * |   ^   |     x     |  _  |   _   |   x   |    _    |     _     |    _    |    _    |  _  |   x   |   _   | Already in the table
         * Granny case 3: declared in GRANNY, used as lock in GRANNY, exposed in CHILD - PARENT only passes down
         * |   ^   |     x     |  x  |   _   |   _   |    _    |     _     |    _    |    _    |  _  |   x   |   _   | Already in the table
         * Granny case 3: declared in GRANNY, used as lock in GRANNY, exposed in CHILD - PARENT only passes down
         * |   ^   |     x     |  x  |   _   |   _   |    _    |     _     |    _    |    _    |  _  |   x   |   _   | Already in the table
         */
        /*
         * @todo:
         *   - Add string with exposed methods
         *   - Add a final test case maybe?
         *   - Refactor and optimize the reporting step
         *   - Check what is the case with package private locks
         *   - Discuss the hard cases
         */

        for (XField lock : declaredLockObjects.keySet()) {
            if (!declaredLockAccessors.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, declaredLockAccessors, declaredLockObjects);
            } else if (!potentialInheritedBugContainingMethods.get(lock).isEmpty()) {
                reportInheritedObjectBugs(lock, potentialInheritedBugContainingMethods.get(lock));
            } else if (!potentialObjectBugContainingMethods.get(lock).isEmpty()) {
                reportObjectBugs(lock, potentialObjectBugContainingMethods.get(lock));
            }
        }

        for (XField lock : inheritedLockObjects.keySet()) {
            if (!declaredLockAccessors.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, declaredLockAccessors, inheritedLockObjects);
            } else if (!lockAccessorsInHierarchy.get(lock).isEmpty()) {
                reportAccessibleObjectBug(lock, lockAccessorsInHierarchy, inheritedLockObjects);
            } else if (!potentialObjectBugContainingMethods.get(lock).isEmpty()) {
                reportObjectBugs(lock, potentialObjectBugContainingMethods.get(lock));
            }
        }

        for (XField lock : lockUsingMethodsInHierarchy.keySet()) {
            if (!declaredFieldReturningMethods.get(lock).isEmpty()) {
                reportExposingLockObjectBugs(lock, lockUsingMethodsInHierarchy.get(lock), declaredFieldReturningMethods.get(lock));
            }
        }

        clearState();
    }

    private boolean isPackagePrivate(XField field) {
        return !(field.isPublic() || field.isProtected() || field.isPrivate());
    }

    private boolean isPossiblyNonPublicObjectBug(XField lockObject) {
        return lockObject.isProtected() || isPackagePrivate(lockObject);
    }

    private boolean isPossiblyPublicObjectBug(XField lockObject) {
        return lockObject.isPublic();
    }

    private boolean isPossiblyPackagePrivateInheritedBug(XField lockObject) {
        return isPackagePrivate(lockObject);
    }

    private boolean isPossiblyProtectedInheritedBug(XField lockObject) {
        return lockObject.isProtected() && !currentClass.isFinal();
    }

    private boolean isInitializerMethod(String methodName) {
        return Const.CONSTRUCTOR_NAME.equals(methodName) || Const.STATIC_INITIALIZER_NAME.equals(methodName);
    }

    private boolean isMethodNotInteresting(Method method, Optional<HashSet<Method>> ownMethods) {
        if (ownMethods.isPresent() && ownMethods.get().contains(method)) {
            return true;
        }

        if (isInitializerMethod(method.getName())) {
            return true;
        }

        return method.getCode() == null;
    }

    private boolean isSameAsLockObject(XField maybeLockObject, XField lockObject) {
        if (maybeLockObject == null || lockObject == null) {
            return false;
        }
        return lockObject.equals(maybeLockObject);
    }

    private boolean inheritsFromHierarchy(XField lockObject) throws ClassNotFoundException {
        String currentClassName = ClassName.toDottedClassName(getClassName());
        String lockObjectDeclaringClassName = lockObject.getClassName();
        if (currentClassName.equals(lockObjectDeclaringClassName)) {
            return false;
        }

        // the lock object was defined at some point in the hierarchy
        JavaClass[] allInterfaces = currentClass.getAllInterfaces();
        for (JavaClass next : allInterfaces) {
            if (lockObjectDeclaringClassName.equals(next.getClassName())) {
                return true;
            }
        }

        JavaClass[] superClasses = currentClass.getSuperClasses();
        for (JavaClass next : superClasses) {
            if (lockObjectDeclaringClassName.equals(next.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private HashSet<Map.Entry<Method, JavaClass>> getAllSuperMethods() throws ClassNotFoundException {
        return Arrays.stream(currentClass.getSuperClasses()).flatMap(cls -> Arrays.stream(cls.getMethods()).map(m -> new AbstractMap.SimpleEntry<>(m,
                cls))).collect(Collectors.toCollection(HashSet::new));
    }

    private HashSet<XMethod> findExposureInHierarchy(XField lockObject) throws ClassNotFoundException {
        /**
         * @NOTE: Probably this will come in handy:
         *       XMethod xmethod = XFactory.createXMethod(jclass, method);
         *       ---------------------------------------------------------------
         *        locationLoop: for (Iterator<Location> iter = cfg.locationIterator(); iter.hasNext();) {
         *             Location location = iter.next();
         *             InstructionHandle handle = location.getHandle();
         *             Instruction ins = handle.getInstruction();
         *
         *             // Only consider invoke instructions
         *             if (!(ins instanceof InvokeInstruction)) {
         *                 continue;
         *             }
         *             if (ins instanceof INVOKEINTERFACE) {
         *                 continue;
         *             }
         *
         *  final ConstantPool cp = cpg.getConstantPool();
         *  final ConstantCP cmr = (ConstantCP) cp.getConstant(super.getIndex());
         *  final ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex());
         *  String fieldName = ((ConstantUtf8) cp.getConstant(cnat.getNameIndex())).getBytes();
         *                          *
         *  ConstantMethodref cmr = (ConstantMethodref) c;
         *  ConstantNameAndType cnat = (ConstantNameAndType) cp.getConstant(cmr.getNameAndTypeIndex(),
         *      Const.CONSTANT_NameAndType);
         *  String methodName = ((ConstantUtf8) cp.getConstant(cnat.getNameIndex(), Const.CONSTANT_Utf8)).getBytes();
         *  String className = cp.getConstantString(cmr.getClassIndex(), Const.CONSTANT_Class).replace('/', '.');
         *  String methodSig = ((ConstantUtf8) cp.getConstant(cnat.getSignatureIndex(), Const.CONSTANT_Utf8)).getBytes();
         */

        HashSet<XMethod> unsafeMethods = new HashSet<>();
        HashSet<Method> ownMethods = new HashSet<>(Arrays.asList(currentClass.getMethods()));

        for (Map.Entry<Method, JavaClass> entry : getAllSuperMethods()) {
            Method possibleAccessorMethod = entry.getKey();
            JavaClass declaringClass = entry.getValue();

            if (isMethodNotInteresting(possibleAccessorMethod, Optional.of(ownMethods))) {
                continue;
            }

            try {
                ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
                ConstantPoolGen cpg = classContext.getConstantPoolGen();

                for (Location location : classContext.getCFG(possibleAccessorMethod).orderedLocations()) {
                    InstructionHandle handle = location.getHandle();
                    Instruction instruction = handle.getInstruction();

                    // this can be changed to
                    // short opcode = instruction.getOpcode();
                    // opcode == Const.PUTFIELD
                    if (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC) {
                        FieldInstruction fieldInstruction = (FieldInstruction) instruction;
                        XField xfield = Hierarchy.findXField(fieldInstruction, cpg);

                        if (isSameAsLockObject(xfield, lockObject) && !xfield.isPublic()) {
                            XMethod unsafeXMethod = XFactory.createXMethod(declaringClass, possibleAccessorMethod);
                            unsafeMethods.add(unsafeXMethod);
                        }
                    }

                    if (instruction instanceof ARETURN) {
                        OpcodeStack currentStack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle
                                .getPosition());
                        XField xField = currentStack.getStackItem(0).getXField();

                        if (isSameAsLockObject(xField, lockObject) && !xField.isPublic()) {
                            XMethod unsafeXMethod = XFactory.createXMethod(declaringClass, possibleAccessorMethod);
                            unsafeMethods.add(unsafeXMethod);
                        }
                    }

                }
            } catch (CFGBuilderException e) {
                AnalysisContext.logError("Error analyzing method", e);
            }
        }

        return unsafeMethods;
    }

    private HashSet<XMethod> findLockUsageInHierarchy(XField field) throws ClassNotFoundException {
        HashSet<XMethod> methodsUsingFieldAsLock = new HashSet<>();
        for (Map.Entry<Method, JavaClass> entry : getAllSuperMethods()) {
            Method possibleAccessorMethod = entry.getKey();
            JavaClass declaringClass = entry.getValue();

            if (isMethodNotInteresting(possibleAccessorMethod, Optional.empty())) {
                continue;
            }

            // analyze the current method
            // check if it uses the field as a lock
            try {
                ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
                CFG cfg = classContext.getCFG(possibleAccessorMethod);

                for (Location location : cfg.orderedLocations()) {
                    InstructionHandle handle = location.getHandle();
                    Instruction instruction = handle.getInstruction();

                    if (instruction.getOpcode() == Const.MONITORENTER) {
                        OpcodeStack currentStack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle
                                .getPosition());
                        OpcodeStack.Item lock = currentStack.getStackItem(0);
                        XField lockObject = lock.getXField();

                        if (isSameAsLockObject(field, lockObject)) {
                            methodsUsingFieldAsLock.add(XFactory.createXMethod(declaringClass, possibleAccessorMethod));
                        }
                    }
                }

            } catch (CFGBuilderException e) {
                throw new RuntimeException(e);
            }
        }

        return methodsUsingFieldAsLock;
    }

    private void checkLockUsageInHierarchy(XField field, XMethod exposingMethod) {
        try {
            if (inheritsFromHierarchy(field)) {
                if (declaredFieldReturningMethods.get(field).isEmpty()) {
                    findLockUsageInHierarchy(field).forEach(m -> lockUsingMethodsInHierarchy.add(field, m));
                }
                declaredFieldReturningMethods.add(field, exposingMethod);
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
    }

    private void reportExposingLockObjectBugs(XField lock, Collection<XMethod> lockUsingMethods, Collection<XMethod> exposingMethods) {
        String exposingMethodsMessage = exposingMethods.stream().map(XMethod::toString).collect(Collectors.joining(",\n"));
        for (XMethod lockUsingMethod : lockUsingMethods) {
            bugReporter.reportBug(new BugInstance(this, EXPOSING_LOCK_OBJECT_BUG, NORMAL_PRIORITY).addClass(this).addMethod(lockUsingMethod).addField(
                    lock).addString(exposingMethodsMessage));
        }
    }

    private void reportAccessibleObjectBug(XField lock, MultiMap<XField, XMethod> lockAccessors, HashMap<XField, XMethod> synchronizedMethods) {
        Collection<XMethod> definedLockAccessors = lockAccessors.get(lock);
        if (!definedLockAccessors.isEmpty()) {
            String problematicMethods = definedLockAccessors.stream().map(XMethod::toString).collect(Collectors.joining(",\n"));
            bugReporter.reportBug(new BugInstance(this, ACCESSIBLE_OBJECT_BUG, NORMAL_PRIORITY).addClass(this).addMethod(synchronizedMethods.get(
                    lock)).addField(lock).addString(problematicMethods));
        }
    }

    private void reportInheritedObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(this, INHERITED_OBJECT_BUG, LOW_PRIORITY).addClass(this).addMethod(synchronizedMethod).addField(
                    lock));
        }
    }

    private void reportObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY).addClass(this).addMethod(synchronizedMethod).addField(lock));
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
        declaredFieldReturningMethods.clear();
        potentialObjectBugContainingMethods.clear();
        potentialInheritedBugContainingMethods.clear();
    }
}
