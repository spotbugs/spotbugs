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

public class FindVulnerableSecurityCheckMethods extends OpcodeStackDetector {
    private final BugReporter bugReporter;
    final Set<String> badMethodNames;

    public FindVulnerableSecurityCheckMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.badMethodNames = new HashSet<String>() {
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
    public void sawOpcode(int seen) {
        JavaClass currentClass;
        currentClass = this.getClassContext().getJavaClass();
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
                                .addClassAndMethod(currentClass, method)
                                .addString(l.getName())
                                .addString(xMethod.getName()));
                    }
                }
            }
        }
    }
}
