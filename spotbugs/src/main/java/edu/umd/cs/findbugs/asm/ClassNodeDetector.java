/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.asm;

import java.util.ArrayList;

import org.objectweb.asm.tree.ClassNode;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.engine.asm.FindBugsASM;

/**
 * Abstract base class to to reduce boilerplate needed for writing ASM-based
 * Detectors implemented as ClassNode visitors
 *
 * @author pugh
 */
abstract public class ClassNodeDetector extends ClassNode implements Detector2 {

    protected final BugReporter bugReporter;

    /**
     * Construct a ClassNodeDetector. The bugReporter is passed to the
     * constructor and stored in a protected final field.
     *
     * @param bugReporter
     *            the BugReporter that bug should be reporter to.
     */
    public ClassNodeDetector(BugReporter bugReporter) {
        super(FindBugsASM.ASM_VERSION);
        this.bugReporter = bugReporter;
    }

    @Override
    public String getDetectorClassName() {
        return this.getClass().getName();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {

        FBClassReader cr = Global.getAnalysisCache().getClassAnalysis(FBClassReader.class, classDescriptor);
        this.interfaces = new ArrayList<>();
        this.innerClasses = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        cr.accept(this, 0);
    }

    @Override
    public void finishPass() {
        // do nothing
    }

}
