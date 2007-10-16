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
import java.util.Iterator;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.ProjectStats;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Compute the union of two sets of bug results,
 * preserving annotations.
 */
public class UnionResults {
	
	static class UnionResultsCommandLine extends CommandLine {
		public String outputFile;
		boolean withMessages;
		
		UnionResultsCommandLine() {
			addSwitch("-withMessages", "Generated XML should contain msgs for external processing");
			addOption("-output", "outputFile", "File in which to store combined results");
		}

		/* (non-Javadoc)
         * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
         */
        @Override
        protected void handleOption(String option, String optionExtraPart) throws IOException {
        	if (option.equals("-withMessages")) withMessages = true;
        	else throw new IllegalArgumentException("Unknown option : " + option);
        }

		/* (non-Javadoc)
         * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
         */
        @Override
        protected void handleOptionWithArgument(String option, String argument) throws IOException {
        	if (option.equals("-output")) outputFile = argument;
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

		return result;
	}

	public static void main(String[] argv) throws Exception {


		final UnionResultsCommandLine commandLine = new UnionResultsCommandLine();

		int argCount = commandLine.parse(argv, 2, Integer.MAX_VALUE, "Usage: " + UnionResults.class.getName()
				+ " [options] [<results1> <results2> ... <resultsn>] ");
		

		SortedBugCollection results = new SortedBugCollection();
		Project project = new Project();
		results.readXML(argv[argCount++], project);
		for(int i = argCount; i < argv.length; i++) {
			try {
			SortedBugCollection more = new SortedBugCollection();
			Project newProject = new Project();
			more.readXML(argv[i], newProject);
			project.add(newProject);
			results = union(results, more);
			} catch (Exception e) {
					System.err.println("Trouble parsing " + argv[i]);
					e.printStackTrace();
			}
		}

		results.setWithMessages(commandLine.withMessages);
		if (commandLine.outputFile == null)
			results.writeXML(System.out, project);
		else
			results.writeXML(commandLine.outputFile, project);
	}


}

// vim:ts=3
