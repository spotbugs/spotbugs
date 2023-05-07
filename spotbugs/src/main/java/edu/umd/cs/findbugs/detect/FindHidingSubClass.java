package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.Arrays;

public class FindHidingSubClass extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private JavaClass directSuperClass;

    public FindHidingSubClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        JavaClass subClass;

        //This is the current class. Named it subClass to better depict the idea of sub and super class.
        subClass = this.getClassContext().getJavaClass();

        try {
            directSuperClass = subClass.getSuperClass();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }


        Method[] superMethods = directSuperClass.getMethods();
        Method[] methods = subClass.getMethods();
        for (Method method : methods) {
            if (!isMainMethod(method)) {
                if (!method.isPrivate() && method.isStatic()) {
                    int index = Arrays.asList(superMethods).indexOf(method);
                    Method superClassMethod;
                    if (index != -1) {
                        superClassMethod = superMethods[index];
                    } else {
                        return;
                    }
                    if (isHiding(superClassMethod, method)) {
                        bugReporter.reportBug(new BugInstance(this, "HSBC_FIND_HIDING_SUB_CLASS", NORMAL_PRIORITY)
                                .addClassAndMethod(this.getClassContext().getJavaClass(), method)
                                .addString(subClass.getClassName())
                                .addString(superClassMethod.getName())
                                .addString(directSuperClass.getClassName()));
                        /*System.out.println("Sub Class Method: "
                                + method.getReturnType() + " " + method.getName() + "(" + printTypeArray(method.getArgumentTypes()) + ")"
                                + "    ->    "
                                + "Super Class Method: "
                                + superClassMethod.getReturnType() + " " + superClassMethod.getName() + "(" + printTypeArray(superClassMethod.getArgumentTypes()) + ")");*/
                    }
                }
            }
        }
    }

    private String printTypeArray(Type[] types) {
        String s = "";
        for (Type t : types) {
            s += t + ",";
        }
        return s;
    }

    private boolean isMainMethod(Method method) {
        return method.isPublic() && method.isStatic() && method.getReturnType().equals(BasicType.VOID)
                && method.getName().equals("main");
        //&& method.getArgumentTypes()[0].toString().equals("String[]");
    }

    private boolean isOverridable(Method original, Method overrider) {
        return original.getName().equals(overrider.getName())
                && Arrays.equals(original.getArgumentTypes(), overrider.getArgumentTypes())
                && !original.isFinal();
    }

    private boolean isHiding(Method original, Method overrider) {
        return isOverridable(original, overrider) && original.getReturnType().equals(overrider.getReturnType());
    }
}
