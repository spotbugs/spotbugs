/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class ConfusedInheritance extends PreorderVisitor implements Detector {

    private final BugReporter bugReporter;

    private JavaClass cls;

    public ConfusedInheritance(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        cls = classContext.getJavaClass();
        if (cls.isFinal()) {
            cls.accept(this);
        }
    }

    @Override
    public void visitField(Field obj) {
        if (obj.isProtected()) {
            bugReporter.reportBug(new BugInstance(this, "CI_CONFUSED_INHERITANCE", LOW_PRIORITY).addClass(cls).addField(
                    new FieldAnnotation(cls.getClassName(), obj.getName(), obj.getSignature(), obj.isStatic())));
        }
    }

    @Override
    public void report() {
    }
}
