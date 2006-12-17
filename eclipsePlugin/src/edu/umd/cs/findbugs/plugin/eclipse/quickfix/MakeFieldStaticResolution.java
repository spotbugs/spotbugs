package edu.umd.cs.findbugs.plugin.eclipse.quickfix;

import org.eclipse.jdt.core.dom.Modifier;

/**
 * The <CODE>MakeFieldStaticResolution</CODE> adds a <CODE>static</CODE>
 * modifier to a field.
 * 
 * @see <a href="http://findbugs.sourceforge.net/bugDescriptions.html#SS_SHOULD_BE_STATIC">SS_SHOULD_BE_STATIC</a>
 */
public class MakeFieldStaticResolution extends FieldModifierResolution {

    @Override
    public Modifier.ModifierKeyword getModifierToAdd() {
        return Modifier.ModifierKeyword.STATIC_KEYWORD;
    }

}
