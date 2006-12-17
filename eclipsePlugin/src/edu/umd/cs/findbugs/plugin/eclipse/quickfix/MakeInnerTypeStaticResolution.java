package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import static edu.umd.cs.findbugs.plugin.eclipse.quickfix.util.ASTUtil.getTypeDeclaration;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.plugin.eclipse.quickfix.exception.BugResolutionException;

/**
 * The <CODE>MakeInnerTypeStaticResolution</CODE> adds a <CODE>static</CODE>
 * modifier to an inner class.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#SIC_INNER_SHOULD_BE_STATIC">SIC_INNER_SHOULD_BE_STATIC</a>
 */
public class MakeInnerTypeStaticResolution extends BugResolution {

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
        Modifier finalMod = workingUnit.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);

        ListRewrite modRewrite = rewrite.getListRewrite(type, TypeDeclaration.MODIFIERS2_PROPERTY);
        modRewrite.insertLast(finalMod, null);
    }

}
