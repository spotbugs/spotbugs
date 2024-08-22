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

    private final BugReporter bugReporter;

    /** Class for defining bad combinations of date format flags and checking for their presence. */
    private static final class DateFormatRule {
        /** forbiddenFlag must be missing to match given DateFormatRule. If this field is {@code null}, rule does
         * not have a forbiddenFlag and the associated check will be bypassed. */
        @Nullable
        String forbiddenFlag;

        /** List of flags required in a pattern to match given DateFormatRule. */
        @NonNull
        List<String> requiredFlags;

        /**
         * DateFormatRule class constructor
         * @param forbiddenFlag - forbidden flag
         * @param requiredFlags - a list of required flags
         * */
        DateFormatRule(String forbiddenFlag, List<String> requiredFlags) {
            this.forbiddenFlag = forbiddenFlag;
            this.requiredFlags = requiredFlags;
        }

        /**
         * Checks if string does not contain a
         * forbiddenFlag and has all requiredFlags
         * @param dateFormatString - string to be checked
         * @return {@code true} if code equals to one of the elements
         * */
        boolean verify(String dateFormatString) {
            if (this.forbiddenFlag != null && dateFormatString.contains(this.forbiddenFlag))
                return false;
            for (String flag : requiredFlags)
                if (!dateFormatString.contains(flag))
                    return false;
            return true;
        }
    }

    /**
     * DateFormatStringChecker class constructor.
     * @param bugReporter - bugReporter
     */
    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Analyzes opcode to find SimpleDateFormat instances calling bad combinations of format flags.
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     * @param seen - seen
     */
    @Override
    public void sawOpcode(int seen) {
        int depth = stack.getStackDepth();
        if (!seenVerify(seen) || depth == 0)
            return;

        String dateFormatString = null;
        for (int i = 0; i < depth; i++) {
            Object formatStr = stack.getStackItem(i).getConstant();
            if (formatStr instanceof String) {
                dateFormatString = (String) formatStr;
                break;
            }
        }
        if (dateFormatString == null)
            return;

        String cl = getClassConstantOperand();
        String nm = getNameConstantOperand();
        String sig = getSigConstantOperand();
        if (!sigVerify(sig) || !nameVerify(nm) || !cl.equals("java/text/SimpleDateFormat"))
            return;
        if (!runDateFormatRuleVerify(dateFormatString))
            return;

        bugReporter.reportBug(new BugInstance(this, "FS_BAD_DATE_FORMAT_FLAG_COMBO", NORMAL_PRIORITY)
                .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
    }

    /**
     * Creates an instance of DateFormatRule using
     * the provided parameters (shorter alias)
     * @param ff - short for forbiddenFlag
     * @param rf - short for requiredFlags
     * @return DateFormat object with provided parameters
     * */
    private DateFormatRule initRule(String ff, String... rf) {
        return new DateFormatRule(ff, Arrays.asList(rf));
    }

    /**
     * Checks if seen code is equals to
     * one of the required by bytecodes
     * @param num - code to be checked
     * @return {@code true} if code equals to one of the elements
     * */
    private boolean seenVerify(int num) {
        /* List of bytecode instructions which could signal a method we should check. */
        int[] bytecodes = { Const.INVOKESPECIAL, Const.INVOKEVIRTUAL, Const.INVOKESTATIC, Const.INVOKEINTERFACE };
        for (int b : bytecodes)
            if (num == b)
                return true;
        return false;
    }

    /**
     * Checks if signal contains the element
     * specified in the list with additional
     * formatting (reuse, saving space)
     * @param str - signal to be checked
     * @return {@code true} if signal contains at least one of the elements
     * */
    private boolean sigVerify(String str) {
        String[] operands = { "", "Ljava/util/Locale;", "Ljava/text/DateFormatSymbols;" };
        for (String o : operands)
            if (str.contains(String.format("Ljava/lang/String;%s)V", o)))
                return true;
        return false;
    }

    /**
     * Checks if name has an exact match with
     * elements specified inside the function
     * @param str - name to be checked
     * @return {@code true} if name equals to one of the elements
     * */
    private boolean nameVerify(String str) {
        String[] operands = { "<init>", "applyPattern", "applyLocalizedPattern" };
        for (String o : operands)
            if (str.equals(o))
                return true;
        return false;
    }

    /**
     * Runs the check per each bad combination
     * (DateFormatRule objects) on the provided string.
     * @param str - string to be checked
     * @return {@code true} if given string any bad combination matches.
     */
    private boolean runDateFormatRuleVerify(String str) {
        // standard time (x2), military time (x2), week-year flag (x1)
        DateFormatRule[] badCombinations = {
                initRule("a", "h"), initRule("a", "K"),
                initRule(null, "H", "a"), initRule(null, "k", "a"),
                initRule("w", "Y", "M", "d")};

        for (DateFormatRule combination : badCombinations)
            if (combination.verify(str))
                return true;
        return false;
    }
}
