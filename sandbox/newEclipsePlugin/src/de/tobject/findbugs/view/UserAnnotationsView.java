/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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

package de.tobject.findbugs.view;

import java.util.Calendar;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * View which shows bug annotations.
 *
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 19.04.2004
 */
public class UserAnnotationsView extends AbstractFindbugsView {

	private String userAnnotation;

	private String firstVersionText;

	private @CheckForNull
	BugInstance theBug;

	private Text userAnnotationTextField;

	private Label firstVersionLabel;

	private Combo designationComboBox;

	private ISelectionListener selectionListener;

	public UserAnnotationsView() {
		super();
		userAnnotation = "";
		firstVersionText = "";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createRootControl(Composite parent) {
		Composite main = new Composite(parent, SWT.VERTICAL);
		main.setLayout(new GridLayout(2, false));
		designationComboBox = new Combo(main, SWT.LEFT | SWT.DROP_DOWN
				| SWT.READ_ONLY);
		designationComboBox.setToolTipText("User-specified bug designation");
		designationComboBox.setLayoutData(new GridData());
		for (String s : I18N.instance().getUserDesignationKeys(true)) {
			designationComboBox.add(I18N.instance().getUserDesignation(s));
		}
		designationComboBox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (theBug != null) {
					theBug.setUserDesignationKeyIndex(
						designationComboBox.getSelectionIndex(), null);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
		designationComboBox.setSize(designationComboBox.computeSize(
				SWT.DEFAULT, SWT.DEFAULT));
		designationComboBox.setEnabled(false);

		firstVersionLabel = new Label(main, SWT.LEFT);
		firstVersionLabel
				.setToolTipText("The earliest version in which the bug was present");
		firstVersionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		userAnnotationTextField = new Text(main, SWT.LEFT | SWT.WRAP
				| SWT.BORDER);
		userAnnotationTextField
				.setToolTipText("Type comments about the selected bug here");
		userAnnotationTextField.setEnabled(false);
		GridData uatfData = new GridData(GridData.FILL_BOTH);
		uatfData.horizontalSpan = 2;
		userAnnotationTextField.setLayoutData(uatfData);
		userAnnotationTextField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (theBug != null) {
					theBug.setAnnotationText(
							userAnnotationTextField.getText(), null);
				}
			}
		});
		// Add selection listener to detect click in problems view or bug tree
		// view
		ISelectionService theService = getSite().getWorkbenchWindow()
				.getSelectionService();

		selectionListener = new MarkerSelectionListener(this);
		theService.addSelectionListener(selectionListener);
		return main;
	}

	@Override
	public void dispose() {
		if (selectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService()
					.removeSelectionListener(selectionListener);
			selectionListener = null;
		}
		super.dispose();
	}

	/**
	 * Updates the control using the current window size and the contents of the
	 * title and description fields.
	 */
	private void updateDisplay() {
		userAnnotationTextField.setText(userAnnotation);
		firstVersionLabel.setText(firstVersionText);
		if (theBug == null) {
			return;
		}
		int comboIndex = theBug.getUserDesignationKeyIndex();
		if (comboIndex == -1) {
			FindbugsPlugin.getDefault()
					.logError("Cannot find user designation");
		} else {
			designationComboBox.select(comboIndex);
		}
	}

	/**
	 * Set the content to be displayed
	 */
	public void setContent(String userAnnotation, BugInstance bug,
			String firstVersionText) {
		this.userAnnotation = (userAnnotation == null) ? "" : userAnnotation
				.trim();
		this.firstVersionText = (firstVersionText == null) ? ""
				: firstVersionText.trim();
		this.theBug = bug;
		this.userAnnotationTextField.setEnabled(theBug != null);
		this.designationComboBox.setEnabled(theBug != null);
		updateDisplay();
	}

	/**
	 * Show the details of a FindBugs marker in the view. Brings the
	 * view to the foreground.
	 *
	 * @param marker
	 *            the FindBugs marker containing the bug pattern to show details
	 *            for
	 */
	private void showInView(IMarker marker) {

		String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
		long timestamp = Long.parseLong(marker.getAttribute(
				FindBugsMarker.FIRST_VERSION, "-2"));
		String firstVersion = "Bug present since: "	+ convertTimestamp(timestamp);
		BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
		if (pattern == null) {
			return;
		}
		BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
		String annotation;
		if (bug == null) {
			annotation = "Error - BugInstance not found.";
		} else {
			annotation = bug.getAnnotationText();
		}

		setContent(annotation, bug, firstVersion);

	}

	void showMarker(IMarker marker) {
		if (marker != null) {
			showInView(marker);
		}
	}

	public void markerSelected(IMarker newMarker) {
		showMarker(newMarker);
		if (!isVisible()) {
			activate();
		}
	}

	private static String convertTimestamp(long timestamp) {
		if (timestamp == -2) {
			return "ERROR - Timestamp not found";
		}
		if (timestamp == -1) {
			return "First version analyzed";
		}
		Calendar theCalendar = Calendar.getInstance();
		theCalendar.setTimeInMillis(System.currentTimeMillis());
		theCalendar.set(theCalendar.get(Calendar.YEAR), theCalendar
				.get(Calendar.MONTH), theCalendar.get(Calendar.DATE), 0, 0, 0);
		long beginningOfToday = theCalendar.getTimeInMillis();
		long beginningOfYesterday = beginningOfToday - 86400000;
		theCalendar.setTimeInMillis(timestamp);
		String timeString = theCalendar.getTime().toString();
		if (timestamp >= beginningOfToday) {
			return "Today "
					+ timeString.substring(timeString.indexOf(":") - 2,
							timeString.indexOf(":") + 3);
		} else if (timestamp >= beginningOfYesterday) {
			return "Yesterday "
					+ timeString.substring(timeString.indexOf(":") - 2,
							timeString.indexOf(":") + 3);
		} else {
			return timeString.substring(0, timeString.indexOf(":") + 3);
		}
	}

	@Override
	public void setFocus() {
		designationComboBox.setFocus();
	}

	@Override
	protected void fillLocalToolBar(IToolBarManager manager) {
		// noop
	}
}
