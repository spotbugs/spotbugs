/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005-2008 University of Maryland
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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class BadSyntaxForRegularExpression extends OpcodeStackDetector {

    BugReporter bugReporter;

    public BadSyntaxForRegularExpression(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    private void singleDotPatternWouldBeSilly(int stackDepth, boolean ignorePasswordMasking) {
        if (ignorePasswordMasking && stackDepth != 1) {
            throw new IllegalArgumentException("Password masking requires stack depth 1, but is " + stackDepth);
        }
        if (stack.getStackDepth() < stackDepth) {
            return;
        }
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        Object value = it.getConstant();
        if (value == null || !(value instanceof String)) {
            return;
        }
        String regex = (String) value;
        boolean dotIsUsed = ".".equals(regex);
        if (!dotIsUsed && !"|".equals(regex)) {
            return;
        }
        int priority = HIGH_PRIORITY;
        if (ignorePasswordMasking && dotIsUsed) {
            priority = NORMAL_PRIORITY;
            OpcodeStack.Item top = stack.getStackItem(0);
            Object topValue = top.getConstant();
            if (topValue instanceof String) {
                String replacementString = (String) topValue;
                if ("x".equals(replacementString.toLowerCase()) || "-".equals(replacementString) || "*".equals(replacementString)
                        || " ".equals(replacementString) || "\\*".equals(replacementString)) {
                    return;
                }
                if (replacementString.length() == 1 && getMethodName().toLowerCase().indexOf("pass") >= 0) {
                    priority = LOW_PRIORITY;
                }
            }
        }

        bugReporter.reportBug(new BugInstance(this, "RE_POSSIBLE_UNINTENDED_PATTERN", priority).addClassAndMethod(this)
                .addCalledMethod(this).addSourceLine(this));
    }

    private void sawRegExPattern(int stackDepth) {
        sawRegExPattern(stackDepth, 0);
    }

    private void sawRegExPattern(int stackDepth, int flags) {
        if (stack.getStackDepth() < stackDepth) {
            return;
        }
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        if (it.getSpecialKind() == OpcodeStack.Item.FILE_SEPARATOR_STRING && (flags & Pattern.LITERAL) == 0) {
            bugReporter.reportBug(new BugInstance(this, "RE_CANT_USE_FILE_SEPARATOR_AS_REGULAR_EXPRESSION", HIGH_PRIORITY)
            .addClassAndMethod(this).addCalledMethod(this).addSourceLine(this));
            return;
        }
        Object value = it.getConstant();
        if (value == null || !(value instanceof String)) {
            return;
        }
        String regex = (String) value;
        try {
            Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            String message = e.getMessage();
            int eol = message.indexOf('\n');
            if (eol > 0) {
                message = message.substring(0, eol);
            }
            BugInstance bug = new BugInstance(this, "RE_BAD_SYNTAX_FOR_REGULAR_EXPRESSION", HIGH_PRIORITY)
            .addClassAndMethod(this).addCalledMethod(this).addString(message).describe(StringAnnotation.ERROR_MSG_ROLE)
            .addString(regex).describe(StringAnnotation.REGEX_ROLE);
            String options = getOptions(flags);
            if (options.length() > 0) {
                bug.addString("Regex flags: " + options).describe(StringAnnotation.STRING_MESSAGE);
            }
            bug.addSourceLine(this);
            bugReporter.reportBug(bug);
        }
    }

    /** return an int on the stack, or 'defaultValue' if can't determine */
    private int getIntValue(int stackDepth, int defaultValue) {
        if (stack.getStackDepth() < stackDepth) {
            return defaultValue;
        }
        OpcodeStack.Item it = stack.getStackItem(stackDepth);
        Object value = it.getConstant();
        if (value == null || !(value instanceof Integer)) {
            return defaultValue;
        }
        return ((Number) value).intValue();
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKESTATIC && "java/util/regex/Pattern".equals(getClassConstantOperand())
                && "compile".equals(getNameConstantOperand()) && getSigConstantOperand().startsWith("(Ljava/lang/String;I)")) {
            sawRegExPattern(1, getIntValue(0, 0));
        } else if (seen == INVOKESTATIC && "java/util/regex/Pattern".equals(getClassConstantOperand())
                && "compile".equals(getNameConstantOperand()) && getSigConstantOperand().startsWith("(Ljava/lang/String;)")) {
            sawRegExPattern(0);
        } else if (seen == INVOKESTATIC && "java/util/regex/Pattern".equals(getClassConstantOperand())
                && "matches".equals(getNameConstantOperand())) {
            sawRegExPattern(1);
        } else if (seen == INVOKEVIRTUAL && "java/lang/String".equals(getClassConstantOperand())
                && "replaceAll".equals(getNameConstantOperand())) {
            sawRegExPattern(1);
            singleDotPatternWouldBeSilly(1, true);
        } else if (seen == INVOKEVIRTUAL && "java/lang/String".equals(getClassConstantOperand())
                && "replaceFirst".equals(getNameConstantOperand())) {
            sawRegExPattern(1);
            singleDotPatternWouldBeSilly(1, false);
        } else if (seen == INVOKEVIRTUAL && "java/lang/String".equals(getClassConstantOperand())
                && "matches".equals(getNameConstantOperand())) {
            sawRegExPattern(0);
            singleDotPatternWouldBeSilly(0, false);
        } else if (seen == INVOKEVIRTUAL && "java/lang/String".equals(getClassConstantOperand())
                && "split".equals(getNameConstantOperand())) {
            sawRegExPattern(0);
            singleDotPatternWouldBeSilly(0, false);
        }

    }

    static void appendOption(StringBuilder b, int flags, int mask, String name) {
        if ((flags & mask) == 0) {
            return;
        }
        if (b.length() > 0) {
            b.append(" | ");
        }
        b.append("Pattern." + name);

    }

    static String getOptions(int flags) {

        StringBuilder b = new StringBuilder();
        appendOption(b, flags, Pattern.CANON_EQ, "CANON_EQ");
        appendOption(b, flags, Pattern.CASE_INSENSITIVE, "CASE_INSENSITIVE");
        appendOption(b, flags, Pattern.COMMENTS, "COMMENTS");
        appendOption(b, flags, Pattern.DOTALL, "DOTALL");
        appendOption(b, flags, Pattern.LITERAL, "LITERAL");
        appendOption(b, flags, Pattern.MULTILINE, "MULTILINE");
        appendOption(b, flags, Pattern.UNICODE_CASE, "UNICODE_CASE");
        appendOption(b, flags, Pattern.UNIX_LINES, "UNIX_LINES");

        return b.toString();
    }
}
