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

import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.bcel.PreorderDetector;

public class DontUseEnum extends PreorderDetector {

    BugReporter bugReporter;

    public DontUseEnum(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Method obj) {
        if (isReservedName(obj.getName())) {
            BugInstance bug = new BugInstance(this, "NM_FUTURE_KEYWORD_USED_AS_MEMBER_IDENTIFIER", isVisible(obj) ? HIGH_PRIORITY
                    : NORMAL_PRIORITY).addClassAndMethod(this);
            bugReporter.reportBug(bug);
        }
    }

    private boolean isVisible(FieldOrMethod obj) {
        return (obj.getAccessFlags() & ACC_PUBLIC) != 0 || (obj.getAccessFlags() & ACC_PROTECTED) != 0;
    }

    private boolean isReservedName(String name) {
        return "enum".equals(name) || "assert".equals(name);
    }

    @Override
    public void visit(Field obj) {
        if (isReservedName(obj.getName())) {
            BugInstance bug = new BugInstance(this, "NM_FUTURE_KEYWORD_USED_AS_MEMBER_IDENTIFIER", isVisible(obj) ? HIGH_PRIORITY
                    : NORMAL_PRIORITY).addClass(this).addField(this);
            bugReporter.reportBug(bug);
        }
    }

    @Override
    public void visit(LocalVariable obj) {
        if (isReservedName(obj.getName())) {
            LocalVariableAnnotation var = new LocalVariableAnnotation(obj.getName(), obj.getIndex(), obj.getStartPC());
            SourceLineAnnotation source = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, obj.getStartPC());
            BugInstance bug = new BugInstance(this, "NM_FUTURE_KEYWORD_USED_AS_IDENTIFIER", NORMAL_PRIORITY)
            .addClassAndMethod(this).add(var).add(source);
            bugReporter.reportBug(bug);
        }
    }

}
