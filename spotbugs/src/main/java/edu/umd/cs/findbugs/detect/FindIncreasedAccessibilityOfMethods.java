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
        JavaClass[] superClasses;
        try {
            superClasses = subClass.getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return;
        }

        List<Method> subClassMethods = getMethodsExceptConstructors(subClass);
        for (JavaClass superClass : superClasses) {
            for (Method superClassMethod : getMethodsExceptConstructors(superClass)) {
                if (isAccessibleFromSubClass(superClassMethod, subClass, superClass)) {
                    for (Method subClassMethod : subClassMethods) {
                        if (isOverridable(superClassMethod, subClassMethod) && !isCloneMethodFromCloneable(subClass, superClassMethod)) {
                            if (superClassMethod.isProtected() && subClassMethod.isPublic()) {
                                bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                                        .addClassAndMethod(subClass, subClassMethod)
                                        .addString(PROTECTED)
                                        .addString(PUBLIC));
                            }
                            if (isPackagePrivate(superClassMethod)) {
                                if (subClassMethod.isPublic()) {
                                    bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                                            .addClassAndMethod(subClass, subClassMethod)
                                            .addString(PACKAGE_PRIVATE)
                                            .addString(PUBLIC));
                                } else if (subClassMethod.isProtected()) {
                                    bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                                            .addClassAndMethod(subClass, subClassMethod)
                                            .addString(PACKAGE_PRIVATE)
                                            .addString(PROTECTED));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void report() {
    }

    private boolean isPackagePrivate(Method method) {
        return !(method.isPublic() || method.isProtected() || method.isPrivate());
    }

    private boolean isAccessibleFromSubClass(Method superClassMethod, JavaClass subClass, JavaClass superClass) {
        return superClassMethod.isProtected() ||
                (subClass.getPackageName().equals(superClass.getPackageName()) && isPackagePrivate(superClassMethod));
    }

    private boolean isOverridable(Method original, Method overrider) {
        return Objects.equals(original, overrider) && !original.isFinal();
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

    private boolean isCloneMethodFromCloneable(JavaClass javaClass, Method method) {
        try {
            return javaClass.implementationOf(Repository.lookupClass(Cloneable.class)) && "clone".equals(method.getName());
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

}
