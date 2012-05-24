package edu.umd.cs.findbugs.testcases;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class Bug3524078 {
    private IEditorPart editorPart;
    public IEditorPart getEditorPart() {
        return editorPart;
    }
    public void setEditorPart(IEditorPart editorPart) {
        this.editorPart = editorPart;
    }
    public ISelectionProvider getSelectionProvider() {
    if (editorPart == null) {
    return null;
    }
    if (editorPart instanceof ITextEditor) {
    return ((ITextEditor) editorPart).getSelectionProvider();
    }
    if (editorPart instanceof ISelectionProvider) {
    return (ISelectionProvider) editorPart; // <- Unchecked/unconfirmed cast from org.eclipse.ui.IEditorPart to org.eclipse.jface.viewers.ISelectionProvider in de.loskutov.anyedit.ui.editor.AbstractEditor.getSelectionProvider()
    }
    return null;
    }

}
