package edu.umd.cs.findbugs.detect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class PublicAttributes
        extends OpcodeStackDetector
        implements Detector {

    private static final Set<String> CONSTRUCTOR_LIKE_NAMES = new HashSet<String>(Arrays.asList(
            Const.CONSTRUCTOR_NAME, Const.STATIC_INITIALIZER_NAME,
            "clone", "init", "initialize", "dispose", "finalize", "this",
            "_jspInit", "jspDestroy"));

    private static final List<String> SETTER_LIKE_NAMES = Arrays.asList(
            "set", "put", "add", "insert", "delete", "remove", "erase", "clear",
            "push", "pop", "enqueue", "dequeue", "write", "append");

    private final BugReporter bugReporter;

    private Set<XField> writtenFields = new HashSet<XField>();

    public PublicAttributes(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    // Check for each statements which write an own attribute of the class
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

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_PRIMITIVE_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field));
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

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_ARRAY_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field));
            writtenFields.add(field);
        } else if (seen == Const.INVOKEINTERFACE ||
                seen == Const.INVOKEVIRTUAL) {
            XMethod xmo = getXMethodOperand();
            if (xmo == null) {
                return;
            }

            // Heuristics: suppose that that model has a proper name in English
            // which roughly describes its behavior, beginning with the verb.
            // If the verb does not hint that the method writes the object
            // then we skip it.
            if (!looksLikeASetter(xmo.getName())) {
                System.err.println("Not a setter: " + xmo.getName());
                return;
            }

            XField field = stack.getStackItem(xmo.getNumParams()).getXField();
            // Do not report the same public field twice.
            if (field == null || writtenFields.contains(field)) {
                return;
            }

            // Only consider attributes of self.
            if (!field.getClassDescriptor().equals(getClassDescriptor())) {
                return;
            }

            if (!field.isPublic() || !field.isReferenceType()) {
                return;
            }

            if (!MutableStaticFields.mutableSignature(field.getSignature())) {
                System.err.println("Immutable: " + field);
                return;
            }

            bugReporter.reportBug(new BugInstance(this,
                    "PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE",
                    NORMAL_PRIORITY)
                            .addClass(this).addField(field));
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
