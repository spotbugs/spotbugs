/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 William Pugh
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SloppyBugComparator;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.VersionInsensitiveBugComparator;
import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Java main application to compute update a historical bug collection with
 * results from another build/analysis.
 * 
 * @author William Pugh
 */

public class Update {

	/**
	 * 
	 */
	private static final String USAGE = "Usage: " + Update.class.getName()
			+ " [options] <historyData> <newData> [<mergedData>] or "
			+ Update.class.getName()
			+ " [options] -output filename data1File data2File data3File ... ";

	private static HashMap<BugInstance, BugInstance> mapFromNewToOldBug = new HashMap<BugInstance, BugInstance>();

	private static HashSet<BugInstance> matchedOldBugs = new HashSet<BugInstance>();

	static class UpdateCommandLine extends CommandLine {
		String revisionName;

		long revisionTimestamp = 0L;

		UpdateCommandLine() {
			addOption("-name", "name", "provide name for new results");
			addOption("-output", "output file",
					"explicit filename for merged results");
			addOption("-timestamp", "when", "timestamp for new results");

		}

		@Override
		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			throw new IllegalArgumentException("no option " + option);

		}

		@Override
		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			if (option.equals("-name"))
				revisionName = argument;
			else if (option.equals("-output"))
				outputFilename = argument;
			else if (option.equals("-timestamp"))
				revisionTimestamp = Date.parse(argument);
			else
				throw new IllegalArgumentException("Can't handle option "
						+ option);

		}

	}

	public static BugCollection mergeCollections(BugCollection origCollection,
			BugCollection newCollection) {
		
		mapFromNewToOldBug.clear();

		matchedOldBugs.clear();
		BugCollection resultCollection = newCollection
				.createEmptyCollectionWithMetadata();
		// Previous sequence number
		long lastSequence = origCollection.getSequenceNumber();
		// The AppVersion history is retained from the orig collection,
		// adding an entry for the sequence/timestamp of the current state
		// of the orig collection.
		resultCollection.clearAppVersions();
		for (Iterator<AppVersion> i = origCollection.appVersionIterator(); i
				.hasNext();) {
			AppVersion appVersion = i.next();
			resultCollection.addAppVersion((AppVersion) appVersion.clone());
		}
		AppVersion origCollectionVersion = new AppVersion(lastSequence);
		origCollectionVersion.setTimestamp(origCollection
				.getCurrentAppVersion().getSequenceNumber());
		origCollectionVersion.setReleaseName(origCollection
				.getCurrentAppVersion().getReleaseName());
		resultCollection.addAppVersion(origCollectionVersion);

		// We assign a sequence number to the new collection as one greater than
		// the
		// original collection.
		long currentSequence = origCollection.getSequenceNumber() + 1;
		resultCollection.setSequenceNumber(currentSequence);

		matchBugs(new SortedBugCollection.BugInstanceComparator(),
				origCollection, newCollection);
		matchBugs(VersionInsensitiveBugComparator.instance(), origCollection,
				newCollection);
		matchBugs(new SloppyBugComparator(), origCollection, newCollection);

		int oldBugs = 0;
		int newlyDeadBugs = 0;
		int persistantBugs = 0;
		int addedBugs = 0;
		int addedInNewCode = 0;
		int deadBugInDeadCode = 0;

		// Copy unmatched bugs
		for (BugInstance bug : origCollection.getCollection())
			if (!matchedOldBugs.contains(bug)) {
				if (bug.getLastVersion() == -1)
					newlyDeadBugs++;
				else
					oldBugs++;
				BugInstance newBug = (BugInstance) bug.clone();

				if (newBug.getLastVersion() == -1) {
					newBug.setLastVersion(lastSequence);
					ClassAnnotation classBugFoundIn = bug.getPrimaryClass();
					String className = classBugFoundIn.getClassName();
					if (newCollection.getProjectStats()
							.getClassStats(className) != null)
						newBug.setRemovedByChangeOfPersistingClass(true);
					else
						deadBugInDeadCode++;
				}

				if (newBug.getFirstVersion() > newBug.getLastVersion())
					throw new IllegalStateException("Illegal Version range: "
							+ newBug.getFirstVersion() + ".."
							+ newBug.getLastVersion());
				resultCollection.add(newBug, false);
			}
		// Copy matched bugs
		for (BugInstance bug : newCollection.getCollection()) {
			BugInstance newBug = (BugInstance) bug.clone();
			if (mapFromNewToOldBug.containsKey(bug)) {
				BugInstance origWarning = mapFromNewToOldBug.get(bug);
				assert origWarning.getLastVersion() == -1;

				newBug.setUniqueId(origWarning.getUniqueId());
				copyBugHistory(origWarning, newBug);
				String annotation = newBug.getAnnotationText();
				if (annotation.length() == 0)
					newBug.setAnnotationText(origWarning.getAnnotationText());

				persistantBugs++;
			} else {
				newBug.setFirstVersion(lastSequence + 1);
				addedBugs++;

				ClassAnnotation classBugFoundIn = bug.getPrimaryClass();

				String className = classBugFoundIn.getClassName();
				if (origCollection.getProjectStats().getClassStats(className) != null) {
					newBug.setIntroducedByChangeOfExistingClass(true);
					// System.out.println("added bug to existing code " +
					// newBug.getUniqueId() + " : " + newBug.getAbbrev() + " in
					// " + classBugFoundIn);
				} else
					addedInNewCode++;
			}
			assert newBug.getLastVersion() == -1;
			if (newBug.getLastVersion() != -1)
				throw new IllegalStateException("Illegal Version range: "
						+ newBug.getFirstVersion() + ".."
						+ newBug.getLastVersion());
			int oldSize = resultCollection.getCollection().size();
			resultCollection.add(newBug, false);
			int newSize = resultCollection.getCollection().size();
			if (newSize != oldSize + 1) {
				System.out.println("Failed to add bug #" + newBug.getUniqueId()
						+ " : " + newBug.getMessage());
			}
		}
		if (verbose) {
			System.out.println(origCollection.getCollection().size()
					+ " orig bugs, " + newCollection.getCollection().size()
					+ " new bugs");
			System.out.println("Bugs: " + oldBugs + " old, "
					+ deadBugInDeadCode + " in removed code, "
					+ (newlyDeadBugs - deadBugInDeadCode) + " died, "
					+ persistantBugs + " persist, " + addedInNewCode
					+ " in new code, " + (addedBugs - addedInNewCode)
					+ " added");
		}
		return resultCollection;

	}

	public static boolean verbose = false;

	public static String outputFilename;

	public static void main(String[] args) throws IOException,
			DocumentException {

		DetectorFactoryCollection.instance();
		UpdateCommandLine commandLine = new UpdateCommandLine();
		int argCount = commandLine.parse(args, 2, Integer.MAX_VALUE, USAGE);

		int lastInputfile = args.length - 1;
		if (outputFilename == null && args.length - argCount == 3) {
			outputFilename = args[args.length - 1];
			lastInputfile--;
		}
		verbose = outputFilename != null;
		String origFileName = args[argCount++];

		BugCollection origCollection;
		origCollection = new SortedBugCollection(
				SortedBugCollection.MultiversionBugInstanceComparator.instance);
		BugCollection oCollection = origCollection;
		origCollection.readXML(origFileName, new Project());

		for (BugInstance bug : origCollection.getCollection())
			if (bug.getLastVersion() >= 0
					&& bug.getFirstVersion() > bug.getLastVersion())
				throw new IllegalStateException("Illegal Version range: "
						+ bug.getFirstVersion() + ".." + bug.getLastVersion());
		Project currentProject = new Project();

		while (argCount <= lastInputfile) {

			BugCollection newCollection = new SortedBugCollection(
					SortedBugCollection.MultiversionBugInstanceComparator.instance);
			BugCollection nCollection = newCollection;

			String newFilename = args[argCount++];
			if (verbose)
				System.out.println("Merging " + newFilename);
			newCollection.readXML(newFilename, currentProject);

			if (commandLine.revisionName != null)
				newCollection.setReleaseName(commandLine.revisionName);
			if (commandLine.revisionTimestamp != 0)
				newCollection.setTimestamp(commandLine.revisionTimestamp);
			origCollection = mergeCollections(origCollection, newCollection);

		}

		if (outputFilename != null) 
			origCollection.writeXML(outputFilename, currentProject);
		else
			origCollection.writeXML(System.out, currentProject);

	}

	private static void copyBugHistory(BugInstance src, BugInstance dest) {

		dest.setFirstVersion(src.getFirstVersion());
		dest.setLastVersion(src.getLastVersion());
		dest.setIntroducedByChangeOfExistingClass(src
				.isIntroducedByChangeOfExistingClass());
		dest.setRemovedByChangeOfPersistingClass(src
				.isRemovedByChangeOfPersistingClass());
	}

	private static void matchBugs(
			Comparator<BugInstance> bugInstanceComparator,
			BugCollection origCollection, BugCollection newCollection) {
		
		TreeMap<BugInstance, LinkedList<BugInstance>> set = new TreeMap<BugInstance, LinkedList<BugInstance>>(
				bugInstanceComparator);
		int oldBugs = 0;
		int newBugs = 0;
		int matchedBugs = 0;
		for (BugInstance bug : origCollection.getCollection())
			if (bug.getLastVersion() == -1 && !matchedOldBugs.contains(bug)) {
				oldBugs++;
				LinkedList<BugInstance> q = set.get(bug);
				if (q == null) {
					q = new LinkedList<BugInstance>();
					set.put(bug, q);
				}
				q.offer(bug);
			}
		for (BugInstance bug : newCollection.getCollection()) if (!mapFromNewToOldBug.containsKey(bug)) {
			newBugs++;
			LinkedList<BugInstance> q = set.get(bug);
			if (q != null && !q.isEmpty()) {
				matchedBugs++;
				BugInstance matchedBug = q.remove();
				mapFromNewToOldBug.put(bug, matchedBug);
				matchedOldBugs.add(matchedBug);
			}
		}
		if (verbose) System.out.println("matched " + matchedBugs + " of " + oldBugs +"o/" + newBugs  + "n bugs using " + bugInstanceComparator.getClass().getName()); 
	}

}
