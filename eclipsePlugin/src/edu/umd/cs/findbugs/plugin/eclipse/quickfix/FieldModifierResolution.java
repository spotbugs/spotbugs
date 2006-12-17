package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getFieldDeclaration;
import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * The <CODE>FieldModifierResolution</CODE> provides a resolution to replace
 * the <CODE>modifier</CODE> of a <CODE>field</CODE>.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#MS_SHOULD_BE_FINAL">MS_SHOULD_BE_FINAL</a>
 */
public abstract class FieldModifierResolution extends BugResolution {

    @Override
    protected boolean resolveBindings() {
        return false;
    }

    @Override
    protected void repairBug(ASTRewrite rewrite, CompilationUnit workingUnit, BugInstance bug) throws BugResolutionException {
        assert rewrite != null;
        assert workingUnit != null;
        assert bug != null;

        TypeDeclaration type = getTypeDeclaration(workingUnit, bug.getPrimaryClass());
        FieldDeclaration field = getFieldDeclaration(type, bug.getPrimaryField());

        Modifier finalModifier = workingUnit.getAST().newModifier(getModifierToAdd());

        ListRewrite modRewrite = rewrite.getListRewrite(field, FieldDeclaration.MODIFIERS2_PROPERTY);
        modRewrite.insertLast(finalModifier, null);
    }

    public abstract ModifierKeyword getModifierToAdd();

}
