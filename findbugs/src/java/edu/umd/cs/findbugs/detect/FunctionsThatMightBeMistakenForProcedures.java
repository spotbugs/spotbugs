/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.HashSet;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

public class FunctionsThatMightBeMistakenForProcedures extends OpcodeStackDetector implements NonReportingDetector {

    final BugReporter bugReporter;

   
    public FunctionsThatMightBeMistakenForProcedures(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        setVisitMethodsInCallOrder(true);
    }

    
    public void visit(JavaClass obj) {
        
        
    }
    @Override
    public void visitAfter(JavaClass obj) {
        mReturnSelf.clear();
        mReturnOther.clear();
        methodsSeen.clear();
        
    }
    
    
    HashSet<XMethod> mReturnSelf = new HashSet<XMethod>();
    HashSet<XMethod> methodsSeen = new HashSet<XMethod>();
    
    HashSet<XMethod> mReturnOther = new HashSet<XMethod>();
    
    int returnSelf, returnOther, returnNew, returnUnknown;
    int updates;
    @Override
    public void visit(Code code) {
         

        methodsSeen.add(getXMethod());
        if (getMethod().isStatic())
            return;
        String signature = getMethodSig();
        SignatureParser parser = new SignatureParser(signature);
       
        String returnType = parser.getReturnTypeSignature();
        @SlashedClassName
        String r = ClassName.fromFieldSignature(returnType);
        if (r == null || !r.equals(getClassName()))
            return;
//        System.out.println("Checking " + getFullyQualifiedMethodName());
        boolean funky = false;
        for (int i = 0; i < parser.getNumParameters(); i++) {
            String p = ClassName.fromFieldSignature(parser.getParameter(i));
            if (getClassName().equals(p)) {
                funky = true;
            }
              
        }
        //
        XMethod m = getXMethod();
        String sourceSig = m.getSourceSignature();
        if (sourceSig != null) {
            GenericSignatureParser sig = new GenericSignatureParser(sourceSig);
            String genericReturnValue = sig.getReturnTypeSignature();
            Type t = GenericUtilities.getType(genericReturnValue);
            if (t instanceof GenericObjectType) {
                funky = true;
            }

            if (false) {
                XClass c = getXClass();
                String classSourceSig = c.getSourceSignature();
                if (!genericReturnValue.equals(classSourceSig))
                    return;
            }
        }

//        System.out.println(getFullyQualifiedMethodName());
        returnSelf = returnOther = updates = returnNew = returnUnknown = 0;
//        System.out.println(" investingating");
        
        super.visit(code); // make callbacks to sawOpcode for all opcodes
//        System.out.printf("  %3d %3d %3d %3d%n", returnSelf, updates, returnOther, returnNew);
        
        if (returnSelf > 0 && returnOther == 0) {
            mReturnSelf.add(getXMethod());
        } else if (funky && returnOther > returnNew) {
            mReturnSelf.add(getXMethod());
        } else if (returnOther > 0 && returnOther >= returnSelf) {
        
            int priority = HIGH_PRIORITY;
            if (returnSelf > 0 || updates > 0)
                priority++;
            if (returnUnknown > 0)
                priority++;
            if (returnNew > 0 && priority > NORMAL_PRIORITY)
                priority = NORMAL_PRIORITY;
            if (updates > 0)
                priority = LOW_PRIORITY;
            if (priority <= HIGH_PRIORITY)
                mReturnOther.add(getXMethod());
            if (priority <= NORMAL_PRIORITY) {
//                System.out.printf("  adding %d %s%n",priority, MethodAnnotation.fromVisitedMethod(this).getSourceLines());
                               
                XFactory xFactory = AnalysisContext.currentXFactory();
                xFactory.addFunctionThatMightBeMistakenForProcedures(getMethodDescriptor());
            }

            if (true)
                bugReporter.reportBug(new BugInstance("TESTING", priority).addClassAndMethod(this).addString(
                        String.format("%3d %3d %5d %3d", returnOther, returnSelf, returnNew, updates)));

        }

    }

    @Override
    public void sawOpcode(int seen) {
        
        if (seen == ARETURN) {
            OpcodeStack.Item rv = stack.getStackItem(0);
            if (rv.isNull()) return;
            if (rv.getRegisterNumber() == 0 && rv.isInitialParameter())
                returnSelf++;
            else {
                XMethod xMethod = rv.getReturnValueOf();
                if (mReturnSelf.contains(xMethod))
                    returnSelf++;
                else if (xMethod != null && !xMethod.isAbstract() 
                        && xMethod.getClassDescriptor().equals(getClassDescriptor())
                        && (getPrevOpcode(1) != INVOKESPECIAL 
                                || xMethod.getName().equals("<init>") 
                                    && !xMethod.getSignature().equals("()V"))) {
                    if (!methodsSeen.contains(xMethod)) {
//                        System.out.println("Not seen: " + xMethod);
                        return;
                    }
                   
                    returnOther++;
//                    System.out.println("  calls " + xMethod);
//                    System.out.println("  at " + MethodAnnotation.fromXMethod(xMethod).getSourceLines());
                    if (xMethod.getName().equals("<init>")
                            || mReturnOther.contains(xMethod))
                        returnNew++;
                } else {
                    returnUnknown++;
                }
            }
        } else if (seen == PUTFIELD) {
            OpcodeStack.Item rv = stack.getStackItem(1);
            if (rv.getRegisterNumber() == 0 && rv.isInitialParameter())
                updates++;
        }
    }


}
