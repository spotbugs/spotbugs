/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

public class IteratorIdioms extends BytecodeScanningDetector implements StatelessDetector {
    private static final String NEXT_NAME = "next";

    private static final String HAS_NEXT_NAME = "hasNext";

    private final ClassDescriptor iteratorDescriptor = DescriptorFactory.createClassDescriptor(java.util.Iterator.class);

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private boolean sawNoSuchElement;

    private boolean sawCall;

    private boolean shouldVisitCode;

    private boolean hasNextAlwaysReturnsTrue;

    private boolean sawBranch;

    public IteratorIdioms(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
        try {
            if (subtypes2.isSubtype(classContext.getClassDescriptor(), iteratorDescriptor)) {
                hasNextAlwaysReturnsTrue = false;

                super.visitClassContext(classContext);
            }
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
    }


    @Override
    public void visit(Method method) {
        if (method.isPublic()
                && (NEXT_NAME.equals(method.getName()) || HAS_NEXT_NAME.equals(method.getName()))
                && method.getArgumentTypes().length == 0) {
            shouldVisitCode = true;
            super.visit(method);
        } else {
            shouldVisitCode = false;
        }
    }

    @Override
    public void visit(Code code) {
        if (!shouldVisitCode) {
            return;
        }
        sawNoSuchElement = false;
        sawCall = false;
        sawBranch = false;

        super.visit(code);

        if (!sawNoSuchElement && NEXT_NAME.equals(getMethodName())) {
            // Accumulate the bug but do not report it because we don't know if we have visited `hasNext()`
            BugInstance bug = new BugInstance(this, "IT_NO_SUCH_ELEMENT", sawCall ? LOW_PRIORITY : NORMAL_PRIORITY);
            bug.addClassAndMethod(this);
            bugAccumulator.accumulateBug(bug, this);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        if (hasNextAlwaysReturnsTrue) {
            bugAccumulator.clearBugs();
        } else {
            bugAccumulator.reportAccumulatedBugs();
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.NEW && "java/util/NoSuchElementException".equals(getClassConstantOperand())) {
            sawNoSuchElement = true;
        } else if (seen == Const.INVOKESPECIAL || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            sawCall = true;
            String name = getNameConstantOperand().toLowerCase();
            if (name.indexOf("next") >= 0
                    || name.indexOf("previous") >= 0) {
                sawNoSuchElement = true;
            }
        }

        // Unconditional `return true;`
        if (seen == Const.IRETURN && getPC() >= 1 && getPrevOpcode(1) == Const.ICONST_1 && !sawBranch) {
            hasNextAlwaysReturnsTrue = true;
        }

        sawBranch |= isBranch(seen);
    }
}
