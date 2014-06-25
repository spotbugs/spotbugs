/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2008 University of Maryland
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.bcel.classfile.ClassFormatException;
import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.asm.FBClassReader;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.SourceInfoMap;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.bugReporter.BugReporterDecorator;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngineRegistrar;
import edu.umd.cs.findbugs.classfile.IClassFactory;
import edu.umd.cs.findbugs.classfile.IClassObserver;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IClassPathBuilder;
import edu.umd.cs.findbugs.classfile.ICodeBase;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.impl.ClassFactory;
import edu.umd.cs.findbugs.config.AnalysisFeatureSetting;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.detect.NoteSuppressedWarnings;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.log.YourKitController;
import edu.umd.cs.findbugs.plan.AnalysisPass;
import edu.umd.cs.findbugs.plan.ExecutionPlan;
import edu.umd.cs.findbugs.plan.OrderingConstraintException;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.TopologicalSort.OutEdges;

/**
 * FindBugs driver class. Orchestrates the analysis of a project, collection of
 * results, etc.
 *
 * @author David Hovemeyer
 */
public class FindBugs2 implements IFindBugsEngine {
    private static final boolean LIST_ORDER = SystemProperties.getBoolean("findbugs.listOrder");

    private static final boolean VERBOSE = SystemProperties.getBoolean("findbugs.verbose");

    public static final boolean DEBUG = VERBOSE || SystemProperties.getBoolean("findbugs.debug");

    public static final boolean PROGRESS = DEBUG || SystemProperties.getBoolean("findbugs.progress");

    private static final boolean SCREEN_FIRST_PASS_CLASSES = SystemProperties.getBoolean("findbugs.screenFirstPass");

    public static final String PROP_FINDBUGS_HOST_APP = "findbugs.hostApp";
    public static final String PROP_FINDBUGS_HOST_APP_VERSION = "findbugs.hostAppVersion";

    private int rankThreshold;

    private List<IClassObserver> classObserverList;

    private BugReporter bugReporter;

    private ErrorCountingBugReporter errorCountingBugReporter;

    private Project project;

    private IClassFactory classFactory;

    private IClassPath classPath;

    private List<ClassDescriptor> appClassList;

    private Collection<ClassDescriptor> referencedClassSet;

    private DetectorFactoryCollection detectorFactoryCollection;

    private ExecutionPlan executionPlan;

    private final YourKitController yourkitController = new YourKitController();

    private String currentClassName;

    private FindBugsProgress progress;

    private IClassScreener classScreener;

    private final AnalysisOptions analysisOptions = new AnalysisOptions(true);

    /**
     * Constructor.
     */
    public FindBugs2() {
        this.classObserverList = new LinkedList<IClassObserver>();
        this.analysisOptions.analysisFeatureSettingList = FindBugs.DEFAULT_EFFORT;
        this.progress = new NoOpFindBugsProgress();

        // By default, do not exclude any classes via the class screener
        this.classScreener = new IClassScreener() {
            @Override
            public boolean matches(String fileName) {
                return true;
            }

            @Override
            public boolean vacuous() {
                return true;
            }
        };

        String hostApp = System.getProperty(PROP_FINDBUGS_HOST_APP);
        String hostAppVersion = null;
        if (hostApp == null || hostApp.trim().length() <= 0) {
            hostApp = "FindBugs TextUI";
            hostAppVersion = System.getProperty(PROP_FINDBUGS_HOST_APP_VERSION);
        }
        if (hostAppVersion == null) {
            hostAppVersion = "";
        }
        Version.registerApplication(hostApp, hostAppVersion);

        // By default, we do not want to scan nested archives
        this.analysisOptions.scanNestedArchives = false;
        // bug 2815983: no bugs are reported anymore
        // there is no info which value should be default, so using the any one
        rankThreshold = BugRanker.VISIBLE_RANK_MAX;
    }

    /**
     * Set the detector factory collection to be used by this FindBugs2 engine.
     * This method should be called before the execute() method is called.
     *
     * @param detectorFactoryCollection
     *            The detectorFactoryCollection to set.
     */
    @Override
    public void setDetectorFactoryCollection(DetectorFactoryCollection detectorFactoryCollection) {
        this.detectorFactoryCollection = detectorFactoryCollection;
    }

    /**
     * Execute the analysis. For obscure reasons, CheckedAnalysisExceptions are
     * re-thrown as IOExceptions. However, these can only happen during the
     * setup phase where we scan codebases for classes.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void execute() throws IOException, InterruptedException {

        if (FindBugs.isNoAnalysis()) {
            throw new UnsupportedOperationException("This FindBugs invocation was started without analysis capabilities");
        }

        Profiler profiler = bugReporter.getProjectStats().getProfiler();

        try {
            try {
                // Get the class factory for creating classpath/codebase/etc.
                classFactory = ClassFactory.instance();

                // The class path object
                createClassPath();

                progress.reportNumberOfArchives(project.getFileCount() + project.getNumAuxClasspathEntries());
                profiler.start(this.getClass());

                // The analysis cache object
                createAnalysisCache();

                // Create BCEL compatibility layer
                createAnalysisContext(project, appClassList, analysisOptions.sourceInfoFileName);

                // Discover all codebases in classpath and
                // enumerate all classes (application and non-application)
                buildClassPath();


                // Build set of classes referenced by application classes
                buildReferencedClassSet();

                // Create BCEL compatibility layer
                setAppClassList(appClassList);

                // Configure the BugCollection (if we are generating one)
                FindBugs.configureBugCollection(this);

                // Enable/disabled relaxed reporting mode
                FindBugsAnalysisFeatures.setRelaxedMode(analysisOptions.relaxedReportingMode);
                FindBugsDisplayFeatures.setAbridgedMessages(analysisOptions.abridgedMessages);

                // Configure training databases
                FindBugs.configureTrainingDatabases(this);

                // Configure analysis features
                configureAnalysisFeatures();

                // Create the execution plan (which passes/detectors to execute)
                createExecutionPlan();

                for (Plugin p : detectorFactoryCollection.plugins()) {
                    for (ComponentPlugin<BugReporterDecorator> brp
                            : p.getComponentPlugins(BugReporterDecorator.class)) {
                        if (brp.isEnabledByDefault() && !brp.isNamed(explicitlyDisabledBugReporterDecorators)
                                || brp.isNamed(explicitlyEnabledBugReporterDecorators)) {
                            bugReporter = BugReporterDecorator.construct(brp, bugReporter);
                        }
                    }
                }
                if (!classScreener.vacuous()) {
                    bugReporter = new DelegatingBugReporter(bugReporter) {

                        @Override
                        public void reportBug(@Nonnull BugInstance bugInstance) {
                            String className = bugInstance.getPrimaryClass().getClassName();
                            String resourceName = className.replace('.', '/') + ".class";
                            if (classScreener.matches(resourceName)) {
                                this.getDelegate().reportBug(bugInstance);
                            }
                        }
                    };
                }

                if (executionPlan.isActive(NoteSuppressedWarnings.class)) {
                    SuppressionMatcher m = AnalysisContext.currentAnalysisContext().getSuppressionMatcher();
                    bugReporter = new FilterBugReporter(bugReporter, m, false);
                }

                if (appClassList.size() == 0) {
                    Map<String, ICodeBaseEntry> codebase = classPath.getApplicationCodebaseEntries();
                    if (analysisOptions.noClassOk) {
                        System.err.println("No classfiles specified; output will have no warnings");
                    } else if  (codebase.isEmpty()) {
                        throw new IOException("No files to analyze could be opened");
                    } else {
                        throw new NoClassesFoundToAnalyzeException(classPath);
                    }
                }

                // Analyze the application
                analyzeApplication();
            } catch (CheckedAnalysisException e) {
                IOException ioe = new IOException("IOException while scanning codebases");
                ioe.initCause(e);
                throw ioe;
            } catch (OutOfMemoryError e) {
                System.err.println("Out of memory");
                System.err.println("Total memory: " + Runtime.getRuntime().maxMemory() / 1000000 + "M");
                System.err.println(" free memory: " + Runtime.getRuntime().freeMemory() / 1000000 + "M");

                for (String s : project.getFileList()) {
                    System.err.println("Analyzed: " + s);
                }
                for (String s : project.getAuxClasspathEntryList()) {
                    System.err.println("     Aux: " + s);
                }
                throw e;
            } finally {
                clearCaches();
                profiler.end(this.getClass());
                profiler.report();
            }
        } catch (IOException e) {
            bugReporter.reportQueuedErrors();
            throw e;
        }
    }

    /**
     * Protected to allow Eclipse plugin remember some cache data for later reuse
     */
    protected void clearCaches() {
        DescriptorFactory.clearInstance();
        ObjectTypeFactory.clearInstance();
        TypeQualifierApplications.clearInstance();
        TypeQualifierAnnotation.clearInstance();
        TypeQualifierValue.clearInstance();
        // Make sure the codebases on the classpath are closed
        AnalysisContext.removeCurrentAnalysisContext();
        Global.removeAnalysisCacheForCurrentThread();
        if (classPath != null) {
            classPath.close();
        }
    }

    /**
     * To avoid cyclic cross-references and allow GC after engine is not more
     * needed. (used by Eclipse plugin)
     */
    public void dispose() {
        if (executionPlan != null) {
            executionPlan.dispose();
        }
        if (appClassList != null) {
            appClassList.clear();
        }
        if (classObserverList != null) {
            classObserverList.clear();
        }
        if (referencedClassSet != null) {
            referencedClassSet.clear();
        }
        analysisOptions.analysisFeatureSettingList = null;
        bugReporter = null;
        classFactory = null;
        classPath = null;
        classScreener = null;
        detectorFactoryCollection = null;
        executionPlan = null;
        progress = null;
        project = null;
        analysisOptions.userPreferences = null;
    }

    @Override
    public BugReporter getBugReporter() {
        return bugReporter;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void addClassObserver(IClassObserver classObserver) {
        classObserverList.add(classObserver);
    }

    @Override
    public void addFilter(String filterFileName, boolean include) throws IOException, FilterException {
        bugReporter = FindBugs.configureFilter(bugReporter, filterFileName, include);
    }

    @Override
    public void excludeBaselineBugs(String baselineBugs) throws IOException, DocumentException {
        bugReporter = FindBugs.configureBaselineFilter(bugReporter, baselineBugs);
    }

    @Override
    public void enableTrainingInput(String trainingInputDir) {
        this.analysisOptions.trainingInputDir = trainingInputDir;
    }

    @Override
    public void enableTrainingOutput(String trainingOutputDir) {
        this.analysisOptions.trainingOutputDir = trainingOutputDir;
    }

    @Override
    public int getBugCount() {
        return errorCountingBugReporter.getBugCount();
    }

    @Override
    public String getCurrentClass() {
        return currentClassName;
    }

    @Override
    public int getErrorCount() {
        return errorCountingBugReporter.getErrorCount();
    }

    @Override
    public int getMissingClassCount() {
        return errorCountingBugReporter.getMissingClassCount();
    }

    @Override
    public String getReleaseName() {
        return analysisOptions.releaseName;
    }

    @Override
    public String getProjectName() {
        return analysisOptions.projectName;
    }

    @Override
    public void setProjectName(String name) {
        analysisOptions.projectName = name;
    }

    @Override
    public void setAnalysisFeatureSettings(AnalysisFeatureSetting[] settingList) {
        this.analysisOptions.analysisFeatureSettingList = settingList;
    }

    @Override
    public void setBugReporter(BugReporter bugReporter) {
        this.bugReporter = this.errorCountingBugReporter = new ErrorCountingBugReporter(bugReporter);

        addClassObserver(bugReporter);
    }

    @Override
    public void setClassScreener(IClassScreener classScreener) {
        this.classScreener = classScreener;
    }

    @Override
    public void setProgressCallback(FindBugsProgress progressCallback) {
        this.progress = progressCallback;
    }

    @Override
    public void setProject(Project project) {
        this.project = project;
    }

    @Override
    public void setRelaxedReportingMode(boolean relaxedReportingMode) {
        this.analysisOptions.relaxedReportingMode = relaxedReportingMode;
    }

    @Override
    public void setReleaseName(String releaseName) {
        this.analysisOptions.releaseName = releaseName;
    }

    @Override
    public void setSourceInfoFile(String sourceInfoFile) {
        this.analysisOptions.sourceInfoFileName = sourceInfoFile;
    }

    @Override
    public void setUserPreferences(UserPreferences userPreferences) {
        this.analysisOptions.userPreferences = userPreferences;
        // TODO should set it here too, but gui2 seems to have issues with it
        // setAnalysisFeatureSettings(userPreferences.getAnalysisFeatureSettings());

        configureFilters(userPreferences);
    }

    protected void configureFilters(UserPreferences userPreferences) {
        IllegalArgumentException deferredError = null;
        Set<Entry<String, Boolean>> excludeBugFiles = userPreferences.getExcludeBugsFiles().entrySet();
        for (Entry<String, Boolean> entry : excludeBugFiles) {
            if (entry.getValue() == null || !entry.getValue()) {
                continue;
            }
            try {
                excludeBaselineBugs(entry.getKey());
            } catch (Exception e) {
                String message = "Unable to read filter: " + entry.getKey() + " : " + e.getMessage();
                if (getBugReporter() != null) {
                    getBugReporter().logError(message, e);
                } else if (deferredError == null){
                    deferredError = new IllegalArgumentException(message, e);
                }
            }
        }
        Set<Entry<String, Boolean>> includeFilterFiles = userPreferences.getIncludeFilterFiles().entrySet();
        for (Entry<String, Boolean> entry : includeFilterFiles) {
            if (entry.getValue() == null || !entry.getValue()) {
                continue;
            }
            try {
                addFilter(entry.getKey(), true);
            } catch (Exception e) {
                String message = "Unable to read filter: " + entry.getKey() + " : " + e.getMessage();
                if (getBugReporter() != null) {
                    getBugReporter().logError(message, e);
                } else if (deferredError == null){
                    deferredError = new IllegalArgumentException(message, e);
                }
            }
        }
        Set<Entry<String, Boolean>> excludeFilterFiles = userPreferences.getExcludeFilterFiles().entrySet();

        for (Entry<String, Boolean> entry : excludeFilterFiles) {
            Boolean value = entry.getValue();
            if (value == null || !value) {
                continue;
            }
            String excludeFilterFile = entry.getKey();
            try {
                addFilter(excludeFilterFile, false);
            } catch (Exception e) {
                String message = "Unable to read filter: " + excludeFilterFile + " : " + e.getMessage();
                if (getBugReporter() != null) {
                    getBugReporter().logError(message, e);
                } else if (deferredError == null){
                    deferredError = new IllegalArgumentException(message, e);
                }
            }
        }
        if (deferredError != null) {
            throw deferredError;
        }
    }

    @Override
    public boolean emitTrainingOutput() {
        return analysisOptions.trainingOutputDir != null;
    }

    @Override
    public UserPreferences getUserPreferences() {
        return analysisOptions.userPreferences;
    }

    /**
     * Create the classpath object.
     */
    private void createClassPath() {
        classPath = classFactory.createClassPath();
    }

    @Override
    public String getTrainingInputDir() {
        return analysisOptions.trainingInputDir;
    }

    @Override
    public String getTrainingOutputDir() {
        return analysisOptions.trainingOutputDir;
    }

    @Override
    public boolean useTrainingInput() {
        return analysisOptions.trainingInputDir != null;
    }

    @Override
    public void setScanNestedArchives(boolean scanNestedArchives) {
        this.analysisOptions.scanNestedArchives = scanNestedArchives;
    }

    @Override
    public void setNoClassOk(boolean noClassOk) {
        this.analysisOptions.noClassOk = noClassOk;
    }

    /**
     * Create the analysis cache object and register it for current execution thread.
     * <p>
     * This method is protected to allow clients override it and possibly reuse
     * some previous analysis data (for Eclipse interactive re-build)
     *
     * @throws IOException
     *             if error occurs registering analysis engines in a plugin
     */
    protected IAnalysisCache createAnalysisCache() throws IOException {
        IAnalysisCache analysisCache = ClassFactory.instance().createAnalysisCache(classPath, bugReporter);

        // Register the "built-in" analysis engines
        registerBuiltInAnalysisEngines(analysisCache);

        // Register analysis engines in plugins
        registerPluginAnalysisEngines(detectorFactoryCollection, analysisCache);

        // Install the DetectorFactoryCollection as a database
        analysisCache.eagerlyPutDatabase(DetectorFactoryCollection.class, detectorFactoryCollection);

        Global.setAnalysisCacheForCurrentThread(analysisCache);
        return analysisCache;
    }
    /**
     * Register the "built-in" analysis engines with given IAnalysisCache.
     *
     * @param analysisCache
     *            an IAnalysisCache
     */
    public static void registerBuiltInAnalysisEngines(IAnalysisCache analysisCache) {
        new edu.umd.cs.findbugs.classfile.engine.EngineRegistrar().registerAnalysisEngines(analysisCache);
        new edu.umd.cs.findbugs.classfile.engine.asm.EngineRegistrar().registerAnalysisEngines(analysisCache);
        new edu.umd.cs.findbugs.classfile.engine.bcel.EngineRegistrar().registerAnalysisEngines(analysisCache);
    }

    /**
     * Register all of the analysis engines defined in the plugins contained in
     * a DetectorFactoryCollection with an IAnalysisCache.
     *
     * @param detectorFactoryCollection
     *            a DetectorFactoryCollection
     * @param analysisCache
     *            an IAnalysisCache
     * @throws IOException
     */
    public static void registerPluginAnalysisEngines(DetectorFactoryCollection detectorFactoryCollection,
            IAnalysisCache analysisCache) throws IOException {
        for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext();) {
            Plugin plugin = i.next();

            Class<? extends IAnalysisEngineRegistrar> engineRegistrarClass = plugin.getEngineRegistrarClass();
            if (engineRegistrarClass != null) {
                try {
                    IAnalysisEngineRegistrar engineRegistrar = engineRegistrarClass.newInstance();
                    engineRegistrar.registerAnalysisEngines(analysisCache);
                } catch (InstantiationException e) {
                    IOException ioe = new IOException("Could not create analysis engine registrar for plugin "
                            + plugin.getPluginId());
                    ioe.initCause(e);
                    throw ioe;
                } catch (IllegalAccessException e) {
                    IOException ioe = new IOException("Could not create analysis engine registrar for plugin "
                            + plugin.getPluginId());
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }
    }

    /**
     * Build the classpath from project codebases and system codebases.
     *
     * @throws InterruptedException
     *             if the analysis thread is interrupted
     * @throws IOException
     *             if an I/O error occurs
     * @throws CheckedAnalysisException
     */
    private void buildClassPath() throws InterruptedException, IOException, CheckedAnalysisException {
        IClassPathBuilder builder = classFactory.createClassPathBuilder(bugReporter);

        {
            HashSet<String> seen = new HashSet<String>();
            for (String path : project.getFileArray()) {
                if (seen.add(path)) {
                    builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), true);
                }
            }
            for (String path : project.getAuxClasspathEntryList()) {
                if (seen.add(path)) {
                    builder.addCodeBase(classFactory.createFilesystemCodeBaseLocator(path), false);
                }
            }
        }

        builder.scanNestedArchives(analysisOptions.scanNestedArchives);

        builder.build(classPath, progress);

        appClassList = builder.getAppClassList();

        if (PROGRESS) {
            System.out.println(appClassList.size() + " classes scanned");
        }

        // If any of the application codebases contain source code,
        // add them to the source path.
        // Also, use the last modified time of application codebases
        // to set the project timestamp.
        for (Iterator<? extends ICodeBase> i = classPath.appCodeBaseIterator(); i.hasNext();) {
            ICodeBase appCodeBase = i.next();

            if (appCodeBase.containsSourceFiles()) {
                String pathName = appCodeBase.getPathName();
                if (pathName != null) {
                    project.addSourceDir(pathName);
                }
            }

            project.addTimestamp(appCodeBase.getLastModifiedTime());
        }

    }

    private void buildReferencedClassSet() throws InterruptedException {
        // XXX: should drive progress dialog (scanning phase)?

        if (PROGRESS) {
            System.out.println("Adding referenced classes");
        }
        Set<String> referencedPackageSet = new HashSet<String>();

        LinkedList<ClassDescriptor> workList = new LinkedList<ClassDescriptor>();
        workList.addAll(appClassList);

        Set<ClassDescriptor> seen = new HashSet<ClassDescriptor>();
        Set<ClassDescriptor> appClassSet = new HashSet<ClassDescriptor>(appClassList);

        Set<ClassDescriptor> badAppClassSet = new HashSet<ClassDescriptor>();
        HashSet<ClassDescriptor> knownDescriptors = new HashSet<ClassDescriptor>(DescriptorFactory.instance()
                .getAllClassDescriptors());
        int count = 0;
        Set<ClassDescriptor> addedToWorkList = new HashSet<ClassDescriptor>(appClassList);

        // add fields
        //noinspection ConstantIfStatement
        /*
        if (false)
            for (ClassDescriptor classDesc : appClassList) {
                try {
                    XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);
                    for (XField f : classNameAndInfo.getXFields()) {
                        String sig = f.getSignature();
                        ClassDescriptor d = DescriptorFactory.createClassDescriptorFromFieldSignature(sig);
                        if (d != null && addedToWorkList.add(d))
                            workList.addLast(d);
                    }
                } catch (RuntimeException e) {
                    bugReporter.logError("Error scanning " + classDesc + " for referenced classes", e);
                    if (appClassSet.contains(classDesc)) {
                        badAppClassSet.add(classDesc);
                    }
                } catch (MissingClassException e) {
                    // Just log it as a missing class
                    bugReporter.reportMissingClass(e.getClassDescriptor());
                    if (appClassSet.contains(classDesc)) {
                        badAppClassSet.add(classDesc);
                    }
                }
            }
         */
        while (!workList.isEmpty()) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            ClassDescriptor classDesc = workList.removeFirst();

            if (seen.contains(classDesc)) {
                continue;
            }
            seen.add(classDesc);

            if (!knownDescriptors.contains(classDesc)) {
                count++;
                if (PROGRESS && count % 5000 == 0) {
                    System.out.println("Adding referenced class " + classDesc);
                }
            }

            referencedPackageSet.add(classDesc.getPackageName());

            // Get list of referenced classes and add them to set.
            // Add superclasses and superinterfaces to worklist.
            try {
                XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);

                ClassDescriptor superclassDescriptor = classNameAndInfo.getSuperclassDescriptor();
                if (superclassDescriptor != null && addedToWorkList.add(superclassDescriptor)) {
                    workList.addLast(superclassDescriptor);
                }

                for (ClassDescriptor ifaceDesc : classNameAndInfo.getInterfaceDescriptorList()) {
                    if (addedToWorkList.add(ifaceDesc)) {
                        workList.addLast(ifaceDesc);
                    }
                }

                ClassDescriptor enclosingClass = classNameAndInfo.getImmediateEnclosingClass();
                if (enclosingClass != null && addedToWorkList.add(enclosingClass)) {
                    workList.addLast(enclosingClass);
                }

            } catch (RuntimeException e) {
                bugReporter.logError("Error scanning " + classDesc + " for referenced classes", e);
                if (appClassSet.contains(classDesc)) {
                    badAppClassSet.add(classDesc);
                }
            } catch (MissingClassException e) {
                // Just log it as a missing class
                bugReporter.reportMissingClass(e.getClassDescriptor());
                if (appClassSet.contains(classDesc)) {
                    badAppClassSet.add(classDesc);
                }
            } catch (CheckedAnalysisException e) {
                // Failed to scan a referenced class --- just log the error and
                // continue
                bugReporter.logError("Error scanning " + classDesc + " for referenced classes", e);
                if (appClassSet.contains(classDesc)) {
                    badAppClassSet.add(classDesc);
                }
            }
        }
        // Delete any application classes that could not be read
        appClassList.removeAll(badAppClassSet);
        DescriptorFactory.instance().purge(badAppClassSet);

        for (ClassDescriptor d : DescriptorFactory.instance().getAllClassDescriptors()) {
            referencedPackageSet.add(d.getPackageName());
        }
        referencedClassSet = new ArrayList<ClassDescriptor>(DescriptorFactory.instance().getAllClassDescriptors());

        // Based on referenced packages, add any resolvable package-info classes
        // to the set of referenced classes.
        if (PROGRESS) {
            referencedPackageSet.remove("");
            System.out.println("Added " + count + " referenced classes");
            System.out.println("Total of " + referencedPackageSet.size() + " packages");
            for (ClassDescriptor d : referencedClassSet) {
                System.out.println("  " + d);
            }
        }
    }

    public List<ClassDescriptor> sortByCallGraph(Collection<ClassDescriptor> classList, OutEdges<ClassDescriptor> outEdges) {
        List<ClassDescriptor> evaluationOrder = edu.umd.cs.findbugs.util.TopologicalSort.sortByCallGraph(classList, outEdges);
        edu.umd.cs.findbugs.util.TopologicalSort.countBadEdges(evaluationOrder, outEdges);
        return evaluationOrder;

    }

    public static void clearAnalysisContext() {
        AnalysisContext.removeCurrentAnalysisContext();
    }

    /**
     * Create the AnalysisContext that will serve as the BCEL-compatibility
     * layer over the AnalysisCache.
     *
     * @param project
     *            The project
     * @param appClassList
     *            list of ClassDescriptors identifying application classes
     * @param sourceInfoFileName
     *            name of source info file (null if none)
     */
    public static void createAnalysisContext(Project project, List<ClassDescriptor> appClassList,
            @CheckForNull String sourceInfoFileName) throws  IOException {
        AnalysisContext analysisContext = new AnalysisContext(project);

        // Make this the current analysis context
        AnalysisContext.setCurrentAnalysisContext(analysisContext);

        // Make the AnalysisCache the backing store for
        // the BCEL Repository
        analysisContext.clearRepository();

        // If needed, load SourceInfoMap
        if (sourceInfoFileName != null) {
            SourceInfoMap sourceInfoMap = analysisContext.getSourceInfoMap();
            sourceInfoMap.read(new FileInputStream(sourceInfoFileName));
        }
    }

    public static void setAppClassList(List<ClassDescriptor> appClassList)  {
        AnalysisContext analysisContext = AnalysisContext
                .currentAnalysisContext();

        analysisContext.setAppClassList(appClassList);
    }

    /**
     * Configure analysis feature settings.
     */
    private void configureAnalysisFeatures() {
        for (AnalysisFeatureSetting setting : analysisOptions.analysisFeatureSettingList) {
            setting.configure(AnalysisContext.currentAnalysisContext());
        }
        AnalysisContext.currentAnalysisContext().setBoolProperty(AnalysisFeatures.MERGE_SIMILAR_WARNINGS,
                analysisOptions.mergeSimilarWarnings);
    }

    /**
     * Create an execution plan.
     *
     * @throws OrderingConstraintException
     *             if the detector ordering constraints are inconsistent
     */
    private void createExecutionPlan() throws OrderingConstraintException {
        executionPlan = new ExecutionPlan();

        // Use user preferences to decide which detectors are enabled.
        DetectorFactoryChooser detectorFactoryChooser = new DetectorFactoryChooser() {
            HashSet<DetectorFactory> forcedEnabled = new HashSet<DetectorFactory>();

            @Override
            public boolean choose(DetectorFactory factory) {
                boolean result = FindBugs.isDetectorEnabled(FindBugs2.this, factory, rankThreshold) || forcedEnabled.contains(factory);
                if (ExecutionPlan.DEBUG) {
                    System.out.printf("  %6s %s %n", result, factory.getShortName());
                }
                return result;
            }

            @Override
            public void enable(DetectorFactory factory) {
                forcedEnabled.add(factory);
                factory.setEnabledButNonReporting(true);
            }

        };
        executionPlan.setDetectorFactoryChooser(detectorFactoryChooser);

        if (ExecutionPlan.DEBUG) {
            System.out.println("rank threshold is " + rankThreshold);
        }
        // Add plugins
        for (Iterator<Plugin> i = detectorFactoryCollection.pluginIterator(); i.hasNext();) {
            Plugin plugin = i.next();
            if (DEBUG) {
                System.out.println("Adding plugin " + plugin.getPluginId() + " to execution plan");
            }
            executionPlan.addPlugin(plugin);
        }

        // Build the execution plan
        executionPlan.build();

        // Stash the ExecutionPlan in the AnalysisCache.
        Global.getAnalysisCache().eagerlyPutDatabase(ExecutionPlan.class, executionPlan);

        if (PROGRESS) {
            System.out.println(executionPlan.getNumPasses() + " passes in execution plan");
        }
    }

    /**
     * Analyze the classes in the application codebase.
     */
    private void analyzeApplication() throws InterruptedException {
        int passCount = 0;
        Profiler profiler = bugReporter.getProjectStats().getProfiler();
        profiler.start(this.getClass());
        AnalysisContext.currentXFactory().canonicalizeAll();
        try {
            boolean multiplePasses = executionPlan.getNumPasses() > 1;
            if (executionPlan.getNumPasses() == 0) {
                throw new AssertionError("no analysis passes");
            }
            int[] classesPerPass = new int[executionPlan.getNumPasses()];
            classesPerPass[0] = referencedClassSet.size();
            for (int i = 0; i < classesPerPass.length; i++) {
                classesPerPass[i] = i == 0 ? referencedClassSet.size() : appClassList.size();
            }
            progress.predictPassCount(classesPerPass);
            XFactory factory = AnalysisContext.currentXFactory();
            Collection<ClassDescriptor> badClasses = new LinkedList<ClassDescriptor>();
            for (ClassDescriptor desc : referencedClassSet) {
                try {
                    XClass info = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc);
                    factory.intern(info);
                } catch (CheckedAnalysisException e) {
                    AnalysisContext.logError("Couldn't get class info for " + desc, e);
                    badClasses.add(desc);
                } catch (RuntimeException e) {
                    AnalysisContext.logError("Couldn't get class info for " + desc, e);
                    badClasses.add(desc);
                }
            }
            if (!badClasses.isEmpty()) {
                referencedClassSet = new LinkedHashSet<ClassDescriptor>(referencedClassSet);
                referencedClassSet.removeAll(badClasses);
            }

            long startTime = System.currentTimeMillis();
            bugReporter.getProjectStats().setReferencedClasses(referencedClassSet.size());
            for (Iterator<AnalysisPass> passIterator = executionPlan.passIterator(); passIterator.hasNext();) {
                AnalysisPass pass = passIterator.next();
                yourkitController.advanceGeneration("Pass " + passCount);
                // The first pass is generally a non-reporting pass which
                // gathers information about referenced classes.
                boolean isNonReportingFirstPass = multiplePasses && passCount == 0;

                // Instantiate the detectors
                Detector2[] detectorList = pass.instantiateDetector2sInPass(bugReporter);

                // If there are multiple passes, then on the first pass,
                // we apply detectors to all classes referenced by the
                // application classes.
                // On subsequent passes, we apply detector only to application
                // classes.
                Collection<ClassDescriptor> classCollection = (isNonReportingFirstPass) ? referencedClassSet : appClassList;
                AnalysisContext.currentXFactory().canonicalizeAll();
                if (PROGRESS || LIST_ORDER) {
                    System.out.printf("%6d : Pass %d: %d classes%n", (System.currentTimeMillis() - startTime)/1000, passCount,  classCollection.size());
                    if (DEBUG) {
                        XFactory.profile();
                    }
                }
                if (!isNonReportingFirstPass) {
                    OutEdges<ClassDescriptor> outEdges = new OutEdges<ClassDescriptor>() {

                        @Override
                        public Collection<ClassDescriptor> getOutEdges(ClassDescriptor e) {
                            try {
                                XClass classNameAndInfo = Global.getAnalysisCache().getClassAnalysis(XClass.class, e);
                                return classNameAndInfo.getCalledClassDescriptors();
                            } catch (CheckedAnalysisException e2) {
                                AnalysisContext.logError("error while analyzing " + e.getClassName(), e2);
                                return Collections.emptyList();

                            }
                        }
                    };

                    classCollection = sortByCallGraph(classCollection, outEdges);
                }
                if (LIST_ORDER) {
                    System.out.println("Analysis order:");
                    for (ClassDescriptor c : classCollection) {
                        System.out.println("  " + c);
                    }
                }
                AnalysisContext currentAnalysisContext = AnalysisContext.currentAnalysisContext();
                currentAnalysisContext.updateDatabases(passCount);

                progress.startAnalysis(classCollection.size());
                int count = 0;
                Global.getAnalysisCache().purgeAllMethodAnalysis();
                Global.getAnalysisCache().purgeClassAnalysis(FBClassReader.class);
                for (ClassDescriptor classDescriptor : classCollection) {
                    long classStartNanoTime = 0;
                    if (PROGRESS) {
                        classStartNanoTime = System.nanoTime();
                        System.out.printf("%6d %d/%d  %d/%d %s%n", (System.currentTimeMillis() - startTime)/1000,
                                passCount, executionPlan.getNumPasses(), count,
                                classCollection.size(), classDescriptor);
                    }
                    count++;
                    if (!isNonReportingFirstPass && count % 1000 == 0) {
                        yourkitController.advanceGeneration(String.format("Pass %d.%02d", passCount, count/1000));
                    }


                    // Check to see if class is excluded by the class screener.
                    // In general, we do not want to screen classes from the
                    // first pass, even if they would otherwise be excluded.
                    if ((SCREEN_FIRST_PASS_CLASSES || !isNonReportingFirstPass)
                            && !classScreener.matches(classDescriptor.toResourceName())) {
                        if (DEBUG) {
                            System.out.println("*** Excluded by class screener");
                        }
                        continue;
                    }
                    boolean isHuge = currentAnalysisContext.isTooBig(classDescriptor);
                    if (isHuge && currentAnalysisContext.isApplicationClass(classDescriptor)) {
                        bugReporter.reportBug(new BugInstance("SKIPPED_CLASS_TOO_BIG", Priorities.NORMAL_PRIORITY)
                        .addClass(classDescriptor));
                    }
                    currentClassName = ClassName.toDottedClassName(classDescriptor.getClassName());
                    notifyClassObservers(classDescriptor);
                    profiler.startContext(currentClassName);
                    currentAnalysisContext.setClassBeingAnalyzed(classDescriptor);

                    try {
                        for (Detector2 detector : detectorList) {
                            if (Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                            if (isHuge && !FirstPassDetector.class.isAssignableFrom(detector.getClass())) {
                                continue;
                            }
                            if (DEBUG) {
                                System.out.println("Applying " + detector.getDetectorClassName() + " to " + classDescriptor);
                                // System.out.println("foo: " +
                                // NonReportingDetector.class.isAssignableFrom(detector.getClass())
                                // + ", bar: " + detector.getClass().getName());
                            }
                            try {
                                profiler.start(detector.getClass());
                                detector.visitClass(classDescriptor);
                            } catch (ClassFormatException e) {
                                logRecoverableException(classDescriptor, detector, e);
                            } catch (MissingClassException e) {
                                Global.getAnalysisCache().getErrorLogger().reportMissingClass(e.getClassDescriptor());
                            } catch (CheckedAnalysisException e) {
                                logRecoverableException(classDescriptor, detector, e);
                            } catch (RuntimeException e) {
                                logRecoverableException(classDescriptor, detector, e);
                            } finally {
                                profiler.end(detector.getClass());
                            }
                        }
                    } finally {

                        progress.finishClass();
                        profiler.endContext(currentClassName);
                        currentAnalysisContext.clearClassBeingAnalyzed();
                        if (PROGRESS) {
                            long usecs = (System.nanoTime() - classStartNanoTime)/1000;
                            if (usecs > 15000) {
                                int classSize = currentAnalysisContext.getClassSize(classDescriptor);
                                long speed = usecs /classSize;
                                if (speed > 15) {
                                    System.out.printf("  %6d usecs/byte  %6d msec  %6d bytes  %d pass %s%n", speed, usecs/1000, classSize, passCount,
                                            classDescriptor);
                                }
                            }

                        }
                    }
                }

                if (!passIterator.hasNext()) {
                    yourkitController.captureMemorySnapshot();
                }
                // Call finishPass on each detector
                for (Detector2 detector : detectorList) {
                    detector.finishPass();
                }

                progress.finishPerClassAnalysis();

                passCount++;
            }


        } finally {

            bugReporter.finish();
            bugReporter.reportQueuedErrors();
            profiler.end(this.getClass());
            if (PROGRESS) {
                System.out.println("Analysis completed");
            }
        }

    }

    /**
     * Notify all IClassObservers that we are visiting given class.
     *
     * @param classDescriptor
     *            the class being visited
     */
    private void notifyClassObservers(ClassDescriptor classDescriptor) {
        for (IClassObserver observer : classObserverList) {
            observer.observeClass(classDescriptor);
        }
    }

    /**
     * Report an exception that occurred while analyzing a class with a
     * detector.
     *
     * @param classDescriptor
     *            class being analyzed
     * @param detector
     *            detector doing the analysis
     * @param e
     *            the exception
     */
    private void logRecoverableException(ClassDescriptor classDescriptor, Detector2 detector, Throwable e) {
        bugReporter.logError(
                "Exception analyzing " + classDescriptor.toDottedClassName() + " using detector "
                        + detector.getDetectorClassName(), e);
    }

    public static void main(String[] args) throws Exception {
        // Sanity-check the loaded BCEL classes
        if (!CheckBcel.check()) {
            System.exit(1);
        }

        // Create FindBugs2 engine
        FindBugs2 findBugs = new FindBugs2();

        // Parse command line and configure the engine
        TextUICommandLine commandLine = new TextUICommandLine();
        FindBugs.processCommandLine(commandLine, args, findBugs);


        boolean justPrintConfiguration = commandLine.justPrintConfiguration();
        if (justPrintConfiguration || commandLine.justPrintVersion()) {
            Version.printVersion(justPrintConfiguration);

            return;
        }
        // Away we go!


        FindBugs.runMain(findBugs, commandLine);

    }


    @Override
    public void setAbridgedMessages(boolean xmlWithAbridgedMessages) {
        analysisOptions.abridgedMessages = xmlWithAbridgedMessages;
    }

    @Override
    public void setMergeSimilarWarnings(boolean mergeSimilarWarnings) {
        this.analysisOptions.mergeSimilarWarnings = mergeSimilarWarnings;
    }

    @Override
    public void setApplySuppression(boolean applySuppression) {
        this.analysisOptions.applySuppression = applySuppression;
    }

    @Override
    public void setRankThreshold(int rankThreshold) {
        this.rankThreshold = rankThreshold;
    }

    @Override
    public void finishSettings() {
        if (analysisOptions.applySuppression) {
            bugReporter = new FilterBugReporter(bugReporter, getProject().getSuppressionFilter(), false);
        }
    }

    @Nonnull
    Set<String> explicitlyEnabledBugReporterDecorators = Collections.emptySet();

    @Nonnull
    Set<String> explicitlyDisabledBugReporterDecorators = Collections.emptySet();

    @Override
    public void setBugReporterDecorators(Set<String> explicitlyEnabled, Set<String> explicitlyDisabled) {
        explicitlyEnabledBugReporterDecorators = explicitlyEnabled;
        explicitlyDisabledBugReporterDecorators = explicitlyDisabled;
    }

}
