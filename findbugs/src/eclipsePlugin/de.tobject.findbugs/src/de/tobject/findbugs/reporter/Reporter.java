/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.SourceType;
import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.Project;
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
	public static boolean DEBUG = false;

	private static final int MAX_CLASS_NAME_LENGTH = 30;
	
	private IProject project;
	private Project findBugsProject;
	
	/** determines how often the progress monitor gets updated */
	private static int MONITOR_INTERVAL = 1;
	private IProgressMonitor monitor;
	
	private Collection bugList;
	
	private boolean workStarted;
	
	public Reporter(IProject project, IProgressMonitor monitor, Project findBugsProject) {
		super();
		this.monitor = monitor;
		this.project = project;
		this.findBugsProject = findBugsProject;
		this.project = project;
	}
	
	/* (non-Javadoc)
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
			System.out.println("BUG in class: " //$NON-NLS-1$
			+ packageName + "." //$NON-NLS-1$
			+ className + ": \n\t" //$NON-NLS-1$
			+ bug.getMessage() + " / Annotation: " //$NON-NLS-1$
			+ bug.getAnnotationText() + " / Source Line: " //$NON-NLS-1$
			+ bug.getPrimarySourceLineAnnotation());
		}
		
		IResource resource = null;
		try {
			resource = getUnderlyingResource(bug);
		}
		catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (resource != null) {
			// default - first class line
		
			int startLine = 1; 
			if (bug.getPrimarySourceLineAnnotation() != null) 
				  startLine = bug.getPrimarySourceLineAnnotation().getStartLine();
			/* TODO: DHH - Eclipse can help us find the line number for fields. //
			 * Need a way to distinguish bugs where the field is the // primary item
			 * of interest.
			 */
			 
			if (DEBUG) {
				System.out.println("Creating marker for " //$NON-NLS-1$
				+ resource.getLocation() + ": line " //$NON-NLS-1$
				+ startLine);
			}
			try {
				this.project.getWorkspace().run(
					new MarkerReporter(bug, resource, startLine), /* Progress window*/ null);
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	

	private IResource getUnderlyingResource(BugInstance bug)
		throws JavaModelException {
		SourceLineAnnotation primarySourceLineAnnotation = bug.getPrimarySourceLineAnnotation();
		PackageMemberAnnotation packageAnnotation = null;
		String packageName = null;
		String qualifiedClassName = null;
		if (primarySourceLineAnnotation == null) {
			packageAnnotation = bug.getPrimaryClass();
			if (packageAnnotation != null) {
				packageName = packageAnnotation.getPackageName();
				qualifiedClassName = packageAnnotation.getClassName();
			}
		}
		else {
			packageName = primarySourceLineAnnotation.getPackageName();
			qualifiedClassName = primarySourceLineAnnotation.getClassName();
		}
		if (qualifiedClassName == null) {
			return null;
		}
		
		if (DEBUG) {
			System.out.println("Looking up class: " //$NON-NLS-1$
			+ packageName + ", " //$NON-NLS-1$
			+ qualifiedClassName);
		}
		int lastDollar =
			Math.max(
				qualifiedClassName.lastIndexOf('$'),
				qualifiedClassName.lastIndexOf('+'));
		boolean isInnerClass = lastDollar > 0;
		//        boolean isAnonInnerClass = Character.isDigit(qualifiedClassName
		//            .charAt(lastDollar + 1));
		IType type = null;
		if (primarySourceLineAnnotation == null && isInnerClass) {
			// cut the useless number value
			String innerName = qualifiedClassName.substring(lastDollar + 1);
			String shortQualifiedClassName =
				qualifiedClassName.substring(0, lastDollar);
			type = getJavaProject().findType(shortQualifiedClassName);
			completeInnerClassInfo(qualifiedClassName, innerName, type, bug);
		}
		else {
			type = getJavaProject().findType(qualifiedClassName);
		}
		if (type != null) {
			return type.getUnderlyingResource();
		}
		return null;
	}
	
	/**
	 * @param innerName
	 * @param type
	 * @param res
	 * @return
	 * @throws JavaModelException
	 */
	private void completeInnerClassInfo(String qualifiedClassName, String innerName, IType type, BugInstance bug) throws JavaModelException {
		int lineNbr = findChildSourceLine(type, innerName);
		// should be always first line, if not found
		lineNbr = lineNbr <= 0 ? 1 : lineNbr;
		String sourceFileStr = ""; //$NON-NLS-1$
		IResource res = type.getUnderlyingResource();
		if (res != null) {
			sourceFileStr = res.getRawLocation().toOSString();
		}
		bug.addSourceLine(
			new SourceLineAnnotation(
				qualifiedClassName,
				sourceFileStr,
				lineNbr,
				lineNbr,
				0,
				0));
	}
	
	/**
	 * @param source
	 * @return
	 * @throws JavaModelException
	 */
	private int getLineStart(SourceType source) throws JavaModelException {
		IOpenable op = source.getOpenable();
		if (op instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) op;
			ISourceRange range = source.getSourceRange();
			IScanner scanner =
				ToolFactory.createScanner(false, false, false, true);
			scanner.setSource(cu.getContents());
			try {
				while (scanner.getNextToken() != ITerminalSymbols.TokenNameEOF) {
					// do nothing, just wait for the end of stream
				}
			}
			catch (InvalidInputException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return scanner.getLineNumber(range.getOffset());
		}
		// start line of enclosing type
		return 1;
	}
	
	private int findChildSourceLine(IJavaElement javaElement, String name) throws JavaModelException {
		if (!Character.isDigit(name.charAt(0))) {
			return findInnerClassSourceLine(javaElement, name);
		}
		try {
			int innerNumber = Integer.parseInt(name);
			return findInnerAnonimousClassSourceLine(javaElement, innerNumber);
		}
		catch (NumberFormatException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return -1;
	}
	
	/**
	 * @param javaElement
	 * @return
	 */
	private int findInnerAnonimousClassSourceLine(IJavaElement javaElement, int innerNumber) {
		IOpenable op = javaElement.getOpenable();
		if (!(op instanceof CompilationUnit)) {
			return -1;
		}
		CompilationUnit cu = (CompilationUnit) op;
		IScanner scanner = ToolFactory.createScanner(false, false, false, true);
		scanner.setSource(cu.getContents());
		try {
			int innerCount = 0;
			int tokenID = scanner.getNextToken();
			while (tokenID != ITerminalSymbols.TokenNameEOF) {
				if (tokenID != ITerminalSymbols.TokenNamenew) {
					tokenID = scanner.getNextToken();
					continue;
				}
				int startClassPos = scanner.getCurrentTokenStartPosition();
				tokenID = scanner.getNextToken();
				if (tokenID != ITerminalSymbols.TokenNameIdentifier) {
					continue;
				}
				tokenID = scanner.getNextToken();
				if (tokenID != ITerminalSymbols.TokenNameLPAREN) {
					continue;
				}
				tokenID = scanner.getNextToken();
				if (tokenID != ITerminalSymbols.TokenNameRPAREN) {
					continue;
				}
				tokenID = scanner.getNextToken();
				if (tokenID != ITerminalSymbols.TokenNameLBRACE) {
					continue;
				}
				tokenID = scanner.getNextToken();
				innerCount++;
				if (innerCount == innerNumber) {
					return scanner.getLineNumber(startClassPos);
				}
			}
		}
		catch (InvalidInputException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	
	/**
	 * @param javaElement
	 * @param name
	 * @param elemName
	 * @return
	 * @throws JavaModelException
	 */
	private int findInnerClassSourceLine(IJavaElement javaElement, String name) throws JavaModelException {
		String elemName = javaElement.getElementName();
		if (name.equals(elemName)) {
			if (javaElement instanceof SourceType) {
				SourceType source = (SourceType) javaElement;
				return getLineStart(source);
			}
		}
		if (javaElement instanceof IParent) {
			IJavaElement[] children = ((IParent) javaElement).getChildren();
			for (int i = 0; i < children.length; i++) {
				// recursive call
				int line = findInnerClassSourceLine(children[i], name);
				if (line > 0) {
					return line;
				}
			}
		}
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#beginReport()
	 */
	public void beginReport() {
		getBugList().clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#reportLine(java.lang.String)
	 */
	public void reportLine(String arg0) {
		if (DEBUG) {
			System.out.println("reportline: " + arg0); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.AbstractBugReporter#endReport()
	 */
	public void endReport() {
		if (DEBUG) {
			System.out.println("endreport"); //$NON-NLS-1$
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.BugReporter#finish()
	 */
	public void finish() {
		if (DEBUG) {
			System.out.println("Finish: Found " + getBugList().size() + " bugs."); //$NON-NLS-1$//$NON-NLS-2$
		}
	}
	
	/**
	 * Returns the list of bugs found in this project. If the list has not been
	 * initialized yet, this will be done before returning.
	 * 
	 * @return The collection that hold the bugs found in this project.
	 */
	public Collection getBugList() {
		if (bugList == null) {
			bugList = new ArrayList();
		}
		return bugList;
	}
	
	/**
	 * Returns the current project.
	 * 
	 * @return The current project.
	 */
	public IProject getProject() {
		return project;
	}
	
	/**
	 * Returns the current project cast into a Java project.
	 * 
	 * @return The current project as a Java project.
	 */
	public IJavaProject getJavaProject() {
		IProject project = getProject();
		return JavaCore.create(project);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.ClassObserver#observeClass(org.apache.bcel.classfile.JavaClass)
	 */
	public void observeClass(JavaClass clazz) {
		if (DEBUG) {
			System.out.println("Observing class: " + clazz.getClassName()); //$NON-NLS-1$
		}
		if (monitor == null) {
			return;
		}
		if (!workStarted) {
			workStarted = true;
			int filesNumber = findBugsProject.getFileCount();
			if (!(monitor instanceof SubProgressMonitor)) {
				monitor.beginTask("Performing bug checking...", filesNumber);
			}
		}
		if (monitor.isCanceled()) {
			// causes break in FindBugs main loop
			Thread.currentThread().interrupt();
		}
		int bugsNbr = getBugList().size();
		monitor.setTaskName(
			"Bug checking... (found "
				+ bugsNbr
				+ ", check in "
				+ getAbbreviatedClassName(clazz)
				+ ")");
		monitor.worked(MONITOR_INTERVAL);
	}
	
	/**
	 * Returns an abreviated version of the class name.
	 * 
	 * @param clazz A Java class.
	 * @return
	 */
	private String getAbbreviatedClassName(JavaClass clazz) {
		String name = clazz.getClassName();
		if (name.length() > MAX_CLASS_NAME_LENGTH) {
			int startCutIdx = name.length() - MAX_CLASS_NAME_LENGTH;
			int pointIdx = name.indexOf(".", startCutIdx); //$NON-NLS-1$
			if (pointIdx > startCutIdx) {
				startCutIdx = pointIdx;
			}
			name = ".." + name.substring(startCutIdx); //$NON-NLS-1$
		}
		return name;
	}
}