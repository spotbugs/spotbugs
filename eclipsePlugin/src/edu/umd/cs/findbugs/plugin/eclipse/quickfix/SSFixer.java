package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.Modifier;


public class SSFixer extends FieldModifierFixer {

	public Modifier.ModifierKeyword getModifierToAdd() {
		return Modifier.ModifierKeyword.STATIC_KEYWORD;
	}


	public String getLabel() {
		return "Change to a \"static\" field";
	}

}
