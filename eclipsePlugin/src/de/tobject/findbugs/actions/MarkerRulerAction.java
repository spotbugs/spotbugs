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
package de.tobject.findbugs.actions;

import java.util.ArrayList;

import javax.annotation.CheckForNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.IUpdate;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;

/**
 * An action that can display a bug marker's details in the FindBugs details
 * view. TODO (PeterF) We should replace this action with a marker resolution or
 * a marker help contribution.
 *
 * @author Phil Crosby
 * @author Peter Friese
 * @version 1.0
 * @since 20.4.2004
 */
public class MarkerRulerAction implements IEditorActionDelegate, IUpdate, MouseListener, IMenuListener {

    private IVerticalRulerInfo ruler;

    @CheckForNull
    private ITextEditor editor;

    /** Contains the markers of the currently selected line in the ruler margin. */
    private final ArrayList<IMarker> markers;

    /**
     * The action sent to this delegate. Enable and disable it based upon
     * whether there are FindBugs markers on the current line
     */
    private IAction action;

    public MarkerRulerAction() {
        super();
        markers = new ArrayList<IMarker>();
    }

    public void setActiveEditor(IAction callerAction, IEditorPart targetEditor) {
        Control control;
        // See if we're already listenting to an editor; if so, stop listening
        if (editor != null) {
            if (ruler != null) {
                control = ruler.getControl();
                if (control != null && !control.isDisposed()) {
                    control.removeMouseListener(this);
                }
            }
            if (editor instanceof ITextEditorExtension) {
                ((ITextEditorExtension) editor).removeRulerContextMenuListener(this);
            }
        }

        // Start listening to the current editor
        if (targetEditor instanceof ITextEditor) {
            ITextEditor textEditor = (ITextEditor) targetEditor;
            editor = textEditor;
            // Check for editor's ruler context listener capability
            if (textEditor instanceof ITextEditorExtension) {
                ((ITextEditorExtension) textEditor).addRulerContextMenuListener(this);
            }
            ruler = (IVerticalRulerInfo) textEditor.getAdapter(IVerticalRulerInfo.class);
            if (ruler != null) {
                control = ruler.getControl();
                if (control != null && !control.isDisposed()) {
                    control.addMouseListener(this);
                }
            }
        } else {
            ruler = null;
            editor = null;
        }
    }

    public void run(IAction action1) {
        this.action = action1;
        obtainFindBugsMarkers();
        if (markers.size() == 0 && editor != null) {
            MessageDialog.openError(editor.getSite().getShell(), "Error Showing Bug Details",
                    "You must first select a FindBugs marker to view bug details.");
        } else {
            update();
        }
    }

    public void selectionChanged(IAction action1, ISelection selection) {
        this.action = action1;
    }

    /**
     * Fills markers field with all of the FindBugs markers associated with the
     * current line in the text editor's ruler marign.
     */
    protected void obtainFindBugsMarkers() {
        // Delete old markers
        markers.clear();
        if (editor == null || ruler == null) {
            return;
        }

        // Obtain all markers in the editor
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IFile.class);
        if(resource == null){
            return;
        }
        IMarker[] allMarkers = MarkerUtil.getMarkers(resource, IResource.DEPTH_ZERO);
        if(allMarkers.length == 0) {
            return;
        }
        // Discover relevant markers, i.e. FindBugsMarkers
        AbstractMarkerAnnotationModel model = getModel();
        IDocument document = getDocument();
        for (int i = 0; i < allMarkers.length; i++) {
            if (includesRulerLine(model.getMarkerPosition(allMarkers[i]), document)) {
                if (MarkerUtil.isFindBugsMarker(allMarkers[i])) {
                    markers.add(allMarkers[i]);
                }
            }
        }
    }

    public void update() {
        if (markers.size() > 0) {
            IMarker marker = markers.get(0);
            if (action.getId().endsWith("showBugInfo")) {
                FindbugsPlugin.showMarker(marker, FindbugsPlugin.DETAILS_VIEW_ID, editor);
            } else {
                // TODO show all
                FindbugsPlugin.showMarker(marker, FindbugsPlugin.TREE_VIEW_ID, editor);
            }
            markers.clear();
        }
    }

    /**
     * Checks a Position in a document to see whether the line of last mouse
     * activity falls within this region.
     *
     * @param position
     *            Position of the marker
     * @param document
     *            the Document the marker resides in
     * @return true if the last mouse click falls on the same line as the marker
     */
    protected boolean includesRulerLine(Position position, IDocument document) {
        if (position != null && ruler != null) {
            try {
                int markerLine = document.getLineOfOffset(position.getOffset());
                int line = ruler.getLineOfLastMouseButtonActivity();
                if (line == markerLine) {
                    return true;
                }
            } catch (BadLocationException x) {
                FindbugsPlugin.getDefault().logException(x, "Error getting marker line");
            }
        }
        return false;
    }

    /**
     * Retrieves the AbstractMarkerAnnontationsModel from the editor.
     *
     * @return AbstractMarkerAnnotatiosnModel from the editor
     */
    @CheckForNull
    protected AbstractMarkerAnnotationModel getModel() {
        if(editor == null) {
            return null;
        }
        IDocumentProvider provider = editor.getDocumentProvider();
        IAnnotationModel model = provider.getAnnotationModel(editor.getEditorInput());
        if (model instanceof AbstractMarkerAnnotationModel) {
            return (AbstractMarkerAnnotationModel) model;
        }
        return null;
    }

    /**
     * Retrieves the document from the editor.
     *
     * @return the document from the editor
     */
    protected IDocument getDocument() {
        Assert.isNotNull(editor);
        IDocumentProvider provider = editor.getDocumentProvider();
        return provider.getDocument(editor.getEditorInput());
    }

    public void menuAboutToShow(IMenuManager manager) {
        if (action != null) {
            obtainFindBugsMarkers();
            action.setEnabled(markers.size() > 0);
        }
    }

    public void mouseDoubleClick(MouseEvent e) {
        //
    }

    public void mouseDown(MouseEvent e) {
        // Only capture left clicks.
        if (e.button == 1) {
            obtainFindBugsMarkers();
            if (markers.size() > 0) {
                update();
            }
        }
    }

    public void mouseUp(MouseEvent e) {
        //
    }

}
