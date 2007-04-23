/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

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
