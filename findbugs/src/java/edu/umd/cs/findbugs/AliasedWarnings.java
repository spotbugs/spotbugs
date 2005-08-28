/*
 * FindBugs - Find Bugs in Java programs
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * @author David Hovemeyer
 */
public class AliasedWarnings {
	
	BugCollection bugCollection;
	Comparator<BugInstance> comparator;
	
	public AliasedWarnings(BugCollection bugCollection, Comparator<BugInstance> comparator) {
		this.bugCollection = bugCollection;
		this.comparator= comparator;
	}
	
	public void execute() {
		ArrayList<BugInstance> list = new ArrayList<BugInstance>();
		list.addAll(bugCollection.getCollection());
		
		for (int i = 0; i < list.size(); ++i) {
			BugInstance a = list.get(i);
			for (int j = i + 1; j < list.size(); ++j) {
				BugInstance b = list.get(j);
				
				if (comparator.compare(a, b) == 0) {
					System.out.println(a.getUniqueId() + "=" + b.getUniqueId());
				}
			}
		}
	}
	
	private static final int VERSION_INSENSITIVE_COMPARATOR = 1;
	private static final int FUZZY_COMPARATOR = 2;
	private static final int SLOPPY_COMPARATOR = 3;
	
	static class AliasedWarningsCommandLine extends CommandLine {
		int comparatorType;
		
		AliasedWarningsCommandLine() {
			addSwitch("-vi", "use version insensitive comparator");
			addSwitch("-fuzzy", "use fuzzy comparator");
			addSwitch("-sloppy", "use sloppy comparator");
		}
		
		/**
		 * @return Returns the comparatorType.
		 */
		public int getComparatorType() {
			return comparatorType;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-vi")) {
				comparatorType = VERSION_INSENSITIVE_COMPARATOR;
			} else if (option.equals("-fuzzy")) {
				comparatorType = FUZZY_COMPARATOR;
			} else if (option.equals("-sloppy")) {
				comparatorType = SLOPPY_COMPARATOR;
			} else {
				throw new IllegalArgumentException("Unknown option: " + option);
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			throw new IllegalArgumentException("Unknown option: " + option);
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		DetectorFactoryCollection.instance(); // load plugins
		
		AliasedWarningsCommandLine commandLine = new AliasedWarningsCommandLine();
		int argCount = commandLine.parse(args);
		int comparatorType = commandLine.getComparatorType();
		
//		System.out.println("argCount=" + argCount);
//		System.out.println("comparatorType=" + comparatorType);
		
		if (comparatorType == 0 || argCount != args.length - 1) {
			printUsage(commandLine);
		}
		
		BugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(args[argCount], new Project());
		
		Comparator<BugInstance> comparator;
		switch (comparatorType) {
		case VERSION_INSENSITIVE_COMPARATOR:
			comparator = VersionInsensitiveBugComparator.instance();
			break;
		case FUZZY_COMPARATOR:
			FuzzyBugComparator fuzzy = new FuzzyBugComparator();
			fuzzy.registerBugCollection(bugCollection);
			comparator = fuzzy;
			break;
		case SLOPPY_COMPARATOR:
			comparator = new SloppyBugComparator();
			break;
		default:
			throw new IllegalStateException();
		}
		
		AliasedWarnings aliasedWarnings = new AliasedWarnings(bugCollection, comparator);
		aliasedWarnings.execute();
	}

	@SuppressWarnings("DM_EXIT")
	private static void printUsage(AliasedWarningsCommandLine commandLine) {
		System.err.println("Usage: AliasedWarnings [-vi|-fuzzy|-sloppy] <bug collection>");
		System.err.println("Options:");
		commandLine.printUsage(System.err);
		System.exit(1);
	}
}
