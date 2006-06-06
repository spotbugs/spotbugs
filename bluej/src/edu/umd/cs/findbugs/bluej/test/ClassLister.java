package edu.umd.cs.findbugs.bluej.test;
import java.io.File;
import java.io.FileFilter;
import java.util.Date;

import javax.swing.JOptionPane;

import bluej.extensions.BPackage;
import bluej.extensions.BProject;
import bluej.extensions.BlueJ;
import bluej.extensions.ExtensionException;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

/**
 * Waits for compile events and, when a compile finishes, pops
 * a list of all the classes modified in the time since it began.
 * @author Reuven Lazarus
 */
public class ClassLister implements CompileListener
{

	private long startTime = -1;
	private BlueJ bluej;

	public ClassLister(BlueJ bluej)
	{
		this.bluej = bluej;
	}
	
	/**
	 * When a compile starts, make a note of the time.
	 */
	public void compileStarted(CompileEvent evt)
	{
		startTime = new Date().getTime();
	}

	/**
	 * When the compile has completed successfully, generate a
	 * list of all the .class files modified since it started,
	 * and pop a dialog box.
	 */
	public void compileSucceeded(CompileEvent evt)
	{
		StringBuilder msg = new StringBuilder();
		
		FileFilter filter = new FileFilter()
		{
			public boolean accept(File f)
			{
				return f.getName().endsWith(".class") && f.lastModified() >= startTime;
			}	
		};
		
		try
		{
			for (BProject i : bluej.getOpenProjects())
				for (BPackage j : i.getPackages())
					for (File k : j.getDir().listFiles(filter))
						msg.append(k.getName().substring(0, k.getName().length() - ".class".length()) + "\n");
		}
		// Checked exceptions that can't arise in this code
		catch (ExtensionException notGonnaHappen) {}
		
		JOptionPane.showMessageDialog(null, msg);
	}

	/** NOOP, but required by CompileListener interface */
	public void compileError(CompileEvent evt) {}
	
	/** NOOP, but required by CompileListener interface */
	public void compileWarning(CompileEvent evt) {}
	
	/** NOOP, but required by CompileListener interface */
	public void compileFailed(CompileEvent evt) {}

}
