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
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.dom4j.DocumentException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.reporter.Reporter;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.filter.FilterException;
import edu.umd.cs.findbugs.workflow.Update;

/**
 * Execute FindBugs on a collection of Java resources in a project.
 *
 * @author Peter Friese
 * @version 1.0
 * @since 26.09.2003
 */
public class FindBugsWorker {

	/** Controls debugging. */
	public static boolean DEBUG;

	private IProgressMonitor monitor;
	private UserPreferences userPrefs;
	private IProject project;

	/**
	 * Creates a new worker.
	 *
	 * @param project The project to work on.
	 * @param monitor A progress monitor.
	 */
	public FindBugsWorker(IProject project, IProgressMonitor monitor) {
		super();
		this.project = project;
		this.monitor = monitor;
		try {
			this.userPrefs = FindbugsPlugin.getUserPreferences(project);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get selected detectors for project");
		}
	}

	/**
	 * Run FindBugs on the given collection of files. (note: This is not thread-safe.)
	 *
	 * @param files A collection of {@link IResource}s.
	 * @param incremental TODO
	 * @throws CoreException
	 */
	public void work(Collection<IFile> files, boolean incremental) throws CoreException {
		work2(files, incremental, false, null);
	}

	public void loadXml(Collection<IFile> files, boolean incremental, String fileName) throws CoreException {
		work2(files, incremental, true, fileName);
	}
	public void work2(Collection<IFile> files, boolean incremental, final boolean loadXml, final String xmlFileName) throws CoreException {
		if (files == null) {
			FindbugsPlugin.getDefault().logError("No files to build");
			return;
		}

		String findBugsHome = FindbugsPlugin.getFindBugsEnginePluginLocation();
		if (DEBUG) {
			FindbugsPlugin.getDefault().logInfo("Looking for detectors in: " + findBugsHome); //$NON-NLS-1$
		}

		// FIXME hardcoded findbugs.home property
		System.setProperty("findbugs.home", findBugsHome); //$NON-NLS-1$

		Set<IPath> outLocations = createOutputLocations();

		final Project findBugsProject = new Project();
		Iterator<IFile> iter = files.iterator();
		Map<File, String> outputFiles = new HashMap<File, String>();
		while (iter.hasNext()) {
			// get the resource
			IFile res = iter.next();
			if (res == null) {
				continue;
			}
			if (Util.isJavaArtifact(res)) {
				res.deleteMarkers(
					FindBugsMarker.NAME,
					true,
					IResource.DEPTH_INFINITE);
			}

			IPath location = res.getLocation();
			if (Util.isClassFile(res) && containsIn(outLocations, location)) {
				// add this file to the work list:
				String fileName = location.toOSString();

				res.refreshLocal(IResource.DEPTH_INFINITE, null);
				if (DEBUG) {
					System.out.println(
							"Resource: " + fileName //$NON-NLS-1$
							+ ": in sync: " + res.isSynchronized(IResource.DEPTH_INFINITE)); //$NON-NLS-1$
				}
				findBugsProject.addFile(fileName);
			}
			else if (Util.isJavaFile(res)) {
				// this is a .java file, so get the corresponding .class file(s)
				// get the compilation unit for this file
				ICompilationUnit cu = JavaCore.createCompilationUnitFrom(res);
				if (cu == null) {
					if (DEBUG) {
						FindbugsPlugin.getDefault().logError("NULL Compilation Unit for "+res.getName());
					}
					continue; // ignore and continue
				}
				// find the output location for this CompilationUnit
				IPackageFragmentRoot pkgRoot = (IPackageFragmentRoot) cu.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
				if (pkgRoot == null) {
					if (DEBUG) {
						FindbugsPlugin.getDefault().logError("NULL Package Root for: "+res.getName());
					}
					continue; // ignore and continue
				}
				IClasspathEntry cpe;
				// fix for Eclipse 3.3: if RawClasspathEntry is missing, 3.2 just
				// returns null but 3.3 throws JavaModelException
				try {
					cpe = pkgRoot.getRawClasspathEntry();
				} catch (JavaModelException e) {
					if (DEBUG) {
						FindbugsPlugin.getDefault().logError("EXCEPTION for: "
								+ res.getName() + ".getRawClasspathEntry()");
					}
					continue; // ignore and continue
				}
				if (cpe == null) {
					if (DEBUG) {
						FindbugsPlugin.getDefault().logError("NULL Classpath Entry for: "+res.getName());
					}
					continue; // ignore and continue
				}
				IPath outLocation = getAbsoluteOutputLocation(pkgRoot, cpe);
				// get the workspace relative path for this .java file
				IPath relativePath = getRelativeFilePath(res, cpe);
				IPath pkgPath = relativePath.removeLastSegments(1);
				String fName = relativePath.lastSegment();
				fName = fName.substring(0, fName.lastIndexOf('.'));
				// find the class and inner classes for this .java file
				IPath clzLocation = outLocation.append(pkgPath);
				String exp = fName+"\\.class"+"|"+fName+"\\$.*\\.class";
				File clzDir = clzLocation.toFile();
				// check if the directory exists in the output locations
				String oldExp = outputFiles.get(clzDir);
				if (oldExp != null) {
					exp = oldExp + "|" + exp;
				}
				outputFiles.put(clzDir, exp);
			}
		}

		// find and add all the class files in the output directories
		addOutputFiles(findBugsProject, outputFiles);
		// clear the map for GC
		outputFiles.clear();

		final Reporter bugReporter = new Reporter(this.project, this.monitor, findBugsProject);
		bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);

		String[] classPathEntries = createClassPathEntries();
		// add to findbugs classpath
		for (int i = 0; i < classPathEntries.length; i++) {
			findBugsProject.addAuxClasspathEntry(classPathEntries[i]);
		}

		final IFindBugsEngine findBugs;

		final FindBugs2 engine = new FindBugs2();
		engine.setBugReporter(bugReporter);
		engine.setProject(findBugsProject);
		engine.setProgressCallback(bugReporter);
		engine.setDetectorFactoryCollection(DetectorFactoryCollection.instance());
		findBugs = engine;

		// configure detectors.
		findBugs.setUserPreferences(this.userPrefs);
		configureExtended(findBugs);



		Runnable r = new Runnable() {
			public void run() {
				try {
					if(loadXml){
						if (DEBUG) {
							System.out.println("Loading XML...");
						}
						if (!(xmlFileName.equals(""))) {
							try {
								FileInputStream input = new FileInputStream(xmlFileName);
								bugReporter.reportBugsFromXml(input, findBugsProject);
							} catch (FileNotFoundException e) {
								FindbugsPlugin.getDefault().logException(e,
										"XML file not found: " + xmlFileName);
							} catch (DocumentException e) {
								FindbugsPlugin.getDefault().logException(e,
										"Invalid XML file: " + xmlFileName);
							}
						}
					} else {
						if (DEBUG) {
							System.out.println("Finding bugs");
						}
						findBugs.execute();
					}
				} catch (InterruptedException e) {
					if (DEBUG) {
						FindbugsPlugin.getDefault().logException(e, "Worker interrupted");
					}
					Thread.currentThread().interrupt();
					// @see IncrementalProjectBuilder.build
					//throw new OperationCanceledException("FindBugs operation cancelled by user");
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs analysis");
				}
			}
		};

		/*
		 * see bug 1828973: https://sourceforge.net/tracker/?func=detail&atid=614693&aid=1828973&group_id=96405
		 * FindBugs2 engine stores the analysis caches etc in the ThreadLocal and
		 * InheritableThreadLocal fileds, which are assumed to be garbage collected
		 * together with the execution thread on the execution finish.
		 * Unfortunately, Eclipse uses thread pools for jobs, which means, that the
		 * execution threads would stay forever and the analysis cache would never be released...
		 * To avoid the OutOfMemory error, we have to run execution in the new thread
		 * which would be destroyed after the execution (also releasing memory occupied
		 * by all created ThreadLocal* fields).
		 */
		Thread t = new Thread(r);
		// Perform the analysis! (note: This is not thread-safe.)
		t.start();
		try{
			t.join();
		} catch (InterruptedException e) {
			if (DEBUG) {
				FindbugsPlugin.getDefault().logException(e, "Worker interrupted");
			}
			// @see IncrementalProjectBuilder.build
			//throw new OperationCanceledException("FindBugs operation cancelled by user");
		}

		try {
			// Merge new results into existing results.
			updateBugCollection(findBugsProject, bugReporter, incremental);
		} catch (IOException e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs results update");
		} catch (DocumentException e) {
			FindbugsPlugin.getDefault().logException(e, "Error performing FindBugs results update");
		}

		// Redisplay markers (this makes sure version information can get in)
		Iterator<IFile> it = files.iterator();
		if(it.hasNext()){
			IResource res = it.next();
			MarkerUtil.redisplayMarkersWithoutProgressDialog(res.getProject());
		}
	}

	/**
	 * Add the output .class files to the FindBugs project in the directories
	 * that match the corresponding patterns in the <code>Map</code> outputFiles.
	 *
	 * @param findBugsProject   findbugs <code>Project</code>
	 * @param outputFiles   Map containing output directories and patterns for .class files.
	 */
	private void addOutputFiles(Project findBugsProject, Map<File, String> outputFiles) {
		for (Map.Entry<File, String> entry: outputFiles.entrySet()) {
			File clzDir = entry.getKey();
			final Pattern pat = Pattern.compile(entry.getValue());
			if (clzDir.exists() && clzDir.isDirectory()) {
				File[] clzs = clzDir.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return pat.matcher(name).find();
					}
				});
				// add the clzs to the list of files to be analysed
				for (File cl: clzs) {
					findBugsProject.addFile(cl.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * Get the workspace relative file path for the given .java file resource.
	 *
	 * @param res   Resource to lookup in the workspace
	 * @param cpe   Classpath entry where the resource resides.
	 * @return workspace relative file path for the .java file.
	 */
	private IPath getRelativeFilePath(IResource res, IClasspathEntry cpe) {
		IPath cpePath = cpe.getPath();
		IPath javaFilePath = res.getFullPath();
		IPath relativePath = javaFilePath.removeFirstSegments(cpePath.matchingFirstSegments(javaFilePath));
		return relativePath;
	}

	/**
	 * Get the absolute path in the local file system for the specified <code>IClasspathEntry</code>.
	 *
	 * @param pkgRoot   Root package fragment for the classpath entry.
	 * @param cpe       Classpath entry for the package.
	 * @return absolute path in the local file system for the classpath entry.
	 * @throws JavaModelException if the default location is not specified.
	 */
	private IPath getAbsoluteOutputLocation(IPackageFragmentRoot pkgRoot, IClasspathEntry cpe) throws JavaModelException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();


		IPath outLocation = cpe.getOutputLocation();
		// check if it uses the default location
		IJavaProject proj = pkgRoot.getJavaProject();
		if (outLocation == null) {
			outLocation = proj.getOutputLocation();
		}
		IResource resource = root.findMember(outLocation);
		return resource.getLocation();
	}

	/**
	 * Update the BugCollection for the project.
	 *
	 * @param findBugsProject FindBugs project representing analyzed classes
	 * @param bugReporter     Reporter used to collect the new warnings
	 * @param incremental TODO
	 * @throws CoreException
	 * @throws IOException
	 * @throws DocumentException
	 */
	private void updateBugCollection(Project findBugsProject, Reporter bugReporter, boolean incremental)
			throws CoreException, IOException, DocumentException {
		SortedBugCollection oldBugCollection = FindbugsPlugin.getBugCollection(project, monitor);
		SortedBugCollection newBugCollection = bugReporter.getBugCollection();

		SortedBugCollection resultCollection = mergeBugCollections(oldBugCollection, newBugCollection, incremental);
		resultCollection.setTimestamp(System.currentTimeMillis());

		FindbugsPlugin.storeBugCollection(project, resultCollection, findBugsProject, monitor);
	}

	private SortedBugCollection mergeBugCollections(SortedBugCollection firstCollection, SortedBugCollection secondCollection, boolean incremental)
	{
		Update update = new Update();
		return (SortedBugCollection)(update.mergeCollections(firstCollection, secondCollection, false, incremental));
	}

	/**
	 * Update the original bug collection to include the information in
	 * the new bug collection, preserving the history and classification
	 * of each warning.
	 *
	 * @param bugReporter      Reporter used to collect the new warnings
	 * @param oldBugCollection original warnings
	 * @param newBugCollection new warnings
	 */

	private void updateBugCollectionIncrementally(
			Reporter bugReporter,
			SortedBugCollection oldBugCollection,
			SortedBugCollection newBugCollection) {
		throw new UnsupportedOperationException();
//		UpdateBugCollection updater = new UpdateBugCollection(oldBugCollection, newBugCollection);
//		updater.setUpdatedClassNameSet(bugReporter.getAnalyzedClassNames());
//		updater.execute();
	}

	/**
	 * Update the original bug collection destructively.
	 * Each warning in the set of analyzed classes is replaced with
	 * warnings from the new bug collection.  Past history is discarded.
	 *
	 * @param bugReporter      Reporter used to collect the new warnings
	 * @param oldBugCollection original warnings
	 * @param newBugCollection new warnings
	 */
	private void updateBugCollectionDestructively(
			Reporter bugReporter,
			SortedBugCollection oldBugCollection,
			SortedBugCollection newBugCollection) {
		// FIXME we do this destructively for now: should do incrementally

		// Algorithm:
		// Remove all old warnings for classes which were just analyzed.
		// Then add all new warnings.
		List<BugInstance> toRemove = new ArrayList<BugInstance>();

		if (oldBugCollection != null) {
			Set<String> analyzedClassNameSet = bugReporter.getAnalyzedClassNames();
			for (Iterator<BugInstance> i = oldBugCollection.iterator(); i.hasNext(); ) {
				BugInstance oldWarning = i.next();
				ClassAnnotation warningClass = oldWarning.getPrimaryClass();
				if (warningClass != null && analyzedClassNameSet.contains(warningClass.getClassName())) {
					toRemove.add(oldWarning); // i.remove() would remove only from the bugSet
				}
			}

			for (BugInstance removeMe : toRemove) {
				oldBugCollection.remove(removeMe); // removes from both bugSet and uniqueIdToBugInstanceMap
			}
		} else {
			oldBugCollection = new SortedBugCollection();
		}
		for (Iterator<BugInstance> i = newBugCollection.iterator(); i.hasNext(); ) {
			BugInstance newWarning = i.next();
			oldBugCollection.add(newWarning);
		}
	}

	private void configureExtended(IFindBugsEngine findBugs) {
		// configure extended preferences
		findBugs.setAnalysisFeatureSettings(userPrefs.getAnalysisFeatureSettings());

		 for(String fileName : userPrefs.getIncludeFilterFiles()) {
			IFile file = project.getFile(fileName);
			// TODO: some error reporting here to indicate that a filter no longer exists
			if (file.exists()) {
				String filterName = file.getLocation().toOSString();
				try {
					findBugs.addFilter(filterName, true);
				} catch (RuntimeException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while loading filter \"" + filterName + "\".");
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while reading filter \"" + filterName + "\".");
				}
			}
		}

		for(String fileName : userPrefs.getExcludeFilterFiles()) {
			IFile file = project.getFile(fileName);
			// TODO: some error reporting here to indicate that a filter no longer exists
			if (file.exists()) {
				String filterName = file.getLocation().toOSString();
				try {
					findBugs.addFilter(filterName, false);
				} catch (FilterException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while loading filter \"" + filterName + "\".");
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while reading filter \"" + filterName + "\".");
				}
			}
		}
		for(String fileName : userPrefs.getExcludeBugsFiles()) {
			IFile file = project.getFile(fileName);
			if (file.exists()) {
				String filterName = file.getLocation().toOSString();
				try {
					findBugs.excludeBaselineBugs(filterName);
				} catch (DocumentException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while loading excluded bugs \"" + filterName + "\".");
				} catch (IOException e) {
					FindbugsPlugin.getDefault().logException(e, "Error while reading excluded bugs \"" + filterName + "\".");
				}
			}
		}
	}

	private String[] createClassPathEntries() {
		IJavaProject javaProject = JavaCore.create(project);
		try {
			return JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		}
		catch (CoreException e) {
			if (DEBUG) {
				FindbugsPlugin.getDefault().logException(e, "Could not compute classpath for project");
			}
		}
		return new String[0];
	}

	/**
	 * @return set with IPath objects which represents all known output locations for
	 * current java project, never null
	 * @throws CoreException
	 */
	private Set<IPath> createOutputLocations() throws CoreException {
		Set<IPath> set = new HashSet<IPath>();
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		IJavaProject javaProject = JavaCore.create(this.project);
		// path to the project without project name itself

		if (javaProject.exists() && javaProject.getProject().isOpen()) {
			IClasspathEntry entries[] = javaProject.getRawClasspath();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry classpathEntry = entries[i];
				IPath path = classpathEntry.getOutputLocation();
				if (path != null && classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					// this location is workspace relative and starts with project dir
					IResource resource = root.findMember(path);
					if (resource != null) {
						set.add(resource.getLocation());
					}
				}
			}
		}

		// add the default location
		IPath def = javaProject.getOutputLocation();

		IResource resource = root.findMember(def);
		if (resource != null) {
			IPath location = resource.getLocation();
			set.add(location);
		}

		return set;
	}

	/**
	 * @param outputLocations
	 * @param path
	 * @return true if given path is a child of any one of path objects from given set
	 */
	private boolean containsIn(Set<IPath> outputLocations, IPath path){
		for (IPath dir : outputLocations) {
			if(dir.isPrefixOf(path)){
				return true;
			}
		}
		return false;
	}
}
