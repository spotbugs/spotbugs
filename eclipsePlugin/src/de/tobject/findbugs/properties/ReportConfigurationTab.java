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

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerSeverity;
import edu.umd.cs.findbugs.BugRankCategory;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
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



    private Combo scariestRankCombo;

    private MarkerSeverity initialScariestRank;


    private Combo scaryRankCombo;

    private MarkerSeverity initialScaryRank;


    private Combo troublingRankCombo;

    private MarkerSeverity initialTroublingRank;

    private Combo ofConcernRankCombo;

    private MarkerSeverity initialOfConcernRank;


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

        Group rankGroup = new Group(parent, SWT.NONE);
        rankGroup.setLayout(new GridLayout(2, false));
        rankGroup.setText("Mark bugs with ... rank as:");
        rankGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));

        Label bugSeverityLabel = new Label(rankGroup, SWT.NONE);
        bugSeverityLabel.setText("Scariest:");

        scariestRankCombo = new Combo(rankGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MarkerSeverity markerSeverity : markerSeverities) {
            scariestRankCombo.add(markerSeverity.name());
        }
        initialScariestRank = MarkerSeverity.get(store.getString(FindBugsConstants.RANK_SCARIEST_MARKER_SEVERITY));
        scariestRankCombo.setText(initialScariestRank.name());

        bugSeverityLabel = new Label(rankGroup, SWT.NONE);
        bugSeverityLabel.setText("Scary:");

        scaryRankCombo = new Combo(rankGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MarkerSeverity markerSeverity : markerSeverities) {
            scaryRankCombo.add(markerSeverity.name());
        }
        initialScaryRank = MarkerSeverity.get(store.getString(FindBugsConstants.RANK_SCARY_MARKER_SEVERITY));
        scaryRankCombo.setText(initialScaryRank.name());

        bugSeverityLabel = new Label(rankGroup, SWT.NONE);
        bugSeverityLabel.setText("Troubling:");

        troublingRankCombo = new Combo(rankGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MarkerSeverity markerSeverity : markerSeverities) {
            troublingRankCombo.add(markerSeverity.name());
        }
        initialTroublingRank = MarkerSeverity.get(store.getString(FindBugsConstants.RANK_TROUBLING_MARKER_SEVERITY));
        troublingRankCombo.setText(initialTroublingRank.name());

        bugSeverityLabel = new Label(rankGroup, SWT.NONE);
        bugSeverityLabel.setText("Of concern:");

        ofConcernRankCombo = new Combo(rankGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (MarkerSeverity markerSeverity : markerSeverities) {
            ofConcernRankCombo.add(markerSeverity.name());
        }
        initialOfConcernRank = MarkerSeverity.get(store.getString(FindBugsConstants.RANK_OFCONCERN_MARKER_SEVERITY));
        ofConcernRankCombo.setText(initialOfConcernRank.name());

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


    }


    private void createRankGroup(Composite parent) {
        Composite prioGroup = new Composite(parent, SWT.NONE);
        prioGroup.setLayout(new GridLayout(2, false));

        Label minRankLabel = new Label(prioGroup, SWT.NONE);
        minRankLabel.setText(getMessage("property.minRank") + System.getProperty("line.separator")
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
        minRankSlider.setMinimum(BugRanker.VISIBLE_RANK_MIN);
        minRankSlider.setMaximum(BugRanker.VISIBLE_RANK_MAX);
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
        int rank = minRankSlider.getSelection();
        BugRankCategory category = BugRankCategory.getRank(rank);
        String label = category.toString();
        rankValueLabel.setText(rank + " (" + label + ")");
    }

    /**
     * Helper method to shorten message access
     *
     * @param key
     *            a message key
     * @return requested message
     */
    protected String getMessage(String key) {
        return FindbugsPlugin.getDefault().getMessage(key);
    }

    /**
     * Build list of bug categories to be enabled or disabled. Populates
     * chkEnableBugCategoryList and bugCategoryList fields.
     *
     * @param parent
     *            control checkboxes should be added to
     * @param project
     *            the project being configured
     */
    private void createBugCategoriesGroup(Composite parent, final IProject project) {
        Group checkBoxGroup = new Group(parent, SWT.SHADOW_ETCHED_OUT);
        checkBoxGroup.setText(getMessage("property.categoriesGroup"));
        checkBoxGroup.setLayout(new GridLayout(1, true));
        checkBoxGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, true, true));

        List<String> bugCategoryList = new LinkedList<String>(DetectorFactoryCollection.instance().getBugCategories());
        chkEnableBugCategoryList = new LinkedList<Button>();
        ProjectFilterSettings origFilterSettings = propertyPage.getOriginalUserPreferences().getFilterSettings();
        for (String category : bugCategoryList) {
            Button checkBox = new Button(checkBoxGroup, SWT.CHECK);
            checkBox.setText(I18N.instance().getBugCategoryDescription(category));
            checkBox.setSelection(origFilterSettings.containsCategory(category));
            GridData layoutData = new GridData();
            layoutData.horizontalIndent = 10;
            checkBox.setLayoutData(layoutData);

            // Every time a checkbox is clicked, rebuild the detector factory
            // table
            // to show only relevant entries
            checkBox.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event e) {
                    syncSelectedCategories();
                }
            });
            checkBox.setData(category);
            chkEnableBugCategoryList.add(checkBox);
        }
    }

    /**
     * Synchronize selected bug category checkboxes with the current user
     * preferences.
     */
    protected void syncSelectedCategories() {
        ProjectFilterSettings filterSettings = getCurrentProps().getFilterSettings();
        for (Button checkBox : chkEnableBugCategoryList) {
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
        ofConcernRankCombo.setEnabled(enabled);
        troublingRankCombo.setEnabled(enabled);
        scaryRankCombo.setEnabled(enabled);
        scariestRankCombo.setEnabled(enabled);
        minRankSlider.setEnabled(enabled);
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
        if (isMarkerSeveritiesChanged(FindBugsConstants.RANK_SCARIEST_MARKER_SEVERITY, initialScariestRank)) {
            return true;
        }
        if (isMarkerSeveritiesChanged(FindBugsConstants.RANK_SCARY_MARKER_SEVERITY, initialScaryRank)) {
            return true;
        }
        if (isMarkerSeveritiesChanged(FindBugsConstants.RANK_TROUBLING_MARKER_SEVERITY, initialTroublingRank)) {
            return true;
        }
        if (isMarkerSeveritiesChanged(FindBugsConstants.RANK_OFCONCERN_MARKER_SEVERITY, initialOfConcernRank)) {
            return true;
        }
        return false;

    }

    private boolean isMarkerSeveritiesChanged(String propertyName, MarkerSeverity marker) {
        IPreferenceStore store = propertyPage.getPreferenceStore();
        return !store.getString(propertyName).equals(marker.name());

    }

    void refreshUI(UserPreferences prefs) {
        IPreferenceStore store = propertyPage.getPreferenceStore();
        scariestRankCombo.setText(MarkerSeverity.get(store.getString(FindBugsConstants.RANK_SCARIEST_MARKER_SEVERITY)).name());
        scaryRankCombo.setText(MarkerSeverity.get(store.getString(FindBugsConstants.RANK_SCARY_MARKER_SEVERITY)).name());
        troublingRankCombo.setText(MarkerSeverity.get(store.getString(FindBugsConstants.RANK_TROUBLING_MARKER_SEVERITY)).name());
        ofConcernRankCombo.setText(MarkerSeverity.get(store.getString(FindBugsConstants.RANK_OFCONCERN_MARKER_SEVERITY)).name());

        ProjectFilterSettings filterSettings = prefs.getFilterSettings();
        minRankSlider.setSelection(filterSettings.getMinRank());
        updateRankValueLabel();
        minPriorityCombo.setText(filterSettings.getMinPriority());
        for (Button checkBox : chkEnableBugCategoryList) {
            checkBox.setSelection(filterSettings.containsCategory((String) checkBox.getData()));
        }
        syncSelectedCategories();
    }

    protected List<Button> getChkEnableBugCategoryList() {
        return chkEnableBugCategoryList;
    }

    public void performOk() {
        IPreferenceStore store = propertyPage.getPreferenceStore();
        String scariest = scariestRankCombo.getText();
        store.setValue(FindBugsConstants.RANK_SCARIEST_MARKER_SEVERITY, scariest);

        String scary = scaryRankCombo.getText();
        store.setValue(FindBugsConstants.RANK_SCARY_MARKER_SEVERITY, scary);

        String troubling = troublingRankCombo.getText();
        store.setValue(FindBugsConstants.RANK_TROUBLING_MARKER_SEVERITY, troubling);

        String ofConcern = ofConcernRankCombo.getText();
        store.setValue(FindBugsConstants.RANK_OFCONCERN_MARKER_SEVERITY, ofConcern);

    }

}
