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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author Tagir Valeev
 */
public class FindComparatorProblems extends OpcodeStackDetector {
    private static final MethodDescriptor FLOAT_DESCRIPTOR = new MethodDescriptor("java/lang/Float", "compare", "(FF)I", true);
    private static final MethodDescriptor DOUBLE_DESCRIPTOR = new MethodDescriptor("java/lang/Double", "compare", "(DD)I", true);

    private boolean isComparator;
    private int lastEmptyStackPC;
    private List<int[]> twoDoublesInStack;
    private final BugAccumulator accumulator;

    public FindComparatorProblems(BugReporter reporter) {
        this.accumulator = new BugAccumulator(reporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        boolean comparator = Subtypes2.instanceOf(classContext.getClassDescriptor(), "java.util.Comparator");
        boolean comparable = Subtypes2.instanceOf(classContext.getClassDescriptor(), "java.lang.Comparable");
        isComparator = comparator;
        if (comparator || comparable) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public boolean shouldVisitCode(Code obj) {
        return !getMethodDescriptor().isStatic()
                && ((isComparator && getMethodName().equals("compare") && getMethodSig().endsWith(")I")) || ((getMethodName()
                        .equals("compareTo") && getMethodSig().equals("(L"+getClassName()+";)I"))));
    }

    @Override
    public void visit(Code obj) {
        this.twoDoublesInStack = new ArrayList<>();
        this.lastEmptyStackPC = 0;
        super.visit(obj);
        this.accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if(getStack().getStackDepth() == 0) {
            this.lastEmptyStackPC = getPC();
        }
        if((seen == DCMPG || seen == DCMPL || seen == FCMPL || seen == FCMPG) && getStack().getStackDepth() == 2) {
            int[] startEnd = new int[] {this.lastEmptyStackPC, getPC()};
            for(Iterator<int[]> iterator = twoDoublesInStack.iterator(); iterator.hasNext(); ) {
                int[] oldStartEnd = iterator.next();
                if(codeEquals(oldStartEnd, startEnd)) {
                    Item item1 = getStack().getStackItem(0);
                    Item item2 = getStack().getStackItem(1);
                    accumulator.accumulateBug(
                            new BugInstance("CO_COMPARETO_INCORRECT_FLOATING", NORMAL_PRIORITY).addClassAndMethod(this)
                            .addType(item1.getSignature())
                            .addMethod(item1.getSignature().equals("D")?DOUBLE_DESCRIPTOR:FLOAT_DESCRIPTOR).describe(MethodAnnotation.SHOULD_CALL)
                            .addValueSource(item1, this)
                            .addValueSource(item2, this), this);
                    iterator.remove();
                    return;
                }
            }
            twoDoublesInStack.add(startEnd);
        }
        if (seen == IRETURN) {
            OpcodeStack.Item top = stack.getStackItem(0);
            Object o = top.getConstant();
            if (o instanceof Integer && ((Integer)o).intValue() == Integer.MIN_VALUE) {
                accumulator.accumulateBug(
                        new BugInstance(this, "CO_COMPARETO_RESULTS_MIN_VALUE", NORMAL_PRIORITY).addClassAndMethod(this), this);
            }
        }
    }

    /**
     * @param oldStartEnd - int[] {oldStart, oldEnd}
     * @param startEnd - int[] {start, end}
     * @return true if code slices are the same
     */
    private boolean codeEquals(int[] oldStartEnd, int[] startEnd) {
        int oldStart = oldStartEnd[0];
        int oldEnd = oldStartEnd[1];
        int start = startEnd[0];
        int end = startEnd[1];
        if(end-start != oldEnd - oldStart) {
            return false;
        }
        for(int i=start; i<end; i++) {
            if(getCodeByte(i) != getCodeByte(i-start+oldStart)) {
                return false;
            }
        }
        return true;
    }
}
