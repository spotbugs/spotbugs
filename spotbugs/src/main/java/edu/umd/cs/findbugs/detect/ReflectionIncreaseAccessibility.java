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

public class ReflectionIncreaseAccessibility extends OpcodeStackDetector {
    private boolean securityCheck = false;

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
        if (seen == Const.INVOKEVIRTUAL) {
            OpcodeStack.Item obj = stack.getItemMethodInvokedOn(this);
            try {
                JavaClass cls = obj.getJavaClass();
                if (cls == null) {
                    return;
                }
                XMethod met = getXMethodOperand();
                if (!securityCheck && obj.isInitialParameter() && "java.lang.Class".equals(cls.getClassName()) &&
                        "newInstance".equals(met.getName()) && "()Ljava/lang/Object;".equals(met.getSignature()) &&
                        getMethod().isPublic()) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "REFL_REFLECTION_INCREASES_ACCESSIBILITY_OF_CLASS", NORMAL_PRIORITY)
                                    .addClassAndMethod(this), this);
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
    }
}
