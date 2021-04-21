package edu.umd.cs.findbugs.detect;

import java.util.Set;
import java.util.HashSet;

import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class PublicAttributes
        extends OpcodeStackDetector
        implements Detector {

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

            Field clsField = getClassField(field, getClassName());
            Type type = clsField.getType();
            // Instances of an immutable classes cannot be modified, thus
            // the method is not a modifier, despite its name.
            if (type instanceof ObjectType &&
                    isImmutable(((ObjectType) type).getClassName())) {
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
        return Const.CONSTRUCTOR_NAME.equals(methodName) ||
                Const.STATIC_INITIALIZER_NAME.equals(methodName) ||
                "clone".equals(methodName) ||
                "init".equals(methodName) ||
                "initialize".equals(methodName) ||
                "dispose".equals(methodName) ||
                "finalize".equals(methodName) ||
                "this".equals(methodName) ||
                "_jspInit".equals(methodName) ||
                "_jspDestroy".equals(methodName);
    }

    private static @CheckReturnValue Field getClassField(XField field, String dottedClassName) {
        try {
            JavaClass cls = Repository.lookupClass(dottedClassName);
            for (Field clsField : cls.getFields()) {
                if (field.getName().equals(clsField.getName())) {
                    return clsField;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            AnalysisContext.reportMissingClass(cnfe);
        }
        return null;
    }

    private static boolean isImmutable(String dottedClassName) {
        if ("java.lang.String".equals(dottedClassName) ||
                "java.lang.Character".equals(dottedClassName) ||
                "java.lang.Byte".equals(dottedClassName) ||
                "java.lang.Short".equals(dottedClassName) ||
                "java.lang.Integer".equals(dottedClassName) ||
                "java.lang.Long".equals(dottedClassName) ||
                "java.lang.Float".equals(dottedClassName) ||
                "java.lang.Double".equals(dottedClassName) ||
                "java.lang.Boolean".equals(dottedClassName)) {
            return true;
        }

        try {
            JavaClass cls = Repository.lookupClass(dottedClassName);

            return isImmutable(cls);
        } catch (ClassNotFoundException cnfe) {
            AnalysisContext.reportMissingClass(cnfe);
        }
        return false;
    }

    private static boolean isImmutable(JavaClass cls) {
        if (!cls.isFinal()) {
            return false;
        }

        for (Field field : cls.getFields()) {
            if (!field.isStatic() &&
                    (!field.isFinal() || !field.isPrivate())) {
                return false;
            }
        }

        try {
            JavaClass sup = cls.getSuperClass();
            return isImmutable(sup);
        } catch (ClassNotFoundException cnfe) {
            return true;
        }
    }

    private static boolean looksLikeASetter(String methodName) {
        return methodName.startsWith("set") ||
                methodName.startsWith("put") ||
                methodName.startsWith("add") ||
                methodName.startsWith("insert") ||
                methodName.startsWith("delete") ||
                methodName.startsWith("remove") ||
                methodName.startsWith("erase") ||
                methodName.startsWith("clear") ||
                methodName.startsWith("push") ||
                methodName.startsWith("pop") ||
                methodName.startsWith("enqueue") ||
                methodName.startsWith("dequeue") ||
                methodName.startsWith("write") ||
                methodName.startsWith("append");
    }
}
