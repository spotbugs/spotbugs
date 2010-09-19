/*
 * Contributions to FindBugs
 * Copyright (C) 2008, Andrei Loskutov
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
package de.tobject.findbugs.wizards;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.ProjectUtilities;
import de.tobject.findbugs.view.explorer.FilterBugsDialog;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugPattern;

/**
 * @author Andrei
 */
public class ExportWizardPage extends WizardPage {

    private static final String SEPARATOR = ",";

    private static final int BY_NAME = 0;

    private static final int BY_NOT_FILTERED_COUNT = 1;

    private static final int BY_OVERALL_COUNT = 2;

    private Composite comp;

    private int sortBy;

    private Text filteredBugIdsText;

    protected ExportWizardPage(String pageName, String title, String descr, String imagePath) {
        super(pageName, title, AbstractUIPlugin.imageDescriptorFromPlugin(FindbugsPlugin.getDefault().getBundle()
                .getSymbolicName(), imagePath));
        setDescription(descr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
     * .Composite)
     */
    public void createControl(Composite parent) {
        comp = new Composite(parent, SWT.BORDER);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.heightHint = 400;
        gd.widthHint = 400;
        comp.setLayoutData(gd);
        setControl(comp);

        Label label = new Label(comp, SWT.NONE);
        label.setText("Sort by:");

        final Combo sortByCombo = new Combo(comp, SWT.READ_ONLY);
        final String[] items = new String[] { "Name", "Not filtered bug count", "Overall bug count" };
        sortByCombo.setItems(items);

        String sortOrder = FindbugsPlugin.getDefault().getPreferenceStore().getString(FindBugsConstants.EXPORT_SORT_ORDER);
        if (FindBugsConstants.ORDER_BY_NOT_FILTERED_BUGS_COUNT.equals(sortOrder)) {
            sortByCombo.select(1);
        } else if (FindBugsConstants.ORDER_BY_OVERALL_BUGS_COUNT.equals(sortOrder)) {
            sortByCombo.select(2);
        } else {
            sortByCombo.select(0);
        }
        sortBy = sortByCombo.getSelectionIndex();

        sortByCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                sortBy = sortByCombo.getSelectionIndex();
            }
        });

        label = new Label(comp, SWT.NONE);
        label.setText("Filter bug ids:");

        Button button = new Button(comp, SWT.PUSH);
        button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
        button.setText("Browse...");
        button.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e) {
                Set<BugPattern> filtered = FindbugsPlugin.getFilteredPatterns();
                Set<BugCode> filteredTypes = FindbugsPlugin.getFilteredPatternTypes();
                FilterBugsDialog dialog = new FilterBugsDialog(getShell(), filtered, filteredTypes);
                dialog.setTitle("Bug Filter Configuration");
                int result = dialog.open();
                if (result != Window.OK) {
                    return;
                }
                String selectedIds = dialog.getSelectedIds();

                FindbugsPlugin.getDefault().getPreferenceStore().setValue(FindBugsConstants.LAST_USED_EXPORT_FILTER, selectedIds);
                filteredBugIdsText.setText(selectedIds);
            }
        });

        label = new Label(comp, SWT.NONE);
        label.setText("");
        filteredBugIdsText = new Text(comp, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
        GridData layoutData = new GridData(GridData.FILL_BOTH);
        layoutData.heightHint = 200;
        layoutData.widthHint = 400;
        filteredBugIdsText.setLayoutData(layoutData);
        filteredBugIdsText.setText(FindbugsPlugin.getDefault().getPreferenceStore()
                .getString(FindBugsConstants.LAST_USED_EXPORT_FILTER));
        filteredBugIdsText.setToolTipText("Bug ids to filter, separated by comma or space");
    }

    @Override
    public void dispose() {
        comp.dispose();
        super.dispose();
    }

    public boolean finish() {
        String data = collectBugsData();
        copyToClipboard(data);
        String filters = FindBugsConstants.encodeIds(getLastUsedExportFilters());
        FindbugsPlugin.getDefault().getPreferenceStore().setValue(FindBugsConstants.LAST_USED_EXPORT_FILTER, filters);
        String sortPref;
        switch (sortBy) {
        case BY_NOT_FILTERED_COUNT:
            sortPref = FindBugsConstants.ORDER_BY_NOT_FILTERED_BUGS_COUNT;
            break;
        case BY_OVERALL_COUNT:
            sortPref = FindBugsConstants.ORDER_BY_OVERALL_BUGS_COUNT;
            break;
        case BY_NAME:
        default:
            sortPref = FindBugsConstants.ORDER_BY_NAME;
            break;
        }
        FindbugsPlugin.getDefault().getPreferenceStore().setValue(FindBugsConstants.EXPORT_SORT_ORDER, sortPref);
        return true;
    }

    private Set<String> getLastUsedExportFilters() {
        String text = filteredBugIdsText.getText();
        return FindBugsConstants.decodeIds(text);
    }

    /**
     * @return
     */
    private String collectBugsData() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        List<Record> lines = new ArrayList<Record>();
        for (IProject project : projects) {
            Record line = createProjectLine(project);
            if (line != null) {
                lines.add(line);
            }
        }
        Collections.sort(lines);
        StringBuilder sb = new StringBuilder();

        createHeader(sb);

        for (Record record : lines) {
            sb.append(record);
        }
        return sb.toString();
    }

    private Record createProjectLine(IProject project) {
        if (ProjectUtilities.isJavaProject(project) /*
                                                     * TODO why not working ??
                                                     * && project.hasNature(
                                                     * FindbugsPlugin.NATURE_ID)
                                                     */) {
            IMarker[] markerArr = MarkerUtil.getAllMarkers(project);
            if (markerArr.length == 0) {
                return null;
            }
            int overallBugCount = markerArr.length;
            int notFilteredBugCount = 0;
            Set<String> usedExportFilters = getLastUsedExportFilters();
            for (IMarker marker : markerArr) {
                if (!MarkerUtil.isFiltered(marker, usedExportFilters)) {
                    notFilteredBugCount++;
                }
            }
            return new Record(project.getName(), overallBugCount, notFilteredBugCount);
        }
        return null;
    }

    protected int getSortBy() {
        return sortBy;
    }

    protected void copyToClipboard(String toolTip) {
        Object[] data = new Object[] { toolTip };
        Transfer[] transfer = new Transfer[] { TextTransfer.getInstance() };
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        clipboard.setContents(data, transfer);
        clipboard.dispose();
    }

    private void createHeader(StringBuilder sb) {
        switch (sortBy) {
        case BY_OVERALL_COUNT:
            sb.append("Overall bugs number" + SEPARATOR + " Project name" + SEPARATOR + "Not filtered bugs number\n");
            break;
        case BY_NOT_FILTERED_COUNT:
            sb.append("Not filtered bugs number" + SEPARATOR + "Project name" + SEPARATOR + "Overall bugs number\n");
            break;
        case BY_NAME:
        default:
            sb.append("Project name" + SEPARATOR + "Not filtered bugs number" + SEPARATOR + "Overall bugs number\n");
            break;
        }
    }

    public class Record implements Comparable<Record> {

        private final String name;

        private final int overallBugs;

        private final int notFilteredBugs;

        @Override
        public int hashCode() {
            int result = ((name == null) ? 0 : name.hashCode());
            result += notFilteredBugs;
            result += overallBugs;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Record)) {
                return false;
            }
            Record other = (Record) obj;
            if (notFilteredBugs != other.notFilteredBugs) {
                return false;
            }
            if (overallBugs != other.overallBugs) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }

        Record(String name, int overallBugs, int notFilteredBugs) {
            this.name = name;
            this.overallBugs = overallBugs;
            this.notFilteredBugs = notFilteredBugs;
        }

        public int compareTo(Record other) {
            int result;
            switch (sortBy) {
            case BY_OVERALL_COUNT:
                result = other.overallBugs - overallBugs;
                if (result == 0) {
                    return name.compareTo(other.name);
                }
                return result;
            case BY_NOT_FILTERED_COUNT:
                result = other.notFilteredBugs - notFilteredBugs;
                if (result == 0) {
                    return name.compareTo(other.name);
                }
                return result;
            case BY_NAME:
            default:
                // name can't be the same, so additional sorting is not needed
                return name.compareTo(other.name);
            }
        }

        @Override
        public String toString() {
            switch (sortBy) {
            case BY_OVERALL_COUNT:
                return overallBugs + SEPARATOR + name + SEPARATOR + notFilteredBugs + "\n";
            case BY_NOT_FILTERED_COUNT:
                return notFilteredBugs + SEPARATOR + name + SEPARATOR + overallBugs + "\n";
            case BY_NAME:
            default:
                return name + SEPARATOR + notFilteredBugs + SEPARATOR + overallBugs + "\n";
            }
        }

    }

}
