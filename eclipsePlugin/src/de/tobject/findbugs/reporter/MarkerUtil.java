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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.view.explorer.BugGroup;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.I18N;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;

/**
	* Utility methods for converting FindBugs BugInstance objects
	* into Eclipse markers.
	*
	* @author Peter Friese
	* @author David Hovemeyer
	*/
public final class MarkerUtil {

	final static Pattern fullName = Pattern.compile("^(.+?)(([$+][0-9].*)?)");
	private static final IMarker[] EMPTY = new IMarker[0];

	/**
	 * don't instantiate an utility class
	 */
	private MarkerUtil() {
		super();
	}

	/**
	 * Create an Eclipse marker for given BugInstance.
	 *
	 * @param project the project
	 * @param monitor
	 */
	public static void createMarkers(IJavaProject project, BugCollection theCollection,
			IProgressMonitor monitor) {

		List<MarkerParameter> bugParameters = createBugParameters(project, theCollection,
				monitor);
		addMarkers(bugParameters, project.getProject(), theCollection, monitor);
	}

	/**
	 * As a side-effect this method updates missing line information for some bugs stored
	 * in the given bug collection
	 * @param project
	 * @param theCollection
	 * @return never null
	 */
	public static List<MarkerParameter> createBugParameters(IJavaProject project,
			BugCollection theCollection, IProgressMonitor monitor) {
		List<MarkerParameter> bugParameters = new ArrayList<MarkerParameter>();
		if (project == null) {
			FindbugsPlugin.getDefault().logException(
					new NullPointerException("project is null"), "project is null");
			return bugParameters;
		}
		Iterator<BugInstance> iterator = theCollection.iterator();
		while (iterator.hasNext() && !monitor.isCanceled()) {
			BugInstance bug = iterator.next();
			MarkerParameter mp = createMarkerParameter(project, bug);
			if(mp != null){
				bugParameters.add(mp);
			}
		}
		return bugParameters;
	}

	private static MarkerParameter createMarkerParameter(IJavaProject project, BugInstance bug) {
		IResource resource = null;
		try {
			resource = getUnderlyingResource(bug, project);
		} catch (JavaModelException e1) {
			FindbugsPlugin.getDefault().logException(
					e1, "Could not find class resource for FindBugs warning");
		}
		if (resource == null) {
			if (Reporter.DEBUG) {
				reportNoResourceFound(bug);
			}
			return null;
		}

		// default - first class line
		int primaryLine = bug.getPrimarySourceLineAnnotation().getStartLine();

		int fieldLine = -1;
		if (primaryLine <= 0) {
			FieldAnnotation primaryField = bug.getPrimaryField();
			if (primaryField != null && primaryField.getSourceLines() != null) {
				fieldLine = primaryField.getSourceLines().getStartLine();
				// Eclipse editor starts with 1, otherwise the marker will not be shown in editor at all
				if(fieldLine > 0) {
					primaryLine = fieldLine;
				}
			}
		}

		MarkerParameter parameter;
		if(fieldLine > 0){
			parameter = new MarkerParameter(bug, resource, fieldLine, primaryLine);
		} else {
			parameter = new MarkerParameter(bug, resource, primaryLine, primaryLine);
		}
		if (Reporter.DEBUG) {
			System.out.println("Creating marker for "
			+ resource.getLocation() + ": line "
			+ parameter.primaryLine
			+ bug.getMessage());
		}
		return parameter;
	}

	private static void reportNoResourceFound(BugInstance bug) {
		String className = null;
		String packageName = null;
		ClassAnnotation primaryClass = bug.getPrimaryClass();
		if (primaryClass != null) {
			className = primaryClass.getClassName();
			packageName = primaryClass.getPackageName();
		}
		if (Reporter.DEBUG) {
			System.out.println("BUG in class: " //$NON-NLS-1$
			+ packageName + "." //$NON-NLS-1$
			+ className + ": \n\t" //$NON-NLS-1$
			+ bug.getMessage() + " / Annotation: " //$NON-NLS-1$
			+ bug.getAnnotationText() + " / Source Line: " //$NON-NLS-1$
			+ bug.getPrimarySourceLineAnnotation());
		}
		System.out.println("NOT found resource for a BUG in class: "
		+ packageName + "."
		+ className + ": \n\t"
		+ bug.getMessage() + " / Annotation: "
		+ bug.getAnnotationText() + " / Source Line: "
		+ bug.getPrimarySourceLineAnnotation());
	}


	public static void addMarkers(List<MarkerParameter> bugParameter, IProject project,
			BugCollection theCollection, IProgressMonitor monitor) {

		try {
			project.getWorkspace().run(
				new MarkerReporter(bugParameter, theCollection, project), // action
				project,  // scheduling rule (null if there are no scheduling restrictions)
				0,     // flags (could specify IWorkspace.AVOID_UPDATE)
				monitor); // progress monitor (null if progress reporting is not desired)
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Core exception on add marker");
		}
	}


	/**
	 * Get the underlying resource (Java class) for given BugInstance.
	 *
	 * @param bug  the BugInstance
	 * @param project  the project
	 * @return the IResource representing the Java class
	 */
	private static @CheckForNull
	IResource getUnderlyingResource(BugInstance bug, IJavaProject project) throws JavaModelException {

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
		} else {
			packageName = primarySourceLineAnnotation.getPackageName();
			qualifiedClassName = primarySourceLineAnnotation.getClassName();
		}
		if (qualifiedClassName == null) {
			return null;
		}

		if (Reporter.DEBUG) {
			System.out.println("Looking up class: " + packageName + ", " + qualifiedClassName);
		}

		Matcher m = fullName.matcher(qualifiedClassName);
		IType type;
		String innerName = null;
		if (m.matches() && m.group(2).length() > 0) {

			String outerQualifiedClassName = m.group(1).replace('$','.');
			innerName  = m.group(2).substring(1);
			// second argument is required to find also secondary types
			type = project.findType(outerQualifiedClassName, (IProgressMonitor)null);

			/*
			 * code below only points to the first line of inner class
			 * even if this is not a class bug but field bug
			 */
			if(!hasLineInfo(primarySourceLineAnnotation)) {
				completeInnerClassInfo(qualifiedClassName, innerName, type, bug);
			}
		} else {
			// second argument is required to find also secondary types
			type =  project.findType(qualifiedClassName.replace('$','.'), (IProgressMonitor)null);

			// for inner classes, some detectors does not properly report source lines:
			// instead of reporting the first line of inner class, they report first line of parent class
			// in this case we will try to fix this here and point to the right start line
			if(type != null && type.isMember()){
				if(!hasLineInfo(primarySourceLineAnnotation)) {
					completeInnerClassInfo(qualifiedClassName, type.getElementName(), type, bug);
				}
			}
		}

		// reassign it as it may be changed for inner classes
		primarySourceLineAnnotation = bug.getPrimarySourceLineAnnotation();

		int startLine;
		/*
		 *  Eclipse can help us find the line number for fields => we trying to add line
		 *  info for fields here
		 */
		if (primarySourceLineAnnotation != null) {
			startLine = primarySourceLineAnnotation.getStartLine();
			if(startLine <= 0 && bug.getPrimaryField() != null){
				completeFieldInfo(qualifiedClassName, innerName, type, bug);
			}
		} else {
			if(bug.getPrimaryField() != null){
				completeFieldInfo(qualifiedClassName, innerName, type, bug);
			}
		}

		if (type != null) {
			return type.getUnderlyingResource();
		}
		return null;
	}

	private static boolean hasLineInfo(SourceLineAnnotation annotation) {
		return annotation != null && annotation.getStartLine() > 0;
	}

	private static void completeFieldInfo(String qualifiedClassName, String innerName,
			IType type, BugInstance bug)  throws JavaModelException  {
		FieldAnnotation field = bug.getPrimaryField();
		if (field == null || type == null) {
			return;
		}

		IField ifield = type.getField(field.getFieldName());
		if (type instanceof SourceType) {
			ISourceRange sourceRange = ifield.getNameRange();
			if(sourceRange == null) {
				sourceRange = ifield.getSourceRange();
			}
			IScanner scanner = initScanner(type, sourceRange);
			int offset = sourceRange.getOffset();
			int lineNbr = scanner.getLineNumber(offset);
			lineNbr = lineNbr <= 0 ? 1 : lineNbr;
			String sourceFileStr = ""; //$NON-NLS-1$
			IResource res = type.getUnderlyingResource();
			if (res != null) {
				sourceFileStr = res.getName();// res.getRawLocation().toOSString();
			}
			field.setSourceLines(
					new SourceLineAnnotation(
						qualifiedClassName,
						sourceFileStr,
						lineNbr,
						lineNbr,
						0,
						0));
		}
	}

	private static void completeInnerClassInfo(String qualifiedClassName,
			String innerName, IType type, BugInstance bug) throws JavaModelException {
		int lineNbr = findChildSourceLine(type, innerName, bug);
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
	 * @return start line of given type, or 1 if line could not be found
	 */
	private static int getLineStart(SourceType source) throws JavaModelException {
		IOpenable op = source.getOpenable();
		if (op instanceof CompilationUnit) {
			ISourceRange range = source.getNameRange();
			if(range == null){
				range = source.getSourceRange();
			}
			IScanner scanner = initScanner(source, range);
			return scanner.getLineNumber(range.getOffset());
		}
		// start line of enclosing type
		return 1;
	}

	/**
	 * @param source must be not null
	 * @param sourceRange
	 * @return may return null, otherwise an initialized scanner which may answer which
	 * source offset index belongs to which source line
	 */
	private static IScanner initScanner(IJavaElement source, ISourceRange range) throws JavaModelException {
		IOpenable op = source.getOpenable();
		if (op instanceof CompilationUnit) {
			CompilationUnit cu = (CompilationUnit) op;
			IScanner scanner =
				ToolFactory.createScanner(false, false, false, true);
			scanner.setSource(cu.getContents());
			int offset = range.getOffset();
			try {
				while (scanner.getNextToken() != ITerminalSymbols.TokenNameEOF) {
					// do nothing, just wait for the end of stream
					if(offset <= scanner.getCurrentTokenEndPosition()){
						break;
					}
				}
			} catch (InvalidInputException e) {
				FindbugsPlugin.getDefault().logException(e,
						"Could not init scanner for type: " + source);
			}
			return scanner;
		}
		return null;
	}

	private static int findChildSourceLine(IJavaElement parentType, String name, BugInstance bug)
			throws JavaModelException {
		if (parentType == null) {
			return -1;
		}
		char firstChar = name.charAt(0);
		boolean firstIsDigit = Character.isDigit(firstChar);
		if (!firstIsDigit) {
			return findInnerClassSourceLine(parentType, name);
		}
		boolean innerFromMember = firstIsDigit && name.length() > 1
			&& !Character.isDigit(name.charAt(1));
		if(innerFromMember){
			return findInnerClassSourceLine(parentType, name.substring(1));
		}
		return findInnerAnonymousClassSourceLine(parentType, name);
	}

	private static int findInnerAnonymousClassSourceLine(IJavaElement parentType,
			String innerName) throws JavaModelException {
		IType anon = JdtUtils.findAnonymous((IType) parentType, innerName);
		int line = -1;
		if (anon instanceof SourceType){
			line = getLineStart((SourceType) anon);
		}
		return line;
	}

	/**
	 * Fix for bug 2032970: reads all tokens until given one or EOF is reached. This is
	 * required if sourcecode of anonymous class contains generics definitions
	 */
	private static int skipUntil(IScanner scanner, int endToken) throws InvalidInputException{
		int tokenID = scanner.getNextToken();
		while (tokenID != endToken && tokenID != ITerminalSymbols.TokenNameEOF){
			tokenID = scanner.getNextToken();
		}
		return tokenID;
	}

	private static int findInnerClassSourceLine(IJavaElement parentType,
			String name) throws JavaModelException {
		String elemName = parentType.getElementName();
		if (name.equals(elemName)) {
			if (parentType instanceof SourceType) {
				SourceType source = (SourceType) parentType;
				return getLineStart(source);
			}
		}
		if (parentType instanceof IParent) {
			IJavaElement[] children = ((IParent) parentType).getChildren();
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
	 * Remove all FindBugs problem markers for given resource.
	 *
	 * @param res the resource
	 */
	public static void removeMarkers(IResource res) throws CoreException {
		// remove any markers added by our builder
		// This triggers resource update on IResourceChangeListener's (BugTreeView)
		res.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
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
		return filterSettings.displayWarning(bugInstance);
	}

	/**
	 * Attempt to redisplay FindBugs problem markers for
	 * given project.
	 *
	 * @param javaProject the project
	 * @param shell   Shell the progress dialog should be tied to
	 */
	public static void redisplayMarkers(final IJavaProject javaProject, Shell shell) {

		ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(shell);
		final IProject project = javaProject.getProject();

		try {
			progressDialog.run(false, false, new IRunnableWithProgress() {

				public void run(IProgressMonitor monitor) {

					try {
						// Get the saved bug collection for the project
						SortedBugCollection bugCollection =
							FindbugsPlugin.getBugCollection(project, monitor);
						if (bugCollection != null) {
							// Remove old markers
							MarkerUtil.removeMarkers(project);
							// Display warnings
							MarkerUtil.createMarkers(javaProject, bugCollection, monitor);
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

	public static @CheckForNull BugCode findBugCodeForMarker(IMarker marker) {
		try {
			Object bugCode = marker.getAttribute(FindBugsMarker.PATTERN_TYPE);
			if(bugCode instanceof String){
				return I18N.instance().getBugCode((String) bugCode);
			}
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Marker does not contain bug code");
			return null;
		}
		return null;
	}

	public static @CheckForNull BugPattern findBugPatternForMarker(IMarker marker) {
		try {
			Object patternId = marker.getAttribute(FindBugsMarker.BUG_TYPE);
			if(patternId instanceof String){
				return I18N.instance().lookupBugPattern((String) patternId);
			}
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Marker does not contain pattern id");
			return null;
		}
		return null;
	}

	/**
	 * Find the BugInstance associated with given FindBugs marker.
	 *
	 * @param marker a FindBugs marker
	 * @return the BugInstance associated with the marker,
	 *         or null if we can't find the BugInstance
	 */
	public static @CheckForNull BugInstance findBugInstanceForMarker(IMarker marker) {
		IResource resource = marker.getResource();
		IProject project = resource.getProject();
		if (project == null) {
			// Also shouldn't happen.
			FindbugsPlugin.getDefault().logError("No project for warning marker");
			return null;
		}
		try {
			if (!isFindBugsMarker(marker)) {
				// log disabled because otherwise each selection in problems view generates
				// 6 new errors (we need refactor all bug views to get rid of this).
//				FindbugsPlugin.getDefault().logError("Selected marker is not a FindBugs marker");
//				FindbugsPlugin.getDefault().logError(marker.getType());
//				FindbugsPlugin.getDefault().logError(FindBugsMarker.NAME);
				return null;
			}

			// We have a FindBugs marker.  Get the corresponding BugInstance.
			String bugId = marker.getAttribute(FindBugsMarker.UNIQUE_ID, null);
			if (bugId == null) {
				FindbugsPlugin.getDefault().logError("Marker does not contain unique id for warning");
				return null;
			}

			BugCollection bugCollection = FindbugsPlugin.getBugCollection(project, null);
			if (bugCollection == null) {
				FindbugsPlugin.getDefault().logError("Could not get BugCollection for FindBugs marker");
				return null;
			}

			String bugType = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
			Integer lineNumber = (Integer)marker.getAttribute(IMarker.LINE_NUMBER);
			Integer primaryLineNumber = (Integer)marker.getAttribute(FindBugsMarker.PRIMARY_LINE);

			// compatibility
			if(primaryLineNumber == null){
				primaryLineNumber = lineNumber;
			}

			if (bugType == null || primaryLineNumber == null) {
				FindbugsPlugin.getDefault().logError("Could not get find attributes for marker " + marker + ": (" + bugId + ", " + bugType +", " + lineNumber+")");
				return null;
			}
			BugInstance bug = bugCollection.findBug(bugId, bugType, primaryLineNumber.intValue());
			return bug;
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e, "Could not get BugInstance for FindBugs marker");
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
	public static Set<IMarker> getMarkerFromSelection(ISelection selection) {
		Set<IMarker> markers = new HashSet<IMarker>();
		if(!(selection instanceof IStructuredSelection)){
			return markers;
		}
		IStructuredSelection sSelection = (IStructuredSelection) selection;
		for (Iterator<?> iter = sSelection.iterator(); iter.hasNext();) {
			Object next = iter.next();
			if(next instanceof IMarker){
				IMarker marker = (IMarker) next;
				if (!isFindBugsMarker(marker)) {
					continue;
				}
				markers.add(marker);
			} else if (next instanceof BugGroup){
				BugGroup group = (BugGroup) next;
				markers.addAll(group.getAllMarkers());
			} else if (next instanceof IResource){
				IResource res = (IResource) next;
				IMarker[] markers2 = MarkerUtil.getAllMarkers(res);
				for (IMarker marker : markers2) {
					markers.add(marker);
				}
			} else if (next instanceof IAdaptable){
				IAdaptable adapter = (IAdaptable) next;
				IMarker marker = (IMarker) adapter.getAdapter(IMarker.class);
				if (!isFindBugsMarker(marker)) {
					continue;
				}
				markers.add(marker);
			}
		}
		return markers;
	}

	public static IMarker getMarkerFromSingleSelection(ISelection selection) {
		if(!(selection instanceof IStructuredSelection)){
			return null;
		}
		IStructuredSelection sSelection = (IStructuredSelection) selection;
		if(sSelection.size() != 1){
			return null;
		}

		Object next = sSelection.getFirstElement();
		if(next instanceof IMarker){
			IMarker marker = (IMarker) next;
			if (!isFindBugsMarker(marker)) {
				return null;
			}
			return marker;
		} else if (next instanceof BugGroup){
			return null;
		} else if (next instanceof IResource){
			return null;
		} else if (next instanceof IAdaptable){
			IAdaptable adapter = (IAdaptable) next;
			IMarker marker = (IMarker) adapter.getAdapter(IMarker.class);
			if (!isFindBugsMarker(marker)) {
				return null;
			}
			return marker;
		}
		return null;
	}

	public static boolean isFindBugsMarker(IMarker marker) {
		try {
			return marker != null && marker.exists() && marker.isSubtypeOf(FindBugsMarker.NAME);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
			"Exception while checking FindBugs type on marker.");
		}
		return false;
	}

	/**
	 * Retrieves all the FB markers from given resource and all its descendants
	 * @param fileOrFolder
	 * @return never null (empty array if nothing there or exception happens).
	 * Exception will be logged
	 */
	public static IMarker[] getAllMarkers(IResource fileOrFolder){
		return getMarkers(fileOrFolder, IResource.DEPTH_INFINITE);
	}

	/**
	 * Retrieves all the FB markers from given resource and all its descendants
	 * @param fileOrFolder
	 * @return never null (empty array if nothing there or exception happens).
	 * Exception will be logged
	 */
	public static IMarker[] getMarkers(IResource fileOrFolder, int depth){
		try {
			return fileOrFolder.findMarkers(FindBugsMarker.NAME, true, depth);
		} catch (CoreException e) {
			FindbugsPlugin.getDefault().logException(e,
					"Cannot collect FindBugs warnings from: " + fileOrFolder);
		}
		return EMPTY;
	}

	/**
	 * @param marker might be null
	 * @param bugIdToFilter might be null
	 * @return true if marker should be filtered
	 */
	public static boolean isFiltered(IMarker marker, Set<String> bugIdToFilter) {
		if(marker == null){
			return true;
		}
		if(bugIdToFilter == null){
			return false;
		}
		String pattern = marker.getAttribute(FindBugsMarker.BUG_TYPE, "not found");
		String patternType = marker.getAttribute(FindBugsMarker.PATTERN_TYPE, "not found");
		for (String badId : bugIdToFilter) {
			if(badId.equals(patternType) || badId.equals(pattern)){
				return true;
			}
		}
		return false;
	}

}
