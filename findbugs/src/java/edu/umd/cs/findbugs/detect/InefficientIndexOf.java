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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Use whenever possible String.indexOf(int) instead of String.indexOf(String),
 * or String.lastIndexOf(int) instead of String.lastIndexOf(String).
 *
 * @author Reto Merz
 */
public class InefficientIndexOf extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public InefficientIndexOf(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0 && getClassConstantOperand().equals("java/lang/String")) {

            boolean lastIndexOf = getNameConstantOperand().equals("lastIndexOf");
            if (lastIndexOf || getNameConstantOperand().equals("indexOf")) {

                int stackOff = -1;
                if (getSigConstantOperand().equals("(Ljava/lang/String;)I")) { // sig: String
                    stackOff = 0;
                } else if (getSigConstantOperand().equals("(Ljava/lang/String;I)I")) { // sig: String, int
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
