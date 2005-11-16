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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
import edu.umd.cs.findbugs.workflow.MineBugHistory.Version;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class ListBugDatabaseInfo {

	static boolean formatDates = false;

	private static final String USAGE = "Usage: " + ListBugDatabaseInfo.class.getName()
			+ " [options] data1File data2File data3File ... ";

	static class ListBugDatabaseInfoCommandLine extends CommandLine {

		ListBugDatabaseInfoCommandLine() {
			addSwitch("-formatDates", "render dates in textual form");
		}

		public void handleOption(String option, String optionalExtraPart) {
			if (option.equals("-formatDates"))
				formatDates = true;
			else
				throw new IllegalArgumentException("unknown option: " + option);
		}

		public void handleOptionWithArgument(String option, String argument) {

			throw new IllegalArgumentException("unknown option: " + option);
		}
	}

	public static void main(String[] args) throws IOException, DocumentException {

		DetectorFactoryCollection.instance();
		ListBugDatabaseInfoCommandLine commandLine = new ListBugDatabaseInfoCommandLine();
		int argCount = commandLine.parse(args, 1, Integer.MAX_VALUE, USAGE);

		Project project = new Project();
		BugCollection origCollection;
		PrintWriter out = new PrintWriter(System.out);
		out.println("version	time	classes	NCSS	file");
		while (argCount < args.length) {
			origCollection = new SortedBugCollection(
					SortedBugCollection.MultiversionBugInstanceComparator.instance);
			BugCollection oCollection = origCollection;
			String fileName = args[argCount++];
			origCollection.readXML(fileName, project);
			AppVersion appVersion = origCollection.getCurrentAppVersion();
			out.print(appVersion.getReleaseName());
			out.print('\t');
			if (formatDates)
				out.print("\""+ new Date(appVersion.getTimestamp()) + "\"");
			else
				out.print(appVersion.getTimestamp());
			out.print('\t');

			out.print(appVersion.getNumClasses());
			out.print('\t');
			out.print(appVersion.getCodeSize());
			out.print('\t');
			out.print(fileName);


			out.println();
		}
		out.close();
	}

}
