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
