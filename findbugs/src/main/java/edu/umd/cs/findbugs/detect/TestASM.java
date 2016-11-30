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

import static org.apache.bcel.Constants.*;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.asm.AbstractFBMethodVisitor;
import edu.umd.cs.findbugs.asm.ClassNodeDetector;

/**
 * Sample detector, using ASM
 *
 * @author David Hovemeyer
 */
public class TestASM extends ClassNodeDetector {

    public TestASM(BugReporter bugReporter) {
        super(bugReporter);
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        if (Character.isUpperCase(name.charAt(0))) {
            BugInstance bug0 = new BugInstance(this, "NM_METHOD_NAMING_CONVENTION", NORMAL_PRIORITY).addClass(this).addMethod(
                    this.name, name, desc, access);
            bugReporter.reportBug(bug0);
        }
        return new AbstractFBMethodVisitor() {
            int prevOpcode;

            int prevPC;

            @Override
            public void visitInsn(int opcode) {
                prevOpcode = opcode;
                prevPC = getPC();
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String invokedName, String invokedDesc, boolean itf) {
                if (prevPC + 1 == getPC() && prevOpcode == I2D && opcode == INVOKESTATIC && "java/lang/Math".equals(owner)
                        && "ceil".equals(invokedName) && "(D)D".equals(invokedDesc)) {
                    BugInstance bug0 = new BugInstance(TestASM.this, "ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", NORMAL_PRIORITY);
                    MethodAnnotation methodAnnotation = MethodAnnotation.fromForeignMethod(TestASM.this.name, name, desc, access);
                    bug0.addClass(TestASM.this).addMethod(methodAnnotation);
                    bugReporter.reportBug(bug0);
                }
            }
        };

    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0 && (access & Opcodes.ACC_PUBLIC) != 0
                && !name.equals(name.toUpperCase())) {
            bugReporter.reportBug(new BugInstance(this, "NM_FIELD_NAMING_CONVENTION", Priorities.LOW_PRIORITY).addClass(this)
                    .addField(this.name, name, desc, access));
        }
        return null;
    }

}
