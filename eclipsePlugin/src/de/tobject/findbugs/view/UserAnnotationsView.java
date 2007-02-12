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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * View which shows bug details.
 * 
 * TODO (PeterF) This info should be displayed in the help system or maybe a
 * marker popup. (philc) Custom marker popup info is notoriously hard as of
 * Eclipse 3.0.
 * 
 * @author Phil Crosby
 * @version 1.0
 * @since 19.04.2004
 */
public class UserAnnotationsView extends ViewPart {

	private static UserAnnotationsView userAnnotationsView;
	
	private String userAnnotation = "";
	
	private String firstVersionText = "";
	
	private BugInstance theBug;
	
	private Text userAnnotationTextField;
	
	private Label firstVersionLabel;
	
	private Combo designationComboBox;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		SashForm top = new SashForm(sash, SWT.HORIZONTAL);
		designationComboBox = new Combo(top, SWT.LEFT|SWT.DROP_DOWN|SWT.READ_ONLY);
		for (String s : I18N.instance().getUserDesignationKeys(true)) {
			designationComboBox.add(I18N.instance().getUserDesignation(s));
		}
		designationComboBox.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e)
			{
				if(theBug != null)
					theBug.getNonnullUserDesignation().setDesignationKey(I18N.instance().getUserDesignationKeys(true).get(designationComboBox.getSelectionIndex()));
			}
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}}
			);
		designationComboBox.setSize(designationComboBox.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		Label versionLabel = new Label(top, SWT.LEFT);
		versionLabel.setText("Bug present since:");
		versionLabel.setSize(versionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		firstVersionLabel = new Label(top, SWT.LEFT);
		//firstVersionLabel.setSize(firstVersionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		top.setSize(top.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		userAnnotationTextField = new Text(sash, SWT.LEFT);
		userAnnotationTextField.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e)
			{
				theBug.getUserDesignation().setAnnotationText(userAnnotationTextField.getText());
			}
		});
		sash.setWeights(new int[]{1, 5});
		top.setWeights(new int[]{2, 3, 12});
		//	Add selection listener to detect click in problems view or bug tree view
		ISelectionService theService = this.getSite().getWorkbenchWindow().getSelectionService();
		theService.addSelectionListener(new ISelectionListener(){
			public void selectionChanged(IWorkbenchPart thePart, ISelection theSelection)
			{
				if(theSelection instanceof IStructuredSelection)
				{
					Object elt = ((IStructuredSelection)theSelection).getFirstElement();
					if(elt instanceof IMarker)
						UserAnnotationsView.showMarker((IMarker)elt, false);
					if(elt instanceof TreeItem)
					{
						IMarker theMarker = BugTreeView.getMarkerForTreeItem((TreeItem)elt);
						if(theMarker != null)
							UserAnnotationsView.showMarker(theMarker, false);
					}
				}
			}
		});
		UserAnnotationsView.userAnnotationsView = this;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		//annotationList.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		//annotationList.dispose();
	}

	/**
	 * Updates the control using the current window size and the contents of the
	 * title and description fields.
	 */
	private void updateDisplay() {
		userAnnotationTextField.setText(userAnnotation);
		firstVersionLabel.setText(firstVersionText);
		int comboIndex = I18N.instance().getUserDesignationKeys(true).indexOf(theBug.getNonnullUserDesignation().getDesignationKey());
		if(comboIndex == -1)
			System.out.println("Cannot find user designation");
		else
			designationComboBox.select(comboIndex);
	}

	/**
	 * Set the content to be displayed.
	 * 
	 * @param title
	 *            the title of the bug
	 * @param description
	 *            the description of the bug
	 */
	public void setContent(String userAnnotation, BugInstance bug, String firstVersionText) {
		this.userAnnotation = (userAnnotation == null) ? "" : userAnnotation.trim();
		this.firstVersionText = (firstVersionText == null) ? "" : firstVersionText.trim();
		this.theBug = bug;
		updateDisplay();
	}

	/**
	 * Show the details of a FindBugs marker in the details view. Brings the
	 * view to the foreground.
	 * 
	 * @param marker
	 *            the FindBugs marker containing the bug pattern to show details
	 *            for
	 * @param focus
	 * 			  True if you want to set the focus to this view
	 */
	public static void showMarker(IMarker marker, boolean focus) {
		// Obtain the current workbench page, and show the details view
		IWorkbenchPage[] pages = FindbugsPlugin.getActiveWorkbenchWindow()
				.getPages();
		if (pages.length > 0) {
			try {
				if(focus)
					pages[0].showView("de.tobject.findbugs.view.userannotationsview");

				String bugType =  marker.getAttribute(
						FindBugsMarker.BUG_TYPE, "");
				String firstVersionText = marker.getAttribute(FindBugsMarker.FIRST_VERSION, "");
				DetectorFactoryCollection.instance().ensureLoaded(); // fix bug#1530195
				BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
				BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
				String userAnnotation = "";
				if(bug == null)
					userAnnotation = "Error - BugInstance not found.";
				else
					userAnnotation = bug.getNonnullUserDesignation().getAnnotationText();
				
				if (pattern != null)
					UserAnnotationsView.getuserAnnotationsView().setContent(userAnnotation, bug, firstVersionText);

			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not update bug details view");
			}
		}
	}

	/**
	 * Accessor for the details view associated with this plugin.
	 * 
	 * @return the details view, or null if it has not been initialized yet
	 */
	public static UserAnnotationsView getuserAnnotationsView() {
		return userAnnotationsView;
	}

	/**
	 * Set the details view for the rest of the plugin. Details view should call
	 * this when it has been initialized.
	 * 
	 * @param view
	 *            the details view
	 */
	public static void setuserAnnotationsView(UserAnnotationsView view) {
		userAnnotationsView = view;
	}

}
