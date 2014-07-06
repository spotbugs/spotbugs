/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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
import edu.umd.cs.findbugs.DeepSubtypeAnalysis;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class ComparatorIdiom extends PreorderVisitor implements Detector {

    BugReporter bugReporter;

    public ComparatorIdiom(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void visit(JavaClass obj) {

        if (Subtypes2.instanceOf(obj, "java.util.Comparator") && !ClassName.isAnonymous(getClassName())
                && !Subtypes2.instanceOf(obj, "java.io.Serializable")) {
            int priority = NORMAL_PRIORITY;
            if (obj.isInterface() || obj.isAbstract()) {
                return;
            }

            double easilySerializable = 1.0;
            for (Field f : obj.getFields()) {
                try {
                    if (f.getName().startsWith("this$")) {
                        return;
                    }
                    String signature = f.getSignature();
                    char firstChar = signature.charAt(0);
                    if (firstChar == 'L' || firstChar == '[') {
                        easilySerializable *= DeepSubtypeAnalysis.isDeepSerializable(signature);
                    }
                } catch (ClassNotFoundException e) {
                    easilySerializable = 0.0;
                    break;
                }
            }

            if (easilySerializable < 0.9) {
                priority = LOW_PRIORITY;
            }

            bugReporter.reportBug(new BugInstance(this, "SE_COMPARATOR_SHOULD_BE_SERIALIZABLE", priority).addClass(this));

        }

    }

    @Override
    public void report() {

    }
}
