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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
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
	private UserPreferences userPrefs;
	private final IProject project;
	private final IJavaProject javaProject;

	/**
	 * Creates a new worker.
	 *
	 * @param project The <b>java</b> project to work on.
	 * @param monitor A progress monitor.
	 * @throws CoreException if the given project is not a java project, does not exists
	 * or is not open
	 */
	public FindBugsWorker(IProject project, IProgressMonitor monitor) throws CoreException {
		super();
		this.project = project;
		this.javaProject = JavaCore.create(project);
		if (javaProject == null || !javaProject.exists()
				|| !javaProject.getProject().isOpen()) {
			throw new CoreException(new Status(IStatus.ERROR, FindbugsPlugin.PLUGIN_ID,
					IStatus.ERROR, "Java project is not open or does not exist: "
							+ project, null));
		}
		this.monitor = monitor;
		try {
			this.userPrefs = FindbugsPlugin.getUserPreferences(project);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Could not get selected detectors for project");
			throw e;
		}
	}

	/**
	 * Run FindBugs on the given collection of resources from same project (note: This is
	 * currently not thread-safe)
	 *
	 * @param resources
	 *            files or directories which should be on the project classpath. All
	 *            resources must belong to the same project, and no one of the elements
	 *            can contain another one. Ergo, if the list contains a project itself,
	 *            then it must have only one element.
	 * @throws CoreException
	 */
	public void work(List<IResource> resources) throws CoreException {
		if (resources == null || resources.isEmpty()) {
			FindbugsPlugin.getDefault().logInfo("No resources to analyse for project " + project);
			return;
		}
		if (DEBUG) {
			System.out.println(resources);
		}

		// clear markers
		clearMarkers(resources);

		final Project findBugsProject = new Project();
		findBugsProject.setProjectName(javaProject.getElementName());
		final Reporter bugReporter = new Reporter(javaProject, monitor);
		bugReporter.setPriorityThreshold(userPrefs.getUserDetectorThreshold());

		FindBugs.setHome(FindbugsPlugin.getFindBugsEnginePluginLocation());

		Map<IPath, IPath> outLocations = createOutputLocations();
		Map<File, String> outputFiles = new HashMap<File, String>();
		// collect all related class file patterns for analysis
		collectClassFilesPatterns(resources, outLocations, outputFiles);

		// find and add all the class files in the output directories
		configureOutputFiles(findBugsProject, outputFiles);

		String[] classPathEntries = createAuxClasspath();
		// add to findbugs classpath
		for (String entry : classPathEntries) {
			findBugsProject.addAuxClasspathEntry(entry);
		}
		final FindBugs2 findBugs = new FindBugs2();
		findBugs.setNoClassOk(true);
		findBugs.setBugReporter(bugReporter);
		findBugs.setProject(findBugsProject);
		findBugs.setProgressCallback(bugReporter);
		findBugs.setDetectorFactoryCollection(DetectorFactoryCollection.instance());

		// configure detectors.
		findBugs.setUserPreferences(userPrefs);

		// configure extended preferences
		findBugs.setAnalysisFeatureSettings(userPrefs.getAnalysisFeatureSettings());
		configureExtendedProps(userPrefs.getIncludeFilterFiles(), findBugs, true, false);
		configureExtendedProps(userPrefs.getExcludeFilterFiles(), findBugs, false, false);
		configureExtendedProps(userPrefs.getExcludeBugsFiles(), findBugs, false, true);

		runFindBugs(findBugs);

		// Merge new results into existing results
		// if the argument is project, then it's not incremental
		boolean incremental = !(resources.get(0) instanceof IProject);
		updateBugCollection(findBugsProject, bugReporter, incremental);
	}


	/**
	 * Load existing FindBugs xml report for the given collection of files.
	 * @param fileName xml file name to load bugs from
	 * @throws CoreException
	 */
	public void loadXml(String fileName) throws CoreException {
		if(fileName == null) {
			return;
		}
		// clear markers
		clearMarkers(null);

		final Project findBugsProject = new Project();
		final Reporter bugReporter = new Reporter(javaProject, monitor);
		bugReporter.setPriorityThreshold(userPrefs.getUserDetectorThreshold());

		reportFromXml(fileName, findBugsProject, bugReporter);
		// Merge new results into existing results.
		updateBugCollection(findBugsProject, bugReporter, false);
	}

	/**
	 * Clear assotiated markers
	 * @param files
	 */
	private void clearMarkers(List<IResource> files) throws CoreException {
		if(files == null) {
			project.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
			return;
		}
		Iterator<IResource> iter = files.iterator();
		while (iter.hasNext()) {
			// get the resource
			IResource res = iter.next();
			if (res == null) {
				continue;
			}
			res.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
		}
	}

	/**
	 * Updates given outputFiles map with class name patterns matching given java source
	 * names
	 * @param resources java sources
	 * @param outLocations key is src root, value is output location
	 * @param outputFiles key is output directory path, value are class name patterns for
	 * this directory
	 */
	private void collectClassFilesPatterns(List<IResource> resources,
			Map<IPath, IPath> outLocations,	Map<File, String> outputFiles) {

		for (IResource resource : resources) {
			if (Util.isJavaFile(resource)) {
				// this is a .java file, so get the corresponding .class file(s)
				addClassPatternsFromFile((IFile) resource, outLocations, outputFiles);
			} else if(resource instanceof IFolder) {
				addClassPatternsFromFolder((IFolder)resource, outLocations, outputFiles);
			} else if(resource instanceof IProject) {
				addClassPatternsFromProject(outLocations, outputFiles);
			}
		}
	}

	private void addClassPatternsFromFolder(IFolder folder, Map<IPath, IPath> outLocations,
			Map<File, String> outputFiles) {
		IPath path = folder.getLocation();
		IPath srcRoot = getMatchingSourceRoot(path, outLocations);
		if(srcRoot == null) {
			return;
		}
		IPath outputRoot = outLocations.get(srcRoot);
		int firstSegments = path.matchingFirstSegments(srcRoot);
		// add relative path to the output path
		IPath out = outputRoot.append(path.removeFirstSegments(firstSegments));
		outputFiles.put(out.toFile(), ".*\\.class");
	}

	private void addClassPatternsFromProject(Map<IPath, IPath> outLocations,
			Map<File, String> outputFiles) {
		// just add anything in all project output folders
		Set<Entry<IPath,IPath>> entrySet = outLocations.entrySet();
		for (Entry<IPath, IPath> entry : entrySet) {
			outputFiles.put(entry.getValue().toFile(), ".*\\.class");
		}
	}

	private void addClassPatternsFromFile(IFile file,
			Map<IPath, IPath> outLocations, Map<File, String> outputFiles) {
		IPath path = file.getLocation();
		IPath srcRoot = getMatchingSourceRoot(path, outLocations);
		IPath outputRoot = outLocations.get(srcRoot);
		int firstSegments = path.matchingFirstSegments(srcRoot);
		// add relative path to the output path
		IPath out = outputRoot.append(path.removeFirstSegments(firstSegments));
		String fileName = path.removeFileExtension().lastSegment();
		String namePattern = fileName + "\\.class|" + fileName + "\\$.*\\.class";
		namePattern = addSecondaryTypesToPattern(file, fileName, namePattern);
		File directory = out.removeLastSegments(1).toFile();
		String filesPattern = outputFiles.get(directory);
		if(filesPattern != null) {
			// add new to existing class patterns
			namePattern += "|" + filesPattern;
		}
		// add parent folder and regexp for file names
		outputFiles.put(directory, namePattern);
	}

	/**
	 * Add secondary types patterns (not nested in the type itself but contained in the
	 * java file)
	 *
	 * @param fileName java file name (not path!) without .java suffix
	 * @param classNamePattern non null pattern for all matching .class file names
	 * @return modified classNamePattern, if there are more then one type defined in the
	 * java file
	 */
	private String addSecondaryTypesToPattern(IFile file, String fileName,
			String classNamePattern) {
		ICompilationUnit cu = JavaCore.createCompilationUnitFrom(file);
		if (cu == null) {
			FindbugsPlugin.getDefault().logError(
					"NULL compilation unit for " + file
							+ ", FB analysis might  be incomplete for included types");
			return classNamePattern;
		}
		try {
			IType[] types = cu.getTypes();
			if (types.length > 1) {
				for (IType type : types) {
					if (fileName.equals(type.getElementName())) {
						// "usual" type with the same name: we have it already
						continue;
					}
					classNamePattern = classNamePattern + "|" + type.getElementName()
							+ "\\.class|" + type.getElementName() + "\\$.*\\.class";
				}
			}
		} catch (JavaModelException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Cannot get types from compilation unit: " + cu);
		}
		return classNamePattern;
	}

	/**
	 * @param srcPath
	 * @param outLocations key is the source root, value is output folder
	 * @return source root folder matching (parent of) given path
	 */
	private IPath getMatchingSourceRoot(IPath srcPath, Map<IPath, IPath> outLocations) {
		Set<Entry<IPath, IPath>> outEntries = outLocations.entrySet();
		IPath result = null;
		int maxSegments = 0;
		for (Entry<IPath, IPath> entry : outEntries) {
			IPath srcRoot = entry.getKey();
			int firstSegments = srcPath.matchingFirstSegments(srcRoot);
			if(firstSegments > maxSegments && firstSegments == srcRoot.segmentCount()) {
				maxSegments = firstSegments;
				result = srcRoot;
			}
		}
		return result;
	}

	/**
	 * this method will block current thread until the findbugs is running
	 * @param findBugs fb engine, which will be <b>disposed</b> after the analysis is done
	 */
	private void runFindBugs(final FindBugs2 findBugs) {
		// bug 1828973 was fixed by findbugs engine, so that workaround to start the
		// analysis in an extra thread is not more needed
		try {
			// Perform the analysis! (note: This is not thread-safe)
			findBugs.execute();
		} catch (InterruptedException e) {
			if (DEBUG) {
				FindbugsPlugin.getDefault().logException(e, "Worker interrupted");
			}
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs analysis");
		} finally {
			findBugs.dispose();
		}
	}

	/**
	 * Add the output .class files to the FindBugs project in the directories
	 * that match the corresponding patterns in the <code>Map</code> outputFiles.
	 *
	 * @param findBugsProject   findbugs <code>Project</code>
	 * @param outputFiles   Map containing output directories and patterns for .class files.
	 * The map content will be deleted after this call
	 */
	private void configureOutputFiles(Project findBugsProject, Map<File, String> outputFiles) {
		for (Map.Entry<File, String> entry: outputFiles.entrySet()) {
			File source = entry.getKey();
			Pattern classNamesPattern = Pattern.compile(entry.getValue());
			ResourceUtils.addFiles(findBugsProject, source, classNamesPattern);
		}
		// clear the map for GC
		outputFiles.clear();
	}

	/**
	 * Update the BugCollection for the project.
	 *
	 * @param findBugsProject FindBugs project representing analyzed classes
	 * @param bugReporter     Reporter used to collect the new warnings
	 * @throws CoreException
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void updateBugCollection(Project findBugsProject, Reporter bugReporter,
			boolean incremental) {
		try {
			SortedBugCollection oldBugCollection = FindbugsPlugin.getBugCollection(project,
					monitor);
			SortedBugCollection newBugCollection = bugReporter.getBugCollection();

			SortedBugCollection resultCollection = mergeBugCollections(oldBugCollection,
					newBugCollection, incremental);
			resultCollection.setTimestamp(System.currentTimeMillis());

			FindbugsPlugin.storeBugCollection(project, resultCollection, findBugsProject,
					monitor);
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs results update");
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs results update");
		}
	}

	private SortedBugCollection mergeBugCollections(SortedBugCollection firstCollection,
			SortedBugCollection secondCollection, boolean incremental) {
		Update update = new Update();
		return (SortedBugCollection) (update.mergeCollections(firstCollection,
				secondCollection, false, incremental));
	}

	private void configureExtendedProps(Collection<String> filterFiles,
			IFindBugsEngine findBugs, boolean include, boolean bugsFilter) {
		for (String fileName : filterFiles) {
			IFile file = project.getFile(fileName);
			if (file.exists()) {
				String filterName = file.getLocation().toOSString();
				try {
					if (bugsFilter) {
						findBugs.excludeBaselineBugs(filterName);
					} else {
						findBugs.addFilter(filterName, include);
					}
				} catch (RuntimeException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Error while loading filter \"" + filterName + "\".");
				} catch (DocumentException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Error while loading excluded bugs \"" + filterName + "\".");
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e,
							"Error while reading filter \"" + filterName + "\".");
				}
			} else {
				FindbugsPlugin.getDefault().logWarning(
						"Include filter not found: " + fileName);
			}
		}
	}

	/**
	 * @return array with required class directories / libs on the classpath
	 */
	private String[] createAuxClasspath() {
		String[] classPath = PDEClassPathGenerator.computeClassPath(javaProject);
		return classPath;
	}

	/**
	 * @return map of all source folders to output folders, for current java project,
	 *         where both are represented by absolute IPath objects
	 *
	 * @throws CoreException
	 */
	private Map<IPath, IPath> createOutputLocations() throws CoreException {

		Map<IPath, IPath> srcToOutputMap = new HashMap<IPath, IPath>();

		// get the default location => relative to wsp
		IPath defaultOutputLocation = ResourceUtils.relativeToAbsolute(javaProject.getOutputLocation());

		// path to the project without project name itself
		IClasspathEntry entries[] = javaProject.getResolvedClasspath(true);
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry classpathEntry = entries[i];
			if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath outputLocation = ResourceUtils.getOutputLocation(classpathEntry,
						defaultOutputLocation);
				// TODO not clear if it is absolute in workspace or in global FS
				IPath srcLocation = ResourceUtils.relativeToAbsolute(classpathEntry.getPath());
				srcToOutputMap.put(srcLocation, outputLocation);
			}
		}

		return srcToOutputMap;
	}

	private void reportFromXml(final String xmlFileName, final Project findBugsProject,
			final Reporter bugReporter) {
		if (!"".equals(xmlFileName)) {
			try {
				FileInputStream input = new FileInputStream(xmlFileName);
				bugReporter.reportBugsFromXml(input, findBugsProject);
			} catch (FileNotFoundException e) {
				FindbugsPlugin.getDefault().logException(e,
						"XML file not found: " + xmlFileName);
			} catch (DocumentException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Invalid XML file: " + xmlFileName);
			} catch (IOException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Error loading FindBugs results xml file: "  + xmlFileName);
			}
		}
	}
}
