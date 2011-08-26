/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.testplugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipFile;

import junit.framework.Assert;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.osgi.framework.Bundle;

import de.tobject.findbugs.FindbugsTestPlugin;

/**
 * Helper methods to set up a IJavaProject.
 */
@SuppressWarnings("restriction")
public class JavaProjectHelper {

    public static final IPath RT_STUBS_15 = new Path("testresources/rtstubs15.jar");

    private static final int MAX_RETRY = 5;

    /**
     * Creates a IJavaProject.
     *
     * @param projectName
     *            The name of the project
     * @param binFolderName
     *            Name of the output folder
     * @return Returns the Java project handle
     * @throws CoreException
     *             Project creation failed
     */
    public static IJavaProject createJavaProject(String projectName, String binFolderName) throws CoreException {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(projectName);
        if (!project.exists()) {
            project.create(null);
        } else {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        }

        if (!project.isOpen()) {
            project.open(null);
        }

        IPath outputLocation;
        if (binFolderName != null && binFolderName.length() > 0) {
            IFolder binFolder = project.getFolder(binFolderName);
            if (!binFolder.exists()) {
                CoreUtility.createFolder(binFolder, false, true, null);
            }
            outputLocation = binFolder.getFullPath();
        } else {
            outputLocation = project.getFullPath();
        }

        if (!project.hasNature(JavaCore.NATURE_ID)) {
            addNatureToProject(project, JavaCore.NATURE_ID, null);
        }

        IJavaProject jproject = JavaCore.create(project);

        jproject.setOutputLocation(outputLocation, null);
        jproject.setRawClasspath(new IClasspathEntry[0], null);

        return jproject;
    }

    /**
     * Sets the compiler options to 1.5 for the given project.
     *
     * @param project
     *            the java project
     */
    public static void set15CompilerOptions(IJavaProject project) {
        @SuppressWarnings("rawtypes")
        Map options = project.getOptions(false);
        JavaProjectHelper.set15CompilerOptions(options);
        project.setOptions(options);
    }

    /**
     * Sets the compiler options to 1.5
     *
     * @param options
     *            The compiler options to configure
     */
    @SuppressWarnings("unchecked")
    public static void set15CompilerOptions(@SuppressWarnings("rawtypes") Map options) {
        options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
        options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
        options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
        options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
    }

    /**
     * Removes a IJavaElement
     *
     * @param elem
     *            The element to remove
     * @throws CoreException
     *             Removing failed
     * @see #ASSERT_NO_MIXED_LINE_DELIMIERS
     */
    public static void delete(final IResource elem) throws CoreException {
        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                try {
                    elem.delete(true, monitor);
                } catch (CoreException e) {
                    JavaPlugin.log(e);
                    throw e;
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, null);
    }

    /**
     * Removes all files in the project and sets the given classpath
     *
     * @param jproject
     *            The project to clear
     * @param entries
     *            The default class path to set
     * @throws CoreException
     *             Clearing the project failed
     */
    public static void clear(final IJavaProject jproject, final IClasspathEntry[] entries) throws CoreException {
        performDummySearch();

        IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                jproject.setRawClasspath(entries, null);

                IResource[] resources = jproject.getProject().members();
                for (int i = 0; i < resources.length; i++) {
                    if (!resources[i].getName().startsWith(".")) {
                        resources[i].delete(true, null);
                    }
                }
            }
        };
        ResourcesPlugin.getWorkspace().run(runnable, null);
    }

    public static void performDummySearch() throws JavaModelException {
        new SearchEngine().searchAllTypeNames(
                null,
                SearchPattern.R_EXACT_MATCH,
                "XXXXXXXXX".toCharArray(), // make sure we search a concrete
                // name. This is faster according to
                // Kent
                SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE, IJavaSearchConstants.CLASS,
                SearchEngine.createJavaSearchScope(new IJavaElement[0]), new Requestor(),
                IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
    }

    /**
     * Adds a source container to a IJavaProject.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     *            The name of the new source container
     * @return The handle to the new source container
     * @throws CoreException
     *             Creation failed
     */
    public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName) throws CoreException {
        return addSourceContainer(jproject, containerName, new Path[0]);
    }

    /**
     * Adds a source container to a IJavaProject.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     *            The name of the new source container
     * @param exclusionFilters
     *            Exclusion filters to set
     * @return The handle to the new source container
     * @throws CoreException
     *             Creation failed
     */
    public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName, IPath[] exclusionFilters)
            throws CoreException {
        return addSourceContainer(jproject, containerName, new Path[0], exclusionFilters);
    }

    /**
     * Adds a source container to a IJavaProject.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     *            The name of the new source container
     * @param inclusionFilters
     *            Inclusion filters to set
     * @param exclusionFilters
     *            Exclusion filters to set
     * @return The handle to the new source container
     * @throws CoreException
     *             Creation failed
     */
    public static IPackageFragmentRoot addSourceContainer(IJavaProject jproject, String containerName, IPath[] inclusionFilters,
            IPath[] exclusionFilters) throws CoreException {
        IProject project = jproject.getProject();
        IContainer container = null;
        if (containerName == null || containerName.length() == 0) {
            container = project;
        } else {
            IFolder folder = project.getFolder(containerName);
            if (!folder.exists()) {
                CoreUtility.createFolder(folder, false, true, null);
            }
            container = folder;
        }
        IPackageFragmentRoot root = jproject.getPackageFragmentRoot(container);

        IClasspathEntry cpe = JavaCore.newSourceEntry(root.getPath(), inclusionFilters, exclusionFilters, null);
        addToClasspath(jproject, cpe);
        return root;
    }

    /**
     * Adds a source container to a IJavaProject and imports all files contained
     * in the given ZIP file.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     *            Name of the source container
     * @param zipFile
     *            Archive to import
     * @param containerEncoding
     *            encoding for the generated source container
     * @return The handle to the new source container
     * @throws InvocationTargetException
     *             Creation failed
     * @throws CoreException
     *             Creation failed
     * @throws IOException
     *             Creation failed
     */
    public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile,
            String containerEncoding) throws InvocationTargetException, CoreException, IOException {
        return addSourceContainerWithImport(jproject, containerName, zipFile, containerEncoding, new Path[0]);
    }

    /**
     * Adds a source container to a IJavaProject and imports all files contained
     * in the given ZIP file.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     *            Name of the source container
     * @param zipFile
     *            Archive to import
     * @param containerEncoding
     *            encoding for the generated source container
     * @param exclusionFilters
     *            Exclusion filters to set
     * @return The handle to the new source container
     * @throws InvocationTargetException
     *             Creation failed
     * @throws CoreException
     *             Creation failed
     * @throws IOException
     *             Creation failed
     */
    public static IPackageFragmentRoot addSourceContainerWithImport(IJavaProject jproject, String containerName, File zipFile,
            String containerEncoding, IPath[] exclusionFilters) throws InvocationTargetException, CoreException, IOException {
        ZipFile file = new ZipFile(zipFile);
        try {
            IPackageFragmentRoot root = addSourceContainer(jproject, containerName, exclusionFilters);
            ((IContainer) root.getCorrespondingResource()).setDefaultCharset(containerEncoding, null);
            importFilesFromZip(file, root.getPath(), null);
            return root;
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    /**
     * Adds a library entry to a IJavaProject.
     *
     * @param jproject
     *            The parent project
     * @param path
     *            The path of the library to add
     * @return The handle of the created root
     * @throws JavaModelException
     */
    public static IPackageFragmentRoot addLibrary(IJavaProject jproject, IPath path) throws JavaModelException {
        return addLibrary(jproject, path, null, null);
    }

    /**
     * Adds a library entry with source attachment to a IJavaProject.
     *
     * @param jproject
     *            The parent project
     * @param path
     *            The path of the library to add
     * @param sourceAttachPath
     *            The source attachment path
     * @param sourceAttachRoot
     *            The source attachment root path
     * @return The handle of the created root
     * @throws JavaModelException
     */
    public static IPackageFragmentRoot addLibrary(IJavaProject jproject, IPath path, IPath sourceAttachPath,
            IPath sourceAttachRoot) throws JavaModelException {
        IClasspathEntry cpe = JavaCore.newLibraryEntry(path, sourceAttachPath, sourceAttachRoot);
        addToClasspath(jproject, cpe);
        return jproject.getPackageFragmentRoot(path.toString());
    }

    /**
     * Copies the library into the project and adds it as library entry.
     *
     * @param jproject
     *            The parent project
     * @param jarPath
     * @param sourceAttachPath
     *            The source attachment path
     * @param sourceAttachRoot
     *            The source attachment root path
     * @return The handle of the created root
     * @throws IOException
     * @throws CoreException
     */
    public static IPackageFragmentRoot addLibraryWithImport(IJavaProject jproject, IPath jarPath, IPath sourceAttachPath,
            IPath sourceAttachRoot) throws IOException, CoreException {
        IProject project = jproject.getProject();
        IFile newFile = project.getFile(jarPath.lastSegment());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(jarPath.toFile());
            newFile.create(inputStream, true, null);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return addLibrary(jproject, newFile.getFullPath(), sourceAttachPath, sourceAttachRoot);
    }

    /**
     * Creates and adds a class folder to the class path.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     * @param sourceAttachPath
     *            The source attachment path
     * @param sourceAttachRoot
     *            The source attachment root path
     * @return The handle of the created root
     * @throws CoreException
     */
    public static IPackageFragmentRoot addClassFolder(IJavaProject jproject, String containerName, IPath sourceAttachPath,
            IPath sourceAttachRoot) throws CoreException {
        IProject project = jproject.getProject();
        IContainer container = null;
        if (containerName == null || containerName.length() == 0) {
            container = project;
        } else {
            IFolder folder = project.getFolder(containerName);
            if (!folder.exists()) {
                CoreUtility.createFolder(folder, false, true, null);
            }
            container = folder;
        }
        IClasspathEntry cpe = JavaCore.newLibraryEntry(container.getFullPath(), sourceAttachPath, sourceAttachRoot);
        addToClasspath(jproject, cpe);
        return jproject.getPackageFragmentRoot(container);
    }

    /**
     * Creates and adds a class folder to the class path and imports all files
     * contained in the given ZIP file.
     *
     * @param jproject
     *            The parent project
     * @param containerName
     * @param sourceAttachPath
     *            The source attachment path
     * @param sourceAttachRoot
     *            The source attachment root path
     * @param zipFile
     * @return The handle of the created root
     * @throws IOException
     * @throws CoreException
     * @throws InvocationTargetException
     */
    public static IPackageFragmentRoot addClassFolderWithImport(IJavaProject jproject, String containerName,
            IPath sourceAttachPath, IPath sourceAttachRoot, File zipFile) throws IOException, CoreException,
            InvocationTargetException {
        ZipFile file = new ZipFile(zipFile);
        try {
            IPackageFragmentRoot root = addClassFolder(jproject, containerName, sourceAttachPath, sourceAttachRoot);
            importFilesFromZip(file, root.getPath(), null);
            return root;
        } finally {
            if (file != null) {
                file.close();
            }
        }
    }

    /**
     * Adds a library entry pointing to a JRE (stubs only) and sets the right
     * compiler options.
     * <p>
     * Currently, the compiler compliance level is 1.5.
     *
     * @param jproject
     *            target
     * @return the new package fragment root
     * @throws CoreException
     */
    public static IPackageFragmentRoot addRTJar(IJavaProject jproject) throws CoreException {
        IPath[] rtJarPath = findRtJar(RT_STUBS_15);
        set15CompilerOptions(jproject);
        return addLibrary(jproject, rtJarPath[0], rtJarPath[1], rtJarPath[2]);
    }

    /**
     * Adds a variable entry with source attachment to a IJavaProject. Can
     * return null if variable can not be resolved.
     *
     * @param jproject
     *            The parent project
     * @param path
     *            The variable path
     * @param sourceAttachPath
     *            The source attachment path (variable path)
     * @param sourceAttachRoot
     *            The source attachment root path (variable path)
     * @return The added package fragment root
     * @throws JavaModelException
     */
    public static IPackageFragmentRoot addVariableEntry(IJavaProject jproject, IPath path, IPath sourceAttachPath,
            IPath sourceAttachRoot) throws JavaModelException {
        IClasspathEntry cpe = JavaCore.newVariableEntry(path, sourceAttachPath, sourceAttachRoot);
        addToClasspath(jproject, cpe);
        IPath resolvedPath = JavaCore.getResolvedVariablePath(path);
        if (resolvedPath != null) {
            return jproject.getPackageFragmentRoot(resolvedPath.toString());
        }
        return null;
    }

    /**
     * Adds a variable entry pointing to a current JRE (stubs only) and sets the
     * compiler compliance level on the project accordingly. The arguments
     * specify the names of the variables to be used. Currently, the compiler
     * compliance level is set to 1.5.
     *
     * @param jproject
     *            the project to add the variable RT JAR
     * @param libVarName
     *            Name of the variable for the library
     * @param srcVarName
     *            Name of the variable for the source attachment. Can be
     *            <code>null</code> .
     * @param srcrootVarName
     *            name of the variable for the source attachment root. Can be
     *            <code>null</code>.
     * @return the new package fragment root
     * @throws CoreException
     *             Creation failed
     */
    public static IPackageFragmentRoot addVariableRTJar(IJavaProject jproject, String libVarName, String srcVarName,
            String srcrootVarName) throws CoreException {
        return addVariableRTJar(jproject, RT_STUBS_15, libVarName, srcVarName, srcrootVarName);
    }

    /**
     * Adds a variable entry pointing to a current JRE (stubs only). The
     * arguments specify the names of the variables to be used. Clients must not
     * forget to set the right compiler compliance level on the project.
     *
     * @param jproject
     *            the project to add the variable RT JAR
     * @param rtStubsPath
     *            path to an rt.jar
     * @param libVarName
     *            name of the variable for the library
     * @param srcVarName
     *            Name of the variable for the source attachment. Can be
     *            <code>null</code> .
     * @param srcrootVarName
     *            Name of the variable for the source attachment root. Can be
     *            <code>null</code>.
     * @return the new package fragment root
     * @throws CoreException
     *             Creation failed
     */
    private static IPackageFragmentRoot addVariableRTJar(IJavaProject jproject, IPath rtStubsPath, String libVarName,
            String srcVarName, String srcrootVarName) throws CoreException {
        IPath[] rtJarPaths = findRtJar(rtStubsPath);
        IPath libVarPath = new Path(libVarName);
        IPath srcVarPath = null;
        IPath srcrootVarPath = null;
        JavaCore.setClasspathVariable(libVarName, rtJarPaths[0], null);
        if (srcVarName != null) {
            IPath varValue = rtJarPaths[1] != null ? rtJarPaths[1] : Path.EMPTY;
            JavaCore.setClasspathVariable(srcVarName, varValue, null);
            srcVarPath = new Path(srcVarName);
        }
        if (srcrootVarName != null) {
            IPath varValue = rtJarPaths[2] != null ? rtJarPaths[2] : Path.EMPTY;
            JavaCore.setClasspathVariable(srcrootVarName, varValue, null);
            srcrootVarPath = new Path(srcrootVarName);
        }
        return addVariableEntry(jproject, libVarPath, srcVarPath, srcrootVarPath);
    }

    /**
     * Sets auto-building state for the test workspace.
     *
     * @param state
     *            The new auto building state
     * @return The previous state
     * @throws CoreException
     *             Change failed
     */
    public static boolean setAutoBuilding(boolean state) throws CoreException {
        // disable auto build
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription desc = workspace.getDescription();
        boolean result = desc.isAutoBuilding();
        desc.setAutoBuilding(state);
        workspace.setDescription(desc);
        return result;
    }

    public static void addToClasspath(IJavaProject jproject, IClasspathEntry cpe) throws JavaModelException {
        IClasspathEntry[] oldEntries = jproject.getRawClasspath();
        for (int i = 0; i < oldEntries.length; i++) {
            if (oldEntries[i].equals(cpe)) {
                return;
            }
        }
        int nEntries = oldEntries.length;
        IClasspathEntry[] newEntries = new IClasspathEntry[nEntries + 1];
        System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
        newEntries[nEntries] = cpe;
        jproject.setRawClasspath(newEntries, null);
    }

    /**
     * @param rtStubsPath
     *            the path to the RT stubs
     * @return a rt.jar (stubs only)
     * @throws CoreException
     */
    public static IPath[] findRtJar(IPath rtStubsPath) throws CoreException {
        File rtStubs = FindbugsTestPlugin.getDefault().getFileInPlugin(rtStubsPath);
        Assert.assertNotNull(rtStubs);
        Assert.assertTrue(rtStubs.exists());
        return new IPath[] { Path.fromOSString(rtStubs.getPath()), null, null };
    }

    private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
        IProjectDescription description = proj.getDescription();
        String[] prevNatures = description.getNatureIds();
        String[] newNatures = new String[prevNatures.length + 1];
        System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
        newNatures[prevNatures.length] = natureId;
        description.setNatureIds(newNatures);
        proj.setDescription(description, monitor);
    }

    private static void importFilesFromZip(ZipFile srcZipFile, IPath destPath, IProgressMonitor monitor)
            throws InvocationTargetException {
        ZipFileStructureProvider structureProvider = new ZipFileStructureProvider(srcZipFile);
        try {
            ImportOperation op = new ImportOperation(destPath, structureProvider.getRoot(), structureProvider,
                    new ImportOverwriteQuery());
            op.run(monitor);
        } catch (InterruptedException e) {
            // should not happen
        }
    }

    /**
     * Imports resources from <code>bundleSourcePath</code> to
     * <code>importTarget</code>.
     *
     * @param importTarget
     *            the parent container
     * @param bundleSourcePath
     *            the path to a folder containing resources
     *
     * @throws CoreException
     *             import failed
     * @throws IOException
     *             import failed
     */
    public static void importResources(IContainer importTarget, Bundle bundle, String bundleSourcePath) throws CoreException,
            IOException {
        Enumeration<?> entryPaths = bundle.getEntryPaths(bundleSourcePath);
        while (entryPaths.hasMoreElements()) {
            String path = (String) entryPaths.nextElement();
            IPath name = new Path(path.substring(bundleSourcePath.length()));
            if (path.endsWith("/.svn/")) {
                continue; // Ignore SVN folders
            } else if (path.endsWith("/")) {
                IFolder folder = importTarget.getFolder(name);
                if (folder.exists()) {
                    folder.delete(true, null);
                }
                folder.create(true, true, null);
                importResources(folder, bundle, path);
            } else {
                URL url = bundle.getEntry(path);
                IFile file = importTarget.getFile(name);
                if (!file.exists()) {
                    file.create(url.openStream(), true, null);
                } else {
                    file.setContents(url.openStream(), true, false, null);
                }
            }
        }
    }

    private static class ImportOverwriteQuery implements IOverwriteQuery {
        public String queryOverwrite(String file) {
            return ALL;
        }
    }

    private static class Requestor extends TypeNameRequestor {
    }
}
