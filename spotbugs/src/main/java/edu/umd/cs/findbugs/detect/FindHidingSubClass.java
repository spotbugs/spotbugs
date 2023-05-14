package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugInstance;
import org.apache.bcel.Const;
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

    public FindHidingSubClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            //This is the current class. Named it subClass to better depict the idea of sub and super class.
            JavaClass subClass = this.getClassContext().getJavaClass();
            JavaClass directSuperClass = null;
            try {
                directSuperClass = subClass.getSuperClass();
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
            if (directSuperClass == null) {
                return;
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
                            continue;
                        }
                        if (isHiding(superClassMethod, method)) {
                            bugReporter.reportBug(new BugInstance(this, "HSBC_FIND_HIDING_SUB_CLASS", NORMAL_PRIORITY)
                                    .addClass(subClass.getClassName())
                                    .addMethod(subClass, method)
                                    .addString(subClass.getClassName())
                                    .addString(superClassMethod.getName())
                                    .addString(directSuperClass.getClassName()));
                        }
                    }
                }
            }
        }
    }

    /**
     * This method checks either the received method is entry point for class or not.
     * It does by checking public static void main(String[] args) signature element by element.
     */
    private boolean isMainMethod(Method method) {
        return method.isPublic() && method.isStatic() && method.getReturnType().equals(BasicType.VOID)
                && method.getName().equals("main")
                && method.getArgumentTypes().length == 1 &&
                method.getArgumentTypes()[0].toString().equals("String[]");
    }

    /**
     * This method checks either the method 'overrider' in subClass is actually overriding the method 'original' in superclass
     * It first evaluates that does the signature of 'overrider' is sub-signature of original or not.
     * It then checks the name, signature and 'original' being non-final constraints of overriding.
     */
    private boolean isOverridable(Method original, Method overrider) {
        boolean isSignaturSubset = true;
        Type[] overriderArguments = original.getArgumentTypes();
        Type[] originalArguments = original.getArgumentTypes();

        for (Type t : overriderArguments) {
            if (!Arrays.asList(originalArguments).contains(t)) {
                isSignaturSubset = false;
                break;
            }
        }
        return original.getName().equals(overrider.getName()) && isSignaturSubset && !original.isFinal();
    }

    /**
     * This method checks the hiding constraints.
     */
    private boolean isHiding(Method original, Method overrider) {
        return isOverridable(original, overrider) && original.getReturnType().equals(overrider.getReturnType());
    }
}
