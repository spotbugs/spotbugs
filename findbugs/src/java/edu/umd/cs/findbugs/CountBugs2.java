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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Count warnings in a BugCollection matching specified criteria. 
 * 
 * @author David Hovemeyer
 */
public class CountBugs2 {
	
	private BugCollection bugCollection;
	private Set<String> categorySet;
	private Set<String> abbrevSet;
	private int minPriority;
	private int count;
	
	public CountBugs2(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
		this.categorySet = new HashSet<String>();
		this.abbrevSet = new HashSet<String>();
		this.minPriority = Detector.NORMAL_PRIORITY;
	}
	
	/**
	 * @param minPriority The minPriority to set.
	 */
	public void setMinPriority(int minPriority) {
		this.minPriority = minPriority;
	}
	
	public void setCategories(String categories) {
		buildSetFromString(categories, categorySet);
	}
	
	public void setAbbrevs(String abbrevs) {
		buildSetFromString(abbrevs, abbrevSet);
	}

	private void buildSetFromString(String str, Set<String> set) {
		StringTokenizer t = new StringTokenizer(str, ",");
		while (t.hasMoreTokens()) {
			String category = t.nextToken();
			set.add(category);
		}
	}
	
	public CountBugs2 execute() {
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext();) {
			BugInstance warning = i.next();
			BugPattern pattern = warning.getBugPattern();

			if (pattern == null && (!categorySet.isEmpty() || !abbrevSet.isEmpty()))
				continue;
			
			if (!categorySet.isEmpty()
					&& !categorySet.contains(pattern.getCategory()))
				continue;
			
			if (!abbrevSet.isEmpty()
					&& !abbrevSet.contains(pattern.getAbbrev()))
				continue;
			
			if (warning.getPriority() > minPriority)
				continue;
			
			++count;
		}
		return this;
	}
	
	/**
	 * @return Returns the count.
	 */
	public int getCount() {
		return count;
	}
	
	static class CountBugs2CommandLine extends CommandLine {
		int minPriority;
		String categories;
		String abbrevs;
		
		CountBugs2CommandLine() {
			addOption("-categories", "cat1,cat2...", "set bug categories");
			addOption("-abbrevs", "abbrev1,abbrev2...", "set bug type abbreviations");
			addOption("-minPriority", "priority", "set min bug priority (3=low, 2=medium, 1=high)");
		}
		
		/**
		 * @return Returns the categories.
		 */
		public String getCategories() {
			return categories;
		}
		
		/**
		 * @return Returns the abbrevs.
		 */
		public String getAbbrevs() {
			return abbrevs;
		}
		
		/**
		 * @return Returns the minPriority.
		 */
		public int getMinPriority() {
			return minPriority;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			throw new IllegalArgumentException("Unknown option: " + option);
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			if (option.equals("-categories")) {
				categories = argument;
			} else if (option.equals("-abbrevs")) {
				abbrevs = argument;
			} else if (option.equals("-minPriority")) {
				minPriority = Integer.parseInt(argument);
			} else {
				throw new IllegalArgumentException("Unknown option: " + option);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		DetectorFactoryCollection.instance(); // load plugins
		
		CountBugs2CommandLine commandLine = new CountBugs2CommandLine();
		int argCount = commandLine.parse(args);
		
		if (args.length - argCount != 1) {
			System.err.println("Usage: " + CountBugs2.class.getName() +
					" [options] <bug collection>");
			System.err.println("Options:");
			commandLine.printUsage(System.err);
			System.exit(1);
		}
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(args[argCount], new Project());
		
		CountBugs2 countBugs = new CountBugs2(bugCollection);
		if (commandLine.getAbbrevs() != null)
			countBugs.setAbbrevs(commandLine.getAbbrevs());
		if (commandLine.getCategories() != null)
			countBugs.setCategories(commandLine.getCategories());
		countBugs.setMinPriority(commandLine.getMinPriority());
		
		countBugs.execute();
		System.out.println(countBugs.getCount());
	}
}
