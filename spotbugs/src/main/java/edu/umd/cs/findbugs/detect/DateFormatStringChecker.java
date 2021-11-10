/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
 * Copyright (C) 2008 Google
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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DateFormatStringChecker extends OpcodeStackDetector {

    private static final ArrayList<String> AM_PM_HOURS_FLAG_1 = new ArrayList<>(Arrays.asList("a", "h"));
    private static final ArrayList<String> AM_PM_HOURS_FLAG_2 = new ArrayList<>(Arrays.asList("a", "K"));
    private static final ArrayList<String> MILITARY_HOURS_FLAG_1 = new ArrayList<>(Arrays.asList(null,"H","a"));
    private static final ArrayList<String> MILITARY_HOURS_FLAG_2 = new ArrayList<>(Arrays.asList(null,"k","a"));
    private static final ArrayList<String> WEEK_YEAR_FLAG_1 = new ArrayList<>(Arrays.asList("w","Y","M","d"));

    private static final ArrayList<Integer> CONST_ARRAY_LIST = new ArrayList<>(Arrays.asList((int)Const.INVOKESPECIAL,
            (int)Const.INVOKEVIRTUAL, (int)Const.INVOKESTATIC, (int)Const.INVOKEINTERFACE));

    private static final String SIG_CONSTANT_OPERAND = "Ljava/lang/String;)V";
    private static final String CLASS_CONSTANT_OPERAND = "java/text/SimpleDateFormat";
    private static final String NAME_CONSTANT_OPERAND_1 = "<init>";
    private static final String NAME_CONSTANT_OPERAND_2 = "applyPattern";
    private static final String BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";

    String dateFormatString;
    final BugReporter bugReporter;

    /**
     * DateFormatStringChecker class constructor
     * @param bugReporter
     */
    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Analyzes opcode to find SimpleDateFormat instances calling bad combinations of format flags
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     * @param seen
     */
    @Override
    public void sawOpcode(int seen) {
        ArrayList<ArrayList<String>> combinations = new ArrayList<>(Arrays.asList(
                AM_PM_HOURS_FLAG_1, AM_PM_HOURS_FLAG_2, MILITARY_HOURS_FLAG_1, MILITARY_HOURS_FLAG_2, WEEK_YEAR_FLAG_1
        ));

        if ((CONST_ARRAY_LIST.contains(seen)) && (stack.getStackDepth() > 0)) {
            Object formatStr = stack.getStackItem(0).getConstant();
            if (formatStr instanceof String) {
                this.dateFormatString = (String) formatStr;
                String cl = getClassConstantOperand();
                String nm = getNameConstantOperand();
                String sig = getSigConstantOperand();

                if (sig.indexOf(SIG_CONSTANT_OPERAND) >= 0
                        && (CLASS_CONSTANT_OPERAND.equals(cl) && (NAME_CONSTANT_OPERAND_1.equals(nm) ||
                        NAME_CONSTANT_OPERAND_2.equals(nm)))) {

                    if (runCheckStringForBadFlagCombo(dateFormatString, combinations)){
                        bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                                .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
                    }
                }
            }
        }
    }

    private boolean checkStringForBadFlagCombo(String stringToCheck, String missingFlag, ArrayList<String> requiredFlags) {
        boolean result = true;
        if ((missingFlag != null) && (stringToCheck.contains(missingFlag))) {
            result = false;
        }
        for (String flag : requiredFlags) {
            if (!stringToCheck.contains(flag)){
                result = false;
            }
        }
        return result;
    }

    private boolean runCheckStringForBadFlagCombo(String stringToCheck, ArrayList<ArrayList<String>> combinationsToRun){
        boolean result = false;
        for (ArrayList<String> combination : combinationsToRun){
            ArrayList<String> requiredFlags = new ArrayList<String>(combination.subList(1,combination.size()));
            String missingFlag = combination.get(0);
            if (checkStringForBadFlagCombo(dateFormatString, missingFlag, requiredFlags)){
                result = true;
            };
        }
        return result;
    }

}
