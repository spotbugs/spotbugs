package edu.umd.cs.findbugs.gui2;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * All Dialogs are FBDialogs so font size will work.
 * @author Kristin
 *
 */
@SuppressWarnings("serial")
public class FBDialog extends JDialog {
	
	public FBDialog(){
		super(MainFrame.getInstance());
	}
	
	public FBDialog(Frame f){
		super(f);
	}
	
	public FBDialog(Dialog d){
		super(d);
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
	
	public void addNotify(){
		super.addNotify();
		
		setFontSize(Driver.getFontSize());
	}
	
}