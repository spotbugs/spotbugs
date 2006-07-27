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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.util.Archive;

/**
 * Implementation of IClassPathBuilder.
 * 
 * @author David Hovemeyer
 */
public class ClassPathBuilder implements IClassPathBuilder {
	private static final boolean VERBOSE = Boolean.getBoolean("findbugs2.verbose");
	private static final boolean DEBUG = VERBOSE || Boolean.getBoolean("findbugs2.debug");

	/**
	 * Worklist item.
	 * Represents one codebase to be processed during the
	 * classpath construction algorithm.
	 */
	static class WorkListItem {
		private ICodeBaseLocator codeBaseLocator;
		private boolean isAppCodeBase;
		
		WorkListItem(ICodeBaseLocator codeBaseLocator, boolean isApplication) {
			this.codeBaseLocator = codeBaseLocator;
			this.isAppCodeBase = isApplication;
		}
		
		public ICodeBaseLocator getCodeBaseLocator() {
			return codeBaseLocator;
		}
		
		public boolean isAppCodeBase() {
			return isAppCodeBase;
		}
	}
	
	private IClassFactory classFactory; 
	private IErrorLogger errorLogger;
	private LinkedList<WorkListItem> projectWorkList;
	private LinkedList<ClassDescriptor> appClassList;
	
	ClassPathBuilder(IClassFactory classFactory, IErrorLogger errorLogger) {
		this.classFactory = classFactory;
		this.errorLogger = errorLogger;
		this.projectWorkList = new LinkedList<WorkListItem>();
		this.appClassList = new LinkedList<ClassDescriptor>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#addCodeBase(edu.umd.cs.findbugs.classfile.ICodeBaseLocator, boolean)
	 */
	public void addCodeBase(ICodeBaseLocator locator, boolean isApplication) {
		addToWorkList(projectWorkList, new WorkListItem(locator, isApplication));
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IClassPathBuilder#build(edu.umd.cs.findbugs.classfile.IClassPath)
	 */
	public void build(IClassPath classPath) throws ResourceNotFoundException, IOException, InterruptedException {
		processWorkList(classPath, projectWorkList);
		processWorkList(classPath, buildSystemCodebaseList());
		
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

		// Seed worklist with system codebases.
		addWorkListItemsForClasspath(workList, System.getProperty("java.class.path"));
		addWorkListItemsForClasspath(workList, System.getProperty("sun.boot.class.path"));
		String extPath = System.getProperty("java.ext.dirs");
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
					classFactory.createFilesystemCodeBaseLocator(entry), false));
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
					classFactory.createFilesystemCodeBaseLocator(archive.getPath()), false));
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
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws ResourceNotFoundException
	 */
	private void processWorkList(IClassPath classPath, LinkedList<WorkListItem> workList) throws InterruptedException, IOException, ResourceNotFoundException {
		// Build the classpath, scanning codebases for nested archives
		// and referenced codebases.
		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();
			if (DEBUG) {
				System.out.println("Working: " + item.getCodeBaseLocator());
			}
			
			// If we are working on an application codebase,
			// then failing to open/scan it is a fatal error.
			// We issue warnings about problems with aux codebases,
			// but continue anyway.
			boolean isAppCodeBase = item.isAppCodeBase();

			try {
				// Open the codebase and add it to the classpath
				ICodeBase codeBase = item.getCodeBaseLocator().openCodeBase();
				codeBase.setApplicationCodeBase(isAppCodeBase);
				classPath.addCodeBase(codeBase);

				// If it is a scannable codebase, check it for nested archives.
				// In addition, if it is an application codebase then
				// make a list of application classes.
				if (codeBase instanceof IScannableCodeBase) {
					scanCodebase(classPath, workList, (IScannableCodeBase) codeBase);
				}

				// Check for a Jar manifest for additional aux classpath entries.
				scanJarManifestForClassPathEntries(workList, codeBase);
			} catch (IOException e) {
				if (isAppCodeBase) {
					throw e;
				} else {
					errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
				}
			} catch (ResourceNotFoundException e) {
				if (isAppCodeBase) {
					throw e;
				} else {
					errorLogger.logError("Cannot open codebase " + item.getCodeBaseLocator(), e);
				}
			}
		}
	}

	/**
	 * Scan given codebase:
	 * <ul>
	 * <li> check a codebase for nested archives
	 *      (adding any found to the worklist)
	 * <li> build a list of classes in application codebases
	 * <li> add resource name to codebase entry mappings
	 *      to the classpath for application codebases
	 * </ul>
	 * 
	 * @param workList the worklist
	 * @param codeBase the codebase to scan
	 * @throws InterruptedException 
	 */
	private void scanCodebase(IClassPath classPath, LinkedList<WorkListItem> workList, IScannableCodeBase codeBase)
			throws InterruptedException {
		if (DEBUG) {
			System.out.println("Scanning " + codeBase.getCodeBaseLocator());
		}
		ICodeBaseIterator i = codeBase.iterator();
		while (i.hasNext()) {
			ICodeBaseEntry entry = i.next();
			if (VERBOSE) {
				System.out.println("Entry: " + entry.getResourceName());
			}

			// Add nested archives to the worklist
			if (Archive.isArchiveFileName(entry.getResourceName())) {
				if (VERBOSE) {
					System.out.println("Entry is an archive!");
				}
				ICodeBaseLocator nestedArchiveLocator =
					classFactory.createNestedArchiveCodeBaseLocator(codeBase, entry.getResourceName());
				addToWorkList(workList, new WorkListItem(nestedArchiveLocator, codeBase.isApplicationCodeBase()));
			}
			
			// In application codebases,
			//   - record all classesd
			//   - add authoritative resource name -> codebase entry mappings to classpath
			if (codeBase.isApplicationCodeBase()) {
				if (ClassDescriptor.isClassResource(entry.getResourceName())) {
					appClassList.add(ClassDescriptor.fromResourceName(entry.getResourceName()));
				}
				classPath.mapResourceNameToCodeBaseEntry(entry.getResourceName(), entry);
			}
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
						addToWorkList(workList, new WorkListItem(relativeCodeBaseLocator, false));
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
