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

public class DateFormatStringChecker extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    /**
     * Contains special flags that can trigger check (keywords); property of the rule
     * (isRequired - whether flag should contain or be absent); flags to be checked
     * and a bypassing flag (whether a rule can be skipped if such flag is included)
     */
    private static final class Rule {
        boolean isRequired;
        String ignore;
        List<String> flags;
        List<String> keywords;

        Rule(List<String> flags, String ignore, boolean isRequired, List<String> keywords) {
            this.flags = flags;
            this.ignore = ignore;
            this.isRequired = isRequired;
            this.keywords = keywords;
        }

        boolean containsAny(String target, List<String> list) {
            if (list == null) {
                return false;
            }
            for (String item : list) {
                if (target.contains(item)) {
                    return true;
                }
            }
            return false;
        }

        boolean verify(String dateFormat) {
            if (!containsAny(dateFormat, this.keywords)
                    || (this.ignore != null && dateFormat.contains(this.ignore))) {
                return false;
            }

            boolean cond = containsAny(dateFormat, this.flags);
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

        int idx = -1;
        String cl = getClassConstantOperand();
        String nm = getNameConstantOperand();
        String si = getSigConstantOperand();
        if ("java/text/SimpleDateFormat".equals(cl)
                && contains(nm, Const.CONSTRUCTOR_NAME, "applyPattern", "applyLocalizedPattern")) {
            switch (si) {
            case "(Ljava/lang/String;)V":
                idx = 0;
                break;
            case "(Ljava/lang/String;Ljava/util/Locale;)V":
            case "(Ljava/lang/String;Ljava/text/DateFormatSymbols;)V":
                idx = 1;
                break;
            }
        } else if ("java/time/format/DateTimeFormatter".equals(cl) && "ofPattern".equals(nm)) {
            switch (si) {
            case "(Ljava/lang/String;)Ljava/time/format/DateTimeFormatter;":
                idx = 0;
                break;
            case "(Ljava/lang/String;Ljava/util/Locale;)Ljava/time/format/DateTimeFormatter;":
                idx = 1;
                break;
            }
        } else if ("org/apache/commons/lang3/time/FastDateFormat".equals(cl) && "getInstance".equals(nm)) {
            switch (si) {
            case "(Ljava/lang/String;)Lorg/apache/commons/lang3/time/FastDateFormat;":
                idx = 0;
                break;
            case "(Ljava/lang/String;Ljava/util/Locale;)Lorg/apache/commons/lang3/time/FastDateFormat;":
            case "(Ljava/lang/String;Ljava/util/TimeZone;)Lorg/apache/commons/lang3/time/FastDateFormat;":
                idx = 1;
                break;
            }
        }

        if (idx == -1) {
            return;
        }

        String dateFormatString = (String) stack.getStackItem(idx).getConstant();
        if (dateFormatString != null && runDateFormatRuleVerify(dateFormatString)) {
            bugReporter.reportBug(new BugInstance(this, "FS_BAD_DATE_FORMAT_FLAG_COMBO", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                    .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
        }
    }

    private boolean contains(String item, String... items) {
        return Arrays.asList(items).contains(item);
    }

    /**
     * Runs the check per each bad combination (Rule objects) on the provided string.
     *
     * @param dateFormat - string to be checked
     * @return {@code true} if given string matches any bad combination.
     */
    private boolean runDateFormatRuleVerify(String dateFormat) {
        for (Rule rule : new Rule[] {
            // when "h" or "K" flags found, make sure that it ALSO CONTAINS "a" or "B"
            new Rule(Arrays.asList("a", "B"), null, true, Arrays.asList("h", "K")),

            // when "H" or "k" flags found, make sure that it DOES NOT CONTAIN "a" or "B"
            new Rule(Arrays.asList("a", "B"), null, false, Arrays.asList("H", "k")),

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
            new Rule(Collections.singletonList("y"), null, false, Collections.singletonList("u"))
        }) {
            if (rule.verify(dateFormat)) {
                return true;
            }
        }
        return false;
    }
}