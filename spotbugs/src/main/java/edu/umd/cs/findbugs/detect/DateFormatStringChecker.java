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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class DateFormatStringChecker extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    /**
     * Contains special flags that can trigger check (triggers); property of the rule
     * (isRequired - whether flag should be contained or be absent); flags to be checked
     * (checkItems) and a bypassing flag (ignoreFlag - whether a rule can be skipped if
     * such flag is included)
     */
    private static final class Rule {
        private final boolean isRequired;
        private final String ignoreFlag;
        private final List<String> checkItems;
        private final List<String> triggers;

        Rule(List<String> checkItems, String ignoreFlag, boolean isRequired, List<String> triggers) {
            this.checkItems = checkItems;
            this.ignoreFlag = ignoreFlag;
            this.isRequired = isRequired;
            this.triggers = triggers;
        }

        /**
         *  dateFormat is being checked for existence of any element from listOfFlags
         */
        boolean containsAny(String dateFormat, List<String> listOfFlags) {
            return listOfFlags != null && listOfFlags.stream().anyMatch(dateFormat::contains);
        }

        /**
         *  dateFormat is being checked for existence of any keywords (triggering flags)
         *  to start the check (if ignoreFlag flag was found - further checking is skipped);
         *  if isRequired property is:
         *      - true:  dateFormat is checked for existence of flags (this.flags)
         *      - false: dateFormat is checked for absence of flags (this.flags)
         */
        boolean verify(String dateFormat) {
            if ((this.ignoreFlag != null && dateFormat.contains(this.ignoreFlag))
                    || !containsAny(dateFormat, this.triggers)) {
                return false;
            }

            boolean cond = containsAny(dateFormat, this.checkItems);
            if (this.isRequired) {
                return !cond;
            }
            return cond;
        }
    }

    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (!(seen == Const.INVOKESPECIAL || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESTATIC
                || seen == Const.INVOKEINTERFACE) || stack.getStackDepth() == 0) {
            return;
        }

        String cl = getClassConstantOperand();
        String nm = getNameConstantOperand();
        String si = getSigConstantOperand();

        int idx = -1;
        /*  idx represents the position of the string in the stack,
            the value is
                0 in cases when it has only 1 parameter provided;
                1 in cases when there are multiple parameters given;
               -1 when no method found, or has different number of parameters
            within the specified functions (given in switch and if) */
        if (("java/text/SimpleDateFormat".equals(cl) && "(Ljava/lang/String;)V".equals(si)
                && (Const.CONSTRUCTOR_NAME.equals(nm) || "applyPattern".equals(nm) || "applyLocalizedPattern".equals(nm)))
                || ("java/time/format/DateTimeFormatter".equals(cl) && "ofPattern".equals(nm)
                        && "(Ljava/lang/String;)Ljava/time/format/DateTimeFormatter;".equals(si))
                || ("org/apache/commons/lang3/time/FastDateFormat".equals(cl) && "getInstance".equals(nm)
                        && "(Ljava/lang/String;)Lorg/apache/commons/lang3/time/FastDateFormat;".equals(si))) {
            idx = 0;
        } else if (("java/text/SimpleDateFormat".equals(cl) && Const.CONSTRUCTOR_NAME.equals(nm)
                && ("(Ljava/lang/String;Ljava/util/Locale;)V".equals(si)
                        || "(Ljava/lang/String;Ljava/text/DateFormatSymbols;)V".equals(si)))
                || ("java/time/format/DateTimeFormatter".equals(cl) && "ofPattern".equals(nm)
                        && "(Ljava/lang/String;Ljava/util/Locale;)Ljava/time/format/DateTimeFormatter;".equals(si))
                || ("org/apache/commons/lang3/time/FastDateFormat".equals(cl) && "getInstance".equals(nm)
                        && ("(Ljava/lang/String;Ljava/util/Locale;)Lorg/apache/commons/lang3/time/FastDateFormat;".equals(si)
                                || "(Ljava/lang/String;Ljava/util/TimeZone;)Lorg/apache/commons/lang3/time/FastDateFormat;".equals(si)))) {
            idx = 1;
        }
        if (idx == -1) {
            return;
        }

        String dateFormatString = (String) stack.getStackItem(idx).getConstant();
        if (dateFormatString != null && runDateFormatRuleVerify(dateFormatString)) {
            bugReporter.reportBug(new BugInstance(this, "FS_BAD_DATE_FORMAT_FLAG_COMBO", NORMAL_PRIORITY)
                    .addClassAndMethod(this)
                    .addCalledMethod(this)
                    .addString(dateFormatString).describe(StringAnnotation.FORMAT_STRING_ROLE)
                    .addSourceLine(this));
        }
    }

    /**
     * Runs the check per each bad combination (Rule objects) on the provided string.
     *
     * @param dateFormat - string to be checked
     * @return {@code true} if given string matches any bad combination.
     */
    private boolean runDateFormatRuleVerify(String dateFormat) {
        return Stream.of(
                // when "h" or "K" flags found, make sure that it ALSO CONTAINS "a" or "B"
                new Rule(Arrays.asList("a", "B"), null, true, Arrays.asList("h", "K")),
                // 5:00 pm (5:00 in the evening)

                // when "H" or "k" flags found, make sure that it DOES NOT CONTAIN "a" or "B"
                new Rule(Arrays.asList("a", "B"), null, false, Arrays.asList("H", "k")),
                // 17:00 (am/pm)

                // when "M" or "d" flags found, make sure that it DOES NOT CONTAIN "Y" (unless "w" is provided)
                new Rule(Collections.singletonList("Y"), "w", false, Arrays.asList("M", "d")),

                // milli-of-day cannot be used together with Hours, Minutes, Seconds
                new Rule(Arrays.asList("H", "h", "K", "k", "m", "s"), null, false, Arrays.asList("A", "N")),

                // milli-of-day and nano-of-day cannot be used together
                new Rule(Collections.singletonList("A"), null, false, Collections.singletonList("N")),
                new Rule(Collections.singletonList("N"), null, false, Collections.singletonList("A")),

                // fraction-of-second and nano-of-second cannot be used together
                new Rule(Collections.singletonList("S"), null, false, Collections.singletonList("n")),
                new Rule(Collections.singletonList("n"), null, false, Collections.singletonList("S")),

                // am-pm marker and period-of-day cannot be used together
                new Rule(Collections.singletonList("a"), null, false, Collections.singletonList("B")),
                new Rule(Collections.singletonList("B"), null, false, Collections.singletonList("a")),

                // year and year-of-era cannot be used together
                new Rule(Collections.singletonList("u"), null, false, Collections.singletonList("y")),
                new Rule(Collections.singletonList("y"), null, false, Collections.singletonList("u"))).anyMatch(rule -> rule.verify(dateFormat));
    }
}
