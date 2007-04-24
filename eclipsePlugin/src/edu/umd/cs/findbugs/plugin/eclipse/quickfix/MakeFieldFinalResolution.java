package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.Modifier;

/**
 * The <CODE>Mak</CODE> adds a <CODE>final</CODE> modifier to a field.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#MS_SHOULD_BE_FINAL">MS_SHOULD_BE_FINAL</a>
 */
public class MakeFieldFinalResolution extends FieldModifierResolution {

	@Override
	public Modifier.ModifierKeyword getModifierToAdd() {
		return Modifier.ModifierKeyword.FINAL_KEYWORD;
    }

}
