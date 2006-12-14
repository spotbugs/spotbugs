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

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugAnnotation;
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
public class DetailsView extends ViewPart {

	private static DetailsView detailsView;

	
	private String description = "";

	private String title = "";

	private List annotationList;

	// HTML presentation classes that don't depend upon Browser
	private StyledText control;
	private DefaultInformationControl.IInformationPresenter presenter;
	private TextPresentation presentation = new TextPresentation();

	@CheckForNull private Browser browser;
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		annotationList = new List(sash, SWT.V_SCROLL);
		try {
			if (true) throw new SWTError("test");
			browser = new Browser(sash, SWT.NONE);
		} catch (SWTError e) {
			control = new StyledText(sash, SWT.READ_ONLY | SWT.H_SCROLL
					| SWT.V_SCROLL);
			control.setEditable(false);
			// Handle control resizing. The HTMLPresenter cares about window size
			// when presenting HTML, so we should redraw the control.
			control.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent e) {
					updateDisplay();
				}
			});
			presenter = new HTMLTextPresenter(false);
		}
		DetailsView.detailsView = this;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		annotationList.setFocus();
		control.setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		annotationList.dispose();
		if (browser != null) browser.dispose();
		else if (control != null) control.dispose();
	}

	/**
	 * Updates the control using the current window size and the contents of the
	 * title and description fields.
	 */
	private void updateDisplay() {
		String html = ("<b>" + title + "</b><br/>" + description);
		setHTMLText(html);
	}

	private void setHTMLText(String html) {
		if (browser != null && !browser.isDisposed()) browser.setText(html);
		else if (control != null && !control.isDisposed()) {
			Rectangle size = this.control.getClientArea();
			html = presenter.updatePresentation(getSite().getShell()
					.getDisplay(), html, presentation, size.width, size.height);
			control.setText(html);
			TextPresentation.applyTextPresentation(presentation, control);
		}
	}
	/**
	 * Set the content to be displayed.
	 * 
	 * @param title
	 *            the title of the bug
	 * @param description
	 *            the description of the bug
	 */
	public void setContent(String title, String description) {
		this.title = (title == null) ? "" : title.trim();
		this.description = (description == null) ? "" : description.trim();
		updateDisplay();
	}

	/**
	 * Show the details of a FindBugs marker in the details view. Brings the
	 * view to the foreground.
	 * 
	 * @param marker
	 *            the FindBugs marker containing the bug pattern to show details
	 *            for
	 */
	public static void showMarker(IMarker marker) {
		// Obtain the current workbench page, and show the details view
		IWorkbenchPage[] pages = FindbugsPlugin.getActiveWorkbenchWindow()
				.getPages();
		if (pages.length > 0) {
			try {
				pages[0].showView("de.tobject.findbugs.view.detailsview");

				String bugType =  marker.getAttribute(
						FindBugsMarker.BUG_TYPE, "");
				DetectorFactoryCollection.instance().ensureLoaded(); // fix bug#1530195
				BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
				if (pattern != null) {
					String shortDescription = pattern.getShortDescription();
					String detailText = pattern.getDetailText();
					DetailsView.getDetailsView().setContent(shortDescription,
							detailText);
				}

				List anList = DetailsView.getDetailsView().annotationList;
				anList.removeAll();
				BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
				// bug may be null, but if so then the error has already been logged.
				if (bug != null) {
					Iterator<BugAnnotation> it = bug.annotationIterator();
					while (it.hasNext()) {
						BugAnnotation ba = it.next();
						anList.add(ba.toString());
					}
				}

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
	public static DetailsView getDetailsView() {
		return detailsView;
	}

	/**
	 * Set the details view for the rest of the plugin. Details view should call
	 * this when it has been initialized.
	 * 
	 * @param view
	 *            the details view
	 */
	public static void setDetailsView(DetailsView view) {
		detailsView = view;
	}

}