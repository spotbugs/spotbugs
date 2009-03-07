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

import de.tobject.findbugs.properties.DetectorConfigurationTab;
import de.tobject.findbugs.properties.FilterFilesTab;
import de.tobject.findbugs.properties.FindbugsPropertyPage;
import de.tobject.findbugs.properties.ReportConfigurationTab;

/**
 * Test subclass of FindbugsPropertyPage that provides methods for handling the properties
 * page for testing purposes.
 * 
 * @author Tomás Pollak
 */
public class FindbugsPropertyPageTestSubclass extends FindbugsPropertyPage {

	private DetectorConfigurationTabTestSubclass detectorTab;
	private ReportConfigurationTabTestSubclass reportTab;

	public FindbugsPropertyPageTestSubclass() {
	}

	public void enableFindBugs(boolean enable) {
		getChkEnableFindBugs().setSelection(enable);
	}

	public void enableProjectProperties() {
		getEnableProjectCheck().setSelection(true);
	}

	public DetectorConfigurationTabTestSubclass getDetectorTab() {
		return detectorTab;
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
		return new FilterFilesTabTestSubclass(tabFolder, this, SWT.NONE);
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
