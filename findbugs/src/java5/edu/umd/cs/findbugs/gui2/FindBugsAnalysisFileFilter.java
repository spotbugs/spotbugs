/**
 * 
 */
package edu.umd.cs.findbugs.gui2;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public final class FindBugsAnalysisFileFilter extends FileFilter {
	
	public static final  FindBugsAnalysisFileFilter INSTANCE = new FindBugsAnalysisFileFilter();
	@Override
	public boolean accept(File arg0) {
		return arg0.getName().endsWith(".xml");
	}

	@Override
	public String getDescription() {
		return "FindBugs analysis results";
	}
}