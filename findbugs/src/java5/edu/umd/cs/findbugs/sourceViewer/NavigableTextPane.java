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

import java.awt.Container;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.PriorityQueue;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyledDocument;

import edu.umd.cs.findbugs.gui2.MainFrame;

/**
 * @author tuc
 */
public class NavigableTextPane extends JTextPane {

	public NavigableTextPane() {
	}

	public NavigableTextPane(StyledDocument doc) {
		super(doc);
	}

	/** return the height of the parent (which is presumably a JViewport).
	 *  If there is no parent, return this.getHeight(). */
	private int parentHeight() {
		Container parent = getParent();
		if (parent != null) return parent.getHeight();
		return getHeight(); // entire pane height, may be huge
	}

	public int getLineOffset(int line) throws BadLocationException {
		return lineToOffset(line);
	}

	private int lineToOffset(int line) throws BadLocationException {
		Document d = getDocument();
		try {
			Element element = d.getDefaultRootElement().getElement(line-1);
			if (element == null) throw new BadLocationException("line "+line+" does not exist", -line);
			return element.getStartOffset();
		}
		catch (ArrayIndexOutOfBoundsException aioobe) {
			BadLocationException ble = new BadLocationException("line "+line+" does not exist", -line);
			ble.initCause(aioobe);
			throw ble;
		}
	}

	private int offsetToY(int offset) throws BadLocationException {
		Rectangle r = modelToView(offset);
		return r.y;
	}

	private int lineToY(int line) throws BadLocationException {
		return offsetToY( lineToOffset(line) );
	}

	private void scrollYToVisibleImpl(int y, int margin) {
		final Rectangle r = new Rectangle(0, y-margin, 4, 2*margin);

		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					scrollRectToVisible(r);
				}
			});
	}

	private void scrollLineToVisibleImpl(int line, int margin) {
		try {
			int y = lineToY(line);
			scrollYToVisibleImpl(y, margin);
		} catch (BadLocationException ble) {
			if (MainFrame.DEBUG) ble.printStackTrace();
		}
	}

	/** scroll the specified line into view, with a margin of 'margin' pixels above and below */
	public void scrollLineToVisible(int line, int margin) {
		int maxMargin = (parentHeight() - 20) / 2;
		if (margin > maxMargin) margin = Math.max(0, maxMargin);
		scrollLineToVisibleImpl(line, margin);
	}

	/** scroll the specified line into the middle third of the view */
	public void scrollLineToVisible(int line) {
		int margin = parentHeight() / 3;
		scrollLineToVisibleImpl(line, margin);
	}

	/** scroll the specified primary lines into view, along
	 *  with as many of the other lines as is convenient */
	public void scrollLinesToVisible(int startLine, int endLine, Collection<Integer> otherLines) {
		int startY, endY;
		try {
			startY = lineToY(startLine);
		} catch (BadLocationException ble) {
			if (MainFrame.DEBUG) ble.printStackTrace();
			return; // give up
		}
		try {
			endY = lineToY(endLine);
		} catch (BadLocationException ble) {
			endY = startY; // better than nothing
		}

		int max = parentHeight() - 0;
		if (endY-startY > max) {
			endY = startY+max;
		}
		else if (otherLines!=null && otherLines.size() > 0) {
			int origin = startY + endY / 2;
			PriorityQueue<Integer> pq = new PriorityQueue<Integer>(otherLines.size(), new DistanceComparator(origin));
			for (int line : otherLines) {
				int otherY;
				try {
					otherY = lineToY(line);
				} catch (BadLocationException ble) {
					continue; // give up on this one
				}
				pq.add(otherY);
			}

			while ( !pq.isEmpty() ) {
				int y = pq.remove();
				int lo = Math.min(startY, y);
				int hi = Math.max(endY, y);
				if (hi-lo > max) break;
				else {
					startY = lo;
					endY = hi;
				}
			}
		}
		scrollYToVisibleImpl((startY+endY)/2, max/2);
	}

	public static class DistanceComparator implements Comparator<Integer>, Serializable {
		private final int origin;
		public DistanceComparator(int origin) {
			this.origin = origin;
		}
		/* Returns a negative integer, zero, or a positive integer as
		 * the first argument is farther from, equadistant, or closer
		 * to (respectively) the origin.
		 * This sounds backwards, but this way closer values get a
		 * higher priority in the priority queue. */
		public int compare(Integer a, Integer b) {
			return Math.abs(b-origin) - Math.abs(a-origin);
		}
	}

}
