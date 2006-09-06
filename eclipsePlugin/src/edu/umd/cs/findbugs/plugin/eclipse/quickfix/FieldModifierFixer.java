package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import edu.umd.cs.findbugs.BugInstance;

public abstract class FieldModifierFixer extends FindBugsFixer {

	@Override
	protected boolean resolveBindings() {
		return false;
	}

	@Override
	protected boolean modify(ASTRewrite rewrite, CompilationUnit astRoot,
			BugInstance bug) {
		TypeDeclaration type;
		FieldDeclaration field;
		
		String className = bug.getPrimaryClass().getClassName();
		String fieldName = bug.getPrimaryField().getFieldName();
		
		type = getTypeDeclaration(astRoot, className);
		if (type == null)
			return false;
		
		field = getFieldDeclaration(type, fieldName);
		if (field == null)
			return false;

		Modifier finalMod = astRoot.getAST().newModifier(getModifierToAdd());

		ListRewrite modRewrite = rewrite.getListRewrite(field, FieldDeclaration.MODIFIERS2_PROPERTY);
		modRewrite.insertLast(finalMod, null);
		
		return true;
	}
	
	public abstract ModifierKeyword getModifierToAdd();

	public abstract String getLabel();

}
