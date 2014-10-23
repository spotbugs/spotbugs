/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Tom�s Pollak
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.FindbugsTestPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolution;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.BugResolutionGenerator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * Base class for FindBugs quickfix tests.
 *
 * @author Tom�s Pollak
 */
public abstract class AbstractQuickfixTest extends AbstractPluginTest {

    private IMarkerResolutionGenerator2 resolutionGenerator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        resolutionGenerator = new BugResolutionGenerator();

        // We need to enable project settings, because some tests need to modify
        // the reporting preferences
        FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
    }

    @Override
    @After
    public void tearDown() throws CoreException {
        resolutionGenerator = null;

        super.tearDown();
    }

    protected void doTestQuickfixResolution(String classFileName, Class<? extends IMarkerResolution> resolutionClass, String... expectedPatterns) throws CoreException, IOException {
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.addBugPatterns(expectedPatterns);

        doTestQuickfixResolution(classFileName, resolutionClass, packager.asList());
    }

    protected void doTestQuickfixResolution(String classFileName, String... expectedPatterns) throws CoreException, IOException {
        doTestQuickfixResolution(classFileName, null, expectedPatterns);
    }

    protected void doTestQuickfixResolution(String classFileName, List<QuickFixTestPackage> packages) throws CoreException, IOException {
        doTestQuickfixResolution(classFileName, null, packages);
    }

    protected void doTestQuickfixResolution(String classFileName, Class<? extends IMarkerResolution> resolutionClass, List<QuickFixTestPackage> packages) throws CoreException, IOException {
        // Run FindBugs on the input class
        work(createFindBugsWorker(), getInputResource(classFileName));

        // Assert the expected markers are present
        IMarker[] markers = getInputFileMarkers(classFileName);
        assertEquals("Too many or too few markers",packages.size(), markers.length);

        sortMarkers(markers);

        assertPresentBugPatterns(packages, markers);
        assertPresentLabels(packages, markers);
        assertPresentLineNumbers(packages, markers);

        // Assert all markers have resolution
        assertAllMarkersHaveResolutions(markers);

        // Apply resolution to each marker
        if (resolutionClass != null) {
            applySpecificResolutionForAllMarkers(markers, resolutionClass);
        } else {
            applySingleResolutionForAllMarkers(markers);
        }

        // Assert output file
        assertEqualFiles(getExpectedOutputFile(classFileName), getInputCompilationUnit(classFileName));
        assertEquals(0, getInputFileMarkers(classFileName).length);
    }

    protected void sortMarkers(IMarker[] markers) {
        Arrays.sort(markers, new Comparator<IMarker>() {

            @Override
            public int compare(IMarker marker1, IMarker marker2) {
                String pattern1 = MarkerUtil.getBugPatternString(marker1);
                String pattern2 = MarkerUtil.getBugPatternString(marker2);
                if (pattern1 != null) {
                    if (pattern1.equals(pattern2)) {
                        return MarkerUtil.findPrimaryLineForMaker(marker1) -
                                MarkerUtil.findPrimaryLineForMaker(marker2);
                    }
                    return pattern1.compareTo(pattern2);
                }
                //else, perhaps fail because markers don't have bugPatternStrings?
                else if (pattern2 == null) {
                    return 0;       //neither is a bugPattern?
                }
                return MarkerUtil.findPrimaryLineForMaker(marker1) - MarkerUtil.findPrimaryLineForMaker(marker2);
            }
        });
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
            IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(markers[i]);
            assertEquals(1, resolutions.length);
            resolutions[0].run(markers[i]);
        }
    }

    private void applySpecificResolutionForAllMarkers(IMarker[] markers, Class<? extends IMarkerResolution> resolutionClass) {
        for (int i = 0; i < markers.length; i++) {
            IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(markers[i]);
            for (int j = 0; j < resolutions.length; j++) {
                if (resolutionClass.isInstance(resolutions[j])) {
                    resolutions[j].run(markers[i]);
                    return;
                }
            }
        }
        Assert.fail("No resolution of class " + resolutionClass);
    }

    protected void assertAllMarkersHaveResolutions(IMarker[] markers) {
        for (int i = 0; i < markers.length; i++) {
            IMarker marker = markers[i];
            boolean hasResolutions = getResolutionGenerator().hasResolutions(marker);
            if(!hasResolutions){
                String pattern = MarkerUtil.getBugPatternString(marker);
                fail("no resolution for: " + pattern);
            }
            assertTrue(hasResolutions);
        }
    }

    protected void assertEqualFiles(URL expectedFile, ICompilationUnit compilationUnit) throws IOException, JavaModelException {
        String expectedSource = readFileContents(expectedFile);
        assertEquals(expectedSource, compilationUnit.getSource());
    }

    @Deprecated
    protected void assertPresentBugPattern(@Nonnull String bugPatternType, IMarker[] markers) {
        for (int i = 0; i < markers.length; i++) {
            BugPattern pattern = MarkerUtil.findBugPatternForMarker(markers[i]);
            if (pattern != null && bugPatternType.equals(pattern.getType())) {
                return;
            }
        }
        fail("Couldn't find pattern " + bugPatternType);
    }

    protected void assertPresentBugPatterns(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < packages.size(); i++) {
            String actualBugpattern = MarkerUtil.getBugPatternString(markers[i]);
            assertEquals("Bug Pattern should match" , packages.get(i).expectedPattern, actualBugpattern);
        }
    }

    protected void assertPresentLineNumbers(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < packages.size(); i++) {
            int lineNumber = MarkerUtil.findPrimaryLineForMaker(markers[i]);
            if (packages.get(i).lineNumber != QuickFixTestPackage.LINE_NUMBER_NOT_SPECIFIED) {
                assertEquals("Line number should match" , packages.get(i).lineNumber, lineNumber);
            }
        }
    }

    protected void assertPresentLabels(List<QuickFixTestPackage> packages, IMarker[] markers) {
        for (int i = 0; i < packages.size(); i++) {
            if (packages.get(i).expectedLabels == null) {
                continue; //TODO migrate older tests to specify their labels
            }
            IMarker marker = markers[i];
            List<String> expectedLabels = new ArrayList<>(packages.get(i).expectedLabels);
            IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(marker);

            assertEquals("The expected number of resolutions availible was wrong", expectedLabels.size(), resolutions.length);

            for (int j = 0; j < resolutions.length; j++) {
                BugResolution resolution = (BugResolution) resolutions[j];
                String label = resolution.getLabel();
                assertTrue("Should have seen label: "+label, expectedLabels.contains(label));
                expectedLabels.remove(label);
            }
        }
    }

    protected URL getExpectedOutputFile(String filename) {
        return FindbugsTestPlugin.getDefault().getBundle().getEntry(getOutputFolderName() + filename);
    }

    protected abstract String getOutputFolderName();

    protected ICompilationUnit getInputCompilationUnit(String classFileName) throws JavaModelException {
        return (ICompilationUnit) getJavaProject().findElement(new Path(classFileName));
    }

    private IMarker[] getInputFileMarkers(String classFileName) throws JavaModelException {
        return MarkerUtil.getAllMarkers(getInputResource(classFileName));
    }

    private IResource getInputResource(String classFileName) throws JavaModelException {
        return getInputCompilationUnit(classFileName).getResource();
    }

    protected IMarkerResolutionGenerator2 getResolutionGenerator() {
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

    public static class QuickFixTestPackage {

        public static final int LINE_NUMBER_NOT_SPECIFIED = -1; //TODO remove this after updating current tests
        public String expectedPattern = null;
        public List<String> expectedLabels = null;
        public int lineNumber = LINE_NUMBER_NOT_SPECIFIED;
        @Override
        public String toString() {
            return "QuickFixTestPackage [expectedPattern=" + expectedPattern + ", expectedLabels=" + expectedLabels
                    + ", lineNumber=" + lineNumber + "]";
        }
    }

    protected static class QuickFixTestPackager {

        private final List<QuickFixTestPackage> packages = new ArrayList<>();

        public QuickFixTestPackager() {
            //made public to be seen by subclasses
        }

        public void addBugPatterns(String... expectedPatterns) {
            for (int i = 0; i < expectedPatterns.length; i++) {
                String pattern = expectedPatterns[i];
                if (packages.size() <= i) {
                    packages.add(new QuickFixTestPackage());
                }
                packages.get(i).expectedPattern = pattern;
            }
        }

        /**
         *
         * @return a sorted list of QuickFixTestPackages to be used in assertions.
         */
        public List<QuickFixTestPackage> asList() {
            Collections.sort(packages, new Comparator<QuickFixTestPackage>() {

                @Override
                public int compare(QuickFixTestPackage o1, QuickFixTestPackage o2) {
                    if (o1.expectedPattern.equals(o2.expectedPattern)) {
                        return o1.lineNumber - o2.lineNumber;
                    }
                    return o1.expectedPattern.compareTo(o2.expectedPattern);
                }
            });
            return Collections.unmodifiableList(packages);
        }

        /*
         * Could be more than one at a given index, so they need to be specified individually
         */
        public void setExpectedLabels(int index, String... expectedLabels) {
            while (packages.size() <= index) {
                packages.add(new QuickFixTestPackage());
            }
             packages.get(index).expectedLabels = Arrays.asList(expectedLabels);

        }

        public void addExpectedLines(int... lineNumbers) {
            for (int i = 0; i < lineNumbers.length; i++) {
                int lineNumber = lineNumbers[i];
                if (packages.size() <= i) {
                    packages.add(new QuickFixTestPackage());
                }
                packages.get(i).lineNumber = lineNumber;
            }
        }

    }
}


