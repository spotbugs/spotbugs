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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IField;
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
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import de.tobject.findbugs.FindBugsJob;
import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.builder.WorkItem;
import de.tobject.findbugs.marker.FindBugsMarker;
import de.tobject.findbugs.marker.FindBugsMarker.MarkerConfidence;
import de.tobject.findbugs.view.explorer.BugGroup;
import edu.umd.cs.findbugs.BugCode;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugPattern;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.PackageMemberAnnotation;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.config.ProjectFilterSettings;

/**
 * Utility methods for converting FindBugs BugInstance objects into Eclipse
 * markers.
 *
 * @author Peter Friese
 * @author David Hovemeyer
 */
public final class MarkerUtil {

    final static Pattern fullName = Pattern.compile("^(.+?)(([$+][0-9].*)?)");

    private static final IMarker[] EMPTY = new IMarker[0];

    private static final int START_LINE_OF_ENCLOSING_TYPE = 1;

    /**
     * don't instantiate an utility class
     */
    private MarkerUtil() {
        super();
    }

    /**
     * Create an Eclipse marker for given BugInstance.
     *
     * @param javaProject
     *            the project
     * @param monitor
     */
    public static void createMarkers(final IJavaProject javaProject, final SortedBugCollection theCollection, final ISchedulingRule rule, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return;
        }
        final List<MarkerParameter> bugParameters = createBugParameters(javaProject, theCollection, monitor);
        if (monitor.isCanceled()) {
            return;
        }
        WorkspaceJob wsJob = new WorkspaceJob("Creating FindBugs markers") {

            @Override
            public IStatus runInWorkspace(IProgressMonitor monitor1) throws CoreException {
                IProject project = javaProject.getProject();
                try {
                    new MarkerReporter(bugParameters, theCollection, project).run(monitor1);
                } catch (CoreException e) {
                    FindbugsPlugin.getDefault().logException(e, "Core exception on add marker");
                    return e.getStatus();
                }
                return monitor1.isCanceled()? Status.CANCEL_STATUS : Status.OK_STATUS;
            }
        };
        wsJob.setRule(rule);
        wsJob.setSystem(true);
        wsJob.setUser(false);
        wsJob.schedule();
    }

    /**
     * As a side-effect this method updates missing line information for some
     * bugs stored in the given bug collection
     *
     * @param project
     * @param theCollection
     * @return never null
     */
    public static List<MarkerParameter> createBugParameters(IJavaProject project, BugCollection theCollection,
            IProgressMonitor monitor) {
        List<MarkerParameter> bugParameters = new ArrayList<MarkerParameter>();
        if (project == null) {
            FindbugsPlugin.getDefault().logException(new NullPointerException("project is null"), "project is null");
            return bugParameters;
        }
        Iterator<BugInstance> iterator = theCollection.iterator();
        while (iterator.hasNext() && !monitor.isCanceled()) {
            BugInstance bug = iterator.next();
            DetectorFactory detectorFactory = bug.getDetectorFactory();
            if(detectorFactory != null && !detectorFactory.getPlugin().isGloballyEnabled()){
                continue;
            }
            MarkerParameter mp = createMarkerParameter(project, bug);
            if (mp != null) {
                bugParameters.add(mp);
            }
        }
        return bugParameters;
    }

    private static MarkerParameter createMarkerParameter(IJavaProject project, BugInstance bug) {
        IJavaElement type = null;
        WorkItem resource = null;
        try {
            type = getJavaElement(bug, project);
            if (type != null) {
                resource = new WorkItem(type);
            }
        } catch (JavaModelException e1) {
            FindbugsPlugin.getDefault().logException(e1, "Could not find Java type for FindBugs warning");
        }
        if (resource == null) {
            if (Reporter.DEBUG) {
                reportNoResourceFound(bug);
            }
            return null;
        }

        // default - first class line
        int primaryLine = bug.getPrimarySourceLineAnnotation().getStartLine();

        // FindBugs needs originally generated primary line in order to find the
        // bug again.
        // Sometimes this primary line is <= 0, which causes Eclipse editor to
        // ignore it
        // So we check if we can replace the "wrong" primary line with a
        // "better" start line
        // If not, we just provide two values - one for Eclipse, another for
        // FindBugs itself.

        int startLine = -1;
        if (primaryLine <= 0) {
            FieldAnnotation primaryField = bug.getPrimaryField();
            if (primaryField != null && primaryField.getSourceLines() != null) {
                startLine = primaryField.getSourceLines().getStartLine();
                if (startLine < 0) {
                    // We have to provide line number, otherwise editor wouldn't
                    // show it
                    startLine = 1;
                }
            } else {
                // We have to provide line number, otherwise editor wouldn't
                // show it
                startLine = 1;
            }
        }

        MarkerParameter parameter;
        if (startLine > 0) {
            parameter = new MarkerParameter(bug, resource, startLine, primaryLine);
        } else {
            parameter = new MarkerParameter(bug, resource, primaryLine, primaryLine);
        }
        if (Reporter.DEBUG) {
            System.out
            .println("Creating marker for " + resource.getPath() + ": line " + parameter.primaryLine + bug.getMessage());
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
            System.out.println("NOT found resource for a BUG in BUG in class: " //$NON-NLS-1$
                    + packageName + "." //$NON-NLS-1$
                    + className + ": \n\t" //$NON-NLS-1$
                    + bug.getMessage() + " / Annotation: " //$NON-NLS-1$
                    + bug.getAnnotationText() + " / Source Line: " //$NON-NLS-1$
                    + bug.getPrimarySourceLineAnnotation());
        }
    }

    /**
     * Get the underlying resource (Java class) for given BugInstance.
     *
     * @param bug
     *            the BugInstance
     * @param project
     *            the project
     * @return the IResource representing the Java class
     */
    private static @CheckForNull
    IJavaElement getJavaElement(BugInstance bug, IJavaProject project) throws JavaModelException {

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

            String outerQualifiedClassName = m.group(1).replace('$', '.');
            innerName = m.group(2).substring(1);
            // second argument is required to find also secondary types
            type = project.findType(outerQualifiedClassName, (IProgressMonitor) null);

            /*
             * code below only points to the first line of inner class even if
             * this is not a class bug but field bug
             */
            if (type != null && !hasLineInfo(primarySourceLineAnnotation)) {
                completeInnerClassInfo(qualifiedClassName, innerName, type, bug);
            }
        } else {
            // second argument is required to find also secondary types
            type = project.findType(qualifiedClassName.replace('$', '.'), (IProgressMonitor) null);

            // for inner classes, some detectors does not properly report source
            // lines:
            // instead of reporting the first line of inner class, they report
            // first line of parent class
            // in this case we will try to fix this here and point to the right
            // start line
            if (type != null && type.isMember()) {
                if (!hasLineInfo(primarySourceLineAnnotation)) {
                    completeInnerClassInfo(qualifiedClassName, type.getElementName(), type, bug);
                }
            }
        }

        // reassign it as it may be changed for inner classes
        primarySourceLineAnnotation = bug.getPrimarySourceLineAnnotation();

        int startLine;
        /*
         * Eclipse can help us find the line number for fields => we trying to
         * add line info for fields here
         */
        if (primarySourceLineAnnotation != null) {
            startLine = primarySourceLineAnnotation.getStartLine();
            if (startLine <= 0 && bug.getPrimaryField() != null) {
                completeFieldInfo(qualifiedClassName, innerName, type, bug);
            }
        } else {
            if (bug.getPrimaryField() != null) {
                completeFieldInfo(qualifiedClassName, innerName, type, bug);
            }
        }

        return type;
    }

    private static boolean hasLineInfo(SourceLineAnnotation annotation) {
        return annotation != null && annotation.getStartLine() > 0;
    }

    private static void completeFieldInfo(String qualifiedClassName, String innerName, IType type, BugInstance bug) {
        FieldAnnotation field = bug.getPrimaryField();
        if (field == null || type == null) {
            return;
        }

        IField ifield = type.getField(field.getFieldName());
        ISourceRange sourceRange = null;
        IScanner scanner = null;
        JavaModelException ex = null;
        try {
            sourceRange = ifield.getNameRange();
        } catch (JavaModelException e) {
            ex = e;
        }
        try {
            // second try...
            if (sourceRange == null) {
                sourceRange = ifield.getSourceRange();
            }
            scanner = initScanner(type, sourceRange);
        } catch (JavaModelException e) {
            String message = "Can not complete field annotation " + field + " for the field: " + ifield + " in class: "
                    + qualifiedClassName + ", type " + type + ", bug " + bug;
            if (ex != null) {
                // report only first one
                e = ex;
            }
            FindbugsPlugin.getDefault().logMessage(IStatus.WARNING, message, e);
        }
        if (scanner == null || sourceRange == null) {
            return;
        }
        int lineNbr = scanner.getLineNumber(sourceRange.getOffset());
        lineNbr = lineNbr <= 0 ? 1 : lineNbr;
        String sourceFileStr = getSourceFileHint(type, qualifiedClassName);
        field.setSourceLines(new SourceLineAnnotation(qualifiedClassName, sourceFileStr, lineNbr, lineNbr, 0, 0));
    }

    private static String getSourceFileHint(IType type, String qualifiedClassName) {
        String sourceFileStr = "";
        IJavaElement primaryElement = type.getPrimaryElement();
        if (primaryElement != null) {
            return primaryElement.getElementName() + ".java";
        }
        return sourceFileStr;
    }

    private static void completeInnerClassInfo(String qualifiedClassName, String innerName, @Nonnull IType type, BugInstance bug)
            throws JavaModelException {
        int lineNbr = findChildSourceLine(type, innerName, bug);
        // should be always first line, if not found
        lineNbr = lineNbr <= 0 ? 1 : lineNbr;
        String sourceFileStr = getSourceFileHint(type, qualifiedClassName);
        if (sourceFileStr != null && sourceFileStr.length() > 0) {
            bug.addSourceLine(new SourceLineAnnotation(qualifiedClassName, sourceFileStr, lineNbr, lineNbr, 0, 0));
        }
    }

    /**
     * @return start line of given type, or 1 if line could not be found
     */
    private static int getLineStart(IType source) throws JavaModelException {
        ISourceRange range = source.getNameRange();
        if (range == null) {
            range = source.getSourceRange();
        }
        IScanner scanner = initScanner(source, range);
        if (scanner != null && range != null) {
            return scanner.getLineNumber(range.getOffset());
        }
        return START_LINE_OF_ENCLOSING_TYPE;
    }

    /**
     * @param source
     *            must be not null
     * @param range
     *            can be null
     * @return may return null, otherwise an initialized scanner which may
     *         answer which source offset index belongs to which source line
     * @throws JavaModelException
     */
    private static IScanner initScanner(IType source, ISourceRange range) throws JavaModelException {
        if (range == null) {
            return null;
        }
        char[] charContent = getContent(source);
        if (charContent == null) {
            return null;
        }
        IScanner scanner = ToolFactory.createScanner(false, false, false, true);
        scanner.setSource(charContent);
        int offset = range.getOffset();
        try {
            while (scanner.getNextToken() != ITerminalSymbols.TokenNameEOF) {
                // do nothing, just wait for the end of stream
                if (offset <= scanner.getCurrentTokenEndPosition()) {
                    break;
                }
            }
        } catch (InvalidInputException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not init scanner for type: " + source);
        }
        return scanner;
    }

    @SuppressWarnings("restriction")
    private static char[] getContent(IType source) throws JavaModelException {
        char[] charContent = null;
        IOpenable op = source.getOpenable();
        if (op instanceof CompilationUnit) {
            charContent = ((CompilationUnit) (op)).getContents();
        }
        if (charContent == null) {
            String content = source.getSource();
            if (content != null) {
                charContent = content.toCharArray();
            }
        }
        return charContent;
    }

    private static int findChildSourceLine(IType parentType, String name, BugInstance bug) throws JavaModelException {
        if (parentType == null) {
            return -1;
        }
        char firstChar = name.charAt(0);
        boolean firstIsDigit = Character.isDigit(firstChar);
        if (!firstIsDigit) {
            return findInnerClassSourceLine(parentType, name);
        }
        boolean innerFromMember = firstIsDigit && name.length() > 1 && !Character.isDigit(name.charAt(1));
        if (innerFromMember) {
            return findInnerClassSourceLine(parentType, name.substring(1));
        }
        return findInnerAnonymousClassSourceLine(parentType, name);
    }

    private static int findInnerAnonymousClassSourceLine(IJavaElement parentType, String innerName) throws JavaModelException {
        IType anon = JdtUtils.findAnonymous((IType) parentType, innerName);
        if (anon != null) {
            return getLineStart(anon);
        }
        return START_LINE_OF_ENCLOSING_TYPE;
    }

    private static int findInnerClassSourceLine(IJavaElement parentType, String name) throws JavaModelException {
        String elemName = parentType.getElementName();
        if (name.equals(elemName)) {
            if (parentType instanceof IType) {
                return getLineStart((IType) parentType);
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
        return START_LINE_OF_ENCLOSING_TYPE;
    }

    /**
     * Remove all FindBugs problem markers for given resource. If the given
     * resource is project, will also clear bug collection.
     *
     * @param res
     *            the resource
     */
    public static void removeMarkers(IResource res) throws CoreException {
        // remove any markers added by our builder
        // This triggers resource update on IResourceChangeListener's
        // (BugTreeView)
    	res.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
    	if (res instanceof IProject) {
            IProject project = (IProject) res;
            FindbugsPlugin.clearBugCollection(project);
        }        
    }

    /**
     * Given current active bug category set, minimum warning priority, and
     * previous user classification, return whether or not a warning (bug
     * instance) should be displayed using a marker.
     *
     * @param bugInstance
     *            the warning
     * @param filterSettings
     *            project filter settings
     * @return true if the warning should be displayed, false if not
     */
    public static boolean shouldDisplayWarning(BugInstance bugInstance, ProjectFilterSettings filterSettings) {
        return filterSettings.displayWarning(bugInstance);
    }

    /**
     * Attempt to redisplay FindBugs problem markers for given project.
     *
     * @param javaProject
     *            the project
     */
    public static void redisplayMarkers(final IJavaProject javaProject) {
        final IProject project = javaProject.getProject();
        FindBugsJob job = new FindBugsJob("Refreshing FindBugs markers", project) {
            @Override
            protected void runWithProgress(IProgressMonitor monitor) throws CoreException {
                // TODO in case we removed some of previously available
                // detectors, we should
                // throw away bugs reported by them

                // Get the saved bug collection for the project
                SortedBugCollection bugs = FindbugsPlugin.getBugCollection(project, monitor);
                // Remove old markers
                project.deleteMarkers(FindBugsMarker.NAME, true, IResource.DEPTH_INFINITE);
                // Display warnings
                createMarkers(javaProject, bugs, project, monitor);
            }
        };
        job.setRule(project);
        job.scheduleInteractive();
    }

    public static @CheckForNull
    BugCode findBugCodeForMarker(IMarker marker) {
        try {
            Object bugCode = marker.getAttribute(FindBugsMarker.PATTERN_TYPE);
            if (bugCode instanceof String) {
                return DetectorFactoryCollection.instance().getBugCode((String) bugCode);
            }
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker does not contain bug code");
            return null;
        }
        return null;
    }

    public static int findBugRankForMarker(IMarker marker) {
        return marker.getAttribute(FindBugsMarker.RANK, BugRanker.VISIBLE_RANK_MAX);
    }

    /**
     * @return priority (aka confidence)
     */
    public static MarkerConfidence findConfidenceForMarker(IMarker marker) {
        return MarkerConfidence.getConfidence(marker.getAttribute(FindBugsMarker.PRIO_AKA_CONFIDENCE,
                MarkerConfidence.Ignore.name()));
    }

    public static @CheckForNull
    BugPattern findBugPatternForMarker(IMarker marker) {
        try {
            Object patternId = marker.getAttribute(FindBugsMarker.BUG_TYPE);
            if (patternId instanceof String) {
                return DetectorFactoryCollection.instance().lookupBugPattern((String) patternId);
            }
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker does not contain pattern id");
            return null;
        }
        return null;
    }

    public static @CheckForNull
    IJavaElement findJavaElementForMarker(IMarker marker) {
        try {
            Object elementId = marker.getAttribute(FindBugsMarker.UNIQUE_JAVA_ID);
            if (elementId instanceof String) {
                return JavaCore.create((String) elementId);
            }
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker does not contain valid java element id");
            return null;
        }
        return null;
    }

    public static @CheckForNull
    Plugin findDetectorPluginFor(IMarker marker) {
        try {
            Object pluginId = marker.getAttribute(FindBugsMarker.DETECTOR_PLUGIN_ID);
            if (pluginId instanceof String) {
                return DetectorFactoryCollection.instance().getPluginById((String) pluginId);
            }
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Marker does not contain valid plugin id");
            return null;
        }
        return null;
    }

    public static Set<IMarker> findMarkerForJavaElement(IJavaElement elt, IMarker[] possibleCandidates, boolean recursive) {
        String id = elt.getHandleIdentifier();
        Set<IMarker> markers = new HashSet<IMarker>();
        for (IMarker marker : possibleCandidates) {
            try {
                Object elementId = marker.getAttribute(FindBugsMarker.UNIQUE_JAVA_ID);
                // UNIQUE_JAVA_ID exists first since 1.3.9 as FB attribute
                if (!(elementId instanceof String)) {
                    continue;
                }
                String stringId = (String) elementId;
                if (!recursive) {
                    if (stringId.equals(id)) {
                        // exact match
                        markers.add(marker);
                    } else if (isDirectChild(id, stringId)) {
                        // direct child: class in the package, but not in the
                        // sub-package
                        markers.add(marker);
                    }
                } else if (stringId.startsWith(id)) {
                    markers.add(marker);
                }
            } catch (CoreException e) {
                FindbugsPlugin.getDefault().logException(e, "Marker does not contain valid java element id");
                continue;
            }
        }
        return markers;
    }

    /**
     *
     * @param parentId
     *            java element id of a parent element
     * @param childId
     *            java element id of possible child
     * @return true if the second string represents a java element which is a
     *         direct child of the parent element.
     */
    @SuppressWarnings("restriction")
    private static boolean isDirectChild(String parentId, String childId) {
        return childId.startsWith(parentId) && (childId.length() > (parentId.length() + 1))
                // if there is NOT a class file separator, then it's not a direct child
                && childId.charAt(parentId.length()) == JavaElement.JEM_CLASSFILE;
    }

    public static class BugCollectionAndInstance {
        public BugCollection getBugCollection() {
            return bugCollection;
        }

        public BugInstance getBugInstance() {
            return bugInstance;
        }

        public BugCollectionAndInstance(@Nonnull BugCollection bugCollection, @Nonnull BugInstance bugInstance) {
            if (bugCollection == null) {
                throw new NullPointerException("Null bug collection");
            }
            if (bugInstance == null) {
                throw new NullPointerException("Null bug instance");
            }
            this.bugCollection = bugCollection;
            this.bugInstance = bugInstance;
        }

        final BugCollection bugCollection;

        final BugInstance bugInstance;

        @Override
        public String toString() {
            return bugInstance.getMessage();
        }
    }

    /**
     * Find the BugInstance associated with given FindBugs marker.
     *
     * @param marker
     *            a FindBugs marker
     * @return the BugInstance associated with the marker, or null if we can't
     *         find the BugInstance
     */
    public static @CheckForNull
    BugInstance findBugInstanceForMarker(IMarker marker) {
        BugCollectionAndInstance bci = findBugCollectionAndInstanceForMarker(marker);
        if (bci == null) {
            return null;
        }
        return bci.bugInstance;
    }

    /**
     * Find the BugCollectionAndInstance associated with given FindBugs marker.
     *
     * @param marker
     *            a FindBugs marker
     * @return the BugInstance associated with the marker, or null if we can't
     *         find the BugInstance
     */
    public static @CheckForNull
    BugCollectionAndInstance findBugCollectionAndInstanceForMarker(IMarker marker) {

        IResource resource = marker.getResource();
        IProject project = resource.getProject();
        if (project == null) {
            // Also shouldn't happen.
            FindbugsPlugin.getDefault().logError("No project for warning marker");
            return null;
        }
        if (!isFindBugsMarker(marker)) {
            // log disabled because otherwise each selection in problems view
            // generates
            // 6 new errors (we need refactor all bug views to get rid of this).
            // FindbugsPlugin.getDefault().logError("Selected marker is not a FindBugs marker");
            // FindbugsPlugin.getDefault().logError(marker.getType());
            // FindbugsPlugin.getDefault().logError(FindBugsMarker.NAME);
            return null;
        }

        // We have a FindBugs marker. Get the corresponding BugInstance.
        String bugId = marker.getAttribute(FindBugsMarker.UNIQUE_ID, null);
        if (bugId == null) {
            FindbugsPlugin.getDefault().logError("Marker does not contain unique id for warning");
            return null;
        }

        try {
            BugCollection bugCollection = FindbugsPlugin.getBugCollection(project, null);
            if (bugCollection == null) {
                FindbugsPlugin.getDefault().logError("Could not get BugCollection for FindBugs marker");
                return null;
            }

            String bugType = (String) marker.getAttribute(FindBugsMarker.BUG_TYPE);
            Integer primaryLineNumber = (Integer) marker.getAttribute(FindBugsMarker.PRIMARY_LINE);

            // compatibility
            if (primaryLineNumber == null) {
                primaryLineNumber = Integer.valueOf(getEditorLine(marker));
            }

            if (bugType == null) {
                FindbugsPlugin.getDefault().logError(
                        "Could not get find attributes for marker " + marker + ": (" + bugId + ", " + primaryLineNumber + ")");
                return null;
            }
            BugInstance bug = bugCollection.findBug(bugId, bugType, primaryLineNumber.intValue());
            if(bug == null) {
                FindbugsPlugin.getDefault().logError(
                        "Could not get find bug for marker on " + resource + ": (" + bugId + ", " + primaryLineNumber + ")");
                return null;
            }
            return new BugCollectionAndInstance(bugCollection, bug);
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Could not get BugInstance for FindBugs marker");
            return null;
        }
    }

    private static int getEditorLine(IMarker marker) {
        return marker.getAttribute(IMarker.LINE_NUMBER, -1);
    }

    /**
     * Fish an IMarker out of given selection.
     *
     * @param selection
     *            the selection
     * @return the selected IMarker, or null if we can't find an IMarker in the
     *         selection
     */
    public static Set<IMarker> getMarkerFromSelection(ISelection selection) {
        Set<IMarker> markers = new HashSet<IMarker>();
        if (!(selection instanceof IStructuredSelection)) {
            return markers;
        }
        IStructuredSelection sSelection = (IStructuredSelection) selection;
        for (Iterator<?> iter = sSelection.iterator(); iter.hasNext();) {
            Object next = iter.next();
            markers.addAll(getMarkers(next));
        }
        return markers;
    }

    public static Set<IMarker> getMarkers(Object obj) {
        Set<IMarker> markers = new HashSet<IMarker>();
        if (obj instanceof IMarker) {
            IMarker marker = (IMarker) obj;
            if (isFindBugsMarker(marker)) {
                markers.add(marker);
            }
        } else if (obj instanceof BugGroup) {
            BugGroup group = (BugGroup) obj;
            markers.addAll(group.getAllMarkers());
        } else if (obj instanceof IResource) {
            IResource res = (IResource) obj;
            IMarker[] markers2 = MarkerUtil.getAllMarkers(res);
            for (IMarker marker : markers2) {
                markers.add(marker);
            }
        } else if (obj instanceof IJavaElement) {
            markers.addAll(new WorkItem((IJavaElement) obj).getMarkers(true));
        } else if (obj instanceof IAdaptable) {
            IAdaptable adapter = (IAdaptable) obj;
            IMarker marker = (IMarker) adapter.getAdapter(IMarker.class);
            if (marker == null) {
                IResource resource = (IResource) adapter.getAdapter(IResource.class);
                if (resource == null) {
                    return markers;
                }
                IMarker[] markers2 = getMarkers(resource, IResource.DEPTH_INFINITE);
                markers.addAll(Arrays.asList(markers2));
            } else if (isFindBugsMarker(marker)) {
                markers.add(marker);
            }
        }
        return markers;
    }

    /**
     * Tries to retrieve right bug marker for given selection. If there are many
     * markers for given editor, and text selection doesn't match any of them,
     * return null. If there is only one marker for given editor, returns this
     * marker in any case.
     *
     * @param selection
     * @param editor
     * @return may return null
     */
    public static IMarker getMarkerFromEditor(ITextSelection selection, IEditorPart editor) {
        IResource resource = (IResource) editor.getEditorInput().getAdapter(IFile.class);
        IMarker[] allMarkers;
        if (resource != null) {
            allMarkers = getMarkers(resource, IResource.DEPTH_ZERO);
        } else {
            IClassFile classFile = (IClassFile) editor.getEditorInput().getAdapter(IClassFile.class);
            if (classFile == null) {
                return null;
            }
            Set<IMarker> markers = getMarkers(classFile.getType());
            allMarkers = markers.toArray(new IMarker[markers.size()]);
        }
        // if editor contains only one FB marker, do some cheating and always
        // return it.
        if (allMarkers.length == 1) {
            return allMarkers[0];
        }
        // +1 because it counts real lines, but editor shows lines + 1
        int startLine = selection.getStartLine() + 1;
        for (IMarker marker : allMarkers) {
            int line = getEditorLine(marker);
            if (startLine == line) {
                return marker;
            }
        }
        return null;
    }

    public static IMarker getMarkerFromSingleSelection(ISelection selection) {
        if (selection instanceof ITextSelection) {
            IEditorPart editor = FindbugsPlugin.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (!(editor instanceof ITextEditor)) {
                return null;
            }
            IMarker marker = MarkerUtil.getMarkerFromEditor((ITextSelection) selection, (ITextEditor) editor);
            if (marker != null) {
                selection = new StructuredSelection(marker);
            } else {
                selection = new StructuredSelection();
            }
        }

        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection sSelection = (IStructuredSelection) selection;
        if (sSelection.size() != 1) {
            return null;
        }

        Object next = sSelection.getFirstElement();
        if (next instanceof IMarker) {
            IMarker marker = (IMarker) next;
            if (!isFindBugsMarker(marker)) {
                return null;
            }
            return marker;
        } else if (next instanceof BugGroup) {
            return null;
        } else if (next instanceof IResource) {
            return null;
        } else if (next instanceof IAdaptable) {
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
            FindbugsPlugin.getDefault().logException(e, "Exception while checking FindBugs type on marker.");
        }
        return false;
    }

    /**
     * @param marker marker to check for plugin id
     * @return detector plugin id, or empty string if the detector plugin is unknown
     */
    @Nonnull
    public static String getPluginId(@Nonnull IMarker marker) {
        return marker.getAttribute(FindBugsMarker.DETECTOR_PLUGIN_ID, "");
    }

    /**
     * Retrieves all the FB markers from given resource and all its descendants
     *
     * @param fileOrFolder
     * @return never null (empty array if nothing there or exception happens).
     *         Exception will be logged
     */
    public static IMarker[] getAllMarkers(IResource fileOrFolder) {
        return getMarkers(fileOrFolder, IResource.DEPTH_INFINITE);
    }

    /**
     * Retrieves all the FB markers from given resource and all its descendants
     *
     * @param fileOrFolder
     * @return never null (empty array if nothing there or exception happens).
     *         Exception will be logged
     */
    @Nonnull
    public static IMarker[] getMarkers(IResource fileOrFolder, int depth) {
        if(fileOrFolder.getType() == IResource.PROJECT) {
            if(!fileOrFolder.isAccessible()) {
                // user just closed the project decorator is working on, avoid exception here
                return EMPTY;
            }
        }
        try {
            return fileOrFolder.findMarkers(FindBugsMarker.NAME, true, depth);
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, "Cannot collect FindBugs warnings from: " + fileOrFolder);
        }
        return EMPTY;
    }

    /**
     * @param marker
     *            might be null
     * @param bugIdToFilter
     *            might be null
     * @return true if marker should be filtered
     */
    public static boolean isFiltered(IMarker marker, Set<String> bugIdToFilter) {
        if (marker == null) {
            return true;
        }
        if (bugIdToFilter == null) {
            return false;
        }
        String pattern = marker.getAttribute(FindBugsMarker.BUG_TYPE, "not found");
        String patternType = marker.getAttribute(FindBugsMarker.PATTERN_TYPE, "not found");
        for (String badId : bugIdToFilter) {
            if (badId.equals(patternType) || badId.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

}
