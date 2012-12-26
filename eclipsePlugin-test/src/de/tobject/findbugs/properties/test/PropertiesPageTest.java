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
package de.tobject.findbugs.properties.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.properties.FindbugsPropertyPage.Effort;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;
import de.tobject.findbugs.util.ProjectUtilities;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * This class tests the FindbugsPropertyPage and related classes.
 *
 * @author Tom�s Pollak
 */
public class PropertiesPageTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private UserPreferences originalProjectPreferences;

    private UserPreferences originalWorkspacePreferences;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Save the preferences and restore them after the test
        originalWorkspacePreferences = getWorkspacePreferences().clone();
        originalProjectPreferences = readProjectPreferences().clone();

        // Enable project settings
        FindbugsPlugin.setProjectSettingsEnabled(getProject(), null, true);
    }

    @Override
    public void tearDown() throws CoreException {
        // Restore the preferences to the original state
        FindbugsPlugin.saveUserPreferences(getProject(), originalProjectPreferences);
        FindbugsPlugin.saveUserPreferences(null, originalWorkspacePreferences);

        super.tearDown();
    }

    @Test
    public void testAddFileToConflictingFilters()  {
        // Check that there are no filter files
        assertNoFilterFiles();

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Add a file to the include and exclude filters
        page.getFilterTab().addFileToIncludeFilter(getFilterFileLocation());
        page.getFilterTab().addFileToExcludeFilter(getFilterFileLocation());

        // Assert that there is an error message
        assertNotNull(page.getErrorMessage());

        // Close the dialog
        dialog.cancelPressed();
    }

    @Test
    public void testAddFileToExcludeBugsFilter()  {
        // Check that there are no filter files
        assertNoFilterFiles();

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Add a file to the exclude bugs filters
        page.getFilterTab().addFileToExcludeBugsFilter(getBugsFileLocation());

        // Accept the dialog
        dialog.okPressed();

        // Check that the file was added to the filter
        assertEmptyFilter(getProjectPreferences().getIncludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeFilterFiles());
        assertSelectedFilter(getBugsFileProjectRelativePath(), getProjectPreferences().getExcludeBugsFiles());
    }

    @Test
    public void testAddFileToExcludeFilter()  {
        // Check that there are no filter files
        assertNoFilterFiles();

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Add a file to the exclude filters
        page.getFilterTab().addFileToExcludeFilter(getFilterFileLocation());

        // Accept the dialog
        dialog.okPressed();

        // Check that the file was added to the filter
        assertEmptyFilter(getProjectPreferences().getIncludeFilterFiles());
        assertSelectedFilter(getFilterFileProjectRelativePath(), getProjectPreferences().getExcludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeBugsFiles());
    }

    @Test
    public void testAddFileToExcludeFilterTwice() {
        // Check that there are no filter files
        assertNoFilterFiles();

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Add a file to the exclude filters twice
        page.getFilterTab().addFileToExcludeFilter(getFilterFileLocation());
        page.getFilterTab().addFileToExcludeFilter(getFilterFileLocation());

        // Accept the dialog
        dialog.okPressed();

        // Check that the file was added to the filter only once
        assertEmptyFilter(getProjectPreferences().getIncludeFilterFiles());
        assertSelectedFilter(getFilterFileProjectRelativePath(), getProjectPreferences().getExcludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeBugsFiles());
    }

    @Test
    public void testAddFileToIncludeFilter()  {
        // Check that there are no filter files
        assertNoFilterFiles();

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Add a file to the include filters
        page.getFilterTab().addFileToIncludeFilter(getFilterFileLocation());

        // Accept the dialog
        dialog.okPressed();

        // Check that the file was added to the filter
        assertSelectedFilter(getFilterFileProjectRelativePath(), getProjectPreferences().getIncludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeBugsFiles());
    }

    @Test
    public void testDeselectAllCategories()  {
        // Add all categories
        addAllBugCategories();
        assertAllBugCategoriesSelected(true);

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Remove all categories
        page.getReportTab().deselectAllBugCategories();

        // Accept the dialog
        dialog.okPressed();

        // Check that no categories are selected
        assertAllBugCategoriesSelected(false);
    }

    @Test
    public void testDisableAllDetectors()  {
        // Enable all detectors
        getProjectPreferences().enableAllDetectors(true);
        assertAllVisibleDetectorsEnabled(true);

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Disable all detectors
        page.getDetectorTab().disableAllDetectors();

        // Accept the dialog
        dialog.okPressed();

        // Check that all detectors are disabled
        assertAllVisibleDetectorsEnabled(false);
    }

    @Test
    public void testDisableFindBugs() throws CoreException {
        // Set the nature
        ProjectUtilities.addFindBugsNature(getProject(), new NullProgressMonitor());
        assertTrue(ProjectUtilities.hasFindBugsNature(getProject()));

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Disable FindBugs
        page.enableFindBugs(false);

        // Accept the dialog
        dialog.okPressed();
        joinJobFamily(FindbugsPlugin.class);

        // Check the project doesn't have the nature
        assertFalse(ProjectUtilities.hasFindBugsNature(getProject()));
    }

    @Test
    public void testDisableProjectProperties()  {
        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Accept the dialog, the plugin should have the project settings
        // enabled
        dialog.okPressed();
        assertTrue(FindbugsPlugin.isProjectSettingsEnabled(getProject()));

        // Create another properties page and another dialog
        page = createProjectPropertiesPage();
        dialog = createAndOpenProjectPropertiesDialog(page);
        page.assertProjectSettingsEnabled(true);

        // Disable the project settings
        page.enableProjectProperties(false);

        // Accept the dialog
        dialog.okPressed();

        // Check that the project settings are disabled
        assertFalse(FindbugsPlugin.isProjectSettingsEnabled(getProject()));

        // Create the third properties page and dialog, this time the project
        // settings
        // should be disabled
        page = createProjectPropertiesPage();
        dialog = createAndOpenProjectPropertiesDialog(page);
        page.assertProjectSettingsEnabled(false);

        // Accept the dialog
        dialog.okPressed();
    }

    @Test
    public void testEnableFindBugs() throws CoreException {
        // Reset the nature
        ProjectUtilities.removeFindBugsNature(getProject(), new NullProgressMonitor());
        assertFalse(ProjectUtilities.hasFindBugsNature(getProject()));

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Enable FindBugs
        page.enableFindBugs(true);

        // Accept the dialog
        dialog.okPressed();
        joinJobFamily(FindbugsPlugin.class);

        // Check the project has the nature
        assertTrue(ProjectUtilities.hasFindBugsNature(getProject()));
    }

    @Test
    public void testEnableOneDetector() {
        // Disable all detectors
        getProjectPreferences().enableAllDetectors(false);
        assertAllVisibleDetectorsEnabled(false);

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Enable one detector
        String detectorShortName = "FindReturnRef";
        page.getDetectorTab().enableDetector(detectorShortName);

        // Accept the dialog
        dialog.okPressed();

        // Check that the expected detector is enabled
        assertOnlyVisibleDetectorEnabled(detectorShortName);
    }

    @Test
    public void testOpenProjectPreferencePage() {
        // Create the preferences page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        page.assertProjectControlsVisible(true);

        // Accept the dialog
        dialog.okPressed();
    }

    @Test
    public void testOpenWorkspacePreferencePage() {
        // Create the preferences page and the dialog
        FindbugsPropertyPageTestSubclass page = createWorkspacePropertiesPage();
        PropertiesTestDialog dialog = createAndOpenWorkspacePreferencesDialog(page);

        page.assertProjectControlsVisible(false);

        // Accept the dialog
        dialog.okPressed();
    }

    @Test
    public void testRemoveFileFromExcludeFilter() throws CoreException {
        // Set the initial preferences with one filter
        setFilterFile(true);

        // Check that the filters are populated
        assertEmptyFilter(getProjectPreferences().getIncludeFilterFiles());
        assertSelectedFilter(getFilterFileLocation(), getProjectPreferences().getExcludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeBugsFiles());

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Remove the files from the exclude filters
        page.getFilterTab().removeFilesFromExcludeFilter();

        // Accept the dialog
        dialog.okPressed();

        // Check that there are no filter files
        assertNoFilterFiles();
    }

    @Test
    public void testSelectOneCategory() {
        // Remove all categories
        removeAllBugCategories();
        assertAllBugCategoriesSelected(false);

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Select one category
        String category = "BAD_PRACTICE";
        page.getReportTab().selectBugCategory(category);

        // Accept the dialog
        dialog.okPressed();

        // Check that one category is selected
        assertOnlySelectedBugCategory(category);
    }

    @Test
    public void testSetEffort() {
        // Reset the effort
        getProjectPreferences().setEffort(UserPreferences.EFFORT_DEFAULT);
        assertEquals(UserPreferences.EFFORT_DEFAULT, getProjectPreferences().getEffort());

        // Create the properties page and the dialog
        FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
        PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

        // Set 'max' effort
        page.setEffort(Effort.MAX);

        // Accept the dialog
        dialog.okPressed();

        // Check the effort has been stored
        assertEquals(UserPreferences.EFFORT_MAX, getProjectPreferences().getEffort());
    }

    private void addAllBugCategories() {
        getProjectPreferences().getFilterSettings().clearAllCategories();
    }

    private void assertAllBugCategoriesSelected(boolean enabled) {
        for (String category : DetectorFactoryCollection.instance().getBugCategories()) {
            assertEquals(enabled, getProjectPreferences().getFilterSettings().containsCategory(category));
        }
    }

    private void assertAllVisibleDetectorsEnabled(boolean enabled) {
        for (Iterator<DetectorFactory> factories = DetectorFactoryCollection.instance().factoryIterator(); factories.hasNext();) {
            DetectorFactory detectorFactory = factories.next();
            if (!detectorFactory.isHidden()) {
                assertEquals(enabled, getProjectPreferences().isDetectorEnabled(detectorFactory));
            }
        }
    }

    private void assertEmptyFilter(Map<String, Boolean> filters) {
        assertTrue(filters.isEmpty());
    }

    private void assertNoFilterFiles() {
        assertEmptyFilter(getProjectPreferences().getIncludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeFilterFiles());
        assertEmptyFilter(getProjectPreferences().getExcludeBugsFiles());
    }

    private void assertOnlySelectedBugCategory(String categoryName) {
        for (String currentCategory : DetectorFactoryCollection.instance().getBugCategories()) {
            boolean expectedEnablement = currentCategory.equals(categoryName);
            assertEquals(expectedEnablement, getProjectPreferences().getFilterSettings().containsCategory(currentCategory));
        }
    }

    private void assertOnlyVisibleDetectorEnabled(String detectorShortName) {
        for (Iterator<DetectorFactory> factories = DetectorFactoryCollection.instance().factoryIterator(); factories.hasNext();) {
            DetectorFactory detectorFactory = factories.next();
            if (!detectorFactory.isHidden()) {
                boolean expectedEnablement = detectorFactory.getShortName().equals(detectorShortName);
                assertEquals(expectedEnablement, getProjectPreferences().isDetectorEnabled(detectorFactory));
            }
        }
    }

    private void assertSelectedFilter(String expectedFilter, Map<String, Boolean> actualFilters) {
        assertEquals(1, actualFilters.size());
        assertTrue(actualFilters.containsKey(expectedFilter));
    }

    private PropertiesTestDialog createAndOpenProjectPropertiesDialog(FindbugsPropertyPageTestSubclass page) {
        PropertiesTestDialog dialog = new PropertiesTestDialog(getParentShell(), page);
        dialog.create();
        page.enableProjectProperties(true);
        dialog.open();
        return dialog;
    }

    private PropertiesTestDialog createAndOpenWorkspacePreferencesDialog(FindbugsPropertyPageTestSubclass page) {
        PropertiesTestDialog dialog = new PropertiesTestDialog(getParentShell(), page);
        dialog.create();
        dialog.open();
        return dialog;
    }

    private FindbugsPropertyPageTestSubclass createProjectPropertiesPage() {
        FindbugsPropertyPageTestSubclass page = new FindbugsPropertyPageTestSubclass();
        page.setElement(getProject());
        return page;
    }

    private FindbugsPropertyPageTestSubclass createWorkspacePropertiesPage() {
        FindbugsPropertyPageTestSubclass page = new FindbugsPropertyPageTestSubclass();
        return page;
    }

    private String getBugsFileProjectRelativePath() {
        return getProject().findMember(BUGS_XML_FILE).getProjectRelativePath().toOSString();
    }

    private String getFilterFileProjectRelativePath() {
        return getProject().findMember(FILTER_FILE).getProjectRelativePath().toOSString();
    }

    private Shell getParentShell() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        return shell;
    }

    private UserPreferences getWorkspacePreferences() {
        return FindbugsPlugin.getUserPreferences(null);
    }

    private void removeAllBugCategories() {
        for (String category : DetectorFactoryCollection.instance().getBugCategories()) {
            getProjectPreferences().getFilterSettings().removeCategory(category);
        }
    }
}
