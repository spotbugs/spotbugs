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

public class DateFormatStringChecker extends OpcodeStackDetector {

    final BugReporter bugReporter;

    public DateFormatStringChecker(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }


    String dateFormatString;


    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
        if ((seen == Const.INVOKESPECIAL || seen == Const.INVOKEVIRTUAL || seen == Const.INVOKESTATIC || seen == Const.INVOKEINTERFACE) && (stack
                .getStackDepth() > 0)) {
            Object formatStr = stack.getStackItem(0).getConstant();
            if (formatStr instanceof String) {
                this.dateFormatString = (String) formatStr;
                String cl = getClassConstantOperand();
                String nm = getNameConstantOperand();
                String sig = getSigConstantOperand();

                if (sig.indexOf("Ljava/lang/String;)V") >= 0
                        && ("java/text/SimpleDateFormat".equals(cl) && ("<init>".equals(nm) || "applyPattern".equals(nm)))) {

                    if (((dateFormatString.indexOf('h') >= 0) && (dateFormatString.indexOf('m') >= 0) && (dateFormatString.indexOf('a') < 0)) ||
                            ((dateFormatString.indexOf('K') >= 0) && (dateFormatString.indexOf('m') >= 0) && (dateFormatString.indexOf('a') < 0)) ||
                            ((dateFormatString.indexOf('Y') >= 0) && (dateFormatString.indexOf('M') >= 0) && (dateFormatString.indexOf('d') >= 0)
                                    && (dateFormatString.indexOf('w') < 0))) {
                        bugReporter.reportBug(new BugInstance(this, "FS_BAD_DATE_FORMAT_FLAG_COMBO", NORMAL_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(this).addString(dateFormatString)
                                .describe(StringAnnotation.FORMAT_STRING_ROLE).addSourceLine(this));
                    }
                }
            }
        }
    }

}
