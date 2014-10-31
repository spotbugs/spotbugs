package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.eclipse.ui.views.markers.internal.Util;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * This class is the basic Quick Fix resolution for FindBugs. It uses a standard
 * pattern to run the fixes. Subclasses must use the ASTRewrite and the AST to
 * make their changes. They are not responsible for the setup and saving of the
 * changes.
 *
 * @author cchristopher@ebay.com
 * @author <a href="mailto:twyss@hsr.ch">Thierry Wyss</a>
 * @author <a href="mailto:mbusarel@hsr.ch">Marco Busarello</a>
 * @author <a href="mailto:g1zgragg@hsr.ch">Guido Zgraggen</a>
 */
public abstract class BugResolution extends WorkbenchMarkerResolution {

    static private final String MISSING_BUG_INSTANCE = "This bug is no longer in the system. "
            + "The bugs somehow got out of sync with the memory representation. "
            + "Try running FindBugs again. If that does not work, check the error log.";

    protected static final String PLACEHOLDER_STRING = "YYY";

    public static final String DEFAULT_REPLACEMENT = "XXX";

    private String label;

    private IProgressMonitor monitor;

    private String bugPattern;

    private IMarker currentMarker;

    private final Map<CompilationUnit, ASTRewrite> reusableRewrites = new HashMap<>();

    //to save memory, we cache only one CompilationUnit.  We can do this because we sort multiple Imarkers by their associated resource
    private ICompilationUnit cachedCompilationUnitKey;

    private CompilationUnit cachedCompilationUnit;

    protected String customizedLabel;

    /**
     * Called by reflection!
     */
    public BugResolution() {
        label = getClass().getSimpleName();
    }

    /**
     * Called on initialization
     * @param options optional arguments
     */
    public void setOptions(@Nonnull Map<String, String> options){
        // noop
    }

    /**
     * Returns the short label that briefly describes the change that will be made.
     *
     * Typically, labels are static and defined in <code>plugin.xml</code>.
     * For runtime-computed labels, define a base label in plugin.xml using the
     * <code>PLACEHOLDER_STRING</code> "YYY" where any custom text should go.  Then,
     * override getLabelFixingVisitor() to scan the code and find the text to replace
     * the placeholder.
     *
     * The visitor is only used to scan once, the result being cached on subsequent visits.
     */
    @Override
    @Nonnull
    public String getLabel() {
        ASTVisitor labelFixingVisitor = getCustomLabelVisitor();
        if (labelFixingVisitor instanceof CustomLabelVisitor) {
            if (customizedLabel == null) {
                String labelReplacement = findLabelReplacement(labelFixingVisitor);
                customizedLabel = label.replace(BugResolution.PLACEHOLDER_STRING, labelReplacement);
            }
            return customizedLabel;
        }
        return label;
    }


    @Nonnull
    private String findLabelReplacement(ASTVisitor labelFixingVisitor) {
        IMarker marker = getMarker();
        try {
            ASTNode node = getNodeForMarker(marker);
            if (node != null) {
                node.accept(labelFixingVisitor);
                String retVal = ((CustomLabelVisitor) labelFixingVisitor).getLabelReplacement();
                return retVal == null ? DEFAULT_REPLACEMENT: retVal;
            }
            // Catch all exceptions (explicit) so that the label creation won't fail
            // FindBugs prefers this being explicit instead of just catching Exception
        } catch (JavaModelException | ASTNodeNotFoundException | RuntimeException e) {
            FindbugsPlugin.getDefault().logException(e, e.getLocalizedMessage());
            return DEFAULT_REPLACEMENT;
        }
        return DEFAULT_REPLACEMENT;
    }

    /**
     * Returns an ASTVisitor that also implements CustomLabelVisitor or null
     * if the label in plugin.xml will suffice.
     *
     * Override this to give a resolution a custom label.
     * @return
     */
    @CheckForNull
    protected ASTVisitor getCustomLabelVisitor() {
        return null;
    }

    @CheckForNull
    protected ASTNode getNodeForMarker(IMarker marker) throws JavaModelException, ASTNodeNotFoundException {
        BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
        if (bug == null) {
            return null;
        }
        ICompilationUnit originalUnit = getCompilationUnit(marker);
        if (originalUnit == null) {
            return null;
        }

        CompilationUnit workingUnit = createWorkingCopy(originalUnit);

        return getASTNode(workingUnit, bug.getPrimarySourceLineAnnotation());
    }

    public void setLabel(String label) {
        requireNonNull(label, "label");
        this.label = label;
    }

    @Override
    public String getDescription() {
        return getLabel();
    }

    @Override
    public Image getImage() {
        ImageRegistry registry = FindbugsPlugin.getDefault().getImageRegistry();
        return registry.get(FindbugsPlugin.ICON_DEFAULT);
    }

    @Override
    public IMarker[] findOtherMarkers(IMarker[] markers) {
        Set<IMarker> set = new HashSet<>(markers.length);
        for (IMarker other : markers) {
            if(currentMarker == other || !MarkerUtil.isFindBugsMarker(other)) {
                continue;
            }
            String pattern = MarkerUtil.getBugPatternString(other);
            if(pattern == null){
                continue;
            }
            if (pattern.equals(bugPattern)) {
                set.add(other);
            }
        }
        IMarker[] retVal = set.toArray(new IMarker[set.size()]);
        de.tobject.findbugs.util.Util.sortIMarkers(retVal);
        return retVal;
    }

    @CheckForNull
    public IProgressMonitor getMonitor() {
        return monitor;
    }

    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public void run(IMarker[] markers, IProgressMonitor multipleFixMonitor) {
        //sort the markers to make the smaller cache work properly
        de.tobject.findbugs.util.Util.sortIMarkers(markers);

        List<PendingRewrite> pendingRewrites = new ArrayList<>(markers.length);
        for (int i = 0; i < markers.length; i++) {
            // this was done in the superclass implementation, copied here.
            if (multipleFixMonitor != null) {
                multipleFixMonitor.subTask(Util.getProperty(IMarker.MESSAGE, markers[i]));
            }
            pendingRewrites.add(resolveWithoutWriting(markers[i]));
        }
        // fully commit all changes
        //TODO disable automatically running FindBugs during this
        for (PendingRewrite pendingRewrite : pendingRewrites) {
            completeRewrite(pendingRewrite);
        }
        //TODO reenable automatically running FindBugs if appropriate
    }

    @CheckForNull
    private IRegion completeRewrite(PendingRewrite p) {
        try {
            if (p != null) {
                return rewriteCompilationUnit(p.rewrite, p.doc, p.originalUnit);
            }
        } catch (JavaModelException | BadLocationException e) {
            reportException(e);
        }
        return null;
    }

    @CheckForNull
    private PendingRewrite resolveWithoutWriting(IMarker marker) {
        requireNonNull(marker, "marker");
        ICompilationUnit originalUnit = null;
        try {
            BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
            if (bug == null) {
                throw new BugResolutionException(MISSING_BUG_INSTANCE);
            }

            IProject project = marker.getResource().getProject();
            originalUnit = getCompilationUnit(marker);
            if (originalUnit == null) {
                throw new BugResolutionException("No compilation unit found for marker " + marker.getType() + " ("
                        + marker.getId() + ')');
            }

            Document doc = new Document(originalUnit.getBuffer().getContents());
            CompilationUnit workingUnit = makeOrReuseWorkingCopy(originalUnit);

            ASTRewrite rewrite = makeOrReuseRewrite(workingUnit);

            repairBug(rewrite, workingUnit, bug);
            marker.delete();
            FindbugsPlugin.getBugCollection(project, monitor).remove(bug);
            return new PendingRewrite(rewrite, doc, originalUnit);
        } catch (BugResolutionException | CoreException e) {
            try {
                if (originalUnit != null) {
                    originalUnit.discardWorkingCopy();
                }
            } catch (JavaModelException e1) {
                reportException(e1);
            }
            reportException(e);
            return null;
        }
    }

    private CompilationUnit makeOrReuseWorkingCopy(@Nonnull ICompilationUnit originalUnit) throws JavaModelException {
        if (originalUnit.equals(cachedCompilationUnitKey)) {
            return cachedCompilationUnit;
        }

        cachedCompilationUnit = createWorkingCopy(originalUnit);
        cachedCompilationUnitKey = originalUnit;
        return cachedCompilationUnit;

    }

    private ASTRewrite makeOrReuseRewrite(CompilationUnit workingUnit) {
        // since we are caching compilation units as well, the hashcode won't
        // change even if rewrites are applied. It would change, however, if
        // makeOrReuseWorkingCopy wasn't used. Finally, both the working copy
        // and the rewrite need to be cached otherwise, nodes could be created
        // that belonged to the wrong CompilationUnit which causes problems for
        // moveTargets and copyTargets
        ASTRewrite rewrite = reusableRewrites.get(workingUnit);
        if (rewrite != null) {
            return rewrite;
        }
        rewrite = ASTRewrite.create(workingUnit.getAST());
        reusableRewrites.put(workingUnit, rewrite);
        return rewrite;
    }


    /**
     * Runs the <CODE>BugResolution</CODE> on the given <CODE>IMarker</CODE>.
     * The <CODE>IMarker</CODE> has to be a FindBugs marker. The
     * <CODE>BugInstance</CODE> associated to the <CODE>IMarker</CODE> will be
     * repaired. All exceptions are reported to the ErrorLog.
     *
     * @param marker
     *            non null The <CODE>IMarker</CODE> that specifies the bug.
     */
    @Override
    public void run(IMarker marker) {
        requireNonNull(marker, "marker");
        try {
            // do NOT inline this method invocation
            runInternal(marker);
        } catch (CoreException e) {
            reportException(e);
        }
    }

    /**
     * This method is used by the test-framework, to catch the thrown exceptions
     * and report it to the user.
     *
     * @see #run(IMarker)
     */
    private void runInternal(IMarker marker) throws CoreException {
        Assert.isNotNull(marker);

       PendingRewrite pending = resolveWithoutWriting(marker);
       if(pending == null){
           return;
       }

        try {
            IRegion region = completeRewrite(pending);
            if(region == null){
                return;
            }
            IEditorPart part = EditorUtility.isOpenInEditor(pending.originalUnit);
            if (part instanceof ITextEditor) {
                ((ITextEditor) part).selectAndReveal(region.getOffset(), region.getLength());
            }
        } finally {
            pending.originalUnit.discardWorkingCopy();
        }
    }

    /**
     * Returns if TypeBindings should be resolved. This is a mildly expensive
     * operation, so if the resolutions don't require knowing about Types,
     * return false. Otherwise, return true.
     *
     * @return
     */
    protected abstract boolean resolveBindings();

    protected abstract void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug)
            throws BugResolutionException;

    /**
     * Get the compilation unit for the marker.
     *
     * @param marker
     *            not null
     * @return The compilation unit for the marker, or null if the file was not
     *         accessible or was not a Java file.
     */
    @CheckForNull
    protected ICompilationUnit getCompilationUnit(IMarker marker) {
        IResource res = marker.getResource();
        if (res instanceof IFile && res.isAccessible()) {
            IJavaElement element = JavaCore.create((IFile) res);
            if (element instanceof ICompilationUnit) {
                return (ICompilationUnit) element;
            }
        }
        return null;
    }

    /**
     * Reports an exception to the user. This method could be overwritten by a
     * subclass to handle some exceptions individual.
     *
     * @param e
     *            not null
     */
    protected void reportException(Exception e) {
        Assert.isNotNull(e);

        FindbugsPlugin.getDefault().logException(e, e.getLocalizedMessage());
        MessageDialog.openError(FindbugsPlugin.getShell(), "BugResolution failed.", e.getLocalizedMessage());
    }

    @Nonnull
    protected final CompilationUnit createWorkingCopy(@Nonnull ICompilationUnit unit) throws JavaModelException {
        unit.becomeWorkingCopy(monitor);
        ASTParser parser = createAstParser();
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings());
        return (CompilationUnit) parser.createAST(monitor);
    }

    @SuppressWarnings("deprecation")
    private static ASTParser createAstParser() {
        ASTParser parser;
        int safeLevel = AST.JLS3;
        int JLS4 = 4; // @since 3.7.1, so can't link to constant
        int JLS8 = 8; // @since 3.10, so can't link to constant
        try {
            parser = ASTParser.newParser(JLS8);
        } catch (IllegalArgumentException e1) {
            try {
                parser = ASTParser.newParser(JLS4);
            } catch (IllegalArgumentException e2) {
                parser = ASTParser.newParser(safeLevel);
            }
        }
        return parser;
    }

    private IRegion rewriteCompilationUnit(ASTRewrite rewrite, IDocument doc, ICompilationUnit originalUnit)
            throws JavaModelException, BadLocationException {
        TextEdit edits = rewrite.rewriteAST(doc, originalUnit.getJavaProject().getOptions(true));
        edits.apply(doc);

        originalUnit.getBuffer().setContents(doc.get());
        originalUnit.commitWorkingCopy(false, monitor);
        return edits.getRegion();
    }

    /**
     * @return the bug type we started to work with (can be different on different resolution instances
     * if the resolution class supports multiple bug patterns)
     */
    @CheckForNull
    public String getBugPattern() {
        return bugPattern;
    }

    public void setBugPattern(@Nonnull String pattern) {
        Objects.requireNonNull(pattern);
        this.bugPattern = pattern;
    }

    /**
     * @return the marker we started to work with (can be different on different resolution instances
     * if the resolution class supports multiple bug types)
     */
    public IMarker getMarker() {
        return currentMarker;
    }

    /**
     * @param initialMarker the initialMarker to set
     */
    public void setMarker(IMarker initialMarker) {
        Objects.requireNonNull(initialMarker);
        this.currentMarker = initialMarker;
    }

    /**
     * If getApplicabilityVisitor() is overwritten, this checks
     * to see if this resolution applies to the code at the given marker.
     *
     * @param marker
     * @return true if this resolution should be visible to the user at the given marker
     */
    public boolean isApplicable(IMarker marker) {
        ASTVisitor prescanVisitor = getApplicabilityVisitor();
        if (prescanVisitor instanceof ApplicabilityVisitor) {       //this has an implicit null check
            return findApplicability(prescanVisitor, marker);
        }
        return true;
    }

    private boolean findApplicability(ASTVisitor prescanVisitor, IMarker marker) {
        try {
            ASTNode node = getNodeForMarker(marker);
            if (node != null) {
                node.accept(prescanVisitor);
                //this cast is safe because isApplicable checks the type before calling
                return ((ApplicabilityVisitor)prescanVisitor).isApplicable();
            }
            // Catch all exceptions (explicit) so that applicability check won't fail
            // FindBugs prefers this being explicit instead of just catching Exception
        } catch (JavaModelException | ASTNodeNotFoundException | RuntimeException e) {
            FindbugsPlugin.getDefault().logException(e, e.getLocalizedMessage());
            return true;
        }
        return true;
    }

    /**
     * Returns an ASTVisitor that also implements ApplicabilityVisitor or null
     * if the resolution will always work as configured.
     *
     * Override this to basically prescan the code to see if a fix is valid
     * before offering it to the user.
     * @return
     */
    protected ASTVisitor getApplicabilityVisitor() {
        return null;
    }

    private static class PendingRewrite {
        public ICompilationUnit originalUnit;
        public Document doc;
        public ASTRewrite rewrite;

        public PendingRewrite(ASTRewrite rewrite, Document doc, ICompilationUnit originalUnit) {
            this.rewrite = rewrite;
            this.doc = doc;
            this.originalUnit = originalUnit;
        }

    }

}
