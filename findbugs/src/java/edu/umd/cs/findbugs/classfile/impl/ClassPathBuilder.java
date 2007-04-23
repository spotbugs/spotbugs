/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.InvalidClassFileFormatException;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.ClassNameAndSuperclassInfo;
import edu.umd.cs.findbugs.classfile.engine.ClassParser;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.Archive;

/**
 * Implementation of IClassPathBuilder.
 * 
 * @author David Hovemeyer
 */
public class ClassPathBuilder implements IClassPathBuilder {
	private static final boolean VERBOSE = SystemProperties.getBoolean("findbugs2.builder.verbose");
	private static final boolean DEBUG = VERBOSE || SystemProperties.getBoolean("findbugs2.builder.debug");
	private static final boolean NO_PARSE_CLASS_NAMES =
		SystemProperties.getBoolean("findbugs2.builder.noparseclassnames");

	/**
	 * Worklist item.
	 * Represents one codebase to be processed during the
	 * classpath construction algorithm.
	 */
	static class WorkListItem {
		private ICodeBaseLocator codeBaseLocator;
		private boolean isAppCodeBase;
		private int howDiscovered;

		@Override
		public String toString() {
			return "WorkListItem(" + codeBaseLocator +", " + isAppCodeBase + ", " + howDiscovered +")";
		}

		public WorkListItem(ICodeBaseLocator codeBaseLocator, boolean isApplication, int howDiscovered) {
			this.codeBaseLocator = codeBaseLocator;
			this.isAppCodeBase = isApplication;
			this.howDiscovered = howDiscovered;
		}

		public ICodeBaseLocator getCodeBaseLocator() {
			return codeBaseLocator;
		}

		public boolean isAppCodeBase() {
			return isAppCodeBase;
		}

		/**
		 * @return Returns the howDiscovered.
		 */
		public int getHowDiscovered() {
			return howDiscovered;
		}
	}

	/**
	 * A codebase discovered during classpath building.
	 */
	static class DiscoveredCodeBase {
		ICodeBase codeBase;
		LinkedList<ICodeBaseEntry> resourceList;

		public DiscoveredCodeBase(ICodeBase codeBase) {
			this.codeBase= codeBase;
			this.resourceList = new LinkedList<ICodeBaseEntry>();
		}

		public ICodeBase getCodeBase() {
			return codeBase;
		}

		public LinkedList<ICodeBaseEntry> getResourceList() {
			return resourceList;
		}

		public void addCodeBaseEntry(ICodeBaseEntry entry) {
			resourceList.add(entry);
		}

		public ICodeBaseIterator iterator() throws InterruptedException {
			if (codeBase instanceof IScannableCodeBase) {
				return ((IScannableCodeBase) codeBase).iterator();
			} else {
				return new ICodeBaseIterator() {
					public boolean hasNext() throws InterruptedException { return false; }

					public ICodeBaseEntry next() throws InterruptedException {
						throw new UnsupportedOperationException();
					}
				};
			}
		}
	}


	// Fields
	private IClassFactory classFactory; 
	private IErrorLogger errorLogger;
	private LinkedList<WorkListItem> projectWorkList;
	private LinkedList<DiscoveredCodeBase> discoveredCodeBaseList;
	private Map<String, DiscoveredCodeBase> discoveredCodeBaseMap;
	private LinkedList<ClassDescriptor> appClassList;
	private boolean scanNestedArchives;

	/**
	 * Constructor.
	 * 
	 * @param classFactory the class factory
	 * @param errorLogger  the error logger
	 */
	ClassPathBuilder(IClassFactory classFactory, IErrorLogger errorLogger) {
		this.classFactory = classFactory;
		this.errorLogger = errorLogger;
		this.projectWorkList = new LinkedList<WorkListItem>();
		this.discoveredCodeBaseList = new LinkedList<DiscoveredCodeBase>();
		this.discoveredCodeBaseMap = new HashMap<String, DiscoveredCodeBase>();
		this.appClassList = new LinkedList<ClassDescriptor>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#addCodeBase(edu.umd.cs.findbugs.classfile.ICodeBaseLocator, boolean)
	 */
	public void addCodeBase(ICodeBaseLocator locator, boolean isApplication) {
		addToWorkList(projectWorkList, new WorkListItem(locator, isApplication, ICodeBase.SPECIFIED));
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#scanNestedArchives(boolean)
	 */
	public void scanNestedArchives(boolean scanNestedArchives) {
		this.scanNestedArchives = scanNestedArchives;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#build(edu.umd.cs.findbugs.classfile.IClassPath, edu.umd.cs.findbugs.classfile.IClassPathBuilderProgress)
	 */
	public void build(IClassPath classPath, IClassPathBuilderProgress progress)
			throws CheckedAnalysisException, IOException, InterruptedException {
		// Discover all directly and indirectly referenced codebases
		processWorkList(classPath, projectWorkList, progress);

		boolean foundJavaLangObject = false;

        for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
			try {
			ICodeBaseEntry entry = discoveredCodeBase.getCodeBase().lookupResource("java/lang/Object.class");
			foundJavaLangObject = true;
            } catch (ResourceNotFoundException e) {
				assert true;
			}
		}
        
		if (!foundJavaLangObject) 
			processWorkList(classPath, buildSystemCodebaseList(), progress);

		// Add all discovered codebases to the classpath
		for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
			classPath.addCodeBase(discoveredCodeBase.getCodeBase());
		}

		Set<ClassDescriptor> appClassSet = new HashSet<ClassDescriptor>();

		// Build collection of all application classes.
		// Also, add resource name -> codebase entry mappings for application classes.
		for (DiscoveredCodeBase discoveredCodeBase : discoveredCodeBaseList) {
			if (!discoveredCodeBase.getCodeBase().isApplicationCodeBase()) {
				continue;
			}

		codeBaseEntryLoop:
			for (ICodeBaseIterator i = discoveredCodeBase.iterator(); i.hasNext(); ) {
				ICodeBaseEntry entry = i.next();
				if (!ClassDescriptor.isClassResource(entry.getResourceName())) {
					continue;
				}

				ClassDescriptor classDescriptor = entry.getClassDescriptor();
				if (classDescriptor == null) throw new IllegalStateException();

				if (appClassSet.contains(classDescriptor)) {
					// An earlier entry takes precedence over this class
					continue codeBaseEntryLoop;
				}
				appClassSet.add(classDescriptor);
				appClassList.add(classDescriptor);

				classPath.mapResourceNameToCodeBaseEntry(entry.getResourceName(), entry);
			}
		}

		if (DEBUG) {
			System.out.println("Classpath:");
			dumpCodeBaseList(classPath.appCodeBaseIterator(), "Application codebases");
			dumpCodeBaseList(classPath.auxCodeBaseIterator(), "Auxiliary codebases");
		}
	}

	private void dumpCodeBaseList(Iterator<? extends ICodeBase> i, String desc)
			throws InterruptedException {
		System.out.println("  " + desc + ":");
		while (i.hasNext()) {
			ICodeBase codeBase = i.next();
			System.out.println("    " + codeBase.getCodeBaseLocator().toString());
			if (codeBase.containsSourceFiles()) {
				System.out.println("      * contains source files");
			}
		}
	}

	private LinkedList<WorkListItem> buildSystemCodebaseList() {
		// This method is based on the
		// org.apache.bcel.util.ClassPath.getClassPath()
		// method.

		LinkedList<WorkListItem> workList = new LinkedList<WorkListItem>();

		String bootClassPath = SystemProperties.getProperty("sun.boot.class.path");
		// Seed worklist with system codebases.
		// addWorkListItemsForClasspath(workList, SystemProperties.getProperty("java.class.path"));
		addWorkListItemsForClasspath(workList, bootClassPath);
		String extPath = SystemProperties.getProperty("java.ext.dirs");
		if (extPath != null) {
			StringTokenizer st = new StringTokenizer(extPath, File.pathSeparator);
			while (st.hasMoreTokens()) {
				String extDir = st.nextToken();
				addWorkListItemsForExtDir(workList, extDir);
			}
		}

		return workList;
	}

	/**
	 * Add worklist items from given system classpath.
	 * 
	 * @param workList the worklist
	 * @param path     a system classpath
	 */
	private void addWorkListItemsForClasspath(LinkedList<WorkListItem> workList, String path) {
		if (path == null) {
			return;
		}

		StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
		while (st.hasMoreTokens()) {
			String entry = st.nextToken();
			if (DEBUG) {
				System.out.println("System classpath entry: " + entry);
			}
			addToWorkList(workList, new WorkListItem(
					classFactory.createFilesystemCodeBaseLocator(entry), false, ICodeBase.IN_SYSTEM_CLASSPATH));
		}
	}

	/**
	 * Add worklist items from given extensions directory.
	 * 
	 * @param workList the worklist
	 * @param extDir   an extensions directory
	 */
	private void addWorkListItemsForExtDir(LinkedList<WorkListItem> workList, String extDir) {
		File dir = new File(extDir);
		File[] fileList = dir.listFiles(new FileFilter() {
			/* (non-Javadoc)
			 * @see java.io.FileFilter#accept(java.io.File)
			 */
			public boolean accept(File pathname) {
				String path = pathname.getParent();
				return Archive.isArchiveFileName(path);
			}
		});
		if (fileList == null) {
			return;
		}

		for (File archive : fileList) {
			addToWorkList(workList, new WorkListItem(
					classFactory.createFilesystemCodeBaseLocator(archive.getPath()), false, ICodeBase.IN_SYSTEM_CLASSPATH));
		}
	}

	/**
	 * Process classpath worklist items.
	 * We will attempt to find all nested archives and
	 * Class-Path entries specified in Jar manifests.  This should give us
	 * as good an idea as possible of all of the classes available (and
	 * which are part of the application).
	 * 
	 * @param workList the worklist to process
	 * @param progress IClassPathBuilderProgress callback
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ResourceNotFoundException
	 */
	private void processWorkList(
			IClassPath classPath,
			LinkedList<WorkListItem> workList, IClassPathBuilderProgress progress)
			throws InterruptedException, IOException, ResourceNotFoundException {
		// Build the classpath, scanning codebases for nested archives
		// and referenced codebases.
		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();
			if (DEBUG) {
				System.out.println("Working: " + item.getCodeBaseLocator());
			}

			DiscoveredCodeBase discoveredCodeBase;

			// See if we have encountered this codebase before
			discoveredCodeBase = discoveredCodeBaseMap.get(item.getCodeBaseLocator().toString());
			if (discoveredCodeBase != null) {
				// If the codebase is not an app codebase and
				// the worklist item says that it is an app codebase,
				// change it.  Otherwise, we have nothing to do.
				if (!discoveredCodeBase.getCodeBase().isApplicationCodeBase() && item.isAppCodeBase()) {
					discoveredCodeBase.getCodeBase().setApplicationCodeBase(true);
				}

				continue;
			}

			// If we are working on an application codebase,
			// then failing to open/scan it is a fatal error.
			// We issue warnings about problems with aux codebases,
			// but continue anyway.

			try {
				// Open the codebase and add it to the classpath
				discoveredCodeBase = new DiscoveredCodeBase(item.getCodeBaseLocator().openCodeBase());
				discoveredCodeBase.getCodeBase().setApplicationCodeBase(item.isAppCodeBase());
				discoveredCodeBase.getCodeBase().setHowDiscovered(item.getHowDiscovered());

				// Note that this codebase has been visited
				discoveredCodeBaseMap.put(item.getCodeBaseLocator().toString(), discoveredCodeBase);
				discoveredCodeBaseList.addLast(discoveredCodeBase);

				// If it is a scannable codebase, check it for nested archives.
				// In addition, if it is an application codebase then
				// make a list of application classes.
				if (discoveredCodeBase.getCodeBase() instanceof IScannableCodeBase && discoveredCodeBase.codeBase.isApplicationCodeBase()) {
					scanCodebase(classPath, workList, discoveredCodeBase);
				}

				// Check for a Jar manifest for additional aux classpath entries.
				scanJarManifestForClassPathEntries(workList, discoveredCodeBase.getCodeBase());
			} catch (IOException e) {
				if (item.isAppCodeBase()) {
					throw e;
				} else if (item.getHowDiscovered() == ICodeBase.SPECIFIED) {
					errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
				}
			} catch (ResourceNotFoundException e) {
				if (item.isAppCodeBase()) {
					throw e;
				} else if (item.getHowDiscovered() == ICodeBase.SPECIFIED) {
					errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
				}
			}

			if (item.getHowDiscovered() == ICodeBase.SPECIFIED) {
				progress.finishArchive();
			}
		}
	}

	/**
	 * Scan given codebase in order to
	 * <ul>
	 * <li> check the codebase for nested archives
	 *      (adding any found to the worklist)
	 * <li> build a list of class resources found in the codebase
	 * </ul>
	 * 
	 * @param workList the worklist
	 * @param discoveredCodeBase the codebase to scan
	 * @throws InterruptedException 
	 */
	private void scanCodebase(IClassPath classPath, LinkedList<WorkListItem> workList, DiscoveredCodeBase discoveredCodeBase)
			throws InterruptedException {
		if (DEBUG) {
			System.out.println("Scanning " + discoveredCodeBase.getCodeBase().getCodeBaseLocator());
		}

		IScannableCodeBase codeBase = (IScannableCodeBase) discoveredCodeBase.getCodeBase();

		ICodeBaseIterator i = codeBase.iterator();
		while (i.hasNext()) {
			ICodeBaseEntry entry = i.next();
			if (VERBOSE) {
				System.out.println("Entry: " + entry.getResourceName());
			}

			if (!NO_PARSE_CLASS_NAMES
					&& codeBase.isApplicationCodeBase()
					&& ClassDescriptor.isClassResource(entry.getResourceName())) {
				parseClassName(entry);
			}

			// Note the resource exists in this codebase
			discoveredCodeBase.addCodeBaseEntry(entry);

			// If resource is a nested archive, add it to the worklist
			if (scanNestedArchives && codeBase.isApplicationCodeBase() && Archive.isArchiveFileName(entry.getResourceName())) {
				if (VERBOSE) {
					System.out.println("Entry is an archive!");
				}
				ICodeBaseLocator nestedArchiveLocator =
					classFactory.createNestedArchiveCodeBaseLocator(codeBase, entry.getResourceName());
				addToWorkList(
						workList,
						new WorkListItem(nestedArchiveLocator, codeBase.isApplicationCodeBase(), ICodeBase.NESTED));
			}
		}
	}

	/**
	 * Attempt to parse data of given resource in order
	 * to divine the real name of the class contained in the
	 * resource. 
	 * 
	 * @param entry the resource
	 */
	private void parseClassName(ICodeBaseEntry entry) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(entry.openResource());
			ClassParser parser = new ClassParser(in, null, entry);

			ClassNameAndSuperclassInfo classInfo = new ClassNameAndSuperclassInfo();
			parser.parse(classInfo);
			entry.overrideResourceName(classInfo.getClassDescriptor().toResourceName());
		} catch (IOException e) {
			errorLogger.logError("Invalid class resource " + entry.getResourceName() +
					" in " + entry, e);
		} catch (InvalidClassFileFormatException e) {
			errorLogger.logError("Invalid class resource " + entry.getResourceName() +
					" in " + entry, e);
		} finally {
			IO.close(in);
		}
	}

	/**
	 * Check a codebase for a Jar manifest to examine for Class-Path entries.
	 * 
	 * @param workList the worklist
	 * @param codeBase the codebase for examine for a Jar manifest
	 * @throws IOException 
	 */
	private void scanJarManifestForClassPathEntries(LinkedList<WorkListItem> workList, ICodeBase codeBase)
			throws IOException {
		try {
			// See if this codebase has a jar manifest
			ICodeBaseEntry manifestEntry = codeBase.lookupResource("META-INF/MANIFEST.MF");

			// Try to read the manifest
			InputStream in = null;
			try {
				in = manifestEntry.openResource();
				Manifest manifest = new Manifest(in);

				Attributes mainAttrs = manifest.getMainAttributes();
				String classPath = mainAttrs.getValue("Class-Path");
				if (classPath != null) {
					String[] pathList = classPath.split("\\s+");

					for (String path : pathList) {
						// Create a codebase locator for the classpath entry
						// relative to the codebase in which we discovered the Jar
						// manifest
						ICodeBaseLocator relativeCodeBaseLocator =
							codeBase.getCodeBaseLocator().createRelativeCodeBaseLocator(path);

						// Codebases found in Class-Path entries are always
						// added to the aux classpath, not the application.
						addToWorkList(workList, new WorkListItem(relativeCodeBaseLocator, false, ICodeBase.IN_JAR_MANIFEST));
					}
				}
			} finally {
				if (in != null) {
					IO.close(in);
				}
			}
		} catch (ResourceNotFoundException e) {
			// Do nothing - no Jar manifest found
		}

	}

	/**
	 * Add a worklist item to the worklist.
	 * This method maintains the invariant that all of the worklist
	 * items representing application codebases appear <em>before</em>
	 * all of the worklist items representing auxiliary codebases.
	 * 
	 * @param projectWorkList the worklist
	 * @param itemToAdd     the worklist item to add
	 */
	private void addToWorkList(LinkedList<WorkListItem> workList, WorkListItem itemToAdd) {
		if (DEBUG) {
			new RuntimeException("Adding work list item " + itemToAdd).printStackTrace(System.out);
		}
		if (!itemToAdd.isAppCodeBase()) {
			// Auxiliary codebases are always added at the end
			workList.addLast(itemToAdd);
			return;
		}

		// Adding an application codebase: position a ListIterator
		// just before first auxiliary codebase (or at the end of the list
		// if there are no auxiliary codebases)
		ListIterator<WorkListItem> i = workList.listIterator();
		while (i.hasNext()) {
			WorkListItem listItem = i.next();
			if (!listItem.isAppCodeBase()) {
				i.previous();
				break;
			}
		}

		// Add the codebase to the worklist
		i.add(itemToAdd);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#getAppClassList()
	 */
	public List<ClassDescriptor> getAppClassList() {
		return appClassList;
	}
}
