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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * DebugRepositoryLookupFailureCallback implementation for debugging.
 * (Test drivers, etc.)  It just prints a message and exits.
 * 
 * @author David Hovemeyer
 */
public class DebugRepositoryLookupFailureCallback implements
		RepositoryLookupFailureCallback {

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback#reportMissingClass(java.lang.ClassNotFoundException)
	 */
	@SuppressWarnings("DM_EXIT")
	public void reportMissingClass(ClassNotFoundException ex) {
		System.out.println("Missing class");
		ex.printStackTrace();
		System.exit(1);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	@SuppressWarnings("DM_EXIT")
	public void reportMissingClass(ClassDescriptor classDescriptor) {
		System.out.println("Missing class: " + classDescriptor);
		System.exit(1);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback#logError(java.lang.String)
	 */
	@SuppressWarnings("DM_EXIT")
	public void logError(String message) {
		System.err.println("Error: " + message);
		System.exit(1);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback#logError(java.lang.String, java.lang.Throwable)
	 */
	@SuppressWarnings("DM_EXIT")
	public void logError(String message, Throwable e) {
		System.err.println("Error: " + message);
		e.printStackTrace();
		System.exit(1);
	}

	/**
	 * Report that we skipped some analysis of a method
	 * @param method
	 */
	public void reportSkippedAnalysis(MethodDescriptor method) {
		System.err.println("Skipping " + method);
	}
}
