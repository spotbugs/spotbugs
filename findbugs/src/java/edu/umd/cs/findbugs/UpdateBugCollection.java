/*
 * FindBugs - Find bugs in Java programs
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.config.CommandLine;

/**
 * Update a BugCollection with new BugInstances, preserving the history
 * of all existing BugInstances.  The update is performed by
 * constructing an entirely new and disjoint BugCollection;
 * the input BugCollections are not modified.
 * 
 * @see edu.umd.cs.findbugs.BugCollection
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class UpdateBugCollection {
	private BugCollection origCollection, newCollection;
	private BugCollection resultCollection;
	private Set<String> updatedClassNameSet;
	private Comparator<BugInstance> comparator;

	/**
	 * Constructor.
	 * 
	 * @param origCollection the BugCollection containing all previous
	 *                       BugInstances over the lifetime of the project
	 * @param newCollection  a BugCollection containing new BugInstances from the
	 *                       most recent analysis
	 */
 	public UpdateBugCollection(BugCollection origCollection, BugCollection newCollection) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
		this.comparator = VersionInsensitiveBugComparator.instance();
	}

 	/**
 	 * Get the comparator used to match warnings from different code versions.
 	 * 
 	 * @return the comparator
 	 */
	public Comparator<BugInstance> getComparator() {
		return comparator;
	}
	
	/**
	 * @param comparator The comparator to set.
	 */
	public void setComparator(Comparator<BugInstance> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Specify the Set of classnames for all classes that were analyzed in
	 * the most recent analysis.
	 * 
	 * @param updatedClassNameSet the Set of classnames of classes analyzed
	 */
	public void setUpdatedClassNameSet(Set<String> updatedClassNameSet) {
		this.updatedClassNameSet = updatedClassNameSet;
	}

	/**
	 * Update the original comprehensive BugCollection to contain the new
	 * results from the most recent analysis.
	 * 
	 * @return this object
	 */
	public UpdateBugCollection execute() {
		// Result collection is initialized using new collection's metadata
		resultCollection = newCollection.createEmptyCollectionWithMetadata();
		
		// The AppVersion history is retained from the orig collection,
		// adding an entry for the sequence/timestamp of the current state
		// of the orig collection.
		resultCollection.clearAppVersions();
		for (Iterator<AppVersion> i = origCollection.appVersionIterator(); i.hasNext();) {
			AppVersion appVersion = i.next();
			resultCollection.addAppVersion((AppVersion) appVersion.clone());
		}
		AppVersion origCollectionVersion = new AppVersion(origCollection.getSequenceNumber());
		origCollectionVersion.setTimestamp(origCollection.getTimestamp());
		origCollectionVersion.setReleaseName(origCollection.getReleaseName());
		resultCollection.addAppVersion(origCollectionVersion);

		// Get Sets with exact contents of orig and new collections
		SortedSet<BugInstance> origSetExact = collectionToExactSet(origCollection);
		SortedSet<BugInstance> newSetExact = collectionToExactSet(newCollection);
		
		// Get "fuzzy" matching sets of orig and new collections,
		// for querying "same" warnings accross the two versions.
		SortedSet<BugInstance> origSetFuzzy = collectionToFuzzySet(origCollection);
		SortedSet<BugInstance> newSetFuzzy = collectionToFuzzySet(newCollection);

		// Previous sequence number
		long lastSequence = origCollection.getSequenceNumber();
		
		// We assign a sequence number to the new collection as one greater than the
		// original collection.
		long currentSequence = origCollection.getSequenceNumber() + 1;
		resultCollection.setSequenceNumber(currentSequence);
		
		// A SequenceInterval representing just the original collection's sequence number.
		SequenceInterval lastVersion = new SequenceInterval(lastSequence, lastSequence);

		// Handle removed and retained warnings.
		// These must be added using the SAME UNIQUE IDs as were present in the original bug collection.
		for (BugInstance origWarning : origSetExact) {

			// Special case: if the original collection doesn't contain history,
			// reset the active time to just the original sequence number.
			// This allows us to handle old bug collections which contain timestamps
			// or other junk in their "active" attributes.
			if (!origCollectionContainsHistory()) {
				SequenceIntervalCollection activeLastTimeOnly = new SequenceIntervalCollection();
				activeLastTimeOnly.add(lastVersion);
				origWarning = (BugInstance) origWarning.clone(); // make a fresh copy
				origWarning.setActiveIntervalCollection(activeLastTimeOnly);
			}

			// See if the warning is in the new collection, too
			BugInstance matchingNewWarning = findMatching(newSetFuzzy, origWarning);

			BugInstance warningToAdd;
			if (matchingNewWarning != null) {
				// Warning is retained.
				//
				// From original, keep:
				// - unique id
				// - BugProperty list (classifications, &c.)
				// - annotation text
				//
				// In all other respects, the warning is a clone of the matching new warning.

				warningToAdd = (BugInstance) matchingNewWarning.clone();
				warningToAdd.setUniqueId(origWarning.getUniqueId());
				copyBugProperties(origWarning, warningToAdd);
				warningToAdd.setAnnotationText(origWarning.getAnnotationText());
				
				// Update the warning as being active at the current sequence number.
				SequenceIntervalCollection whenActive = origWarning.getActiveIntervalCollection();
				updateActiveIntervalCollection(lastSequence, currentSequence, whenActive);
				warningToAdd.setActiveIntervalCollection(whenActive);
			} else {
				// Warning is removed.
				// The old warning remains, but its metadata (classifications,
				// when it was active) remains exactly the same.
				warningToAdd = (BugInstance) origWarning.clone();
			}
			
			resultCollection.add(warningToAdd, false);
		}
		
		// Handle new warnings.
		// These will be assigned new unique ids guaranteed not to conflict
		// with any existing unique ids.
		for (BugInstance newWarning : newSetExact) {
			BugInstance matchingOrigWarning = findMatching(origSetFuzzy, newWarning);
			
			if (matchingOrigWarning == null) {
				// Added warning.
				// Mark active at current time only, and add to result collection directly
				
				SequenceIntervalCollection activeNow = new SequenceIntervalCollection();
				activeNow.add(new SequenceInterval(currentSequence, currentSequence));
				
				BugInstance warningToAdd = (BugInstance) newWarning.clone();
				warningToAdd.setActiveIntervalCollection(activeNow);
				
				resultCollection.add(warningToAdd, false);
			}
		}
		
		return this;
	}
	
	/**
	 * Get the result collection.
	 * 
	 * @return the result collection.
	 */
	public BugCollection getResultCollection() {
		return resultCollection;
	}
	
	/**
	 * Return whether or not the original collection actually contains the results
	 * of multiple application versions.
	 * 
	 * @return true if the original collection has the results of multiple application
	 *         versions, false if not
	 */
	private boolean origCollectionContainsHistory() {
		// We know that if the original collection contains the results of multiple
		// versions it will contain at least one AppVersion.
		return origCollection.appVersionIterator().hasNext();
	}

	/**
	 * Update the given TimestampIntervalCollection to mark a bug instance
	 * as active at the current timestamp.  If the bug instance was active
	 * at the time of the last analysis, then the interval containing the
	 * timestamp of the last analysis is extended to include the current
	 * timestamp.  (I.e., we assume continuity between the last timestamp and
	 * the current one.)
	 * 
	 * @param lastSequence    timestamp of the last analysis
	 * @param currentSequence timestamp of the current analysis
	 * @param activeCollection TimestampIntervalCollection to update
	 */
	private void updateActiveIntervalCollection(
			long lastSequence, long currentSequence, SequenceIntervalCollection activeCollection) {
		
		// If the original bug instance was active during the most recent
		// analysis, then extend the latest active interval to include
		// the current timestamp.
		boolean added = false;
		if (activeCollection.contains(lastSequence)) {
			int lastActiveIndex = activeCollection.findInterval(lastSequence);
			if (lastActiveIndex < 0)
				throw new IllegalStateException();
			SequenceInterval lastActiveInterval = activeCollection.get(lastActiveIndex);
			
			// Current timestamp should be later than any timestamp in
			// the existing interval collection.
			if (currentSequence > lastActiveInterval.getEnd()) {
				SequenceInterval updatedActiveInterval =
					new SequenceInterval(lastActiveInterval.getBegin(), currentSequence);
				
				activeCollection.remove(lastActiveIndex);
				activeCollection.add(updatedActiveInterval);
				
				added = true;
			}
		}

		// The original bug instance was not active during the most recent
		// analysis.  However, it is active now, so just add the current timestamp.
		if (!added) {
			activeCollection.add(new SequenceInterval(currentSequence, currentSequence));
		}
	}

	private void copyBugProperties(BugInstance src, BugInstance dest) {
		for (Iterator<BugProperty> propIter = src.propertyIterator(); propIter.hasNext();) {
			BugProperty prop = propIter.next();
			dest.setProperty(prop.getName(), prop.getValue());
		}
	}

	private SortedSet<BugInstance> collectionToExactSet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = new TreeSet<BugInstance>(new SortedBugCollection.BugInstanceComparator());
		set.addAll(bugCollection.getCollection());
		return set;
	}
	
	private SortedSet<BugInstance> collectionToFuzzySet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = new TreeSet<BugInstance>(getComparator());
		set.addAll(bugCollection.getCollection());
		return set;
	}

	private BugInstance findMatching(
			SortedSet<BugInstance> bugInstanceSet, BugInstance bugInstance) {
		SortedSet<BugInstance> tailSet = bugInstanceSet.tailSet(bugInstance);
		if (tailSet.isEmpty())
			return null;
		BugInstance correspondingBugInstance = tailSet.first();
		return getComparator().compare(bugInstance, correspondingBugInstance) == 0
				? correspondingBugInstance
				: null;
	}
	
	private static final int VERSION_INSENSITIVE_COMPARATOR = 1;
	private static final int FUZZY_COMPARATOR = 2;
	private static final int SLOPPY_COMPARATOR = 3;
	
	private static class UpdateBugCollectionCommandLine extends CommandLine {
		private int comparatorType = VERSION_INSENSITIVE_COMPARATOR;
		
		public UpdateBugCollectionCommandLine() {
			addSwitch("-fuzzy", "use FuzzyBugComparator");
			addSwitch("-sloppy", "use SloppyBugComparator");
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-fuzzy")) {
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
		
		/**
		 * @return Returns the comparatorType.
		 */
		public int getComparatorType() {
			return comparatorType;
		}
	}
	
	public static void main(String[] args) throws IOException, DocumentException {
		
		UpdateBugCollectionCommandLine commandLine = new UpdateBugCollectionCommandLine();
		int argCount = commandLine.parse(args);

		if (args.length - argCount != 3) {
			System.err.println("Usage: " + UpdateBugCollection.class.getName() + " [options] " +
					" <bug collection to update> <new bug colllection> <output bug collection>");
			System.err.println("Options:");
			commandLine.printUsage(System.err);
			System.exit(1);
		}
		
		String origFileName = args[argCount++];
		String newFileName = args[argCount++];
		String outputFileName = args[argCount++];
		
		SortedBugCollection origCollection = new SortedBugCollection();
		origCollection.readXML(origFileName, new Project());
		
		Project currentProject = new Project();
		SortedBugCollection newCollection = new SortedBugCollection();
		newCollection.readXML(newFileName, currentProject);
		
		UpdateBugCollection updater = new UpdateBugCollection(origCollection, newCollection);

		Comparator<BugInstance> comparator;
		switch (commandLine.getComparatorType()) {
		case VERSION_INSENSITIVE_COMPARATOR:
			comparator = VersionInsensitiveBugComparator.instance();
			break;
		case FUZZY_COMPARATOR:
			FuzzyBugComparator fuzzy = new FuzzyBugComparator();
			fuzzy.registerBugCollection(origCollection);
			fuzzy.registerBugCollection(newCollection);
			comparator = fuzzy;
			break;
		case SLOPPY_COMPARATOR:
			comparator = new SloppyBugComparator();
			break;
		default:
			throw new IllegalStateException();
		}
		updater.setComparator(comparator);
		
		updater.execute();
		
		updater.getResultCollection().writeXML(outputFileName, currentProject);
	}
}
