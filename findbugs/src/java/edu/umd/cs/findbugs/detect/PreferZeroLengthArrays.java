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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;

public class PreferZeroLengthArrays extends BytecodeScanningDetector implements StatelessDetector {
    boolean nullOnTOS = false;

    private final BugReporter bugReporter;

    public PreferZeroLengthArrays(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    Collection<SourceLineAnnotation> found = new LinkedList<SourceLineAnnotation>();

    @Override
    public void visit(Code obj) {
        found.clear();
        // Solution to sourceforge bug 1765925; returning null is the
        // convention used by java.io.File.listFiles()
        if ("listFiles".equals(getMethodName())) {
            return;
        }
        String returnType = getMethodSig().substring(getMethodSig().indexOf(')') + 1);
        if (returnType.startsWith("[")) {
            nullOnTOS = false;
            super.visit(obj);
            if (!found.isEmpty()) {
                BugInstance bug = new BugInstance(this, "PZLA_PREFER_ZERO_LENGTH_ARRAYS", LOW_PRIORITY).addClassAndMethod(this);
                for (SourceLineAnnotation s : found) {
                    bug.add(s);
                }
                bugReporter.reportBug(bug);
                found.clear();
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {

        switch (seen) {
        case ACONST_NULL:
            nullOnTOS = true;
            return;
        case ARETURN:
            if (nullOnTOS) {
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                        getPC());
                if (sourceLineAnnotation != null) {
                    found.add(sourceLineAnnotation);
                }
            }
            break;
        default:
            break;
        }
        nullOnTOS = false;
    }
}
