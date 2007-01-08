package edu.umd.cs.findbugs.bluej;

//
import bluej.extensions.BlueJ;
import bluej.extensions.event.CompileEvent;
import bluej.extensions.event.CompileListener;

public class FindBugsCompileListener implements CompileListener {

	private int numClasses;
	RegularMenuBuilder menu;
	BlueJ bluej;
	
	public FindBugsCompileListener(BlueJ bluej, int numClasses, RegularMenuBuilder menu){
		this.numClasses = numClasses;
		this.menu = menu;
		this.bluej = bluej;
	}
	
	public void compileStarted(CompileEvent evt) {
	}

	public void compileError(CompileEvent arg0) {
		
	}

	public void compileWarning(CompileEvent arg0) {
		
	}

	public void compileSucceeded(CompileEvent arg0) {
		numClasses--;
		
		if(numClasses <= 0){
			try {
				menu.getAllClassesAndRun();
			} catch (Exception e) {
				Log.recordBug(e);
			}
			bluej.removeCompileListener(this);
		}
	}

	public void compileFailed(CompileEvent arg0) {
		bluej.removeCompileListener(this);
	}
}