/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004-2005, University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package de.tobject.findbugs.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.actions.FindBugsAction;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.preferences.FindBugsPreferenceInitializer;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.ProjectUtilities;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.Cloud;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.cloud.DoNothingCloud;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Combined workspace/project properties page for setting FindBugs properties.
 * <p>
 * There are two different preference files: FindBugs core preferences, saved in
 * the "*.fbprefs" file, and Eclipse plugin preferences, saved in the
 * "edu.umd.cs.findbugs.plugin.eclipse.prefs" file. The difference is, that FB
 * core prefs are saved/read by the FB core API and there should go all FB
 * engine settings and none of Eclipse related stuff.
 * <p>
 * To retrieve FB core preferences, one should use
 * {@link #getOriginalUserPreferences()} and
 * {@link #getCurrentUserPreferences()}.
 * <p>
 * To retrieve Eclipse plugin preferences, one should use
 * {@link #getPreferenceStore()}.
 * <p>
 * In both cases project settings are only available in the context of the
 * project properties page if the project settings are enabled, global settings
 * are used otherwise.
 *
 * @author Andrei Loskutov
 * @author Peter Friese
 * @author David Hovemeyer
 * @author Phil Crosby
 * @version 2.0
 * @since 17.06.2004
 */
public class FindbugsPropertyPage extends PropertyPage implements IWorkbenchPreferencePage {

    private boolean natureEnabled;

    private UserPreferences origUserPreferences;

    private UserPreferences currentUserPreferences;

    private IProject project;

    private Button chkEnableFindBugs;

    private Button chkRunAtFullBuild;

    private Button restoreDefaultsButton;

    private ComboViewer effortViewer;

    private TabFolder tabFolder;

    private DetectorConfigurationTab detectorTab;

    private FilterFilesTab filterFilesTab;

    private ReportConfigurationTab reportConfigurationTab;

    private final Map<DetectorFactory, Boolean> visibleDetectors;

    private Button enableProjectCheck;

    private Link workspaceSettingsLink;

    private boolean projectPropsInitiallyEnabled;

    //  Nonnull if there is a current project
    @Nullable
    private ScopedPreferenceStore projectStore;

    /** never null */
    private ScopedPreferenceStore workspaceStore;

    private WorkspaceSettingsTab workspaceSettingsTab;

    private List<CloudPlugin> clouds;

    private Combo cloudCombo;

    private Label cloudLabel;

    /**
     * Constructor for FindbugsPropertyPage.
     */
    public FindbugsPropertyPage() {
        super();
        visibleDetectors = new HashMap<DetectorFactory, Boolean>();
    }

    @Override
    protected Control createContents(Composite parent) {

        noDefaultAndApplyButton();

        // getElement returns the element this page has been opened for,
        // in our case this is a Java Project (IJavaProject).
        IAdaptable resource = getElement();
        if (resource != null) {
            project = (IProject) resource.getAdapter(IProject.class);
        }

        initPreferencesStore(project);

        // initially trigger load of all custom FB plugins if not yet loaded
        FindbugsPlugin.applyCustomDetectors(false);

        createGlobalElements(parent);

        createConfigurationTabFolder(parent);

        createDefaultsButton(parent);

        setProjectEnabled(enableProjectCheck == null || enableProjectCheck.getSelection());

        return parent;
    }

    private void initPreferencesStore(IProject currProject) {
        workspaceStore = new ScopedPreferenceStore(new InstanceScope(), FindbugsPlugin.PLUGIN_ID);
        if (currProject != null) {
            projectStore = new ScopedPreferenceStore(new ProjectScope(currProject), FindbugsPlugin.PLUGIN_ID);
            projectPropsInitiallyEnabled = FindbugsPlugin.isProjectSettingsEnabled(currProject);
            if (!projectPropsInitiallyEnabled) {
                // use workspace properties instead
                currProject = null;
            }
            setPreferenceStore(projectStore);
        } else {
            setPreferenceStore(workspaceStore);
        }
        loadPreferences(currProject);
    }

    /**
     * @param currProject
     *            if null, workspace properties are used
     */
    private UserPreferences loadPreferences(IProject currProject) {
        // Get current user preferences for project
        if (currProject == null) {
            origUserPreferences = FindbugsPlugin.getCorePreferences(null, true);
        } else {
            origUserPreferences = FindbugsPlugin.getProjectPreferences(currProject, true);
        }
        currentUserPreferences = origUserPreferences.clone();
        return currentUserPreferences;
    }

    private void createConfigurationTabFolder(Composite composite) {
        tabFolder = new TabFolder(composite, SWT.TOP);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL
                | GridData.GRAB_VERTICAL);
        layoutData.verticalIndent = -5;
        tabFolder.setLayoutData(layoutData);

        reportConfigurationTab = createReportConfigurationTab(tabFolder);
        filterFilesTab = createFilterFilesTab(tabFolder);
        workspaceSettingsTab = createWorkspaceSettings(tabFolder);
        detectorTab = createDetectorConfigurationTab(tabFolder);
    }

    private WorkspaceSettingsTab createWorkspaceSettings(TabFolder parentTabFolder) {
        return new WorkspaceSettingsTab(parentTabFolder, this, SWT.NONE);
    }

    private void createDefaultsButton(Composite composite) {
        restoreDefaultsButton = new Button(composite, SWT.NONE);
        restoreDefaultsButton.setText(getMessage("property.restoreSettings"));
        restoreDefaultsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        restoreDefaultsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                restoreDefaultSettings();
            }
        });
    }

    private void createGlobalElements(Composite parent) {
        if (getProject() != null) {
            createWorkspaceButtons(parent);
        }

        Composite globalGroup = new Composite(parent, SWT.TOP);
        GridLayout layout = new GridLayout(3, false);
        //layout.marginHeight = 0;
        //layout.marginWidth = 0;
        globalGroup.setLayout(layout);
        GridData layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        //layoutData.verticalIndent = -2;

        globalGroup.setLayoutData(layoutData);

        natureEnabled = ProjectUtilities.hasFindBugsNature(getProject());

        if (getProject() != null) {
            chkEnableFindBugs = new Button(globalGroup, SWT.CHECK);
            chkEnableFindBugs.setText(getMessage("property.runAuto"));
            chkEnableFindBugs.setSelection(natureEnabled);
            chkEnableFindBugs.setToolTipText(getMessage("property.runAuto.tip"));

            chkEnableFindBugs.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean enabled = chkEnableFindBugs.getSelection();
                    chkRunAtFullBuild.setEnabled(enabled);
                }
            });

            chkRunAtFullBuild = new Button(globalGroup, SWT.CHECK);
            chkRunAtFullBuild.setText(getMessage("property.runFull"));
            chkRunAtFullBuild.setSelection(origUserPreferences.isRunAtFullBuild());
            chkRunAtFullBuild.setToolTipText(getMessage("property.runFull.tip"));
            chkRunAtFullBuild.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent event) {
                    boolean selection = chkRunAtFullBuild.getSelection();
                    currentUserPreferences.setRunAtFullBuild(selection);
                }
            });
            chkRunAtFullBuild.setEnabled(chkEnableFindBugs.getSelection());
        }

        Composite prioGroup = new Composite(globalGroup, SWT.NONE);
        GridLayout prioLayout = new GridLayout(2, false);
        prioGroup.setLayout(prioLayout);
        layoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        layoutData.horizontalIndent = -5;
        //layoutData.verticalIndent = -5;
        prioGroup.setLayoutData(layoutData);

        // effort
        Label effortLabel = new Label(prioGroup, SWT.NULL);
        effortLabel.setText(getMessage("property.effort"));
        effortViewer = new ComboViewer(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        effortViewer.add(Effort.values());

        String effortLevel = currentUserPreferences.getEffort();
        effortViewer.setSelection(new StructuredSelection(Effort.getEffort(effortLevel)), true);
        effortViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                Effort placeHolder = (Effort) ((IStructuredSelection) event.getSelection()).getFirstElement();
                currentUserPreferences.setEffort(placeHolder.getEffortLevel());
            }
        });
        effortLabel.setToolTipText("Set FindBugs analysis effort (minimal is faster but less precise)");
        effortViewer.getCombo().setToolTipText("Set FindBugs analysis effort (minimal is faster but less precise)");


        cloudLabel = new Label(globalGroup, SWT.NONE);
        cloudCombo = new Combo(globalGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        cloudCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

        enableOrDisableCloudControls();
        String cloudid = getCloudId();

        clouds = new ArrayList<CloudPlugin>();
        populateCloudsCombo(cloudid);
    }

    private int populateCloudsCombo(String cloudid) {
        int i = 0;
        boolean cloudSelected = false;
        int defaultIndex = -1;
        for (CloudPlugin cloud : DetectorFactoryCollection.instance().getRegisteredClouds().values()) {
            if (cloud.isHidden() && !cloud.getId().equals(cloudid)) {
                continue;
            }
            cloudCombo.add(cloud.getDescription());
            clouds.add(cloud);
            if (cloud.getId().equals(cloudid)) {
                cloudCombo.select(i);
                cloudSelected = true;
            }
            if (cloud.getId().equals(CloudFactory.DEFAULT_CLOUD)) {
                defaultIndex = i;
            }
            i++;
        }
        if (!cloudSelected && cloudid != null && cloudid.trim().length() > 0) {
            if (defaultIndex != -1) {
                cloudCombo.select(defaultIndex);
            } else {
                // should not happen: default local cloud should be available
                FindbugsPlugin.getDefault().logWarning("Failed to find default local cloud (edu.umd.cs.findbugs.cloud.Local)");
            }
        }
        return defaultIndex;
    }

    private String getCloudId() {
        final IProject eclipseProj = getProject();
        String cloudid = null;
        if (eclipseProj != null) {
            SortedBugCollection collection = FindbugsPlugin.getBugCollectionIfSet(eclipseProj);
            if (collection != null) {
                Cloud cloud = collection.getCloud();
                if (!(cloud instanceof DoNothingCloud)) {
                    cloudid = cloud.getPlugin().getId();
                }
            }
        }
        if (cloudid == null) {
            cloudid = currentUserPreferences.getCloudId();
        }
        if (cloudid == null) {
            cloudid =  CloudFactory.DEFAULT_CLOUD;
        }
        return cloudid;
    }

    private IProject enableOrDisableCloudControls() {
        IProject eclipseProj = getProject();
        String txt = "Store issue evaluations in:";
        if (eclipseProj == null) {
            cloudLabel.setEnabled(false);
            cloudCombo.setEnabled(false);
            cloudLabel.setText(txt + "\n(only configurable at the project level)");
        } else {
            cloudLabel.setEnabled(true);
            cloudCombo.setEnabled(true);
            cloudLabel.setText(txt);
        }
        return eclipseProj;
    }

    private void createWorkspaceButtons(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.numColumns = 2;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        enableProjectCheck = createLabeledCheck("Enable project specific settings",
                "These settings would be used for the current project only", projectPropsInitiallyEnabled, composite);

        enableProjectCheck.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                boolean enabled = enableProjectCheck.getSelection();
                IProject currProject;
                if (enabled) {
                    currProject = getProject();
                } else {
                    currProject = null;
                }
                refreshUI(loadPreferences(currProject));
                setProjectEnabled(enabled);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                // ignored
            }
        });

        workspaceSettingsLink = createLink(composite, "Configure Workspace Settings...");
        workspaceSettingsLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));

        Label sep = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        sep.setLayoutData(gridData);
    }

    protected void refreshUI(UserPreferences prefs) {
        visibleDetectors.clear();
        effortViewer.setSelection(new StructuredSelection(Effort.getEffort(prefs.getEffort())), true);
        detectorTab.refreshUI(prefs);
        filterFilesTab.refreshUI(prefs);
        reportConfigurationTab.refreshUI(prefs);
        if (workspaceSettingsTab != null) {
            workspaceSettingsTab.refreshUI(prefs);
        }
        String cloudid = getCloudId();
        cloudCombo.removeAll();
        clouds.clear();
        populateCloudsCombo(cloudid);
    }

    private Link createLink(Composite composite, String text) {
        Link link = new Link(composite, SWT.NONE);
        link.setFont(composite.getFont());
        link.setText("<A>" + text + "</A>");
        link.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                String id = "FindBugsPreferencePage";
                int result = PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null).open();
                if (result == Window.OK) {
                    // refresh prefs: workspace settings may change
                    refreshUI(loadPreferences(null));
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        link.setToolTipText("Configure global workspace (user) settings");
        return link;
    }

    /**
     * Enable/disable all UI elements except for project props enabled check
     *
     * @param selection
     */
    protected void setProjectEnabled(boolean selection) {
        // chkEnableFindBugs.setEnabled(selection);
        // chkRunAtFullBuild.setEnabled(selection &&
        // chkEnableFindBugs.getSelection());
        if (enableProjectCheck != null) {
            // this link should always be enabled
            //workspaceSettingsLink.setEnabled(!selection);
        }
        detectorTab.setEnabled(selection);
        filterFilesTab.setEnabled(selection);
        reportConfigurationTab.setEnabled(selection);
        restoreDefaultsButton.setEnabled(selection);
        effortViewer.getCombo().setEnabled(selection);
        if (selection) {
            enableOrDisableCloudControls();
        } else {
            cloudCombo.setEnabled(false);
        }
    }

    protected static Button createLabeledCheck(String title, String tooltip, boolean value, Composite defPanel) {
        Button fButton = new Button(defPanel, SWT.CHECK | SWT.LEFT);
        GridData data = new GridData();
        fButton.setLayoutData(data);
        fButton.setText(title);
        fButton.setSelection(value);
        fButton.setToolTipText(tooltip);
        return fButton;
    }

    /**
     * Restore default settings. This just changes the dialog widgets - the user
     * still needs to confirm by clicking the "OK" button.
     */
    private void restoreDefaultSettings() {
        if (getProject() != null) {
            // By default, don't run FindBugs automatically
            chkEnableFindBugs.setSelection(false);
            chkRunAtFullBuild.setEnabled(false);
            FindBugsPreferenceInitializer.restoreDefaults(projectStore);
        } else {
            FindBugsPreferenceInitializer.restoreDefaults(workspaceStore);
        }
        currentUserPreferences = FindBugsPreferenceInitializer.createDefaultUserPreferences();
        refreshUI(currentUserPreferences);
    }

    @Override
    protected void performDefaults() {
        // no-op because our defaults button is custom-made
        super.performDefaults();
    }

    /**
     * Will be called when the user presses the OK button.
     *
     * @see IPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        reportConfigurationTab.performOk();
        boolean analysisSettingsChanged = false;
        boolean reporterSettingsChanged = false;
        boolean needRedisplayMarkers = false;
        if (workspaceSettingsTab != null) {
            workspaceSettingsTab.performOK();
        }

        IProject eclipseProj = getProject();
        if (eclipseProj != null) {
            CloudPlugin item = clouds.get(cloudCombo.getSelectionIndex());
            if (item != null) {
                currentUserPreferences.setCloudId(item.getId());

                SortedBugCollection collection = FindbugsPlugin.getBugCollectionIfSet(eclipseProj);
                if (collection != null) {
                    Project fbProject = collection.getProject();
                    if (fbProject != null && !item.getId().equals(fbProject.getCloudId())) {
                        fbProject.setCloudId(item.getId());
                        collection.reinitializeCloud();
                        IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage();
                        if (page != null) {
                            IViewPart view = page.findView(FindbugsPlugin.TREE_VIEW_ID);
                            if (view instanceof CommonNavigator) {
                                CommonNavigator nav = ((CommonNavigator) view);
                                nav.getCommonViewer().refresh(true);
                            }
                        }
                    }
                }
            }
        }
        boolean pluginsChanged = false;
        // Have user preferences for project changed?
        // If so, write them to the user preferences file & re-run builder
        if (!currentUserPreferences.equals(origUserPreferences)) {
            pluginsChanged = !currentUserPreferences.getCustomPlugins().equals(origUserPreferences.getCustomPlugins());
            // save only if we in the workspace page OR in the project page with
            // enabled
            // project settings
            if (getProject() == null || enableProjectCheck.getSelection()) {
                try {
                    FindbugsPlugin.saveUserPreferences(getProject(), currentUserPreferences);
                } catch (CoreException e) {
                    FindbugsPlugin.getDefault().logException(e, "Could not store FindBugs preferences for project");
                }
            }
            if(pluginsChanged) {
                FindbugsPlugin.applyCustomDetectors(true);
            }
        }

        analysisSettingsChanged = pluginsChanged || areAnalysisPrefsChanged(currentUserPreferences, origUserPreferences);

        reporterSettingsChanged = !currentUserPreferences.getFilterSettings().equals(origUserPreferences.getFilterSettings());

        boolean markerSeveritiesChanged = reportConfigurationTab.isMarkerSeveritiesChanged();

        needRedisplayMarkers = pluginsChanged || markerSeveritiesChanged || reporterSettingsChanged;

        boolean builderEnabled = false;

        if (getProject() != null) {
            builderEnabled = chkEnableFindBugs.getSelection();
            // Update whether or not FindBugs is run automatically.
            if (!natureEnabled && builderEnabled) {
                addNature();
            } else if (natureEnabled && !builderEnabled) {
                removeNature();
            }

            // update the flag to match the incremental/not property
            builderEnabled &= chkRunAtFullBuild.getSelection();
            boolean newSelection = enableProjectCheck.getSelection();
            if (projectPropsInitiallyEnabled != newSelection) {
                analysisSettingsChanged = true;
                FindbugsPlugin.setProjectSettingsEnabled(project, projectStore, newSelection);
            }
        }

        if (analysisSettingsChanged) {
            // trigger a Findbugs rebuild here
            if (builderEnabled) {
                runFindbugsBuilder();
                needRedisplayMarkers = false;
            } else {
                if (!getPreferenceStore().getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD)) {
                    remindAboutFullBuild();
                }
            }
        }

        if (needRedisplayMarkers) {
            redisplayMarkers();
        }

        return true;
    }

    protected void remindAboutFullBuild() {
        MessageDialogWithToggle dialog = MessageDialogWithToggle.openInformation(getShell(), "Full FindBugs build required",
                "FindBugs analysis settings are changed." + "\nReexecute FindBugs analysis to see updated results.",
                "Do not show this warning again", false, null, null);

        getPreferenceStore().setValue(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD, dialog.getToggleState());
    }

    private void redisplayMarkers() {
        // if filter settings changed, and builder is not enabled, manually
        // trigger update
        if (project != null) {
            MarkerUtil.redisplayMarkers(JavaCore.create(project));
        } else {
            // trigger redisplay for workbench change too
            List<IProject> projects = ProjectUtilities.getFindBugsProjects();
            for (IProject aProject : projects) {
                MarkerUtil.redisplayMarkers(JavaCore.create(aProject));
            }
        }
    }

    private boolean areAnalysisPrefsChanged(UserPreferences pref1, UserPreferences pref2) {
        String effort1 = "" + pref1.getEffort();
        String effort2 = pref2.getEffort();
        return !effort1.equals(effort2) || isDetectorConfigurationChanged(pref1, pref2)
                || !pref1.getExcludeBugsFiles().equals(pref2.getExcludeBugsFiles())
                || !pref1.getExcludeFilterFiles().equals(pref2.getExcludeFilterFiles())
                || !pref1.getIncludeFilterFiles().equals(pref2.getIncludeFilterFiles());
    }

    boolean isDetectorConfigurationChanged(UserPreferences pref1, UserPreferences pref2) {

        Iterator<DetectorFactory> iterator = DetectorFactoryCollection.instance().factoryIterator();
        while (iterator.hasNext()) {
            DetectorFactory factory = iterator.next();
            // Only compare non-hidden factories
            if (factory.isHidden() && !detectorTab.isHiddenVisible()) {
                continue;
            }
            if (pref1.isDetectorEnabled(factory) ^ pref2.isDetectorEnabled(factory)) {
                return true;
            }
        }
        return false;
    }

    protected IProject getProject() {
        return project;
    }

    private void runFindbugsBuilder() {
        IProject myProject = getProject();
        if (myProject != null) {
            runBuild(myProject);
        } else {
            // workspace settings change: trigger workspace build
            List<IProject> projects = ProjectUtilities.getFindBugsProjects();
            for (IProject iProject : projects) {
                runBuild(iProject);
            }
        }
    }

    /**
     * Triggers FB analysis on given project
     * @param myProject opened project with FindBugs nature
     */
    private static void runBuild(@Nonnull IProject myProject) {
        StructuredSelection selection = new StructuredSelection(myProject);
        FindBugsAction action = new FindBugsAction();
        action.selectionChanged(null, selection);
        action.run(null);
    }

    /**
     * Add the nature to the current project. The real work is done by the inner
     * class NatureWorker
     */
    private void addNature() {
        NatureWorker worker = new NatureWorker(true);
        worker.scheduleInteractive();
    }

    /**
     * Remove the nature from the project.
     */
    private void removeNature() {
        NatureWorker worker = new NatureWorker(false);
        worker.scheduleInteractive();
    }

    private final class NatureWorker extends FindBugsJob {
        private boolean add = true;

        public NatureWorker(boolean add) {
            super((add ? "Adding FindBugs nature to " : "Removing FindBugs nature from ") + getProject(), getProject());
            this.add = add;
            // adding/removing nature uses workspace scope
            setRule(ResourcesPlugin.getWorkspace().getRoot());
        }

        @Override
        protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
            if (add) {
                ProjectUtilities.addFindBugsNature(getProject(), monitor);
            } else {
                ProjectUtilities.removeFindBugsNature(getProject(), monitor);
            }
        }
    }

    /**
     * Helper method to shorten message access
     *
     * @param key
     *            a message key
     * @return requested message
     */
    protected static String getMessage(String key) {
        return FindbugsPlugin.getDefault().getMessage(key);
    }

    protected UserPreferences getCurrentUserPreferences() {
        return currentUserPreferences;
    }

    UserPreferences getOriginalUserPreferences() {
        return origUserPreferences;
    }

    /**
     * @return detectors, which markers will be shown in Eclipse
     */
    Map<DetectorFactory, Boolean> getVisibleDetectors() {
        return visibleDetectors;
    }

    DetectorConfigurationTab getDetectorTab() {
        return detectorTab;
    }

    /**
     * Enum to hold an effort level and internationalizable label value.
     */
    public enum Effort {

        MIN(UserPreferences.EFFORT_MIN, "property.effortmin"), DEFAULT(UserPreferences.EFFORT_DEFAULT, "property.effortdefault"), MAX(
                UserPreferences.EFFORT_MAX, "property.effortmax");

        private final String effortLevel;

        private final String message;

        private Effort(String level, String messageKey) {
            effortLevel = level;
            message = getMessage(messageKey);
        }

        @Override
        public String toString() {
            return message;
        }

        public String getEffortLevel() {
            return effortLevel;
        }

        static Effort getEffort(String level) {
            Effort[] efforts = values();
            for (Effort effort : efforts) {
                if (effort.getEffortLevel().equals(level)) {
                    return effort;
                }
            }
            return DEFAULT;
        }
    }

    @Override
    public void setErrorMessage(String newMessage) {
        setValid(newMessage == null);
        super.setErrorMessage(newMessage);
    }

    public void init(IWorkbench workbench) {
        // noop
    }

    protected Button getChkEnableFindBugs() {
        return chkEnableFindBugs;
    }

    protected Button getEnableProjectCheck() {
        return enableProjectCheck;
    }

    protected ComboViewer getEffortViewer() {
        return effortViewer;
    }

    protected DetectorConfigurationTab createDetectorConfigurationTab(TabFolder parentTabFolder) {
        return new DetectorConfigurationTab(parentTabFolder, this, SWT.NONE);
    }

    protected ReportConfigurationTab createReportConfigurationTab(TabFolder parentTabFolder) {
        return new ReportConfigurationTab(parentTabFolder, this, SWT.NONE);
    }

    protected FilterFilesTab createFilterFilesTab(TabFolder parentTabFolder) {
        return new FilterFilesTab(parentTabFolder, this, SWT.NONE);
    }
}
