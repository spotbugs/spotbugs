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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import de.tobject.findbugs.preferences.FindBugsConstants;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * @author Andrei Loskutov
 */
public class WorkspaceSettingsTab extends Composite {

	private final Button confirmSwitch;
	private final Button switchTo;
	private final IPreferenceStore store;
	private final Button confirmBuild;

	public WorkspaceSettingsTab(TabFolder tabFolder, final FindbugsPropertyPage page,
			int style) {
		super(tabFolder, style);
		setLayout(new GridLayout());
		store = page.getPreferenceStore();

		TabItem tabDetector = new TabItem(tabFolder, SWT.NONE);
		tabDetector.setText("Workspace Settings");
		tabDetector.setControl(this);

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

	public void refreshUI(UserPreferences prefs) {
		confirmSwitch.setSelection(store
				.getBoolean(FindBugsConstants.ASK_ABOUT_PERSPECTIVE_SWITCH));
		switchTo.setSelection(store
				.getBoolean(FindBugsConstants.SWITCH_PERSPECTIVE_AFTER_ANALYSIS));
		confirmBuild.setSelection(!store
				.getBoolean(FindBugsConstants.DONT_REMIND_ABOUT_FULL_BUILD));
	}

}
