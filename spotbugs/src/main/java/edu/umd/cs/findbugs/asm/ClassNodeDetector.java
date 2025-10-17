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

import javax.annotation.CheckForNull;

import org.objectweb.asm.tree.ClassNode;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.ba.XClass;
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
public abstract class ClassNodeDetector extends ClassNode implements Detector2 {

    protected final BugReporter bugReporter;

    protected XClass xclass;

    /**
     * Construct a ClassNodeDetector. The bugReporter is passed to the
     * constructor and stored in a protected final field.
     *
     * @param bugReporter
     *            the BugReporter that bug should be reporter to.
     */
    protected ClassNodeDetector(BugReporter bugReporter) {
        super(FindBugsASM.ASM_VERSION);
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        xclass = getClassInfo(classDescriptor);
        if (xclass != null) {
            FBClassReader cr = Global.getAnalysisCache().getClassAnalysis(FBClassReader.class, classDescriptor);
            this.interfaces = new ArrayList<>();
            this.innerClasses = new ArrayList<>();
            this.fields = new ArrayList<>();
            this.methods = new ArrayList<>();
            cr.accept(this, 0);
        }
    }

    @CheckForNull
    protected XClass getClassInfo(ClassDescriptor classDescr) {
        if (classDescr == null) {
            return null;
        }
        try {
            return Global.getAnalysisCache().getClassAnalysis(XClass.class, classDescr);
        } catch (CheckedAnalysisException e) {
            bugReporter.reportMissingClass(classDescr, e);
            return null;
        }
    }

    @Override
    public void finishPass() {
        // do nothing
    }
}
