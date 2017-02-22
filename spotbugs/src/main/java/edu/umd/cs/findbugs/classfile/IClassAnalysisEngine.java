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

package edu.umd.cs.findbugs.classfile;

/**
 * Engine for performing an analysis on classes.
 *
 * @author David Hovemeyer
 */
public interface IClassAnalysisEngine<ResultType> extends IAnalysisEngine<ClassDescriptor, ResultType> {

    /**
     * Return true if analysis results produced by this analysis engine can be
     * recomputed. Unless some correctness criterion prevents analysis results
     * from being recomputed, analysis engines should return true (allowing the
     * cache to be kept to a manageable size).
     *
     * @return true if analysis results produced by this engine can be
     *         recomputed, false if for some reason the analysis results must be
     *         retained indefinitely
     */
    public boolean canRecompute();
}
