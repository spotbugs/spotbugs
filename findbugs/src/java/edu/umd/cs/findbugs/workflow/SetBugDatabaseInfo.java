/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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
package edu.umd.cs.findbugs.workflow;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SloppyBugComparator;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.VersionInsensitiveBugComparator;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class SetBugDatabaseInfo {

	/**
	 * 
	 */
	private static final String USAGE = "Usage: <cmd> " 
			+ " [options] [<oldData> [<newData>]]";

	
	static class SetInfoCommandLine extends CommandLine {
		String revisionName;

		long revisionTimestamp = 0L;
		public  List<String> sourcePaths = new LinkedList<String>();

		SetInfoCommandLine() {
			addOption("-name", "name", "set name for (last) revision");
			addOption("-timestamp", "when", "set timestamp for (last) revision");
			addOption("-source", "directory", "Add this directory to the source search path");
		}

		@Override
		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			throw new IllegalArgumentException("no option " + option);

		}

		@Override
		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			if (option.equals("-name"))
				revisionName = argument;
			else if (option.equals("-timestamp"))
				revisionTimestamp = Date.parse(argument);
			else if (option.equals("-source"))
				revisionTimestamp = Date.parse(argument);
			else if (option.equals("-source"))
					sourcePaths.add(argument);
			else
				throw new IllegalArgumentException("Can't handle option "
						+ option);

		}

	}

	public static void main(String[] args) throws IOException,
			DocumentException {

		DetectorFactoryCollection.instance();
		SetInfoCommandLine commandLine = new SetInfoCommandLine();
		int argCount = commandLine.parse(args, 0, 2, USAGE);

		Project project = new Project();
		BugCollection origCollection;
		origCollection = new SortedBugCollection(
				SortedBugCollection.MultiversionBugInstanceComparator.instance);

		if (argCount < args.length) 
			origCollection.readXML(args[argCount++], project);
		else
			origCollection.readXML(System.in, project);
		

		if (commandLine.revisionName != null)
			origCollection.setReleaseName(commandLine.revisionName);
		if (commandLine.revisionTimestamp != 0)
			origCollection.setTimestamp(commandLine.revisionTimestamp);
		for(String source : commandLine.sourcePaths)
			project.addSourceDir(source);
		
		if (argCount < args.length) 
			origCollection.writeXML(args[argCount++], project);
		else
			origCollection.writeXML(System.out, project);

	}



}
