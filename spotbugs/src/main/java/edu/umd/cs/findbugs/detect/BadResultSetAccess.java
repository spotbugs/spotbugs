/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.Collections;
import java.util.Set;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class BadResultSetAccess extends OpcodeStackDetector {

    @StaticConstant
    private static final Set<String> dbFieldTypesSet = Set.of("Array", "AsciiStream", "BigDecimal", "BinaryStream", "Blob", "Boolean", "Byte",
            "Bytes", "CharacterStream", "Clob", "Date", "Double", "Float", "Int", "Long", "Object", "Ref", "RowId", "Short", "String", "Time",
            "Timestamp", "UnicodeStream", "URL");

    private final BugReporter bugReporter;

    public BadResultSetAccess(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if (hasInterestingClass(classContext.getJavaClass().getConstantPool(), Collections.singleton("java/sql/ResultSet"))) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == Const.INVOKEINTERFACE) {
            String methodName = getNameConstantOperand();
            String clsConstant = getClassConstantOperand();
            if (("java/sql/ResultSet".equals(clsConstant) && ((methodName.startsWith("get") && dbFieldTypesSet
                    .contains(methodName.substring(3))) || (methodName.startsWith("update") && dbFieldTypesSet
                            .contains(methodName.substring(6)))))
                    || (("java/sql/PreparedStatement".equals(clsConstant) && ((methodName.startsWith("set") && dbFieldTypesSet
                            .contains(methodName.substring(3))))))) {
                String signature = getSigConstantOperand();
                int numParms = PreorderVisitor.getNumberArguments(signature);
                if (stack.getStackDepth() >= numParms) {
                    OpcodeStack.Item item = stack.getStackItem(numParms - 1);

                    if ("I".equals(item.getSignature()) && item.couldBeZero()) {
                        bugReporter.reportBug(new BugInstance(this,
                                "java/sql/PreparedStatement".equals(clsConstant) ? "SQL_BAD_PREPARED_STATEMENT_ACCESS"
                                        : "SQL_BAD_RESULTSET_ACCESS", item.mustBeZero() ? HIGH_PRIORITY : NORMAL_PRIORITY)
                                .addClassAndMethod(this).addSourceLine(this));
                    }
                }
            }
        }

    }
}
