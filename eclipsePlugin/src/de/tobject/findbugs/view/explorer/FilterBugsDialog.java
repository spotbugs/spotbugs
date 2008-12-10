/**
 *
 */
package de.tobject.findbugs.view.explorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.SelectionDialog;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.BugPattern;

/**
 * @author Andrei
 *
 */
public class FilterBugsDialog extends SelectionDialog {

	private final static class PatternLabelProvider implements ILabelProvider {
		public Image getImage(Object element) {
			return null;
		}

		public String getText(Object element) {
			if (!(element instanceof BugPattern)) {
				return null;
			}
			BugPattern pattern = (BugPattern) element;
			return pattern.getAbbrev() + " : " + pattern.getType();
		}

		public void addListener(ILabelProviderListener listener) {
			// noop
		}

		public void dispose() {
			// noop
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
			// noop
		}
	}

	private final Set<BugPattern> allowedPatterns;
	private final Map<String, Set<BugPattern>> preSelectedPatterns;
	private final Map<BugPattern, Boolean> selectionMap;
	private CheckboxTableViewer checkList;

	public FilterBugsDialog(Shell parentShell, Map<String, Set<BugPattern>> filtered) {
		super(parentShell);
		this.preSelectedPatterns = filtered;

		this.allowedPatterns = FindbugsPlugin.getKnownPatterns();

		selectionMap = new HashMap<BugPattern, Boolean>();

		initSelections();
	}

	private void initSelections() {
		for (BugPattern pattern : allowedPatterns) {
			selectionMap.put(pattern, isPreselected(pattern));
		}
	}

	private Boolean isPreselected(BugPattern pattern) {
		Collection<Set<BugPattern>> values = preSelectedPatterns.values();
		for (Set<BugPattern> set : values) {
			for (BugPattern bugPattern : set) {
				if(bugPattern.equals(pattern)){
					return Boolean.TRUE;
				}
			}
		}
		return Boolean.FALSE;
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		Composite composite = new Composite(parent, SWT.NONE);
		int columns = 2;
		composite.setLayout(new GridLayout(columns, false));
		GridData layoutData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL
				| GridData.GRAB_HORIZONTAL);
		layoutData.minimumHeight = 100;
		layoutData.minimumWidth = 100;
		layoutData.heightHint = 200;
		layoutData.widthHint = 400;

		composite.setLayoutData(layoutData);

		checkList = CheckboxTableViewer.newCheckList(composite, SWT.SINGLE | SWT.BORDER
				| SWT.RESIZE | SWT.V_SCROLL | SWT.H_SCROLL);

		Table table = checkList.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(false);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		TableColumn nameColumn = new TableColumn(table, SWT.NONE);
		nameColumn.setText("Bug Pattern");
		nameColumn.setResizable(true);
		nameColumn.setWidth(400);
		checkList.setContentProvider(new ArrayContentProvider());
		checkList.setLabelProvider(new PatternLabelProvider());
		checkList.setSorter(new ViewerSorter(){
			@Override
			public int category(Object element) {
				if(element instanceof BugPattern){
					return ((BugPattern)element).getAbbrev().hashCode();
				}
				return super.category(element);
			}
		});
		checkList.setInput(allowedPatterns);
		checkList.setCheckedElements(getPreselected());

		checkList.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				boolean checked = event.getChecked();
				BugPattern element = (BugPattern) event.getElement();
				selectionMap.put(element, Boolean.valueOf(checked));
			}
		});

		return composite;
	}

	private Object[] getPreselected() {
		Collection<Set<BugPattern>> values = preSelectedPatterns.values();
		List<BugPattern> all = new ArrayList<BugPattern>();
		for (Set<BugPattern> set : values) {
			all.addAll(set);
		}
		return all.toArray();
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings dialogSettings = FindbugsPlugin.getDefault().getDialogSettings();
		IDialogSettings section = dialogSettings.getSection("FilterBugDialog");
		if(section == null){
			dialogSettings.addNewSection("FilterBugDialog");
		}
		return section;
	}

	public Set<BugPattern> getPatterns() {
		Set<BugPattern> selected = new HashSet<BugPattern>();
		for (BugPattern pattern : allowedPatterns) {
			if (selectionMap.get(pattern).booleanValue()) {
				selected.add(pattern);
			}
		}
		return selected;
	}
}
