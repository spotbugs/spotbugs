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

package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * An adapter allowing classes implementing the Detector interface to support
 * the new Detector2 interface.
 *
 * @author David Hovemeyer
 */
public class DetectorToDetector2Adapter implements Detector2 {
    private final Detector detector;

    /**
     * Constructor.
     *
     * @param detector
     *            the Detector we want to adapt
     */
    public DetectorToDetector2Adapter(Detector detector) {
        this.detector = detector;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector2#finishPass()
     */
    @Override
    public void finishPass() {
        detector.report();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.Detector2#visitClass(edu.umd.cs.findbugs.classfile
     * .ClassDescriptor)
     */
    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {

        // Just get the ClassContext from the analysis cache
        // and apply the detector to it.

        IAnalysisCache analysisCache = Global.getAnalysisCache();
        ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, classDescriptor);
        Profiler profiler = analysisCache.getProfiler();
        profiler.start(detector.getClass());
        try {
            detector.visitClassContext(classContext);
        } finally {
            profiler.end(detector.getClass());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector2#getDetectorClassName()
     */
    @Override
    public String getDetectorClassName() {
        return detector.getClass().getName();
    }
}
