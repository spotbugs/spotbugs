/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.builder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;

import de.tobject.findbugs.EclipseGuiCallback;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.util.Util.StopTimer;
import de.tobject.findbugs.view.FindBugsConsole;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.workflow.Update;

/**
 * Execute FindBugs on a collection of Java resources in a project.
 *
 * @author Peter Friese
 * @author Andrei Loskutov
 * @version 2.0
 * @since 26.09.2003
 */
public class FindBugsWorker {

    /** Controls debugging. */
    public static boolean DEBUG;

    private final IProgressMonitor monitor;

    private final UserPreferences userPrefs;

    private final IProject project;

    private final IJavaProject javaProject;

    private StopTimer st;

    private final IResource resource;


    public FindBugsWorker(IResource resource, IProgressMonitor monitor) throws CoreException {
        super();
        this.resource = resource;
        this.project = resource.getProject();
        this.javaProject = JavaCore.create(project);
        if (javaProject == null || !javaProject.exists() || !javaProject.getProject().isOpen()) {
            throw new CoreException(FindbugsPlugin.createErrorStatus("Java project is not open or does not exist: " + project,
                    null));
        }
        this.monitor = monitor;
        // clone is required because we rewrite project relative references to absolute
        this.userPrefs = FindbugsPlugin.getUserPreferences(project).clone();
    }

    /**
     * Creates a new worker.
     *
     * @param project
     *            The <b>java</b> project to work on.
     * @param monitor
     *            A progress monitor.
     * @throws CoreException
     *             if the given project is not a java project, does not exists
     *             or is not open
     */
    public FindBugsWorker(IProject project, IProgressMonitor monitor) throws CoreException {
        this((IResource) project, monitor);
    }

    /**
     * Run FindBugs on the given collection of resources from same project
     * (note: This is currently not thread-safe)
     *
     * @param resources
     *            files or directories which should be on the project classpath.
     *            All resources must belong to the same project, and no one of
     *            the elements can contain another one. Ergo, if the list
     *            contains a project itself, then it must have only one element.
     * @throws CoreException
     */
    public void work(List<WorkItem> resources) throws CoreException {
        if (resources == null || resources.isEmpty()) {
            if (DEBUG) {
                FindbugsPlugin.getDefault().logInfo("No resources to analyse for project " + project);
            }
            return;
        }
        if (DEBUG) {
            System.out.println(resources);
        }
        st = new StopTimer();
        st.newPoint("initPlugins");

        // make sure it's initialized
        FindbugsPlugin.applyCustomDetectors(false);

        st.newPoint("clearMarkers");

        // clear markers
        clearMarkers(resources);

        st.newPoint("configureOutputFiles");

        final Project findBugsProject = new Project();
        findBugsProject.setProjectName(javaProject.getElementName());
        final Reporter bugReporter = new Reporter(javaProject, findBugsProject, monitor);
        if (FindBugsConsole.getConsole() != null) {
            bugReporter.setReportingStream(FindBugsConsole.getConsole().newOutputStream());
        }
        bugReporter.setPriorityThreshold(userPrefs.getUserDetectorThreshold());

        FindBugs.setHome(FindbugsPlugin.getFindBugsEnginePluginLocation());

        Map<IPath, IPath> outLocations = createOutputLocations();

        // collect all related class/jar/war etc files for analysis
        collectClassFiles(resources, outLocations, findBugsProject);

        // attach source directories (can be used by some detectors, see
        // SwitchFallthrough)
        configureSourceDirectories(findBugsProject, outLocations);

        if (findBugsProject.getFileCount() == 0) {
            if (DEBUG) {
                FindbugsPlugin.getDefault().logInfo("No resources to analyse for project " + project);
            }
            return;
        }

        st.newPoint("createAuxClasspath");

        String[] classPathEntries = createAuxClasspath();
        // add to findbugs classpath
        for (String entry : classPathEntries) {
            findBugsProject.addAuxClasspathEntry(entry);
        }

        st.newPoint("configureProps");
        IPreferenceStore store = FindbugsPlugin.getPluginPreferences(project);
        boolean cacheClassData = store.getBoolean(FindBugsConstants.KEY_CACHE_CLASS_DATA);

        final FindBugs2 findBugs = new FindBugs2Eclipse(project, cacheClassData, bugReporter);
        findBugs.setNoClassOk(true);
        findBugs.setProject(findBugsProject);
        findBugs.setBugReporter(bugReporter);
        findBugs.setProgressCallback(bugReporter);

        findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

        // configure detectors.
        userPrefs.setIncludeFilterFiles(relativeToAbsolute(userPrefs.getIncludeFilterFiles()));
        userPrefs.setExcludeFilterFiles(relativeToAbsolute(userPrefs.getExcludeFilterFiles()));
        userPrefs.setExcludeBugsFiles(relativeToAbsolute(userPrefs.getExcludeBugsFiles()));
        findBugs.setUserPreferences(userPrefs);

        // configure extended preferences
        findBugs.setAnalysisFeatureSettings(userPrefs.getAnalysisFeatureSettings());
        findBugs.setMergeSimilarWarnings(userPrefs.getMergeSimilarWarnings());

        if (cacheClassData) {
            FindBugs2Eclipse.checkClassPathChanges(findBugs.getProject().getAuxClasspathEntryList(), project);
        }

        st.newPoint("runFindBugs");
        if (DEBUG) {
            FindbugsPlugin.log("Running SpotBugs");
        }

        try {
            runFindBugs(findBugs);
        } finally {
            FindBugs2.resetPriorityAdjustments();
        }

        if (DEBUG) {
            FindbugsPlugin.log("Done running SpotBugs");
        }

        // Merge new results into existing results
        // if the argument is project, then it's not incremental
        boolean incremental = !(resources.get(0) instanceof IProject);
        updateBugCollection(findBugsProject, bugReporter, incremental);
        st.newPoint("done");
        st = null;
        monitor.done();
    }



    private void configureSourceDirectories(Project findBugsProject, Map<IPath, IPath> outLocations) {
        Set<IPath> srcDirs = outLocations.keySet();
        findBugsProject.addSourceDirs(srcDirs.stream().map(IPath::toOSString).collect(Collectors.toList()));
    }

    /**
     * Load existing FindBugs xml report for the given collection of files.
     *
     * @param fileName
     *            xml file name to load bugs from
     * @throws CoreException
     */
    public void loadXml(String fileName) throws CoreException {
        if (fileName == null) {
            return;
        }
        st = new StopTimer();

        // clear markers
        clearMarkers(null);

        final Project findBugsProject = new Project();
        final Reporter bugReporter = new Reporter(javaProject, findBugsProject, monitor);
        bugReporter.setPriorityThreshold(userPrefs.getUserDetectorThreshold());

        reportFromXml(fileName, findBugsProject, bugReporter);
        // Merge new results into existing results.
        updateBugCollection(findBugsProject, bugReporter, false);
        monitor.done();
    }

    /**
     * Clear associated markers
     *
     * @param files
     */
    private void clearMarkers(List<WorkItem> files) throws CoreException {
        if (files == null) {
            project.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
            return;
        }
        for (WorkItem item : files) {
            if (item != null) {
                item.clearMarkers();
            }
        }
    }

    /**
     * Updates given outputFiles map with class name patterns matching given
     * java source names
     *
     * @param resources
     *            java sources
     * @param outLocations
     *            key is src root, value is output location this directory
     * @param fbProject
     */
    private void collectClassFiles(List<WorkItem> resources, Map<IPath, IPath> outLocations, Project fbProject) {
        for (WorkItem workItem : resources) {
            workItem.addFilesToProject(fbProject, outLocations);
        }
    }

    /**
     * this method will block current thread until the findbugs is running
     *
     * @param findBugs
     *            fb engine, which will be <b>disposed</b> after the analysis is
     *            done
     */
    private static void runFindBugs(final FindBugs2 findBugs) {
        if (DEBUG) {
            FindbugsPlugin.log("Running findbugs in thread " + Thread.currentThread().getName());
        }
        System.setProperty("findbugs.progress", "true");
        try {
            // Perform the analysis! (note: This is not thread-safe)
            findBugs.execute();
        } catch (Exception e) {
            if (isInterrupted(e)) {
                if (DEBUG) {
                    FindbugsPlugin.getDefault().logException(e, "Worker interrupted");
                }
            } else {
                FindbugsPlugin.getDefault().logException(e, "Error performing SpotBugs analysis");
            }
        } finally {
            findBugs.dispose();
        }

    }

    private static boolean isInterrupted(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return true;
        }
        Throwable cause = e.getCause();
        if (cause instanceof InterruptedException) {
            return true;
        }
        if (cause instanceof ExecutionException) {
            ExecutionException ee = (ExecutionException) cause;
            if (ee.getCause() instanceof InterruptedException) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the BugCollection for the project.
     *
     * @param findBugsProject
     *            FindBugs project representing analyzed classes
     * @param bugReporter
     *            Reporter used to collect the new warnings
     */
    private void updateBugCollection(Project findBugsProject, Reporter bugReporter, boolean incremental) {
        SortedBugCollection newBugCollection = bugReporter.getBugCollection();
        try {
            st.newPoint("getBugCollection");
            SortedBugCollection oldBugCollection = FindbugsPlugin.getBugCollection(project, monitor);

            st.newPoint("mergeBugCollections");
            SortedBugCollection resultCollection = mergeBugCollections(oldBugCollection, newBugCollection, incremental);
            resultCollection.getProject().setGuiCallback(new EclipseGuiCallback(project));
            resultCollection.setTimestamp(System.currentTimeMillis());

            // will store bugs in the default FB file + Eclipse project session props
            st.newPoint("storeBugCollection");
            FindbugsPlugin.storeBugCollection(project, resultCollection, monitor);
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Error performing SpotBugs results update");
        }

        // will store bugs as markers in Eclipse workspace
        st.newPoint("createMarkers");
        MarkerUtil.createMarkers(javaProject, newBugCollection, resource, monitor);
    }

    private SortedBugCollection mergeBugCollections(SortedBugCollection firstCollection, SortedBugCollection secondCollection,
            boolean incremental) {
        Update update = new Update();
        // TODO copyDeadBugs must be true, otherwise incremental compile leads to
        // unknown bug instances appearing (merged collection doesn't contain all bugs)
        boolean copyDeadBugs = incremental;
        return (SortedBugCollection) (update.mergeCollections(firstCollection, secondCollection, copyDeadBugs, incremental));
    }

    private Map<String, Boolean> relativeToAbsolute(Map<String, Boolean> map) {
        Map<String, Boolean> resultMap = new TreeMap<>();
        for (Entry<String, Boolean> entry : map.entrySet()) {
            if (!entry.getValue().booleanValue()) {
                continue;
            }
            String filePath = entry.getKey();
            IPath path = getFilterPath(filePath, project);
            if (!path.toFile().exists()) {
                FindbugsPlugin.getDefault().logWarning("Filter not found: " + filePath);
                continue;
            }
            String filterName = path.toOSString();
            resultMap.put(filterName, Boolean.TRUE);
        }
        return resultMap;
    }

    /**
     * Checks the given path and convert it to absolute path if it is specified
     * relative to the given project or workspace
     *
     * @param filePath
     *            project relative OR workspace relative OR absolute OS file
     *            path (1.3.8+ version)
     * @param project
     *            might be null (only for workspace relative or absolute paths)
     * @return absolute path which matches given relative or absolute path,
     *         never null
     */
    public static IPath getFilterPath(String filePath, IProject project) {
        IPath path = new Path(filePath);
        if (path.isAbsolute()) {
            return path;
        }
        if (project != null) {
            // try first project relative location
            IPath newPath = project.getLocation().append(path);
            if (newPath.toFile().exists()) {
                return newPath;
            }
        }

        // try to resolve relative to workspace (if we use workspace properties
        // for project)
        IPath wspLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath newPath = wspLocation.append(path);
        if (newPath.toFile().exists()) {
            return newPath;
        }

        // something which we have no idea what it can be (or missing/wrong file
        // path)
        return path;
    }

    /**
     * Checks the given absolute path and convert it to relative path if it is
     * relative to the given project or workspace. This representation can be
     * used to store filter paths in user preferences file
     *
     * @param filePath
     *            absolute OS file path
     * @param project
     *            might be null
     * @return filter file path as stored in preferences which matches given
     *         path
     */
    public static IPath toFilterPath(String filePath, IProject project) {
        IPath path = new Path(filePath);
        IPath commonPath;
        if (project != null) {
            commonPath = project.getLocation();
            IPath relativePath = getRelativePath(path, commonPath);
            if (!relativePath.equals(path)) {
                return relativePath;
            }
        }
        commonPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        return getRelativePath(path, commonPath);
    }

    /**
     * @param filePath
     *            path (eventually absolute) to a file
     * @param commonPath
     *            absolute path of some common location
     * @return path relative to common root if given path is contained under the
     *         common directory, otherwise unchanged path
     */
    private static IPath getRelativePath(IPath filePath, IPath commonPath) {
        if (!filePath.isAbsolute()) {
            return filePath;
        }
        // since Equinox 3.5 we can use IPath.makeRelativeTo(IPath)
        return filePath.makeRelativeTo(commonPath);
    }

    /**
     * @return array with required class directories / libs on the classpath
     */
    private String[] createAuxClasspath() {
        return PDEClassPathGenerator.computeClassPath(javaProject);
    }

    /**
     * @return map of all source folders to output folders, for current java
     *         project, where both are represented by absolute IPath objects
     *
     * @throws CoreException
     */
    private Map<IPath, IPath> createOutputLocations() throws CoreException {

        Map<IPath, IPath> srcToOutputMap = new HashMap<>();

        // get the default location => relative to wsp
        IPath defaultOutputLocation = ResourceUtils.relativeToAbsolute(javaProject.getOutputLocation());
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        // path to the project without project name itself
        IClasspathEntry[] entries = javaProject.getResolvedClasspath(true);
        for (IClasspathEntry classpathEntry : entries) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath outputLocation = ResourceUtils.getOutputLocation(classpathEntry, defaultOutputLocation);
                if (outputLocation == null) {
                    continue;
                }
                IResource cpeResource = root.findMember(classpathEntry.getPath());
                // patch from 2891041: do not analyze derived "source" folders
                // because they probably contain auto-generated classes
                if (cpeResource != null && cpeResource.isDerived()) {
                    continue;
                }
                // TODO not clear if it is absolute in workspace or in global FS
                IPath srcLocation = ResourceUtils.relativeToAbsolute(classpathEntry.getPath());
                if (srcLocation != null) {
                    srcToOutputMap.put(srcLocation, outputLocation);
                }
            }
        }

        return srcToOutputMap;
    }

    private void reportFromXml(final String xmlFileName, final Project findBugsProject, final Reporter bugReporter) {
        if (!"".equals(xmlFileName)) {
            try (InputStream input = Files.newInputStream(java.nio.file.Path.of(xmlFileName))) {
                bugReporter.reportBugsFromXml(input, findBugsProject);
            } catch (DocumentException e) {
                FindbugsPlugin.getDefault().logException(e, "Invalid XML file: " + xmlFileName);
            } catch (IOException e) {
                FindbugsPlugin.getDefault().logException(e, "Error loading SpotBugs results xml file: " + xmlFileName);
            }
        }
    }
}
