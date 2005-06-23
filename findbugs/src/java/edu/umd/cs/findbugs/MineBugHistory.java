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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
	static final int TUPLE_SIZE = 4;
	
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
		}
		
		int get(int key) {
			return tuple[key];
		}
	}
	
	BugCollection bugCollection;
	Version[] versionList;
	Map<Long, AppVersion> sequenceToAppVersionMap;
	
	public MineBugHistory(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
		this.sequenceToAppVersionMap = new HashMap<Long, AppVersion>();
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
		
		for (Iterator<BugInstance> j = bugCollection.iterator(); j.hasNext();) {
			for (int i = 1; i <= maxSequence; ++i) {
				BugInstance bugInstance = j.next();
				
				SequenceIntervalCollection whenActive = bugInstance.getActiveIntervalCollection();
				
				boolean activePrevious = whenActive.contains(((long)i) -1);
				boolean activeCurrent = whenActive.contains((long)i);
				
				int key = getKey(activePrevious, activeCurrent);
				versionList[i].increment(key);
			}
		}
		
		return this;
	}
	
	public void dump(PrintStream out) {
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
		}
	}

	/**
	 * Get key used to classify the presence and/or abscence of a BugInstance
	 * in successive versions in the history.
	 * 
	 * @param activePrevious true if the bug was active in the previous version, false if not
	 * @param activeCurrent  trus if the bug is active in the current version, false if not
	 * @return the key: one of ADDED, RETAINED, REMOVED, and DEAD
	 */
	private int getKey(boolean activePrevious, boolean activeCurrent) {
		if (activePrevious)
			return activeCurrent ? RETAINED : REMOVED;
		else
			return activeCurrent ? ADDED : DEAD;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Usage: " + MineBugHistory.class.getName() + " <bug collection>");
			System.exit(1);
		}
		
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(args[0], new Project());
		MineBugHistory mineBugHistory = new MineBugHistory(bugCollection);
		mineBugHistory.execute();
		mineBugHistory.dump(System.out);
		
	}
}
