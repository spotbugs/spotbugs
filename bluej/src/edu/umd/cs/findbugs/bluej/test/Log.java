package edu.umd.cs.findbugs.bluej.test;

import java.util.ArrayList;
import javax.swing.JOptionPane;

public class Log {
	final static boolean DEBUG = true;
	static ArrayList<Exception> exceptionList = new ArrayList<Exception>();
	
	/**
	 * Takes the exception and dependi on DEBUG either posts onto a dialog
	 * box or will record it for later use.
	 * @param e
	 */
	static void recordBug(Exception e){
		if(DEBUG){
			StringBuilder msg = new StringBuilder();
			msg.append(e.getClass().getName() + ": " + e.getMessage() + "\n");
			for (StackTraceElement i : e.getStackTrace())
				msg.append(i + "\n");
			JOptionPane.showMessageDialog(null, msg);
		}
		else{
			exceptionList.add(e);
		}
	}
}
