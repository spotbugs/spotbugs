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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;

/**
 * FindBugs driver class.
 * Experimental version to use the new bytecode-framework-neutral
 * codebase/classpath/classfile infrastructure.
 * Don't expect this class to do anything useful for a while.
 * 
 * @author David Hovemeyer
 */
public class FindBugs2 {
	private static final boolean VERBOSE = Boolean.getBoolean("findbugs2.verbose");
	private static final boolean DEBUG = VERBOSE || Boolean.getBoolean("findbugs2.debug");
	
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
		analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);
		
		// TODO: this would be a good place to load "analysis plugins" which could
		// add additional analysis engines.  Or, perhaps when we load
		// detector plugins we should check for analysis engines.
		// Either way, allowing plugins to add new analyses would be nice.
		new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
		new edu.umd.cs.findbugs.classfile.engine.bcel.EngineRegistrar().registerAnalysisEngines(analysisCache);
	}

	/**
	 * Build the classpath from project codebases and system codebases.
	 * 
	 * @throws InterruptedException if the analysis thread is interrupted
	 * @throws IOException if an I/O error occurs
	 * @throws ResourceNotFoundException 
	 */
	private void buildClassPath() throws InterruptedException, IOException, ResourceNotFoundException {
		IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);
		
		for (String path : project.getFileArray()) {
			builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), true);
		}
		for (String path : project.getAuxClasspathEntryList()) {
			builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), false);
		}
		
		builder.build(classPath);
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
