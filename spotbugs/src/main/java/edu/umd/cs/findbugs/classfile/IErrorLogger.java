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
 * Interface for objects that log various kinds of analysis errors.
 *
 * @author David Hovemeyer
 */
public interface IErrorLogger {
    /**
     * Called to report a class lookup failure.
     *
     * @param ex
     *            a ClassNotFoundException resulting from the class lookup
     *            failure
     */
    public void reportMissingClass(ClassNotFoundException ex);

    /**
     * Called to report a class lookup failure.
     *
     * @param classDescriptor
     *            ClassDescriptor of a missing class
     */
    public void reportMissingClass(ClassDescriptor classDescriptor);

    /**
     * Called to report a class lookup failure.
     *
     * @param classDescriptor
     *            ClassDescriptor of a missing class
     * @param exception
     *            The exception thrown when trying to get the class
     */
    public default void reportMissingClass(ClassDescriptor classDescriptor, CheckedAnalysisException exception) {
        if (exception instanceof MissingClassException) {
            reportMissingClass(classDescriptor);
        } else {
            logError("Error looking up class " + classDescriptor.getDottedClassName(), exception);
        }
    }

    /**
     * Log an error that occurs while performing analysis.
     *
     * @param message
     *            the error message
     */
    public void logError(String message);

    /**
     * Log an error that occurs while performing analysis.
     *
     * @param message
     *            the error message
     * @param e
     *            the exception which is the underlying cause of the error
     */
    public void logError(String message, Throwable e);

    /**
     * Report that we skipped some analysis of a method
     *
     * @param method
     *            the method we skipped
     */
    public void reportSkippedAnalysis(MethodDescriptor method);
}
