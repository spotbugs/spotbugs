/*
 * Contributions to FindBugs
 * Copyright (C) 2009, Andrei Loskutov
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
package de.tobject.findbugs.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Editor related stuff (line selection, annotation etc)
 * 
 * @author Andrei
 */
public class EditorUtil {

    /**
     * Default line which can be used to create a marker or annotation in the
     * editor
     */
    public static final int DEFAULT_LINE_IN_EDITOR = 1;

    /**
     * Selects and reveals the given line in given editor.
     * <p>
     * Must be executed from UI thread.
     * 
     * @param editorPart
     * @param lineNumber
     */
    public static void goToLine(IEditorPart editorPart, int lineNumber) {
        if (!(editorPart instanceof ITextEditor) || lineNumber < DEFAULT_LINE_IN_EDITOR) {
            return;
        }
        ITextEditor editor = (ITextEditor) editorPart;
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
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
                editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
            }
        }
    }
}
