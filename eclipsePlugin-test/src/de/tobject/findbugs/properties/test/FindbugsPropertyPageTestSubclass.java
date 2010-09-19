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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TabFolder;
import org.junit.Assert;

import de.tobject.findbugs.properties.DetectorConfigurationTab;
import de.tobject.findbugs.properties.FilterFilesTab;
import de.tobject.findbugs.properties.FindbugsPropertyPage;
import de.tobject.findbugs.properties.ReportConfigurationTab;

/**
 * Test subclass of FindbugsPropertyPage that provides methods for handling the
 * properties page for testing purposes.
 * 
 * @author Tomás Pollak
 */
public class FindbugsPropertyPageTestSubclass extends FindbugsPropertyPage {

    private DetectorConfigurationTabTestSubclass detectorTab;

    private FilterFilesTabTestSubclass filterTab;

    private ReportConfigurationTabTestSubclass reportTab;

    public FindbugsPropertyPageTestSubclass() {
    }

    /**
     * Some widgets of the properties page are only created if it is opened for
     * a project and not for the workspace. This is a custom assertion that
     * verifies this behaviour.
     * 
     * @param expectsProject
     *            true if expecting the controls for a project, false for the
     *            workspace
     */
    public void assertProjectControlsVisible(boolean expectsProject) {
        boolean chkEnableFindBugsVisible = getChkEnableFindBugs() != null;
        boolean chkEnableProjectVisible = getEnableProjectCheck() != null;
        boolean hasProject = getProject() != null;

        Assert.assertTrue(expectsProject == hasProject);
        Assert.assertTrue(expectsProject == chkEnableFindBugsVisible);
        Assert.assertTrue(expectsProject == chkEnableProjectVisible);
    }

    /**
     * When the page is opened for a project, but the project doesn't have any
     * properties, the workspace preferences are inherited. In that case, many
     * controls should be disabled. This is a custom assertion that verifies
     * this behaviour.
     * 
     * @param enabled
     *            true if the controls should be enabled, false otherwise
     */
    public void assertProjectSettingsEnabled(boolean enabled) {
        Assert.assertEquals(enabled, getDetectorTab().isEnabled());
        Assert.assertEquals(enabled, getReportTab().isEnabled());
        Assert.assertEquals(enabled, getFilterTab().isEnabled());
        Assert.assertEquals(enabled, getEffortViewer().getCombo().isEnabled());
    }

    public void enableFindBugs(boolean enable) {
        getChkEnableFindBugs().setSelection(enable);
    }

    public void enableProjectProperties(boolean enable) {
        getEnableProjectCheck().setSelection(enable);
    }

    public DetectorConfigurationTabTestSubclass getDetectorTab() {
        return detectorTab;
    }

    public FilterFilesTabTestSubclass getFilterTab() {
        return filterTab;
    }

    public ReportConfigurationTabTestSubclass getReportTab() {
        return reportTab;
    }

    public void setEffort(Effort effort) {
        getCurrentUserPreferences().setEffort(effort.getEffortLevel());
    }

    @Override
    protected DetectorConfigurationTab createDetectorConfigurationTab(TabFolder tabFolder) {
        detectorTab = new DetectorConfigurationTabTestSubclass(tabFolder, this, SWT.NONE);
        return detectorTab;
    }

    @Override
    protected FilterFilesTab createFilterFilesTab(TabFolder tabFolder) {
        filterTab = new FilterFilesTabTestSubclass(tabFolder, this, SWT.NONE);
        return filterTab;
    }

    @Override
    protected ReportConfigurationTab createReportConfigurationTab(TabFolder tabFolder) {
        reportTab = new ReportConfigurationTabTestSubclass(tabFolder, this, SWT.NONE);
        return reportTab;
    }

    @Override
    protected void remindAboutFullBuild() {
        // Don't do anything
    }
}
