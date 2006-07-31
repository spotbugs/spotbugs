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

import edu.umd.cs.findbugs.ba.AnalysisCacheToAnalysisContextAdapter;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.ClassObserver;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBaseLocator;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;
import edu.umd.cs.findbugs.classfile.analysis.ClassData;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.plan.AnalysisPass;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;

/**
 * FindBugs driver class.
 * Experimental version to use the new bytecode-framework-neutral
 * codebase/classpath/classfile infrastructure.
 * It can run detectors, but lacks many features.
 * 
 * @author David Hovemeyer
 */
public class FindBugs2 implements IFindBugsEngine {
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
	
	/**
	 * Constructor.
	 */
	public FindBugs2() {
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
		createAnalysisCache();
		
		try {
			buildClassPath();
			createAnalysisContext();
			createExecutionPlan();
			analyzeApplication();
			
			// TODO: the execution plan, analysis, etc.
		} finally {
			// Make sure the codebases on the classpath are closed
			classPath.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#addClassObserver(edu.umd.cs.findbugs.ba.ClassObserver)
	 */
	public void addClassObserver(ClassObserver classObserver) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#addFilter(java.lang.String, boolean)
	 */
	public void addFilter(String filterFileName, boolean include) throws IOException, FilterException {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#enableTrainingInput(java.lang.String)
	 */
	public void enableTrainingInput(String trainingInputDir) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#enableTrainingOutput(java.lang.String)
	 */
	public void enableTrainingOutput(String trainingOutputDir) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getBugCount()
	 */
	public int getBugCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getCurrentClass()
	 */
	public String getCurrentClass() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getErrorCount()
	 */
	public int getErrorCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getMissingClassCount()
	 */
	public int getMissingClassCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#getReleaseName()
	 */
	public String getReleaseName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setAnalysisFeatureSettings(edu.umd.cs.findbugs.config.AnalysisFeatureSetting[])
	 */
	public void setAnalysisFeatureSettings(AnalysisFeatureSetting[] settingList) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setBugReporter(edu.umd.cs.findbugs.BugReporter)
	 */
	public void setBugReporter(BugReporter bugReporter) {
		this.bugReporter = new DelegatingBugReporter(bugReporter); 
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setClassScreener(edu.umd.cs.findbugs.ClassScreener)
	 */
	public void setClassScreener(ClassScreener classScreener) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setProgressCallback(edu.umd.cs.findbugs.FindBugsProgress)
	 */
	public void setProgressCallback(FindBugsProgress progressCallback) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setProject(edu.umd.cs.findbugs.Project)
	 */
	public void setProject(Project project) {
		this.project = project;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setRelaxedReportingMode(boolean)
	 */
	public void setRelaxedReportingMode(boolean relaxedReportingMode) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setReleaseName(java.lang.String)
	 */
	public void setReleaseName(String releaseName) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setSourceInfoFile(java.lang.String)
	 */
	public void setSourceInfoFile(String sourceInfoFile) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.IFindBugsEngine#setUserPreferences(edu.umd.cs.findbugs.config.UserPreferences)
	 */
	public void setUserPreferences(UserPreferences userPreferences) {
		// TODO Auto-generated method stub
		
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
		
		Global.setAnalysisCacheForCurrentThread(analysisCache);
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
		
		appClassList = builder.getAppClassList();
	}
	
	/**
	 * Create the AnalysisContext that will serve as the BCEL-compatibility
	 * layer over the AnalysisCache.
	 */
	private void createAnalysisContext() throws CheckedAnalysisException {
		AnalysisCacheToAnalysisContextAdapter analysisContext =
			new AnalysisCacheToAnalysisContextAdapter();
		analysisContext.setAppClassList(appClassList);
		AnalysisContext.setCurrentAnalysisContext(analysisContext);
	}

	/**
	 * Create an execution plan.
	 * 
	 * @throws OrderingConstraintException if the detector ordering constraints are inconsistent
	 */
	private void createExecutionPlan() throws OrderingConstraintException {
		executionPlan = new ExecutionPlan();
		
		// For now, enabled all default-enabled detectors.
		// Eventually base this on the user preferences.
		DetectorFactoryChooser detectorFactoryChooser = new DetectorFactoryChooser() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.DetectorFactoryChooser#choose(edu.umd.cs.findbugs.DetectorFactory)
			 */
			public boolean choose(DetectorFactory factory) {
				return factory.isDefaultEnabled();
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
		int passCount = 1;
		for (Iterator<AnalysisPass> i = executionPlan.passIterator(); i.hasNext(); ) {
			if (VERBOSE) {
				System.out.println("Pass " + passCount++);
			}
			AnalysisPass pass = i.next();
			
			// TODO: on first pass, apply detectors to referenced classes too
			
			Detector2[] detectorList = new Detector2[pass.getNumDetectors()];
			int count = 0;
			for (Iterator<DetectorFactory> j = pass.iterator(); j.hasNext();) {
				detectorList[count++] = j.next().createDetector2(bugReporter);
			}

			// On each class, apply each detector
			for (ClassDescriptor classDescriptor : appClassList) {
				if (DEBUG) {
					System.out.println("Class " + classDescriptor);
				}
				for (Detector2 detector : detectorList) {
					if (DEBUG) {
						System.out.println("Applying " + detector.getDetectorClassName() + " to " + classDescriptor);
					}
					try {
						detector.visitClass(classDescriptor);
					} catch (CheckedAnalysisException e) {
						logRecoverableException(classDescriptor, detector, e);
					} catch (AnalysisException e) {
						logRecoverableException(classDescriptor, detector, e);
					}
				}
			}
			
			// Call finishPass on each detector
			for (Detector2 detector : detectorList) {
				detector.finishPass();
			}
		}
	}
	
	/**
	 * Report an exception that occurred while analyzing a class
	 * with a detector.
	 * 
	 * @param classDescriptor class being analyzed
	 * @param detector        detector doing the analysis
	 * @param e               the exception
	 */
	private void logRecoverableException(
			ClassDescriptor classDescriptor, Detector2 detector, Throwable e) {
		bugReporter.logError("Exception analyzing " + classDescriptor.toDottedClassName() +
				" using detector " + detector.getDetectorClassName(), e);
	}

	public static void main(String[] args) throws Exception {
		if (System.getProperty("findbugs.home") == null) {
			throw new IllegalArgumentException("findbugs.home property must be set!");
		}

		// Create DetectorFactoryCollection and load plugins
		DetectorFactoryCollection detectorFactoryCollection = new DetectorFactoryCollection();
		detectorFactoryCollection.loadPlugins();
		
		// Create FindBugs2 engine
		FindBugs2 findBugs = new FindBugs2();
		findBugs.setDetectorFactoryCollection(detectorFactoryCollection);
		
		// Parse command line and configure the engine
		TextUICommandLine commandLine = new TextUICommandLine(detectorFactoryCollection);
		commandLine.parse(args);
		commandLine.configureEngine(findBugs);
		
		// Away we go!
		findBugs.execute();
	}
}
