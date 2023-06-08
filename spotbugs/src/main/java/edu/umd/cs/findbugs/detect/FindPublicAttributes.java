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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.MutableClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

public class FindPublicAttributes extends OpcodeStackDetector {

    private static final Set<String> CONSTRUCTOR_LIKE_NAMES = new HashSet<String>(Arrays.asList(
            Const.CONSTRUCTOR_NAME, Const.STATIC_INITIALIZER_NAME,
            "clone", "init", "initialize", "dispose", "finalize", "this",
            "_jspInit", "jspDestroy"));

    private static final List<String> SETTER_LIKE_NAMES = Arrays.asList(
            "set", "put", "add", "insert", "delete", "remove", "erase", "clear",
            "push", "pop", "enqueue", "dequeue", "write", "append");

    private final BugReporter bugReporter;

    private final Set<XField> writtenFields = new HashSet<XField>();

    private final Map<XField, SourceLineAnnotation> fieldDefLineMap = new HashMap<XField, SourceLineAnnotation>();

    public FindPublicAttributes(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawField() {
        XField field = getXFieldOperand();
        if (field != null && !fieldDefLineMap.containsKey(field)) {
            SourceLineAnnotation sla = SourceLineAnnotation.fromVisitedInstruction(this);
            fieldDefLineMap.put(field, sla);
        }
    }

    @Override
    public void visit(JavaClass obj) {
        for (Method m : obj.getMethods()) {
            String methodName = m.getName();
            // First visit the Constructor and the static initializer to collect the field initialization lines
            if (Const.CONSTRUCTOR_NAME.equals(methodName) || Const.STATIC_INITIALIZER_NAME.equals(methodName)) {
                doVisitMethod(m);
            }
        }
    }

    // Check for each statement which writes an own attribute of the class
    // instance. If the attribute written is public then report a bug.
    @Override
    public void sawOpcode(int seen) {
        // It is normal that classes used as simple data types have a
        // constructor to make initialization easy.
        if (isConstructorLikeMethod(getMethodName())) {
            return;
        }

        // We do not care for enums and nested classes.
        if (getThisClass().isEnum() || getClassName().indexOf('$') >= 0) {
            return;
        }

        if (seen == Const.PUTFIELD || seen == Const.PUTSTATIC) {
            XField field = getXFieldOperand();
            // Do not report the same public field twice.
            if (field == null || writtenFields.contains(field)) {
                return;
            }

            // Only consider attributes of self.
            if (!field.getClassDescriptor().equals(getClassDescriptor())) {
                return;
            }

            if (!field.isPublic()) {
                return;
            }

            SourceLineAnnotation sla = fieldDefLineMap.containsKey(field)
                    ? fieldDefLineMap.get(field)
                    : SourceLineAnnotation.fromVisitedInstruction(this);

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field).addSourceLine(sla));
            writtenFields.add(field);
        } else if (seen == Const.AASTORE) {
            XField field = stack.getStackItem(2).getXField();
            // Do not report the same public field twice.
            if (field == null || writtenFields.contains(field)) {
                return;
            }

            // Only consider attributes of self.
            if (!field.getClassDescriptor().equals(getClassDescriptor())) {
                return;
            }

            if (!field.isPublic()) {
                return;
            }

            SourceLineAnnotation sla = fieldDefLineMap.containsKey(field)
                    ? fieldDefLineMap.get(field)
                    : SourceLineAnnotation.fromVisitedInstruction(this);

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_ARRAY_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field).addSourceLine(sla));
            writtenFields.add(field);
        } else if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            XMethod xmo = getXMethodOperand();
            if (xmo == null) {
                return;
            }

            // Heuristics: suppose that that model has a proper name in English
            // which roughly describes its behavior, beginning with the verb.
            // If the verb does not hint that the method writes the object
            // then we skip it.
            if (!looksLikeASetter(xmo.getName())) {
                return;
            }

            XField field = stack.getStackItem(xmo.getNumParams()).getXField();
            // Do not report the same public field twice.
            if (field == null || writtenFields.contains(field)) {
                return;
            }

            // Mutable static fields are already detected by MS
            if (field.isStatic()) {
                return;
            }

            // Only consider attributes of self.
            if (!field.getClassDescriptor().equals(getClassDescriptor())) {
                return;
            }

            if (!field.isPublic() || !field.isReferenceType()) {
                return;
            }

            if (!MutableClasses.mutableSignature(field.getSignature())) {
                return;
            }

            SourceLineAnnotation sla = fieldDefLineMap.containsKey(field)
                    ? fieldDefLineMap.get(field)
                    : SourceLineAnnotation.fromVisitedInstruction(this);

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field).addSourceLine(sla));
            writtenFields.add(field);
        }
    }

    private static boolean isConstructorLikeMethod(String methodName) {
        return CONSTRUCTOR_LIKE_NAMES.contains(methodName);
    }

    private static boolean looksLikeASetter(String methodName) {
        for (String name : SETTER_LIKE_NAMES) {
            if (methodName.startsWith(name)) {
                return true;
            }
        }
        return false;
    }
}
