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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;


import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

@OpcodeStack.CustomUserValue
public class ReflectionIncreaseAccessibility extends OpcodeStackDetector {
    private boolean securityCheck = false;
    private boolean fieldNameUnderGet = false;

    private final BugAccumulator bugAccumulator;

    public ReflectionIncreaseAccessibility(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Method met) {
        securityCheck = false;
        super.visit(met);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        fieldNameUnderGet = false;

        if (seen != Const.INVOKEVIRTUAL || !getThisClass().isPublic() || !getMethod().isPublic()) {
            return;
        }

        OpcodeStack.Item obj = stack.getItemMethodInvokedOn(this);
        try {
            JavaClass cls = obj.getJavaClass();
            if (cls == null) {
                return;
            }
            XMethod met = getXMethodOperand();
            if (!securityCheck && obj.isInitialParameter() && obj.getXField() == null &&
                    "java.lang.Class".equals(cls.getClassName()) &&
                    "newInstance".equals(met.getName()) &&
                    "()Ljava/lang/Object;".equals(met.getSignature())) {
                bugAccumulator.accumulateBug(new BugInstance(this,
                        "REFLC_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_CLASS", NORMAL_PRIORITY)
                        .addClassAndMethod(this), this);
            } else if ("java.lang.Class".equals(cls.getClassName()) &&
                    "getDeclaredField".equals(met.getName()) &&
                    "(Ljava/lang/String;)Ljava/lang/reflect/Field;".equals(met.getSignature())) {
                OpcodeStack.Item param = stack.getStackItem(0);
                fieldNameUnderGet = param.isInitialParameter() && param.getXField() == null;
            } else if (!securityCheck && "java.lang.reflect.Field".equals(cls.getClassName()) &&
                    met.getName().startsWith("set")) {
                Boolean fieldIsFromParam = (Boolean) obj.getUserValue();
                if (fieldIsFromParam != null && fieldIsFromParam.booleanValue()) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "REFLF_REFLECTION_MAY_INCREASE_ACCESSIBILITY_OF_FIELD", NORMAL_PRIORITY)
                            .addClassAndMethod(this), this);
                }
            } else if ("java.lang.SecurityManager".equals(cls.getClassName()) &&
                    "checkPackageAccess".equals(met.getName()) &&
                    "(Ljava/lang/String;)V".equals(met.getSignature()) &&
                    getPackageName().equals((String) stack.getStackItem(0).getConstant())) {
                securityCheck = true;
            }
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if (fieldNameUnderGet) {
            stack.getStackItem(0).setUserValue(true);
        }

    }
}
