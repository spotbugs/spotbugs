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

package edu.umd.cs.findbugs.workflow;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Mine historical information from a BugCollection.
 * The BugCollection should be built using UpdateBugCollection
 * to record the history of analyzing all versions over time.
 * 
 * @author David Hovemeyer
 * @author William Pugh
 */
public class MineBugHistory {
	static final int ADDED      = 0;
	static final int NEWCODE    = 1;
	static final int REMOVED    = 2;
	static final int REMOVEDCODE= 3;
	static final int RETAINED   = 4;
	static final int DEAD       = 5;
	static final int ACTIVE_NOW = 6;
	static final int TUPLE_SIZE = 7;
	
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
			if (key == ADDED || key == RETAINED || key == NEWCODE)
				tuple[ACTIVE_NOW]++;
		}
		
		int get(int key) {
			return tuple[key];
		}
	}
	
	BugCollection bugCollection;
	Version[] versionList;
	Map<Long, AppVersion> sequenceToAppVersionMap = new HashMap<Long, AppVersion>();
	boolean formatDates = false;
	
	public MineBugHistory() {
	}
	public MineBugHistory(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}

	public void setBugCollection(BugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}

	public void setFormatDates(boolean value) {
		this.formatDates = value;
	}

	
	
	public MineBugHistory execute() {
		long sequenceNumber = bugCollection.getSequenceNumber();
		int maxSequence = (int)sequenceNumber;
		versionList = new Version[maxSequence + 1];
		for (int i = 0; i <= maxSequence; ++i) {
			versionList[i] = new Version(i);
		}
		
		for (Iterator<AppVersion> i = bugCollection.appVersionIterator(); i.hasNext();) {
			AppVersion appVersion = i.next();
			long versionSequenceNumber = appVersion.getSequenceNumber();
			sequenceToAppVersionMap.put(Long.valueOf(versionSequenceNumber), appVersion);
		}
		
		AppVersion currentAppVersion = bugCollection.getCurrentAppVersion();
		sequenceToAppVersionMap.put(
			Long.valueOf(sequenceNumber),
			currentAppVersion);
		
		for (Iterator<BugInstance> j = bugCollection.iterator(); j.hasNext();) {
			BugInstance bugInstance = j.next();

			for (int i = 0; i <= maxSequence; ++i) {
				if (bugInstance.getFirstVersion() > i) continue;
				boolean activePrevious = bugInstance.getFirstVersion() < i
					&& (bugInstance.getLastVersion() == -1 || bugInstance.getLastVersion() >= i-1 );
				boolean activeCurrent = bugInstance.getLastVersion() == -1 || bugInstance.getLastVersion() >= i ;
				
				int key = getKey(activePrevious, activeCurrent);
				if (key == REMOVED && !bugInstance.isRemovedByChangeOfPersistingClass()) key = REMOVEDCODE;
				else if (key == ADDED && !bugInstance.isIntroducedByChangeOfExistingClass()) key = NEWCODE;
				versionList[i].increment(key);
			}
		}
		
		return this;
	}

	
	

	public void dump(PrintStream out) {
		out.println("seq	version	time	classes	NCSS	added	newCode	fixed	removed	retained	dead	active");
		for (int i = 0; i < versionList.length; ++i) {
			Version version = versionList[i];
			AppVersion appVersion = sequenceToAppVersionMap.get(version.getSequence());
			out.print(i);
			out.print('\t');
			out.print(appVersion != null ? appVersion.getReleaseName() : "");
			out.print('\t');
			if (formatDates)
				out.print("\"" + (appVersion != null ?  new Date(appVersion.getTimestamp()).toString() : "") + "\"");
			else out.print(appVersion != null ? appVersion.getTimestamp() : 0L);
			out.print('\t');
			if (appVersion != null) {
				out.print(appVersion.getNumClasses());
				out.print('\t');
				out.print(appVersion.getCodeSize());

			} else out.print("\t0\t0");

			for (int j = 0; j < TUPLE_SIZE; ++j) {
				out.print('\t');
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

	 class MineBugHistoryCommandLine extends CommandLine {

		MineBugHistoryCommandLine() {
			addSwitch("-formatDates", "render dates in textual form");
		}

		public void handleOption(String option, String optionalExtraPart) {
			if  (option.equals("-formatDates")) 
				setFormatDates(true);
			else 
			throw new IllegalArgumentException("unknown option: " + option);
		}

		public void handleOptionWithArgument(String option, String argument) {

				throw new IllegalArgumentException("unknown option: " + option);
		}
	}

	public static void main(String[] args) throws Exception {
		DetectorFactoryCollection.instance(); // load plugins

		MineBugHistory mineBugHistory = new MineBugHistory();
		MineBugHistoryCommandLine commandLine = mineBugHistory.new MineBugHistoryCommandLine();
		int argCount = commandLine.parse(args, 0, 2, "Usage: " + MineBugHistory.class.getName()
				+ " [options] [<xml results> [<history]] ");

		SortedBugCollection bugCollection = new SortedBugCollection();
		if (argCount < args.length)  
			bugCollection.readXML(args[argCount++], new Project());
		else bugCollection.readXML(System.in, new Project());
		mineBugHistory.setBugCollection(bugCollection);

		mineBugHistory.execute();
		PrintStream out = System.out;
		
		try {
		if (argCount < args.length)  {
			out = new PrintStream(new FileOutputStream(args[argCount++]), true);
			}
		mineBugHistory.dump(out);
		} finally {
		out.close();
		}
		
	}
}
