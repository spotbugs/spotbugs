/*
 * FindBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs;

/**
 * Exception indicating that a TimestampInterval is invalid.
 * This can occur when decoding one or more TimestampIntervals
 * from a String, or when defining new intervals.
 * 
 * @see edu.umd.cs.findbugs.TimestampInterval
 * @see edu.umd.cs.findbugs.TimestampIntervalCollection
 * @author David Hovemeyer
 */
public class InvalidTimestampIntervalException extends Exception {
	private static final long serialVersionUID = 3977296603520381746L;

	/**
	 * Constructor.
	 * 
	 * @param msg message describing reason for exception
	 */
	public InvalidTimestampIntervalException(String msg) {
		super(msg);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param msg   message describing reason for exception
	 * @param cause reason for exception (generally a RuntimeException)
	 */
	public InvalidTimestampIntervalException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
