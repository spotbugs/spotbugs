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

import java.awt.PageAttributes.OriginType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.SourceFinder;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.filter.Filter;

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
		String exclusionFilterFile;
	 boolean withMessages = false;

		long revisionTimestamp = 0L;
		public  List<String> sourcePaths = new LinkedList<String>();
		public  List<String> searchSourcePaths = new LinkedList<String>();

		SetInfoCommandLine() {
			addOption("-name", "name", "set name for (last) revision");
			addOption("-timestamp", "when", "set timestamp for (last) revision");
			addSwitch("-resetSource", "remove all source search paths");
			addOption("-source", "directory", "Add this directory to the source search path");
			addOption("-findSource", "directory", "Find and add all relevant source directions contained within this directory");
			addOption("-suppressWarnings", "exclusion filter file", "Update the project to suppress all of the warnings contained in the suppression file (replaces previous suppressions)");
			addSwitch("-withMessages", "Add bug descriptions");
		}

		@Override
		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			if (option.equals("-withMessages"))
				withMessages = true;
			else if (option.equals("-resetSource"))
				sourcePaths.clear();
		else
		   throw new IllegalArgumentException("no option " + option);

		}

		@Override
		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			if (option.equals("-name"))
				revisionName = argument;
			else if (option.equals("-suppressWarnings"))
				exclusionFilterFile = argument;
			else if (option.equals("-timestamp"))
				revisionTimestamp = Date.parse(argument);

			else if (option.equals("-source"))
				sourcePaths.add(argument);
			else if (option.equals("-findSource"))
				searchSourcePaths.add(argument);
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
		origCollection = new SortedBugCollection();

		if (argCount < args.length) 
			origCollection.readXML(args[argCount++], project);
		else
			origCollection.readXML(System.in, project);


		if (commandLine.revisionName != null)
			origCollection.setReleaseName(commandLine.revisionName);
		if (commandLine.revisionTimestamp != 0)
			origCollection.setTimestamp(commandLine.revisionTimestamp);
		origCollection.setWithMessages(commandLine.withMessages);

		if (commandLine.exclusionFilterFile != null) {
			project.setSuppressionFilter(Filter.parseFilter(commandLine.exclusionFilterFile));
		}
		for(String source : commandLine.sourcePaths)
			project.addSourceDir(source);

		Map<String,Set<String>> missingFiles = new HashMap<String,Set<String>>();
		if (!commandLine.searchSourcePaths.isEmpty()) {
			sourceSearcher = new SourceSearcher(project);
			for(BugInstance bug : origCollection.getCollection()) {
				SourceLineAnnotation src = bug.getPrimarySourceLineAnnotation();
				if (!sourceSearcher.sourceNotFound.contains(src.getClassName())
						&& !sourceSearcher.findSource(src)) {
					Set<String> paths = missingFiles.get(src.getSourceFile());
					if (paths == null) {
						paths = new HashSet<String>();
						missingFiles.put(src.getSourceFile(), paths);
					}
					String fullPath = fullPath(src);
					// System.out.println("Missing " + fullPath);
					paths.add(fullPath);
				}
			}
			Set<String> foundPaths = new HashSet<String>();
			for(String f : commandLine.searchSourcePaths) 
				for(File javaFile :  RecursiveSearchForJavaFiles.search(new File(f))) {
					Set<String> matchingMissingClasses = missingFiles.get(javaFile.getName());
					if (matchingMissingClasses == null) {
						// System.out.println("Nothing for " + javaFile);
					} else for(String sourcePath : matchingMissingClasses) {
						String path = javaFile.getAbsolutePath();
						if (path.endsWith(sourcePath)) {
							String dir = path.substring(0,path.length() - sourcePath.length());
							if (foundPaths.add(dir)) {
								project.addSourceDir(dir);
								if (argCount < args.length) 
									System.out.println("Found " + dir);
							}
						}
					}


				}


				}

			// OK, now we know all the missing source files
			// we also know all the .java files in the directories we were pointed to





		if (argCount < args.length) 
			origCollection.writeXML(args[argCount++], project);
		else
			origCollection.writeXML(System.out, project);

	}
	static String fullPath(SourceLineAnnotation src) {
		return src.getPackageName().replace('.', File.separatorChar)
		+ File.separatorChar + src.getSourceFile();
	}
	static SourceSearcher sourceSearcher;


}
