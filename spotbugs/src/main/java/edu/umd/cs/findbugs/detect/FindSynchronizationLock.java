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
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MultiMap;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/* done: it probably should be a new type of bug because a string message must be added where the lock is EXPOSED or Updated
    also add multiple little test cases like:
    - what happens if the new value comes from a function parameter
    - what happens if the new value comes from a local variable
    - what happens if the new value is directly assigned to the lock
    - what happens if the field is declared final
    - what happens if the field is declared final and there is a setMethod
    - what happens if the field is declared final and there is a getMethod(this is compilation error, but you should be mentioning it in a comment)
    - add a bug that contains a accessor/exposing method too(as string)
*/
/*
todo:
  - check if there is an overlap in the tests - possible there is
    - refactor them if needed based on their type: volatile tests, accessible static tests, etc. - kind of refactored
  - refactor detector:
    - it should detect locks that are accessible from outside
    - it should detect locks that are accessible from outside in the presence of an accessor/exposing method
    - it shouldn't report them multiple times; for example if a lock is public and there is an accessor too, it must be reported only once
 */

/**
    Detector logic: from the highest priority to lowest<br>
*      <ol>
*      <li>if a lock is public:
*          report it as OBJECT_BUG
*       </li>
*      <li>if a lock is protected:
*          <ul>
*              <li>
*                  the class it was declared in has accessor/getter:
*                      report it as an ACCESSIBLE_OBJECT BUG
*              </li>
*              <li> the class it was declared in has NO accessor/getter:
*                  <ul>
*                      <li>the class it was declared in isn't final:
*                          report it as an OBJECT_BUG
*                      </li>
*                  </ul>
*              </li>
*          </ul>
*      </li>
*      <li>if a lock is private(AND not final):
*          <ul>
*              <li>if the class it was declared in has an accessor/getter: report it as an ACCESSIBLE_OBJECT_BUG</li>
*          </ul>
*      </li>
*      <li>if a lock is volatile(and none of the above applies, but I think they most probably will; MAYBE package private locks remained):
*          <ul>
*              <li>
*                  if the class it was declared in has an accessor/getter: report it as an ACCESSIBLE_OBJECT_BUG
*              </li>
*          </ul>
*      </li>
*      <li>if a lock is static(and none of the above applies, but I think they most probably will; MAYBE package private locks remained):
*          <ul>
*              <li>
*                  if the class it was declared in has an accessor/getter: report it as an ACCESSIBLE_OBJECT_BUG
*              </li>
*          </ul>
*      </li>
*
*      <li>if a lock is package private:
*          report it as OBJECT_BUG
*      </li>
*      </ol>
*/
public class FindSynchronizationLock extends OpcodeStackDetector {

    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String ACCESSIBLE_OBJECT_BUG = "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private final BugReporter bugReporter;
    private final HashSet<XMethod> synchronizedMethods;
    private final HashSet<Method> exposingMethods;
    private final MultiMap<XField, XMethod> lockAccessors;
    private final HashMap<XField, XMethod> usedLockObjects;
    private JavaClass currentClass;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.synchronizedMethods = new HashSet<>();
        this.exposingMethods = new HashSet<>();
        this.usedLockObjects = new HashMap<>();
        this.lockAccessors = new MultiMap<>(HashSet.class);
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.MONITORENTER) {
            OpcodeStack.Item lock = stack.getStackItem(0);
            XField lockObject = lock.getXField();

            if (lockObject != null) {
                // only interested in it if the lock is inside the current class
                // or it was inherited
                String declaringClass = ClassName.toSlashedClassName(lockObject.getClassName());
                try {
                    if (getClassName().equals(declaringClass) || inheritsFromHierarchy(
                            lockObject)) { /* @note what if we use the param in a function as a lock? */
                        XMethod xMethod = getXMethod();
                        if (lockObject.isPublic()) {
                            bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                    .addClassAndMethod(xMethod)
                                    .addField(lockObject));
                        } else {
                            usedLockObjects.put(lockObject, xMethod);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }

        }

        if ((seen == Const.PUTSTATIC || seen == Const.PUTFIELD) && !Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
            XMethod updateMethod = getXMethod();
            if (updateMethod.isPublic() || updateMethod.isProtected()) { /* @note:  What happens if this a private or protected method? */
                XField updatedField = getXFieldOperand();
                if (updatedField == null || updatedField.isPublic()) {
                    return;
                }
                lockAccessors.add(updatedField, updateMethod);

                System.out.println("Kind of breakpoint");
            }
        }

        if (seen == Const.ARETURN) {
            /* @note: Add method here that returned it */
            OpcodeStack.Item returnValue = stack.getStackItem(0);
            XField returnedField = returnValue.getXField();
            if (returnedField == null || returnedField.isPublic()) {
                return;
            }
            XMethod exposingMethod = getXMethod();
            lockAccessors.add(returnedField, exposingMethod);

            System.out.println("Stop here");
        }
    }

    private Boolean inheritsFromHierarchy(XField lockObject) throws ClassNotFoundException {
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

    @Override
    public void visit(Method obj) {
        XMethod xMethod = getXMethod();
        if (xMethod.isPublic() && xMethod.isSynchronized()) {
            if (xMethod.isStatic()) {
                bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                        .addClassAndMethod(this));
            } else {
                synchronizedMethods.add(xMethod);
            }
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
        super.visit(obj);
    }

    @Override
    public void visit(JavaClass obj) {
        currentClass = obj;
    }

    private boolean isAccessible(XField lock) {
        return lockAccessors.containsKey(lock);
    }

    private Collection<XMethod> getAccessorMethods(XField lock) {
        /** @NOTE
          *  iterate over the methods that accessed fields or returned them
          *  if this was accessed or returned by a  method add to the list
          *  return the list
          */
        /**
         * @todo:
         *  you got two maps with all the methods that either expose or update the field
         *  it would be nice if you would collect these inside a map for each field, and only check if the field is present in the map:
         *  HashMap<XField, HashSet<XMethod>> sketchyMethods;
         *  sketchyMethods.get(lock)
         */
        return lockAccessors.get(lock);
    }

    private Boolean isFinalDeclaringClass(XField lock) throws ClassNotFoundException {
        return currentClass.isFinal();
    }

    private Boolean isPackagePrivate(XField field) {
        return !(field.isPublic() || field.isProtected() || field.isPrivate());
    }
    @Override
    public void visitAfter(JavaClass obj) {
        if (!exposingMethods.isEmpty()) {
            String exposingMethodsMessage = exposingMethods.stream().map(Method::toString).collect(Collectors.joining(",\n"));
            for (XMethod synchronizedMethod : synchronizedMethods) {
                BugInstance bugInstance = new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                        .addClassAndMethod(synchronizedMethod)
                        .addString(exposingMethodsMessage);
                bugReporter.reportBug(bugInstance);
            }
        }
        exposingMethods.clear();
        synchronizedMethods.clear();

        /*
        * @todo:
        *   - Add string with exposed methods
        *   - Add a final test case maybe?
        *   - Refactor and optimize the reporting step
        *   - Check what is the case with package private locks
        *   - Discuss the hard cases
        */
        for (XField lock : usedLockObjects.keySet()) {
            // don't check public fields, because they were already reported at this point
            if (lock.isProtected()) {
                if (isAccessible(lock)) {
                    bugReporter.reportBug(new BugInstance(this, ACCESSIBLE_OBJECT_BUG, NORMAL_PRIORITY)
                            .addClass(this)
                            .addMethod(usedLockObjects.get(lock))
                            .addField(lock)
                    //                          .addString(MethodsThatAccessedOrReturnedTheLock)
                    );
                } else {
                    try {
                        if (!isFinalDeclaringClass(lock)) {
                            bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                    .addClass(this)
                                    .addMethod(usedLockObjects.get(lock))
                                    .addField(lock));
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }
                }
            } else if (lock.isPrivate() && !lock.isFinal()) {
                if (isAccessible(lock)) {
                    bugReporter.reportBug(new BugInstance(this, ACCESSIBLE_OBJECT_BUG, NORMAL_PRIORITY)
                            .addClass(this)
                            .addMethod(usedLockObjects.get(lock))
                            .addField(lock));
                    //                          .addString(MethodsThatAccessedOrReturnedTheLock)
                }
            } else if (isPackagePrivate(lock)) {
                bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                        .addClass(this)
                        .addMethod(usedLockObjects.get(lock))
                        .addField(lock));
                //                          .addString(MethodsThatAccessedOrReturnedTheLock)
            } else if (isAccessible(lock)) {
                bugReporter.reportBug(new BugInstance(this, ACCESSIBLE_OBJECT_BUG, NORMAL_PRIORITY)
                        .addClass(this)
                        .addMethod(usedLockObjects.get(lock))
                        .addField(lock));
                //                          .addString(MethodsThatAccessedOrReturnedTheLock)
            }
        }
        usedLockObjects.clear();
        lockAccessors.clear();
    }

}
