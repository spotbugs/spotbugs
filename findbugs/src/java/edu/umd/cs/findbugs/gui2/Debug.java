package edu.umd.cs.findbugs.gui2;

import edu.umd.cs.findbugs.BugInstance;

/**
 * For debugging purposes only... Make sure DEBUG is set to false before you release a new version.
 * @author Dan
 *
 */
public class Debug {
	public static void println(Object s)
	{
		if (MainFrame.DEBUG)
			System.out.println(s);
	}
	
	public static void println(Exception e){
		if(MainFrame.DEBUG)
			e.printStackTrace();
	}
	
	public static void main(String[] args)
	{

	}
}
