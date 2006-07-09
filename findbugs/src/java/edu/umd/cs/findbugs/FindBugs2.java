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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.io.IO;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;
import edu.umd.cs.findbugs.util.Archive;

/**
 * FindBugs driver class.
 * Experimental version to use the new bytecode-framework-neutral
 * codebase/classpath/classfile infrastructure.
 * Don't expect this class to do anything useful for a while.
 * 
 * @author David Hovemeyer
 */
public class FindBugs2 {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs2.debug");
	
	private BugReporter bugReporter;
	private Project project;
	private IClassFactory classFactory;
	private IClassPath classPath;
	private IAnalysisCache analysisCache;
	private List<ClassDescriptor> appClassList;
	private DetectorFactoryCollection detectorFactoryCollection;
	private ExecutionPlan executionPlan;
	
	public FindBugs2(BugReporter bugReporter, Project project) {
		this.bugReporter = bugReporter;
		this.project = project;
	}
	
	/**
	 * Set the detector factory collection to be used by this
	 * FindBugs2 engine.  This method should be called before
	 * the execute() method is called.
	 * 
	 * @param detectorFactoryCollection The detectorFactoryCollection to set.
	 */
	public void setDetectorFactoryCollection(
			DetectorFactoryCollection detectorFactoryCollection) {
		this.detectorFactoryCollection = detectorFactoryCollection;
	}

	/**
	 * Execute the analysis.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws CheckedAnalysisException
	 */
	public void execute() throws IOException, InterruptedException, CheckedAnalysisException {
		// Get the class factory for creating classpath/codebase/etc. 
		classFactory = ClassFactory.instance();
		
		// The class path object
		createClassPath();
		
		// The analysis cache object
		// FIXME: should also be in the analysis context
		createAnalysisCache();

		// List of application classes found while scanning application codebases
		appClassList = new LinkedList<ClassDescriptor>();
		
		try {
			buildClassPath();
			createExecutionPlan();
			analyzeApplication();
			
			// TODO: the execution plan, analysis, etc.
		} finally {
			// Make sure the codebases on the classpath are closed
			classPath.close();
		}
	}

	/**
	 * Create the classpath object.
	 */
	private void createClassPath() {
		// FIXME: this should be in the analysis context eventually
		classPath = classFactory.createClassPath();
	}

	/**
	 * Create the analysis cache object.
	 */
	private void createAnalysisCache() {
		analysisCache = ClassFactory.instance().createAnalysisCache(classPath);
		
		// TODO: this would be a good place to load "analysis plugins" which could
		// add additional analysis engines.  Or, perhaps when we load
		// detector plugins we should check for analysis engines.
		// Either way, allowing plugins to add new analyses would be nice.
		new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
	}

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

	/**
	 * Build the classpath by scanning the application and aux classpath entries
	 * specified in the project.  We will attempt to find all nested archives and
	 * Class-Path entries specified in Jar manifests.  This should give us
	 * as good an idea as possible of all of the classes available (and
	 * which are part of the application).
	 * 
	 * @throws InterruptedException if the analysis thread is interrupted
	 * @throws IOException if an I/O error occurs
	 * @throws ResourceNotFoundException 
	 */
	private void buildClassPath() throws InterruptedException, IOException, ResourceNotFoundException {
		// Seed worklist with app codebases and aux codebases.
		LinkedList<WorkListItem> workList = new LinkedList<WorkListItem>();
		for (String path : project.getFileArray()) {
			addToWorkList(workList, new WorkListItem(classFactory.createFilesystemCodeBaseLocator(path), true));
		}
		for (String path : project.getAuxClasspathEntryList()) {
			addToWorkList(workList, new WorkListItem(classFactory.createFilesystemCodeBaseLocator(path), false));
		}

		// Build the classpath, scanning codebases for nested archives
		// and referenced codebases.
		while (!workList.isEmpty()) {
			WorkListItem item = workList.removeFirst();
			
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
					scanCodebase(workList, (IScannableCodeBase) codeBase);
				}

				// Check for a Jar manifest for additional aux classpath entries.
				scanJarManifestForClassPathEntries(workList, codeBase);
			} catch (IOException e) {
				if (isAppCodeBase) {
					throw e;
				} else {
					// TODO: log warning
				}
			} catch (ResourceNotFoundException e) {
				if (isAppCodeBase) {
					throw e;
				} else {
					// TODO: log warning
				}
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
	private void scanCodebase(LinkedList<WorkListItem> workList, IScannableCodeBase codeBase)
			throws InterruptedException {
		if (DEBUG) {
			System.out.println("Scanning " + codeBase.getCodeBaseLocator());
		}
		ICodeBaseIterator i = codeBase.iterator();
		while (i.hasNext()) {
			ICodeBaseEntry entry = i.next();
			if (DEBUG) {
				System.out.println("Entry: " + entry.getResourceName());
			}

			// Add nested archives to the worklist
			if (Archive.isArchiveFileName(entry.getResourceName())) {
				if (DEBUG) {
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
	 * @param workList the worklist
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

	/**
	 * Create an execution plan.
	 * 
	 * @throws OrderingConstraintException if the detector ordering constraints are inconsistent
	 */
	private void createExecutionPlan() throws OrderingConstraintException {
		executionPlan = new ExecutionPlan();
		
		// For now, all detectors are enabled.
		// Eventually base this on the user preferences.
		DetectorFactoryChooser detectorFactoryChooser = new DetectorFactoryChooser() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.DetectorFactoryChooser#choose(edu.umd.cs.findbugs.DetectorFactory)
			 */
			public boolean choose(DetectorFactory factory) {
				return true;
			}
		};
		executionPlan.setDetectorFactoryChooser(detectorFactoryChooser);
		
		// Add plugins
		for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext(); ) {
			Plugin plugin = i.next();
			if (DEBUG) {
				System.out.println("Adding plugin " + plugin.getPluginId() + " to execution plan");
			}
			executionPlan.addPlugin(plugin);
		}
		
		// Build the execution plan
		executionPlan.build();
		
		if (DEBUG) {
			System.out.println(executionPlan.getNumPasses() + " passes in execution plan");
		}
	}

	/**
	 * Analyze the classes in the application codebase.
	 * @throws CheckedAnalysisException 
	 */
	private void analyzeApplication() throws CheckedAnalysisException {
		// FIXME: for the moment, we're not building an execution plan or supporting multiple passes
		
		for (ClassDescriptor descriptor : appClassList) {
			System.out.println("App class: " + descriptor);
			
			ClassData classData = analysisCache.getClassAnalysis(ClassData.class, descriptor);
			System.out.println(" ** contains " + classData.getData().length + " bytes of data");
			
			ClassInfo classInfo = analysisCache.getClassAnalysis(ClassInfo.class, descriptor);
			System.out.println(" ** class:" + classInfo.getClassDescriptor());
			System.out.println(" ** superclass:" + classInfo.getSuperclassDescriptor());
			System.out.println(" ** access flags:" + classInfo.getAccessFlags());
		}
	}
	
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + FindBugs2.class.getName() + " <project>");
			System.exit(1);
		}

		if (System.getProperty("findbugs.home") == null) {
			throw new IllegalArgumentException("findbugs.home property must be set!");
		}
		
		BugReporter bugReporter = new PrintingBugReporter();
		
		Project project = new Project();
		project.read(args[0]);
		
		FindBugs2 findBugs = new FindBugs2(bugReporter, project);
		
		// Load plugins
		DetectorFactoryCollection detectorFactoryCollection = new DetectorFactoryCollection();
		detectorFactoryCollection.loadPlugins();
		findBugs.setDetectorFactoryCollection(detectorFactoryCollection);
		
		findBugs.execute();
	}
}
