/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.BytecodeScanner;
import edu.umd.cs.findbugs.ba.MethodBytecodeSet;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * @author David Hovemeyer
 */
public class MethodBytecodeSetFactory extends AnalysisFactory<MethodBytecodeSet> {

    /**
     * @param analysisName
     * @param analysisClass
     */
    public MethodBytecodeSetFactory(String analysisName, Class<MethodBytecodeSet> analysisClass) {
        super(analysisName, analysisClass);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public MethodBytecodeSet analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);
        Code code = method.getCode();
        if (code == null) {
            return null;
        }

        byte[] instructionList = code.getCode();

        // Create callback
        UnpackedBytecodeCallback callback = new UnpackedBytecodeCallback(instructionList.length);

        // Scan the method.
        BytecodeScanner scanner = new BytecodeScanner();
        scanner.scan(instructionList, callback);

        UnpackedCode unpackedCode = callback.getUnpackedCode();
        MethodBytecodeSet result = null;
        if (unpackedCode != null) {
            result = unpackedCode.getBytecodeSet();
        }

        return result;
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
        analysisCache.registerMethodAnalysisEngine(MethodBytecodeSet.class, this);
    }

}
