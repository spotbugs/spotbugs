/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.ba.ClassContext;

public class FindRoughConstants extends BytecodeScanningDetector {

    static class BadConstant {
        double base;
        double factor;
        String replacement;
        double value;
        int basePriority;

        Set<Number> approxSet = new HashSet<Number>();

        BadConstant(double base, double factor, String replacement, int basePriority) {
            this.base = base;
            this.factor = factor;
            this.value = this.base * this.factor;
            this.replacement = replacement;
            this.basePriority = basePriority;
            BigDecimal valueBig = BigDecimal.valueOf(value);
            BigDecimal baseBig = BigDecimal.valueOf(base);
            BigDecimal factorBig = BigDecimal.valueOf(factor);
            for (int prec = 0; prec < 14; prec++) {
                addApprox(baseBig.round(new MathContext(prec, RoundingMode.FLOOR)).multiply(factorBig));
                addApprox(baseBig.round(new MathContext(prec, RoundingMode.CEILING)).multiply(factorBig));
                addApprox(valueBig.round(new MathContext(prec, RoundingMode.FLOOR)));
                addApprox(valueBig.round(new MathContext(prec, RoundingMode.CEILING)));
            }
        }

        @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
        public boolean exact(Number candidate) {
            if (candidate instanceof Double) {
                return candidate.doubleValue() == value;
            }
            return candidate.floatValue() == (float) value;
        }

        public double diff(double candidate) {
            return Math.abs(value - candidate) / value;
        }

        public boolean equalPrefix(Number candidate) {
            return approxSet.contains(candidate);
        }

        @SuppressFBWarnings("FE_FLOATING_POINT_EQUALITY")
        private void addApprox(BigDecimal roundFloor) {
            double approxDouble = roundFloor.doubleValue();
            if (approxDouble != value && Math.abs(approxDouble - value) / value < 0.001) {
                approxSet.add(approxDouble);
            }
            float approxFloat = roundFloor.floatValue();
            if (Math.abs(approxFloat - value) / value < 0.001) {
                approxSet.add(approxFloat);
                approxSet.add((double) approxFloat);
            }
        }
    }

    private static final BadConstant[] badConstants = new BadConstant[] {
        new BadConstant(Math.PI, 1, "Math.PI", HIGH_PRIORITY),
        new BadConstant(Math.PI, 1/2.0, "Math.PI/2", NORMAL_PRIORITY),
        new BadConstant(Math.PI, 1/3.0, "Math.PI/3", LOW_PRIORITY),
        new BadConstant(Math.PI, 1/4.0, "Math.PI/4", LOW_PRIORITY),
        new BadConstant(Math.PI, 2, "2*Math.PI", NORMAL_PRIORITY),
        new BadConstant(Math.E, 1, "Math.E", LOW_PRIORITY)
    };

    private final BugAccumulator bugAccumulator;

    private BugInstance lastBug;
    private int lastPriority;

    public FindRoughConstants(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(hasInterestingConstant(classContext.getJavaClass().getConstantPool())) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == LDC || seen == LDC_W || seen == LDC2_W) {
            Constant c = getConstantRefOperand();
            if (c instanceof ConstantFloat) {
                checkConst(((ConstantFloat) c).getBytes());
            } else if (c instanceof ConstantDouble) {
                checkConst(((ConstantDouble) c).getBytes());
            }
            return;
        }
        // Lower priority if the constant is put into array immediately or after the boxing:
        // this is likely to be just similar number in some predefined dataset (like lookup table)
        if(seen == INVOKESTATIC && lastBug != null) {
            if (getNextOpcode() == AASTORE
                    && getNameConstantOperand().equals("valueOf")
                    && (getClassConstantOperand().equals("java/lang/Double") || getClassConstantOperand().equals(
                            "java/lang/Float"))) {
                lastBug = ((BugInstance)lastBug.clone());
                lastBug.setPriority(lastPriority+1);
                bugAccumulator.forgetLastBug();
                bugAccumulator.accumulateBug(lastBug, this);
            }
        }
        lastBug = null;
    }

    private boolean hasInterestingConstant(ConstantPool cp) {
        for(Constant constant : cp.getConstantPool()) {
            if(constant instanceof ConstantFloat) {
                float val = ((ConstantFloat)constant).getBytes();
                if(isInteresting(val, val)) {
                    return true;
                }
            }
            if(constant instanceof ConstantDouble) {
                double val = ((ConstantDouble)constant).getBytes();
                if(isInteresting(val, val)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isInteresting(Number constValue, double candidate) {
        for (BadConstant badConstant : badConstants) {
            if(getPriority(badConstant, constValue, candidate) < IGNORE_PRIORITY) {
                return true;
            }
        }
        return false;
    }

    private int getPriority(BadConstant badConstant, Number constValue, double candidate) {
        if (badConstant.exact(constValue)) {
            return IGNORE_PRIORITY;
        }
        double diff = badConstant.diff(candidate);
        if (diff > 1e-3) {
            return IGNORE_PRIORITY;
        }
        if (badConstant.equalPrefix(constValue)) {
            return diff > 1e-4 ? badConstant.basePriority+1 :
                diff < 1e-6 ? badConstant.basePriority-1 : badConstant.basePriority;
        }
        if (diff > 1e-7) {
            return IGNORE_PRIORITY;
        }
        return badConstant.basePriority+1;
    }

    private void checkConst(Number constValue) {
        double candidate = constValue.doubleValue();
        if (Double.isNaN(candidate) || Double.isInfinite(candidate)) {
            return;
        }
        for (BadConstant badConstant : badConstants) {
            int priority = getPriority(badConstant, constValue, candidate);
            if(getNextOpcode() == FASTORE || getNextOpcode() == DASTORE) {
                priority++;
            }
            if(priority < IGNORE_PRIORITY) {
                lastPriority = priority;
                lastBug = new BugInstance(this, "CNT_ROUGH_CONSTANT_VALUE", priority).addClassAndMethod(this)
                        .addString(constValue.toString()).addString(badConstant.replacement);
                bugAccumulator.accumulateBug(lastBug, this);
                return;
            }
        }
    }
}
