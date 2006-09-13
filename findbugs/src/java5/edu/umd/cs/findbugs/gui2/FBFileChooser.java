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
	
	public void addNotify(){
		super.addNotify();
		setFontSize(Driver.getFontSize());
		
	}

	private static void workAroundJFileChooserBug() {
		//Travis McLeskey
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
	
	public int showOpenDialog(Component parent)
	{
		int x=super.showOpenDialog(parent);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();
		
		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());
		
		return x;
	}

	public int showSaveDialog(Component parent){
		int x=super.showSaveDialog(parent);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();
		
		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());
		
		return x;
	}

	public int showDialog(Component parent, String approveButtonText){
		int x=super.showDialog(parent, approveButtonText);
		if (SystemProperties.getProperty("os.name").startsWith("Mac"))
			workAroundJFileChooserBug();
		
		GUISaveState.getInstance().setStarterDirectoryForLoadBugs(getCurrentDirectory());
		
		return x;
	}

	
}