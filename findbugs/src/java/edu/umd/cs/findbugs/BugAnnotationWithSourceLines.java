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

/**
 * @author pugh
 */
public abstract class BugAnnotationWithSourceLines implements BugAnnotation {

    protected String sourceFileName;

    protected SourceLineAnnotation sourceLines;

    /**
     *
     */
    public BugAnnotationWithSourceLines() {
        super();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Get the source file name.
     */
    public final String getSourceFileName() {
        return sourceFileName;
    }

    /**
     * Set a SourceLineAnnotation describing the source lines where the package
     * element is defined.
     */
    public void setSourceLines(SourceLineAnnotation sourceLines) {
        this.sourceLines = sourceLines;
        sourceFileName = sourceLines.getSourceFile();
    }

    /**
     * Get the SourceLineAnnotation describing the source lines where the method
     * is defined.
     *
     * @return the SourceLineAnnotation, or null if there is no source
     *         information for this package element
     */
    public SourceLineAnnotation getSourceLines() {
        return sourceLines;
    }

    @Override
    public String toString(ClassAnnotation primaryClass) {
        return toString();
    }

}
