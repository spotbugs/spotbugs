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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;

/**
 * @author Andrei
 */
public class ExportWizardPage extends WizardPage {

	private static final int BY_NAME = 0;
	private static final int BY_NOT_FILTERED_COUNT = 1;
	private static final int BY_OVERALL_COUNT = 2;
	private static final String SEPARATOR = ",";
	private Composite comp;
	private int sortBy;
	private Text filteredBugIdsText;

	/**
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	protected ExportWizardPage(String pageName, String title, String descr,
			String imagePath) {
		super(pageName, title, AbstractUIPlugin.imageDescriptorFromPlugin(FindbugsPlugin
				.getDefault().getBundle().getSymbolicName(), imagePath));
		setDescription(descr);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		comp = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		comp.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		comp.setLayoutData(gd);
		setControl(comp);

		Label label = new Label(comp, SWT.NONE);
		label.setText("Sort by:");

		final Combo sortByCombo = new Combo(comp, SWT.READ_ONLY);
		final String[] items = new String[] { "Name", "Not filtered bug count", "Overall bug count" };
		sortByCombo.setItems(items);

		String sortOrder = FindbugsPlugin.getDefault().getPreferenceStore().getString(
				FindBugsConstants.EXPORT_SORT_ORDER);
		if(FindBugsConstants.ORDER_BY_NOT_FILTERED_BUGS_COUNT.equals(sortOrder)) {
			sortByCombo.select(1);
		} else if(FindBugsConstants.ORDER_BY_OVERALL_BUGS_COUNT.equals(sortOrder)) {
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
		filteredBugIdsText = new Text(comp, SWT.SHADOW_IN | SWT.BORDER);
		GridData layoutData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		filteredBugIdsText.setLayoutData(layoutData);
		filteredBugIdsText.setText(FindbugsPlugin.getDefault().getPreferenceStore().getString(
				FindBugsConstants.LAST_USED_EXPORT_FILTER));
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
		String filters = getLastUsedExportFilters();
		FindbugsPlugin.getDefault().getPreferenceStore().setValue(
				FindBugsConstants.LAST_USED_EXPORT_FILTER, filters);
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
		FindbugsPlugin.getDefault().getPreferenceStore().setValue(
				FindBugsConstants.EXPORT_SORT_ORDER, sortPref);
		return true;
	}

	/**
	 * @return
	 */
	private String getLastUsedExportFilters() {
		String text = filteredBugIdsText.getText();
		if(text == null || text.trim().length() == 0) {
			return "";
		}
		String[] split = text.split("[^a-zA-Z]+");
		Arrays.sort(split);
		StringBuilder sb = new StringBuilder();
		for (String string : split) {
			sb.append(string).append(SEPARATOR);
		}
		if(sb.length() > 0) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * @return
	 */
	private String collectBugsData() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<Record> lines = new ArrayList<Record>();
		for (IProject project : projects) {
			Record line = createProjectLine(project);
			if(line != null) {
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

	/**
	 * @param project
	 * @param sortByName
	 * @return
	 */
	private Record createProjectLine(IProject project) {
		try {
			if(Util.isJavaProject(project) /* TODO why not working ?? && project.hasNature(FindbugsPlugin.NATURE_ID) */) {
				IMarker[] markerArr = project.findMarkers(FindBugsMarker.NAME, true,
						IResource.DEPTH_INFINITE);
				if (markerArr.length == 0) {
					return null;
				}
				int overallBugCount = markerArr.length;
				int notFilteredBugCount = 0;
				String usedExportFilters = getLastUsedExportFilters();
				for (IMarker marker : markerArr) {
					if(!isFiltered(marker, usedExportFilters)) {
						notFilteredBugCount ++;
					}
				}
				return new Record(project.getName(), overallBugCount, notFilteredBugCount);
			}
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Can't export project bugs for: " + project);
		}
		return null;
	}

	/**
	 * @param marker might be null
	 * @param usedExportFilters non null
	 * @return true if marker should be filtered
	 */
	private boolean isFiltered(IMarker marker, String usedExportFilters) {
		String type = marker.getAttribute(FindBugsMarker.BUG_TYPE, "not found");
		BugPattern result =  I18N.instance().lookupBugPattern(type);
		if(result == null) {
			return false;
		}
		String id = result.getAbbrev();
		return usedExportFilters.indexOf(id) >= 0;
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
			sb.append("Overall bugs number" + SEPARATOR + " Project name" + SEPARATOR
					+ "Not filtered bugs number\n");
			break;
		case BY_NOT_FILTERED_COUNT:
			sb.append("Not filtered bugs number" + SEPARATOR + "Project name" + SEPARATOR
					+ "Overall bugs number\n");
			break;
		case BY_NAME:
		default:
			sb.append("Project name" + SEPARATOR + "Not filtered bugs number" + SEPARATOR
					+ "Overall bugs number\n");
			break;
		}
	}

	public class Record implements Comparable<Record> {

		private final String name;
		private final int overallBugs;
		private final int notFilteredBugs;

		Record(String name, int overallBugs, int notFilteredBugs){
			this.name = name;
			this.overallBugs = overallBugs;
			this.notFilteredBugs = notFilteredBugs;
		}

		public int compareTo(Record other) {
			int result;
			switch (sortBy) {
			case BY_OVERALL_COUNT:
				result = other.overallBugs - overallBugs;
				if(result == 0) {
					return name.compareTo(other.name);
				}
				return result;
			case BY_NOT_FILTERED_COUNT:
				result = other.notFilteredBugs - notFilteredBugs;
				if(result == 0) {
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
