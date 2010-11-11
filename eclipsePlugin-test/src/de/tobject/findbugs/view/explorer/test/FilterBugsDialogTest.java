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
package de.tobject.findbugs.view.explorer.test;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.test.AbstractFindBugsTest;
import de.tobject.findbugs.test.TestScenario;
import de.tobject.findbugs.view.explorer.FilterBugsDialog;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;

/**
 * This class tests the FilterBugsDialog and its related classes.
 *
 * @author Tom�s Pollak
 */
public class FilterBugsDialogTest extends AbstractFindBugsTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        setUpTestProject(TestScenario.DEFAULT);
    }

    @AfterClass
    public static void tearDownClass() throws CoreException {
        tearDownTestProject();
    }

    private String originalFilteredIds;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Hold the original filter value and restore it after the test
        originalFilteredIds = getFilteredIds();
    }

    @Override
    public void tearDown() throws CoreException {
        // Restore the original filter value
        storeFilteredIds(originalFilteredIds);
        super.tearDown();
    }

    @Test
    public void testEmptyFilter() {
        // Open the dialog
        FilterBugsDialogTestSubclass dialog = openFilterBugsDialog(Collections.<BugPattern> emptySet(),
                Collections.<BugCode> emptySet());

        // Close the dialog
        closeDialogAndStoreResult(dialog);

        // Check the results
        assertTrue(FindbugsPlugin.getFilteredPatterns().isEmpty());
        assertTrue(FindbugsPlugin.getFilteredPatternTypes().isEmpty());
    }

    @Test
    public void testFullFilter() {
        // Open the dialog
        FilterBugsDialogTestSubclass dialog = openFilterBugsDialog(FindbugsPlugin.getKnownPatterns(),
                FindbugsPlugin.getKnownPatternTypes());

        // Close the dialog
        closeDialogAndStoreResult(dialog);

        // We expect all the pattern types and no patterns (they are included)
        assertEquals(FindbugsPlugin.getKnownPatternTypes(), FindbugsPlugin.getFilteredPatternTypes());
        assertTrue(FindbugsPlugin.getFilteredPatterns().isEmpty());
    }

    @Test
    public void testOnePatternSelectsPattern() {
        // Open the dialog
        FilterBugsDialogTestSubclass dialog = openFilterBugsDialog(Collections.<BugPattern> emptySet(),
                Collections.<BugCode> emptySet());

        // Add one BugPattern
        BugPattern pattern = DetectorFactoryCollection.instance().lookupBugPattern("HE_EQUALS_NO_HASHCODE");
        dialog.addBugPatternToFilter(pattern);

        // Close the dialog
        closeDialogAndStoreResult(dialog);

        // We expect 'HE_EQUALS_NO_HASHCODE' to be selected and no bug code,
        // since there are more patterns for the 'HE' bug code.
        assertEquals(Collections.singleton(pattern), FindbugsPlugin.getFilteredPatterns());
        assertTrue(FindbugsPlugin.getFilteredPatternTypes().isEmpty());
    }

    @Test
    public void testOnePatternSelectsType() {
        // Open the dialog
        FilterBugsDialogTestSubclass dialog = openFilterBugsDialog(Collections.<BugPattern> emptySet(),
                Collections.<BugCode> emptySet());

        // Add one BugPattern
        BugPattern pattern = DetectorFactoryCollection.instance().lookupBugPattern("EI_EXPOSE_REP");
        dialog.addBugPatternToFilter(pattern);

        // Close the dialog
        closeDialogAndStoreResult(dialog);

        // We expect the 'EI' bug code to be selected, since 'EI_EXPOSE_REP'
        // is the only pattern for that code.
        assertTrue(FindbugsPlugin.getFilteredPatterns().isEmpty());
        BugCode expectedBugCode = DetectorFactoryCollection.instance().getBugCode("EI");
        assertEquals(Collections.singleton(expectedBugCode), FindbugsPlugin.getFilteredPatternTypes());
    }

    @Test
    public void testOneType() {
        // Open the dialog
        FilterBugsDialogTestSubclass dialog = openFilterBugsDialog(Collections.<BugPattern> emptySet(),
                Collections.<BugCode> emptySet());

        // Add one BugCode
        BugCode bugCode = DetectorFactoryCollection.instance().getBugCode("EI");
        dialog.addBugCodeToFilter(bugCode);

        // Close the dialog
        closeDialogAndStoreResult(dialog);

        // We expect the 'EI' bug code to be selected
        assertTrue(FindbugsPlugin.getFilteredPatterns().isEmpty());
        assertEquals(Collections.singleton(bugCode), FindbugsPlugin.getFilteredPatternTypes());
    }

    private void closeDialogAndStoreResult(FilterBugsDialog dialog) {
        dialog.close();
        String selectedIds = dialog.getSelectedIds();
        storeFilteredIds(selectedIds);
    }

    private String getFilteredIds() {
        return getPreferenceStore().getString(FindBugsConstants.LAST_USED_EXPORT_FILTER);
    }

    private Shell getParentShell() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        return shell;
    }

    private FilterBugsDialogTestSubclass openFilterBugsDialog(Set<BugPattern> filteredPatterns, Set<BugCode> filteredTypes) {
        FilterBugsDialogTestSubclass dialog = new FilterBugsDialogTestSubclass(getParentShell(), filteredPatterns, filteredTypes);
        dialog.open();
        return dialog;
    }

    private void storeFilteredIds(String selectedIds) {
        getPreferenceStore().setValue(FindBugsConstants.LAST_USED_EXPORT_FILTER, selectedIds);
    }
}
