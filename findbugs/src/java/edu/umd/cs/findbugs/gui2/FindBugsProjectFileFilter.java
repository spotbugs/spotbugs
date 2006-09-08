/**
 * 
 */
package edu.umd.cs.findbugs.gui2;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public final class FindBugsProjectFileFilter extends FileFilter {
	
	public static final  FindBugsProjectFileFilter INSTANCE = new FindBugsProjectFileFilter();
	
	
	@Override
	public boolean accept(File arg0) {
		return arg0.getName().endsWith(".fb");
	}

	@Override
	public String getDescription() {
		return "FindBugs project files";
	}
}