/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * Base class for Detectors which want to extend DismantleBytecode.
 *
 * @see DismantleBytecode
 */
public class BytecodeScanningDetector extends DismantleBytecode implements Detector {
    private ClassContext classContext;

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;
        classContext.getJavaClass().accept(this);
    }

    /**
     * Get the ClassContext of the class currently being visited.
     *
     * @return the current ClassContext
     */
    public ClassContext getClassContext() {
        return classContext;
    }

    /**
     * Check see if the Code for this method should be visited.
     *
     * @param obj
     *            Code attribute
     * @return true if the Code should be visited
     */
    public boolean shouldVisitCode(Code obj) {
        return true;
    }

    @Override
    public void report() {
    }
}

