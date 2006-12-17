package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ConditionCheck.checkForNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;

import de.tobject.findbugs.FindbugsPlugin;
import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * This class is the basic Quick Fix resolution for FindBugs. It uses a standard
 * pattern to run the fixes. Subclasses must use the ASTRewrite and the AST to
 * make their changes. They are not responsible for the setup and saving of the
 * changes.
 * 
 * @author cchristopher@ebay.com
 */
public abstract class BugResolution implements IMarkerResolution {

    static private final String MISSING_BUG_INSTANCE = "This bug is no longer in the system. " + "The bugs somehow got out of sync with the memory representation. " + "Try running FindBugs again. If that does not work, check the error log and remove the *.fbwarnings files.";

    private String label = getClass().getSimpleName();

    private IProgressMonitor monitor = null;

    @CheckForNull
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        checkForNull(label, "label");
        this.label = label;
    }

    @CheckForNull
    public IProgressMonitor getMonitor() {
        return monitor;
    }

    public void setMonitor(IProgressMonitor monitor) {
        this.monitor = monitor;
    }

    public void run(IMarker marker) {
        checkForNull(marker, "marker");
        try {
            BugInstance bug = MarkerUtil.findBugInstanceForMarker(marker);
            if (bug == null) {
                MessageDialog.openError(FindbugsPlugin.getShell(), "Missing Bug", MISSING_BUG_INSTANCE);
                return;
            }

            IProject project = marker.getResource().getProject();
            ICompilationUnit originalUnit = getCompilationUnit(marker);

            Document doc = new Document(originalUnit.getBuffer().getContents());
            CompilationUnit workingUnit = createWorkingCopy(originalUnit);

            ASTRewrite rewrite = ASTRewrite.create(workingUnit.getAST());

            try {
                repairBug(rewrite, workingUnit, bug);
                rewriteCompilationUnit(rewrite, doc, originalUnit);

                FindbugsPlugin.getBugCollection(project, monitor).remove(bug);

                marker.delete();
                MarkerUtil.redisplayMarkers(project, FindbugsPlugin.getShell());
            } finally {
                originalUnit.discardWorkingCopy();
            }

        } catch (BugResolutionException e) {
            FindbugsPlugin.getDefault().logException(e, e.getMessage());
        } catch (JavaModelException e) {
            FindbugsPlugin.getDefault().logException(e, e.getMessage());
        } catch (BadLocationException e) {
            FindbugsPlugin.getDefault().logException(e, e.getMessage());
        } catch (CoreException e) {
            FindbugsPlugin.getDefault().logException(e, e.getMessage());
        }
    }

    protected abstract boolean resolveBindings();

    protected abstract void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException;

    /**
     * Get the compilation unit for the marker
     * 
     * @param marker
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

    protected CompilationUnit createWorkingCopy(ICompilationUnit unit) throws JavaModelException {
        assert unit != null;

        unit.becomeWorkingCopy(null, monitor);
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(unit);
        parser.setResolveBindings(resolveBindings());
        return (CompilationUnit) parser.createAST(monitor);
    }

    protected void rewriteCompilationUnit(ASTRewrite rewrite, IDocument doc, ICompilationUnit originalUnit) throws JavaModelException, BadLocationException {
        TextEdit edits = rewrite.rewriteAST(doc, originalUnit.getJavaProject().getOptions(true));
        edits.apply(doc);

        originalUnit.getBuffer().setContents(doc.get());
        originalUnit.commitWorkingCopy(false, monitor);
    }

}
