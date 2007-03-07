/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FuzzyBugComparator;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SloppyBugComparator;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.VersionInsensitiveBugComparator;
import edu.umd.cs.findbugs.WarningComparator;
import edu.umd.cs.findbugs.config.CommandLine;
import edu.umd.cs.findbugs.model.MovedClassMap;
import edu.umd.cs.findbugs.util.Util;

/**
 * Analyze bug results to find new, fixed, and retained bugs
 * between versions of the same program.  Uses VersionInsensitiveBugComparator
 * (or FuzzyBugComparator)
 * to determine when two BugInstances are the "same".
 * The new BugCollection returned is a deep copy of one of the input collections
 * (depending on the operation performed), with only a subset of the original
 * BugInstances retained.  Because it is a deep copy, it may be freely modified.
 * 
 * @author David Hovemeyer
 */
@Deprecated
public class BugHistory {
	private static final boolean DEBUG = false;
	
	private static class BugCollectionAndProject {
		SortedBugCollection bugCollection;
		Project project;
		
		public BugCollectionAndProject(SortedBugCollection bugCollection, Project project) {
			this.bugCollection = bugCollection;
			this.project = project;
		}
		
		/**
		 * @return Returns the bugCollection.
		 */
		public SortedBugCollection getBugCollection() {
			return bugCollection;
		}
		
		/**
		 * @return Returns the project.
		 */
		public Project getProject() {
			return project;
		}
	}

	/**
	 * Cache of BugCollections and Projects for when we're operating in bulk mode.
	 * If the pairs of files form a chronological sequence, then we won't have to
	 * repeatedly perform I/O.
	 */
	private static class BugCollectionAndProjectCache extends LinkedHashMap<String,BugCollectionAndProject> {
		private static final long serialVersionUID = 1L;
		
		// 2 should be sufficient if the pairs are sorted
		private static final int CACHE_SIZE = 5;
		
		/* (non-Javadoc)
		 * @see java.util.LinkedHashMap#removeEldestEntry(java.util.Map.Entry)
		 */
		
		@Override
		protected boolean removeEldestEntry(Entry<String, BugCollectionAndProject> eldest) {
			return size() > CACHE_SIZE;
		}
		
		/**
		 * Fetch an entry, reading it if necessary.
		 * 
		 * @param fileName file to get
		 * @return the BugCollectionAndProject for the file
		 * @throws IOException
		 * @throws DocumentException
		 */
		public BugCollectionAndProject fetch(String fileName) throws IOException, DocumentException {
			BugCollectionAndProject result = get(fileName);
			if (result == null) {
				Project project = new Project();
				SortedBugCollection bugCollection = readCollection(fileName, project);
				result = new BugCollectionAndProject(bugCollection, project);
				put(fileName, result);
			}
			return result;
		}
	}
	
	/**
	 * A set operation between two bug collections.
	 */
	public interface SetOperation {
		/**
		 * Perform the set operation.
		 * 
		 * @param result         Set to put the resulting BugInstances in
		 * @param origCollection original BugCollection
		 * @param newCollection  new BugCollection
		 * @return the input bug collection the results are taken from
		 */
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection);
	}
	
	/**
	 * Get the warnings which were <em>added</em>,
	 * meaning that they were not part of the original BugCollection.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation ADDED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(newCollection.getCollection());

			// Get shared instances
			List<BugInstance> inBoth = getSharedInstances(result, origCollection);
			
			// Remove the shared instances from the result
			removeBugInstances(result, inBoth);
			
			return newCollection;
		}
	};
	
	/**
	 * Get the warnings which were <em>retained</em>,
	 * meaning that they occur in both the original and new BugCollections.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation RETAINED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(newCollection.getCollection());
			if (DEBUG) System.out.println(result.size() + " instances initially");
			
			// Get shared instances
			List<BugInstance> inBoth = getSharedInstances(result, origCollection);
			
			// Replace instances with only those shared
			replaceBugInstances(result, inBoth);
			
			if (DEBUG) System.out.println(result.size() + " after retaining new instances");
			return newCollection;
		}
	};
	
	/**
	 * Get the warnings which were <em>removed</em>,
	 * meaning that they occur in the original BugCollection but not in
	 * the new BugCollection.
	 * The BugInstances returned are from the original BugCollection.
	 */
	public static final SetOperation REMOVED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(origCollection.getCollection());
			
			// Get shared instances
			List<BugInstance> inBoth = getSharedInstances(result, newCollection);
			
			// Remove shared instances
			removeBugInstances(result, inBoth);
			
			return origCollection;
		}
	};
	
	private SortedBugCollection origCollection, newCollection;
	private SortedBugCollection resultCollection;
	private SortedBugCollection originator;
	private WarningComparator comparator;
	
	/**
	 * Contructor.
	 * 
	 * @param origCollection the original BugCollection
	 * @param newCollection  the new BugCollection
	 */
	public BugHistory(SortedBugCollection origCollection, SortedBugCollection newCollection) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
	}
	
	/**
	 * Get the Comparator used to compare BugInstances from different BugCollections.
	 */
	public WarningComparator getComparator() {
		return comparator;
	}
	
	/**
	 * @param comparator The comparator to set.
	 */
	public void setComparator(WarningComparator comparator) {
		this.comparator = comparator;
	}

	/**
	 * Perform a SetOperation.
	 * 
	 * @param operation the SetOperation
	 * @return the BugCollection resulting from performing the SetOperation
	 */
	public SortedBugCollection performSetOperation(SetOperation operation) {
		// Create a result set which uses the version-insensitive/fuzzy bug comparator.
		// This will help figure out which bug instances are the "same"
		// between versions.
		TreeSet<BugInstance> result = new TreeSet<BugInstance>(getComparator());
		
		// Perform the operation, keeping track of which input BugCollection
		// should be cloned for metadata.
		originator = operation.perform(result, origCollection, newCollection);
		
		// Clone the actual BugInstances selected by the set operation.
		Collection<BugInstance> selected = new LinkedList<BugInstance>();
		SortedBugCollection.cloneAll(selected, result);
		
		// Duplicate the collection from which the results came,
		// in order to copy all metadata, such as analysis errors,
		// class/method hashes, etc.
		SortedBugCollection resultCollection = originator.duplicate();
		
		// Replace with just the cloned instances of the subset selected by the set operation.
		resultCollection.clearBugInstances();
		resultCollection.addAll(selected);
		
		this.resultCollection = resultCollection;
		
		return resultCollection;
	}
	
	/**
	 * @return Returns the originator.
	 */
	public SortedBugCollection getOriginator() {
		return originator;
	}
	
	/**
	 * @return Returns the origCollection.
	 */
	public SortedBugCollection getOrigCollection() {
		return origCollection;
	}
	
	/**
	 * @return Returns the newCollection.
	 */
	public SortedBugCollection getNewCollection() {
		return newCollection;
	}
	
	/**
	 * @return Returns the result.
	 */
	public SortedBugCollection getResultCollection() {
		return resultCollection;
	}

	public void writeResultCollection(Project origProject, Project newProject, OutputStream outputStream) throws IOException {
		getResultCollection().writeXML(
				outputStream, getOriginator() == getOrigCollection() ? origProject : newProject);
	}

	/**
	 * Get instances shared between given Set and BugCollection.
	 * The Set is queried for membership, because it has a special Comparator
	 * which can match BugInstances from different versions.
	 * 
	 * @param result     the Set
	 * @param collection the BugCollection
	 * @return List of shared instances
	 */
	private static List<BugInstance> getSharedInstances(Set<BugInstance> result, SortedBugCollection collection) {
		List<BugInstance> inBoth = new LinkedList<BugInstance>();
		for (Iterator<BugInstance> i = collection.iterator(); i.hasNext();) {
			BugInstance origBugInstance = i.next();
			if (result.contains(origBugInstance)) {
				inBoth.add(origBugInstance);
			}
		}
		return inBoth;
	}

	/**
	 * Replace all of the BugInstances in given Set with the given Collection.
	 * 
	 * @param dest   the Set to replace the instances of
	 * @param source the Collection containing the instances to put in the Set
	 */
	private static void replaceBugInstances(Set<BugInstance> dest, Collection<BugInstance> source) {
		dest.clear();
		dest.addAll(source);
	}

	/**
	 * Remove bug instances from Set.
	 * 
	 * @param result   the Set
	 * @param toRemove Collection of BugInstances to remove
	 */
	private static void removeBugInstances(Set<BugInstance> result, Collection<BugInstance> toRemove) {
		for (BugInstance aToRemove : toRemove) {
			result.remove(aToRemove);
		}
	}
	
	private static final int VERSION_INSENSITIVE_COMPARATOR = 0;
	private static final int FUZZY_COMPARATOR = 1;
	private static final int SLOPPY_COMPARATOR = 2;
	
	private static class BugHistoryCommandLine extends CommandLine {
		private int comparatorType = VERSION_INSENSITIVE_COMPARATOR;
		private boolean count;
		private String opName;
		private SetOperation setOp;
		private String listFile;
		private String outputDir;
		private boolean verbose;
		
		public BugHistoryCommandLine() {
			addSwitch("-fuzzy", "use fuzzy warning matching");
			addSwitch("-sloppy", "use sloppy warning matching");
			addSwitch("-added", "compute added warnings");
			addSwitch("-new", "same as \"-added\" switch");
			addSwitch("-removed", "compute removed warnings");
			addSwitch("-fixed", "same as \"-removed\" switch");
			addSwitch("-retained", "compute retained warnings");
			addSwitch("-count", "just print warning count");
			addOption("-bulk", "file of csv xml file pairs", "bulk mode, output written to v2-OP.xml");
			addOption("-outputDir", "output dir", "output directory for bulk mode (optional)");
			addSwitch("-verbose", "verbose output for bulk mode");
		}
		
		 /* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		
		@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-fuzzy")) {
				comparatorType = FUZZY_COMPARATOR;
			} else if (option.equals("-sloppy")) {
				comparatorType = SLOPPY_COMPARATOR;
			} else if (option.equals("-added") || option.equals("-new")) {
				opName = option;
				setOp = ADDED_WARNINGS;
			} else if (option.equals("-removed") || option.equals("-fixed")) {
				opName = option;
				setOp = REMOVED_WARNINGS;
			} else if (option.equals("-retained")) {
				opName = option;
				setOp = RETAINED_WARNINGS;
			} else if (option.equals("-count")) {
				count = true;
			} else if (option.equals("-verbose")) {
				verbose = true;
			} else {
				throw new IllegalArgumentException("Unknown option: " + option);
			}
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		
		@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			if (option.equals("-bulk")) {
				listFile = argument;
			} else if (option.equals("-outputDir")) {
				outputDir = argument;
			} else {
				throw new IllegalArgumentException("Unknown option: " + option);
			}
		}
		
		/**
		 * @return Returns the comparatorType.
		 */
		public int getComparatorType() {
			return comparatorType;
		}
		
		/**
		 * @return true if we should just output the delta
		 */
		public boolean isCount() {
			return count;
		}
		
		/**
		 * @return Returns the opName.
		 */
		public String getOpName() {
			return opName;
		}
		
		/**
		 * @return Returns the set operation to apply.
		 */
		public SetOperation getSetOp() {
			return setOp;
		}
		
		/**
		 * @return Returns the listFile.
		 */
		public String getListFile() {
			return listFile;
		}
		
		/**
		 * @return Returns the outputDir.
		 */
		public String getOutputDir() {
			return outputDir;
		}
		
		/**
		 * @return Returns the verbose.
		 */
		public boolean isVerbose() {
			return verbose;
		}

		public void configure(BugHistory bugHistory, SortedBugCollection origCollection, SortedBugCollection newCollection) {
			// Create comparator
			WarningComparator comparator;
			switch (getComparatorType()) {
			case VERSION_INSENSITIVE_COMPARATOR:
				comparator = new VersionInsensitiveBugComparator();
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
			
			// Handle renamed classes
			MovedClassMap classNameRewriter = new MovedClassMap(origCollection, newCollection).execute();
			comparator.setClassNameRewriter(classNameRewriter);
			
			bugHistory.setComparator(comparator);
		}
		
		public BugHistory createAndExecute(
				String origFile, String newFile, Project origProject, Project newProject) throws IOException, DocumentException {
			SortedBugCollection origCollection = readCollection(origFile, origProject);
			SortedBugCollection newCollection = readCollection(newFile, newProject);

			return createAndExecute(origCollection, newCollection, origProject, newProject);
		}
		
		public BugHistory createAndExecute(
				SortedBugCollection origCollection,
				SortedBugCollection newCollection,
				Project origProject,
				Project newProject) {
			BugHistory bugHistory = new BugHistory(origCollection, newCollection);

			configure(bugHistory, origCollection, newCollection);
			
			// We can ignore the return value because it will be accessible by calling getResult()
			bugHistory.performSetOperation(getSetOp());

			return bugHistory;
		}
		
		public String getBulkOutputFileName(String fileName) {
			File file = new File(fileName);
			
			String filePart = file.getName();
			int ext = filePart.lastIndexOf('.');
			if (ext < 0 ) {
				filePart = filePart + getOpName();
			} else {
				filePart = filePart.substring(0, ext) + getOpName() + filePart.substring(ext);
			}

			String dirPart = (getOutputDir() != null) ? getOutputDir() : file.getParent();
					
			File outputFile = new File(dirPart, filePart);
			return outputFile.getPath();
		}
	}
	
	private static SortedBugCollection readCollection(String fileName, Project project)
			throws IOException, DocumentException {
		SortedBugCollection result = new SortedBugCollection();
		result.readXML(fileName, project);
		return result;
	}

	public static void main(String[] argv) throws Exception {
		BugHistoryCommandLine commandLine = new BugHistoryCommandLine();
		int argCount = commandLine.parse(argv);
		
		if (commandLine.getSetOp() == null) {
			System.err.println("No set operation specified");
			printUsage();
			System.exit(1);
		}

		if (commandLine.getListFile() != null) {
			if (argv.length != argCount) {
				printUsage();
			}

			runBulk(commandLine);
		} else{
			if (argv.length - argCount != 2) {
				printUsage();
			}
			
			String origFile = argv[argCount++];
			String newFile = argv[argCount++];
			
			runSinglePair(commandLine, origFile, newFile);
		}
	}

	private static void runBulk(BugHistoryCommandLine commandLine) throws FileNotFoundException, IOException, DocumentException {
		BufferedReader reader;
		if (commandLine.getListFile().equals("-")) {
			reader = new BufferedReader(new InputStreamReader(System.in));
		} else {
			reader = new BufferedReader(Util.getFileReader(commandLine.getListFile()));
		}
		int missing = 0;
		try {
		BugCollectionAndProjectCache cache = new BugCollectionAndProjectCache();

		
		String csvRecord;
		while ((csvRecord = reader.readLine()) != null) {
			csvRecord = csvRecord.trim();
			String[] tuple = csvRecord.split(",");
			if (tuple.length < 2)
				continue;

			String origFile = tuple[0];
			String newFile = tuple[1];
			
			BugCollectionAndProject orig;
			BugCollectionAndProject next;
			
			try {
				orig = cache.fetch(origFile);
				next = cache.fetch(newFile);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e ) {
				System.err.println("Warning: error reading bug collection: " + e.toString());
				++missing;
				continue;
			}
			
			if (commandLine.isVerbose()) {
				System.out.print("Computing delta from " + origFile + " to " + newFile + "...");
				System.out.flush();
			}

			BugHistory bugHistory = commandLine.createAndExecute(
					orig.getBugCollection(), next.getBugCollection(), orig.getProject(), next.getProject());
			
			String outputFile = commandLine.getBulkOutputFileName(newFile);
			if (commandLine.isVerbose()) {
				System.out.print("Writing " + outputFile + "...");
				System.out.flush();
			}
			
			
			bugHistory.writeResultCollection(orig.getProject(), next.getProject(),
					new BufferedOutputStream(new FileOutputStream(outputFile)));
			if (commandLine.isVerbose()) {
				System.out.println("done");
			}
		}
		} finally {
			reader.close();
		}
		if (missing > 0) {
			System.err.println(missing + " pairs skipped because of missing files");
		}
	}

	private static void runSinglePair(BugHistoryCommandLine commandLine, String origFile, String newFile) throws IOException, DocumentException {
		Project origProject = new Project();
		Project newProject = new Project();
		BugHistory bugHistory = commandLine.createAndExecute(origFile, newFile, origProject, newProject);
		
		if (commandLine.isCount()) {
			System.out.println(bugHistory.getResultCollection().getCollection().size());
		} else {
			OutputStream outputStream = System.out;
			bugHistory.writeResultCollection(origProject, newProject, outputStream);
		}
	}

	/**
	 * Print usage and exit.
	 */
	private static void printUsage() {
		System.err.println("Usage: " + BugHistory.class.getName() +
		        " [options] <operation> <old results> <new results>");
		new BugHistoryCommandLine().printUsage(System.err);
		System.exit(1);
	}
}

// vim:ts=4
