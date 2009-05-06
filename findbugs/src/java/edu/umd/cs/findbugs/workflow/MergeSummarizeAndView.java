/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.CommandLineUiCallback;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.PrintingBugReporter;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.gui2.FindBugsLayoutManagerFactory;
import edu.umd.cs.findbugs.gui2.GUISaveState;
import edu.umd.cs.findbugs.gui2.MainFrame;
import edu.umd.cs.findbugs.gui2.SplitLayout;

/**
 * Compute the union of two sets of bug results,
 * preserving annotations.
 */
public class MergeSummarizeAndView {

	static class MyCommandLine extends CommandLine {
		public List<String> workingDirList = new ArrayList<String>();
		public List<String> srcDirList = new ArrayList<String>();
		int maxRank = 12;
		int maxAge = 7;
		boolean alwaysShowGui = false;

		MyCommandLine() {
			addOption("-workingDir", "filename", "Comma separated list of current working directory paths, used to resolve relative paths (Jar, AuxClasspathEntry, SrcDir)");
			addOption("-srcDir", "filename", "Comma separated list of directory paths, used to resolve relative SourceFile paths");
			addOption("-maxRank", "rank", "maximum rank of issues to show in summary (default 12)");
			addOption("-maxAge", "days", "maximum age of issues to show in summary (default 7)");
			addSwitch("-gui", "display GUI for any warnings. Default: Displays GUI for warnings meeting filtering criteria");
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
                  if (option.equals("-gui")) alwaysShowGui = true;
                  else throw new IllegalArgumentException("Unknown option : " + option);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			if (option.equals("-workingDir")) workingDirList = Arrays.asList(argument.split(","));
			else if (option.equals("-srcDir")) srcDirList = Arrays.asList(argument.split(","));
			else if (option.equals("-maxRank")) maxRank = Integer.parseInt(argument);
			else if (option.equals("-maxAge")) maxAge = Integer.parseInt(argument);
			else throw new IllegalArgumentException("Unknown option : " + option);
		}

	}

	static {
		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins
	}
	static public SortedBugCollection union (SortedBugCollection origCollection, SortedBugCollection newCollection) {

		SortedBugCollection result = origCollection.duplicate();

		for (Iterator<BugInstance> i = newCollection.iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();
			result.add(bugInstance);
		}
		ProjectStats stats = result.getProjectStats();
		ProjectStats stats2 = newCollection.getProjectStats();
		stats.addStats(stats2);

		Project project = result.getProject();
		project.add(newCollection.getProject());

		return result;
	}

	public static void main(String[] argv) throws Exception  {


		final MyCommandLine commandLine = new MyCommandLine();

		int argCount = commandLine.parse(argv, 1, Integer.MAX_VALUE, "Usage: " + MergeSummarizeAndView.class.getName()
				+ " [options] [<results1> <results2> ... <resultsn>] ");

		if (commandLine.workingDirList.isEmpty()) {
			String userDir = System.getProperty("user.dir");
			if (null != userDir && !"".equals(userDir)) {
				commandLine.workingDirList.add(userDir);
			}
		}

		IGuiCallback cliUiCallback = new CommandLineUiCallback();
		SortedBugCollection results = null;
		for(int i = argCount; i < argv.length; i++) {
			try {
				SortedBugCollection more = createPreconfiguredBugCollection(
						commandLine.workingDirList, commandLine.srcDirList, cliUiCallback);

				more.readXML(argv[i]);
				if (results != null) {
					results = union(results, more);
				} else {
					results = more;
				}
			} catch (IOException e) {
				System.err.println("Trouble reading/parsing " + argv[i]);
			} catch (DocumentException e) {
				System.err.println("Trouble reading/parsing " + argv[i]);
			}
		}

		if (results == null) {
			System.err.println("No files successfully read");
			System.exit(1);
		}

		
		results.setRequestDatabaseCloud(true);
		Cloud cloud = results.reinitializeCloud();
		Project project = results.getProject();
		Cloud.Mode originalMode = cloud.getMode();
		System.out.println("Cloud is " + cloud.getClass().getName());
		System.out.println("Original cloud mode is " + originalMode);
		
		cloud.setMode(Cloud.Mode.COMMUNAL);
		MyBugReporter reporter = new MyBugReporter();
                // TODO: Is firstSeen really in ms and not in seconds? If not, divide the following by 1000.
		long old = System.currentTimeMillis() - commandLine.maxAge * 24 * 3600 * 1000L; 
		RuntimeException storedException = null;
		int badRank = 0;
		int tooOld = 0;
		int shown = 0;
		for (BugInstance warning :  results.getCollection())
			if (!reporter.isApplySuppressions() || !project.getSuppressionFilter().match(warning) ) {
				int rank =  BugRanker.findRank(warning);
				if (rank > 20) continue;
				if (cloud.overallClassificationIsNotAProblem(warning))
					continue;
				long firstSeen = cloud.getFirstSeen(warning);
				boolean isOld = firstSeen != 0 && firstSeen < old;
				boolean highRank = rank > commandLine.maxRank;
				if (highRank)
					badRank++;
				else if (isOld)
					tooOld++;
				else try {
					if (shown == 0) {
						System.out.printf("%4s\n","days");
						System.out.printf("%4s %4s %s\n","old", "rank", "issue");
					}
					shown++;
					System.out.printf("%4d %4d ", ageInDays(firstSeen), rank);
					reporter.printBug(warning);
				} catch (RuntimeException e) {
					if (storedException == null) 
						storedException = e;
				}
			}
		
		if (badRank > 0 || tooOld > 0) {
			System.out.print("\nplus ");
			if (badRank > 0) 
				System.out.printf("%d less scary issues", badRank);
			if (badRank > 0 && tooOld > 0)
				System.out.printf(" and ");
			if (tooOld > 0)
				System.out.printf("%d older issues", tooOld);
			System.out.println();
		}
		if (storedException != null)
			throw storedException;
		
		if (shown > 0 || (commandLine.alwaysShowGui && results.getCollection().size() > 0)) {
			GUISaveState.loadInstance();
			cloud.setMode(originalMode);
			FindBugsLayoutManagerFactory factory = new FindBugsLayoutManagerFactory(SplitLayout.class.getName());
			MainFrame.makeInstance(factory);
			MainFrame instance = MainFrame.getInstance();
			instance.waitUntilReady();

			instance.openBugCollection(results);
		}


	}

	static SortedBugCollection createPreconfiguredBugCollection(List<String> workingDirList, List<String> srcDirList, IGuiCallback guiCallback) {
		Project project = new Project();
		for (String cwd : workingDirList) {
			project.addWorkingDir(cwd);
		}
		for (String srcDir : srcDirList) {
			project.addSourceDir(srcDir);
		}
		project.setGuiCallback(guiCallback);
		return new SortedBugCollection(project);
	}


	static int ageInDays(long firstSeen) {
		return (int) (NOW - firstSeen) / 24 / 3600 / 1000;
	}
	static final long NOW = System.currentTimeMillis();

	static class MyBugReporter extends PrintingBugReporter {

		@Override
		public void printBug(BugInstance bugInstance) {
			super.printBug(bugInstance);
		}
	}



}

// vim:ts=3
