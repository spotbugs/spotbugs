package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.Modifier;


public class MSFixer extends FieldModifierFixer {

	public String getLabel() {
		return "Add \"final\" to field";
	}
	
	public Modifier.ModifierKeyword getModifierToAdd() {
		return Modifier.ModifierKeyword.FINAL_KEYWORD;
	}

}
