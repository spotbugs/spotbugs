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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.tobject.findbugs.FindbugsPlugin;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugin.eclipse.util.FileSelectionDialog;

/**
 * @author Andrei Loskutov
 */
public class FilterFilesTab extends Composite {

	private final FindbugsPropertyPage propertyPage;


	private static final class FilePlaceHolder extends WorkbenchAdapter
			implements IAdaptable {

		private final IFile file;

		public FilePlaceHolder(IFile file) {
			this.file = file;
		}

		@Override
		public String getLabel(Object object) {
			return file.getProjectRelativePath().toString();
		}

		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			IWorkbenchAdapter adapter = (IWorkbenchAdapter) file
					.getAdapter(IWorkbenchAdapter.class);
			if (adapter != null) {
				return adapter.getImageDescriptor(file);
			}

			return super.getImageDescriptor(object);
		}

		public Object getAdapter(Class adapter) {
			if (adapter.equals(IWorkbenchAdapter.class)) {
				return this;
			}
			return null;
		}

		public IFile getFile() {
			return file;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			if (obj instanceof FilePlaceHolder) {
				return file.equals(((FilePlaceHolder) obj).file);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return file.hashCode();
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

		createFilterTable(this, FilterKind.INCLUDE);
		createFilterTable(this, FilterKind.EXCLUDE);
		createFilterTable(this, FilterKind.EXCLUDE_BUGS);

	}

	/**
	 * Helper method to shorten message access
	 * @param key a message key
	 * @return requested message
	 */
	protected String getMessage(String key) {
		return FindbugsPlugin.getDefault().getMessage(key);
	}

	private void createFilterTable(Composite parent, final FilterKind kind) {
		Composite tableComposite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		tableComposite.setLayout(layout);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 2, 1));
		Label titleLabel = new Label(tableComposite, SWT.NULL);
		final String title = getMessage(kind.propertyName);

		titleLabel.setText(title);
		titleLabel.setLayoutData(new GridData(SWT.LEAD, SWT.CENTER, true,
				false, 2, 1));
		final TableViewer viewer = new TableViewer(tableComposite, SWT.MULTI
				| SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setContentProvider(new BaseWorkbenchContentProvider());
		viewer.setLabelProvider(new WorkbenchLabelProvider());
		viewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
		Collection<String> filterFiles = kind.selectedFiles(propertyPage.getCurrentUserPreferences());

		final List<FilePlaceHolder> filters = new ArrayList<FilePlaceHolder>();
		final IProject project = propertyPage.getProject();
		if (filterFiles != null) {
			for (String s : filterFiles) {
				filters.add(new FilePlaceHolder(project.getFile(s)));
			}
		}
		viewer.add(filters.toArray());
		final Button addButton = new Button(tableComposite, SWT.PUSH);
		String addButtonLabel = getMessage(kind.propertyName +"addbutton");

		addButton.setText(addButtonLabel);
		addButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false,
				false));
		final UserPreferences currentUserPreferences = propertyPage.getCurrentUserPreferences();
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileSelectionDialog dialog =
					new FileSelectionDialog(addButton.getShell(), title, ".xml");
				dialog.setInput(project);
				dialog.setAllowMultiple(true);
				// The validator checks to see if the user's selection
				// is valid given the type of the object selected (e.g.
				// it can't be a folder) and the objects that have
				// already been selected
				dialog.setValidator(new ISelectionStatusValidator() {
					public IStatus validate(Object[] selection) {
						for(int i = 0; i < selection.length; i++) {
							if(selection[i] instanceof IContainer) {
								return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
										IStatus.ERROR, "Folder selected", null);
							}
							else if(selection[i] instanceof IFile) {
								final Collection<String> fFiles = kind.selectedFiles(currentUserPreferences );
								final Collection<String> fOFiles  = kind.excludedFiles(currentUserPreferences);

								IFile f = (IFile)selection[i];
								String fn = f.getProjectRelativePath().toString();
								if(fOFiles.contains(fn)) {
									// File is already selected in the
									// other filter
									return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
											IStatus.ERROR, "File " + fn +
											" already selected in a conflicting list", null);
								}
								else if(fFiles.contains(fn)) {
									// File is already selected in this
									// filter
									return new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID,
											IStatus.ERROR, "File " + fn +
											" already selected for this filter", null);
								}
							}
						}
						return new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
								IStatus.OK, "", null);
					}
				});
				if (dialog.open() == ElementTreeSelectionDialog.OK) {
					Object[] result = dialog.getResult();
					for (int i = 0; i < result.length; i++) {
						FilePlaceHolder holder = new FilePlaceHolder((IFile) result[i]);
						filters.add(holder);
						viewer.add(holder);
					}

					kind.setFiles(currentUserPreferences, filesToStrings(filters));

				}
			}
		});
		final Button removeButton = new Button(tableComposite, SWT.PUSH);
		removeButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false,
				true));
		String removeButtonLabel = getMessage(kind.propertyName +"removebutton");

		removeButton.setText(removeButtonLabel);
		removeButton.setEnabled(false);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Iterator selectionIter = ((IStructuredSelection) viewer
						.getSelection()).iterator();
				while (selectionIter.hasNext()) {
					Object element = selectionIter.next();
					FilePlaceHolder holder = (FilePlaceHolder) element;
					filters.remove(holder);
					viewer.remove(holder);
				}
				kind.setFiles(currentUserPreferences, filesToStrings(filters));

			}
		});

		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				removeButton.setEnabled(!event.getSelection().isEmpty());
			}
		});
	}


	private Set<String> filesToStrings(List<FilePlaceHolder> filters) {
		Set<String>result = new LinkedHashSet<String>();
		for (FilePlaceHolder holder : filters) {
			result.add(holder.getFile().getProjectRelativePath().toString());
		}

		return result;
	}

	void restoreDefaultSettings() {
		// XXX restore default filter settings (just made tables empty?)
	}

	private enum FilterKind {
		INCLUDE("property.includefilter") {
			@Override
			Collection<String> selectedFiles(UserPreferences u) {
				return u.getIncludeFilterFiles();
			}

			@Override
			Collection<String> excludedFiles(UserPreferences u) {
				return u.getExcludeFilterFiles();
			}

			@Override
			void setFiles(UserPreferences u, Collection<String> files) {
				u.setIncludeFilterFiles(files);
			}
		},
		EXCLUDE("property.excludefilter") {
			@Override
			Collection<String> selectedFiles(UserPreferences u) {
				return u.getExcludeFilterFiles();
			}

			@Override
			Collection<String> excludedFiles(UserPreferences u) {
				return u.getIncludeFilterFiles();
			}

			@Override
			void setFiles(UserPreferences u, Collection<String> files) {
				u.setExcludeFilterFiles(files);
			}
		},
		EXCLUDE_BUGS("property.excludebugs") {
			@Override
			Collection<String> selectedFiles(UserPreferences u) {
				return u.getExcludeBugsFiles();
			}

			@Override
			Collection<String> excludedFiles(UserPreferences u) {
				return Collections.emptyList();
			}

			@Override
			void setFiles(UserPreferences u, Collection<String> files) {
				u.setExcludeBugsFiles(files);
			}
		};
		final String propertyName;

		FilterKind(String propertyName) {
			this.propertyName = propertyName;
		}

		abstract Collection<String> selectedFiles(UserPreferences u);

		abstract Collection<String> excludedFiles(UserPreferences u);

		abstract void setFiles(UserPreferences u, Collection<String> files);
	}
}


