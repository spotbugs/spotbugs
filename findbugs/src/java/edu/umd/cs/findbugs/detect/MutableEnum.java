/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * @author Tagir Valeev
 */
public class MutableEnum extends OpcodeStackDetector {

    private final BugReporter reporter;
    private boolean skip;

    public MutableEnum(BugReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(!classContext.getJavaClass().isEnum() || !classContext.getJavaClass().isPublic()) {
            return;
        }
        boolean hasInterestingField = false;
        for(XField field : classContext.getXClass().getXFields()) {
            if(!field.isStatic() && !field.isFinal() && !field.isSynthetic()) {
                if(field.isPublic()) {
                    reporter.reportBug(new BugInstance("ME_MUTABLE_ENUM_FIELD", NORMAL_PRIORITY).addClass(classContext.getJavaClass())
                            .addField(field));
                } else {
                    hasInterestingField = true;
                }
            }
        }
        if(hasInterestingField) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public boolean shouldVisitCode(Code obj) {
        skip = false;
        if(getXMethod().isPublic() && getNumberMethodArguments() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void sawOpcode(int seen) {
        if(skip) {
            return;
        }
        if(isBranch(seen) || seen == ATHROW || isReturn(seen)) {
            skip = true;
        }
        if(seen == PUTFIELD) {
            XField xField = getXFieldOperand();
            if(xField != null && xField.getClassDescriptor().getClassName().equals(getClassName())) {
                Item val = getStack().getStackItem(0);
                if(val.isInitialParameter()) {
                    reporter.reportBug(new BugInstance("ME_ENUM_FIELD_SETTER", NORMAL_PRIORITY).addClassAndMethod(this).addField(xField)
                            .addSourceLine(this));
                }
            }
        }
    }
}
