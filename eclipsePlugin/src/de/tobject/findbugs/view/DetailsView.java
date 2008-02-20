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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenterExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * View which shows bug details.
 *
 *
 * @author Phil Crosby
 * @author Andrei Loskutov
 * @version 2.0
 * @since 19.04.2004
 */
public class DetailsView extends AbstractFindbugsView {

	private String description;

	private String title;

	private List annotationList;

	private BugInstance theBug;

	private ISelectionListener selectionListener;

	// HTML presentation classes that don't depend upon Browser
	@CheckForNull
	private StyledText htmlControl;

	private DefaultInformationControl.IInformationPresenterExtension presenter;

	private TextPresentation presentation;

	@CheckForNull
	private Browser browser;

	private IFile file;

	private IMarker marker;

	public DetailsView() {
		super();
		description = "";
		title = "";
		presentation = new TextPresentation();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createRootControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL | SWT.SMOOTH);

		annotationList = new List(sash, SWT.V_SCROLL | SWT.H_SCROLL
				| SWT.BORDER);
		annotationList.setFont(JFaceResources.getDialogFont());
		annotationList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent evnt) {
				selectInEditor(false);
			}
		});
		annotationList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				selectInEditor(true);
			}
		});
		try {
			browser = new Browser(sash, SWT.NONE | SWT.BORDER);
		} catch (SWTError e) {
			htmlControl = new StyledText(sash, SWT.READ_ONLY | SWT.H_SCROLL
					| SWT.V_SCROLL | SWT.BORDER);
			htmlControl.setEditable(false);
			// Handle control resizing. The HTMLPresenter cares about window size
			// when presenting HTML, so we should redraw the control.
			htmlControl.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					updateDisplay();
				}
			});

			try {
				Class<IInformationPresenterExtension> presenterClass
				= (Class<IInformationPresenterExtension>) Class.forName("org.eclipse.jdt.internal.ui.text.HTMLTextPresenter.HTMLTextPresenter");
				presenter = presenterClass.getConstructor(Boolean.TYPE).newInstance(false);
			} catch (Exception e2) {
				FindbugsPlugin
						.getDefault()
						.logException(new RuntimeException(e.getMessage(), e),
								"Could not create a org.eclipse.swt.widgets.Composite.Browser");
			}

		}
		sash.setWeights(new int[] {1, 2 });
		// Add selection listener to detect click in problems view or in tree
		// view
		ISelectionService theService = getSite().getWorkbenchWindow()
				.getSelectionService();
		selectionListener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart thePart,
					ISelection theSelection) {
				if (!(theSelection instanceof IStructuredSelection)) {
					return;
				}
				if (!isVisible()) {
					return;
				}
				IMarker theMarker = null;
				Object elt = ((IStructuredSelection) theSelection)
						.getFirstElement();
				if (elt instanceof IMarker) {
					theMarker = (IMarker) elt;
				} else if (elt instanceof TreeItem) {
					theMarker = BugTreeView
							.getMarkerForTreeItem((TreeItem) elt);
				}
				if (theMarker != null) {
					selectMarker(theMarker);
				}
			}
		};
		theService.addSelectionListener(selectionListener);
		return sash;
	}

	private static void goToLine(IEditorPart editorPart, int lineNumber) {
		if (!(editorPart instanceof ITextEditor) || lineNumber <= 0) {
			return;
		}
		ITextEditor editor = (ITextEditor) editorPart;
		IDocument document = editor.getDocumentProvider().getDocument(
				editor.getEditorInput());
		if (document != null) {
			IRegion lineInfo = null;
			try {
				// line count internaly starts with 0, and not with 1 like in
				// GUI
				lineInfo = document.getLineInformation(lineNumber - 1);
			} catch (BadLocationException e) {
				// ignored because line number may not really exist in document,
				// we guess this...
			}
			if (lineInfo != null) {
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo
						.getLength());
			}
		}
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
		if (selectionListener != null) {
			getSite().getWorkbenchWindow().getSelectionService()
					.removeSelectionListener(selectionListener);
			selectionListener = null;
		}
		theBug = null;
		file = null;
		super.dispose();
	}

	/**
	 * Updates the control using the current window size and the contents of the
	 * title and description fields.
	 */
	private void updateDisplay() {
		String html = "<b>" + title + "</b><br/>" + description;
		setHTMLText(html);
	}

	@SuppressWarnings("deprecation")
	private void setHTMLText(String html) {
		if (browser != null && !browser.isDisposed()) {
			browser.setText(html);
		} else {
			if (htmlControl != null && !htmlControl.isDisposed() && presenter != null) {
				Rectangle size = htmlControl.getClientArea();
				html = presenter.updatePresentation(getSite().getShell()
						.getDisplay(), html, presentation, size.width,
						size.height);
				htmlControl.setText(html);
				TextPresentation.applyTextPresentation(presentation, htmlControl);
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
	 *            the BugInstance
	 * @param priorityTypeString
	 *            A string describing the priority and ategory (e.g. "High
	 *            Priority Correctness"
	 * @param marker
	 */
	private void setContent(BugPattern pattern, BugInstance theBug,
			String priorityTypeString, IMarker marker) {
		this.marker = marker;
		String abbrev = null;
		if (pattern != null) {
			String shortDescription = pattern.getShortDescription();
			String detailText = pattern.getDetailText();
			abbrev = "[" + pattern.getAbbrev() + "] ";
			title = (shortDescription == null) ? abbrev : abbrev + shortDescription.trim();
			description = (detailText == null) ? "" : detailText.trim();
		} else {
			title = "";
			description = "";
			abbrev = "";
		}
		this.theBug = theBug;
		this.file = (IFile) (marker.getResource() instanceof IFile ? marker
				.getResource() : null);
		if (file == null) {
			FindbugsPlugin.getDefault().logError(
					"Could not find file for " + theBug.getMessage());
		}
		setContentDescription(abbrev + priorityTypeString);
		setTitleToolTip(getTitle());
		showAnnotations(theBug);
		updateDisplay();
		IViewPart viewPart = getSite().getPage()
			.findView(FindbugsPlugin.USER_ANNOTATIONS_VIEW_ID);
		if (viewPart instanceof UserAnnotationsView) {
			UserAnnotationsView.showMarker(marker);
		}
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
		IWorkbenchPage page = FindbugsPlugin.getActiveWorkbenchWindow()
				.getActivePage();
		// first find view, if it is already open - this does not steal focus
		// from editor
		IViewPart viewPart = page.findView(FindbugsPlugin.DETAILS_VIEW_ID);
		if (!(viewPart instanceof DetailsView)) {
			// view is not shown => open it in the page
			viewPart = AbstractFindbugsView.showDetailsView();
		}
		if (viewPart instanceof DetailsView) {
			showInView(marker, (DetailsView) viewPart);
		}
	}

	/**
	 * @param marker
	 * @param detailsView
	 */
	private static void showInView(IMarker marker, DetailsView detailsView) {
		String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
		String priorityTypeString = marker.getAttribute(
				FindBugsMarker.PRIORITY_TYPE, "");
		BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
		BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
		detailsView.setContent(pattern, bug, priorityTypeString, marker);
		detailsView.activate();
	}

	private void selectMarker(IMarker newMarker) {
		if (!isVisible()) {
			showMarker(newMarker);
		} else {
			showInView(newMarker, this);
		}
	}

	/**
	 * @param bug
	 */
	private void showAnnotations(BugInstance bug) {
		annotationList.removeAll();

		// bug may be null, but if so then the error has already been
		// logged.
		if (bug != null) {
			Iterator<BugAnnotation> it = bug.annotationIterator();
			while (it.hasNext()) {
				BugAnnotation ba = it.next();
				annotationList.add(ba.toString());
			}
		}
	}

	/**
	 *
	 */
	private void selectInEditor(boolean openEditor) {
		if (theBug == null || file == null) {
			return;
		}
		IEditorPart activeEditor = getSite().getPage().getActiveEditor();
		IEditorInput input = activeEditor != null? activeEditor.getEditorInput() : null;

		if (openEditor && !matchInput(input)) {
			try {
				activeEditor = IDE
						.openEditor(getSite().getPage(), file);
				input = activeEditor.getEditorInput();
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not open editor for " + theBug.getMessage());
			}
		}
		if(matchInput(input)) {
			int startLine = getLineToSelect();
			goToLine(activeEditor, startLine);
		}
	}

	/**
	 * @param input
	 * @return
	 */
	private boolean matchInput(IEditorInput input) {
		return (input instanceof IFileEditorInput)
				&& file.equals(((IFileEditorInput) input).getFile());
	}

	/**
	 * @return
	 */
	private int getLineToSelect() {
		int index = annotationList.getSelectionIndex();
		Iterator<BugAnnotation> theIterator = theBug.annotationIterator();
		BugAnnotation theAnnotation = theIterator.next();
		for (int i = 0; i < index; i++) {
			theAnnotation = theIterator.next();
		}
		if (!(theAnnotation instanceof SourceLineAnnotation)) {
			// return the line from our initial marker
			return marker.getAttribute(IMarker.LINE_NUMBER, -1);
		}
		SourceLineAnnotation sla = (SourceLineAnnotation) theAnnotation;
		int startLine = sla.getStartLine();
		return startLine;
	}

	/**
	 * @return the marker
	 */
	public IMarker getMarker() {
		return marker;
	}
}
