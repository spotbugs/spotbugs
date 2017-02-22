/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.ClassNameMismatchException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;

/**
 * Analysis engine to produce the ClassInfo for a loaded class. We parse just
 * enough information from the classfile to get the needed information.
 *
 * @author David Hovemeyer
 */
public class ClassNameAndSuperclassInfoAnalysisEngine implements IClassAnalysisEngine<ClassNameAndSuperclassInfo> {

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public ClassNameAndSuperclassInfo analyze(IAnalysisCache analysisCache, ClassDescriptor descriptor)
            throws CheckedAnalysisException {
        // Get InputStream reading from class data
        ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);
        DataInputStream classDataIn = new DataInputStream(new ByteArrayInputStream(classData.getData()));

        // Read the class info
        ClassParserInterface parser = new ClassParser(classDataIn, descriptor, classData.getCodeBaseEntry());
        ClassNameAndSuperclassInfo.Builder classInfoBuilder = new ClassNameAndSuperclassInfo.Builder();
        parser.parse(classInfoBuilder);
        ClassNameAndSuperclassInfo classInfo = classInfoBuilder.build();

        if (!classInfo.getClassDescriptor().equals(descriptor)) {
            throw new ClassNameMismatchException(descriptor, classInfo.getClassDescriptor(), classData.getCodeBaseEntry());
        }
        return classInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs
     * .findbugs.classfile.IAnalysisCache)
     */
    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerClassAnalysisEngine(ClassNameAndSuperclassInfo.class, this);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#canRecompute()
     */
    @Override
    public boolean canRecompute() {
        // ClassInfo objects serve as XClass objects,
        // which we want interned. So, they are never purged from the cache.
        return false;
    }
}
