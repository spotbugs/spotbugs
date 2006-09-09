package edu.umd.cs.findbugs.sourceViewer;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.WeakHashMap;

import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;


// Code inspired by http://www.developer.com/java/other/article.php/3318421

class NumberedParagraphView extends ParagraphView {
    public static short NUMBERS_WIDTH=45;
    HighlightInformation highlight;
    public NumberedParagraphView(Element e, HighlightInformation highlight) {
        super(e);
        this.highlight = highlight;
    }


    public void paint(Graphics g, Shape allocation) {
        Rectangle r = (allocation instanceof Rectangle) ?
                (Rectangle)allocation : allocation.getBounds();

    	AttributeSet attributes = getAttributes();
    	Object o = attributes.getAttribute(StyleConstants.Background);
    	Color oldColor = g.getColor();
		Integer lineNumber = getLineNumber();
    	Color highlightColor = highlight.getHighlight(lineNumber);
    	if (highlightColor != null) {
    		g.setColor(highlightColor);
    		g.fillRect(r.x, r.y, r.width, r.height);
    		g.setColor(oldColor);
    	}
		r.x += NUMBERS_WIDTH;
		
		super.paint(g, r);


        FontMetrics metrics = g.getFontMetrics();
         
        g.setColor(Color.GRAY);
        String lineNumberString = lineNumber.toString();
        int width = metrics.stringWidth(lineNumberString);
        int numberX = r.x - width-9;
        int numberY = r.y + metrics.getAscent();
       
		g.drawString(lineNumberString, numberX, numberY);
        g.setColor(oldColor);
        // System.out.println("Drawing line for " + lineNumber + " @ " + numberX +"," + numberY);
        r.x -= NUMBERS_WIDTH;
        
    }

    public int getPreviousLineCount0() {
        int lineCount = 0;
        View parent = this.getParent();
        int count = parent.getViewCount();
        for (int i = 0; i < count; i++) {
            if (parent.getView(i) == this) {
                break;
            }
            else {
                lineCount += parent.getView(i).getViewCount();
            }
        }
        return lineCount;
    }
    static WeakHashMap<Element, Integer> elementLineNumberCache = new WeakHashMap<Element, Integer>();
    public Integer getLineNumber() {
    
        Element element = this.getElement();
    	Integer result = elementLineNumberCache.get(element);
    	if (result != null) return result;
        Element parent = element.getParentElement();
        int count = parent.getElementCount();
        for (int i = 0; i < count; i++) {
        	elementLineNumberCache.put(parent.getElement(i), i+1);
        }
       result = elementLineNumberCache.get(element);
    	if (result != null) return result;
    	return -1;

    }
}