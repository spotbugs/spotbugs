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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import java.util.*;

import org.dom4j.DocumentException;

/**
 * Count bugs in a result file by category.
 */
public class CountBugs {
	private SortedBugCollection bugCollection;
	private Project project;
	private HashSet<String> categorySet;
	private TreeMap<String, Integer> countMap;

	public CountBugs(String resultsFileName) throws IOException, DocumentException {
		this(new SortedBugCollection(), new Project());

		bugCollection.readXML(resultsFileName, project);
	}

	public CountBugs(SortedBugCollection bugCollection, Project project) {
		this.bugCollection = bugCollection;
		this.project = project;
		this.categorySet = new HashSet<String>();
		this.countMap = new TreeMap<String, Integer>();
	}

	public SortedBugCollection getBugCollection() {
		return bugCollection;
	}

	public Project getProject() {
		return project;
	}

	public void addCategory(String category) {
		categorySet.add(category);
	}

	public void setCategories(String categoryList) {
		StringTokenizer tok = new StringTokenizer(categoryList, ",");
		while (tok.hasMoreTokens()) {
			String category = tok.nextToken();
			categorySet.add(category);
			countMap.put(category, new Integer(0));
		}
	}

	public Integer getCount(String category) {
		Integer count = countMap.get(category);
		if (count == null)
			count = new Integer(0);
		return count;
	}

	public Iterator<Map.Entry<String, Integer>> entryIterator() {
		return countMap.entrySet().iterator();
	}

	public void execute() {
		DetectorFactoryCollection.instance(); // load plugins

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			String category = bugInstance.getAbbrev();

			if (categorySet.size() > 0 && !categorySet.contains(category))
				continue;

			Integer count = countMap.get(category);
			if (count == null)
				count = new Integer(0);
			countMap.put(category, new Integer(count.intValue() + 1));
		}
	}

	public void diffCounts(CountBugs other) {
		TreeSet<String> allCategories = new TreeSet<String>();
		allCategories.addAll(countMap.keySet());
		allCategories.addAll(other.countMap.keySet());

		for (Iterator<String> i = allCategories.iterator(); i.hasNext(); ) {
			String category = i.next();
			Integer delta = new Integer(getCount(category).intValue() - other.getCount(category).intValue());
			countMap.put(category, delta);
		}
	}

	public void printCounts(OutputStream out, boolean deltas) {
		PrintStream pout = new PrintStream(out);

		Iterator<Map.Entry<String, Integer>> j = entryIterator();
		while(j.hasNext()) {
			Map.Entry<String,Integer> entry = j.next();
			int count = entry.getValue().intValue();
			if (count > 0) {
				pout.print(entry.getKey() + ":\t");
				if (deltas) {
					if (count < 0)
						pout.print("-");
					else if (count > 0)
						pout.print("+");
				}
				pout.println(count);
			}
		}
		pout.flush();
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println(CountBugs.class.getName() + " [-categories <categories>] <results file>");
		System.out.println(CountBugs.class.getName() + " [-categories <categories>] -diff <orig results> <new results>");
		System.exit(1);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 1) {
			usage();
		}

		int arg = 0;
		String categoryList = "";
		boolean diffMode = false;

		while (arg < argv.length - 1) {
			String option = argv[arg];

			if (option.equals("-categories")) {
				++arg;
				if (arg >= argv.length)
					throw new IllegalArgumentException("-categories option requires argument");
				categoryList = argv[arg];
			} else if (option.equals("-diff")) {
				diffMode = true;
			} else
				break;

			++arg;
		}

		if (arg >= argv.length)
			usage();

		String filename = argv[arg++];
		CountBugs countBugs = new CountBugs(filename);
		countBugs.execute();

		if (diffMode) {
			if (arg >= argv.length)
				usage();
			CountBugs countBugs2 = new CountBugs(argv[arg]);
			countBugs2.execute();

			countBugs.diffCounts(countBugs2);
		}

		countBugs.printCounts(System.out, diffMode);
	}
}

// vim:ts=3
