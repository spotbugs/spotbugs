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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.WeakHashMap;

import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.View;

import edu.umd.cs.findbugs.gui2.Driver;

// Code inspired by http://www.developer.com/java/other/article.php/3318421

class NumberedParagraphView extends ParagraphView {
    public final static int NUMBERS_WIDTH = (int) Driver.getFontSize() * 3 + 9;

    HighlightInformation highlight;

    public NumberedParagraphView(Element e, HighlightInformation highlight) {
        super(e);
        this.highlight = highlight;
    }

    // protected void setInsets(short top, short left, short bottom,
    // short right) {super.setInsets
    // (top,(short)(left+NUMBERS_WIDTH),
    // bottom,right);
    // }
    @Override
    public void paint(Graphics g, Shape allocation) {
        Rectangle r = (allocation instanceof Rectangle) ? (Rectangle) allocation : allocation.getBounds();

        Color oldColor = g.getColor();
        Integer lineNumber = getLineNumber();
        Color highlightColor = highlight.getHighlight(lineNumber);
        if (highlightColor != null) {
            g.setColor(highlightColor);
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(oldColor);
        }
        // r.x += NUMBERS_WIDTH;

        super.paint(g, r);

        FontMetrics metrics = g.getFontMetrics();

        g.setColor(Color.GRAY);
        String lineNumberString = lineNumber.toString();
        int width = metrics.stringWidth(lineNumberString);
        int numberX = r.x - width - 9 + NUMBERS_WIDTH;
        int numberY = r.y + metrics.getAscent();

        g.drawString(lineNumberString, numberX, numberY);
        g.setColor(oldColor);
        // System.out.println("Drawing line for " + lineNumber + " @ " + numberX
        // +"," + numberY);
        // r.x -= NUMBERS_WIDTH;

    }

    public int getPreviousLineCount0() {
        int lineCount = 0;
        View parent = this.getParent();
        int count = parent.getViewCount();
        for (int i = 0; i < count; i++) {
            if (parent.getView(i) == this) {
                break;
            } else {
                lineCount += parent.getView(i).getViewCount();
            }
        }
        return lineCount;
    }

    static WeakHashMap<Element, Integer> elementLineNumberCache = new WeakHashMap<Element, Integer>();

    public Integer getLineNumber() {

        Element element = this.getElement();
        Integer result = elementLineNumberCache.get(element);
        if (result != null) {
            return result;
        }
        Element parent = element.getParentElement();
        int count = parent.getElementCount();
        for (int i = 0; i < count; i++) {
            elementLineNumberCache.put(parent.getElement(i), i + 1);
        }
        result = elementLineNumberCache.get(element);
        if (result != null) {
            return result;
        }
        return -1;

    }
}
