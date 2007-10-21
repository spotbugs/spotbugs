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

package edu.umd.cs.findbugs.anttask;

import org.apache.tools.ant.BuildException;

import sun.security.provider.certpath.Builder;

/**
 * Ant task to generate HTML or plain text from a
 * saved XML analysis results file.
 * 
 * @author David Hovemeyer
 */
public class ConvertXmlToTextTask extends AbstractFindBugsTask {

	private boolean longBugCodes;
	private String input;
	private String output;
	private String format = "html";
	
	public ConvertXmlToTextTask() {
		super("edu.umd.cs.findbugs.PrintingBugReporter");
		
		setFailOnError(true);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#checkParameters()
	 */
	@Override
	protected void checkParameters() {
		if (input == null) {
			throw new BuildException("input attribute is required", getLocation());
		}
		if (output == null) {
			throw new BuildException("output attribute is required", getLocation());
		}
		if (!format.equals("text") && !(format.equals("html") || format.startsWith("html:"))) {
			throw new BuildException(
					"invalid value " + format + " for format attribute",
					getLocation());
		}
		
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#configureFindbugsEngine()
	 */
	@Override
	protected void configureFindbugsEngine() {
		if (format.startsWith("html")) {
			addArg("-" + format);
		}
		if (longBugCodes) {
			addArg("-longBugCodes");
		}
		addArg(input);
		addArg(output);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#beforeExecuteJavaProcess()
	 */
	@Override
	protected void beforeExecuteJavaProcess() {
		log("Converting " + input + " to " + output + " using format " + format);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.anttask.AbstractFindBugsTask#afterExecuteJavaProcess(int)
	 */
	@Override
	protected void afterExecuteJavaProcess(int rc) {
		if (rc == 0) {
			log("Success");
		}
	}

}
