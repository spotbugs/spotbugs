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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.FindBugsWorker;
import de.tobject.findbugs.preferences.FindBugsConstants;
import de.tobject.findbugs.preferences.PrefsUtil;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class WorkspaceSettingsTab extends Composite {


	private final Button confirmSwitch;
	private final Button switchTo;
	private final IPreferenceStore store;
	private final Button confirmBuild;
	private final FindbugsPropertyPage page;
	private final DetectorProvider detectorProvider;
	private boolean pluginsChanged;

	public WorkspaceSettingsTab(TabFolder tabFolder, final FindbugsPropertyPage page,
			int style) {
		super(tabFolder, style);
		this.page = page;
		setLayout(new GridLayout());
		store = page.getPreferenceStore();

		TabItem tabDetector = new TabItem(tabFolder, SWT.NONE);
		tabDetector.setText("Misc. Settings");
		tabDetector.setControl(this);


		ManagePathsWidget pathsWidget = new ManagePathsWidget(this);
		ListViewer viewer = pathsWidget.createViewer("Custom Detectors",
				"See: <a href=\"http://www.ibm.com/developerworks/library/j-findbug2/\">'Writing custom detectors'</a>" +
				" and <a href=\"http://fb-contrib.sourceforge.net/\">fb-contrib</a>: additional bug detectors package");
		detectorProvider = createDetectorProvider(viewer);
		pathsWidget.createButtonsArea(detectorProvider);
		detectorProvider.refresh();

		switchTo = new Button(this, SWT.CHECK);
		switchTo.setText("Switch to the FindBugs perspective after analysis");
		switchTo.setSelection(store
				.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS));
		switchTo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				store.setValue(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS,
						switchTo.getSelection());
			}
		});

		confirmSwitch = new Button(this, SWT.CHECK);
		confirmSwitch.setText("Ask before switching to the FindBugs perspective");
		confirmSwitch.setSelection(store
				.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH));
		confirmSwitch.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				store.setValue(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH,
						confirmSwitch.getSelection());
			}
		});

		confirmBuild = new Button(this, SWT.CHECK);
		confirmBuild.setText("Remind to redo analysis after changes of relevant settings");
		confirmBuild.setSelection(!store
				.getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD));
		confirmBuild.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				store.setValue(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD,
						!confirmBuild.getSelection());
			}
		});
	}

	protected DetectorProvider createDetectorProvider(ListViewer viewer) {
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
		confirmSwitch.setSelection(store
				.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH));
		switchTo.setSelection(store
				.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS));
		confirmBuild.setSelection(!store
				.getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD));
		detectorProvider.setFilters(store);
		detectorProvider.refresh();
	}



	static class DetectorProvider extends PathsProvider {

		protected DetectorProvider(ListViewer viewer, FindbugsPropertyPage propertyPage) {
			super(viewer, propertyPage);
			setFilters(propertyPage.getPreferenceStore());
		}

		List<PathElement> getFilterFiles(IPreferenceStore prefs) {
			// TODO project is currently not supported (always null).
			IProject project = propertyPage.getProject();
			final List<PathElement> newPaths = new ArrayList<PathElement>();
			Collection<String> filterPaths = PrefsUtil.readDetectorPaths(prefs);
			if (filterPaths != null) {
				for (String path : filterPaths) {
					IPath filterPath = FindBugsWorker.getFilterPath(path, project);
//					if(filterPath.toFile().exists()) {
						newPaths.add(new PathElement(filterPath, Status.OK_STATUS));
//					}
				}
			}
			return newPaths;
		}

		@Override
		protected void applyToPreferences() {
			super.applyToPreferences();
			PrefsUtil.writeDetectorPaths(propertyPage.getPreferenceStore(), pathsToStrings());
		}

		void setFilters(IPreferenceStore prefs) {
			setFilters(getFilterFiles(prefs));
		}

		@Override
		protected IStatus validate() {
			DetectorValidator validator = new DetectorValidator();
			IStatus bad = null;
			for (PathElement path : paths) {
				IStatus status = validator.validate(path.getPath());
				path.setStatus(status);
				if(!status.isOK()){
					bad = status;
					break;
				}
			}
			return bad;
		}

		@Override
		protected void configureDialog(FileDialog dialog) {
			dialog.setFilterExtensions(new String[]{"*.jar"});
			dialog.setText("Select jar file(s) containing custom detectors");
		}
	}



	public void performOK() {
		final SortedSet<String> detectorPaths = PrefsUtil.readDetectorPaths(store);

		if(DetectorFactoryCollection.isLoaded()) {
			DetectorFactoryCollection dfc = DetectorFactoryCollection.instance();
			URL[] pluginList = dfc.getPluginList();
			boolean shouldReplace = pluginList.length != detectorPaths.size();
			if(!shouldReplace) {
				// check if both lists are really identical
				for (URL url : pluginList) {
					String file = url.getFile();
					IPath filterPath = FindBugsWorker.getFilterPath(file, null);
					if(!detectorPaths.contains(filterPath.toPortableString())) {
						shouldReplace = true;
						break;
					}
				}
			}
			if(!shouldReplace) {
				return;
			}
		} else if(detectorPaths.isEmpty()) {
			return;
		}
		FindbugsPlugin.applyCustomDetectors(detectorPaths, true);
		pluginsChanged = true;
	}

	public boolean arePluginsChanged() {
		return pluginsChanged;
	}

}
