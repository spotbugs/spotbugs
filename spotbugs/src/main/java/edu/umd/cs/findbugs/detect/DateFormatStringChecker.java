package edu.umd.cs.findbugs.detect;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class DateFormatStringChecker extends OpcodeStackDetector {

    private static final List<String> AM_PM_HOURS_FLAG_1 = new ArrayList<>(Arrays.asList("a", "h"));
    private static final List<String> AM_PM_HOURS_FLAG_2 = new ArrayList<>(Arrays.asList("a", "K"));
    private static final List<String> MILITARY_HOURS_FLAG_1 = new ArrayList<>(Arrays.asList(null,"H","a"));
    private static final List<String> MILITARY_HOURS_FLAG_2 = new ArrayList<>(Arrays.asList(null,"k","a"));
    private static final List<String> WEEK_YEAR_FLAG_1 = new ArrayList<>(Arrays.asList("w","Y","M","d"));

    private static final List<Integer> CONST_ARRAY_LIST = new ArrayList<>(Arrays.asList((int)Const.INVOKESPECIAL,
            (int)Const.INVOKEVIRTUAL, (int)Const.INVOKESTATIC, (int)Const.INVOKEINTERFACE));

    private static final String SIG_CONSTANT_OPERAND_1 = "Ljava/lang/String;)V";
    private static final String SIG_CONSTANT_OPERAND_2 = "Ljava/lang/String;Ljava/util/Locale;)V";
    private static final String SIG_CONSTANT_OPERAND_3 = "Ljava/lang/String;Ljava/text/DateFormatSymbols;)V";
    private static final String CLASS_CONSTANT_OPERAND = "java/text/SimpleDateFormat";
    private static final String NAME_CONSTANT_OPERAND_1 = "<init>";
    private static final String NAME_CONSTANT_OPERAND_2 = "applyPattern";
    private static final String NAME_CONSTANT_OPERAND_3 = "applyLocalizedPattern";

    private static final String BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";

    String dateFormatString;
    final BugReporter bugReporter;

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
        List<List<String>> combinations = new ArrayList<>(Arrays.asList(
                AM_PM_HOURS_FLAG_1, AM_PM_HOURS_FLAG_2, MILITARY_HOURS_FLAG_1, MILITARY_HOURS_FLAG_2, WEEK_YEAR_FLAG_1
        ));

        if (CONST_ARRAY_LIST.contains(seen) && stack.getStackDepth() > 0) {
            Object formatStr1 = stack.getStackItem(0).getConstant();
            if (formatStr1 instanceof String) {
                this.dateFormatString = (String) formatStr1;
            }

            else if (stack.getStackDepth() > 1) {
                Object formatStr2 = stack.getStackItem(1).getConstant();
                if (formatStr2 instanceof String) {
                    this.dateFormatString = (String) formatStr2;
                }
            }

            else {
                this.dateFormatString = null;
            }

            String cl = getClassConstantOperand();
            String nm = getNameConstantOperand();
            String sig = getSigConstantOperand();

            if (this.dateFormatString != null && (sig.indexOf(SIG_CONSTANT_OPERAND_1) >= 0 ||
                    sig.indexOf(SIG_CONSTANT_OPERAND_2) >= 0 || sig.indexOf(SIG_CONSTANT_OPERAND_3) >= 0 )
                    && CLASS_CONSTANT_OPERAND.equals(cl) && (NAME_CONSTANT_OPERAND_1.equals(nm)
                    || NAME_CONSTANT_OPERAND_2.equals(nm) || NAME_CONSTANT_OPERAND_3.equals(nm))) {

                    if (runCheckStringForBadCombo(dateFormatString, combinations)){
                        bugReporter.reportBug(new BugInstance(this, BUG_TYPE, NORMAL_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                                .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
                    }
            }
        }
    }

    private boolean runCheckStringForBadCombo(String stringToCheck, List<List<String>> combinationsToRun){
        boolean result = false;
        for (List<String> combination : combinationsToRun){
            List<String> requiredFlags = new ArrayList<String>(combination.subList(1,combination.size()));
            String missingFlag = combination.get(0);
            if (checkStringForBadCombo(stringToCheck, missingFlag, requiredFlags)) {
                result = true;
            }
        }
        return result;
    }

    private boolean checkStringForBadCombo(String stringToCheck, String missingFlag, List<String> requiredFlags) {
        boolean result = true;
        if (missingFlag != null && stringToCheck.contains(missingFlag)) {
            result = false;
        }
        for (String flag : requiredFlags) {
            if (!stringToCheck.contains(flag)){
                result = false;
                break;
            }
        }
        return result;
    }

}