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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.ProjectUtilities;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Project properties page for setting FindBugs properties.
 *
 * @author Andrei Loskutov
 * @author Peter Friese
 * @author David Hovemeyer
 * @author Phil Crosby
 * @version 1.0
 * @since 17.06.2004
 */
public class FindbugsPropertyPage extends PropertyPage {

	private boolean initialEnabled;
	private UserPreferences origUserPreferences;
	private UserPreferences currentUserPreferences;
	private IProject project;

	private Button chkEnableFindBugs;
	private Button restoreDefaultsButton;
	private ComboViewer effortViewer;
	private EffortPlaceHolder defaultEffortLevel;
	private TabFolder tabFolder;
	private DetectorConfigurationTab detectorTab;
	private FilterFilesTab filterFilesTab;
	private ReportConfigurationTab reportConfigurationTab;
	private Map<DetectorFactory, Boolean> visibleDetectors;

	/**
	 * Constructor for FindbugsPropertyPage.
	 */
	public FindbugsPropertyPage() {
		super();
		visibleDetectors = new HashMap<DetectorFactory, Boolean>();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent) {

		noDefaultAndApplyButton();

		// getElement returns the element this page has been opened for,
		// in our case this is a Java Project (IJavaProject).
		IAdaptable resource = getElement();
		this.project = (IProject) resource.getAdapter(IProject.class);

		collectUserPreferences();

		createGlobalElements(parent);

		createConfigurationTabFolder(parent);

		createDefaultsButton(parent);

		return parent;
	}

	/**
	 * @param composite
	 */
	private void createConfigurationTabFolder(Composite composite) {
		tabFolder = new TabFolder(composite, SWT.TOP);
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL | GridData.FILL_VERTICAL | GridData.GRAB_VERTICAL);
		layoutData.verticalIndent = -5;
		tabFolder.setLayoutData(layoutData);

		detectorTab = new DetectorConfigurationTab(tabFolder, this, SWT.NONE);
		reportConfigurationTab = new ReportConfigurationTab(tabFolder, this, SWT.NONE);
		filterFilesTab = new FilterFilesTab(tabFolder, this, SWT.NONE);
	}

	/**
	 * @param composite
	 */
	private void createDefaultsButton(Composite composite) {
		restoreDefaultsButton = new Button(composite, SWT.NONE);
		restoreDefaultsButton.setText(getMessage("property.restoreSettings"));
		restoreDefaultsButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		restoreDefaultsButton.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				restoreDefaultSettings();
			}
		});
	}


	/**
	 * @param parent
	 */
	private void createGlobalElements(Composite parent) {
		Composite globalGroup = new Composite(parent, SWT.TOP);
		GridLayout layout = new GridLayout(2,false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		globalGroup.setLayout(layout);
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		layoutData.verticalIndent = -2;

		globalGroup.setLayoutData(layoutData);


		initialEnabled = isEnabled();

		chkEnableFindBugs = new Button(globalGroup, SWT.CHECK);
		chkEnableFindBugs.setText(getMessage("property.runAuto"));
		chkEnableFindBugs.setSelection(initialEnabled);
		chkEnableFindBugs.setToolTipText("Enable / disable FindBugs project builder (disabled by default)");

		Composite prioGroup = new Composite(globalGroup, SWT.NONE);
		GridLayout prioLayout = new GridLayout(2, false);
		prioGroup.setLayout(prioLayout);
		layoutData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		layoutData.horizontalIndent = -5;
		layoutData.verticalIndent = -5;
		prioGroup.setLayoutData(layoutData);

		// effort
		Label effortLabel = new Label(prioGroup, SWT.NULL);
		effortLabel.setText(getMessage("property.effort"));
		effortViewer = new ComboViewer(prioGroup, SWT.DROP_DOWN
				| SWT.READ_ONLY);
		effortViewer.setLabelProvider(new WorkbenchLabelProvider());
		effortViewer.setContentProvider(new BaseWorkbenchContentProvider());
		defaultEffortLevel = new EffortPlaceHolder(getMessage("property.effortdefault"),
				UserPreferences.EFFORT_DEFAULT);
		EffortPlaceHolder[] effortLevels = new EffortPlaceHolder[] {
				new EffortPlaceHolder(getMessage("property.effortmin"),
						UserPreferences.EFFORT_MIN),
				defaultEffortLevel,
				new EffortPlaceHolder(getMessage("property.effortmax"),
						UserPreferences.EFFORT_MAX) };
		effortViewer.add(effortLevels);

		String effort = currentUserPreferences.getEffort();
		for (int i = 0; i < effortLevels.length; i++) {
			if (effortLevels[i].getEffortLevel().equals(effort)) {
				effortViewer.setSelection(new StructuredSelection(effortLevels[i]), true);
			}
		}
		effortViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				EffortPlaceHolder placeHolder = (EffortPlaceHolder) ((IStructuredSelection) event
						.getSelection()).getFirstElement();
				currentUserPreferences.setEffort(placeHolder.getEffortLevel());
			}
		});
		effortLabel.setToolTipText("Set FindBugs analysis effort (minimal is faster but less precise)");
		effortViewer.getCombo().setToolTipText("Set FindBugs analysis effort (minimal is faster but less precise)");
	}

	private void collectUserPreferences() {
		// Get current user preferences for project
		try {
			this.origUserPreferences = FindbugsPlugin.getUserPreferences(project);
		} catch (CoreException e) {
			// Use default settings
			FindbugsPlugin.getDefault().logException(e, "Could not get user preferences for project");
			this.origUserPreferences = UserPreferences.createDefaultUserPreferences();
		}
		this.currentUserPreferences = (UserPreferences) origUserPreferences.clone();
	}

	/**
	 * Restore default settings.
	 * This just changes the dialog widgets - the user still needs
	 * to confirm by clicking the "OK" button.
	 */
	private void restoreDefaultSettings() {
		visibleDetectors.clear();

		// By default, don't run FindBugs automatically
		chkEnableFindBugs.setSelection(false);

		effortViewer.setSelection(new StructuredSelection(defaultEffortLevel), true);

		getDetectorTab().restoreDefaultSettings();
		reportConfigurationTab.restoreDefaultSettings();
		filterFilesTab.restoreDefaultSettings();
	}

	/**
	 * Will be called when the user presses the OK button.
	 * @see IPreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean builderEnabled = chkEnableFindBugs.getSelection();

		// Update whether or not FindBugs is run automatically.
		if (!initialEnabled && builderEnabled) {
			addNature();
		} else if (initialEnabled && !builderEnabled) {
			removeNature();
		}

		// Have user preferences for project changed?
		// If so, write them to the user preferences file & re-run builder
		if (!currentUserPreferences.equals(origUserPreferences)) {
			try {
				FindbugsPlugin.saveUserPreferences(project, currentUserPreferences);
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not store FindBugs preferences for project");
			} catch (IOException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not store FindBugs preferences for project");
			}

			// If already enabled (and still enabled) trigger a Findbugs rebuild here
			if (builderEnabled) {
				runFindbugsBuilder();
			}
		}

		// if filter settings changed, and builder is not enabled, manually trigger update
		if (!builderEnabled
				&& !currentUserPreferences.getFilterSettings().equals(
						origUserPreferences.getFilterSettings())) {
			MarkerUtil.redisplayMarkers(project, getShell());
		}
		return true;
	}

	/**
	 * Using the natures name, check whether the current
	 * project has the given nature.
	 *
	 * @return boolean <code>true</code>, if the nature is
	 *   assigned to the project, <code>false</code> otherwise.
	 */
	private boolean isEnabled() {
		try {
			return project.hasNature(FindbugsPlugin.NATURE_ID);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
			"Error while testing FindBugs nature for project " + project);
		}
		return false;
	}

	protected IProject getProject() {
		return project;
	}

	private void runFindbugsBuilder() {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {
					try {
						getProject().build(IncrementalProjectBuilder.CLEAN_BUILD,
								FindbugsPlugin.BUILDER_ID, null, monitor);
					} catch (OperationCanceledException e) {
						// Do nothing when operation cancelled.
					} catch (CoreException e) {
						FindbugsPlugin.getDefault().logException(e,
								"Error while runnning FindBugs builder for project");
					}
				}

			});
		} catch (InvocationTargetException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Error while runnning FindBugs builder for project");
		} catch (InterruptedException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Findbugs builder was interrupted");
		}
	}

	/**
	 * Add the nature to the current project. The real work is
	 * done by the inner class NatureWorker
	 * @return boolean <code>true</code> if the nature could
	 *   be added successfully, <code>false</code> otherwise.
	 */
	private boolean addNature() {
		try {
			NatureWorker worker = new NatureWorker(true);
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(getShell());
			monitor.run(true, true, worker);
			return true;
		} catch (InvocationTargetException e) {
			FindbugsPlugin.getDefault().logException(e, "'Add nature' failed");
		} catch (InterruptedException e) {
			FindbugsPlugin.getDefault().logException(e, "'Add nature' interrupted");
		}
		return false;
	}

	/**
	 * Remove the nature from the project.
	 * @return boolean <code>true</code> if the nature could
	 *   be added successfully, <code>false</code> otherwise.
	 */
	private boolean removeNature() {
		try {
			NatureWorker worker = new NatureWorker(false);
			ProgressMonitorDialog monitor = new ProgressMonitorDialog(getShell());
			monitor.run(true, true, worker);
			return true;
		} catch (InvocationTargetException e) {
			FindbugsPlugin.getDefault().logException(e, "'Remove nature' failed");
		} catch (InterruptedException e) {
			FindbugsPlugin.getDefault().logException(e, "'Remove nature' interrupted");
		}
		return false;
	}

	private final class NatureWorker implements IRunnableWithProgress {
		private boolean add = true;

		public NatureWorker(boolean add) {
			this.add = add;
		}

		/**
		 * @see IRunnableWithProgress#run(IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) {
			try {
				if (add) {
					ProjectUtilities.addFindBugsNature(project, monitor);
				} else {
					ProjectUtilities.removeFindBugsNature(project, monitor);
				}
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Core exception in NatureWorker");
			}
		}
	}

	/**
	 * Helper method to shorten message access
	 * @param key a message key
	 * @return requested message
	 */
	protected String getMessage(String key) {
		return FindbugsPlugin.getDefault().getMessage(key);
	}

	/**
	 * @return the currentUserPreferences
	 */
	UserPreferences getCurrentUserPreferences() {
		return currentUserPreferences;
	}
	/**
	 * @return the origUserPreferences
	 */
	UserPreferences getOriginalUserPreferences() {
		return origUserPreferences;
	}

	/**
	 * @return detectors, which markers will be shown in Eclipse
	 */
	Map<DetectorFactory, Boolean> getVisibleDetectors() {
		return visibleDetectors;
	}

	/**
	 * @return the detectorTab
	 */
	DetectorConfigurationTab getDetectorTab() {
		return detectorTab;
	}

	/**
	 * Helper class to hold an effort level and internationalizable label value.
	 *
	 * @author Peter Hendriks
	 */
	private static final class EffortPlaceHolder extends WorkbenchAdapter
			implements IAdaptable {

		private final String name;
		private final String effortLevel;

		public EffortPlaceHolder(String name, String effortLevel) {
			this.name = name;
			this.effortLevel = effortLevel;
		}

		@Override
		public String getLabel(Object object) {
			return name;
		}

		public String getEffortLevel() {
			return effortLevel;
		}

		public Object getAdapter(Class adapter) {
			if (adapter.equals(IWorkbenchAdapter.class)) {
				return this;
			}
			return null;
		}
	}
}
