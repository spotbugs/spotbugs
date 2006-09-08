package edu.umd.cs.findbugs.gui2;

import javax.swing.JOptionPane;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * Does wonderful things like let you quit with apple Q, actually thats all it does
 * also could be used to pop up an about frame, though it isn't right now
 * @author Dan
 *
 */
public class MacFileMenuHandler extends ApplicationAdapter{
	public void handleQuit(ApplicationEvent e)
	{
		e.setHandled(MainFrame.getInstance().callOnClose());
	}
	
	public void handleAbout(ApplicationEvent e)
	{
		//TODO The About Frame Goes here
		e.setHandled(true);
	}
}
