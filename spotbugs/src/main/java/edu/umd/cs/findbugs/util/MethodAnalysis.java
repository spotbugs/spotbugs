/*
 * SpotBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.util;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.FinallyDuplicatesInfoFactory;

/**
 * Utility class for method analysis.
 */
public final class MethodAnalysis {

    private MethodAnalysis() {
    }

    /**
     * Check if the location is duplicated in the method.
     *
     * @return {@code true} if the location is duplicated in the method, {@code false} otherwise
     * @throws CheckedAnalysisException if an error occurs during the analysis
     */
    public static boolean isDuplicatedLocation(MethodDescriptor methodDescriptor, int pc) throws CheckedAnalysisException {
        FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo methodAnalysis =
                Global.getAnalysisCache().getMethodAnalysis(FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo.class, methodDescriptor);
        return methodAnalysis.getDuplicates(pc).stream().anyMatch(duplicatePc -> duplicatePc < pc);
    }
}
