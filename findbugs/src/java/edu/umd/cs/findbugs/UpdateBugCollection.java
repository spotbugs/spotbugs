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
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.DocumentException;

/**
 * Update a BugCollection with new BugInstances, preserving the history
 * of all existing BugInstances.  Note that both BugCollections
 * will be destructively modified, and afterwards the "original" collection will
 * be the new "up to date" collection, containing all information from
 * the old and new collections.
 * 
 * @see edu.umd.cs.findbugs.BugCollection
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class UpdateBugCollection {
	private BugCollection collectionToUpdate, newCollection;
	private Set<String> updatedClassNameSet;

	/**
	 * Constructor.
	 * 
	 * @param collectionToUpdate the BugCollection containing all previous
	 *                           BugInstances over the lifetime of the project;
	 *                           will be modified
	 * @param newCollection      a BugCollection containing new BugInstances from the
	 *                           most recent analysis
	 */
 	public UpdateBugCollection(BugCollection collectionToUpdate, BugCollection newCollection) {
		this.collectionToUpdate = collectionToUpdate;
		this.newCollection = newCollection;
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
	 */
	public void execute() {
		SortedSet<BugInstance> origSet = collectionToSet(collectionToUpdate);
		SortedSet<BugInstance> newSet = collectionToSet(newCollection);
		
		long lastTimestamp = collectionToUpdate.getTimestamp();
		
		long currentTimestamp = newCollection.getTimestamp();
		if (currentTimestamp <= lastTimestamp) {
			// Looks like we have unsynchronized clocks.
			// Make the current timestamp greater than the last timestamp.
			currentTimestamp = lastTimestamp + 11;
		}
		
		// Handle warnings which are in the original bug collection
		// (and possibly in the new one).
		for (Iterator<BugInstance> i = origSet.iterator(); i.hasNext();) {
			BugInstance origInstance = i.next();
			
			BugInstance correspondingNewInstance = findMatching(newSet, origInstance);

			TimestampIntervalCollection updatedActiveCollection = origInstance.getActiveIntervalCollection();
			updateActiveIntervalCollection(
					lastTimestamp, currentTimestamp, updatedActiveCollection);
			
			if (correspondingNewInstance != null) {
				// Combine the new and orig instances
				//
				// From original, keep:
				// - unique id
				// - BugProperty list (classifications, &c.)
				// - annotation text
				//
				// If the original instance was active in the most recent update
				// (timestamp), extend the interval containing that timestamp to
				// include the current timestamp.
				//
				// All other information taken from new instance.
				
				// This operation is implemented by copying the required information
				// into the new instance and replacing the original instance
				// with the new instance.

				correspondingNewInstance.setUniqueId(origInstance.getUniqueId());
				copyBugProperties(origInstance, correspondingNewInstance);
				correspondingNewInstance.setAnnotationText(origInstance.getAnnotationText());
				
				correspondingNewInstance.setActiveIntervalCollection(updatedActiveCollection);
				
				collectionToUpdate.remove(origInstance);
				collectionToUpdate.add(correspondingNewInstance);
			} else {
				// BugInstance exists in original set, but not in new set.
				
				// If the orig bug instance is not part of the set of updated classes,
				// then we mark it active anyway, assuming that it still exists,
				// but the analysis didn't look at that class.  Otherwise, we leave
				// the active collection as-is, meaning that the instance won't
				// be considered active any more.
				
				if (updatedClassNameSet != null) {
					ClassAnnotation primaryClass = origInstance.getPrimaryClass();
					if (primaryClass != null && updatedClassNameSet.contains(primaryClass.getClassName())) {
						origInstance.setActiveIntervalCollection(updatedActiveCollection);
					}
				}
			}
		}
		
		// Handle warnings that are only in the new collection.
		// This is easy: just copy them into the orig collection.
		for (Iterator<BugInstance> i = newSet.iterator(); i.hasNext();) {
			BugInstance newInstance = i.next();
			
			BugInstance correspondingOrigInstance = findMatching(origSet, newInstance);
			if (correspondingOrigInstance == null) {
				// Note that the BugCollection will assign a new unique id
				// to the new instance, so that it won't conflict with
				// existing uids.
				collectionToUpdate.add(newInstance);
			}
		}
		
		// Update the timestamp
		collectionToUpdate.setTimestamp(currentTimestamp);
	}

	/**
	 * Update the given TimestampIntervalCollection to mark a bug instance
	 * as active at the current timestamp.  If the bug instance was active
	 * at the time of the last analysis, then the interval containing the
	 * timestamp of the last analysis is extended to include the current
	 * timestamp.  (I.e., we assume continuity between the last timestamp and
	 * the current one.)
	 * 
	 * @param lastTimestamp    timestamp of the last analysis
	 * @param currentTimestamp timestamp of the current analysis
	 * @param activeCollection TimestampIntervalCollection to update
	 */
	private void updateActiveIntervalCollection(
			long lastTimestamp, long currentTimestamp, TimestampIntervalCollection activeCollection) {
		
		// If the original bug instance was active during the most recent
		// analysis, then extend the latest active interval to include
		// the current timestamp.
		boolean added = false;
		if (activeCollection.contains(lastTimestamp)) {
			int lastActiveIndex = activeCollection.findInterval(lastTimestamp);
			if (lastActiveIndex < 0)
				throw new IllegalStateException();
			TimestampInterval lastActiveInterval = activeCollection.get(lastActiveIndex);
			
			// Current timestamp should be later than any timestamp in
			// the existing interval collection.
			if (currentTimestamp > lastActiveInterval.getEnd()) {
				TimestampInterval updatedActiveInterval =
					new TimestampInterval(lastActiveInterval.getBegin(), currentTimestamp);
				
				activeCollection.remove(lastActiveIndex);
				activeCollection.add(updatedActiveInterval);
				
				added = true;
			}
		}

		// The original bug instance was not active during the most recent
		// analysis.  However, it is active now, so just add the current timestamp.
		if (!added) {
			activeCollection.add(new TimestampInterval(currentTimestamp, currentTimestamp));
		}
	}

	private void copyBugProperties(BugInstance src, BugInstance dest) {
		for (Iterator<BugProperty> propIter = src.propertyIterator(); propIter.hasNext();) {
			BugProperty prop = propIter.next();
			dest.setProperty(prop.getName(), prop.getValue());
		}
	}

	private SortedSet<BugInstance> collectionToSet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = new TreeSet<BugInstance>(VersionInsensitiveBugComparator.instance());
		set.addAll(bugCollection.getCollection());
		return set;
	}

	private BugInstance findMatching(
			SortedSet<BugInstance> bugInstanceSet, BugInstance bugInstance) {
		SortedSet<BugInstance> tailSet = bugInstanceSet.tailSet(bugInstance);
		if (tailSet.isEmpty())
			return null;
		BugInstance correspondingBugInstance = tailSet.first();
		return VersionInsensitiveBugComparator.instance().compare(bugInstance, correspondingBugInstance) == 0
				? correspondingBugInstance
				: null;
	}
	
	public static void main(String[] args) throws IOException, DocumentException {
		if (args.length != 3) {
			System.err.println("Usage: " + UpdateBugCollection.class.getName() +
					" <bug collection to update> <new bug colllection> <output bug collection>");
			System.exit(1);
		}
		
		String origFileName = args[0];
		String newFileName = args[1];
		String outputFileName = args[2];
		
		SortedBugCollection origCollection = new SortedBugCollection();
		origCollection.readXML(origFileName, new Project());
		
		Project currentProject = new Project();
		SortedBugCollection newCollection = new SortedBugCollection();
		newCollection.readXML(newFileName, currentProject);
		
		UpdateBugCollection updater = new UpdateBugCollection(origCollection, newCollection);
		updater.execute();
		
		origCollection.writeXML(outputFileName, currentProject);
	}
}
