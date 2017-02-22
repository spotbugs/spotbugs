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

import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class SynchronizeOnClassLiteralNotGetClass extends OpcodeStackDetector {

    BugReporter bugReporter;

    public SynchronizeOnClassLiteralNotGetClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code code) {
        boolean interesting = !getMethod().isStatic() && !getThisClass().isFinal();

        if (interesting) {
            // initialize any variables we want to initialize for the method
            pendingBug = null;
            super.visit(code); // make callbacks to sawOpcode for all opcodes
            assert (pendingBug == null);
        }
    }

    /*
     * Looking for ALOAD 0 INVOKEVIRTUAL
     * java/lang/Object.getClass()Ljava/lang/Class; DUP ASTORE 1 MONITORENTER
     */
    int state = 0;

    boolean seenPutStatic, seenGetStatic;

    BugInstance pendingBug;

    @Override
    public void sawOpcode(int seen) {
        if (pendingBug != null) {
            if (seen == PUTSTATIC) {
                String classConstantOperand = getClassConstantOperand();
                String thisClassName = getThisClass().getClassName().replace('.', '/');
                if (classConstantOperand.equals(thisClassName)) {
                    seenPutStatic = true;
                }
            } else if (seen == GETSTATIC) {
                String classConstantOperand = getClassConstantOperand();
                String thisClassName = getThisClass().getClassName().replace('.', '/');
                if (classConstantOperand.equals(thisClassName)) {
                    seenGetStatic = true;
                }
            } else if (seen == MONITOREXIT) {
                int priority = LOW_PRIORITY;
                if (seenPutStatic || seenGetStatic) {
                    priority--;
                }
                try {
                    Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
                    Set<ClassDescriptor> directSubtypes = subtypes2.getDirectSubtypes(getClassDescriptor());
                    if (!directSubtypes.isEmpty()) {
                        for (ClassDescriptor sub : directSubtypes) {
                            pendingBug.addClass(sub).describe(ClassAnnotation.SUBCLASS_ROLE);
                        }
                        priority--;
                    }
                } catch (ClassNotFoundException e) {
                    bugReporter.reportMissingClass(e);
                }
                pendingBug.setPriority(priority);
                bugReporter.reportBug(pendingBug);
                pendingBug = null;
            }
            return;
        }
        switch (state) {
        case 0:
            if (seen == ALOAD_0) {
                state = 1;
            }
            break;
        case 1:
            if (seen == INVOKEVIRTUAL && "getClass".equals(getNameConstantOperand())
            && "()Ljava/lang/Class;".equals(getSigConstantOperand())) {
                state = 2;
            } else {
                state = 0;
            }
            break;
        case 2:
            if (seen == DUP) {
                state = 3;
            } else {
                state = 0;
            }
            break;
        case 3:
            if (isRegisterStore()) {
                state = 4;
            } else {
                state = 0;
            }
            break;
        case 4:
            if (seen == MONITORENTER) {
                pendingBug = new BugInstance(this, "WL_USING_GETCLASS_RATHER_THAN_CLASS_LITERAL", NORMAL_PRIORITY)
                .addClassAndMethod(this).addSourceLine(this);
            }
            state = 0;
            seenGetStatic = seenPutStatic = false;
            break;
        default:
            break;
        }
    }

}
