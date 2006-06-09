package edu.umd.cs.findbugs.bluej;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JOptionPane;

/**
 * Used to log any exceptions that are thrown. Can be used to log or debug.
 * If debugging can use to pop a JOptionPane with the stackTrace.
 * @author Kristin Stephens
 *
 */
public class Log
{
	//If true will pop a window with the stackTrace in it.
	//If false will simply log onto logFile.
	private final static boolean DEBUG = true;
	private static File logFile;

	/**
	 * Set where Log will log any exceptions.
	 * @param logFile
	 */
	static void setPath(File logFile)
	{
		Log.logFile = logFile;
	}

	/**
	 * Takes the exception and dependi on DEBUG either posts onto a dialog box
	 * or will record it for later use.
	 * 
	 * @param e
	 */
	static void recordBug(Exception e)
	{
		StringBuilder msg = new StringBuilder();
		msg.append(e.getClass().getName() + ": " + e.getMessage() + "\n");
		for (StackTraceElement i : e.getStackTrace())
			msg.append(i + "\n");

		if (DEBUG)
			JOptionPane.showMessageDialog(null, msg);
		else
		{
			try
			{
				logFile.createNewFile();
				PrintWriter pw = new PrintWriter(new FileOutputStream(logFile));
				pw.println(msg);
				pw.close();
			}
			catch (IOException ohWell) {}
		}
	}
}
