/*
 * Contributions to FindBugs
 * Copyright (C) 2010, Andrei Loskutov
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
package de.tobject.findbugs.properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.preferences.FindBugsConstants;
import edu.umd.cs.findbugs.Version;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class WorkspaceSettingsTab extends Composite {

    private Button confirmSwitch;

    private Button switchTo;

    private final IPreferenceStore store;

    private Button confirmBuild;

    private final FindbugsPropertyPage page;

    private DetectorProvider detectorProvider;

    private Button cacheClassData;

    private Button runAsExtraJob;

    public WorkspaceSettingsTab(TabFolder tabFolder, final FindbugsPropertyPage page, int style) {
        super(tabFolder, style);
        this.page = page;
        setLayout(new GridLayout());
        store = page.getPreferenceStore();

        TabItem tabDetector = new TabItem(tabFolder, SWT.NONE);
        tabDetector.setText("Plugins and misc. Settings");
        tabDetector.setControl(this);

        Label versionLabel = new Label(this, SWT.NONE);
        versionLabel.setText("FindBugs version: " + Version.RELEASE);

        runAsExtraJob = new Button(this, SWT.CHECK);
        runAsExtraJob.setSelection(store.getBoolean(FindBugsConstants.KEY_RUN_ANALYSIS_AS_EXTRA_JOB));
        runAsExtraJob.setText("Run FB analysis as extra job (independent from build job)");
        runAsExtraJob.setToolTipText("Should improve user experience especially for the incremented compile");

        cacheClassData = new Button(this, SWT.CHECK);
        cacheClassData.setSelection(store.getBoolean(FindBugsConstants.KEY_CACHE_CLASS_DATA));
        cacheClassData.setText("Cache .class data (useful for slow file system && lot of RAM) (experimental)");
        cacheClassData.setToolTipText("Reuse .class data for the next FindBugs analysis. " +
                "The cache will survive until the next project build.");

        if(!isWorkspaceSettings()) {
            Label lbl = new Label(this,SWT.WRAP );
            lbl.setText("Currently, plugins can only be updated from the workspace settings");
            return;
        }
        ManagePathsWidget pathsWidget = new ManagePathsWidget(this);
        CheckboxTableViewer viewer = pathsWidget.createViewer("FindBugs Plugins",
                "See: <a href=\"http://www.ibm.com/developerworks/library/j-findbug2/\">'Writing custom plugins'</a>"
                        + " and <a href=\"http://fb-contrib.sourceforge.net/\">fb-contrib</a>: additional bug detectors package",
                        true); // set true to enable checkbox to allow enable/disable detectors without removing them
        detectorProvider = createDetectorProvider(viewer);
        pathsWidget.createButtonsArea(detectorProvider);
        detectorProvider.refresh();

        switchTo = new Button(this, SWT.CHECK);
        switchTo.setText("Switch to the FindBugs perspective after analysis");
        switchTo.setSelection(store.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS));
        switchTo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                store.setValue(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS, switchTo.getSelection());
            }
        });

        confirmSwitch = new Button(this, SWT.CHECK);
        confirmSwitch.setText("Ask before switching to the FindBugs perspective");
        confirmSwitch.setSelection(store.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH));
        confirmSwitch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                store.setValue(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH, confirmSwitch.getSelection());
            }
        });

        confirmBuild = new Button(this, SWT.CHECK);
        confirmBuild.setText("Remind to redo analysis after changes of relevant settings");
        confirmBuild.setSelection(!store.getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD));
        confirmBuild.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                store.setValue(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD, !confirmBuild.getSelection());
            }
        });
    }

    protected DetectorProvider createDetectorProvider(CheckboxTableViewer viewer) {
        final DetectorProvider filterProvider = new DetectorProvider(viewer, page);
        filterProvider.addListener(new Listener() {
            public void handleEvent(Event event) {
                page.setErrorMessage(null);
                filterProvider.refresh();
            }
        });
        return filterProvider;
    }

    public void refreshUI(UserPreferences prefs) {
        cacheClassData.setSelection(store.getBoolean(FindBugsConstants.KEY_CACHE_CLASS_DATA));
        runAsExtraJob.setSelection(store.getBoolean(FindBugsConstants.KEY_RUN_ANALYSIS_AS_EXTRA_JOB));
        if(!isWorkspaceSettings()) {
            return;
        }
        confirmSwitch.setSelection(store.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH));
        switchTo.setSelection(store.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS));
        confirmBuild.setSelection(!store.getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD));
        detectorProvider.setDetectorPlugins(prefs);
        detectorProvider.refresh();
    }

    /**
     * @return
     */
    private boolean isWorkspaceSettings() {
        return page.getProject() == null;
    }

    public void performOK() {
        store.setValue(FindBugsConstants.KEY_CACHE_CLASS_DATA, cacheClassData.getSelection());
        store.setValue(FindBugsConstants.KEY_RUN_ANALYSIS_AS_EXTRA_JOB, runAsExtraJob.getSelection());
        if(!isWorkspaceSettings()) {
            return;
        }
    }

}
