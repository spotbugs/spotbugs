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

package edu.umd.cs.findbugs.gui2;

import java.awt.Component;
import java.awt.Container;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
@SuppressWarnings("serial")
public class FBFrame extends JFrame {

	/**
	 * Sets size of font
	 * @param size
	 */
	protected void setFontSize(float size){
		setFont(this.getFont().deriveFont(size));
		
		setFontSizeHelper(this.getComponents(), size);		
	}
	
	/*
	 * Helps above method, runs through all components recursively.
	 */
	protected void setFontSizeHelper(Component[] comps, float size){
		if(comps.length <= 0)
			return;
		
		for(Component comp : comps){
			comp.setFont(comp.getFont().deriveFont(size));
			if(comp instanceof Container)
				setFontSizeHelper(((Container)comp).getComponents(), size);
		}
	}
	
	@Override
	public void addNotify(){
		super.addNotify();
		
		try {
			setIconImage(ImageIO.read(MainFrame.class.getResource("smallBuggy.png")));
		} catch (IOException e) {
			Debug.println(e);
		}
		
		setFontSize(Driver.getFontSize());
	}
}
