/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
 * Process exit codes returned when the FindBugs text UI is
 * invoked with the -exitcode command line argument.
 */
public interface ExitCodes {
	/**
	 * Exit status code to indicate that serious analysis errors
	 * occurred.
	 */
	public static final int ERROR_EXIT_CODE = 3;

	/**
	 * Exit status code to indicate that classes needed for analysis
	 * were missing.  This is a minor error, so the bug reports may
	 * generally be trusted, although they might not be as accurate as
	 * possible.
	 */
	public static final int MISSING_CLASS_EXIT_CODE = 2;

	/**
	 * Exit status code to indicate that the analysis completed normally,
	 * but bugs were reported.
	 */
	public static final int BUGS_FOUND_EXIT_CODE = 1;

	/**
	 * Exit status code to indicate that the analysis completed normally,
	 * and no bugs were found in the analyzed code.
	 */
	public static final int NO_BUGS_FOUND_EXIT_CODE = 0;
}

// vim:ts=4
