/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tomás Pollak
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
package de.tobject.findbugs.test;

import static org.eclipse.jdt.testplugin.JavaProjectHelper.*;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.After;
import org.junit.Before;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.FindbugsTestPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Base class for FindBugs tests.
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractPluginTest {

	protected static final String SRC = "src";
	protected static final String TEST_PROJECT = "TestProject";
	private IJavaProject project;

	public AbstractPluginTest() {
		super();
	}

	/**
	 * Create a new Java project with a source folder and copy the test files of the
	 * plugin to the source folder. Compile the project.
	 * 
	 * @throws CoreException
	 * @throws IOException
	 */
	@Before
	public void setUp() throws CoreException, IOException {
		// Create the test project
		project = createJavaProject(TEST_PROJECT, "bin");
		addRTJar(getJavaProject());
		addSourceContainer(getJavaProject(), SRC);
		addExtraClassPathEntries();

		// Copy test workspace
		importResources(getProject().getFolder(SRC), FindbugsTestPlugin.getDefault()
				.getBundle(), getTestFilesPath());

		// Compile project
		getProject().getProject().build(IncrementalProjectBuilder.FULL_BUILD, null);
		performDummySearch();

		// Start with a clean FindBugs state
		clearBugsState();
	}

	/**
	 * Delete the Java project used for this test.
	 * 
	 * @throws CoreException
	 */
	@After
	public void tearDown() throws CoreException {
		// Clean the FindBugs state
		clearBugsState();

		// Delete the test project
		delete(project);
	}

	/**
	 * Hook for subclasses to add extra classpath entries to the test project during the
	 * setup of the test.
	 */
	protected void addExtraClassPathEntries() throws CoreException {
		// Default implementation
	}

	protected void clearBugsState() throws CoreException {
		MarkerUtil.removeMarkers(getProject());
		FindbugsPlugin.getBugCollection(getProject(), null).clearBugInstances();
	}

	protected FindBugsWorker createFindBugsWorker() throws CoreException {
		FindBugsWorker worker = new FindBugsWorker(getProject(),
				new NullProgressMonitor());
		return worker;
	}

	/**
	 * Returns the Java project for this test.
	 * 
	 * @return An IJavaProject.
	 */
	protected IJavaProject getJavaProject() {
		return project;
	}

	/**
	 * Returns the project for this test.
	 * 
	 * @return An IProject.
	 */
	protected IProject getProject() {
		return getJavaProject().getProject();
	}

	protected UserPreferences getProjectPreferences() {
		return FindbugsPlugin.getProjectPreferences(getProject(), false);
	}

	/**
	 * Subclasses must implement this method and return a path inside of the bundle where
	 * the test files are located.
	 */
	protected abstract String getTestFilesPath();

	/**
	 * Runs the FindBugs worker on the test project.
	 */
	protected void work(FindBugsWorker worker) throws CoreException {
		work(worker, getProject());
	}

	/**
	 * Runs the FindBugs worker on the given resource.
	 */
	protected void work(FindBugsWorker worker, IResource resource) throws CoreException {
		worker.work(Collections.singletonList(resource));
	}
}