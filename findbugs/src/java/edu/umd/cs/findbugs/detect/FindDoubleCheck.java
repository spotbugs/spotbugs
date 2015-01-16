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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.FindNoSideEffectMethods.MethodSideEffectStatus;
import edu.umd.cs.findbugs.detect.FindNoSideEffectMethods.NoSideEffectMethodsDatabase;

public class FindDoubleCheck extends OpcodeStackDetector {
    static final boolean DEBUG = false;

    int stage = 0;

    int startPC, endPC, assignPC;

    int count;

    boolean sawMonitorEnter;

    Set<FieldAnnotation> fields = new HashSet<FieldAnnotation>();

    Set<FieldAnnotation> twice = new HashSet<FieldAnnotation>();

    FieldAnnotation pendingFieldLoad;

    XField currentDoubleCheckField;

    int countSinceGetReference;

    int countSinceGetBoolean;

    private final BugReporter bugReporter;

    private final NoSideEffectMethodsDatabase nse;

    public FindDoubleCheck(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.nse = Global.getAnalysisCache().getDatabase(NoSideEffectMethodsDatabase.class);
    }

    @Override
    public void visit(Method obj) {
        if (DEBUG) {
            System.out.println(getFullyQualifiedMethodName());
        }
        super.visit(obj);
        fields.clear();
        twice.clear();
        stage = 0;
        count = 0;
        countSinceGetReference = 1000;
        countSinceGetBoolean = 1000;
        sawMonitorEnter = false;
        pendingFieldLoad = null;
        currentDoubleCheckField = null;
    }

    @Override
    public void sawOpcode(int seen) {
        if (DEBUG) {
            System.out.println(getPC() + "\t" + OPCODE_NAMES[seen] + "\t" + stage + "\t" + count + "\t" + countSinceGetReference);
        }

        if (seen == MONITORENTER) {
            sawMonitorEnter = true;
        }
        if (seen == GETFIELD || seen == GETSTATIC) {
            pendingFieldLoad = FieldAnnotation.fromReferencedField(this);
            if (DEBUG) {
                System.out.println("\t" + pendingFieldLoad);
            }
            String sig = getSigConstantOperand();
            if ("Z".equals(sig)) {
                countSinceGetBoolean = 0;
                countSinceGetReference++;
            } else if (sig.startsWith("L") || sig.startsWith("[")) {
                countSinceGetBoolean++;
                countSinceGetReference = 0;
            }
        } else {
            countSinceGetReference++;
        }
        switch (stage) {
        case 0:
            if (((seen == IFNULL || seen == IFNONNULL) && countSinceGetReference < 5)
                    || ((seen == IFEQ || seen == IFNE) && countSinceGetBoolean < 5)) {
                int b = getBranchOffset();
                if (DEBUG) {
                    System.out.println("branch offset is : " + b);
                }
                if (b > 0 && !(seen == IFNULL && b > 9) && !(seen == IFEQ && (b > 9 && b < 34))
                        && !(seen == IFNE && (b > 9 && b < 34)) && (!sawMonitorEnter)) {
                    fields.add(pendingFieldLoad);
                    startPC = getPC();
                    stage = 1;
                }
            }
            count = 0;
            break;
        case 1:
            if (seen == MONITORENTER) {
                stage = 2;
                count = 0;
            } else if (((seen == IFNULL || seen == IFNONNULL) && countSinceGetReference < 5)
                    || ((seen == IFEQ || seen == IFNE) && countSinceGetBoolean < 5)) {
                int b = getBranchOffset();
                if (b > 0 && (seen == IFNONNULL || b < 10)) {
                    fields.add(pendingFieldLoad);
                    startPC = getPC();
                    count = 0;
                }
            } else {
                count++;
                if (count > 10) {
                    stage = 0;
                }
            }
            break;
        case 2:
            if (((seen == IFNULL || seen == IFNONNULL) && countSinceGetReference < 5)
                    || ((seen == IFEQ || seen == IFNE) && countSinceGetBoolean < 5)) {
                if (getBranchOffset() >= 0 && fields.contains(pendingFieldLoad)) {
                    endPC = getPC();
                    stage++;
                    twice.add(pendingFieldLoad);
                    break;
                }
            }
            count++;
            if (count > 10) {
                stage = 0;
            }
            break;
        case 3:
            if (seen == PUTFIELD || seen == PUTSTATIC) {
                FieldAnnotation f = FieldAnnotation.fromReferencedField(this);
                if (DEBUG) {
                    System.out.println("\t" + f);
                }
                if (twice.contains(f) && !getNameConstantOperand().startsWith("class$")
                        && !"Ljava/lang/String;".equals(getSigConstantOperand())) {
                    XField declaration = getXFieldOperand();
                    if (declaration == null || !declaration.isVolatile()) {
                        bugReporter.reportBug(new BugInstance(this, "DC_DOUBLECHECK", NORMAL_PRIORITY).addClassAndMethod(this)
                                .addField(f).describe("FIELD_ON").addSourceLineRange(this, startPC, endPC));
                    } else {
                        if(declaration.isReferenceType()) {
                            currentDoubleCheckField = declaration;
                            assignPC = getPC();
                        }
                    }
                    stage++;
                }
            }
            break;
        case 4:
            if(currentDoubleCheckField != null) {
                switch(seen) {
                case MONITOREXIT:
                    stage++;
                    break;
                case INVOKEINTERFACE:
                case INVOKESPECIAL:
                case INVOKEVIRTUAL:
                    if(nse.is(getMethodDescriptorOperand(), MethodSideEffectStatus.OBJ, MethodSideEffectStatus.SE)) {
                        checkStackValue(getNumberArguments(getMethodDescriptorOperand().getSignature()));
                    }
                    break;
                case PUTFIELD:
                    checkStackValue(1);
                    break;
                case DASTORE:
                case FASTORE:
                case SASTORE:
                case LASTORE:
                case BASTORE:
                case CASTORE:
                case AASTORE:
                case IASTORE:
                    checkStackValue(2);
                    break;
                }
            }
            break;
        default:
            break;
        }
    }

    private void checkStackValue(int arg) {
        Item item = getStack().getStackItem(arg);
        if(item.getXField() == currentDoubleCheckField) {
            bugReporter.reportBug(new BugInstance(this, "DC_PARTIALLY_CONSTRUCTED", NORMAL_PRIORITY).addClassAndMethod(this)
                    .addField(currentDoubleCheckField).describe("FIELD_ON").addSourceLine(this).addSourceLine(this, assignPC)
                    .describe("SOURCE_LINE_STORED"));
            stage++;
        }
    }
}
