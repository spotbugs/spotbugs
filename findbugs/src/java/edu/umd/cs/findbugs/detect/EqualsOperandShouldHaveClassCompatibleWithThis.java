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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FirstPassDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassSummary;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.ClassName;

public class EqualsOperandShouldHaveClassCompatibleWithThis extends OpcodeStackDetector implements FirstPassDetector {

    final BugReporter bugReporter;

    final BugAccumulator bugAccumulator;

    final ClassSummary classSummary = new ClassSummary();

    public EqualsOperandShouldHaveClassCompatibleWithThis(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        AnalysisContext context = AnalysisContext.currentAnalysisContext();
        context.setClassSummary(classSummary);
    }

    @Override
    public void visit(Code obj) {
        if ("equals".equals(getMethodName()) && "(Ljava/lang/Object;)Z".equals(getMethodSig())) {
            super.visit(obj);
            if (AnalysisContext.currentAnalysisContext().isApplicationClass(getThisClass())) {
                bugAccumulator.reportAccumulatedBugs();
            }
            bugAccumulator.clearBugs();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL) {
            if ("equals".equals(getNameConstantOperand()) && "(Ljava/lang/Object;)Z".equals(getSigConstantOperand())) {
                OpcodeStack.Item item = stack.getStackItem(1);
                ClassDescriptor c = DescriptorFactory.createClassDescriptorFromSignature(item.getSignature());
                check(c);

            } else if ("java/lang/Class".equals(getClassConstantOperand())
                    && ("isInstance".equals(getNameConstantOperand()) || "cast".equals(getNameConstantOperand()))) {
                OpcodeStack.Item item = stack.getStackItem(1);
                if ("Ljava/lang/Class;".equals(item.getSignature())) {
                    Object value = item.getConstant();
                    if (value instanceof String) {
                        ClassDescriptor c = DescriptorFactory.createClassDescriptor((String) value);
                        check(c);
                    }
                }

            }

        } else if (seen == INSTANCEOF || seen == CHECKCAST) {
            check(getClassDescriptorOperand());
        }

    }

    /**
     *
     */
    private void check(ClassDescriptor c) {
        OpcodeStack.Item item = stack.getStackItem(0);
        if (item.isInitialParameter() && item.getRegisterNumber() == 1) {
            ClassDescriptor thisClassDescriptor = getClassDescriptor();
            if (c.equals(thisClassDescriptor)) {
                return;
            }
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            try {
                if (!c.isArray() && (subtypes2.isSubtype(c, thisClassDescriptor) || subtypes2.isSubtype(thisClassDescriptor, c))) {
                    return;
                }

                Type thisType = Type.getType(thisClassDescriptor.getSignature());
                Type cType = Type.getType(c.getSignature());
                IncompatibleTypes check = IncompatibleTypes.getPriorityForAssumingCompatible(thisType, cType, false);
                int priority = check.getPriority();
                if ("java/lang/Object".equals(getSuperclassName()) && ClassName.isAnonymous(getClassName())) {
                    priority++;
                }
                bugAccumulator.accumulateBug(new BugInstance(this, "EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS", priority)
                .addClassAndMethod(this).addType(c).describe(TypeAnnotation.FOUND_ROLE), this);
                classSummary.checksForEqualTo(thisClassDescriptor, c);

            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }

        }
    }

}
