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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JFileChooser;

import edu.umd.cs.findbugs.SystemProperties;
/**
 * All FileChoosers are FBFileChoosers so font size will work
 * @author Kristin
 *
 */
public class FBFileChooser extends JFileChooser {

	public FBFileChooser(){
		super();
		assert java.awt.EventQueue.isDispatchThread();
		this.setCurrentDirectory(GUISaveState.getInstance().getStarterDirectoryForLoadBugs());
	}

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
		setFontSize(Driver.getFontSize());

	}

	private static void workAroundJFileChooserBug() {
		//Travis McLeskey
		// http://www.mcleskey.org/bugs.html
		try {
			Object o = javax.swing.UIManager.getBorder( "TableHeader.cellBorder" );
			Method m = o.getClass().getMethod( "setHorizontalShift",
					new Class[] { int.class } );
			m.invoke( o, 0 );
		}
		catch ( NoSuchMethodException e ) { assert false; }
		catch ( InvocationTargetException e ) { assert false; }
		catch ( IllegalAccessException e ) { assert false; }
	}

	@Override
	public int showOpenDialog(Component parent)
	{
		 assert java.awt.EventQueue.isDispatchThread();
		int x=super.showOpenDialog(parent);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();

		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());

		return x;
	}

	@Override
	public int showSaveDialog(Component parent){
		 assert java.awt.EventQueue.isDispatchThread();
		int x=super.showSaveDialog(parent);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();

		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());

		return x;
	}

	@Override
	public int showDialog(Component parent, String approveButtonText){
		 assert java.awt.EventQueue.isDispatchThread();
		int x=super.showDialog(parent, approveButtonText);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();

		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());

		return x;
	}


}
