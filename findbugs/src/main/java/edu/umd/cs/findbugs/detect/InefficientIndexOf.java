/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import java.util.List;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Use whenever possible String.indexOf(int) instead of String.indexOf(String),
 * or String.lastIndexOf(int) instead of String.lastIndexOf(String).
 *
 * @author Reto Merz
 */
public class InefficientIndexOf extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    private static final List<MethodDescriptor> methods = Arrays.asList(
            new MethodDescriptor("java/lang/String", "indexOf", "(Ljava/lang/String;)I"),
            new MethodDescriptor("java/lang/String", "lastIndexOf", "(Ljava/lang/String;)I"),
            new MethodDescriptor("java/lang/String", "indexOf", "(Ljava/lang/String;I)I"),
            new MethodDescriptor("java/lang/String", "lastIndexOf", "(Ljava/lang/String;I)I")
            );

    public InefficientIndexOf(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(hasInterestingMethod(classContext.getJavaClass().getConstantPool(), methods)) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0 && "java/lang/String".equals(getClassConstantOperand())) {

            boolean lastIndexOf = "lastIndexOf".equals(getNameConstantOperand());
            if (lastIndexOf || "indexOf".equals(getNameConstantOperand())) {

                int stackOff = -1;
                if ("(Ljava/lang/String;)I".equals(getSigConstantOperand())) { // sig: String
                    stackOff = 0;
                } else if ("(Ljava/lang/String;I)I".equals(getSigConstantOperand())) { // sig: String, int
                    stackOff = 1;
                }
                if (stackOff > -1) {
                    OpcodeStack.Item item = stack.getStackItem(stackOff);
                    Object o = item.getConstant();
                    if (o != null && ((String) o).length() == 1) {
                        bugReporter.reportBug(new BugInstance(this, lastIndexOf ? "IIO_INEFFICIENT_LAST_INDEX_OF" : "IIO_INEFFICIENT_INDEX_OF", LOW_PRIORITY).addClassAndMethod(this)
                                .describe(StringAnnotation.STRING_MESSAGE).addCalledMethod(this).addSourceLine(this));
                    }
                }
            }
        }
    }

}
