/*
 * FindBugs Eclipse Plug-in.
 * Copyright (C) 2003 - 2004, Peter Friese
 * Copyright (C) 2004-2005, University of Maryland
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

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.internal.core.CompilationUnit;
import org.eclipse.jdt.internal.core.SourceType;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * Utility methods for converting FindBugs BugInstance objects
 * into Eclipse markers.
 *
 * @author Peter Friese
 * @author David Hovemeyer
 */
public abstract class MarkerUtil {

	/**
	 * Create an Eclipse marker for given BugInstance.
	 *
	 * @param bug     the BugInstance
	 * @param project the project
	 */
	public static void createMarker(BugInstance bug, IProject project) {
		String className = null;
		String packageName = null;
		if (bug.getPrimaryClass() != null) {
			className = bug.getPrimaryClass().getClassName();
			packageName = bug.getPrimaryClass().getPackageName();
		}
		if (Reporter.DEBUG) {
			System.out.println("BUG in class: " //$NON-NLS-1$
			+ packageName + "." //$NON-NLS-1$
			+ className + ": \n\t" //$NON-NLS-1$
			+ bug.getMessage() + " / Annotation: " //$NON-NLS-1$
			+ bug.getAnnotationText() + " / Source Line: " //$NON-NLS-1$
			+ bug.getPrimarySourceLineAnnotation());
		}

		IResource resource = null;
		try {
			resource = getUnderlyingResource(bug, project);
		}
		catch (JavaModelException e1) {
			FindbugsPlugin.getDefault().logException(
					e1, "Could not find class resource for FindBugs warning");
		}
		if (resource != null) {
			// default - first class line

			int startLine = 1;
			if (bug.getPrimarySourceLineAnnotation() != null)
				  startLine = bug.getPrimarySourceLineAnnotation().getStartLine();
			/* TODO: DHH - Eclipse can help us find the line number for fields.
			 * Need a way to distinguish bugs where the field is the primary item
			 * of interest.
			 */

			if (Reporter.DEBUG) {
				System.out.println("Creating marker for " //$NON-NLS-1$
				+ resource.getLocation() + ": line " //$NON-NLS-1$
				+ startLine);
			}
			try {
				project.getWorkspace().run(
					new MarkerReporter(bug, resource, startLine), /* Progress window*/ null);
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get the underlying resource (Java class) for given BugInstance.
	 *
	 * @param bug     the BugInstance
	 * @param project the project
	 * @return the IResource representing the Java class
	 * @throws JavaModelException
	 */
	public static IResource getUnderlyingResource(BugInstance bug, IProject project)
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

		if (Reporter.DEBUG) {
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
		if (isInnerClass) {
			// cut the useless number value
			String innerName = qualifiedClassName.substring(lastDollar + 1);
			String shortQualifiedClassName =
				qualifiedClassName.substring(0, lastDollar);
			type = Reporter.getJavaProject(project).findType(shortQualifiedClassName);
			completeInnerClassInfo(qualifiedClassName, innerName, type, bug);
		}
		else {
			type = Reporter.getJavaProject(project).findType(qualifiedClassName);
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
	public static void completeInnerClassInfo(String qualifiedClassName, String innerName, IType type, BugInstance bug) throws JavaModelException {
		int lineNbr = findChildSourceLine(type, innerName);
		// should be always first line, if not found
		lineNbr = lineNbr <= 0 ? 1 : lineNbr;
		String sourceFileStr = ""; //$NON-NLS-1$
		IResource res = type==null ? null : type.getUnderlyingResource();
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
	public static int getLineStart(SourceType source) throws JavaModelException {
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
				FindbugsPlugin.getDefault().logException(e, "Could not find line number for Java type");
			}
			return scanner.getLineNumber(range.getOffset());
		}
		// start line of enclosing type
		return 1;
	}

	public static int findChildSourceLine(IJavaElement javaElement, String name) throws JavaModelException {
		if (javaElement == null) {
			//new Exception("trace: javaElement is null").printStackTrace();
			return -1;
		}
		if (!Character.isDigit(name.charAt(0))) {
			return findInnerClassSourceLine(javaElement, name);
		}
		try {
			int innerNumber = Integer.parseInt(name);
			return findInnerAnonymousClassSourceLine(javaElement, innerNumber);
		}
		catch (NumberFormatException e) {
			FindbugsPlugin.getDefault().logException(
					e, "Could not find source line information for class member");
		}
		return -1;
	}

	/**
	 * @param javaElement
	 * @return
	 */
	public static int findInnerAnonymousClassSourceLine(IJavaElement javaElement, int innerNumber) {
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
			FindbugsPlugin.getDefault().logException(e, "Error scanning for inner class start line");
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
	public static int findInnerClassSourceLine(IJavaElement javaElement, String name) throws JavaModelException {
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

	/**
	 * Remove all FindBugs problem markers for given project.
	 *
	 * @param project the project
	 * @throws CoreException
	 */
	public static void removeMarkers(IProject project) throws CoreException {
		// remove any markers added by our builder
		project.deleteMarkers(
			FindBugsMarker.NAME,
			true,
			IResource.DEPTH_INFINITE);
	}

	/**
	 * Given current active bug category set, minimum warning priority,
	 * and previous user classification, return whether or not a warning
	 * (bug instance) should be displayed using a marker.
	 *
	 * @param bugInstance    the warning
	 * @param filterSettings project filter settings
	 * @return true if the warning should be displayed, false if not
	 */
	public static boolean displayWarning(BugInstance bugInstance, ProjectFilterSettings filterSettings) {
		// Detector plugins need to be loaded for category filtering to work!
		DetectorFactoryCollection.instance();

		return filterSettings.displayWarning(bugInstance);
	}

	/**
	 * Attempt to redisplay FindBugs problem markers for
	 * given project.
	 *
	 * @param project the project
	 * @param shell   Shell the progress dialog should be tied to
	 */
	public static void redisplayMarkers(final IProject project, Shell shell) {
		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);


		try {
			progressDialog.run(false, false, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {

					try {
						// Get user preferences for project,
						// so we know what to diplay
						UserPreferences userPrefs = FindbugsPlugin.getUserPreferences(project);

						// Get the saved bug collection for the project
						SortedBugCollection bugCollection =
							FindbugsPlugin.getBugCollection(project, monitor);

						if (bugCollection != null) {
							// Remove old markers
							MarkerUtil.removeMarkers(project);

							// Display warnings
							for (Iterator i = bugCollection.iterator(); i.hasNext();) {
								BugInstance bugInstance = (BugInstance) i.next();
								if (displayWarning(bugInstance, userPrefs.getFilterSettings())) {
									MarkerUtil.createMarker(bugInstance, project);
								}
							}
						}

					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						// Multiple checked exception types caught here
						FindbugsPlugin.getDefault().logException(
								e, "Error redisplaying FindBugs warning markers");
					}
				}

			});
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			// Multiple checked exception types caught here
			FindbugsPlugin.getDefault().logException(
					e, "Error redisplaying FindBugs warning markers");
		}

	}


	/**
	 * Find the BugInstance associated with given FindBugs marker.
	 *
	 * @param marker a FindBugs marker
	 * @return the BugInstance associated with the marker,
	 *         or null if we can't find the BugInstance
	 */
	public static BugInstance findBugInstanceForMarker(IMarker marker) {
		IResource resource = marker.getResource();
		if (resource == null) {
			// Also shouldn't happen.
			FindbugsPlugin.getDefault().logError("No resource for warning marker");
			return null;
		}
		IProject project = resource.getProject();
		if (project == null) {
			// Also shouldn't happen.
			FindbugsPlugin.getDefault().logError("No project for warning marker");
			return null;
		}
		try {
			String markerType = marker.getType();
			//System.out.println("Marker type is " + markerType);

			if (!markerType.equals(FindBugsMarker.NAME)) {
				FindbugsPlugin.getDefault().logError("Selected marker is not a FindBugs marker");
				return null;
			}

			// We have a FindBugs marker.  Get the corresponding BugInstance.
			String uniqueId = marker.getAttribute(FindBugsMarker.UNIQUE_ID, null);
			if (uniqueId == null) {
				FindbugsPlugin.getDefault().logError("Marker does not contain unique id for warning");
				return null;
			}

			BugCollection bugCollection = FindbugsPlugin.getBugCollection(project, null);

			return bugCollection.lookupFromUniqueId(uniqueId);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			// Multiple exception types caught here
			FindbugsPlugin.getDefault().logException(e, "Could not get BugInstance for FindBugs marker");
			return null;
		}
	}

	/**
	 * Return the marker for given warning.
	 *
	 * @param project the project in which the warning was reported
	 * @param warning the warning
	 * @return the marker, or null if no marker is displayed for this warning
	 *         (or we can't find the marker for some reason)
	 */
	public IMarker findMarkerForWarning(IProject project, BugInstance warning) {
		String warningUID = warning.getUniqueId();
		if (warningUID == null) {
			FindbugsPlugin.getDefault().logError("Bug instance has no unique id");
			return null;
		}
		try {
			IResource resource = getUnderlyingResource(warning, project);
			IMarker[] markerList =
				resource.findMarkers(FindBugsMarker.NAME, false, IResource.DEPTH_INFINITE);
			for (int i = 0; i < markerList.length; ++i) {
				IMarker marker = markerList[i];
				String markerUID = marker.getAttribute(FindBugsMarker.UNIQUE_ID, "");

				if (warningUID.equals(markerUID))
					return marker;
			}
			return null;
		} catch (JavaModelException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get marker for BugInstance");
			return null;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get marker for BugInstance");
			return null;
		}
	}

	/**
	 * Fish an IMarker out of given selection.
	 *
	 * @param selection the selection
	 * @return the selected IMarker, or null if we can't find an IMarker
	 *         in the selection
	 */
	public static IMarker getMarkerFromSelection(ISelection selection) {
		if (selection instanceof StructuredSelection) {
			StructuredSelection structuredSelection = (StructuredSelection) selection;

			for (Iterator i = structuredSelection.iterator(); i.hasNext(); ) {
				Object selectedObj = i.next();
				//System.out.println("\tSelection element: " + selectedObj.getClass().getName());
				if (selectedObj instanceof IMarker) {
					System.out.println("Selection element is an IMarker!");
					return (IMarker) selectedObj;
				}
			}
		}

		return null;
	}
}
