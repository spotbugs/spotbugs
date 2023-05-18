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

import edu.umd.cs.findbugs.BugInstance;
import org.apache.bcel.Const;
import org.apache.bcel.generic.BasicType;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import java.util.Arrays;

/**
 * This detector finds all the methods of a subclass which are hiding the static methods of the superclass and
 * tries to invoke that method using the instance variable.
 * Please see @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/MET07-J.+Never+declare+a+class+method+that+hides+a+method+declared+in+a+superclass+or+superinterface">SEI CERT MET07-J</a>
 * @author Nazir, Muhammad Zafar Iqbal
 */
public class FindHidingSubClass extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    public FindHidingSubClass(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        //I first try to filter out the part of code where there a call to overridden method.
        if (seen == Const.INVOKEVIRTUAL) {
            //Then I get the subclass and superclass in variables.
            //This is the current class. Named it subClass to better depict the idea of sub and super class.
            JavaClass subClass = this.getClassContext().getJavaClass();
            if (subClass.getClass().getEnclosingClass() != null) {
                return;
            }
            JavaClass directSuperClass = null;
            try {
                directSuperClass = subClass.getSuperClass();
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
            if (directSuperClass == null) {
                return;
            }
            //I store all the methods of both subclass and superclass (in separate variables)
            Method[] superMethods = directSuperClass.getMethods();
            Method[] methods = subClass.getMethods();
            //I go through each method of the subclass and filter the non-private and static methods, as private methods can't be overridden.
            //I also perform check for main method using auxiliary private method
            for (Method method : methods) {
                if (!isMainMethod(method)) {
                    if (!method.isPrivate() && method.isStatic()) {
                        //I check either the method of subclass is present in the super class.
                        // If it is, I store it in a variable for further usage.
                        int index = Arrays.asList(superMethods).indexOf(method);
                        Method superClassMethod;
                        if (index != -1) {
                            superClassMethod = superMethods[index];
                        } else {
                            continue;
                        }
                        //Here I check for the exception cases of main method and inner class using two auxiliary private methods.
                        if (isConstructor(method) || isHidingInnerClass(method)) {
                            continue;
                        }
                        //I check using an auxiliary private method, either the subclass method is hiding the superclass method.
                        //If yes, then I report the bug.
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
     *This method checks for the inner class exceptional cases.
     * As whenever there an inner class, '.access$' methods are created hiddenly to access the outer class attributes.
     */
    private boolean isHidingInnerClass(Method method) {
        return method.getName().contains("access$");
    }

    /**
     * This method checks for the exceptional case of main method.
     * As we know main method always have the signature "public static void main(String[] args)".
     * It is static but usually public class have its own main method as its entry point.
     * Therefore, it is not a error caused by Programming but a utility to provide UI to user.
     */
    private boolean isConstructor(Method method) {
        return method.getName().contains("clinit") || method.getName().contains("init");
    }

    /**
     * This method checks either the received method is entry point for class or not.
     * It does by checking public static void main(String[] args) signature element by element.
     */
    private boolean isMainMethod(Method method) {
        return method.isPublic() && method.isStatic() && method.getReturnType().equals(BasicType.VOID)
                && method.getName().equals("main")
                && this.isStringArray(method);
    }

    private boolean isStringArray(Method method) {
        return method.getArgumentTypes().length == 1 &&
                method.getArgumentTypes()[0].toString().contains("String") &&
                method.getArgumentTypes()[0].toString().contains("[");
    }

    /**
     * This method checks either the method 'overrider' in subClass is actually overriding the method 'original' in superclass
     * It first evaluates that does the signature of 'overrider' is sub-signature of original or not.
     * It then checks the name, signature and 'original' being non-final constraints of overriding.
     */
    private boolean isOverridable(Method original, Method overrider) {
        return original.getName().equals(overrider.getName())
                && Arrays.equals(original.getArgumentTypes(), overrider.getArgumentTypes())
                && !original.isFinal();
    }

    /**
     * This method checks the hiding constraints.
     */
    private boolean isHiding(Method original, Method overrider) {
        return isOverridable(original, overrider) && original.getReturnType().equals(overrider.getReturnType());
    }
}
