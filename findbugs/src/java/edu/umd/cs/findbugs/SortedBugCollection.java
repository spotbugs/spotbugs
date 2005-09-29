/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.model.ClassFeatureSet;

/**
 * An implementation of {@link BugCollection} that keeps the BugInstances
 * sorted by class (using the native comparison ordering of BugInstance's
 * compareTo() method as a tie-breaker).
 *
 * @see BugInstance
 * @author David Hovemeyer
 */
public class SortedBugCollection extends AbstractBugCollection {
	public static class BugInstanceComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			ClassAnnotation lca = lhs.getPrimaryClass();
			ClassAnnotation rca = rhs.getPrimaryClass();
			if (lca == null || rca == null)
				throw new IllegalStateException("null class annotation: " + lca + "," + rca);
			int cmp = lca.getClassName().compareTo(rca.getClassName());
			if (cmp != 0)
				return cmp;
			return lhs.compareTo(rhs);
		}
		public static final BugInstanceComparator instance = new BugInstanceComparator();
	}
	
	public static class MultiversionBugInstanceComparator extends BugInstanceComparator {
		public int compare(BugInstance lhs, BugInstance rhs) {
			int result = super.compare(lhs,rhs);
			if (result != 0) return result;
			long diff  = lhs.getFirstVersion() - rhs.getFirstVersion();
			if (diff == 0) 
				diff = lhs.getLastVersion() - rhs.getLastVersion();
			if (diff < 0) return -1;
			if (diff > 0) return 1;
			return 0;
		}
		public static final MultiversionBugInstanceComparator instance = new MultiversionBugInstanceComparator();
	}
	private Comparator<BugInstance> comparator;
	private TreeSet<BugInstance> bugSet;
	private List<AnalysisError> errorList;
	private TreeSet<String> missingClassSet;
	private String summaryHTML;
	private ProjectStats projectStats;
//	private Map<String, ClassHash> classHashMap;
	private Map<String, ClassFeatureSet> classFeatureSetMap;
	private List<AppVersion> appVersionList;

	private Map<String, BugInstance> uniqueIdToBugInstanceMap;
	private int generatedUniqueIdCount;
	
	/**
	 * Sequence number of the most-recently analyzed version
	 * of the code.
	 */
	private long sequence;
	/**
	 * Release name of the analyzed application.
	 */
	private String releaseName;
	/**
	 * Current analysis timestamp.
	 */
	private long timestamp;

	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SortedBugCollection() {
		this(new ProjectStats());
	}
	
	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SortedBugCollection(Comparator<BugInstance> comparator) {
		this(new ProjectStats(), comparator);
	}
	
	/**
	 * Constructor.
	 * Creates an empty object given an existing ProjectStats.
	 * 
	 * @param projectStats the ProjectStats
	 */
	public SortedBugCollection(ProjectStats projectStats) {
		this(projectStats, BugInstanceComparator.instance);
	}
	/**
	 * Constructor.
	 * Creates an empty object given an existing ProjectStats.
	 * 
	 * @param projectStats the ProjectStats
	 * @param comparator to use for sorting bug instances
	 */
	public SortedBugCollection(ProjectStats projectStats, Comparator<BugInstance> comparator) {
		this.projectStats = projectStats;
		this.comparator = comparator;
		bugSet = new TreeSet<BugInstance>(comparator);
		bugSet = new TreeSet<BugInstance>(comparator);
		errorList = new LinkedList<AnalysisError>();
		missingClassSet = new TreeSet<String>();
		summaryHTML = "";
//		classHashMap = new TreeMap<String, ClassHash>();
		classFeatureSetMap = new TreeMap<String, ClassFeatureSet>();
		uniqueIdToBugInstanceMap = new HashMap<String, BugInstance>();
		generatedUniqueIdCount = 0;
		sequence = 0L;
		appVersionList = new LinkedList<AppVersion>();
		releaseName = "";
		timestamp = -1L;
	}

	public boolean add(BugInstance bugInstance, boolean updateActiveTime) {
		registerUniqueId(bugInstance);

		if (updateActiveTime) {
			bugInstance.setFirstVersion(sequence);
		}
		
		return bugSet.add(bugInstance);
	}

	/**
	 * Create a unique id for a BugInstance if it doesn't already have one,
	 * or if the unique id it has conflicts with a BugInstance that is
	 * already in the collection.
	 * 
	 * @param bugInstance the BugInstance
	 */
	private void registerUniqueId(BugInstance bugInstance) {
		// If the BugInstance has no unique id, generate one.
		// If the BugInstance has a unique id which conflicts with
		// an existing BugInstance, then we also generate a new
		// unique id.
		String uniqueId = bugInstance.getUniqueId();
		if (uniqueId == null || uniqueIdToBugInstanceMap.get(uniqueId) != null) {
			uniqueId = assignUniqueId(bugInstance);
		}
		uniqueIdToBugInstanceMap.put(uniqueId, bugInstance);
	}

	/**
	 * Assign a unique id to given BugInstance.
	 * 
	 * @param bugInstance the BugInstance to be assigned a unique id
	 */
	private String assignUniqueId(BugInstance bugInstance) {
		String uniqueId;
		do {
			uniqueId = String.valueOf(generatedUniqueIdCount++);
		} while (uniqueIdToBugInstanceMap.get(uniqueId) != null);
		
		bugInstance.setUniqueId(uniqueId);
		return uniqueId;
	}

	public boolean remove(BugInstance bugInstance) {
		boolean present = bugSet.remove(bugInstance);
		if (present) {
			uniqueIdToBugInstanceMap.remove(bugInstance.getUniqueId());
		}
		return present;
	}

	public Iterator<BugInstance> iterator() {
		return bugSet.iterator();
	}

	public Collection<BugInstance> getCollection() {
		return bugSet;
	}

	@Override
	public void addError(String message, Throwable exception) {
		if (exception instanceof MissingClassException) {
			MissingClassException e = (MissingClassException) exception;
			addMissingClass(AbstractBugReporter.getMissingClassName(e.getClassNotFoundException()));
			return;
		}
		if (exception instanceof ClassNotFoundException) {
			ClassNotFoundException e = (ClassNotFoundException) exception;
			addMissingClass(AbstractBugReporter.getMissingClassName(e));
			return;
		}
		errorList.add(new AnalysisError(message, exception));
	}

	
	public void addError(AnalysisError error) {
		errorList.add(error);
	}
	
	public void addMissingClass(String className) {
		if (className.startsWith("[")) {
			assert false;
			return;
		}
		missingClassSet.add(className);
	}

	public Iterator<AnalysisError> errorIterator() {
		return errorList.iterator();
	}

	public Iterator<String> missingClassIterator() {
		return missingClassSet.iterator();
	}

	public boolean contains(BugInstance bugInstance) {
		return bugSet.contains(bugInstance);
	}

	public BugInstance getMatching(BugInstance bugInstance) {
		SortedSet<BugInstance> tailSet = bugSet.tailSet(bugInstance);
		if (tailSet.isEmpty())
			return null;
		BugInstance first = tailSet.first();
		return bugInstance.equals(first) ? first : null;
	}

	public String getSummaryHTML() throws IOException {
		if ( summaryHTML == null ) {
			try {
				StringWriter writer = new StringWriter();
				ProjectStats stats = getProjectStats();
				stats.transformSummaryToHTML(writer);
				summaryHTML = writer.toString();
			} catch (final TransformerException e) {
				IOException ioe = new IOException("Couldn't generate summary HTML");
				ioe.initCause(e);
				throw ioe;
			}
		}

		return summaryHTML;
	}

	public ProjectStats getProjectStats() {
		return projectStats;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#lookupFromUniqueId(java.lang.String)
	 */
	public BugInstance lookupFromUniqueId(String uniqueId) {
		return uniqueIdToBugInstanceMap.get(uniqueId);
	}

	public long getSequenceNumber() {
		return sequence;
	}

	public void setSequenceNumber(long sequence) {
		this.sequence = sequence;
	}
	
//	/**
//	 * Get ClassHash for given class.
//	 * 
//	 * @param className name of class
//	 * @return the ClassHash for that class, or null if we don't have a hash for that class
//	 */
//	public ClassHash getClassHash(String className) {
//		return classHashMap.get(className);
//	}
//
//	/* (non-Javadoc)
//	 * @see edu.umd.cs.findbugs.BugCollection#setClassHash(java.lang.String, edu.umd.cs.findbugs.ba.ClassHash)
//	 */
//	//@Override
//	public void setClassHash(String className, ClassHash classHash) {
//		//System.out.println("Put class hash: " + className + "->" + classHash);
//		classHashMap.put(className, classHash);
//	}
//	
//	/* (non-Javadoc)
//	 * @see edu.umd.cs.findbugs.BugCollection#classHashIterator()
//	 */
//	//@Override
//	public Iterator<ClassHash> classHashIterator() {
//		return classHashMap.values().iterator();
//	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#duplicate()
	 */
	@Override
	public SortedBugCollection duplicate() {
		SortedBugCollection dup = new SortedBugCollection((ProjectStats) projectStats.clone(), comparator);
		
		AbstractBugCollection.cloneAll(dup.bugSet, this.bugSet);
		dup.errorList.addAll(this.errorList);
		dup.missingClassSet.addAll(this.missingClassSet);
		dup.summaryHTML = this.summaryHTML;
//		dup.classHashMap.putAll(this.classHashMap);
		dup.classFeatureSetMap.putAll(this.classFeatureSetMap);
		for (BugInstance bugInstance : dup.bugSet) {
			uniqueIdToBugInstanceMap.put(bugInstance.getUniqueId(), bugInstance);
		}
		dup.generatedUniqueIdCount = this.generatedUniqueIdCount;
		dup.sequence = this.sequence;
		dup.timestamp = this.timestamp;
		dup.releaseName = this.releaseName;
		for (AppVersion appVersion : appVersionList) {
			dup.appVersionList.add((AppVersion) appVersion.clone());
		}
		
		return dup;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#clearBugInstances()
	 */
	@Override
	public void clearBugInstances() {
		bugSet.clear();
		uniqueIdToBugInstanceMap.clear();
		generatedUniqueIdCount = 0;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getReleaseName()
	 */
	@Override
	public String getReleaseName() {
		return releaseName;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#setReleaseName(java.lang.String)
	 */
	@Override
	public void setReleaseName(String releaseName) {
		this.releaseName = releaseName;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#appVersionIterator()
	 */
	@Override
	public Iterator<AppVersion> appVersionIterator() {
		return appVersionList.iterator();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#addAppVersion(edu.umd.cs.findbugs.AppVersion)
	 */
	@Override
	public void addAppVersion(AppVersion appVersion) {
		appVersionList.add(appVersion);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#clearAppVersions()
	 */
	@Override
	public void clearAppVersions() {
		appVersionList.clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#createEmptyCollectionWithMetadata()
	 */
	@Override
	public SortedBugCollection createEmptyCollectionWithMetadata() {
		SortedBugCollection result = duplicate();
		result.clearBugInstances();
		
		return result;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#setTimestamp(long)
	 */
	@Override
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getTimestamp()
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#getClassFeatureSet(java.lang.String)
	 */
	@Override
	public ClassFeatureSet getClassFeatureSet(String className) {
		return classFeatureSetMap.get(className);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#setClassFeatureSet(edu.umd.cs.findbugs.model.ClassFeatureSet)
	 */
	@Override
	public void setClassFeatureSet(ClassFeatureSet classFeatureSet) {
		classFeatureSetMap.put(classFeatureSet.getClassName(), classFeatureSet);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#classFeatureSetIterator()
	 */
	@Override
	public Iterator<ClassFeatureSet> classFeatureSetIterator() {
		return classFeatureSetMap.values().iterator();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugCollection#clearClassFeatures()
	 */
	public void clearClassFeatures() {
		classFeatureSetMap.clear();
	}
}

// vim:ts=4
