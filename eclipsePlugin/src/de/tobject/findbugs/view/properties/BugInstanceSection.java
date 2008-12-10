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
package de.tobject.findbugs.view.properties;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.Util;
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * @author Andrei
 */
public class BugInstanceSection extends AbstractPropertySection {

	private Composite rootComposite;
	private List annotationList;
	private BugInstance bug;
	private IMarker marker;
	private IFile file;
	private String title;

	public BugInstanceSection() {
		super();
	}

	@Override
	public void createControls(Composite parent,
			final TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
		rootComposite = getWidgetFactory().createFlatFormComposite(parent);
		rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);
		Label label = getWidgetFactory().createLabel(rootComposite, "Bug annotations: ");
		label.setFont(JFaceResources.getBannerFont());
		FormData labelData = new FormData();
		labelData.left = new FormAttachment(0, 0);
		labelData.right = new FormAttachment(100, 0);
		labelData.top = new FormAttachment(0, 0);
		labelData.bottom = new FormAttachment(1, 0);
		label.setLayoutData(labelData);

		annotationList = getWidgetFactory().createList(rootComposite, SWT.NONE);
		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.top = new FormAttachment(label, 0);
		data.bottom = new FormAttachment(100, 0);
		annotationList.setLayoutData(data);
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
		final Menu menu = new Menu (annotationList);
		final MenuItem item = new MenuItem (menu, SWT.PUSH);
		item.setText ("Copy To Clipboard");
		item.addListener (SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				copyInfoToClipboard();
			}
		});
		menu.addListener (SWT.Show, new Listener () {
			public void handleEvent (Event event) {
				item.setEnabled(bug != null);
			}
		});
		annotationList.setMenu(menu);

	}

	@Override
	public void setInput(IWorkbenchPart part, ISelection selection) {
		super.setInput(part, selection);
		marker = MarkerUtil.getMarkerFromSingleSelection(selection);
		if(marker == null){
			bug = null;
			file = null;
			title = null;
		} else {
			bug = MarkerUtil.findBugInstanceForMarker(marker);
			file = (IFile) (marker.getResource() instanceof IFile ? marker
					.getResource() : null);
			if (file == null) {
				FindbugsPlugin.getDefault().logError(
						"Could not find file for " + bug.getMessage());
			}
			refreshTitle();
		}
		refreshAnnotations();
	}

	@Override
	public void refresh() {
		super.refresh();

	}

	@Override
	public void dispose() {
		if(rootComposite != null) {
			rootComposite.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean shouldUseExtraSpace() {
		return true;
	}

	private void selectInEditor(boolean openEditor) {
		if (bug == null || file == null) {
			return;
		}
		IWorkbenchPage page = getPart().getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		IEditorInput input = activeEditor != null? activeEditor.getEditorInput() : null;

		if (openEditor && !matchInput(input)) {
			try {
				activeEditor = IDE.openEditor(page, file);
				input = activeEditor.getEditorInput();
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not open editor for " + bug.getMessage());
			}
		}
		if(matchInput(input)) {
			int startLine = getLineToSelect();
			goToLine(activeEditor, startLine);
		}
	}

	private boolean matchInput(IEditorInput input) {
		return (input instanceof IFileEditorInput)
				&& file.equals(((IFileEditorInput) input).getFile());
	}

	private void refreshTitle() {
		String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
		BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
		if (pattern == null) {
			title = "";
			return;
		}
		String shortDescription = pattern.getShortDescription();
		String abbrev = "["
			+ bug.getPriorityAbbreviation()
			+ " " + bug.getCategoryAbbrev()
			+ " " + pattern.getAbbrev()
			+ "] ";
		if (shortDescription == null) {
			title = abbrev;
		} else {
			title = abbrev
					+ shortDescription.trim() + " [" + pattern.getType() + "]";
		}

	}

	private void refreshAnnotations() {
		annotationList.removeAll();

		// bug may be null, but if so then the error has already been logged.
		if (bug != null) {
			Iterator<BugAnnotation> it = bug.annotationIterator();
			while (it.hasNext()) {
				BugAnnotation ba = it.next();
				annotationList.add(ba.toString());
			}
		}
	}

	private int getLineToSelect() {
		int index = annotationList.getSelectionIndex();
		Iterator<BugAnnotation> theIterator = bug.annotationIterator();
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

	private void copyInfoToClipboard() {
		StringBuffer sb = new StringBuffer();
		sb.append(bug.getPriorityTypeString()).append(" ");
		sb.append(title);
		sb.append("\n");
		Iterator<BugAnnotation> iterator = bug.annotationIterator();
		while (iterator.hasNext()) {
			BugAnnotation bugAnnotation = iterator.next();
			sb.append(bugAnnotation.toString()).append("\n");
		}
		Util.copyToClipboard(sb.toString());
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
}
