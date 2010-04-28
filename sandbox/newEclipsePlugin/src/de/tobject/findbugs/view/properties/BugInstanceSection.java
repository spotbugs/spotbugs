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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.reporter.MarkerUtil;
import de.tobject.findbugs.util.EditorUtil;
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
	private IJavaElement javaElt;

	public BugInstanceSection() {
		super();
	}

	@Override
	public void createControls(Composite parent,
			final TabbedPropertySheetPage tabbedPropertySheetPage) {
		super.createControls(parent, tabbedPropertySheetPage);
		Color background = tabbedPropertySheetPage.getWidgetFactory().getColors()
				.getBackground();

		rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = 5;
		layout.marginTop = 5;
		rootComposite.setLayout(layout);
		rootComposite.setSize(SWT.DEFAULT, SWT.DEFAULT);

		Group group = new Group(rootComposite, SWT.NONE);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(1, false));
		group.setText("Bug annotations:");

		rootComposite.setBackground(background);
		group.setBackground(background);

		annotationList = new List(group, SWT.NONE);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalIndent = 0;
		data.verticalIndent = 0;
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
		item.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(
				ISharedImages.IMG_TOOL_COPY));
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
			javaElt = null;
		} else {
			bug = MarkerUtil.findBugInstanceForMarker(marker);
			file = (IFile) (marker.getResource() instanceof IFile ? marker
					.getResource() : null);
			javaElt = MarkerUtil.findJavaElementForMarker(marker);
			if (file == null && javaElt == null) {
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
		if (bug == null || (file == null && javaElt == null)) {
			return;
		}
		IWorkbenchPage page = getPart().getSite().getPage();
		IEditorPart activeEditor = page.getActiveEditor();
		IEditorInput input = activeEditor != null? activeEditor.getEditorInput() : null;

		if (openEditor && !matchInput(input)) {
			try {
				if(file != null) {
					activeEditor = IDE.openEditor(page, file);
				} else if(javaElt != null){
					activeEditor = JavaUI.openInEditor(javaElt, true, true);
				}
				if(activeEditor != null) {
					input = activeEditor.getEditorInput();
				}
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not open editor for " + bug.getMessage());
			} catch (CoreException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not open editor for " + bug.getMessage());
			}
		}
		if(matchInput(input)) {
			int startLine = getLineToSelect();
			EditorUtil.goToLine(activeEditor, startLine);
		}
	}

	private boolean matchInput(IEditorInput input) {
		if(file != null && (input instanceof IFileEditorInput)){
			return file.equals(((IFileEditorInput) input).getFile());
		}
		if(javaElt != null && input != null){
			IJavaElement javaElement = JavaUI.getEditorInputJavaElement(input);
			if(javaElt.equals(javaElement)){
				return true;
			}
			IJavaElement parent = javaElt.getParent();
			while(parent != null && !parent.equals(javaElement)){
				parent = parent.getParent();
			}
			if(parent != null && parent.equals(javaElement)){
				return true;
			}
		}
		return false;
	}

	private void refreshTitle() {
		String bugType = marker.getAttribute(FindBugsMarker.BUG_TYPE, "");
		BugPattern pattern = I18N.instance().lookupBugPattern(bugType);
		if (pattern == null || bug == null) {
			title = "";
			return;
		}
		String shortDescription = bug.getAbridgedMessage();
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
			return marker.getAttribute(IMarker.LINE_NUMBER, EditorUtil.DEFAULT_LINE_IN_EDITOR);
		}
		SourceLineAnnotation sla = (SourceLineAnnotation) theAnnotation;
		int startLine = sla.getStartLine();
		return startLine <= 0? EditorUtil.DEFAULT_LINE_IN_EDITOR : startLine;
	}

	private void copyInfoToClipboard() {
		StringBuffer sb = new StringBuffer();
		sb.append(title);
		sb.append("\n");
		sb.append(bug.getPriorityTypeString()).append(" ");
		sb.append("\n");
		Iterator<BugAnnotation> iterator = bug.annotationIterator();
		while (iterator.hasNext()) {
			BugAnnotation bugAnnotation = iterator.next();
			sb.append(bugAnnotation.toString()).append("\n");
		}
		if(file != null){
			sb.append(file.getLocation()).append("\n");
		}
		Util.copyToClipboard(sb.toString());
	}

}
