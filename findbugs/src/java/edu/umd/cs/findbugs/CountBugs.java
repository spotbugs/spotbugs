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
	private interface Key extends Comparable<Key> {
	}

	private interface KeyFactory {
		public Key createKey(BugInstance bugInstance);

		public Set<Key> createKeySet(String s);
	}

	private static class CategoryKey implements Key {
		private String category;

		public CategoryKey(String category) {
			this.category = category;
		}

		public int compareTo(Key o) {
			int cmp;

			cmp = this.getClass().getName().compareTo(o.getClass().getName());
			if (cmp != 0) return cmp;
			CategoryKey other = (CategoryKey) o;
			return category.compareTo(other.category);
		}

		public int hashCode() {
			return category.hashCode();
		}

		public boolean equals(Object o) {
			if (this.getClass() != o.getClass()) return false;
			return category.equals(((CategoryKey) o).category);
		}

		public String toString() {
			return category;
		}
	}

	private static class CategoryKeyFactory implements KeyFactory {
		public Key createKey(BugInstance bugInstance) {
			return new CategoryKey(bugInstance.getAbbrev());
		}

		public Set<Key> createKeySet(String keyList) {
			Set<Key> keySet = new HashSet<Key>();
			StringTokenizer tok = new StringTokenizer(keyList, ",");
			while (tok.hasMoreTokens()) {
				String category = tok.nextToken();
				keySet.add(new CategoryKey(category));
			}
			return keySet;
		}
	}

	private static final HashMap<String, String> kingdomToAbbrevMap = new HashMap<String, String>();
	private static final HashMap<Integer, String> priorityToAbbrevMap = new HashMap<Integer, String>();

	static {
		kingdomToAbbrevMap.put("CORRECTNESS", "C");
		kingdomToAbbrevMap.put("MT_CORRECTNESS", "M");
		kingdomToAbbrevMap.put("MALICIOUS_CODE", "V");
		kingdomToAbbrevMap.put("PERFORMANCE", "P");
		priorityToAbbrevMap.put(new Integer(1), "H");
		priorityToAbbrevMap.put(new Integer(2), "M");
		priorityToAbbrevMap.put(new Integer(3), "L");
	}

	private static class KingdomAndPriorityKey implements Key {
		private String legend;

		public KingdomAndPriorityKey(String legend) {
			this.legend = legend;
		}

		public KingdomAndPriorityKey(String kingdom, int prio) {
			this(priorityToAbbrevMap.get(new Integer(prio)) + "/" + kingdomToAbbrevMap.get(kingdom));
		}

		public int compareTo(Key o) {
			int cmp;
			cmp = this.getClass().getName().compareTo(o.getClass().getName());
			if (cmp != 0) return cmp;
			KingdomAndPriorityKey other = (KingdomAndPriorityKey) o;
			return legend.compareTo(other.legend);
		}

		public int hashCode() {
			return legend.hashCode();
		}

		public boolean equals(Object o) {
			if (this.getClass() != o.getClass()) return false;
			KingdomAndPriorityKey other = (KingdomAndPriorityKey) o;
			return legend.equals(other.legend);
		}

		public String toString() {
			return legend;
		}
	}

	private static class KingdomAndPriorityKeyFactory implements KeyFactory {
		public Key createKey(BugInstance bugInstance) {
			BugPattern bugPattern = bugInstance.getBugPattern();
			return new KingdomAndPriorityKey(bugPattern.getCategory(), bugInstance.getPriority());
		}

		public Set<Key> createKeySet(String keyList) {
			//System.out.println("key list string is " + keyList);
			Set<Key> keySet = new HashSet<Key>();
			StringTokenizer tok = new StringTokenizer(keyList, ",");
			while (tok.hasMoreTokens()) {
				String legend = tok.nextToken();
				//System.out.println("adding " + legend);
				keySet.add(new KingdomAndPriorityKey(legend));
			}
			return keySet;
		}
	}

	private SortedBugCollection bugCollection;
	private Project project;
	private KeyFactory keyFactory;
	private Set<Key> keySet;
	private TreeMap<Key, Integer> countMap;
	private int minPriority;

	public CountBugs(String resultsFileName) throws IOException, DocumentException {
		this(new SortedBugCollection(), new Project());

		bugCollection.readXML(resultsFileName, project);
	}

	public CountBugs(SortedBugCollection bugCollection, Project project) {
		this.bugCollection = bugCollection;
		this.project = project;
		this.keyFactory = new CategoryKeyFactory();
		this.keySet = new HashSet<Key>();
		this.countMap = new TreeMap<Key, Integer>();
		this.minPriority = 3;
	}

	public SortedBugCollection getBugCollection() {
		return bugCollection;
	}

	public Project getProject() {
		return project;
	}

	public void addKey(Key key) {
		keySet.add(key);
	}

	public void setKeyFactory(String keyMode) {
		if (keyMode.equals("-categories"))
			keyFactory = new CategoryKeyFactory();
		else if (keyMode.equals("-kingdomAndPriority"))
			keyFactory = new KingdomAndPriorityKeyFactory();
		else
			throw new IllegalArgumentException("Unknown key mode: " + keyMode);
	}

	public void setKeys(String keyList) {
		keySet = keyFactory.createKeySet(keyList);
	}

	public Integer getCount(Key key) {
		Integer count = countMap.get(key);
		if (count == null)
			count = new Integer(0);
		return count;
	}

	public int getTotal() {
		int total = 0;
		for (Iterator<Integer> i = countMap.values().iterator(); i.hasNext();) {
			total += i.next().intValue();
		}
		return total;
	}

	public void execute() {
		DetectorFactoryCollection.instance(); // load plugins

		Iterator<BugInstance> i = bugCollection.iterator();
		while (i.hasNext()) {
			BugInstance bugInstance = i.next();
			Key key = keyFactory.createKey(bugInstance);//bugInstance.getAbbrev();

			if (bugInstance.getPriority() > minPriority)
				continue;

			if (keySet.size() > 0 && !keySet.contains(key))
				continue;

			Integer count = countMap.get(key);
			if (count == null)
				count = new Integer(0);
			countMap.put(key, new Integer(count.intValue() + 1));
		}
	}

	public void diffCounts(CountBugs newer) {
		TreeSet<Key> allCategories = new TreeSet<Key>();
		allCategories.addAll(countMap.keySet());
		allCategories.addAll(newer.countMap.keySet());

		for (Iterator<Key> i = allCategories.iterator(); i.hasNext();) {
			Key key = i.next();
			Integer delta = new Integer(newer.getCount(key).intValue() - getCount(key).intValue());
			countMap.put(key, delta);
		}
	}

	public void printCounts(OutputStream out, boolean deltas) {
		PrintStream pout = new PrintStream(out);

		Iterator<Key> j = countMap.keySet().iterator();
		while (j.hasNext()) {
			Key key = j.next();
			int count = countMap.get(key).intValue();

			if (count != 0) {
				pout.print(key + ":\t");
				if (deltas && count > 0)
					pout.print("+");
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
		String keyList = "";
		String keyMode = "-categories";
		boolean diffMode = false;
		int minPriority = 3;

		while (arg < argv.length - 1) {
			String option = argv[arg];

			if (option.equals("-categories") || option.equals("-kingdomAndPriority")) {
				keyMode = argv[arg];
				++arg;
				if (arg >= argv.length)
					throw new IllegalArgumentException("-categories option requires argument");
				keyList = argv[arg];
			} else if (option.equals("-diff")) {
				diffMode = true;
			} else if (option.equals("-minPriority")) {
				++arg;
				if (arg >= argv.length)
					throw new IllegalArgumentException("-minPriority option requires argument");
				minPriority = Integer.parseInt(argv[arg]);
				//System.err.println("Min priority is " + minPriority);
			} else
				break;

			++arg;
		}

		if (arg >= argv.length)
			usage();

		String filename = argv[arg++];
		CountBugs countBugs = new CountBugs(filename);
		countBugs.setKeyFactory(keyMode);
		countBugs.setKeys(keyList);
		countBugs.minPriority = minPriority;
		countBugs.execute();

		if (diffMode) {
			if (arg >= argv.length)
				usage();
			CountBugs countBugs2 = new CountBugs(argv[arg++]);
			countBugs2.setKeyFactory(keyMode);
			countBugs2.setKeys(keyList);
			countBugs2.minPriority = minPriority;
			countBugs2.execute();

			countBugs.diffCounts(countBugs2);
		}

		countBugs.printCounts(System.out, diffMode);
		if (!diffMode)
			System.out.println("Total:\t" + countBugs.getTotal());
	}
}

// vim:ts=3
