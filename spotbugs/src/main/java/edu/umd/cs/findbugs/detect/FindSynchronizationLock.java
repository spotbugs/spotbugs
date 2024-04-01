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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class FindSynchronizationLock extends OpcodeStackDetector {
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
    public void visit(Method obj) {
        XMethod xMethod = getXMethod();
        if (xMethod.isPublic() && xMethod.isSynchronized()) {
            if (xMethod.isStatic()) {
                bugReporter.reportBug(new BugInstance(
                        this, "PFL_BAD_STATIC_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                        .addClassAndMethod(this));
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

                        if (lockObject.isPublic()) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        } else if (isPossiblyNonPublicObjectBug(lockObject)) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }
                    } else {
                        declaredLockObjects.put(lockObject, xMethod);
                        if (lockObject.isPublic()) {
                            potentialObjectBugContainingMethods.add(lockObject, xMethod);
                        }

                        if (isPossiblyProtectedInheritedBug(lockObject)) {
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
            String exposingMethodsMessage = buildMethodsMessage(exposingMethods);
            for (XMethod synchronizedMethod : synchronizedMethods) {
                bugReporter.reportBug(new BugInstance(
                        this, "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
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
        return lockObject.isProtected() || !(lockObject.isPublic() || lockObject.isPrivate());
    }

    private boolean isPossiblyProtectedInheritedBug(XField lockObject) {
        return lockObject.isProtected() && !getThisClass().isFinal();
    }

    private boolean isInitializerMethod(String methodName) {
        return Const.CONSTRUCTOR_NAME.equals(methodName) || Const.STATIC_INITIALIZER_NAME.equals(methodName);
    }

    private boolean isMethodNotInteresting(Method method, Method[] ownMethods) {
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

    private boolean inheritsFromHierarchy(XField lockObject) throws ClassNotFoundException {
        return !getDottedClassName().equals(lockObject.getClassName());
    }

    private Set<XMethod> findExposureInHierarchy(XField lockObject) throws ClassNotFoundException {
        HashSet<XMethod> unsafeMethods = new HashSet<>();
        Method[] ownMethods = getThisClass().getMethods();

        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            for (Method possibleAccessorMethod : declaringClass.getMethods()) {

                if (isMethodNotInteresting(possibleAccessorMethod, ownMethods)) {
                    continue;
                }

                try {
                    ClassContext classContext = new ClassContext(declaringClass, AnalysisContext.currentAnalysisContext());
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
                            OpcodeStack currentStack = OpcodeStackScanner.getStackAt(classContext.getJavaClass(), possibleAccessorMethod, handle.getPosition());
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
        }
        return unsafeMethods;
    }

    private HashSet<XMethod> findLockUsageInHierarchy(XField field) throws ClassNotFoundException {
        HashSet<XMethod> methodsUsingFieldAsLock = new HashSet<>();
        for (JavaClass declaringClass : getThisClass().getSuperClasses()) {
            for (Method possibleAccessorMethod : declaringClass.getMethods()) {

                if (isMethodNotInteresting(possibleAccessorMethod, new Method[0])) {
                    continue;
                }

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
                    AnalysisContext.logError("Error building CFG", e);
                }
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

    private <T> String buildMethodsMessage(Collection<T> methods) {
        return methods.stream().map(Object::toString).collect(Collectors.joining(",\n"));
    }

    private void reportExposingLockObjectBugs(XField lock, Collection<XMethod> lockUsingMethods, Collection<XMethod> exposingMethods) {
        String exposingMethodsMessage = buildMethodsMessage(exposingMethods);
        for (XMethod lockUsingMethod : lockUsingMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "PFL_BAD_EXPOSING_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(lockUsingMethod)
                    .addField(lock)
                    .addString(exposingMethodsMessage));
        }
    }

    private void reportAccessibleObjectBug(XField lock, Collection<XMethod> definedLockAccessors, HashMap<XField, XMethod> synchronizedMethods) {
        if (!definedLockAccessors.isEmpty()) {
            String problematicMethods = buildMethodsMessage(definedLockAccessors);
            bugReporter.reportBug(new BugInstance(
                    this, "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethods.get(lock))
                    .addField(lock)
                    .addString(problematicMethods));
        }
    }

    private void reportInheritedObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "PFL_BAD_INHERITED_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", LOW_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethod)
                    .addField(lock));
        }
    }

    private void reportObjectBugs(XField lock, Collection<XMethod> synchronizedMethods) {
        for (XMethod synchronizedMethod : synchronizedMethods) {
            bugReporter.reportBug(new BugInstance(
                    this, "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS", NORMAL_PRIORITY)
                    .addClass(this)
                    .addMethod(synchronizedMethod)
                    .addField(lock));
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
