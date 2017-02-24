package sfBugs;

public class Bug3524078 {
    private Number editorPart;
    public Number getEditorPart() {
        return editorPart;
    }
    public void setEditorPart(Number editorPart) {
        this.editorPart = editorPart;
    }
    public int getSelectionProvider() {
    if (editorPart == null) {
    return 0;
    }
    if (editorPart instanceof Float) {
    return ((Float) editorPart).intValue();
    }
    if (editorPart instanceof Integer) {
    return ((Integer) editorPart).intValue(); // <- Unchecked/unconfirmed cast from org.eclipse.ui.IEditorPart to org.eclipse.jface.viewers.ISelectionProvider in de.loskutov.anyedit.ui.editor.AbstractEditor.getSelectionProvider()
    }
    return 0;
    }
}
