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
package de.tobject.findbugs.properties.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.properties.FindbugsPropertyPage.Effort;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.util.ProjectUtilities;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * This class tests the FindbugsPropertyPage and related classes.
 * 
 * @author Tomás Pollak
 */
public class PropertiesPageTest extends AbstractFindBugsTest {

	private UserPreferences originalProjectPreferences;
	private UserPreferences originalWorkspacePreferences;

	@Override
	public void setUp() throws CoreException, IOException {
		super.setUp();

		// Save the preferences and restore them after the test
		originalWorkspacePreferences = getWorkspacePreferences();
		originalProjectPreferences = getProjectPreferences();
	}

	@Override
	public void tearDown() throws CoreException {
		// Restore the preferences to the original state
		FindbugsPlugin.saveUserPreferences(getProject(), originalProjectPreferences);

		super.tearDown();
	}

	@Test
	public void testDeselectAllCategories() throws CoreException {
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
	public void testDisableAllDetectors() throws CoreException {
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
	public void testDisableProjectProperties() throws CoreException {
		// Create the properties page and the dialog
		FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
		PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);
		
		// Accept the dialog, the plugin should have the project settings enabled
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

		// Create the third properties page and dialog, this time the project settings should be disabled
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

		// Check the project has the nature
		assertTrue(ProjectUtilities.hasFindBugsNature(getProject()));
	}

	@Test
	public void testEnableOneDetector() throws CoreException {
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
	public void testOpenProjectPreferencePage() throws CoreException {
		// Create the preferences page and the dialog
		FindbugsPropertyPageTestSubclass page = createProjectPropertiesPage();
		PropertiesTestDialog dialog = createAndOpenProjectPropertiesDialog(page);

		page.assertProjectControlsVisible(true);

		// Accept the dialog
		dialog.okPressed();
	}

	@Test
	public void testOpenWorkspacePreferencePage() throws CoreException {
		// Create the preferences page and the dialog
		FindbugsPropertyPageTestSubclass page = createWorkspacePropertiesPage();
		PropertiesTestDialog dialog = createAndOpenWorkspacePreferencesDialog(page);

		page.assertProjectControlsVisible(false);

		// Accept the dialog
		dialog.okPressed();
	}

	@Test
	public void testSelectOneCategory() throws CoreException {
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
	public void testSetEffort() throws CoreException {
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
		for (String category : I18N.instance().getBugCategories()) {
			assertEquals(enabled, getProjectPreferences().getFilterSettings()
					.containsCategory(category));
		}
	}

	private void assertAllVisibleDetectorsEnabled(boolean enabled) {
		for (Iterator<DetectorFactory> factories = DetectorFactoryCollection.instance()
				.factoryIterator(); factories.hasNext();) {
			DetectorFactory detectorFactory = factories.next();
			if (!detectorFactory.isHidden()) {
				assertEquals(enabled, getProjectPreferences().isDetectorEnabled(
						detectorFactory));
			}
		}
	}

	private void assertOnlySelectedBugCategory(String categoryName) {
		for (String currentCategory : I18N.instance().getBugCategories()) {
			boolean expectedEnablement = currentCategory.equals(categoryName);
			assertEquals(expectedEnablement, getProjectPreferences().getFilterSettings()
					.containsCategory(currentCategory));
		}
	}

	private void assertOnlyVisibleDetectorEnabled(String detectorShortName) {
		for (Iterator<DetectorFactory> factories = DetectorFactoryCollection.instance()
				.factoryIterator(); factories.hasNext();) {
			DetectorFactory detectorFactory = factories.next();
			if (!detectorFactory.isHidden()) {
				boolean expectedEnablement = detectorFactory.getShortName().equals(
						detectorShortName);
				assertEquals(expectedEnablement, getProjectPreferences()
						.isDetectorEnabled(detectorFactory));
			}
		}
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

	private Shell getParentShell() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		return shell;
	}

	private UserPreferences getProjectPreferences() {
		return FindbugsPlugin.getProjectPreferences(getProject(), false);
	}

	private UserPreferences getWorkspacePreferences() {
		return FindbugsPlugin.getUserPreferences(null);
	}

	private void removeAllBugCategories() {
		for (String category : I18N.instance().getBugCategories()) {
			getProjectPreferences().getFilterSettings().removeCategory(category);
		}
	}
}
