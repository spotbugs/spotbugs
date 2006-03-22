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
import java.text.SimpleDateFormat;
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
import edu.umd.cs.findbugs.TigerSubstitutes;
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
	
	static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd-HH:mm");

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
	boolean noTabs = false;
	
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

	public void setNoTabs(boolean value) {
		this.noTabs = value;
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
			sequenceToAppVersionMap.put(TigerSubstitutes.valueOf(versionSequenceNumber), appVersion);
		}
		
		AppVersion currentAppVersion = bugCollection.getCurrentAppVersion();
		sequenceToAppVersionMap.put(
			TigerSubstitutes.valueOf(sequenceNumber),
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
		if (noTabs) dumpNoTabs(out);
		else dumpOriginal(out);
	}

	/** This is how dump() was implemented up to and including version 0.9.5. */
	public void dumpOriginal(PrintStream out) {
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

	/** emit <code>width</code> space characters to <code>out</code> */
	private static void pad(int width, PrintStream out) {
		while (width-- > 0) out.print(' ');
	}

	/** equivalent to out.print(obj) except it may be padded on the left or right
	 * @param width      padding will occur if the stringified oxj is shorter than this
	 * @param alignRight true to pad on the left, false to pad on the right
	 * @param out        the PrintStream printed to
	 * @param obj        the value to print (may be an auto-boxed primitive) */
	private static void print(int width, boolean alignRight, PrintStream out, Object obj) {
		String s = String.valueOf(obj);
		int padLen = width - s.length();
		if (alignRight) pad(padLen, out);
		out.print(s); // doesn't truncate if (s.length() > width)
		if (!alignRight) pad(padLen, out);
	}

	/** This implementation of dump() tries to better align columns (when viewed
	 *  with a fixed-width font) by padding with spaces instead of using tabs.
	 *  Also, timestamps are formatted more tersely (-formatDates option).
	 *  The bad news is that it requires a minimum of 112 columns.
	 * @see dumpOriginal(PrintStream) */
	public void dumpNoTabs(PrintStream out) {
		//out.println("seq	version	time	classes	NCSS	added	newCode	fixed	removed	retained	dead	active");
		print(3, true, out, "seq");
		out.print(' ');
		print(19, false, out, "version");
		out.print(' ');
		print(16, false, out, "time");
		print(1+7, true, out, "classes");
		print(1+7, true, out, "NCSS");
		print(1+7, true, out, "added");
		print(1+7, true, out, "newCode");
		print(1+7, true, out, "fixed");
		print(1+7, true, out, "removed");
		print(1+8, true, out, "retained");
		print(1+6, true, out, "dead");
		print(1+7, true, out, "active");
		out.println();
		//note: if we were allowed to depend on JDK 1.5 we could use out.printf():
		//Object line[] = { "seq", "version", "time", "classes", "NCSS", "added", "newCode", "fixed", "removed", "retained", "dead", "active" };
		//out.printf("%3s %-19s %-16s %7s %7s %7s %7s %7s %7s %8s %6s %7s%n", line);
		for (int i = 0; i < versionList.length; ++i) {
			Version version = versionList[i];
			AppVersion appVersion = sequenceToAppVersionMap.get(version.getSequence());
			print(3, true, out, i); //out.print(i);
			out.print(' '); // '\t'
			print(19, false, out, appVersion != null ? appVersion.getReleaseName() : "");
			out.print(' ');

			long ts = (appVersion != null ? appVersion.getTimestamp(): 0L);
			if (formatDates)
				print(16, false, out, dateFormat.format(ts));
			else print(16, false, out, ts);
			out.print(' ');

			print(7, true, out, appVersion != null ? appVersion.getNumClasses() : 0);
			out.print(' ');
			print(7, true, out, appVersion != null ? appVersion.getCodeSize() : 0);

			for (int j = 0; j < TUPLE_SIZE; ++j) {
				out.print(' ');
				print(7, true, out, version.get(j));
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
			addSwitch("-noTabs", "delimit columns with groups of spaces for better alignment");
		}

		public void handleOption(String option, String optionalExtraPart) {
			if  (option.equals("-formatDates")) 
				setFormatDates(true);
			else 
			if (option.equals("-noTabs")) setNoTabs(true);
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
