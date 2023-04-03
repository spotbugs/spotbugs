package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.Arrays;

public class FindHidingSubClass extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private JavaClass subClass;
    private JavaClass directSuperClass;

    public FindHidingSubClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        JavaClass[] superClasses;

        //This is the current class. Named it subClass to better depict the idea of sub and super class.
        subClass = this.getClassContext().getJavaClass();

        try {
            directSuperClass = subClass.getSuperClass();
            superClasses = subClass.getSuperClasses();
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            //return;
        }

        Method[] methods = subClass.getMethods();
        System.out.println("I am stuck here!!!");
        /*
        for (Method method : methods) {
            if (method == null) {
                return;
            }
        
        }
        */

    }

    private boolean isPackagePrivate(Method method) {
        return !(method.isPublic() || method.isProtected() || method.isPrivate());
    }

    private boolean isAccessibleFromSubClass(Method method) {
        return method.isProtected() || (subClass.getPackageName().equals(directSuperClass.getPackageName()) && isPackagePrivate(method));
    }

    private boolean isOverridable(Method original, Method overrider) {
        return original.getName().equals(overrider.getName())
                && Arrays.equals(original.getArgumentTypes(), overrider.getArgumentTypes())
                && !original.isFinal();
    }
}
