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

package edu.umd.cs.findbugs;

import java.io.IOException;

import edu.umd.cs.findbugs.classfile.IClassPath;

/**
 * Signals that FindBugs found no classes on the classpath it was invoked to
 * analyze.
 * <p>
 * To be consistent with FindBugs 1.3 this exception is an {@link IOException}
 * and replicates the message used in that release (because I suspect some tools
 * looked for that text pattern to come out at the console).
 *
 * @author Tim Halloran
 */
public class NoClassesFoundToAnalyzeException extends IOException {

    private final IClassPath f_classPath;

    /**
     * Gets the classpath this exception is about.
     *
     * @return Gets the non-null classpath this exception is about.
     */
    public IClassPath getClassPath() {
        return f_classPath;
    }

    /**
     * Constructs an {@code NoClassesFoundToAnalyze} on the passed classpath.
     *
     * @param classPath
     *            the classpath used
     */
    public NoClassesFoundToAnalyzeException(final IClassPath classPath) {
        super("No classes found to analyze in " + classPath);
        if (classPath == null) {
            throw new IllegalArgumentException("classpath must be non-null");
        }
        f_classPath = classPath;
    }
}
