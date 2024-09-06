/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.List;

public class DateFormatStringChecker extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    /**
     * Stores required and forbidden flags. Then confirms the absence of a forbidden
     * flag and presence of all required flags via verify method
     */
    private static final class DateFormatRule {

        /**
         * forbiddenFlag must be missing to match given DateFormatRule. If this field
         * is {@code null}, rule does not have a forbiddenFlag and the associated check
         * will be bypassed.
         */
        @Nullable
        String forbiddenFlag;

        /**
         * List of flags required in a pattern to match given DateFormatRule.
         */
        @NonNull
        List<String> requiredFlags;

        DateFormatRule(String forbiddenFlag, String... requiredFlags) {
            this.forbiddenFlag = forbiddenFlag;
            this.requiredFlags = Arrays.asList(requiredFlags);
        }

        /**
         * Checks if string does not contain a forbiddenFlag and has all requiredFlags
         *
         * @param dateFormatString - string to be checked
         * @return {@code true} if code equals to one of the elements
         */
        boolean verify(String dateFormatString) {
            if (this.forbiddenFlag != null && dateFormatString.contains(this.forbiddenFlag)) {
                return false;
            }
            for (String flag : requiredFlags) {
                if (!dateFormatString.contains(flag)) {
                    return false;
                }
            }
            return true;
        }
    }

    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (!(seen == Const.INVOKESPECIAL || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESTATIC
                || seen == Const.INVOKEINTERFACE) || stack.getStackDepth() == 0
                || !"java/text/SimpleDateFormat".equals(getClassConstantOperand())
                || !Arrays.asList(Const.CONSTRUCTOR_NAME, "applyPattern", "applyLocalizedPattern")
                        .contains(getNameConstantOperand())) {
            return;
        }

        int idx;
        switch (getSigConstantOperand()) {
        case "(Ljava/lang/String;)V":
            idx = 0;
            break;
        case "(Ljava/lang/String;Ljava/util/Locale;)V":
        case "(Ljava/lang/String;Ljava/text/DateFormatSymbols;)V":
            idx = 1;
            break;
        default:
            return;
        }

        String dateFormatString = (String) stack.getStackItem(idx).getConstant();
        if (dateFormatString != null && runDateFormatRuleVerify(dateFormatString)) {
            bugReporter.reportBug(new BugInstance(this, "FS_BAD_DATE_FORMAT_FLAG_COMBO", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                    .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
        }
    }

    /**
     * Runs the check per each bad combination (DateFormatRule objects) on the provided string.
     *
     * @param dateFormat - string to be checked
     * @return {@code true} if given string matches any bad combination.
     */
    private boolean runDateFormatRuleVerify(String dateFormat) {
        // standard time (x2), military time (x2), week-year flag (x1)
        DateFormatRule[] badCombinations = {
            new DateFormatRule("a", "h"), new DateFormatRule("a", "K"),
            new DateFormatRule(null, "H", "a"), new DateFormatRule(null, "k", "a"),
            new DateFormatRule("w", "Y", "M", "d") };

        for (DateFormatRule combination : badCombinations) {
            if (combination.verify(dateFormat)) {
                return true;
            }
        }
        return false;
    }
}
