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

import org.eclipse.jdt.internal.ui.text.HTMLTextPresenter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

/**
 * View which shows bug details.
 * 
 * TODO (PeterF) This info should be displayed in the help system or maybe a marker popup.
 * @author Phil Crosby
 * @version 1.0
 * @since 19.04.2004
 */
public class DetailsView extends ViewPart {
	
	private static DetailsView detailsView;
	
	private StyledText control;
	
	private String description = "";
	
	private String title = "";
	
	// HTML presentation classes
	private DefaultInformationControl.IInformationPresenter presenter;
	private TextPresentation presentation = new TextPresentation();
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		control = new StyledText(parent, SWT.READ_ONLY | SWT.H_SCROLL
				| SWT.V_SCROLL);
		control.setEditable(false);
		// Handle control resizing. The HTMLPresenter cares about window size
		// when presenting HTML, so we should redraw the control.
		control.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				updateDisplay();
			}
		});
		presenter = new HTMLTextPresenter(false);
		DetailsView.detailsView = this;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	public void setFocus() {
		control.setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		control.dispose();
	}
	
	/**
	 * Updates the control using the current window size and the contents
	 * of the title and description fields.
	 */
	private void updateDisplay() {
		if (control != null && !control.isDisposed()) {
			presentation.clear();
			Rectangle size = this.control.getClientArea();
			String html = ("<b>" + title + "</b><br>" + description);
			html = presenter.updatePresentation(getSite().getShell()
					.getDisplay(), html, presentation, size.width, size.height);
			control.setText(html);
			TextPresentation.applyTextPresentation(presentation, control);
		}
	}
	
	/**
	 * Set the content to be displayed.
	 * 
	 * @param title the title of the bug
	 * @param description the description of the bug
	 */
	public void setContent(String title, String description) {
		this.title = (title == null) ? "" : title.trim();
		this.description = (description == null) ? "" : description.trim();
		updateDisplay();
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
	 * @param view the details view
	 */
	public static void setDetailsView(DetailsView view) {
		detailsView = view;
	}

}