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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.navigator.CommonNavigator;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerSeverity;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.cloud.CloudFactory;
import edu.umd.cs.findbugs.cloud.CloudPlugin;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class ReportConfigurationTab extends Composite {

	private final FindbugsPropertyPage propertyPage;
	private List<Button> chkEnableBugCategoryList;
	private Scale minRankSlider;
	private Label rankValueLabel;
	private Combo minPriorityCombo;
	private Combo cloudCombo;
	private Label cloudLabel;
	private List<CloudPlugin> clouds;
	private Combo normalPrioCombo;
	private MarkerSeverity initialNormalPrio;
	private Combo highPrioCombo;
	private MarkerSeverity initialHighPrio;
	private Combo lowPrioCombo;
	private MarkerSeverity initialLowPrio;

	public ReportConfigurationTab(TabFolder parent, FindbugsPropertyPage page, int style) {
		super(parent, style);
		this.propertyPage = page;
		setLayout(new GridLayout());

		TabItem tabDetector = new TabItem(parent, SWT.NONE);
		tabDetector.setText(getMessage("property.reportConfigurationTab"));
		tabDetector.setControl(this);
		tabDetector.setToolTipText("Configure bugs reported to the UI");

		Composite rankAndPrioGroup = new Composite(this, SWT.NONE);
		rankAndPrioGroup.setLayout(new GridLayout(2, false));

		createRankGroup(rankAndPrioGroup);
		createPriorityGroup(rankAndPrioGroup);

		createBugCategoriesGroup(rankAndPrioGroup, page.getProject());
		createBugSeverityGroup(rankAndPrioGroup);
	}

	private void createBugSeverityGroup(Composite parent) {
		IPreferenceStore store = propertyPage.getPreferenceStore();
		MarkerSeverity[] markerSeverities = MarkerSeverity.values();

		Group prioGroup = new Group(parent, SWT.NONE);
		prioGroup.setLayout(new GridLayout(2, false));
		prioGroup.setText("Mark bugs with ... priority as:");
		prioGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));


		Label bugSeverityLabel = new Label(prioGroup, SWT.NONE);
		bugSeverityLabel.setText("High Priority:");

		highPrioCombo = new Combo(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (MarkerSeverity markerSeverity : markerSeverities) {
			highPrioCombo.add(markerSeverity.name());
		}
		initialHighPrio = MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_HIGH_MARKER_SEVERITY));
		highPrioCombo.setText(initialHighPrio.name());

		bugSeverityLabel = new Label(prioGroup, SWT.NONE);
		bugSeverityLabel.setText("Medium Priority:");

		normalPrioCombo = new Combo(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (MarkerSeverity markerSeverity : markerSeverities) {
			normalPrioCombo.add(markerSeverity.name());
		}
		initialNormalPrio = MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_NORMAL_MARKER_SEVERITY));
		normalPrioCombo.setText(initialNormalPrio.name());

		bugSeverityLabel = new Label(prioGroup, SWT.NONE);
		bugSeverityLabel.setText("Low Priority:");

		lowPrioCombo = new Combo(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		for (MarkerSeverity markerSeverity : markerSeverities) {
			lowPrioCombo.add(markerSeverity.name());
		}
		initialLowPrio = MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_LOW_MARKER_SEVERITY));
		lowPrioCombo.setText(initialLowPrio.name());

	}

	private void createPriorityGroup(Composite parent) {
		Composite prioGroup = new Composite(parent, SWT.NONE);
		prioGroup.setLayout(new GridLayout(2, false));

		Label minPrioLabel = new Label(prioGroup, SWT.NONE);
		minPrioLabel.setText(getMessage("property.minPriority"));
		minPrioLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		minPriorityCombo = new Combo(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		minPriorityCombo.add(ProjectFilterSettings.HIGH_PRIORITY);
		minPriorityCombo.add(ProjectFilterSettings.MEDIUM_PRIORITY);
		minPriorityCombo.add(ProjectFilterSettings.LOW_PRIORITY);
		minPriorityCombo.setText(propertyPage.getOriginalUserPreferences().getFilterSettings().getMinPriority());
		minPriorityCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		minPriorityCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String data = minPriorityCombo.getText();
				getCurrentProps().getFilterSettings().setMinPriority(data);
			}
		});

		cloudLabel = new Label(prioGroup, SWT.NONE);
		cloudCombo = new Combo(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		cloudCombo.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		enableOrDisableCloudControls();
		String cloudid = getCloudIdFromCollection();

		clouds = new ArrayList<CloudPlugin>();
		populateCloudsCombo(cloudid);
	}

	private int populateCloudsCombo(String cloudid) {
		int i = 0;
		boolean cloudSelected = false;
		int defaultIndex = -1;
		for (CloudPlugin cloud : CloudFactory.getRegisteredClouds().values()) {
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
		if(!cloudSelected && cloudid != null && cloudid.trim().length() > 0) {
			if (defaultIndex != -1) {
				cloudCombo.select(defaultIndex);
			} else {
				// should not happen: default local cloud should be available
				FindbugsPlugin.getDefault().logWarning("Failed to find default local cloud (edu.umd.cs.findbugs.cloud.Local)");
			}
		}
		return defaultIndex;
	}

	private String getCloudIdFromCollection() {
		final IProject eclipseProj = propertyPage.getProject();
		String cloudid =  CloudFactory.DEFAULT_CLOUD;
		if (eclipseProj != null) {
			SortedBugCollection collection = FindbugsPlugin.getBugCollectionIfSet(eclipseProj);
			if (collection != null) {
				cloudid = collection.getCloud().getPlugin().getId();
			}
		}
		return cloudid;
	}

	private IProject enableOrDisableCloudControls() {
		IProject eclipseProj = propertyPage.getProject();
		String txt = "Comment storage:";
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


	private void createRankGroup(Composite parent) {
		Composite prioGroup = new Composite(parent, SWT.NONE);
		prioGroup.setLayout(new GridLayout(2, false));

		Label minRankLabel = new Label(prioGroup, SWT.NONE);
		minRankLabel.setText(getMessage("property.minRank")
				+ System.getProperty("line.separator")
				+ getMessage("property.minRank.line2"));
		minRankLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));

		minRankSlider = new Scale(prioGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
		minRankSlider.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
		minRankSlider.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int rank = minRankSlider.getSelection();
				getCurrentProps().getFilterSettings().setMinRank(rank);
				updateRankValueLabel();
			}
		});
		minRankSlider.setMinimum(0);
		minRankSlider.setMaximum(20);
		minRankSlider.setSelection(getCurrentProps().getFilterSettings().getMinRank());
		minRankSlider.setIncrement(1);
		minRankSlider.setPageIncrement(5);
		Label dummyLabel = new Label(prioGroup, SWT.NONE);
		dummyLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

		rankValueLabel = new Label(prioGroup, SWT.NONE);
		rankValueLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
		updateRankValueLabel();
	}

	private void updateRankValueLabel() {
		String label;
		int rank = minRankSlider.getSelection();
		if (rank < 5) {
			label = "Scariest";
		} else if (rank < 10) {
			label = "Scary";
		} else if (rank < 15) {
			label = "Troubling";
		} else {
			label = "Possible";
		}
		rankValueLabel.setText(rank + " (" + label + ")");
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
	 * Build list of bug categories to be enabled or disabled.
	 * Populates chkEnableBugCategoryList and bugCategoryList fields.
	 *
	 * @param parent control checkboxes should be added to
	 * @param project       the project being configured
	 */
	private void createBugCategoriesGroup(Composite parent, final IProject project) {
		Group checkBoxGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
		checkBoxGroup.setText(getMessage("property.categoriesGroup"));
		checkBoxGroup.setLayout(new GridLayout(1, true));
		checkBoxGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));

		List<String> bugCategoryList = new LinkedList<String>(I18N.instance().getBugCategories());
		chkEnableBugCategoryList = new LinkedList<Button>();
		ProjectFilterSettings origFilterSettings = propertyPage
				.getOriginalUserPreferences().getFilterSettings();
		for (String category: bugCategoryList) {
			Button checkBox = new Button(checkBoxGroup, SWT.CHECK);
			checkBox.setText(I18N.instance().getBugCategoryDescription(category));
			checkBox.setSelection(origFilterSettings.containsCategory(category));
			GridData layoutData = new GridData();
			layoutData.horizontalIndent = 10;
			checkBox.setLayoutData(layoutData);

			// Every time a checkbox is clicked, rebuild the detector factory table
			// to show only relevant entries
			checkBox.addListener(SWT.Selection,
				new Listener(){
					public void handleEvent(Event e){
						syncSelectedCategories();
					}
				}
			);
			checkBox.setData(category);
			chkEnableBugCategoryList.add(checkBox);
		}
	}

	/**
	 * Synchronize selected bug category checkboxes with the current user preferences.
	 */
	protected void syncSelectedCategories() {
		ProjectFilterSettings filterSettings = getCurrentProps().getFilterSettings();
		for (Button checkBox: chkEnableBugCategoryList) {
			String category = (String) checkBox.getData();
			if (checkBox.getSelection()) {
				filterSettings.addCategory(category);
			} else {
				filterSettings.removeCategory(category);
			}
		}
		propertyPage.getVisibleDetectors().clear();
	}

	protected UserPreferences getCurrentProps() {
		return propertyPage.getCurrentUserPreferences();
	}

	@Override
	public void setEnabled(boolean enabled) {
		minPriorityCombo.setEnabled(enabled);
		lowPrioCombo.setEnabled(enabled);
		normalPrioCombo.setEnabled(enabled);
		highPrioCombo.setEnabled(enabled);
		minRankSlider.setEnabled(enabled);
		if (enabled) {
			enableOrDisableCloudControls();
		} else {
			cloudCombo.setEnabled(false);
		}
		for (Button checkBox : chkEnableBugCategoryList) {
			checkBox.setEnabled(enabled);
		}
		super.setEnabled(enabled);
	}

	public void setMinRank(int rank) {
		minRankSlider.setSelection(rank);
	}

	public int getMinRank() {
		return minRankSlider.getSelection();
	}

	public boolean isMarkerSeveritiesChanged() {
		IPreferenceStore store = propertyPage.getPreferenceStore();
		String highPrio = store.getString(FindBugsConstants.PRIO_HIGH_MARKER_SEVERITY);
		String normalPrio = store.getString(FindBugsConstants.PRIO_NORMAL_MARKER_SEVERITY);
		String lowPrio = store.getString(FindBugsConstants.PRIO_HIGH_MARKER_SEVERITY);
		return !initialHighPrio.name().equals(highPrio)
				|| !initialNormalPrio.name().equals(normalPrio)
				|| !initialLowPrio.name().equals(lowPrio);
	}

	void refreshUI(UserPreferences prefs) {
		IPreferenceStore store = propertyPage.getPreferenceStore();
		highPrioCombo.setText(MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_HIGH_MARKER_SEVERITY)).name());
		normalPrioCombo.setText(MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_NORMAL_MARKER_SEVERITY)).name());
		lowPrioCombo.setText(MarkerSeverity.get(
				store.getString(FindBugsConstants.PRIO_LOW_MARKER_SEVERITY)).name());

		ProjectFilterSettings filterSettings = prefs.getFilterSettings();
		minRankSlider.setSelection(filterSettings.getMinRank());
		updateRankValueLabel();
		minPriorityCombo.setText(filterSettings.getMinPriority());
		for (Button checkBox: chkEnableBugCategoryList) {
			checkBox.setSelection(filterSettings.containsCategory((String) checkBox.getData()));
		}
		syncSelectedCategories();
		String cloudid = getCloudIdFromCollection();
		cloudCombo.removeAll();
		clouds.clear();
		populateCloudsCombo(cloudid);
	}

	protected List<Button> getChkEnableBugCategoryList() {
		return chkEnableBugCategoryList;
	}

	public void performOk() {
		IPreferenceStore store = propertyPage.getPreferenceStore();
		String highPrio = highPrioCombo.getText();
		store.setValue(FindBugsConstants.PRIO_HIGH_MARKER_SEVERITY, highPrio);

		String normalPrio = normalPrioCombo.getText();
		store.setValue(FindBugsConstants.PRIO_NORMAL_MARKER_SEVERITY, normalPrio);

		String lowPrio = lowPrioCombo.getText();
		store.setValue(FindBugsConstants.PRIO_LOW_MARKER_SEVERITY, lowPrio);

		IProject eclipseProj = propertyPage.getProject();
		if (eclipseProj == null) {
			return;
		}
		SortedBugCollection collection = FindbugsPlugin.getBugCollectionIfSet(eclipseProj);
		if(collection == null){
			return;
		}
		Project project = collection.getProject();
		CloudPlugin item = clouds.get(cloudCombo.getSelectionIndex());
		if (item != null && project != null && !item.getId().equals(project.getCloudId())) {
			project.setCloudId(item.getId());
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


