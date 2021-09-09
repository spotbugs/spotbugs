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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class FindDangerousPermissionCombination extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;

    public FindDangerousPermissionCombination(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKESPECIAL && Const.CONSTRUCTOR_NAME.equals(getNameConstantOperand())) {
            ClassDescriptor cd = getClassDescriptorOperand();
            if (cd != null && "Ljava/lang/reflect/ReflectPermission;".equals(cd.getSignature())) {
                String stringParam = (String) stack.getStackItem(0).getConstant();
                if (stringParam != null && "suppressAccessChecks".equals(stringParam)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "DPC_DANGEROUS_PERMISSION_COMBINATION", NORMAL_PRIORITY)
                            .addClassAndMethod(this).addString("ReflectPermission").addString("suppressAccessChecks"), this);
                }
            } else if (cd != null && "Ljava/lang/RuntimePermission;".equals(cd.getSignature())) {
                String stringParam = (String) stack.getStackItem(0).getConstant();
                if (stringParam != null && "createClassLoader".equals(stringParam)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "DPC_DANGEROUS_PERMISSION_COMBINATION", NORMAL_PRIORITY)
                            .addClassAndMethod(this).addString("RuntimePermission").addString("createClassLoader"), this);
                }
            }
        }
    }
}
