package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;

public class SICFixer extends FindBugsFixer {

	@Override
	protected boolean resolveBindings() {
		return false;
	}

	@Override
	protected boolean modify(ASTRewrite rewrite, CompilationUnit astRoot,
			BugInstance bug) {
		TypeDeclaration type;
		
		String className = bug.getPrimaryClass().getClassName();
		
		type = getTypeDeclaration(astRoot, className);
		if (type == null)
			return false;

		Modifier finalMod = astRoot.getAST().newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);

		ListRewrite modRewrite = rewrite.getListRewrite(type, TypeDeclaration.MODIFIERS2_PROPERTY);
		modRewrite.insertLast(finalMod, null);
		
		return true;

	}

	public String getLabel() {
		return "Make inner class static";
	}

}
