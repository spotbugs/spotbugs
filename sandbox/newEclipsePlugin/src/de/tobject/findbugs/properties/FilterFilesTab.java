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
package de.tobject.findbugs.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class FilterFilesTab extends Composite {

	private static IPath lastUsedPath;
	private final FindbugsPropertyPage propertyPage;
	private final FilterProvider filterIncl;
	private final FilterProvider filterExcl;
	private final FilterProvider filterExclBugs;

	private final class SelectionValidator {
		private final UserPreferences prefs;
		private final Collection<String> exclFiles;

		public SelectionValidator(FilterKind kind) {
			prefs = propertyPage.getCurrentUserPreferences();
			exclFiles = kind.excludedPaths(prefs);
		}

		public IStatus validate(String path) {
			if (exclFiles.contains(path)) {
				return FindbugsPlugin.createErrorStatus("Filter selected in a conflicting list", null);
			}
			return FindbugsPlugin.createStatus(IStatus.OK, "", null);
		}
	}

	protected class FilterProvider extends SelectionAdapter implements IStructuredContentProvider {

		protected final List<PathElement> paths;
		private final FilterKind kind;
		private final Control control;
		private final ListViewer viewer;

		protected FilterProvider(ListViewer viewer, FilterKind kind) {
			this.paths = new ArrayList<PathElement>();
			this.viewer = viewer;
			this.control = viewer.getList();
			this.kind = kind;
			setFilters(propertyPage.getCurrentUserPreferences());
		}

		void setFilters(UserPreferences prefs) {
			paths.clear();
			paths.addAll(getFilterFiles(kind, prefs));
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			addFiles(e.display.getActiveShell());
		}
		
		public void addFiles(Shell parentShell) {
			FileDialog dialog = createFileDialog(parentShell);

			// The validator checks to see if the user's selection
			// is valid given the type of the object selected (e.g.
			// it can't be a folder) and the objects that have
			// already been selected
			String pathStr = openFileDialog(dialog);
			if (pathStr == null) {
				return;
			}
			addSelectedPaths(dialog);
			applyToPreferences();
			validateAllFilters();
		}
		
		private FileDialog createFileDialog(Shell parentShell) {
			FileDialog dialog = new FileDialog(parentShell, SWT.OPEN | SWT.MULTI);
			dialog.setFilterExtensions(new String[]{"*.xml"});
			dialog.setText(getMessage(kind.propertyName) + ": select xml file(s) containing filters");

			IPath lastUsed = getLastUsedPath();
			String filterPath = null;
			if(lastUsed != null && lastUsed.toFile().isDirectory()){
				filterPath = lastUsed.toOSString();
				dialog.setFilterPath(filterPath);
			}
			return dialog;
		}
		
		protected String openFileDialog(FileDialog dialog) {
			return dialog.open();
		}

		protected String[] getFileNames(FileDialog dialog) {
			return dialog.getFileNames();
		}

		protected String getFilterPath(FileDialog dialog) {
			return dialog.getFilterPath();
		}
		
		private void addSelectedPaths(FileDialog dialog) {
			String[] names = getFileNames(dialog);
			String filterPath = getFilterPath(dialog);
			for (String fileName : names) {
				IPath path = new Path(filterPath).append(fileName);
				PathElement pathElt = new PathElement(path, Status.OK_STATUS);
				if(!paths.contains(pathElt)) {
					paths.add(pathElt);
				}
			}
		}

		public void dispose() {
			//
		}

		public void inputChanged(Viewer viewer1, Object oldInput, Object newInput) {
			//
		}

		public Object[] getElements(Object inputElement) {
			return paths.toArray();
		}

		boolean contains(Object o){
			return paths.contains(o);
		}

		void setControlEnabled(boolean enabled){
			control.setEnabled(enabled);
		}

		void refresh(){
			validate();
			viewer.setSelection(null);
			viewer.setInput(new Object());
			viewer.refresh(true);
		}

		private void validate() {
			SelectionValidator validator = new SelectionValidator(kind);
			IStatus bad = null;
			IProject project = propertyPage.getProject();
			for (PathElement path : paths) {
				String filterPath = FindBugsWorker.toFilterPath(path.getPath(), project).toOSString();
				IStatus status = validator.validate(filterPath);
				path.setStatus(status);
				if(!status.isOK()){
					bad = status;
				}
			}
			if(bad != null){
				propertyPage.setErrorMessage(bad.getMessage());
			}
		}

		public void remove(PathElement holder) {
			paths.remove(holder);
			applyToPreferences();
			validateAllFilters();
		}

		private void applyToPreferences() {
			validate();
			kind.setPaths(propertyPage.getCurrentUserPreferences(),	pathsToStrings(paths));
		}
	}

	protected static final class PathElement {

		private final IPath path;
		private IStatus status;

		public PathElement(IPath path, IStatus status) {
			this.path = path;
			this.status = status;
		}

		public void setStatus(IStatus status) {
			this.status = status;
		}

		@Override
		public String toString() {
			return path.toString() + (status.isOK()? "" : " (" + status.getMessage() + ")");
		}

		public String getPath() {
			return path.toOSString();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof PathElement) {
				return path.equals(((PathElement) obj).path);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}
	}

	/**
	 * @param parent
	 * @param style
	 */
	public FilterFilesTab(TabFolder parent, FindbugsPropertyPage page, int style) {
		super(parent, style);
		this.propertyPage = page;
		setLayout(new GridLayout(2, true));

		TabItem tabDetector = new TabItem(parent, SWT.NONE);
		tabDetector.setText(getMessage("property.filterFilesTab"));
		tabDetector.setControl(this);
		tabDetector.setToolTipText("Configure external bug reporting filters");

		filterIncl = createFilter(this, FilterKind.INCLUDE);
		filterExcl = createFilter(this, FilterKind.EXCLUDE);
		filterExclBugs = createFilter(this, FilterKind.EXCLUDE_BUGS);
		validateAllFilters();
	}

	public void validateAllFilters() {
		propertyPage.setErrorMessage(null);
		filterIncl.refresh();
		filterExcl.refresh();
		filterExclBugs.refresh();
	}

	public static void setLastUsedPath(IPath lastUsed) {
		// TODO write to preferences
		lastUsedPath = lastUsed;
	}

	public static IPath getLastUsedPath() {
		// TODO read from preferences
		return lastUsedPath;
	}

	/**
	 * Helper method to shorten message access
	 * @param key a message key
	 * @return requested message
	 */
	protected String getMessage(String key) {
		return FindbugsPlugin.getDefault().getMessage(key);
	}

	private FilterProvider createFilter(final Composite parent, final FilterKind kind) {
		Composite tableComposite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		tableComposite.setLayout(layout);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		Label titleLabel = new Label(tableComposite, SWT.NULL);
		final String title = getMessage(kind.propertyName);

		titleLabel.setText(title);
		titleLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true, false, 2, 1));
		final ListViewer viewer = new ListViewer(tableComposite, SWT.MULTI | SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));

		final FilterProvider contentProvider = createFilterProvider(viewer, kind);
		viewer.setContentProvider(contentProvider);
		final Button addButton = new Button(tableComposite, SWT.PUSH);
		String addButtonLabel = getMessage(kind.propertyName + "addbutton");

		addButton.setText(addButtonLabel);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false));

		addButton.addSelectionListener(contentProvider);
		final Button removeButton = new Button(tableComposite, SWT.PUSH);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, true));
		String removeButtonLabel = getMessage(kind.propertyName + "removebutton");

		removeButton.setText(removeButtonLabel);
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Iterator<?> selectionIter = ((IStructuredSelection) viewer.getSelection())
						.iterator();
				while (selectionIter.hasNext()) {
					contentProvider.remove((PathElement) selectionIter.next());
				}
			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!event.getSelection().isEmpty());
			}
		});
		return contentProvider;
	}

	protected FilterProvider createFilterProvider(ListViewer viewer, FilterKind kind) {
		return new FilterProvider(viewer, kind);
	}

	private List<PathElement> getFilterFiles(FilterKind kind, UserPreferences prefs) {
		IProject project = propertyPage.getProject();
		final List<PathElement> paths = new ArrayList<PathElement>();
		Collection<String> filterPaths = kind.selectedPaths(prefs);
		if (filterPaths != null) {
			for (String path : filterPaths) {
				IPath filterPath = FindBugsWorker.getFilterPath(path, project);
				if(filterPath.toFile().exists()) {
					paths.add(new PathElement(filterPath, Status.OK_STATUS));
				}
			}
		}
		return paths;
	}


	private Set<String> pathsToStrings(List<PathElement> paths) {
		IProject project = propertyPage.getProject();
		Set<String>result = new LinkedHashSet<String>();
		for (PathElement path : paths) {
			IPath filterPath = FindBugsWorker.toFilterPath(path.getPath(), project);
			result.add(filterPath.toOSString());
		}
		return result;
	}

	protected enum FilterKind {
		INCLUDE("property.includefilter") {
			@Override
			Collection<String> selectedPaths(UserPreferences u) {
				return u.getIncludeFilterFiles();
			}

			@Override
			Collection<String> excludedPaths(UserPreferences u) {
				Set<String> excl = new HashSet<String>();
				excl.addAll(u.getExcludeFilterFiles());
				excl.addAll(u.getExcludeBugsFiles());
				return excl;
			}

			@Override
			void setPaths(UserPreferences u, Collection<String> files) {
				u.setIncludeFilterFiles(files);
			}
		},
		EXCLUDE("property.excludefilter") {
			@Override
			Collection<String> selectedPaths(UserPreferences u) {
				return u.getExcludeFilterFiles();
			}

			@Override
			Collection<String> excludedPaths(UserPreferences u) {
				Set<String> excl = new HashSet<String>();
				excl.addAll(u.getIncludeFilterFiles());
				excl.addAll(u.getExcludeBugsFiles());
				return excl;
			}

			@Override
			void setPaths(UserPreferences u, Collection<String> files) {
				u.setExcludeFilterFiles(files);
			}
		},
		EXCLUDE_BUGS("property.excludebugs") {
			@Override
			Collection<String> selectedPaths(UserPreferences u) {
				return u.getExcludeBugsFiles();
			}

			@Override
			Collection<String> excludedPaths(UserPreferences u) {
				Set<String> excl = new HashSet<String>();
				excl.addAll(u.getIncludeFilterFiles());
				excl.addAll(u.getExcludeFilterFiles());
				return excl;
			}

			@Override
			void setPaths(UserPreferences u, Collection<String> files) {
				u.setExcludeBugsFiles(files);
			}
		};
		final String propertyName;

		FilterKind(String propertyName) {
			this.propertyName = propertyName;
		}

		abstract Collection<String> selectedPaths(UserPreferences u);

		abstract Collection<String> excludedPaths(UserPreferences u);

		abstract void setPaths(UserPreferences u, Collection<String> files);
	}

	@Override
	public void setEnabled(boolean enabled) {
		filterExcl.setControlEnabled(enabled);
		filterIncl.setControlEnabled(enabled);
		filterExclBugs.setControlEnabled(enabled);
		super.setEnabled(enabled);
	}

	void refreshUI(UserPreferences prefs) {
		filterExcl.setFilters(prefs);
		filterExclBugs.setFilters(prefs);
		filterIncl.setFilters(prefs);
		validateAllFilters();
	}
	
	protected FilterProvider getFilterIncl() {
		return filterIncl;
	}
	
	protected FilterProvider getFilterExcl() {
		return filterExcl;
	}
	
	protected FilterProvider getFilterExclBugs() {
		return filterExclBugs;
	}
}
