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
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;

import java.util.HashSet;
import java.util.Set;

/***
 * This detector finds all the vulnerable methods which uses Security Manager to perform
 * some security check but are declared non-final and non-private in a non-final class.
 * Please see @see <a href="https://wiki.sei.cmu.edu/confluence/display/java/MET03-J.+Methods+that+perform+a+security+check+must+be+declared+private+or+final">SEI CERT MET03-J</a>
 * @author Nazir, Muhammad Zafar Iqbal
 */
public class FindVulnerableSecurityCheckMethods extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    private static final Set<String> badMethodNames;

    public FindVulnerableSecurityCheckMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;

    }

    static {
        badMethodNames = new HashSet<String>() {
            {
                add("checkAccept");
                add("checkAccess");
                add("checkAwtEventQueueAccess");
                add("checkConnect");
                add("checkCreateClassLoader");
                add("checkDelete");
                add("checkExec");
                add("checkExit");
                add("checkLink");
                add("checkListen");
                add("checkMemberAccess");
                add("checkMulticast");
                add("checkPackageAccess");
                add("checkPackageDefinition");
                add("checkPermission");
                add("checkPrintJobAccess");
                add("checkPropertiesAccess");
                add("checkRead");
                add("checkSecurityAccess");
                add("checkSetFactory");
                add("checkSystemClipboardAccess");
                add("checkTopLevelWindow");
                add("checkWrite");

                //Deprecated Method Call Support
                add("classDepth");
                add("classLoaderDepth");
                add("currentClassLoader");
                add("currentLoadedClass");

                add("getInCheck");
                add("inClass");
                add("inClassLoader");
            }
        };
    }

    @Override
    public void visitMethod(Method method) {
        if(!method.isFinal() && !method.isPrivate() && !method.isStatic()) {
            super.visitMethod(method);
        }
    }

    /*
    I first check the class being final. Then I filter out all the non-final and non-private methods.
    After the method is filtered, I find all the variables available in this method.
    If there is some SecurityManager variable, I verify either some security check is being performed or not.
    For this purpose I use the documentation of SecurityManager class and compile the list of method names
    which can perform really perform security check.
    I then check either any of these methods is being called over the SecurityManager variable.
    If it does so, bug is reported.
     */
    @Override
    public void sawOpcode(int seen) {
        JavaClass currentClass = this.getClassContext().getJavaClass();
        if (!currentClass.isFinal()) {
            Method[] methods = currentClass.getMethods();
            for (Method method : methods) {
                if (!method.isFinal() && !method.isPrivate()) {
                    LocalVariableTable localVariableTable = method.getLocalVariableTable();
                    if (localVariableTable == null) {
                        continue;
                    }
                    LocalVariable[] localVariables = localVariableTable.getLocalVariableTable();
                    if (localVariables == null) {
                        continue;
                    }
                    boolean doesCheckSecurity = false;
                    LocalVariable l = null;
                    int li = 0;
                    XMethod xMethod = null;
                    while (!doesCheckSecurity && li < localVariables.length) {
                        l = localVariables[li];
                        if (l.getSignature().contains("SecurityManager")) {
                            if (seen == Const.INVOKEVIRTUAL) {
                                xMethod = this.getXMethodOperand();
                                if (xMethod == null) {
                                    return;
                                }
                                if ("java.lang.SecurityManager".equals(xMethod.getClassName())
                                        && badMethodNames.contains(xMethod.getName())) {
                                    doesCheckSecurity = true;
                                }
                            }
                        }
                        li++;
                    }
                    if (doesCheckSecurity && l != null) {
                        bugReporter.reportBug(new BugInstance(this, "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", NORMAL_PRIORITY)
                                .addClass(currentClass.getClassName())
                                .addMethod(currentClass, method)
                                .addString(l.getName())
                                .addMethod(xMethod));
                    }
                }
            }
        }
    }
}
