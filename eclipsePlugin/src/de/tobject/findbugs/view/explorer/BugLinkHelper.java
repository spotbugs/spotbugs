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
package de.tobject.findbugs.view.explorer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.texteditor.ITextEditor;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * This class adds the "link to editor" ability to the Bug Explorer View.
 *
 * @author Andrei
 */
public class BugLinkHelper implements ILinkHelper {

	private volatile IEditorInput lastSelectedInput;

	public BugLinkHelper() {
		super();
	}

	public void activateEditor(IWorkbenchPage page, IStructuredSelection selection) {
//		IWorkbenchPart activePart = page.getActivePart();
		IMarker markerFromSelection = MarkerUtil.getMarkerFromSingleSelection(selection);
		IResource resource = null;
		if(markerFromSelection != null) {
			resource = markerFromSelection.getResource();
		} else {
			resource = getFile(selection);
			if(resource == null) {
				return;
			}
		}

//		boolean editorActivated = false;
		if (resource instanceof IFile) {
			/*editorActivated = */activateEditor(page, (IFile) resource, markerFromSelection);
		}
		// This leads to endless loop if editor AND problems view both are active
//		if (activePart != null && editorActivated) {
//			page.activate(activePart);
//		}
	}

	private IFile getFile(IStructuredSelection selection) {
		if(selection.size() != 1){
			return null;
		}
		Object element = selection.getFirstElement();
		if(element instanceof IAdaptable){
			IAdaptable adaptable = (IAdaptable) element;
			IResource res = (IResource) adaptable.getAdapter(IResource.class);
			if(res != null && res.getType() == IResource.FILE){
				return (IFile) res;
			}
		}
		return null;
	}

	private boolean activateEditor(IWorkbenchPage page, IFile file, IMarker marker) {
		IEditorPart activeEditor = page.getActiveEditor();
		if (activeEditor == null) {
			return false;
		}
		IEditorInput editorInput = activeEditor.getEditorInput();
		if (matchInput(editorInput, file)) {
			try {
				if(marker != null){
					// code below leads to unexpected scrolling to the marker on editor switch
					// just because bug explorer was open (even if it is not visible)
					// IDE.openEditor(page, marker);
					IDE.openEditor(page, (IFile) marker.getResource());
				} else {
					IDE.openEditor(page, file);
				}
				lastSelectedInput = editorInput;
				return true;
			} catch (PartInitException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Failed to link to the editor for file: " + file);
			}
		}
		return false;
	}

	private boolean matchInput(IEditorInput input, IResource file) {
		return file.equals(input.getAdapter(IFile.class));
	}



	public IStructuredSelection findSelection(IEditorInput input) {
		if (lastSelectedInput == input) {
			lastSelectedInput = null;
			return StructuredSelection.EMPTY;
		}
		lastSelectedInput = null;

		int startLine = getStartLine();
		IMarker[] allMarkers;
		IResource resource = (IResource) input.getAdapter(IFile.class);
		allMarkers = MarkerUtil.getAllMarkers(resource);
		if(allMarkers.length == 0) {
			return StructuredSelection.EMPTY;
		}
		if (startLine < 0) {
			return (allMarkers.length != 0) ? new StructuredSelection(allMarkers)
					: StructuredSelection.EMPTY;
		}
		// line count starts internaly with 0, and not with 1 like in the Editor GUI
		startLine += 1;
		for (IMarker marker : allMarkers) {
			int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
			if (line == startLine) {
				return new StructuredSelection(marker);
			}
		}
		return (allMarkers.length != 0) ? new StructuredSelection(allMarkers)
				: StructuredSelection.EMPTY;
	}

	private int getStartLine() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage();
		IEditorPart activeEditor = activePage.getActiveEditor();
		if (!(activeEditor instanceof ITextEditor)) {
			return -1;
		}
		ITextEditor textEditor = (ITextEditor) activeEditor;
		ITextSelection selection2 = (ITextSelection) textEditor.getSelectionProvider()
				.getSelection();
		return selection2 != null ? selection2.getStartLine() : -1;
	}

}
