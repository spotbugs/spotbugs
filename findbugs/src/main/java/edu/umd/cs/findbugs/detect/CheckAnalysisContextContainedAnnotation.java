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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.OpcodeStack.JumpInfo;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.internalAnnotations.AnalysisContextContained;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;

public class CheckAnalysisContextContainedAnnotation extends OpcodeStackDetector.WithCustomJumpInfo {

    final BugReporter bugReporter;

    final BugAccumulator accumulator;

    private final boolean testingEnabled;

    public CheckAnalysisContextContainedAnnotation(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    final static ClassDescriptor ConstantAnnotation = DescriptorFactory.createClassDescriptor(StaticConstant.class);
    final static ClassDescriptor AnalysisContextContainedAnnotation = DescriptorFactory.createClassDescriptor(AnalysisContextContained.class);


    private boolean analysisContextContained(XClass xclass) {
        AnnotatedObject ao = xclass;
        do {
            if (ao.getAnnotation(AnalysisContextContainedAnnotation) != null) {
                return true;
            }
            ao = ao.getContainingScope();

        } while (ao != null);
        return false;

    }
    @Override
    public void visit(Field field) {
        if (!field.isStatic()) {
            return;
        }
        String signature = field.getSignature();
        if (signature.startsWith("Ljava/util/") && !"Ljava/util/regex/Pattern;".equals(signature)
                && !"Ljava/util/logging/Logger;".equals(signature) && !"Ljava/util/BitSet;".equals(signature)
                && !"Ljava/util/ResourceBundle;".equals(signature)
                && !"Ljava/util/Comparator;".equals(signature)
                && getXField().getAnnotation(ConstantAnnotation) == null) {
            boolean flagged = analysisContextContained(getXClass());

            bugReporter.reportBug(new BugInstance(this, "TESTING", flagged ? NORMAL_PRIORITY : LOW_PRIORITY).addClass(this).addField(this).addType(signature));

        }
    }
    @Override
    public void visit(Code code) {
        boolean interesting = testingEnabled;
        if (interesting) {
            // initialize any variables we want to initialize for the method
            super.visit(code); // make callbacks to sawOpcode for all opcodes
        }
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        default:
            break;
        case Constants.IF_ICMPEQ:
        case Constants.IF_ICMPNE:
            OpcodeStack.Item left = stack.getStackItem(1);
            OpcodeStack.Item right = stack.getStackItem(0);
            if (bad(left, right) || bad(right, left)) {
                accumulator.accumulateBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClassAndMethod(this)
                        .addValueSource(left, this).addValueSource(right, this)
                        .addString("Just check the sign of the result of compare or compareTo, not specific values such as 1 or -1"), this);
            }
            break;
        }

    }

    private boolean bad(Item left, Item right) {
        XMethod m = left.getReturnValueOf();

        if (m == null) {
            return false;
        }
        Object value = right.getConstant();
        if (!(value instanceof Integer) || ((Integer) value).intValue() == 0) {
            return false;
        }
        if (m.isStatic() || !m.isPublic()) {
            return false;
        }

        if ("compareTo".equals(m.getName()) && "(Ljava/lang/Object;)I".equals(m.getSignature())) {
            return true;
        }
        if ("compare".equals(m.getName()) && "(Ljava/lang/Object;Ljava/lang/Object;)I".equals(m.getSignature())) {
            return true;
        }

        return false;

    }

    @Override
    public JumpInfo customJumpInfo() {
        // TODO Auto-generated method stub
        return null;
    }

}
