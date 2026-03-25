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
import edu.umd.cs.findbugs.bytecode.MemberUtils;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class FindIncreasedAccessibilityOfMethods implements Detector {
    private static final String PACKAGE_PRIVATE = "package private";
    private static final String PROTECTED = "protected";
    private static final String PUBLIC = "public";
    private static final JavaClass CLONEABLE;

    private final BugReporter bugReporter;

    static {
        JavaClass tmp = null;
        try {
            tmp = AnalysisContext.lookupSystemClass("java.lang.Cloneable");
        } catch (ClassNotFoundException cnfe) {
            AnalysisContext.reportMissingClass(cnfe);
        }
        CLONEABLE = tmp;
    }

    public FindIncreasedAccessibilityOfMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass cls = classContext.getJavaClass();
        for (Method m : cls.getMethods()) {
            visitMethod(cls, m);
        }
    }

    private void visitMethod(JavaClass cls, Method m) {
        String mName = m.getName();
        if (m.isPrivate() || !MemberUtils.isUserGenerated(m) || isCloneMethodFromCloneable(cls, m)
                || Const.CONSTRUCTOR_NAME.equals(mName) || Const.STATIC_INITIALIZER_NAME.equals(mName)) {
            return;
        }

        JavaClass[] superClasses;
        try {
            superClasses = cls.getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return;
        }

        for (final JavaClass superClass : superClasses) {
            for (final Method superMethod : superClass.getMethods()) {
                if (m.equals(superMethod)) {
                    if (superMethod.isProtected() && m.isPublic()) {
                        reportBug(cls, m, PROTECTED, PUBLIC);
                    } else if (isPackagePrivate(superMethod)) {
                        if (m.isPublic()) {
                            reportBug(cls, m, PACKAGE_PRIVATE, PUBLIC);
                        } else if (m.isProtected()) {
                            reportBug(cls, m, PACKAGE_PRIVATE, PROTECTED);
                        }
                    }
                    // we are only interested in the closest implementation in the ancestors, then we can return
                    return;
                }
            }
        }
    }

    private void reportBug(JavaClass cls, Method m, String superAccessibility, String subClassAccessibility) {
        bugReporter.reportBug(new BugInstance(this, "IAOM_DO_NOT_INCREASE_METHOD_ACCESSIBILITY", LOW_PRIORITY)
                .addClassAndMethod(cls, m)
                .addString(superAccessibility)
                .addString(subClassAccessibility));
    }

    @Override
    public void report() {
        // noop
    }

    private boolean isPackagePrivate(Method method) {
        return !(method.isPublic() || method.isProtected() || method.isPrivate());
    }

    private boolean isCloneMethodFromCloneable(JavaClass javaClass, Method method) {
        try {
            return javaClass.implementationOf(CLONEABLE) && "clone".equals(method.getName()) && "()Ljava/lang/Object;".equals(method.getSignature());
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }
}
