/*
 * Contributions to FindBugs
 * Copyright (C) 2014, Kevin Lubick
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
package de.tobject.findbugs.quickfix.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.test.AbstractQuickfixTest;
import de.tobject.findbugs.test.TestScenario;

import edu.umd.cs.findbugs.config.ProjectFilterSettings;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *  Tests the ability to fix multiple bugs at once from the Problems view
 *
 * @author Kevin Lubick
 *
 */
public class QuickfixMulti extends AbstractQuickfixTest {


    @Override
    protected TestScenario getTestScenario() {
          return TestScenario.MULTIQUICKFIX;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.MULTIQUICKFIX);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ProjectFilterSettings settings = getProjectPreferences().getFilterSettings();
        settings.setMinPriority("Low");
        settings.setMinRank(20);
    }


    @Test
    public void testMultiUseValueOf() throws Exception {
        QuickFixTestPackager packager = new QuickFixTestPackager();
        packager.addBugPatterns("DM_FP_NUMBER_CTOR","DM_FP_NUMBER_CTOR","DM_FP_NUMBER_CTOR","DM_FP_NUMBER_CTOR","DM_FP_NUMBER_CTOR","DM_FP_NUMBER_CTOR");
        packager.addExpectedLines(6, //OneProblemHere.java
                11,24,   //TwoProblemsHere.java
                7,9,16  //ThreeProblemsHere.java
                );
        packager.setExpectedLabels(0, "Use Double.valueOf(6.1) instead");
        packager.setExpectedLabels(1, "Use Double.valueOf(7.1) instead");
        packager.setExpectedLabels(2, "Use Double.valueOf(7.2) instead");
        packager.setExpectedLabels(3, "Use Double.valueOf(8.1) instead");
        packager.setExpectedLabels(4, "Use Float.valueOf(8.2f) instead");
        packager.setExpectedLabels(5, "Use Double.valueOf(8.3) instead");
        doTestMultiQuickfixResolution(getJavaProject().getProject(), packager.asList());

    }


    protected void doTestMultiQuickfixResolution(IProject project, List<QuickFixTestPackage> packages) throws CoreException, IOException {
        // Run FindBugs on the entire project
        work(createFindBugsWorker(), project);

        // Assert the expected markers are present
        IMarker[] markers = MarkerUtil.getAllMarkers(project);

        markers = filterMarkers(markers, packages);
        sortMarkers(markers);

        assertEquals("Too many or too few markers",packages.size(), markers.length);

        assertPresentBugPatterns(packages, markers);
        assertPresentLabels(packages, markers);
        assertPresentLineNumbers(packages, markers);

        // Assert all markers have resolution
        assertAllMarkersHaveResolutions(markers);

        // Apply resolution to each marker
        applyMultiResolutionToAllMarkers(markers);

        //check project inputs and outputs
        checkJavaFiles(project.members());
    }

    private void checkJavaFiles(IResource[] iResources) throws CoreException, IOException, JavaModelException {
        for (IResource resource : iResources) {
            if (resource instanceof IFile) {
                String fileName = resource.getName();
                if (fileName.endsWith(".java")) {
                    assertEqualFiles(getExpectedOutputFile(fileName), getInputCompilationUnit(fileName));
                }
            }
            else if (resource instanceof IFolder) {
                checkJavaFiles(((IFolder) resource).members());
            }
        }
    }


    private void applyMultiResolutionToAllMarkers(IMarker[] markers) {
        IMarkerResolution[] resolutions = getResolutionGenerator().getResolutions(markers[0]);
        if (resolutions[0] instanceof WorkbenchMarkerResolution) {
            //this represents one of the bugs a user would click on in the problems menu
            WorkbenchMarkerResolution resolutionFromProblemsMenu = ((WorkbenchMarkerResolution)resolutions[0]);

            //in theory, we should have filtered all the bugs of the passed in type
            //So, findOtherMarkers should return them all
            assertEquals(markers.length - 1, resolutionFromProblemsMenu.findOtherMarkers(markers).length);

            resolutionFromProblemsMenu.run(markers, null);
        } else {
            fail("Should have been a WorkBenchMarkerResolution: "+resolutions[0]);
        }

    }

    //Filters out markers so that only markers with the expected pattern are in the array
    private IMarker[] filterMarkers(IMarker[] markers, List<QuickFixTestPackage> list) {
        ArrayList<IMarker> filteredMarkers = new ArrayList<>();

        Set<String> bugPatternTypes = new HashSet<>();
        for(QuickFixTestPackage pack : list) {
            bugPatternTypes.add(pack.expectedPattern);
        }

        for (int i = 0; i < markers.length; i++) {
            String pattern = MarkerUtil.getBugPatternString(markers[i]);
            if (bugPatternTypes.contains(pattern)) {
                filteredMarkers.add(markers[i]);
            }
        }

        return filteredMarkers.toArray(new IMarker[filteredMarkers.size()]);
    }

    @Override
    protected String getOutputFolderName() {
        return "/multiQuickfixOutput/";
    }

}
