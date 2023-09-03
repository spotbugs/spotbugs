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
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FindIncreasedAccessibilityOfMethods implements Detector {
    private static final String BUG_TYPE = "IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY";
    private static final String PACKAGE_PRIVATE = "package private";
    private static final String PROTECTED = "protected";
    private static final String PUBLIC = "public";

    private final BugReporter bugReporter;

    public FindIncreasedAccessibilityOfMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass subClass = classContext.getJavaClass();
        for (IAOMBug bug : findIAOMBugs(subClass)) {
            bugReporter.reportBug(new BugInstance(this, BUG_TYPE, LOW_PRIORITY)
                    .addClassAndMethod(bug.clazz, bug.method)
                    .addString(bug.oldAccessibility)
                    .addString(bug.newAccessibility));
        }
    }

    @Override
    public void report() {
    }

    private List<IAOMBug> findIAOMBugs(JavaClass subClass) {
        List<IAOMBug> iaomBugs = new ArrayList<>();
        JavaClass[] superClasses;
        try {
            superClasses = subClass.getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return iaomBugs;
        }

        List<Method> subClassMethods = getMethodsExceptConstructors(subClass);
        List<IAOMBug> superClassBugs = new ArrayList<>();
        for (JavaClass superClass : superClasses) {
            superClassBugs.addAll(findIAOMBugs(superClass));
            for (Method superClassMethod : getMethodsExceptConstructors(superClass)) {
                if (isAccessibleFromSubClass(superClassMethod, subClass, superClass)) {
                    for (Method subClassMethod : subClassMethods) {
                        if (isOverridable(superClassMethod, subClassMethod) && !isCloneMethodFromCloneable(subClass, superClassMethod)) {
                            if (superClassMethod.isProtected() && subClassMethod.isPublic()) {
                                iaomBugs.add(new IAOMBug(subClass, subClassMethod, PROTECTED, PUBLIC));
                            }
                            if (isPackagePrivate(superClassMethod)) {
                                if (subClassMethod.isPublic()) {
                                    iaomBugs.add(new IAOMBug(subClass, subClassMethod, PACKAGE_PRIVATE, PUBLIC));
                                } else if (subClassMethod.isProtected()) {
                                    iaomBugs.add(new IAOMBug(subClass, subClassMethod, PACKAGE_PRIVATE, PROTECTED));
                                }
                            }
                        }
                    }
                }
            }
        }

        return iaomBugs.stream().filter(bug -> !containsBug(superClassBugs, bug)).collect(Collectors.toList());
    }

    private List<Method> getMethodsExceptConstructors(JavaClass javaClass) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Method method : javaClass.getMethods()) {
            if (!Const.CONSTRUCTOR_NAME.equals(method.getName())) {
                methods.add(method);
            }
        }
        return methods;
    }

    private boolean isAccessibleFromSubClass(Method superClassMethod, JavaClass subClass, JavaClass superClass) {
        return superClassMethod.isProtected() ||
                (subClass.getPackageName().equals(superClass.getPackageName()) && isPackagePrivate(superClassMethod));
    }

    private boolean isOverridable(Method original, Method overrider) {
        return Objects.equals(original, overrider) && !original.isFinal();
    }

    private boolean isPackagePrivate(Method method) {
        return !(method.isPublic() || method.isProtected() || method.isPrivate());
    }

    private boolean isCloneMethodFromCloneable(JavaClass javaClass, Method method) {
        try {
            return javaClass.implementationOf(Repository.lookupClass(Cloneable.class)) && "clone".equals(method.getName());
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    private boolean containsBug(List<IAOMBug> bugs, IAOMBug bug) {
        for (IAOMBug other : bugs) {
            if (bug.isSameMethodAndAccessibility(other)) {
                return true;
            }
        }
        return false;
    }

    private static class IAOMBug {
        final JavaClass clazz;
        final Method method;
        final String oldAccessibility;
        final String newAccessibility;

        IAOMBug(JavaClass clazz, Method method, String oldAccessibility, String newAccessibility) {
            this.clazz = clazz;
            this.method = method;
            this.oldAccessibility = oldAccessibility;
            this.newAccessibility = newAccessibility;
        }

        boolean isSameMethodAndAccessibility(IAOMBug other) {
            return Objects.equals(method.getSignature(), other.method.getSignature())
                    && Objects.equals(oldAccessibility, other.oldAccessibility)
                    && Objects.equals(newAccessibility, other.newAccessibility);
        }
    }
}
