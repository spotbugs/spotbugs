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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindRoughConstants extends OpcodeStackDetector {

    static class BadConstant {

        double base;

        double factor;

        String replacement;

        double value;

        Set<Number> approxSet = new HashSet<Number>();

        BadConstant(double base, double factor, String replacement) {
            this.base = base;
            this.factor = factor;
            this.value = this.base * this.factor;
            this.replacement = replacement;
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
            if (approxDouble != value && Math.abs(approxDouble - value) / value < 0.0001) {
                approxSet.add(approxDouble);
            }
            float approxFloat = roundFloor.floatValue();
            if (Math.abs(approxFloat - value) / value < 0.0001) {
                approxSet.add(approxFloat);
                approxSet.add((double) approxFloat);
            }
        }
    }

    private static final BadConstant[] badConstants = new BadConstant[] {
        new BadConstant(Math.PI, 1, "Math.PI"),
        new BadConstant(Math.PI, 1/2.0, "Math.PI/2"),
        new BadConstant(Math.PI, 1/3.0, "Math.PI/3"),
        new BadConstant(Math.PI, 1/4.0, "Math.PI/4"),
        new BadConstant(Math.PI, 2, "2*Math.PI"),
        new BadConstant(Math.E, 1, "Math.E")
    };

    private final BugReporter bugReporter;

    public FindRoughConstants(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
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
        }
    }

    private void checkConst(Number constValue) {
        double candidate = constValue.doubleValue();
        if (Double.isNaN(candidate) || Double.isInfinite(candidate)) {
            return;
        }
        for (BadConstant badConstant : badConstants) {
            if (badConstant.exact(constValue)) {
                continue;
            }
            double diff = badConstant.diff(candidate);
            if (diff > 0.0001) {
                continue;
            }
            if (badConstant.equalPrefix(constValue)) {
                bugReporter.reportBug(new BugInstance(this,
                        "CNT_ROUGH_CONSTANT_VALUE", NORMAL_PRIORITY)
                .addClassAndMethod(this).addSourceLine(this)
                .addString(constValue.toString())
                .addString(badConstant.replacement));
                return;
            }
            if (diff > 0.0000001) {
                continue;
            }
            bugReporter.reportBug(new BugInstance(this,
                    "CNT_ROUGH_CONSTANT_VALUE", LOW_PRIORITY)
            .addClassAndMethod(this).addSourceLine(this)
            .addString(constValue.toString())
            .addString(badConstant.replacement));
        }
    }
}
