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
 * Common superclass for all checked exceptions that can be thrown while
 * performing some kind of analysis.
 *
 * @author David Hovemeyer
 */
public class CheckedAnalysisException extends Exception {
    /**
     * Constructor.
     */
    public CheckedAnalysisException() {
    }

    public CheckedAnalysisException(CheckedAnalysisException e) {
        super(e.getMessage(), e.getCause());
    }

    /**
     * Constructor.
     *
     * @param msg
     *            message
     */
    public CheckedAnalysisException(String msg) {
        super(msg);
    }

    /**
     * Constructor.
     *
     * @param msg
     *            message
     * @param cause
     *            root cause of this exception
     */
    public CheckedAnalysisException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
