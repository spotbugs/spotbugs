/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.Set;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class InheritanceUnsafeGetResource extends BytecodeScanningDetector implements StatelessDetector {

    private final BugReporter bugReporter;

    private boolean classIsFinal;

    // private boolean methodIsVisibleToOtherPackages;
    private boolean classIsVisibleToOtherPackages;

    // private boolean methodIsFinal;
    private boolean methodIsStatic;

    int state = 0;

    int sawGetClass;

    boolean reportedForThisClass;

    String stringConstant;

    int prevOpcode;

    public InheritanceUnsafeGetResource(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(JavaClass obj) {
        classIsFinal = obj.isFinal();
        reportedForThisClass = false;
        classIsVisibleToOtherPackages = obj.isPublic() || obj.isProtected();
    }

    @Override
    public void visit(Method obj) {
        methodIsStatic = obj.isStatic();
        state = 0;
        sawGetClass = -100;
    }

    @Override
    public void sawOpcode(int seen) {
        if (reportedForThisClass) {
            return;
        }

        switch (seen) {
        case LDC:
            Constant constantValue = getConstantRefOperand();
            if (constantValue instanceof ConstantClass) {
                sawGetClass = -100;
            } else if (constantValue instanceof ConstantString) {
                stringConstant = ((ConstantString) constantValue).getBytes(getConstantPool());
            }
            break;

        case ALOAD_0:
            state = 1;
            break;
        case INVOKEVIRTUAL:
            if ("java/lang/Class".equals(getClassConstantOperand())
                    && ("getResource".equals(getNameConstantOperand()) || "getResourceAsStream".equals(getNameConstantOperand()))
                    && sawGetClass + 10 >= getPC()) {
                int priority = NORMAL_PRIORITY;
                if (prevOpcode == LDC && stringConstant != null && stringConstant.length() > 0 && stringConstant.charAt(0) == '/') {
                    priority = LOW_PRIORITY;
                } else {
                    priority = adjustPriority(priority);
                }
                bugReporter.reportBug(new BugInstance(this, "UI_INHERITANCE_UNSAFE_GETRESOURCE", priority)
                .addClassAndMethod(this).addSourceLine(this));
                reportedForThisClass = true;

            } else if (state == 1 && !methodIsStatic && !classIsFinal && classIsVisibleToOtherPackages
                    && "getClass".equals(getNameConstantOperand()) && "()Ljava/lang/Class;".equals(getSigConstantOperand())) {
                sawGetClass = getPC();
            }
            state = 0;
            break;
        default:
            state = 0;
            break;
        }
        if (seen != LDC) {
            stringConstant = null;
        }
        prevOpcode = seen;

    }

    /**
     * Adjust the priority of a warning about to be reported.
     *
     * @param priority
     *            initial priority
     * @return adjusted priority
     */
    private int adjustPriority(int priority) {

        try {
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

            if (!subtypes2.hasSubtypes(getClassDescriptor())) {
                priority++;
            } else {
                Set<ClassDescriptor> mySubtypes = subtypes2.getSubtypes(getClassDescriptor());

                String myPackagename = getThisClass().getPackageName();

                for (ClassDescriptor c : mySubtypes) {
                    if (c.equals(getClassDescriptor())) {
                        continue;
                    }
                    if (!c.getPackageName().equals(myPackagename)) {
                        priority--;
                        break;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
        return priority;
    }

}
