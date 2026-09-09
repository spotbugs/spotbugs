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

import java.util.Arrays;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class PermissionsSuper extends OpcodeStackDetector {
    private boolean checkClass = false;
    private boolean checkMethod = false;

    private final BugAccumulator bugAccumulator;

    public PermissionsSuper(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        checkClass = false;
        try {
            checkClass = obj.instanceOf(Repository.lookupClass("java.security.SecureClassLoader"));
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        super.visit(obj);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(Method met) {
        checkMethod = checkClass &&
                "(Ljava/security/CodeSource;)Ljava/security/PermissionCollection;"
                        .equals(met.getSignature());
        super.visit(met);
    }

    @Override
    public void visit(Code obj) {
        if (checkMethod) {
            super.visit(obj);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.ARETURN) {
            XMethod origin = stack.getStackItem(0).getReturnValueOf();
            if (origin != null) {
                try {
                    if (Arrays.stream(getThisClass().getSuperClasses())
                            .anyMatch(cls -> cls.getClassName().equals(origin.getClassName())) &&
                            getMethod().getName().equals(origin.getName()) &&
                            getMethod().getSignature().equals(origin.getSignature())) {
                        return;
                    }
                } catch (ClassNotFoundException e) {
                    AnalysisContext.reportMissingClass(e);
                }
            }
            bugAccumulator.accumulateBug(new BugInstance(this,
                    "PERM_SUPER_NOT_CALLED_IN_GETPERMISSIONS", NORMAL_PRIORITY)
                    .addClassAndMethod(this), this);
        }
    }
}
