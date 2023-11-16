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
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.HashSet;

public class FindSynchronizationLock extends OpcodeStackDetector {

    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private final BugReporter bugReporter;
    private final HashSet<XMethod> synchronizedMethods;
    private boolean hasExposingMethod;
    private JavaClass currentClass;

    public FindSynchronizationLock(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.synchronizedMethods = new HashSet<>();
        this.hasExposingMethod = false;
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
                    if (getClassName().equals(declaringClass) || inheritsFromHierarchy(lockObject)) {
                        XMethod xMethod = getXMethod();

                        if (lockObject.isPublic() || lockObject.isProtected()) {
                            if (lockObject.isFinal() || !lockObject.isFinal()) {
                                bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                        .addClassAndMethod(xMethod)
                                        .addField(lockObject));
                            }
                        }

                        if (lockObject.isVolatile() && !lockObject.isFinal()) {
                            bugReporter.reportBug(new BugInstance(this, OBJECT_BUG, NORMAL_PRIORITY)
                                    .addClassAndMethod(xMethod)
                                    .addField(lockObject));
                        }
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }

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
        // don't visit constructors
        if (Const.CONSTRUCTOR_NAME.equals(obj.getName())) {
            return;
        }

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
                hasExposingMethod = true;
            }
        } else {
            SignatureParser signature = new SignatureParser(obj.getSignature());
            String returnType = signature.getReturnTypeSignature();
            if (returnType.contains(getClassName())) {
                hasExposingMethod = true;
            }
        }
        super.visit(obj);
    }

    @Override
    public void visit(JavaClass obj) {
        currentClass = obj;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (hasExposingMethod) {
            synchronizedMethods.forEach(method -> bugReporter.reportBug(new BugInstance(this, METHOD_BUG, NORMAL_PRIORITY)
                    .addClassAndMethod(method)));

        }
    }
}
