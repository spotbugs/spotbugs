/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SwitchHandler;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.SourceFile;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.charsets.UTF8;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

public class SwitchFallthrough extends OpcodeStackDetector implements StatelessDetector {
    private static final boolean DEBUG = SystemProperties.getBoolean("switchFallthrough.debug");

    private static final boolean LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT = SystemProperties.getBoolean("findbugs.sf.comment");

    private SwitchHandler switchHdlr;

    private boolean reachable;

    private final BugAccumulator bugAccumulator;

    private int lastPC;
    private int biggestJumpTarget;

    private final BitSet potentiallyDeadStores = new BitSet();

    private final Set<XField> potentiallyDeadFields = new HashSet<XField>();

    private BitSet potentiallyDeadStoresFromBeforeFallthrough = new BitSet();

    private Set<XField> potentiallyDeadFieldsFromBeforeFallthrough = new HashSet<XField>();

    private LocalVariableAnnotation deadStore = null;

    private int priority;

    private int fallthroughDistance;

    public SwitchFallthrough(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

    Collection<SourceLineAnnotation> found = new LinkedList<SourceLineAnnotation>();

    @Override
    public void visit(Code obj) {
        if (DEBUG) {
            System.out.printf("%nVisiting %s%n", getMethodDescriptor());
        }
        reachable = false;
        lastPC = 0;
        biggestJumpTarget = -1;
        found.clear();
        switchHdlr = new SwitchHandler();
        clearAllDeadStores();
        deadStore = null;
        priority = NORMAL_PRIORITY;
        fallthroughDistance = 1000;
        enumType = null;
        super.visit(obj);
        enumType = null;
        if (!found.isEmpty()) {
            if (found.size() >= 4 && priority == NORMAL_PRIORITY) {
                priority = LOW_PRIORITY;
            }
            for (SourceLineAnnotation s : found) {
                bugAccumulator.accumulateBug(new BugInstance(this, "SF_SWITCH_FALLTHROUGH", priority).addClassAndMethod(this), s);
            }
        }

        bugAccumulator.reportAccumulatedBugs();
    }

    private void foundSwitchNoDefault(SourceLineAnnotation s) {
        LineNumberTable table = getCode().getLineNumberTable();

        if (table != null) {
            int startLine = s.getStartLine();
            int prev = Integer.MIN_VALUE;
            for (LineNumber ln : table.getLineNumberTable()) {
                int thisLineNumber = ln.getLineNumber();
                if (thisLineNumber < startLine && thisLineNumber > prev && ln.getStartPC() < s.getStartBytecode()) {
                    prev = thisLineNumber;
                }
            }
            int diff = startLine - prev;
            if (diff > 5) {
                return;
            }

            bugAccumulator.accumulateBug(new BugInstance(this, "SF_SWITCH_NO_DEFAULT", NORMAL_PRIORITY).addClassAndMethod(this),
                    s);
        }

    }

    XClass enumType = null;

    @Override
    public void sawOpcode(int seen) {
        boolean isDefaultOffset = switchHdlr.getDefaultOffset() == getPC();
        boolean isCaseOffset = switchHdlr.isOnSwitchOffset(this);

        if (DEBUG) {
            if (seen == GOTO) {
                System.out.printf("%4d: goto %-7d %s %s %s %d%n", getPC(), getBranchTarget(), reachable, isCaseOffset,
                        isDefaultOffset, switchHdlr.stackSize());
            } else {
                System.out.printf("%4d: %-12s %s %s %s %d%n", getPC(), OPCODE_NAMES[seen], reachable, isCaseOffset,
                        isDefaultOffset, switchHdlr.stackSize());
            }
        }

        if (reachable && (isDefaultOffset || isCaseOffset)) {
            if (DEBUG) {
                System.out.println("Fallthrough at : " + getPC() + ": " + OPCODE_NAMES[seen]);
            }
            fallthroughDistance = 0;
            potentiallyDeadStoresFromBeforeFallthrough = (BitSet) potentiallyDeadStores.clone();
            potentiallyDeadFieldsFromBeforeFallthrough = new HashSet<XField>(potentiallyDeadFields);
            if (!hasFallThruComment(lastPC + 1, getPC() - 1)) {
                if (!isDefaultOffset) {
                    SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstructionRange(
                            getClassContext(), this, lastPC, getPC());
                    found.add(sourceLineAnnotation);
                } else if ( getPC() >= biggestJumpTarget) {
                    SourceLineAnnotation sourceLineAnnotation = switchHdlr.getCurrentSwitchStatement(this);
                    if (DEBUG) {
                        System.out.printf("Found fallthrough to default offset at %d (BJT is %d)%n", getPC(), biggestJumpTarget);
                    }

                    foundSwitchNoDefault(sourceLineAnnotation);
                }
            }

        }

        if (isBranch(seen) || isSwitch(seen) || seen == GOTO || seen == ARETURN || seen == IRETURN || seen == RETURN
                || seen == LRETURN || seen == DRETURN || seen == FRETURN) {
            clearAllDeadStores();
        }

        if (seen == GETFIELD && stack.getStackDepth() > 0) {
            OpcodeStack.Item top = stack.getStackItem(0);
            if (top.getRegisterNumber() == 0) {
                potentiallyDeadFields.remove(getXFieldOperand());
            }
        }

        else if (seen == PUTFIELD && stack.getStackDepth() >= 2) {
            OpcodeStack.Item obj = stack.getStackItem(1);
            if (obj.getRegisterNumber() == 0) {
                XField f = getXFieldOperand();
                if (potentiallyDeadFields.contains(f) && potentiallyDeadFieldsFromBeforeFallthrough.contains(f)) {
                    // killed store
                    priority = HIGH_PRIORITY;
                    bugAccumulator.accumulateBug(new BugInstance(this, "SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", priority)
                    .addClassAndMethod(this).addField(f), this);

                }
                potentiallyDeadFields.add(f);
            }
        }

        if (seen == ATHROW) {
            int sz = edu.umd.cs.findbugs.visitclass.Util.getSizeOfSurroundingTryBlock(getMethod(), (String) null, getPC());
            if (sz == Integer.MAX_VALUE) {

                BitSet dead = new BitSet();
                dead.or(potentiallyDeadStores);
                dead.and(potentiallyDeadStoresFromBeforeFallthrough);
                if (dead.cardinality() > 0) {
                    int register = dead.nextSetBit(0);
                    priority = HIGH_PRIORITY;
                    deadStore = LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), register, getPC() - 1, getPC());
                    bugAccumulator.accumulateBug(new BugInstance(this, "SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW",
                            priority).addClassAndMethod(this).add(deadStore), this);
                }
            }
            clearAllDeadStores();
        }

        if (isRegisterLoad()) {
            potentiallyDeadStores.clear(getRegisterOperand());
        } else if (isRegisterStore() && !atCatchBlock()) {
            int register = getRegisterOperand();
            if (potentiallyDeadStores.get(register) && (potentiallyDeadStoresFromBeforeFallthrough.get(register))) {
                // killed store
                priority = HIGH_PRIORITY;
                deadStore = LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), register, getPC() - 1, getPC());
                bugAccumulator.accumulateBug(new BugInstance(this, "SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", priority)
                .addClassAndMethod(this).add(deadStore), this);

            }
            potentiallyDeadStores.set(register);
        }


        if (seen == INVOKEVIRTUAL && "ordinal".equals(getNameConstantOperand()) && "()I".equals(getSigConstantOperand())) {
            XClass c = getXClassOperand();
            if (c != null) {
                ClassDescriptor superclassDescriptor = c.getSuperclassDescriptor();
                if (superclassDescriptor != null && "java/lang/Enum".equals(superclassDescriptor.getClassName())) {
                    enumType = c;
                }
                if (DEBUG) {
                    System.out.println("Saw " + enumType + ".ordinal()");
                }
            }
        } else if (seen != TABLESWITCH && seen != LOOKUPSWITCH && seen != IALOAD) {
            enumType = null;
        }

        switch (seen) {
        case TABLESWITCH:
        case LOOKUPSWITCH:
            if (justSawHashcode)
            {
                break; // javac compiled switch statement
            }
            reachable = false;
            biggestJumpTarget = -1;
            switchHdlr.enterSwitch(this, enumType);
            if (DEBUG) {
                System.out.printf("  entered switch, default is %d%n", switchHdlr.getDefaultOffset());
            }
            break;

        case GOTO_W:
        case GOTO:
            if (biggestJumpTarget < getBranchTarget()) {
                biggestJumpTarget = getBranchTarget();
                if (DEBUG) {
                    System.out.printf("  Setting BJT to %d%n", biggestJumpTarget);
                }
            }

            reachable = false;
            break;

        case ATHROW:
        case RETURN:
        case ARETURN:
        case IRETURN:
        case LRETURN:
        case DRETURN:
        case FRETURN:
            reachable = false;
            break;

        case INVOKESTATIC:
            reachable = !("exit".equals(getNameConstantOperand()) && "java/lang/System".equals(getClassConstantOperand()));
            break;

        default:
            reachable = true;
        }

        justSawHashcode =   seen == INVOKEVIRTUAL && "hashCode".equals(getNameConstantOperand()) && "()I".equals(getSigConstantOperand());
        lastPC = getPC();
        fallthroughDistance++;
    }

    boolean justSawHashcode;
    /**
     *
     */
    private void clearAllDeadStores() {
        potentiallyDeadStores.clear();
        potentiallyDeadStoresFromBeforeFallthrough.clear();
        potentiallyDeadFields.clear();
        potentiallyDeadFieldsFromBeforeFallthrough.clear();
    }

    private boolean hasFallThruComment(int startPC, int endPC) {
        if (LOOK_IN_SOURCE_FOR_FALLTHRU_COMMENT) {
            BufferedReader r = null;
            try {
                SourceLineAnnotation srcLine = SourceLineAnnotation.fromVisitedInstructionRange(this, lastPC, getPC());
                SourceFinder sourceFinder = AnalysisContext.currentAnalysisContext().getSourceFinder();
                SourceFile sourceFile = sourceFinder.findSourceFile(srcLine.getPackageName(), srcLine.getSourceFile());

                int startLine = srcLine.getStartLine();
                int numLines = srcLine.getEndLine() - startLine - 1;
                if (numLines <= 0) {
                    return false;
                }
                r = UTF8.bufferedReader(sourceFile.getInputStream());
                for (int i = 0; i < startLine; i++) {
                    String line = r.readLine();
                    if (line == null) {
                        return false;
                    }
                }
                for (int i = 0; i < numLines; i++) {
                    String line = r.readLine();
                    if (line == null) {
                        return false;
                    }
                    line = line.toLowerCase();
                    if (line.indexOf("fall") >= 0 || line.indexOf("nobreak") >= 0) {
                        return true;
                    }
                }
            } catch (IOException ioe) {
                // Problems with source file, mean report the bug
            } finally {
                try {
                    if (r != null) {
                        r.close();
                    }
                } catch (IOException ioe) {
                }
            }
        }
        return false;
    }
}
