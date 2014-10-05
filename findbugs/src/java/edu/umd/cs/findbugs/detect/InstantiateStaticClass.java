/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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

import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class InstantiateStaticClass extends BytecodeScanningDetector {
    private final BugReporter bugReporter;

    public InstantiateStaticClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {

        if ((seen == INVOKESPECIAL) && "<init>".equals(getNameConstantOperand()) && "()V".equals(getSigConstantOperand())) {
            XClass xClass = getXClassOperand();
            if (xClass == null) {
                return;
            }
            String clsName = getClassConstantOperand();
            if ("java/lang/Object".equals(clsName)) {
                return;
            }

            // ignore superclass synthesized ctor calls
            if ("<init>".equals(getMethodName()) && (getPC() == 1)) {
                return;
            }

            // ignore the typesafe enumerated constant pattern
            if ("<clinit>".equals(getMethodName()) && (getClassName().equals(clsName))) {
                return;
            }

            if (isStaticOnlyClass(xClass)) {
                bugReporter.reportBug(new BugInstance(this, "ISC_INSTANTIATE_STATIC_CLASS", LOW_PRIORITY).addClassAndMethod(
                        this).addSourceLine(this));
            }
        }

    }

    private boolean isStaticOnlyClass(XClass xClass)  {

        if (xClass.getInterfaceDescriptorList().length > 0) {
            return false;
        }
        ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
        if (superclassDescriptor == null) {
            return false;
        }
        String superClassName = superclassDescriptor.getClassName();
        if (!"java/lang/Object".equals(superClassName)) {
            return false;
        }
        int staticCount = 0;

        List<? extends XMethod> methods = xClass.getXMethods();
        for (XMethod m : methods) {
            // !m.isSynthetic(): bug #1282: No warning should be generated if only static methods are synthetic
            if (m.isStatic() && !m.isSynthetic()) {
                staticCount++;
            } else if (!"<init>".equals(m.getName()) || !"()V".equals(m.getSignature())) {
                return false;
            }
        }

        List<? extends XField> fields = xClass.getXFields();
        for (XField f : fields) {
            if (f.isStatic()) {
                staticCount++;
            } else if (!f.isPrivate()) {
                return false;
            }
        }

        if (staticCount == 0) {
            return false;
        }
        return true;

    }

}
