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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class FindOverridableMethodCall extends OpcodeStackDetector {
    private final BugAccumulator bugAccumulator;

    public FindOverridableMethodCall(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            XMethod method = getXMethodOperand();
            OpcodeStack.Item item = stack.getStackItem(0);

            // The method operand may be null, if a class needed for the analysis is missing.
            if (method != null && !method.isPrivate() && !method.isFinal()) {
                if (item.getRegisterNumber() == 0
                        && Const.CONSTRUCTOR_NAME.equals(getMethodName())) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", LOW_PRIORITY)
                                    .addClassAndMethod(this).addString(method.getName()), this);

                } else if ("clone".equals(getMethodName())
                        && (("()" + getClassDescriptor().getSignature()).equals(getMethodSig())
                                || "()Ljava.lang.Object;".equals(getMethodSig()))
                        && item.getReturnValueOf() != null
                        && item.getReturnValueOf().equals(superClone(getXClass()))) {
                    bugAccumulator.accumulateBug(new BugInstance(this,
                            "MC_OVERRIDABLE_METHOD_CALL_IN_CLONE", NORMAL_PRIORITY)
                                    .addClassAndMethod(this).addString(method.getName()), this);
                }
            }
        }
    }

    private XMethod superClone(XClass clazz) {
        ClassDescriptor superD = clazz.getSuperclassDescriptor();
        XClass xSuper;
        try {
            xSuper = superD.getXClass();
            XMethod cloneMethod = xSuper.findMethod("clone", "()" + superD.getSignature(), false);
            if (cloneMethod == null) {
                cloneMethod = xSuper.findMethod("clone", "()Ljava/lang/Object;", false);
            }
            return cloneMethod;
        } catch (CheckedAnalysisException e) {
            AnalysisContext.logError("Could not find XClass object for " + superD + ".");
            return null;
        }
    }
}
