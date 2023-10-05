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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class SynchronizationOnSharedBuiltinConstant extends OpcodeStackDetector {

    final Set<String> badSignatures;

    final BugAccumulator bugAccumulator;

    public SynchronizationOnSharedBuiltinConstant(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        badSignatures = new HashSet<>();
        badSignatures.addAll(Arrays.asList(new String[] { "Ljava/lang/Boolean;", "Ljava/lang/Double;", "Ljava/lang/Float;",
            "Ljava/lang/Byte;", "Ljava/lang/Character;", "Ljava/lang/Short;", "Ljava/lang/Integer;", "Ljava/lang/Long;" }));
    }

    private static boolean newlyConstructedObject(OpcodeStack.Item item) {
        XMethod method = item.getReturnValueOf();
        if (method == null) {
            return false;
        }
        return Const.CONSTRUCTOR_NAME.equals(method.getName());
    }

    private static boolean internedString(OpcodeStack.Item item) {
        XMethod method = item.getReturnValueOf();
        if (method == null) {
            return false;
        }
        return "intern".equals(method.getName());
    }

    private static final Pattern identified = Pattern.compile("\\p{Alnum}+");

    BugInstance pendingBug;

    int monitorEnterPC;

    String syncSignature;

    boolean isSyncOnBoolean;

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        accumulateBug();
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Const.MONITORENTER:
            OpcodeStack.Item top = stack.getStackItem(0);

            if (pendingBug != null) {
                accumulateBug();
            }
            monitorEnterPC = getPC();

            syncSignature = top.getSignature();
            isSyncOnBoolean = false;
            Object constant = top.getConstant();
            XField field = top.getXField();
            FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
            OpcodeStack.Item summary = fieldSummary.getSummary(field);

            if ("Ljava/lang/String;".equals(syncSignature)) {
                if (internedString(summary)) {
                    pendingBug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_INTERNED_STRING", NORMAL_PRIORITY)
                            .addClassAndMethod(this);
                } else if (constant instanceof String) {
                    pendingBug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_SHARED_CONSTANT", NORMAL_PRIORITY)
                            .addClassAndMethod(this);

                    String value = (String) constant;
                    if (identified.matcher(value).matches()) {
                        pendingBug.addString(value).describe(StringAnnotation.STRING_CONSTANT_ROLE);
                    }
                }

            } else if (badSignatures.contains(syncSignature)) {
                isSyncOnBoolean = "Ljava/lang/Boolean;".equals(syncSignature);
                int priority = NORMAL_PRIORITY;
                if (isSyncOnBoolean) {
                    priority--;
                }
                if (newlyConstructedObject(summary)) {
                    pendingBug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_UNSHARED_BOXED_PRIMITIVE", NORMAL_PRIORITY)
                            .addClassAndMethod(this).addType(syncSignature).addOptionalField(field)
                            .addOptionalLocalVariable(this, top);
                } else if (isSyncOnBoolean) {
                    pendingBug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_BOOLEAN", priority).addClassAndMethod(this)
                            .addOptionalField(field).addOptionalLocalVariable(this, top);
                } else {
                    pendingBug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE", priority).addClassAndMethod(this)
                            .addType(syncSignature).addOptionalField(field).addOptionalLocalVariable(this, top);
                }
            }
            break;
        case Const.MONITOREXIT:
            accumulateBug();
            break;
        default:
            break;
        }
    }

    private void accumulateBug() {
        if (pendingBug == null) {
            return;
        }
        bugAccumulator.accumulateBug(pendingBug, SourceLineAnnotation.fromVisitedInstruction(this, monitorEnterPC));
        pendingBug = null;
    }
}
