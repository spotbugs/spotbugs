/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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
 * Common base class for unchecked analysis exceptions.
 * 
 * @author David Hovemeyer
 */
public class UncheckedAnalysisException extends RuntimeException {
	/**
	 * Constructor.
	 * 
	 * @param message message describing the exception
	 */
	public UncheckedAnalysisException(String message) {
		super(message);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param message message describing the exception
	 * @param cause   another exception which is the underlying cause of this one
	 */
	public UncheckedAnalysisException(String message, Throwable cause) {
		super(message, cause);
	}
}
