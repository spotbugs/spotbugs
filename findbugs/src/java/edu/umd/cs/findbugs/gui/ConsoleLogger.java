/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

/*
 * ConsoleLogger.java
 *
 * Created on April 3, 2003, 1:37 PM
 */

package edu.umd.cs.findbugs.gui;

import java.util.Date;

import edu.umd.cs.findbugs.L10N;

/**
 * @author David Hovemeyer
 */
public class ConsoleLogger implements Logger {


	private LogSync logSync;

	/**
	 * Creates a new instance of ConsoleLogger
	 */
	public ConsoleLogger(LogSync logSync) {
		this.logSync = logSync;
	}

	public void logMessage(int severity, String message) {
		// If this is an error, pass it to the GUI
		if (severity == ERROR)
			logSync.error(message);

		// Format a message for the console window
		Date date = new Date();
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		buf.append(date.toString());
		buf.append("] ");
		if (severity == ERROR)
			buf.append(L10N.getLocalString("msg.error_txt", "ERROR: "));
		else if (severity == WARNING)
			buf.append(L10N.getLocalString("msg.warning_txt", "WARNING: "));
		buf.append(message);
		logSync.writeToLog(buf.toString());
	}

}
