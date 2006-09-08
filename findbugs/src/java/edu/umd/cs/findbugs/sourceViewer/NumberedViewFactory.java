package edu.umd.cs.findbugs.sourceViewer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;


class NumberedViewFactory implements ViewFactory {
	final HighlightInformation highlight;
    public NumberedViewFactory(HighlightInformation highlight) {
		this.highlight = highlight;
	}

	public View create(Element elem) {
        String kind = elem.getName();
       //  System.out.println("Kind: " + kind);
        if (kind != null)
            if (kind.equals(AbstractDocument.ContentElementName)) {
                return new LabelView(elem);
            }
            else if (kind.equals(AbstractDocument.
                             ParagraphElementName)) {
                return new NumberedParagraphView(elem, highlight);
            }
            else if (kind.equals(AbstractDocument.
                     SectionElementName)) {
                return new NoWrapBoxView(elem, View.Y_AXIS);
            }
            else if (kind.equals(StyleConstants.
                     ComponentElementName)) {
                return new ComponentView(elem);
            }
            else if (kind.equals(StyleConstants.IconElementName)) {
                return new IconView(elem);
            }
        // default to text display
        return new LabelView(elem);
    }
}
