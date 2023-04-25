package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;

import java.util.HashSet;
import java.util.Set;


public class FindVulnerableSecurityCheckMethods extends OpcodeStackDetector {

    private final BugReporter bugReporter;
    private JavaClass currentClass;

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

        currentClass = this.getClassContext().getJavaClass();

        if(!currentClass.isFinal()) {
            Method[] methods = currentClass.getMethods();
            for(Method method : methods) {
                if (method == null) {
                    return;
                }
                if (!method.isFinal() && !method.isPrivate()) {
                    LocalVariableTable localVariableTable = method.getLocalVariableTable();
                    if(localVariableTable == null) {
                        continue;
                    }
                    LocalVariable[] localVariables;
                    localVariables = localVariableTable.getLocalVariableTable();
                    if(localVariables == null) {
                        continue;
                    }
                    //System.out.println("Length: " + localVariableTable.length);
                    boolean doesCheckSecurity = false;
                    LocalVariable l = null;
                    int li = 0;

                    XMethod xMethod = null;

                    while (!doesCheckSecurity && li<localVariables.length) {
                        l =  localVariables[li];
                        if(l.getSignature().contains("SecurityManager")) {
                            if(seen == Const.INVOKEVIRTUAL) {
                                xMethod = this.getXMethodOperand();
                                if (xMethod == null) {
                                    return;
                                }
                                if ("java.lang.SecurityManager".equals(xMethod.getClassName()) && badMethodNames.contains(xMethod.getName()) ) {
                                    doesCheckSecurity = true;
                                }
                            }
                        }
                        li++;
                    }

                    if(doesCheckSecurity && l!= null){
                        bugReporter.reportBug(new BugInstance(this, "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", NORMAL_PRIORITY)
                                .addClassAndMethod(currentClass, method)
                                .addString(l.getName())
                                .addString(xMethod.getName())
                        );
                    }
                }
            }
        }
    }
}

/*public class FindVulnerableSecurityCheckMethods implements Detector  {

    private final BugReporter bugReporter;
    private JavaClass currentClass;

    public FindVulnerableSecurityCheckMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }


    @Override
    public void visitClassContext(ClassContext classContext) {

        currentClass = classContext.getJavaClass();

        if(!currentClass.isFinal()) {
            Method[] methods = currentClass.getMethods();
            for(Method method : methods) {
                if (method == null) {
                    return;
                }
                if (!method.isFinal() && !method.isPrivate()) {
                    LocalVariable[] localVariableTable = method.getLocalVariableTable().getLocalVariableTable();
                    //System.out.println("Length: " + localVariableTable.length);
                    boolean doesCheckSecurity = false;
                    LocalVariable l = null;
                    int li = 0;

                    while (!doesCheckSecurity && li<localVariableTable.length) {
                        l =  localVariableTable[li];
                        if(l.getSignature().contains("SecurityManager")) {

                            doesCheckSecurity = true;
                        }
                        li++;
                    }

                    if(doesCheckSecurity && l!= null){
                        bugReporter.reportBug(new BugInstance(this, "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", NORMAL_PRIORITY)
                                .addClassAndMethod(currentClass, method)
                                .addString(l.getName()));
                    }
                }
            }
        }
    }

    @Override
    public void report() {

    }
}*/


/*public class FindVulnerableSecurityCheckMethods extends OpcodeStackDetector {
    //Do I really need this?
    //final Set<String> badPrintedString;
    final BugAccumulator bugAccumulator;

    private final BugReporter bugReporter;

    private JavaClass currentClass;
    public FindVulnerableSecurityCheckMethods(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {

        //Version3
        currentClass = this.getClassContext().getJavaClass();

        if(!currentClass.isFinal()) {
            Method method = this.getMethod();
            if (method == null) {
                return;
            }
            if (!method.isFinal() && !method.isPrivate()) {
                LocalVariable[] localVariableTable = method.getLocalVariableTable().getLocalVariableTable();
                //System.out.println("Length: " + localVariableTable.length);
                boolean doesCheckSecurity = false;
                LocalVariable l = null;
                int li = 0;

                while (!doesCheckSecurity && li<localVariableTable.length) {
                    l =  localVariableTable[li];
                    if(l.getSignature().contains("SecurityManager")) {
                        doesCheckSecurity = true;
                    }
                    li++;
                }

                if(doesCheckSecurity && l!= null){
                    bugReporter.reportBug(new BugInstance(this, "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addString(l.getName())
                            .addString(l.getSignature())
                            .addString(SourceLineAnnotation.fromVisitedInstruction(this).toString()));

                    *//*
                    BugInstance pBug = new BugInstance(this, "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addString(l.getName())
                            .addString(l.getSignature())
                            .addString(SourceLineAnnotation.fromVisitedInstruction(this).toString());
                        //.addSourceLine(this); //line number
                    System.out.printf("new bug created %s%n", pBug);

                    bugAccumulator.accumulateBug(pBug, SourceLineAnnotation.fromVisitedInstruction(this, this.getPC()));

                     *//*
                }
            }
        }

        //Version2
        *//**
 currentClass = this.getClassContext().getJavaClass();

 if(!currentClass.isFinal()) {
 Method method = this.getMethod();
 if (method == null) {
 return;
 }
 if (!method.isFinal() && !method.isPrivate()) {
 System.out.println("Process it: " + method.getName());
 LocalVariable[] localVariableTable = method.getLocalVariableTable().getLocalVariableTable();

 for(LocalVariable l : localVariableTable) {
 System.out.println(l.getName() + " : " + l.getSignature());

 if(l.getSignature().contains("SecurityManager")) {
 System.out.println("You caught me! " + l.getName() + " : " + l.getSignature());
 }
 }
 }
 }
 **//*

        //Version1
        *//**
 currentClass = this.getClassContext().getJavaClass();

 if(!currentClass.isFinal()) {
 System.out.println("Not a Final Class: " + currentClass.getClassName());

 Method method = this.getMethod();
 if (method == null) {
 return;
 }

 if(method.isFinal()){
 //Branch to check for the sake completeness of testing cases
 System.out.println("Final Method:" + method.getName());
 }
 else {
 System.out.println("Not a Final Method:" +  method.getName());
 }

 if(method.isPrivate()) {
 //Branch to check for the sake completeness of testing cases
 System.out.println("Private Method:" + method.getName());
 }
 else {
 System.out.println("Not a Private Method:" +  method.getName());
 }
 }
 else {
 //Branch to check for the sake completeness of testing cases
 System.out.println("Final Class:" + currentClass.getClassName());
 }
 **//*
    }
}*/