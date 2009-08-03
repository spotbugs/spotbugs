/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom\u00e1s Pollak
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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * This test runs a FindBugs analysis on the findbugsTestCases project and
 * checks if there are any unexpected bugs.
 *
 * The results are checked for the unexpected bugs of type
 * FB_MISSING_EXPECTED_WARNING or FB_UNEXPECTED_WARNING.
 *
 * @see ExpectWarning
 * @see NoWarning
 * @author Tom\u00e1s Pollak
 */

public class DetectorsTest {
	/**
     * 
     */
    private static final String FB_UNEXPECTED_WARNING = "FB_UNEXPECTED_WARNING";

	/**
     * 
     */
    private static final String FB_MISSING_EXPECTED_WARNING = "FB_MISSING_EXPECTED_WARNING";

	private BugCollectionBugReporter bugReporter;

	private IFindBugsEngine2 engine;

	@Before
	public void setUp() throws Exception {
		loadFindbugsPlugin();
	}

	@Test
	public void testAllRegressionFiles() throws IOException,
			InterruptedException {
		setUpEngine();

		engine.execute();

		// If there are zero bugs, then something's wrong
		assertFalse(
				"No bugs were reported. Something is wrong with the configuration",
				bugReporter.getBugCollection().getCollection().isEmpty());

		
		List<BugInstance> unexpectedBugs = new ArrayList<BugInstance>();
		for (BugInstance bug : bugReporter.getBugCollection()) {
			if (isUnexpectedBug(bug) && bug.getPriority() == Priorities.HIGH_PRIORITY) {
				unexpectedBugs.add(bug);
				System.out.println(bug.getMessageWithPriorityTypeAbbreviation());
				System.out.println("  " + bug.getPrimarySourceLineAnnotation());
			}
		}

		if (!unexpectedBugs.isEmpty())
		  Assert.fail("Unexpected bugs (" + unexpectedBugs.size() + "):"+ getBugsLocations(unexpectedBugs));
	}

	

	/**
	 * Returns a printable String concatenating bug locations.
	 */
	private String getBugsLocations(List<BugInstance> unexpectedBugs) {
		StringBuilder message = new StringBuilder();
		for (BugInstance bugInstance : unexpectedBugs) {
			message.append("\n");
			if (bugInstance.getBugPattern().getType().equals(FB_MISSING_EXPECTED_WARNING))
				message.append("missing " );
			else
				message.append("unexpected " );
			StringAnnotation pattern = (StringAnnotation) bugInstance.getAnnotations().get(2);
			message.append(pattern.getValue());
			message.append(" ");
			message.append(bugInstance.getPrimarySourceLineAnnotation());
		}
		return message.toString();
	}

	/**
	 * Returns if a bug instance is unexpected for this test.
	 */
	private boolean isUnexpectedBug(BugInstance bug) {
		return FB_MISSING_EXPECTED_WARNING.equals(bug.getType())
				|| FB_UNEXPECTED_WARNING.equals(bug.getType());
	}

	/**
	 * Loads the default detectors from findbugs.jar, to isolate the test from
	 * others that use fake plugins.
	 */
	private void loadFindbugsPlugin() throws MalformedURLException {
		File workingDir = new File(System.getProperty("user.dir"));
		File findbugsJar = new File(workingDir.getParentFile(),
				"findbugs/build/lib/findbugs.jar");
		URL[] pluginList = new URL[] { findbugsJar.toURI().toURL() };
		DetectorFactoryCollection dfc = new DetectorFactoryCollection();
		dfc.setPluginList(pluginList);
		DetectorFactoryCollection.resetInstance(dfc);
	}

	/**
	 * Sets up a FB engine to run on the 'findbugsTestCases' project. It enables
	 * all the available detectors and reports all the bug categories. Uses a
	 * normal priority threshold.
	 */
	private void setUpEngine() {
		this.engine = new FindBugs2();
		Project project = new Project();
		project.setProjectName("findbugsTestCases");
		this.engine.setProject(project);

		DetectorFactoryCollection detectorFactoryCollection = DetectorFactoryCollection
				.instance();
		engine.setDetectorFactoryCollection(detectorFactoryCollection);
		
		bugReporter = new BugCollectionBugReporter(project);
		bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
		
		engine.setBugReporter(this.bugReporter);
		UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
		DetectorFactory checkExpectedWarnings = DetectorFactoryCollection.instance().getFactory("CheckExpectedWarnings");
		preferences.enableDetector(checkExpectedWarnings, true);
		preferences.getFilterSettings().clearAllCategories();
		this.engine.setUserPreferences(preferences);


		// This is ugly. We should think how to improve this.
		project.addFile("../findbugsTestCases/build/classes/");
		project.addAuxClasspathEntry("../findbugsTestCases/lib/j2ee.jar");
		project.addAuxClasspathEntry("lib/junit.jar");
		project.addAuxClasspathEntry("../findbugs/lib/jsr305.jar");
		project.addAuxClasspathEntry("../findbugs/lib/annotations.jar");
	}
}
