/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, Mike Fagan <mfagan@tde.com>
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

import java.io.PrintStream;
import java.util.BitSet;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugReporterObserver;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class FindBugsSummaryStats extends PreorderVisitor implements Detector, BugReporterObserver, NonReportingDetector {
    private final ProjectStats stats;

    BitSet lines = new BitSet(500);

    int methods = 0;

    int fields = 0;

    int classCodeSize;

    int totalNCSS = 0;

    int totalCodeSize = 0;

    int totalMethods = 0;

    int totalFields = 0;

    boolean sawLineNumbers;

    @Override
    public void visitJavaClass(JavaClass obj) {
        if (AnalysisContext.currentAnalysisContext().isApplicationClass(obj)) {
            super.visitJavaClass(obj);
        }
    }

    @Override
    public void visit(JavaClass obj) {

        lines.clear();
        methods = 0;
        fields = 0;
        classCodeSize = 0;
        sawLineNumbers = false;
    }

    @Override
    public void visit(Method obj) {
        methods++;
    }

    @Override
    public void visit(Field obj) {
        fields++;
    }

    @Override
    public void visit(Code obj) {
        classCodeSize += obj.getCode().length;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        int linesNCSS = 1 + methods + fields;
        if (sawLineNumbers) {
            linesNCSS += lines.cardinality();
        } else {
            linesNCSS += classCodeSize / 10;
        }
        if (stats != null) {
            stats.addClass(getDottedClassName(), obj.getSourceFileName(), obj.isInterface(), linesNCSS);
        }
        totalCodeSize += classCodeSize;
        totalNCSS += linesNCSS;
        totalMethods += methods;
        totalFields += fields;

    }

    @Override
    public void visit(LineNumber obj) {
        sawLineNumbers = true;
        int line = obj.getLineNumber();
        lines.set(line);
    }

    public FindBugsSummaryStats(BugReporter bugReporter) {
        this.stats = bugReporter.getProjectStats();
        bugReporter.addObserver(this);
    }

    public FindBugsSummaryStats() {
        this.stats = null;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

    @Override
    public void report() {
    }

    @Override
    public void report(PrintStream out) {
        out.println("NCSS\t" + totalNCSS);
        out.println("codeSz\t" + totalCodeSize);
        out.println("methods\t" + totalMethods);
        out.println("fields\t" + totalFields);
    }

    @Override
    public void reportBug(BugInstance bug) {
        // already added when bug was added to bug collection
    }

}
