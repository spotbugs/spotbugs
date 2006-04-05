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

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.AppVersion;
import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.VersionInsensitiveBugComparator;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.model.MovedClassMap;

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
	private static final String USAGE = "Usage: " 
			+ Update.class.getName()
			+ " [options]  data1File data2File data3File ... ";

	private static HashMap<BugInstance, BugInstance> mapFromNewToOldBug = new HashMap<BugInstance, BugInstance>();

	private static HashSet<BugInstance> matchedOldBugs = new HashSet<BugInstance>();
	static 	boolean noPackageMoves = false;

	static class UpdateCommandLine extends CommandLine {
		boolean overrideRevisionNames = false;
	
		 String outputFilename;
		UpdateCommandLine() {
			addSwitch("-overrideRevisionNames", 
			"override revision names for each version with names computed filenames");
			addSwitch("-noPackageMoves", 
			"if a class seems to have moved from one package to another, treat warnings in that class as two seperate warnings");
			
			addSwitch("-precisePriorityMatch", 
			"only consider two warnings to be the same if their priorities match exactly");
			addOption("-output", "output file",
					"explicit filename for merged results (standard out used if not specified)");
	
		}

		@Override
		protected void handleOption(String option, String optionExtraPart)
				throws IOException {
			if (option.equals("-overrideRevisionNames")) {
				if (optionExtraPart.length() == 0)
					overrideRevisionNames = true;
				else
					overrideRevisionNames = TigerSubstitutes.parseBoolean(optionExtraPart);
			}
			else if (option.equals("-noPackageMoves")) {
				if (optionExtraPart.length() == 0)
					noPackageMoves = true;
				else
					noPackageMoves = TigerSubstitutes.parseBoolean(optionExtraPart);
			}
			else 	if (option.equals("-precisePriorityMatch")) 
				versionInsensitiveBugComparator.setComparePriorities(true);
				
			else throw new IllegalArgumentException("no option " + option);

		}

		@Override
		protected void handleOptionWithArgument(String option, String argument)
				throws IOException {
			if (option.equals("-output"))
				outputFilename = argument;
			else
				throw new IllegalArgumentException("Can't handle option "
						+ option);

		}

	}
	static VersionInsensitiveBugComparator versionInsensitiveBugComparator = new VersionInsensitiveBugComparator();

	
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
		//why not do: AppVersion origCollectionVersion = origCollection.getCurrentAppVersion();
		AppVersion origCollectionVersion = new AppVersion(lastSequence);
		origCollectionVersion.setTimestamp(origCollection
				.getCurrentAppVersion().getTimestamp());
		origCollectionVersion.setReleaseName(origCollection
				.getCurrentAppVersion().getReleaseName());
		origCollectionVersion.setNumClasses(origCollection.getProjectStats().getNumClasses());
		origCollectionVersion.setCodeSize(origCollection.getProjectStats().getCodeSize());
		
		resultCollection.addAppVersion(origCollectionVersion);

		// We assign a sequence number to the new collection as one greater than
		// the original collection.
		long currentSequence = origCollection.getSequenceNumber() + 1;
		resultCollection.setSequenceNumber(currentSequence);

		matchBugs(SortedBugCollection.BugInstanceComparator.instance,
				origCollection, newCollection);
		matchBugs(versionInsensitiveBugComparator, origCollection,
				newCollection);
		{
		VersionInsensitiveBugComparator fuzzyBugPatternMatcher = new VersionInsensitiveBugComparator();
		fuzzyBugPatternMatcher.setExactBugPatternMatch(false);
		matchBugs(fuzzyBugPatternMatcher, origCollection,
			newCollection);
		}
		if (!noPackageMoves) {
			VersionInsensitiveBugComparator movedBugComparator = new VersionInsensitiveBugComparator();
			movedBugComparator.setClassNameRewriter(new MovedClassMap(origCollection,newCollection).execute());
			matchBugs(movedBugComparator, origCollection,
					newCollection);
			movedBugComparator.setExactBugPatternMatch(false);
			matchBugs(movedBugComparator, origCollection,
				newCollection);
		}

		// matchBugs(new SloppyBugComparator(), origCollection, newCollection);

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
		if (false && verbose) {
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

	public static String [] getFilePathParts(String filePath) {
		return filePath.split(File.separator);
	}
	public static void main(String[] args) throws IOException,
			DocumentException {

		DetectorFactoryCollection.instance();
		UpdateCommandLine commandLine = new UpdateCommandLine();
		int argCount = commandLine.parse(args, 2, Integer.MAX_VALUE, USAGE);

		verbose = commandLine.outputFilename != null;
		String[] firstPathParts = getFilePathParts(args[argCount]);
		int commonPrefix = firstPathParts.length;
		for(int i = argCount+1; i <= (args.length - 1); i++) {

			commonPrefix = Math.min(commonPrefix, 
					lengthCommonPrefix(
							firstPathParts, 
							getFilePathParts(args[i])));
		}
		
		
		String origFilename = args[argCount++];
		Project project = new Project();
		BugCollection origCollection;
		origCollection = new SortedBugCollection(
				SortedBugCollection.MultiversionBugInstanceComparator.instance);
		if (verbose)
			System.out.println("Starting with " + origFilename);

		origCollection.readXML(origFilename, project);

		if (commandLine.overrideRevisionNames || origCollection.getReleaseName() == null || origCollection.getReleaseName().length() == 0)
			origCollection.setReleaseName(firstPathParts[commonPrefix]);
		for (BugInstance bug : origCollection.getCollection())
			if (bug.getLastVersion() >= 0
					&& bug.getFirstVersion() > bug.getLastVersion())
				throw new IllegalStateException("Illegal Version range: "
						+ bug.getFirstVersion() + ".." + bug.getLastVersion());


		while (argCount <= (args.length - 1)) {

			BugCollection newCollection = new SortedBugCollection(
					SortedBugCollection.MultiversionBugInstanceComparator.instance);

			String newFilename = args[argCount++];
			if (verbose)
				System.out.println("Merging " + newFilename);
			project = new Project();
			newCollection.readXML(newFilename, project);


			if (commandLine.overrideRevisionNames || newCollection.getReleaseName() == null || newCollection.getReleaseName().length() == 0) 
					newCollection.setReleaseName(getFilePathParts(newFilename)[commonPrefix]);

			origCollection = mergeCollections(origCollection, newCollection);

		}

		if (commandLine.outputFilename != null) 
			origCollection.writeXML(commandLine.outputFilename, project);
		else
			origCollection.writeXML(System.out, project);

	}


	private static int lengthCommonPrefix(String[] string, String[] string2) {
		int maxLength = Math.min(string.length, string2.length);
		for (int result = 0; result < maxLength; result++)
			if (!string[result].equals(string2[result])) return result;
		return maxLength;
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
				q.add(bug);
			}
		for (BugInstance bug : newCollection.getCollection()) if (!mapFromNewToOldBug.containsKey(bug)) {
			newBugs++;
			LinkedList<BugInstance> q = set.get(bug);
			if (q != null && !q.isEmpty()) {
				matchedBugs++;
				BugInstance matchedBug = q.removeFirst();
				mapFromNewToOldBug.put(bug, matchedBug);
				matchedOldBugs.add(matchedBug);
			}
		}
		if (false && verbose) System.out.println("matched " + matchedBugs + " of " + oldBugs +"o/" + newBugs  + "n bugs using " + bugInstanceComparator.getClass().getName()); 
	}

}
