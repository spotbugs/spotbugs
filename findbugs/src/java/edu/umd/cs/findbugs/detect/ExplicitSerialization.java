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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

public class ExplicitSerialization extends OpcodeStackDetector implements NonReportingDetector {

    final static XMethod writeObject = XFactory.createXMethod("java.io.ObjectOutputStream", "writeObject", "(Ljava/lang/Object;)V", false);

    final static XMethod readObject = XFactory.createXMethod("java.io.ObjectInputStream", "readObject", "()Ljava/lang/Object;", false);

    final static ClassDescriptor ObjectOutputStream = DescriptorFactory.createClassDescriptor(ObjectOutputStream.class);
    final static ClassDescriptor ObjectInputStream = DescriptorFactory.createClassDescriptor(ObjectInputStream.class);

    final UnreadFieldsData unreadFields;

    final BugReporter bugReporter;

    public ExplicitSerialization(BugReporter bugReporter) {
        AnalysisContext context = AnalysisContext.currentAnalysisContext();
        unreadFields = context.getUnreadFieldsData();
        this.bugReporter = bugReporter;
    }

    @Override
    public boolean shouldVisit(JavaClass obj) {
        XClass xClass = getXClass();
        return xClass.getCalledClassDescriptors().contains(ObjectOutputStream)
                || xClass.getCalledClassDescriptors().contains(ObjectInputStream);
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKEVIRTUAL && writeObject.equals(getXMethodOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            String signature = top.getSignature();
            while (signature.charAt(0) == '[') {
                signature = signature.substring(1);
            }
            ClassDescriptor c = DescriptorFactory.createClassDescriptorFromFieldSignature(signature);
            if (c == null || !Subtypes2.instanceOf(c, Serializable.class)) {
                return;
            }

            try {
                XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                if (xClass.isInterface()) {
                    return;
                }
                if (xClass.isSynthetic()) {
                    return;
                }
                if (xClass.isAbstract()) {
                    return;
                }
                unreadFields.strongEvidenceForIntendedSerialization(c);
            } catch (CheckedAnalysisException e) {
                bugReporter.logError("Error looking up xClass of " + c, e);
            }

        }
        if (seen == CHECKCAST) {
            OpcodeStack.Item top = stack.getStackItem(0);
            if (readObject.equals(top.getReturnValueOf())) {
                ClassDescriptor c = getClassDescriptorOperand();
                if (!Subtypes2.instanceOf(c, Serializable.class)) {
                    return;
                }

                try {
                    XClass xClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, c);
                    if (xClass.isInterface()) {
                        return;
                    }
                    if (xClass.isSynthetic()) {
                        return;
                    }
                    if (xClass.isAbstract()) {
                        return;
                    }
                    unreadFields.strongEvidenceForIntendedSerialization(c);
                } catch (CheckedAnalysisException e) {
                    bugReporter.logError("Error looking up xClass of " + c, e);
                }
            }
        }

    }
}
