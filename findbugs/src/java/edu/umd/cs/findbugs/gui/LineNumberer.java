/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	 See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA	 02111-1307	 USA
 */
package edu.umd.cs.findbugs.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.JTextArea;

public class LineNumberer extends JComponent
{
	public static final int PAD = 10;
	public static final String PROTOTYPE = "00000";
	
	private JTextArea textArea;
	private FontMetrics fm;
	
	public LineNumberer(JTextArea ta) {
		setFont( ta.getFont() );
		textArea = ta;
		setForeground( Color.BLUE );
		
		fm = this.getFontMetrics(ta.getFont());
		setWidths();
	}
	
	public void setFont(Font font)
	{
		//ignore
	}
	
	private void setWidths() {
		int width = fm.stringWidth( PROTOTYPE );
		Dimension d = getPreferredSize();
		d.setSize(PAD + width, Integer.MAX_VALUE);
		setPreferredSize( d );
		setSize( d );
	}


	public void paintComponent(Graphics g)
	{
		int lineHeight = fm.getHeight();
		int startOffset = textArea.getInsets().top + fm.getAscent();

		Rectangle clip = g.getClipBounds();

		g.setColor( getBackground() );
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		g.setColor( getForeground() );
		int beginLineNumber = (clip.y / lineHeight) + 1;
		int endLineNumber = beginLineNumber + (clip.height / lineHeight);

		int y = (clip.y / lineHeight) * lineHeight + startOffset;

		for (int i = beginLineNumber; i <= endLineNumber; i++)
		{
			String ln = String.valueOf(i);
			int width = fm.stringWidth( ln );
			int rowWidth = getSize().width;
			g.drawString(ln, rowWidth - width - PAD, y);
			y += lineHeight;
		}
	}
}
