/* 
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003, Peter Friese
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
 
package de.tobject.findbugs.reporter;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.bcel.classfile.JavaClass;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.SourceLineAnnotation;

/**
 * The <code>Reporter</code> is a class that is called by the FindBugs engine
 * in order to record and report bugs that have been found. This implementation
 * displays the bugs found as tasks in the task view.
 * 
 * @author Peter Friese
 * @version 1.0
 * @since 28.07.2003
 */
public class Reporter extends AbstractBugReporter {
	
	/** Controls debugging for the reporter */
	public static boolean DEBUG;

	private IProject project;

	/** determines how often the progress monitor gets updated */
	private static int MONITOR_INTERVAL = 1;		
	
	private IProgressMonitor monitor;

	private Collection bugList = null;
	
	public Reporter(IProject project, IProgressMonitor monitor) {
		super();
		this.monitor = monitor;
		this.project = project;
	}
	
	/**
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#doReportBug(edu.umd.cs.findbugs.BugInstance)
	 */
	protected void doReportBug(BugInstance bug) {
		getBugList().add(bug);
		
		String className = null;
		String packageName = null;
		if (bug.getPrimaryClass() != null) {
			className = bug.getPrimaryClass().getClassName();
			packageName = bug.getPrimaryClass().getPackageName();
		}
		
		if (DEBUG) {
			System.out.println("BUG in class: " + packageName + "." + className + ": " + bug.getMessage() + " / Annotation: " + bug.getAnnotationText() + " / Source Line: " + bug.getPrimarySourceLineAnnotation());
		}		
		
		IResource resource = getUnderlyingResource(bug);
		if (resource != null) {
			
			int startLine = bug.getPrimarySourceLineAnnotation().getStartLine();
			
			if (DEBUG) {
				System.out.println("Creating marker for " + resource.getLocation() + ": line " + startLine);
			}
			try {
//				HashMap attributes = new HashMap();
//				IJavaElement element = JavaCore.create(resource);
//				JavaCore.addJavaElementMarkerAttributes(attributes, element);
//				attributes.put(IMarker.LINE_NUMBER, new Integer(startLine));
//				attributes.put(IMarker.MESSAGE, bug.getMessage());
//				attributes.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_WARNING));
//				IMarker marker = resource.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
//				marker.setAttributes(attributes);
				
				IMarker marker = resource.createMarker(FindBugsMarker.NAME);
				marker.setAttribute(IMarker.LINE_NUMBER, startLine);
				
				BugPattern pattern = I18N.instance().lookupBugPattern(bug.getType());
				if (pattern != null){
					marker.setAttribute("shortDescription",pattern.getShortDescription());
					marker.setAttribute("detailText",pattern.getDetailText());
				}				
				marker.setAttribute(IMarker.MESSAGE, bug.getMessage());
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);				
				
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
			
		}		
		
		advanceProgressMonitor();
	}
	
	private IResource getUnderlyingResource(BugInstance bug) {
		try {
			SourceLineAnnotation primarySourceLineAnnotation = bug.getPrimarySourceLineAnnotation();
			if (primarySourceLineAnnotation != null) {
				String packageName = primarySourceLineAnnotation.getPackageName();
				String qualifiedClassName = primarySourceLineAnnotation.getClassName();

				if (DEBUG) {
					System.out.println("Looking up class: " + packageName + ", " + qualifiedClassName);
				}
				IType type = getJavaProject().findType(qualifiedClassName);
				if (type != null) {
					IResource res = type.getUnderlyingResource();
					if (res != null) {
						return res;
					}
				}
			}
		}
		catch (JavaModelException e) {
			e.printStackTrace();
		}
		return null;		
	}	
	
	private void advanceProgressMonitor() {
		// advance progress monitor
		if (monitor != null && getBugList().size() % MONITOR_INTERVAL == 0) {
			monitor.worked(MONITOR_INTERVAL);
			monitor.subTask("Performing bug checking.");
//			if (monitor.isCanceled()) {
//				break;
//			}
		}
	}

	/**
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#beginReport()
	 */
	public void beginReport() {
		getBugList().clear();
	}

	/**
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#reportLine(java.lang.String)
	 */
	public void reportLine(String arg0) {
		if (DEBUG) {
			System.out.println("reportline: " + arg0);
		}
	}

	/**
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#endReport()
	 */
	public void endReport() {
		if (DEBUG) {
			System.out.println("endreport");
		}
	}

	/**
	 * @see edu.umd.cs.findbugs.BugReporter#finish()
	 */
	public void finish() {
		if (DEBUG) {
			System.out.println("Finish: Found " + getBugList().size() + " bugs.");
		}
	}

	/**
	 * Returns the list of bugs found in this project. If the list has not been 
	 * initialized yet, this will be done before returning.
	 * @return The collection that hold the bugs found in this project.
	 */
	public Collection getBugList() {
		if (bugList == null) {
			bugList = new ArrayList();
		}
		return bugList;
	}

	/**
	 * @return
	 */
	public IProject getProject() {
		return project;
	}
	
	public IJavaProject getJavaProject() {
		IProject project = getProject();
		return (IJavaProject) JavaCore.create(project);
	}

	/**
	 * @see edu.umd.cs.findbugs.ba.ClassObserver#observeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void observeClass(JavaClass clazz) {
		System.out.println("Observing class: " +  clazz.getClassName() );		
	}

}
