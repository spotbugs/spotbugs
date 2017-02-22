/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Detector to find private methods that are never called.
 */
public class ReflectiveClasses extends BytecodeScanningDetector implements NonReportingDetector {

    public ReflectiveClasses(BugReporter bugReporter) {
        AnalysisContext.currentXFactory().addReflectiveClasses(DescriptorFactory.createClassDescriptor(java.lang.System.class));
    }

    String constantString;

    @Override
    public void sawString(String s) {
        constantString = s;
    }

    @Override
    public void sawClass() {
        int opcode = getOpcode();
        if ((opcode == LDC) || (opcode == LDC_W)) {
            process(getClassConstantOperand());
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == INVOKESTATIC) {
            // System.out.println(getClassConstantOperand()+ "." +
            // getNameConstantOperand());
            if (constantString != null && "java/lang/Class".equals(getClassConstantOperand())
                    && "forName".equals(getNameConstantOperand())) {
                process(ClassName.toSlashedClassName(constantString));
            }

        }
        constantString = null;
    }

    private void process(@SlashedClassName String className) {
        ClassDescriptor d = DescriptorFactory.createClassDescriptor(className);
        AnalysisContext.currentXFactory().addReflectiveClasses(d);
    }
}

