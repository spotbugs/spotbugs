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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.junit.Assert;
import org.junit.Before;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.FindbugsTestPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolutionGenerator;

/**
 * Base class for FindBugs quickfix tests.
 * 
 * @author Tomás Pollak
 */
public abstract class AbstractQuickfixTest extends AbstractPluginTest {

	private IMarkerResolutionGenerator2 resolutionGenerator;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		resolutionGenerator = new BugResolutionGenerator();

		// We need to enable project settings, because some tests need to modify the
		// reporting preferences
		FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
	}

	@Override
	public void tearDown() throws CoreException {
		resolutionGenerator = null;

		super.tearDown();
	}

	protected void doTestQuickfixResolution(String classFileName,
			Class<? extends IMarkerResolution> resolutionClass,
			String... expectedPatterns) throws CoreException, IOException {
		// Run FindBugs on the input class
		work(createFindBugsWorker(), getInputResource(classFileName));

		// Assert the expected markers are present
		IMarker[] markers = getInputFileMarkers(classFileName);
		assertPresentBugPatterns(expectedPatterns, markers);
		assertEquals(expectedPatterns.length, markers.length);

		// Assert all markers have resolution
		assertAllMarkersHaveResolutions(markers);

		// Apply resolution to each marker
		if (resolutionClass != null) {
			applySpecificResolutionForAllMarkers(markers, resolutionClass);
		} else {
			applySingleResolutionForAllMarkers(markers);
		}

		// Assert output file
		assertEqualFiles(getExpectedOutputFile(classFileName),
				getInputCompilationUnit(classFileName));
		assertEquals(0, getInputFileMarkers(classFileName).length);
	}

	protected void doTestQuickfixResolution(String classFileName,
			String... expectedPatterns) throws CoreException, IOException {
		doTestQuickfixResolution(classFileName, null, expectedPatterns);
	}

	protected void enableBugCategory(String category) {
		getProjectPreferences().getFilterSettings().addCategory(category);
	}

	@Override
	protected TestScenario getTestScenario() {
		return TestScenario.QUICKFIX;
	}

	private void applySingleResolutionForAllMarkers(IMarker[] markers) {
		for (int i = 0; i < markers.length; i++) {
			IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(
					markers[i]);
			assertEquals(1, resolutions.length);
			resolutions[0].run(markers[i]);
		}
	}

	private void applySpecificResolutionForAllMarkers(IMarker[] markers,
			Class<? extends IMarkerResolution> resolutionClass) {
		for (int i = 0; i < markers.length; i++) {
			IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(
					markers[i]);
			for (int j = 0; j < resolutions.length; j++) {
				if (resolutionClass.isInstance(resolutions[j])) {
					resolutions[j].run(markers[i]);
					return;
				}
			}
		}
		Assert.fail("No resolution of class " + resolutionClass);
	}

	private void assertAllMarkersHaveResolutions(IMarker[] markers) {
		for (int i = 0; i < markers.length; i++) {
			assertTrue(getResolutionGenerator().hasResolutions(markers[i]));
		}
	}

	private void assertEqualFiles(URL expectedFile, ICompilationUnit compilationUnit)
			throws IOException, JavaModelException {
		String expectedSource = readFileContents(expectedFile);
		assertEquals(expectedSource, compilationUnit.getSource());
	}

	private void assertPresentBugPattern(String bugPatternType, IMarker[] markers) {
		for (int i = 0; i < markers.length; i++) {
			BugPattern pattern = MarkerUtil.findBugPatternForMarker(markers[i]);
			if (pattern.getType().equals(bugPatternType)) {
				return;
			}
		}
		fail("Couldn't find pattern " + bugPatternType);
	}

	private void assertPresentBugPatterns(String[] expectedPatterns, IMarker[] markers) {
		for (int i = 0; i < expectedPatterns.length; i++) {
			assertPresentBugPattern(expectedPatterns[i], markers);
		}
	}

	private URL getExpectedOutputFile(String filename) {
		URL url = FindbugsTestPlugin.getDefault().getBundle().getEntry(
				"/quickfixOutput/" + filename);
		return url;
	}

	private ICompilationUnit getInputCompilationUnit(String classFileName)
			throws JavaModelException {
		ICompilationUnit compilationUnit = (ICompilationUnit) getJavaProject()
				.findElement(new Path(classFileName));
		return compilationUnit;
	}

	private IMarker[] getInputFileMarkers(String classFileName) throws JavaModelException {
		return MarkerUtil.getAllMarkers(getInputResource(classFileName));
	}

	private IResource getInputResource(String classFileName) throws JavaModelException {
		return getInputCompilationUnit(classFileName).getResource();
	}

	private IMarkerResolutionGenerator2 getResolutionGenerator() {
		return resolutionGenerator;
	}

	private String readFileContents(URL url) throws IOException {
		StringWriter writer = new StringWriter();
		InputStream input = null;
		try {
			input = url.openStream();
			int nextChar;
			while ((nextChar = input.read()) != -1) {
				writer.write(nextChar);
			}
		} finally {
			if (input != null) {
				input.close();
			}
		}
		return writer.toString();
	}
}
