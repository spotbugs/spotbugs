package edu.umd.cs.findbugs.sourceViewer;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.ViewFactory;

class NumberedEditorKit extends StyledEditorKit {
	final HighlightInformation highlight;
	public  NumberedEditorKit(HighlightInformation highlight) {
		this.highlight = highlight;
	}
    public ViewFactory getViewFactory() {
        return new NumberedViewFactory(highlight);
    }
}