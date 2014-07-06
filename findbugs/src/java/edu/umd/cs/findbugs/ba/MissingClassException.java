/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * DataflowAnalysisException variant to report a class lookup failure that
 * caused dataflow analysis to abort.
 *
 * @author David Hovemeyer
 */
public class MissingClassException extends DataflowAnalysisException {
    private static final long serialVersionUID = 1L;

    private @DottedClassName
    final
    String className;

    private MissingClassException(String className) {
        super("Missing class" + (className != null ? ": " + className : ""));
        this.className = className;
    }

    /**
     * Constructor.
     *
     * @param exception
     *            the ClassNotFoundException that caused this exception
     */
    public MissingClassException(ClassNotFoundException exception) {
        this(ClassNotFoundExceptionParser.getMissingClassName(exception));
        initCause(exception);
    }

    /**
     * Get the name of the missing class.
     *
     * @return name of the missing class, or null if the missing class name is
     *         unknown
     */
    public String getClassName() {
        return className;
    }

    public ClassDescriptor getClassDescriptor() {
        return DescriptorFactory.createClassDescriptorFromDottedClassName(className);
    }

    /**
     * Get the ClassNotFoundException that caused this exception.
     *
     * @return the ClassNotFoundException that caused this exception
     */
    public ClassNotFoundException getClassNotFoundException() {
        return (ClassNotFoundException) getCause();
    }
}
