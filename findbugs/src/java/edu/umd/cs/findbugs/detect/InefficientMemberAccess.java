/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.InnerClassAccess;
import edu.umd.cs.findbugs.ba.SignatureParser;

public class InefficientMemberAccess extends BytecodeScanningDetector implements StatelessDetector {

    public static final String ACCESS_PREFIX = "access$";

    private final BugReporter bugReporter;

    private String clsName;

    public InefficientMemberAccess(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass cls = classContext.getJavaClass();
        clsName = cls.getClassName();
        if (clsName.indexOf('$') >= 0) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if (seen == INVOKESTATIC) {
            String methodName = getNameConstantOperand();
            if (!methodName.startsWith(ACCESS_PREFIX)) {
                return;
            }

            String methodSig = getSigConstantOperand();
            Type[] argTypes = Type.getArgumentTypes(methodSig);
            if ((argTypes.length < 1) || (argTypes.length > 2)) {
                return;
            }
            String parCls = argTypes[0].getSignature();
            if (parCls.length() < 3) {
                return;
            }
            parCls = parCls.substring(1, parCls.length() - 1);
            if (!parCls.equals(getClassConstantOperand())) {
                return;
            }
            if ((argTypes.length == 2) && !argTypes[1].getSignature().equals(new SignatureParser(methodSig).getReturnTypeSignature())) {
                return;
            }

            InnerClassAccess access = null;
            try {
                String dottedClassConstantOperand = getDottedClassConstantOperand();
                access = AnalysisContext.currentAnalysisContext().getInnerClassAccessMap().getInnerClassAccess(dottedClassConstantOperand, methodName);
                if(access != null) {
                    // if the enclosing class of the field differs from the enclosing class of the method, we shouln't report
                    // because there is nothing wrong: see bug 1226
                    if (!access.getField().getClassName().equals(dottedClassConstantOperand)) {
                        return;
                    }
                    // the access method is created to access the synthetic reference to the enclosing class, we shouln't report
                    // user can't do anything here, see bug 1191
                    if(access.getField().isSynthetic()){
                        return;
                    }
                }
            } catch (ClassNotFoundException e) {
            }

            BugInstance bug = new BugInstance(this, "IMA_INEFFICIENT_MEMBER_ACCESS", LOW_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this);
            if(access != null) {
                bug.addField(access.getField());
            }
            bugReporter.reportBug(bug);
        }
    }

}
