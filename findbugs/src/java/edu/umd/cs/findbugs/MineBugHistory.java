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

import edu.umd.cs.findbugs.config.CommandLine;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Mine historical information from a BugCollection.
 * The BugCollection should be built using UpdateBugCollection
 * to record the history of analyzing all versions over time.
 * 
 * @author David Hovemeyer
 */
public class MineBugHistory {
	static final int ADDED      = 0;
	static final int REMOVED    = 1;
	static final int RETAINED   = 2;
	static final int DEAD       = 3;
	static final int ACTIVE_NOW = 4;
	static final int TUPLE_SIZE = 5;
	
	static class Version {
		long sequence;
		int tuple[] = new int[TUPLE_SIZE];
		
		Version(long sequence) {
			this.sequence = sequence;
		}
		
		/**
		 * @return Returns the sequence.
		 */
		public long getSequence() {
			return sequence;
		}

		void increment(int key) {
			tuple[key]++;
			if (key == ADDED || key == RETAINED)
				tuple[ACTIVE_NOW]++;
		}
		
		int get(int key) {
			return tuple[key];
		}
	}
	
	BugCollection bugCollection;
	Version[] versionList;
	Map<Long, AppVersion> sequenceToAppVersionMap;
	int prio = Detector.LOW_PRIORITY;
	Set<String> categorySet;
	
	public MineBugHistory(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
		this.sequenceToAppVersionMap = new HashMap<Long, AppVersion>();
		this.categorySet = new HashSet<String>();
	}

	public void setPrio(int prio) {
		this.prio = prio;
	}

	public void setCategories(String categories) {
		StringTokenizer t = new StringTokenizer(categories, ",");
		while (t.hasMoreTokens()) {
			String category = t.nextToken();
			categorySet.add(category);
		}
	}
	
	public MineBugHistory execute() {
		int maxSequence = (int)bugCollection.getSequenceNumber();
		versionList = new Version[maxSequence + 1];
		for (int i = 0; i <= maxSequence; ++i) {
			versionList[i] = new Version(i);
		}
		
		for (Iterator<AppVersion> i = bugCollection.appVersionIterator(); i.hasNext();) {
			AppVersion appVersion = i.next();
			sequenceToAppVersionMap.put(Long.valueOf(appVersion.getSequenceNumber()), appVersion);
		}
		sequenceToAppVersionMap.put(
			Long.valueOf(bugCollection.getSequenceNumber()),
			bugCollection.getCurrentAppVersion());
		
		for (Iterator<BugInstance> j = bugCollection.iterator(); j.hasNext();) {
			BugInstance bugInstance = j.next();

			if (ignore(bugInstance))
				continue;

			for (int i = 0; i <= maxSequence; ++i) {
				if (bugInstance.getFirstVersion() > i) continue;
				boolean activePrevious = bugInstance.getFirstVersion() < i;
				boolean activeCurrent = bugInstance.getLastVersion() == -1 || bugInstance.getLastVersion() >= i ;
				
				int key = getKey(activePrevious, activeCurrent);
				versionList[i].increment(key);
			}
		}
		
		return this;
	}

	private boolean ignore(BugInstance bugInstance) {
		if (bugInstance.getPriority() > this.prio)
			return true;
		if (!categorySet.isEmpty()
			&& !categorySet.contains(bugInstance.getBugPattern().getCategory()))
			return true;
		return false;
	}
	
	public void dump(PrintStream out) {
		out.println("seq,release,time,added,removed,persist,dead,active");
		for (int i = 1; i < versionList.length; ++i) {
			Version version = versionList[i];
			AppVersion appVersion = sequenceToAppVersionMap.get(version.getSequence());
			out.print(i);
			out.print(',');
			out.print(appVersion != null ? appVersion.getReleaseName() : "");
			out.print(',');
			out.print(appVersion != null ? appVersion.getTimestamp() : 0L);
			for (int j = 0; j < TUPLE_SIZE; ++j) {
				out.print(',');
				out.print(version.get(j));
			}
			out.println();
		}
	}

	/**
	 * Get key used to classify the presence and/or abscence of a BugInstance
	 * in successive versions in the history.
	 * 
	 * @param activePrevious true if the bug was active in the previous version, false if not
	 * @param activeCurrent  true if the bug is active in the current version, false if not
	 * @return the key: one of ADDED, RETAINED, REMOVED, and DEAD
	 */
	private int getKey(boolean activePrevious, boolean activeCurrent) {
		if (activePrevious)
			return activeCurrent ? RETAINED : REMOVED;
		else // !activePrevious
			return activeCurrent ? ADDED : DEAD;
	}

	static class MineBugHistoryCommandLine extends CommandLine {
		int prio = Detector.LOW_PRIORITY;
		String categories = "";

		MineBugHistoryCommandLine() {
			addOption("-prio", "min priority", "set min priority");
			addOption("-categories", "cat1[,cat2...]", "set categories");
		}

		public int getPrio() {
			return prio;
		}

		public String getCategories() {
			return categories;
		}

		public void handleOption(String option, String optionalExtraPart) {
			throw new IllegalArgumentException("unknown option: " + option);
		}

		public void handleOptionWithArgument(String option, String argument) {
			if (option.equals("-prio")) {
				prio = Integer.parseInt(argument);
			} else if (option.equals("-categories")) {
				categories = argument;
			} else {
				throw new IllegalArgumentException("unknown option: " + option);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		DetectorFactoryCollection.instance(); // load plugins

		MineBugHistoryCommandLine commandLine = new MineBugHistoryCommandLine();
		int argCount = commandLine.parse(args);

		if (args.length - argCount != 1) {
			System.err.println("Usage: " + MineBugHistory.class.getName() + " [options] <bug collection>");
			System.err.println("Options:");
			commandLine.printUsage(System.err);
			System.exit(1);
		}

		String fileName = args[argCount];
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(fileName, new Project());

		MineBugHistory mineBugHistory = new MineBugHistory(bugCollection);
		mineBugHistory.setPrio(commandLine.getPrio());
		mineBugHistory.setCategories(commandLine.getCategories());

		mineBugHistory.execute();
		mineBugHistory.dump(System.out);
		
	}
}
