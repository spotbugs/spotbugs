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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class ListBugDatabaseInfo {

	private static final String USAGE = "Usage: " + ListBugDatabaseInfo.class.getName()
			+ " [options] data1File data2File data3File ... ";

	static class ListBugDatabaseInfoCommandLine extends CommandLine {

		 boolean formatDates = false;

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
		int argCount = commandLine.parse(args, 0, Integer.MAX_VALUE, USAGE);


		PrintWriter out = new PrintWriter(System.out);
		if (argCount == args.length) 
			listVersion(out,null, commandLine.formatDates);
		else {
			out.println("version	time	classes	NCSS	total	high	medium	low	file");
			while (argCount < args.length) {
				String fileName = args[argCount++];
				listVersion(out, fileName, commandLine.formatDates);
				}
		}
		out.close();
	}

	private static void listVersion(PrintWriter out, @CheckForNull String fileName, boolean formatDates) throws IOException, DocumentException {
		Project project = new Project();
		BugCollection origCollection;
		origCollection = new SortedBugCollection();

		if (fileName == null)
			origCollection.readXML(System.in, project);
		else origCollection.readXML(fileName, project);
		AppVersion appVersion = origCollection.getCurrentAppVersion();
		ProjectStats stats = origCollection.getProjectStats();
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
		out.print(stats.getTotalBugs());
		out.print('\t');
		out.print(stats.getBugsOfPriority(Detector.HIGH_PRIORITY));
		out.print('\t');
		out.print(stats.getBugsOfPriority(Detector.NORMAL_PRIORITY));
		out.print('\t');
		out.print(stats.getBugsOfPriority(Detector.LOW_PRIORITY));
		if (fileName != null) {
			out.print('\t');
			out.print(fileName);
		}


		out.println();
	}

}
