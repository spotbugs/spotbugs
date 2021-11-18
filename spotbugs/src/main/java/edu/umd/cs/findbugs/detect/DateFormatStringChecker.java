package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DateFormatStringChecker extends OpcodeStackDetector {

    /** List of bytecode instructions which could signal a method we should check. */
    private static final List<Integer> CONST_ARRAY_LIST = new ArrayList<>(Arrays.asList((int) Const.INVOKESPECIAL,
            (int) Const.INVOKEVIRTUAL, (int) Const.INVOKESTATIC, (int) Const.INVOKEINTERFACE));

    /** List of operands matching target pattern. */
    private static final String SIG_CONSTANT_OPERAND_1 = "Ljava/lang/String;)V";
    private static final String SIG_CONSTANT_OPERAND_2 = "Ljava/lang/String;Ljava/util/Locale;)V";
    private static final String SIG_CONSTANT_OPERAND_3 = "Ljava/lang/String;Ljava/text/DateFormatSymbols;)V";

    private static final String CLASS_CONSTANT_OPERAND = "java/text/SimpleDateFormat";

    private static final String NAME_CONSTANT_OPERAND_1 = "<init>";
    private static final String NAME_CONSTANT_OPERAND_2 = "applyPattern";
    private static final String NAME_CONSTANT_OPERAND_3 = "applyLocalizedPattern";

    /** Creates DateFormatRule objects for different bad combinations of date format flags. */
    private static final DateFormatStringChecker.DateFormatRule STANDARD_TIME_FLAG_1 =
            new DateFormatStringChecker.DateFormatRule("a", Arrays.asList("h"));
    private static final DateFormatStringChecker.DateFormatRule STANDARD_TIME_FLAG_2 =
            new DateFormatStringChecker.DateFormatRule("a", Arrays.asList("K"));
    private static final DateFormatStringChecker.DateFormatRule MILITARY_FLAG_1 =
            new DateFormatStringChecker.DateFormatRule(null, Arrays.asList("H", "a"));
    private static final DateFormatStringChecker.DateFormatRule MILITARY_FLAG_2 =
            new DateFormatStringChecker.DateFormatRule(null, Arrays.asList("k", "a"));
    private static final DateFormatStringChecker.DateFormatRule WEEK_YEAR_FLAG =
            new DateFormatStringChecker.DateFormatRule("w", Arrays.asList("Y", "M", "d"));

    private static final String BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";
    final BugReporter bugReporter;

    /** Class for defining bad combinations of date format flags and checking for their presence. */
    private static final class DateFormatRule {
        /** forbiddenFlag must be missing to match given DateFormatRule. If this field is {@code null}, rule does
         * not have a forbiddenFlag and the associated check will be bypassed. */
        @Nullable
        String forbiddenFlag;

        /** List of flags required in a pattern to match given DateFormatRule. */
        @NonNull
        List<String> requiredFlags;

        DateFormatRule(String forbiddenFlag, List<String> requiredFlags) {
            this.forbiddenFlag = forbiddenFlag;
            this.requiredFlags = requiredFlags;
        }

        /** @return {@code true} if given dateFormatString does not have forbiddenFlag and has all required flags. */
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

    /**
     * DateFormatStringChecker class constructor.
     * @param bugReporter
     */
    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Analyzes opcode to find SimpleDateFormat instances calling bad combinations of format flags.
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     * @param seen
     */
    @Override
    public void sawOpcode(int seen) {
        String dateFormatString = null;

        List<DateFormatRule> badCombinations = new ArrayList<>(Arrays.asList(
                STANDARD_TIME_FLAG_1, STANDARD_TIME_FLAG_2, MILITARY_FLAG_1, MILITARY_FLAG_2, WEEK_YEAR_FLAG));

        if (!CONST_ARRAY_LIST.contains(seen) || stack.getStackDepth() == 0) {
            return;
        }

        int i = 0;
        while (i < stack.getStackDepth()) {
            Object formatStr = stack.getStackItem(i).getConstant();
            if (formatStr instanceof String) {
                dateFormatString = (String) formatStr;
                break;
            }
            i++;
        }

        if (dateFormatString == null) {
            return;
        }

        String cl = getClassConstantOperand();
        String nm = getNameConstantOperand();
        String sig = getSigConstantOperand();

        if ((sig.indexOf(SIG_CONSTANT_OPERAND_1) >= 0 || sig.indexOf(SIG_CONSTANT_OPERAND_2) >= 0 ||
                sig.indexOf(SIG_CONSTANT_OPERAND_3) >= 0) && CLASS_CONSTANT_OPERAND.equals(cl) &&
                (NAME_CONSTANT_OPERAND_1.equals(nm) || NAME_CONSTANT_OPERAND_2.equals(nm) ||
                        NAME_CONSTANT_OPERAND_3.equals(nm))) {

            if (runDateFormatRuleVerify(dateFormatString, badCombinations)) {
                bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                        .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                        .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
            }
        }
    }

    /**
     * Checks for presence of a list of bad combinations (DateFormatRule objects) in a given string.
     * @param stringToCheck
     * @param badCombinations
     * @return {@code true} if given stringToCheck matches at least one of the given bad DateFormatRule objects.
     */
    private boolean runDateFormatRuleVerify(String stringToCheck, List<DateFormatRule> badCombinations) {
        for (DateFormatRule combination : badCombinations) {
            if (combination.verify(stringToCheck)) {
                return true;
            }
        }
        return false;
    }
}
