/*
 * This file is a part of FindBugs(TM)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getASTNode;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import de.tobject.findbugs.reporter.MarkerUtil;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.ASTNodeNotFoundException;

/**
 * Like <code>BugResolution</code>, with additional support for a runtime-computed label.
 *
 * Typically, labels are static and defined in <code>plugin.xml</code>.
 * For runtime-computed labels, define a base label in plugin.xml using the
 * <code>PLACEHOLDER_STRING</code> "YYY" where any custom text should go.  Then,
 * return a <code>CustomLabelVisitor</code> to scan the code and find the text to replace
 * the placeholder.
 *
 * The visitor is only used to scan once, the result being cached on subsequent visits.
 *
 * @author <a href="mailto:kjlubick@ncsu.edu">Kevin Lubick</a>
 */
public abstract class CustomLabelBugResolution extends BugResolution {

    private static final String PLACEHOLDER_STRING = "YYY";
    public static final String DEFAULT_REPLACEMENT = "XXX";

    protected String customizedLabel;

    @Override
    public String getLabel() {
        if (customizedLabel == null) {
            String labelReplacement = findLabelReplacement(getLabelFixingVisitor());
            customizedLabel = super.getLabel().replace(CustomLabelBugResolution.PLACEHOLDER_STRING, labelReplacement);
        }
        return customizedLabel;
    }

    @Nonnull
    protected abstract CustomLabelVisitor getLabelFixingVisitor();

    @Nonnull
    private String findLabelReplacement(CustomLabelVisitor labelVisitor) {
        IMarker marker = getMarker();
        try {
            ASTNode node = getNodeForMarker(marker);
            if (node != null) {
                node.accept(labelVisitor);
                String retVal = labelVisitor.getLabelReplacement();
                return retVal == null ? DEFAULT_REPLACEMENT: retVal;
            }
            // Catch all exceptions (explicit) so that the label creation won't fail
            // FindBugs prefers this being explicit instead of just catching Exception
        } catch (JavaModelException | ASTNodeNotFoundException | RuntimeException e) {
            return DEFAULT_REPLACEMENT;
        }
        return DEFAULT_REPLACEMENT;
    }

    @CheckForNull
    private ASTNode getNodeForMarker(IMarker marker) throws JavaModelException, ASTNodeNotFoundException {
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

}
