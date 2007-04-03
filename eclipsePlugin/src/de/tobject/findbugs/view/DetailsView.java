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

import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SourceLineAnnotation;
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

    private Text priorityTypeArea;

    private String priorityTypeString = "";

    private BugInstance theBug;

    private ISelectionListener selectionListener;

    private SashForm sash;

    // HTML presentation classes that don't depend upon Browser
    @CheckForNull
    private StyledText control;

    private DefaultInformationControl.IInformationPresenter presenter;

    private TextPresentation presentation = new TextPresentation();

    @CheckForNull
    private Browser browser;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl(Composite parent) {
        sash = new SashForm(parent, SWT.VERTICAL);
        priorityTypeArea = new Text(sash, SWT.VERTICAL);
        priorityTypeArea.setEditable(false);
        annotationList = new List(sash, SWT.V_SCROLL);
        annotationList.setToolTipText("Additional information about the selected bug");
        annotationList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (theBug == null)
                    return;
                int index = annotationList.getSelectionIndex();
                Iterator<BugAnnotation> theIterator = theBug.annotationIterator();
                BugAnnotation theAnnotation = theIterator.next();
                for (int i = 0; i < index; i++)
                    theAnnotation = theIterator.next();
                if (theAnnotation instanceof SourceLineAnnotation) {
                    SourceLineAnnotation sla = (SourceLineAnnotation) theAnnotation;
                    IFile file = null;
                    try {
                        IProject project = MarkerUtil.findProjectForWarning(theBug);
                        if (project == null) return;
                        file = (IFile) MarkerUtil.getUnderlyingResource(theBug, project, sla);
                    } catch (JavaModelException ex) {
                        FindbugsPlugin.getDefault().logException(
                                ex, "Could not find file for " + theBug.getMessage());
                        return;
                    }
                    if (file == null){
                        FindbugsPlugin.getDefault().logError("Could not find file for " + theBug.getMessage());
                        return;
                    }
                    HashMap<Object, Object> map = new HashMap<Object, Object>();
                    map.put(IMarker.LINE_NUMBER, sla.getStartLine());
                   //  map.put(IDE.EDITOR_ID_ATTR,  org.eclipse.jdt.core.JavaCore;
                    try {
                        IMarker marker = file.createMarker(IMarker.TEXT);
                        marker.setAttributes(map);
                        IDE.openEditor(getSite().getPage(), marker); // 3.0 API
                        marker.delete();
                    } catch (PartInitException x) {
                        FindbugsPlugin.getDefault().logException(
                                x, "Could not create marker for " + theBug.getMessage());
                    } catch (CoreException y) {
                        FindbugsPlugin.getDefault().logException(
                                y, "Could not create marker for " + theBug.getMessage());
                    }
                }
            }
        });
        try {
            browser = new Browser(sash, SWT.NONE);
            browser.setToolTipText("Description of the selected bug");
        } catch (SWTError e) {
            control = new StyledText(sash, SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
            control.setEditable(false);
            // Handle control resizing. The HTMLPresenter cares about window
            // size
            // when presenting HTML, so we should redraw the control.
            control.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    updateDisplay();
                }
            });

            try {
                Class presenterClass = Class.forName("org.eclipse.jdt.internal.ui.text.HTMLTextPresenter.HTMLTextPresenter");
                presenter = (IInformationPresenter) presenterClass.getConstructor(Boolean.TYPE).newInstance(false);


            } catch (Exception e2) {
                FindbugsPlugin.getDefault().logException(
                        e2, "Could not create HTMLTextPresenter");
            }

        }
        sash.setWeights(new int[] { 1, 4, 8 });
        // Add selection listener to detect click in problems view or in tree
        // view
        ISelectionService theService = getSite().getWorkbenchWindow().getSelectionService();
        selectionListener = new ISelectionListener() {
            public void selectionChanged(IWorkbenchPart thePart, ISelection theSelection) {
                if (theSelection instanceof IStructuredSelection) {
                    Object elt = ((IStructuredSelection) theSelection).getFirstElement();
                    if (elt instanceof IMarker)
                        DetailsView.showMarker((IMarker) elt, false);
                    if (elt instanceof TreeItem) {
                        IMarker theMarker = BugTreeView.getMarkerForTreeItem((TreeItem) elt);
                        if (theMarker != null)
                            DetailsView.showMarker(theMarker, false);
                    }
                }
            }
        };
        theService.addSelectionListener(selectionListener);
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
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        if(selectionListener != null){
            getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(
                    selectionListener);
            selectionListener = null;
        }
        sash.dispose();
    	detailsView = null;
        super.dispose();
    }

    /**
     * Updates the control using the current window size and the contents of the
     * title and description fields.
     */
    private void updateDisplay() {
        String html = ("<b>" + title + "</b><br/>" + description);
        setHTMLText(html);
        priorityTypeArea.setText(this.priorityTypeString);
    }

    @SuppressWarnings("deprecation")
    private void setHTMLText(String html) {
        if (browser != null && !browser.isDisposed())
            browser.setText(html);

        else {
            StyledText control = this.control;
            if (control != null && !control.isDisposed() && presenter != null) {
                Rectangle size = this.control.getClientArea();
                html = presenter.updatePresentation(getSite().getShell().getDisplay(), html, presentation, size.width,
                        size.height);
                control.setText(html);
                TextPresentation.applyTextPresentation(presentation, control);
            }
        }
    }

    /**
     * Set the content to be displayed.
     *
     * @param title
     *            the title of the bug
     * @param description
     *            the description of the bug
     * @param theBug
     * 			  the BugInstance
     * @param priorityTypeString
     * 			  A string describing the priority and ategory (e.g. "High Priority Correctness"
     */
    public void setContent(String title, String description, BugInstance theBug, String priorityTypeString) {
        this.title = (title == null) ? "" : title.trim();
        this.description = (description == null) ? "" : description.trim();
        this.theBug = theBug;
        this.priorityTypeString = priorityTypeString;
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
     *            True if you want to set the focus to this view - but it won't if the user
     *            annotations view is already selected
     */
    public static void showMarker(IMarker marker, boolean focus) {

        // Obtain the current workbench page, and show the details view
        IWorkbenchPage[] pages = FindbugsPlugin.getActiveWorkbenchWindow().getPages();
        if (pages.length > 0) {
            try {
                if (focus && !(UserAnnotationsView.isVisible()))
                	pages[0].showView("de.tobject.findbugs.view.detailsview");
                if(detailsView == null)
                    return;
                String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
                String priorityTypeString = marker.getAttribute(FindBugsMarker.PRIORITY_TYPE, "");
                DetectorFactoryCollection.instance().ensureLoaded(); // fix
                // bug#1530195
                BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
                BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
                if (pattern != null) {
                    String shortDescription = pattern.getShortDescription();
                    String detailText = pattern.getDetailText();
                    if(DetailsView.getDetailsView() != null)
                    	DetailsView.getDetailsView().setContent(shortDescription, detailText, bug, priorityTypeString);
                }

                List anList = DetailsView.getDetailsView().annotationList;
                anList.removeAll();

                // bug may be null, but if so then the error has already been
                // logged.
                if (bug != null) {
                    Iterator<BugAnnotation> it = bug.annotationIterator();
                    while (it.hasNext()) {
                        BugAnnotation ba = it.next();
                        anList.add(ba.toString());
                    }
                }

            } catch (PartInitException e) {
                FindbugsPlugin.getDefault().logException(e, "Could not update bug details view");
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