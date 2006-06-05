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

public class ClassLister implements CompileListener
{

	long startTime = -1;
	private BlueJ bluej;
	
	public ClassLister(BlueJ bluej)
	{
		this.bluej = bluej;
	}
	
	public void compileStarted(CompileEvent evt)
	{
		startTime = new Date().getTime();
	}

	public void compileError(CompileEvent evt)
	{
		// TODO Auto-generated method stub

	}

	public void compileWarning(CompileEvent evt)
	{
		// TODO Auto-generated method stub

	}

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

	public void compileFailed(CompileEvent evt)
	{
		// TODO Auto-generated method stub

	}

}
